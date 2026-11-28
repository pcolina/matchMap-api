# Proposal

Closes #52 (https://github.com/pcolina/matchMap-api/issues/52)

## Why

Users of MatchMap need to show on a world map the countries and cities they have visited, lived in or want to go to. Today the API has no place catalog and no way to store that information, so the map cannot be built. The API needs its own copy of the countries and cities, because pins must point to rows that always exist, and the map must not depend on an external service at request time.

## What Changes

- Add a geographic catalog of countries and cities, stored in the database and loaded once from a local data set that the administrator prepares and loads from `matchmap-control-panel` (issue #2). Each city belongs to exactly one country and has a latitude and a longitude, so that the map can show it as a point. Only administrators change the catalog, through the API. Cities are retired and countries are merged into their successor; nothing is deleted.
- Add per-user pins for cities. Each pin links one user and one city with one status: `VISITED`, `WANT_TO_GO` or `LIVED`. A city where the user lives is a visited city, so its status is `LIVED`.
- Derive the status of each country from its pinned cities, without storing it. Priority: `LIVED` over `VISITED` over `WANT_TO_GO`. A country with no pinned cities has no status.
- Expose the catalog and the user's pins through GraphQL, so the web and mobile clients can paint the map: the country colour depends on its derived status, and the cities with status `VISITED` or `LIVED` are shown as points at their coordinates. Countries with status `WANT_TO_GO` are also painted, in their own colour.
- Country names are returned in the language requested through `Accept-Language`, in seven languages: `en`, `es`, `de`, `fr`, `ja`, `zh-Hant` (Traditional Chinese) and `ko`. If the requested language has no name for a country, the name in the source language is returned. The list comes from the frontend issue #17.

## Capabilities

### New Capabilities

- `geo-catalog`: the read-only catalog of countries and cities, loaded from a local data set into the database, with the country each city belongs to, and the country names in the language requested.
- `travel-pins`: the signed-in user's pins on cities, the status of each pin, and the status of each country derived from them.

### Modified Capabilities

None. No requirement of an existing capability changes. `user-accounts` and `authentication` are only used to identify the signed-in user.

## Out of scope

- Suggestions of countries or cities of any kind. They may be calculated on demand in a future change and are not stored.
- Cities that a user adds that are not in the catalog.
- Pins on countries. A country status exists only as the result of its cities.
- Comparison of maps between users (friends, Jaccard score).
- Calls to any external data source at request time. The local data set is used only by the import.
- Changes to the web or mobile clients, which live in another repository.
- The border geometry of the map. The API stores no polygons; the client keeps them as its own asset and joins them to the catalog by the ISO 3166-1 alpha-2 country code.

## Depends on

- `user-accounts`: the signed-in account that owns the pins.
- `authentication`: access tokens that identify the signed-in user on protected operations.
- Frontend issue #17 (https://github.com/pcolina/MatchMap/issues/17): source of the seven languages. Its backend part is included in this change.
- `matchmap-control-panel` issue #2 (https://github.com/pcolina/matchmap-control-panel/issues/2): the panel the administrator uses to manage catalog rows through the API.
- `matchMap-api` issue #53 (https://github.com/pcolina/matchMap-api/issues/53): the `ADMIN` role that authorises catalog changes.
- Frontend issue `pcolina/MatchMap#68` (https://github.com/pcolina/MatchMap/issues/68): client work that reads country names and city points from this catalog instead of bundled data. It is separate from this change and does not block it.

## Open points

- The initial load is one batch import (`importCatalog`); single rows use the admin mutations.
- The administrator decides which cities the data set includes. The API stores the cities that the data set provides.

## Impact

- Database: new tables for countries, cities and user city pins, added by new Flyway migrations.
- Contracts: new GraphQL types, queries and mutations in `contracts/graphql/`.
- Code: a new catalog loader, the pin operations and the derived country status.
- Dependencies: none listed yet; any new dependency needs an approved `design.md`.
