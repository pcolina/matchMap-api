# Design

## Context

The API has no place data yet. The only domain capabilities are `user-accounts`, `authentication`, `username-moderation`, `api-version` and `service-health` (see proposal.md - Depends on). The database is PostgreSQL with PostGIS, managed by Flyway; the latest migration is `V7__add_user_profile.sql`, so the new migrations start at `V8`. The contracts are in `contracts/graphql/account.graphqls` and `schema.graphqls`.

Contract baseline for the Definition of Done check: `06fed8d`, the commit where this change was proposed.

## Goals / Non-Goals

**Goals:**
- Keep the catalog in the database, so that pins always reference existing rows.
- Derive the country status from the user's city pins in one query per request, not per country.
- Give web and mobile clients everything they need to paint the map, through the contract in `contracts/graphql/`.

**Non-Goals:**
- Storing a status for countries.
- No PostGIS geometry. The clients paint the country shapes from the ISO country code, and cities are stored as plain latitude and longitude values, because the map only needs points and no spatial queries.
- Suggestions, friend comparison and changes to the catalog by users.

## Decisions

### Decision: own copy of the catalog, loaded once

The catalog is stored in the database and loaded once through an import, not read from a public API at request time.

- Pins reference rows of `city` by foreign key, so a city cannot disappear or change identity between requests.
- The map and the city lists do not depend on a network call or on the limits of an external service.
- Alternative considered: calling a public API per request. Rejected: it adds latency and a failure point, and it cannot enforce referential integrity.
- The source is a set of three CSV files that the administrator prepares: `countries.csv`, `country_names.csv` and `cities.csv`. The columns follow the table columns, except that `country_code` replaces `country_id` and `successor_code` replaces `successor_id`, because a UUID is not known before the import. `id` and `active` are not in the files.
- The key of a country is its ISO code, not its name. The source language is English: `country.name` holds the English name, and `country_name` holds `es`, `de`, `fr`, `ja`, `zh-Hant` and `ko`.
- A file with any invalid row is rejected as a whole. The import reports each invalid row with its line number and the reason.
- `successor_code` is filled only when every country recognises the absorption. A territory that only some countries recognise stays an independent country, with no successor. The administrator decides the recognition; the API stores that decision.

### Decision: tables

- `country`: `id` (UUID), `code` (ISO 3166-1 alpha-2, unique, `CHECK (code ~ '^[A-Z]{2}$')`), `name` (source language), `successor_id` (UUID, nullable, foreign key to `country`, `CHECK (successor_id IS NULL OR successor_id <> id)`), set when the country is merged into another.
- `country_name`: `country_id`, `language`, `name`, primary key on `(country_id, language)`. Holds the names requested through `Accept-Language`, for the seven supported tags `en`, `es`, `de`, `fr`, `ja`, `zh-Hant` and `ko`. The fallback to the source name is applied in the query.
- `city`: `id` (UUID), `country_id` (`NOT NULL`, foreign key to `country`), `name`, `latitude` and `longitude` (`numeric(9,6)`, both `NOT NULL`, with `CHECK (latitude BETWEEN -90 AND 90)` and `CHECK (longitude BETWEEN -180 AND 180)`), `active` (`boolean`, `NOT NULL`, default `true`; `false` for a retired city), `city_key` (`varchar(50)`, `NOT NULL`, unique; a stable identifier assigned by the administrator, used to match a city across imports).

Alternative considered for the coordinates: a PostGIS `geography(Point)` column. Rejected for now: the map needs only points, and no spatial query is planned. It can be added later without changing the contract.
- `user_city_pin`: `user_id` (foreign key to `user_account`), `city_id` (foreign key to `city`), `status` (`CHECK (status IN ('VISITED','WANT_TO_GO','LIVED'))`), primary key on `(user_id, city_id)`. The primary key enforces "one pin per user and city".

Alternative considered: one status column in `country`. Rejected: the status belongs to the user, not to the country.

Alternative considered: a single table with a `type` column for countries and cities. Rejected: it loses the foreign key from city to country and from pin to city.

### Decision: country status is derived in the query

The GraphQL field `Country.status` is resolved from the user's pins with one aggregate query, using the priority `LIVED` > `VISITED` > `WANT_TO_GO`. The clients receive the derived value; they never receive a stored one.

Alternative considered: a stored `user_country_status` table updated on each pin change. Rejected for now: it duplicates data that can be recomputed, and it needs extra logic to keep it in step when a pin is removed.

### Decision: contract shape

Proposed contract changes, to be written in `contracts/graphql/` before the implementation:

- Types: `Country { code, name, status: PinStatus, cities: [City!]! }`, `City { id, name, latitude: Float!, longitude: Float!, country: Country! }`, `CityPin { city: City!, status: PinStatus! }`, and `enum PinStatus { VISITED WANT_TO_GO LIVED }`.
- Queries: `countries: [Country!]!` (catalog with the status of the signed-in user; the status is `null` without a token or without pins), and `cityPins: [CityPin!]!` (signed-in user only).
- Mutations: `setCityPin(cityId: ID!, status: PinStatus!): CityPin!` and `removeCityPin(cityId: ID!): Boolean!`.
- Admin mutations, only for the `ADMIN` role: `createCountry(code: String!, name: String!): Country!`, `createCity(countryId: ID!, name: String!, latitude: Float!, longitude: Float!): City!`, `retireCity(cityId: ID!): City!` and `mergeCountry(countryId: ID!, successorId: ID!): Country!`. There is no delete mutation.
- Batch import, for the initial load only: `importCatalog(input: CatalogImportInput!): CatalogImportResult!`. It validates the whole batch, writes nothing if any row fails, and returns each failing row with its line number and reason. Single rows use the admin mutations above.
- `countries` returns a merged country as its successor (code and name), so clients paint the successor's shape and status. The stored row of the merged country is kept.

The `cities` field of `Country` returns only `VISITED` and `LIVED` cities of the user, as stated in `travel-pins`. The catalog is changed only by administrators (`geo-catalog`), through the admin mutations above. Cities are retired and countries are merged; nothing is deleted.

Alternative considered: an OpenAPI endpoint for the map. Rejected: GraphQL already fits these operations, and the project rule is GraphQL by default.

### Decision: Flyway migrations

- `V8__create_geo_catalog.sql`: creates `country`, `country_name` and `city`, the last one with `latitude` and `longitude`.
- `V9__create_user_city_pin.sql`: creates `user_city_pin`.

Migrations are append-only; `V1` already enables PostGIS, so no extension is created here. The catalog data is loaded by the import, not inside a migration, so that the migration stays the same whatever the source is.

### Decision: dependencies

No new dependency is added. The import reads the local data set file. Its format decides the parser; if that needs a new dependency, it is proposed in a design update before it is added.

## Risks / Trade-offs

- [Licence or attribution of the local data set is not checked here] → The administrator owns the data set. This change adds no attribution; confirm the licence before the catalog is published.
- [Catalog too large for the MVP] → The city threshold or list is an open point; the schema does not change with it.
- [Data set has no coordinates, or they are imprecise] → The data set must give a latitude and a longitude for each city; the import rejects a city without them.
- [Source lacks some country names in the seven languages] → The import lists the missing names in its output; a missing name falls back to the source name, as the spec says.
- [Derived status query gets slow with many pins] → One aggregate per request, filtered by `user_id`; the primary key of `user_city_pin` starts with `user_id`, so the lookup is indexed.
- [Clients paint a country with a stale status after a pin change] → Clients refetch `countries` after a mutation; the status is never cached on the server.
- [Duplicate pins on concurrent requests] → The primary key `(user_id, city_id)` makes the second insert fail; `setCityPin` uses an upsert so that the status is replaced, as specified.

## Open Questions

The source is decided: the three CSV files above, prepared by the administrator. The city list is decided by the administrator. The loading path is decided: single rows use the admin mutations, and the initial load uses one batch import. The `city_key` column is added to `city`. Neither point changes the approach.
