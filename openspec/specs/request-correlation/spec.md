# request-correlation Specification

## Purpose
Lets a single request be traced across the client and the API logs by attaching one correlation ID to every HTTP request and every log line it produces.

## Requirements

### Requirement: Reuse a correlation ID sent by the client
When an HTTP request carries a valid `X-Correlation-Id` header, the system SHALL use that value as the correlation ID of the request and SHALL return it unchanged in the `X-Correlation-Id` response header.

#### Scenario: Client sends a correlation ID
- **WHEN** a client sends any HTTP request with the header `X-Correlation-Id: 3f2b8c1e-7a4d-4e2b-9c11-5d6e7f8a9b0c`
- **THEN** the response has the header `X-Correlation-Id: 3f2b8c1e-7a4d-4e2b-9c11-5d6e7f8a9b0c`

### Requirement: Generate a correlation ID when the client sends none
When an HTTP request has no `X-Correlation-Id` header, the system SHALL generate a new random UUID as the correlation ID and SHALL return it in the `X-Correlation-Id` response header. Two requests without the header SHALL receive different IDs.

#### Scenario: Client sends no correlation ID
- **WHEN** a client sends an HTTP request without the `X-Correlation-Id` header
- **THEN** the response has an `X-Correlation-Id` header whose value is a UUID

#### Scenario: Each request gets its own ID
- **WHEN** a client sends two HTTP requests without the `X-Correlation-Id` header
- **THEN** the two responses have different `X-Correlation-Id` values

### Requirement: Reject unsafe correlation IDs
A client-supplied `X-Correlation-Id` SHALL be valid only if it has 1 to 64 characters from `A-Z`, `a-z`, `0-9` and `-`. The system SHALL ignore any other value, including an empty one, and SHALL generate a new UUID as if the header were absent.

#### Scenario: Client sends an over-long correlation ID
- **WHEN** a client sends an HTTP request whose `X-Correlation-Id` value has 65 characters
- **THEN** the response `X-Correlation-Id` header is a newly generated UUID

#### Scenario: Client sends an invalid correlation ID
- **WHEN** a client sends an HTTP request whose `X-Correlation-Id` value contains a line break
- **THEN** the response `X-Correlation-Id` header is a newly generated UUID
- **AND** the invalid value does not appear in the logs

### Requirement: Correlation ID appears in the logs
Every log line that the system writes while handling an HTTP request SHALL include that request's correlation ID. Log lines written outside a request SHALL NOT carry a correlation ID from a previous request.

#### Scenario: Log lines carry the request's ID
- **WHEN** a client sends an HTTP request with `X-Correlation-Id: test-correlation-1` and the system writes a log line while handling it
- **THEN** that log line contains `test-correlation-1`

#### Scenario: ID does not leak into the next request
- **WHEN** a request with `X-Correlation-Id: test-correlation-1` finishes and a second request without the header is handled on the same thread
- **THEN** no log line written for the second request contains `test-correlation-1`
