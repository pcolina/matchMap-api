## Purpose

Lets a signed-in user pin cities with a status (visited, lived or want to go), and derives the status of each country from the cities the user has pinned, so that clients can paint the world map. Only the owner of the pins can read or change them.

## ADDED Requirements

### Requirement: A user pins a city with one status

The signed-in user SHALL be able to pin a city of the catalog with one status: `VISITED`, `WANT_TO_GO` or `LIVED`. The pin SHALL belong to that user only. A user SHALL have at most one pin per city: pinning a city that already has a pin SHALL replace its status. A city where the user lives SHALL be pinned as `LIVED`, which counts as a visited city.

#### Scenario: Pin a city as visited
- **WHEN** a signed-in user pins the city with id `c1` as `VISITED`
- **THEN** the user has one pin for `c1` with status `VISITED`

#### Scenario: Pinning again replaces the status
- **WHEN** a signed-in user has the city `c1` pinned as `WANT_TO_GO` and pins `c1` as `VISITED`
- **THEN** the user has exactly one pin for `c1`, with status `VISITED`

#### Scenario: Pins are not shared between users
- **WHEN** user `ana` pins the city `c1` as `VISITED` and user `bob` has no pin for `c1`
- **THEN** `bob` still has no pin for `c1`

### Requirement: Pins require a signed-in user and an existing city

Pin operations SHALL require a valid access token. A pin SHALL reference a city that exists in the catalog. The status SHALL be one of the three allowed values. [NEEDS CLARIFICATION: Which error codes and messages are returned for an unknown city, an invalid status and a missing token? Use the existing error code style of `user-accounts` unless told otherwise.]

#### Scenario: Pin without a token is rejected
- **WHEN** a client without an access token pins the city `c1`
- **THEN** the request fails and no pin is stored

#### Scenario: Pin of a city that does not exist is rejected
- **WHEN** a signed-in user pins a city identifier that is not in the catalog
- **THEN** the request fails and no pin is stored

#### Scenario: Unknown status is rejected
- **WHEN** a signed-in user pins the city `c1` with the status `PREMIUM`
- **THEN** the request fails and no pin is stored

### Requirement: A user removes a city pin

The signed-in user SHALL be able to remove their own pin on a city. Removing a city without a pin SHALL leave the user's pins unchanged. A removed pin SHALL no longer count in the derived country status.

#### Scenario: Remove an existing pin
- **WHEN** a signed-in user with a pin on `c1` removes it
- **THEN** the user has no pin for `c1`

#### Scenario: Remove a city without a pin
- **WHEN** a signed-in user removes the pin of a city they have not pinned
- **THEN** the user's pins are unchanged

### Requirement: A user reads only their own pins

The signed-in user SHALL be able to read the list of their own city pins, each with the city and its status. A client SHALL NOT be able to read the pins of another user.

#### Scenario: Read own pins
- **WHEN** a signed-in user with pins on `c1` as `VISITED` and `c2` as `WANT_TO_GO` reads their pins
- **THEN** the result contains both pins with their statuses

#### Scenario: No pins for a new user
- **WHEN** a signed-in user who has never pinned a city reads their pins
- **THEN** the result is an empty list

#### Scenario: Pins are not readable without a token
- **WHEN** a client without an access token reads pins
- **THEN** the request fails and returns no pin data

### Requirement: Country status is derived from its cities

The status of a country for a user SHALL be calculated from the user's city pins in that country, and SHALL NOT be stored. The priority SHALL be: `LIVED` if any city of the country is pinned as `LIVED`; otherwise `VISITED` if any city is pinned as `VISITED`; otherwise `WANT_TO_GO` if any city is pinned as `WANT_TO_GO`. A country with no pinned cities SHALL have no status.

#### Scenario: Lived city makes the country lived
- **WHEN** a user has `Madrid` pinned as `LIVED` and `Granada` pinned as `WANT_TO_GO`, both in Spain
- **THEN** the status of Spain for that user is `LIVED`

#### Scenario: Visited city makes the country visited
- **WHEN** a user has `New York` pinned as `VISITED` and no other pins in the United States
- **THEN** the status of the United States for that user is `VISITED`

#### Scenario: Only wanted cities make the country want to go
- **WHEN** a user has `San Francisco` pinned as `WANT_TO_GO` and no visited or lived pins in the United States
- **THEN** the status of the United States for that user is `WANT_TO_GO`

#### Scenario: Lived beats visited in the same country
- **WHEN** a user has `New York` pinned as `VISITED` and `Denver` pinned as `LIVED`
- **THEN** the status of the United States for that user is `LIVED`

#### Scenario: Country without pins has no status
- **WHEN** a user has no pins in Japan
- **THEN** the status of Japan for that user is empty

### Requirement: Map data lists the cities that the user has visited

For each country, the result SHALL include the cities pinned as `VISITED` or `LIVED`, and SHALL NOT include the cities pinned as `WANT_TO_GO` in that list. The cities pinned as `WANT_TO_GO` SHALL still appear in the user's pins.

#### Scenario: Wanted cities are not in the visited list
- **WHEN** a user has `Granada` pinned as `WANT_TO_GO` and `Madrid` pinned as `LIVED` in Spain
- **THEN** the visited cities of Spain for that user are `Madrid` only
- **AND** the user's pins still include `Granada` with status `WANT_TO_GO`

#### Scenario: Visited cities come with coordinates
- **WHEN** a user has `Madrid` pinned as `LIVED`, with latitude `40.4168` and longitude `-3.7038`, and asks for the visited cities of Spain
- **THEN** the result contains `Madrid` with its latitude and longitude
