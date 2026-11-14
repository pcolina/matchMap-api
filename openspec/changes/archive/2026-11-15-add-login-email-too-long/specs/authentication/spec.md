## MODIFIED Requirements

### Requirement: Sign-in input limits
`POST /api/auth/login` SHALL check `email` and `password` against the limits of `LoginRequest` in `contracts/openapi/auth.yaml` before it looks up any account. The `email` SHALL be 1 to 254 characters and SHALL be in standard email format. The `password` SHALL be 6 to 20 characters. A request whose `email` is longer than 254 characters SHALL be rejected with HTTP 400 and the body `{"error": "email_too_long"}`, whatever the format of the `email`. A request whose `email` is empty or not in standard email format SHALL be rejected with HTTP 400 and the body `{"error": "invalid_email"}`. A request whose `password` breaks a limit SHALL be rejected with HTTP 400 and the body `{"error": "invalid_password"}`. The body SHALL NOT reveal whether an account exists. The complexity rules of registration SHALL NOT apply to login.

#### Scenario: Empty email
- **WHEN** a login request has an empty `email`
- **THEN** the request is rejected with `{"error": "invalid_email"}`

#### Scenario: Email too long
- **WHEN** a login request has an `email` of 255 characters in standard email format
- **THEN** the request is rejected with HTTP 400 and `{"error": "email_too_long"}`

#### Scenario: Email too long and not in standard format
- **WHEN** a login request has an `email` of 255 characters that is not in standard email format
- **THEN** the request is rejected with HTTP 400 and `{"error": "email_too_long"}`

#### Scenario: Email not in standard format
- **WHEN** a login request has an `email` of 1 to 254 characters that is not in standard email format
- **THEN** the request is rejected with HTTP 400 and `{"error": "invalid_email"}`

#### Scenario: Password too short
- **WHEN** a login request has a `password` of 5 characters
- **THEN** the request is rejected with HTTP 400 and `{"error": "invalid_password"}`

#### Scenario: Password too long
- **WHEN** a login request has a `password` of 21 characters
- **THEN** the request is rejected with HTTP 400 and `{"error": "invalid_password"}`

#### Scenario: Password without uppercase is not rejected for that reason
- **WHEN** a login request has a `password` of 6 to 20 characters with no uppercase letter
- **THEN** the request is not rejected because of the limits and follows the sign-in flow

#### Scenario: Limit breach is not logged with the submitted values
- **WHEN** a login request is rejected for breaking a limit
- **THEN** the submitted email and password do not appear in the logs

#### Scenario: Surrounding spaces
- **WHEN** a login request has an `email` or a `password` with spaces before or after the value
- **THEN** the server removes those spaces before it checks the limits and before it looks up the account

#### Scenario: Surrounding spaces do not count toward the email length
- **WHEN** a login request has an `email` of 250 characters in standard email format with 5 spaces before and 5 spaces after the value
- **THEN** the server removes the spaces, the email is 250 characters and the request is not rejected with `email_too_long`

#### Scenario: Whitespace inside the password
- **WHEN** a login request has a `password` that contains a whitespace character between its characters, after the surrounding spaces are removed
- **THEN** the request is rejected with `{"error": "invalid_password"}`

#### Scenario: Email letter case
- **WHEN** a login request has an `email` with upper-case letters
- **THEN** the server lower-cases the email before it looks up the account
