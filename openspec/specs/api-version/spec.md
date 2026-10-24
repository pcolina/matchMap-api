# api-version Specification

## Purpose
Lets clients and developers confirm which version of the API they are talking to, and proves the GraphQL contract is served end to end.

## Requirements

### Requirement: GraphQL endpoint is available
The system SHALL accept GraphQL operations over `POST /graphql` with a JSON body, and the operations it accepts SHALL be exactly those defined in `contracts/graphql/`.

#### Scenario: Unknown field is rejected
- **WHEN** a client sends `POST /graphql` with the query `{ unknownField }`
- **THEN** the response contains an `errors` array with a validation error
- **AND** the response contains no `data.unknownField`

### Requirement: API version query
The GraphQL contract SHALL define the query `apiVersion: String!`, which returns the version of the running build. The value SHALL be the same version that the build assigns to the API artifact.

#### Scenario: Client requests the API version
- **WHEN** a client sends `POST /graphql` with the query `{ apiVersion }`
- **THEN** the response is HTTP 200
- **AND** `data.apiVersion` equals the version of the running build
- **AND** the response contains no `errors`
