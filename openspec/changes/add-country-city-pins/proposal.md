# Proposal

Closes #52 (https://github.com/pcolina/matchMap-api/issues/52)

## Why

Users of MatchMap need to show on a world map the countries and cities they have visited, lived in or want to go to. Today the API has no place catalog and no way to store that information, so the map cannot be built. The API needs its own copy of the countries and cities, because pins must point to rows that always exist, and the map must not depend on an external service at request time.

## What Changes

- Add a geographic catalog of countries and cities, stored in the database and loaded once from a public source. Each city belongs to exactly one country and has a latitude and a longitude, so that the map can show it as a point. The catalog is read-only for users.
- Add per-user pins for cities. Each pin links one user and one city with one status: `VISITED`, `WANT_TO_GO` or `LIVED`. A city where the user lives is a visited city, so its status is `LIVED`.
- Derive the status of each country from its pinned cities, without storing it. Priority: `LIVED` over `VISITED` over `WANT_TO_GO`. A country with no pinned cities has no status.
- Expose the catalog and the user's pins through GraphQL, so the web and mobile clients can paint the map: the country colour depends on its derived status, and the cities with status `VISITED` or `LIVED` are shown as points at their coordinates. Countries with status `WANT_TO_GO` are also painted, in their own colour.
- Country names are returned in the language requested through `Accept-Language`, in seven languages: `en`, `es`, `de`, `fr`, `ja`, `zh-Hant` (Traditional Chinese) and `ko`. If the requested language has no name for a country, the name in the source language is returned. The list comes from the frontend issue #17.

## Capabilities

### New Capabilities

- `geo-catalog`: the read-only catalog of countries and cities, loaded from a public source into the database, with the country each city belongs to, and the country names in the language requested.
- `travel-pins`: the signed-in user's pins on cities, the status of each pin, and the status of each country derived from them.

### Modified Capabilities

None. No requirement of an existing capability changes. `user-accounts` and `authentication` are only used to identify the signed-in user.

## Out of scope

- Suggestions of countries or cities of any kind. They may be calculated on demand in a future change and are not stored.
- Cities that a user adds that are not in the catalog.
- Pins on countries. A country status exists only as the result of its cities.
- Comparison of maps between users (friends, Jaccard score).
- Calls to a public API at request time. The public source is used only to load the catalog.
- Changes to the web or mobile clients, which live in another repository.

## Depends on

- `user-accounts`: the signed-in account that owns the pins.
- `authentication`: access tokens that identify the signed-in user on protected operations.
- Frontend issue #17 (https://github.com/pcolina/MatchMap/issues/17): source of the seven languages. Its backend part is included in this change.

## Open points

- [NEEDS CLARIFICATION: Which public source feeds the catalog, and does its licence allow storing and redistributing the data? Which attribution is required? The source must provide a latitude and a longitude for each city.]
- [NEEDS CLARIFICATION: Which cities does the catalog include (for example, all cities above a population threshold, or a fixed list), and what is the threshold or list?]

## Impact

- Database: new tables for countries, cities and user city pins, added by new Flyway migrations.
- Contracts: new GraphQL types, queries and mutations in `contracts/graphql/`.
- Code: a new catalog loader, the pin operations and the derived country status.
- Dependencies: none listed yet; any new dependency needs an approved `design.md`.
