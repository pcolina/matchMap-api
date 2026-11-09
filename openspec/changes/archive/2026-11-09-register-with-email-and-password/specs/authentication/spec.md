# Spec Delta

## MODIFIED Requirements

### Requirement: Sign in with Google
`POST /api/auth/google` SHALL be public and accept `{"idToken": "<Google ID token>"}` and an `X-Client` header with the value `web` or `mobile`. The system SHALL accept the token only if its signature verifies against Google's published keys, its issuer is Google, its audience is one of the configured Google client IDs and it has not expired. On success it SHALL return the same token response as password sign-in for the same `X-Client` value. An invalid token SHALL return HTTP 401 with body `{"error": "invalid_google_token"}`. When Google's published keys cannot be fetched, the system SHALL retry the fetch up to two more times with exponential backoff starting at 500 ms; if it still fails, the response SHALL be HTTP 503 with body `{"error": "google_unavailable"}`. For a new account, the username SHALL be generated from the Google profile name with the same sanitization and suffix rules as registration: the name is lowercased, every character outside `a-z`, `0-9` and `_` is replaced by `_`, the result is cut at 30 characters, and a taken username gets the suffix `_<n>` counting up from 2.

#### Scenario: First Google sign-in creates an account
- **WHEN** a client posts a valid Google ID token for subject `g-123`, verified email `carla@gmail.com` and name `Carla`, and no account has that email or subject
- **THEN** the response is HTTP 200 with tokens
- **AND** an account exists with email `carla@gmail.com`, username `carla`, and no password

#### Scenario: Google name with characters outside the username set
- **WHEN** a client posts a valid Google ID token with name `Ana Travels` for a new account and no account has username `ana_travels`
- **THEN** the account's username is `ana_travels`

#### Scenario: Google name already used as username
- **WHEN** an account with username `carla` exists and a client posts a valid Google ID token with name `Carla` for a new email
- **THEN** the new account's username is `carla_2` (assumption: the suffix counts up from 2)

#### Scenario: Google profile without a name
- **WHEN** a client posts a valid Google ID token for a new account whose name is empty
- **THEN** the username is generated from the part of the email before the `@`, with the same sanitization and suffix rules, and the sign-in succeeds

#### Scenario: Returning Google user
- **WHEN** an account is linked to Google subject `g-123` and a client posts a valid Google ID token for subject `g-123`
- **THEN** the response is HTTP 200 with tokens for that same account

#### Scenario: Google sign-in from web
- **WHEN** a web client posts a valid Google ID token with `X-Client: web`
- **THEN** the response is HTTP 200 with `accessToken` and `expiresIn` in the body, sets the cookie `refresh_token` as defined for password sign-in, and the body does not contain a `refreshToken`

#### Scenario: Token for another audience
- **WHEN** a client posts a Google ID token whose audience is not a configured client ID
- **THEN** the response is HTTP 401 with body `{"error": "invalid_google_token"}`

#### Scenario: Expired token
- **WHEN** a client posts an expired Google ID token
- **THEN** the response is HTTP 401 with body `{"error": "invalid_google_token"}`

#### Scenario: Google keys unavailable
- **WHEN** a client posts a valid Google ID token and Google's published keys cannot be fetched on any of the three attempts
- **THEN** the response is HTTP 503 with body `{"error": "google_unavailable"}`
