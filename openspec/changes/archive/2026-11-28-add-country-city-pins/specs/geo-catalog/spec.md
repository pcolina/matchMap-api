## Purpose

Holds the countries and cities that users can pin, as a copy owned by MatchMap. Each city belongs to exactly one country, and every country and city has a stable identifier that pins can reference.

## ADDED Requirements

### Requirement: Country and city catalog is stored in the database

The system SHALL store countries and cities in the database. Each country SHALL have an identifier, an ISO 3166-1 alpha-2 code that is unique, and a name in the source language. Each city SHALL have an identifier, a name in the source language, exactly one country it belongs to, and a latitude and a longitude in decimal degrees (WGS 84).

#### Scenario: Catalog data is read from the database
- **WHEN** a client asks for a country that is stored in the database
- **THEN** the response is built from the stored row, with no call to any external service

#### Scenario: Every city belongs to a country
- **WHEN** a row is inserted into the city table with no country identifier
- **THEN** the database rejects the row

#### Scenario: Country codes are unique
- **WHEN** a country is inserted with a code that already exists
- **THEN** the database rejects the row

#### Scenario: Country codes are two uppercase letters
- **WHEN** a country is inserted with the code `es` or `SPAIN`
- **THEN** the database rejects the row

#### Scenario: City key is unique
- **WHEN** a city is inserted with a city key that another city already has
- **THEN** the database rejects the row

#### Scenario: City without latitude is rejected
- **WHEN** a row is inserted into the city table with no latitude
- **THEN** the database rejects the row

#### Scenario: Latitude outside the range is rejected
- **WHEN** a city is inserted with latitude `91`
- **THEN** the database rejects the row

#### Scenario: Longitude outside the range is rejected
- **WHEN** a city is inserted with longitude `181`
- **THEN** the database rejects the row

### Requirement: Cities expose their coordinates

The GraphQL type `City` SHALL expose `latitude: Float!` and `longitude: Float!`, with the stored values. The field `cities` of `Country` SHALL return every city of the catalog in that country, whether or not any user has pinned it.

#### Scenario: City coordinates are returned
- **WHEN** a client asks for the cities of Spain with `{ countries { code cities { name latitude longitude } } }` and `Madrid` is stored at latitude `40.4168` and longitude `-3.7038`
- **THEN** the response contains `latitude` `40.4168` and `longitude` `-3.7038` for `Madrid`

### Requirement: Catalog source

The catalog SHALL be loaded only from a local data set that the administrator prepares. The API SHALL NOT query any external data source at request time.

#### Scenario: Source is used only by the import
- **WHEN** the API handles any GraphQL request
- **THEN** it does not open a connection to any external data source

### Requirement: Catalog is loaded once, not at request time

The system SHALL load the catalog through a one-time import or a migration, not on each request. The import SHALL be repeatable without creating duplicate countries or cities. Each city SHALL be matched across imports by its city key, which is unique in the catalog. The administrator decides which cities the data set includes.

#### Scenario: Invalid row rejects the whole file
- **WHEN** an import file contains a row that breaks a stored rule, such as a country code in lowercase
- **THEN** the import writes nothing from that file
- **AND** the import reports the line number and the reason of each invalid row

#### Scenario: Re-running the import creates no duplicates
- **WHEN** the catalog import runs twice on the same database
- **THEN** the number of countries and cities is the same after the second run as after the first

#### Scenario: Requests do not read the data set
- **WHEN** a client sends a GraphQL query for countries after the import has run
- **THEN** the response contains the catalog data stored in the database

### Requirement: Countries are returned in the language requested

Country names SHALL be returned in the language requested through the `Accept-Language` header. The supported languages SHALL be `en`, `es`, `de`, `fr`, `ja`, `zh-Hant` (Traditional Chinese) and `ko`. If no name exists in the requested language, or the language is not supported, the system SHALL return the name in the source language. City names are returned in the source language only.

#### Scenario: Country name in the requested language
- **WHEN** a client sends `Accept-Language: es` and asks for the name of the country with code `FR`
- **THEN** the response contains the Spanish name of France

#### Scenario: Fallback to the source language
- **WHEN** a client sends `Accept-Language: de` and the catalog has no German name for the country with code `FR`
- **THEN** the response contains the source-language name of France

#### Scenario: Japanese and Traditional Chinese names
- **WHEN** a client sends `Accept-Language: ja` and asks for the name of the country with code `FR`, and then sends `Accept-Language: zh-Hant` and asks for the same country
- **THEN** the first response contains the Japanese name of France and the second contains its Traditional Chinese name

### Requirement: Only administrators change the catalog

Countries and cities SHALL be created, retired and merged only through the API, by a user with the `ADMIN` role. No user without that role SHALL change the catalog.

#### Scenario: Non-admin cannot change the catalog
- **WHEN** a signed-in user without the `ADMIN` role sends a mutation that creates, retires or merges a country or a city
- **THEN** the API rejects the operation and changes nothing

### Requirement: Cities are retired, not deleted

A city that no longer exists as a place SHALL be retired. A retired city keeps its row and its identifier, and SHALL NOT accept new pins. Its existing pins SHALL remain.

#### Scenario: Retired city keeps its pins
- **WHEN** an administrator retires the city `Madrid` and a user has a pin on it
- **THEN** the pin still exists and the city is still returned, with its coordinates, to the user who has it pinned

#### Scenario: Retired city rejects new pins
- **WHEN** a user tries to pin a retired city
- **THEN** the API rejects the pin and writes nothing

### Requirement: Countries are merged, not deleted

When a country becomes part of another country, an administrator SHALL merge it into that successor. The cities of the merged country SHALL move to the successor. The merged country SHALL keep its row and its code, and SHALL point to its successor. A merged country SHALL NOT accept new cities.

#### Scenario: Merged country keeps its cities
- **WHEN** an administrator merges country `X` into country `Y`
- **THEN** every city of `X` belongs to `Y`, and `X` keeps its code and points to `Y` as its successor
- **AND** the pins on those cities are unchanged

#### Scenario: Country cannot merge into itself or a missing country
- **WHEN** an administrator merges a country into itself or into an identifier that does not exist
- **THEN** the API rejects the operation and changes nothing

### Requirement: Merges require universal recognition

A merge SHALL be recorded only when every country recognises the absorption. A territory that only some countries recognise SHALL remain an independent country, with its own code and cities, and no successor.

#### Scenario: Territory recognised by some countries stays independent
- **WHEN** a territory is recognised as part of another country by some countries but not by all
- **THEN** it is stored as an independent country with its own code and cities
- **AND** it has no successor

### Requirement: Merged country is returned as its successor

The `countries` query SHALL NOT return a merged country under its own code. It SHALL return the successor, with the cities that moved from the merged country.

#### Scenario: Merged country is returned as its successor
- **WHEN** a client asks for countries after country `X` is merged into country `Y`
- **THEN** the response has no entry with the code of `X`
- **AND** the entry for `Y` includes the cities that moved from `X`

### Requirement: No deletion of countries or cities

No operation in the API SHALL delete a country or a city.

#### Scenario: Deletion operations do not exist
- **WHEN** a client asks the schema for a mutation that deletes a country or a city
- **THEN** no such mutation is defined in `contracts/graphql/`
