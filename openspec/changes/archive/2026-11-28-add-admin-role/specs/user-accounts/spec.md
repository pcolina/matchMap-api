## MODIFIED Requirements

### Requirement: Read own account
The GraphQL query `me: User!` SHALL return the account of the user identified by the access token, with `id`, `email`, `username`, `createdAt`, `isFirstVisit`, `firstName`, `lastName`, `subscription`, `nationality`, `birthCountry` and `role`. Without an access token it SHALL fail with an error whose `extensions.classification` is `UNAUTHORIZED` and SHALL return no account data.

#### Scenario: Signed-in user reads their account
- **WHEN** a client sends `{ me { id email username isFirstVisit firstName lastName subscription nationality birthCountry } }` with a valid access token for `ana@example.com`
- **THEN** `data.me` contains the id, email, username, `isFirstVisit`, the profile fields and `subscription` of that account

#### Scenario: Signed-in user reads their role
- **WHEN** a client sends `{ me { role } }` with a valid access token for `ana@example.com`
- **THEN** `data.me.role` is `USER` for an account that has not been given the administrator role

#### Scenario: Anonymous client reads me
- **WHEN** a client sends `{ me { id } }` without an access token
- **THEN** the response has an error whose `extensions.classification` is `UNAUTHORIZED`
- **AND** the response contains no `data.me` account

#### Scenario: Me has no displayName field
- **WHEN** a client sends `{ me { displayName } }` with a valid access token
- **THEN** the response has a GraphQL validation error, because `User` has no `displayName` field
