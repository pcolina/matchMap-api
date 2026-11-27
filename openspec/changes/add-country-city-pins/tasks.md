# Tasks

Every task follows TDD: write the failing test first, run it and show it failing, write the minimum code that makes it pass, then refactor. Each task is one commit with `Task: <id>` in the body.

## 1. Contract

- [x] 1.1 Add the types `Country`, `City`, `CityPin` and `enum PinStatus { VISITED WANT_TO_GO LIVED }` to `contracts/graphql/account.graphqls` or to a new `contracts/graphql/places.graphqls`, following the design. Test: the schema loads with the `Country.status`, `City.latitude`, `City.longitude` and `CityPin` fields (covers the GraphQL schema check used by `api-version`).
- [x] 1.2 Add the queries `countries` and `cityPins` and the mutations `setCityPin` and `removeCityPin` to the contract. Test: schema introspection lists the queries `countries` and `cityPins` and the mutations `setCityPin` and `removeCityPin`, with their arguments. The check for unmapped fields moves to the tasks that implement each resolver (3.3, 3.4, 6.1, 6.7 and 7.1).

## 2. Catalog storage

- [ ] 2.1 Test (Testcontainers): inserting a city without a country is rejected. Covers scenario "Every city belongs to a country" in `geo-catalog`. Then add migration `V8__create_geo_catalog.sql` with the tables `country`, `country_name` and `city`.
- [ ] 2.2 Test: a country with a duplicate code is rejected. Covers "Country codes are unique". Then add the unique constraint in `V8`.
- [ ] 2.3 Test: a country with code `es` or `SPAIN` is rejected. Covers "Country codes are two uppercase letters". Then add the `CHECK` constraint in `V8`.
- [ ] 2.4 Test: a city without latitude is rejected. Covers "City without latitude is rejected". Then add `latitude` and `longitude` as `numeric(9,6) NOT NULL` in `V8`.
- [ ] 2.5 Test: a city with latitude `91` is rejected. Covers "Latitude outside the range is rejected". Then add the latitude `CHECK` in `V8`.
- [ ] 2.6 Test: a city with longitude `181` is rejected. Covers "Longitude outside the range is rejected". Then add the longitude `CHECK` in `V8`.

## 3. Catalog import

- [ ] 3.1 [NEEDS CLARIFICATION: Which public source feeds the catalog, its licence and attribution; which cities are included; the source must give a latitude and a longitude for each city. Do not start this task until these are answered.]
- [ ] 3.2 Test: running the import twice leaves the same number of countries and cities. Covers "Re-running the import creates no duplicates". Then implement the idempotent import, which stores the coordinates of each city.
- [ ] 3.3 Test: a GraphQL query for `countries` returns the stored data while the public source is unreachable. Covers "Requests do not call the public source". Then wire the query to the database only. Also test: schema inspection reports no unmapped field on `countries`.
- [ ] 3.4 Test: a query for the cities of Spain returns `Madrid` with latitude `40.4168` and longitude `-3.7038`. Covers "City coordinates are returned". Then expose `latitude` and `longitude` on `City`. Also test: schema inspection reports no unmapped field on `City`.

## 4. Country names by language

- [ ] 4.1 Test: `Accept-Language: es` returns the Spanish name of France. Covers "Country name in the requested language". Then implement the lookup in `country_name`.
- [ ] 4.2 Test: `Accept-Language: de` without a German name returns the source name. Covers "Fallback to the source language". Then implement the fallback.
- [ ] 4.3 Test: `Accept-Language: ja` returns the Japanese name of France, and `Accept-Language: zh-Hant` returns its Traditional Chinese name. Covers "Japanese and Traditional Chinese names". Then load the names for `de`, `fr`, `ja`, `zh-Hant` and `ko`, and list the missing names in the import output.

## 5. Pins storage

- [ ] 5.1 Test: a pin with a status other than the three allowed values is rejected by the database. Covers part of "Unknown status is rejected". Then add migration `V9__create_user_city_pin.sql` with `user_city_pin`, the primary key `(user_id, city_id)` and the `CHECK` on `status`.
- [ ] 5.2 Test: a second pin on the same user and city is rejected by the primary key. Then confirm the constraint in `V9`.

## 6. Pin operations

- [ ] 6.1 Test: a signed-in user pins `c1` as `VISITED` and has one pin with that status. Covers "Pin a city as visited". Then implement `setCityPin` as an upsert. Also test: schema inspection reports no unmapped field or argument on `setCityPin`.
- [ ] 6.2 Test: pinning `c1` again replaces the status and keeps one row. Covers "Pinning again replaces the status". Then keep the upsert behaviour.
- [ ] 6.3 Test: user `bob` has no pin for `c1` after `ana` pins it. Covers "Pins are not shared between users". Then scope every pin query to the signed-in user.
- [ ] 6.4 Test: a pin without a token is rejected. Covers "Pin without a token is rejected". Then require the access token on `setCityPin`.
- [ ] 6.5 Test: a pin on a city that does not exist is rejected. Covers "Pin of a city that does not exist is rejected". Then validate the city before writing.
- [ ] 6.6 Test: an unknown status is rejected. Covers "Unknown status is rejected". Then validate the status before writing.
- [ ] 6.7 Test: removing an existing pin deletes it. Covers "Remove an existing pin". Then implement `removeCityPin`. Also test: schema inspection reports no unmapped field or argument on `removeCityPin`.
- [ ] 6.8 Test: removing a city without a pin leaves the pins unchanged. Covers "Remove a city without a pin". Then make the removal idempotent.

## 7. Reading pins

- [ ] 7.1 Test: a user with pins on `c1` (`VISITED`) and `c2` (`WANT_TO_GO`) reads both. Covers "Read own pins". Then implement `cityPins`. Also test: schema inspection reports no unmapped field or argument on `cityPins`.
- [ ] 7.2 Test: a new user reads an empty list. Covers "No pins for a new user". Then keep the empty list.
- [ ] 7.3 Test: a client without a token reads no pin data. Covers "Pins are not readable without a token". Then require the token on `cityPins`.

## 8. Derived country status

- [ ] 8.1 Test: Madrid `LIVED` and Granada `WANT_TO_GO` give Spain `LIVED`. Covers "Lived city makes the country lived". Then implement the aggregate query with priority `LIVED`.
- [ ] 8.2 Test: New York `VISITED` gives the United States `VISITED`. Covers "Visited city makes the country visited". Then add the `VISITED` branch.
- [ ] 8.3 Test: San Francisco `WANT_TO_GO` gives the United States `WANT_TO_GO`. Covers "Only wanted cities make the country want to go". Then add the `WANT_TO_GO` branch.
- [ ] 8.4 Test: New York `VISITED` and Denver `LIVED` give `LIVED`. Covers "Lived beats visited in the same country". Then check the priority order.
- [ ] 8.5 Test: a country without pins has no status. Covers "Country without pins has no status". Then return `null`.
- [ ] 8.6 Test: the visited cities of Spain are Madrid only, while the pins still include Granada. Covers "Wanted cities are not in the visited list". Then filter `Country.cities` to `VISITED` and `LIVED`.
- [ ] 8.7 Test: the visited cities of Spain include `Madrid` with its latitude and longitude. Covers "Visited cities come with coordinates". Then return the coordinates in `Country.cities`.

## 9. Coverage of the specs

- [ ] 9.1 Test: every scenario in `geo-catalog` and `travel-pins` has a test that names it. Then fix any scenario without a test.
