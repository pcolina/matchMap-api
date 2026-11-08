# Spec Delta

## Purpose

Makes `POST /api/auth/login` reject email and password values outside the limits declared in the contract, before any account lookup, so that no client can bypass them.

## ADDED Requirements

### Requirement: Sign-in input limits
`POST /api/auth/login` SHALL check `email` and `password` against the limits of `LoginRequest` in `contracts/openapi/auth.yaml` before it looks up any account. The `email` SHALL be 1 to 254 characters and SHALL be in standard email format. The `password` SHALL be 6 to 20 characters. A request whose `email` breaks a limit SHALL be rejected with HTTP 400 and the body `{"error": "invalid_email"}`. A request whose `password` breaks a limit SHALL be rejected with HTTP 400 and the body `{"error": "invalid_password"}`. The body SHALL NOT reveal whether an account exists. The complexity rules of registration SHALL NOT apply to login.

#### Scenario: Empty email
- **WHEN** a login request has an empty `email`
- **THEN** the request is rejected with the response defined above

#### Scenario: Email too long
- **WHEN** a login request has an `email` of 255 characters
- **THEN** the request is rejected with the response defined above

#### Scenario: Email not in standard format
- **WHEN** a login request has an `email` that is not in standard email format
- **THEN** the request is rejected with the response defined above

#### Scenario: Password too short
- **WHEN** a login request has a `password` of 5 characters
- **THEN** the request is rejected with the response defined above

#### Scenario: Password too long
- **WHEN** a login request has a `password` of 21 characters
- **THEN** the request is rejected with the response defined above

#### Scenario: Password without uppercase is not rejected for that reason
- **WHEN** a login request has a `password` of 6 to 20 characters with no uppercase letter
- **THEN** the request is not rejected because of the limits and follows the sign-in flow

#### Scenario: Limit breach is not logged with the submitted values
- **WHEN** a login request is rejected for breaking a limit
- **THEN** the submitted email and password do not appear in the logs

#### Scenario: Surrounding spaces
- **WHEN** a login request has an `email` or a `password` with spaces before or after the value
- **THEN** the server removes those spaces before it checks the limits and before it looks up the account

#### Scenario: Whitespace inside the password
- **WHEN** a login request has a `password` that contains a whitespace character between its characters, after the surrounding spaces are removed
- **THEN** the request is rejected with `{"error": "invalid_password"}`

#### Scenario: Email letter case
- **WHEN** a login request has an `email` with upper-case letters
- **THEN** the server lower-cases the email before it looks up the account
