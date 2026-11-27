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

The GraphQL type `City` SHALL expose `latitude: Float!` and `longitude: Float!`, with the stored values.

#### Scenario: City coordinates are returned
- **WHEN** a client asks for the cities of Spain with `{ countries { code cities { name latitude longitude } } }` and `Madrid` is stored at latitude `40.4168` and longitude `-3.7038`
- **THEN** the response contains `latitude` `40.4168` and `longitude` `-3.7038` for `Madrid`

### Requirement: Catalog source and licence

The catalog SHALL be loaded from a public source and SHALL NOT be queried from that source at request time. [NEEDS CLARIFICATION: Which public source feeds the catalog, and does its licence allow storing and redistributing the data? Which attribution is required? The source must provide a latitude and a longitude for each city.]

#### Scenario: Source is used only by the import
- **WHEN** the API handles any GraphQL request
- **THEN** it does not open a connection to the public source

### Requirement: Catalog is loaded once, not at request time

The system SHALL load the catalog through a one-time import or a migration, not on each request. The import SHALL be repeatable without creating duplicate countries or cities. [NEEDS CLARIFICATION: Which cities does the catalog include (all cities above a population threshold, or a fixed list)? State the threshold or list.]

#### Scenario: Re-running the import creates no duplicates
- **WHEN** the catalog import runs twice on the same database
- **THEN** the number of countries and cities is the same after the second run as after the first

#### Scenario: Requests do not call the public source
- **WHEN** a client sends a GraphQL query for countries while the public source is unreachable
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

### Requirement: Catalog is read-only for users

No user SHALL be able to create, change or delete a country or a city through the API. The catalog SHALL change only through the import.

#### Scenario: Catalog mutations do not exist
- **WHEN** a signed-in client sends a GraphQL mutation that creates a city
- **THEN** the API rejects the operation because no such mutation is defined in `contracts/graphql/`
