# Spec Delta

## Purpose

Lets people sign in with a password or with Google, keeps them signed in on web and mobile with access and refresh tokens, and ensures only signed-in users reach protected operations.

## ADDED Requirements

### Requirement: Sign in with email and password
`POST /api/auth/login` SHALL be public and accept a JSON body with `email` and `password`. The email SHALL be compared after trimming and lowercasing. On success it SHALL return HTTP 200 with `tokenType` `Bearer`, `accessToken`, `expiresIn` (seconds until the access token expires) and `refreshToken`.

#### Scenario: Correct credentials
- **WHEN** an account `ana@example.com` exists with password `correct horse battery` and a client posts `{"email": "Ana@Example.com", "password": "correct horse battery"}` to `/api/auth/login`
- **THEN** the response is HTTP 200 with `tokenType` `Bearer`, a non-empty `accessToken`, a positive `expiresIn` and a non-empty `refreshToken`

### Requirement: Sign-in failures do not reveal which accounts exist
A wrong password, an unknown email and an account that has no password (Google only) SHALL all produce the same HTTP 401 response with body `{"error": "invalid_credentials"}`.

#### Scenario: Wrong password
- **WHEN** a client posts the correct email of an existing account with a wrong password
- **THEN** the response is HTTP 401 with body `{"error": "invalid_credentials"}`

#### Scenario: Unknown email
- **WHEN** a client posts an email that belongs to no account
- **THEN** the response is HTTP 401 with body `{"error": "invalid_credentials"}`

#### Scenario: Account without password
- **WHEN** a client posts the email of an account created through Google that has no password
- **THEN** the response is HTTP 401 with body `{"error": "invalid_credentials"}`

### Requirement: Throttle failed sign-ins
After 5 failed password sign-ins for the same email from the same client IP within 15 minutes, further attempts for that pair SHALL be rejected until the window ends. A throttled attempt SHALL return HTTP 429 with body `{"error": "too_many_attempts"}` and a `Retry-After` header, even if the password is correct. A successful sign-in SHALL reset the count for that pair.

#### Scenario: Too many failed attempts
- **WHEN** a client posts 5 wrong passwords for `ana@example.com` within 15 minutes and then posts the correct password
- **THEN** the response is HTTP 429 with body `{"error": "too_many_attempts"}` and a `Retry-After` header

#### Scenario: Other emails are not affected
- **WHEN** a client is throttled for `ana@example.com` and posts correct credentials for `bob@example.com`
- **THEN** the response is HTTP 200

### Requirement: Sign in with Google
`POST /api/auth/google` SHALL be public and accept `{"idToken": "<Google ID token>"}`. The system SHALL accept the token only if its signature verifies against Google's published keys, its issuer is Google, its audience is one of the configured Google client IDs and it has not expired. On success it SHALL return the same token response as password sign-in. An invalid token SHALL return HTTP 401 with body `{"error": "invalid_google_token"}`.

#### Scenario: First Google sign-in creates an account
- **WHEN** a client posts a valid Google ID token for subject `g-123`, verified email `carla@gmail.com` and name `Carla`, and no account has that email or subject
- **THEN** the response is HTTP 200 with tokens
- **AND** an account exists with email `carla@gmail.com`, display name `Carla`, no password and a generated username that follows the username rules

#### Scenario: Returning Google user
- **WHEN** an account is linked to Google subject `g-123` and a client posts a valid Google ID token for subject `g-123`
- **THEN** the response is HTTP 200 with tokens for that same account

#### Scenario: Token for another audience
- **WHEN** a client posts a Google ID token whose audience is not a configured client ID
- **THEN** the response is HTTP 401 with body `{"error": "invalid_google_token"}`

#### Scenario: Expired token
- **WHEN** a client posts an expired Google ID token
- **THEN** the response is HTTP 401 with body `{"error": "invalid_google_token"}`

### Requirement: Link Google to an existing account by verified email
When a valid Google ID token has no linked account but its email matches an existing account and Google marks the email as verified, the system SHALL link the Google subject to that account and sign it in. When Google marks the email as not verified, the system SHALL return HTTP 403 with body `{"error": "google_email_not_verified"}` and SHALL NOT create or link any account.

#### Scenario: Linking a password account
- **WHEN** an account `ana@example.com` with a password exists and a client posts a valid Google ID token with verified email `ana@example.com`
- **THEN** the response is HTTP 200 with tokens for the existing account
- **AND** the account can still sign in with its password

#### Scenario: Unverified Google email
- **WHEN** a client posts a valid Google ID token whose email is marked as not verified
- **THEN** the response is HTTP 403 with body `{"error": "google_email_not_verified"}`

### Requirement: Token lifetimes
Access tokens SHALL be signed by the API, SHALL identify the account by its id and SHALL expire. Access tokens SHALL expire 15 minutes after issue and refresh tokens 30 days after issue. `expiresIn` in the sign-in response SHALL equal the access token lifetime in seconds.

#### Scenario: expiresIn matches the access token lifetime
- **WHEN** a client signs in with a password
- **THEN** `expiresIn` is `900`
- **AND** the access token's expiry is that many seconds after its issue time

### Requirement: Protected operations require a valid access token
A protected operation SHALL require `Authorization: Bearer <accessToken>`. A request that carries a malformed, tampered or expired access token SHALL get HTTP 401 with a `WWW-Authenticate: Bearer` header on any endpoint, public or not. A protected GraphQL operation called without any token SHALL fail with `extensions.classification` `UNAUTHORIZED`.

#### Scenario: Valid token
- **WHEN** a client calls `{ me { id } }` with an access token obtained from sign-in
- **THEN** the response contains the account id

#### Scenario: Tampered token
- **WHEN** a client calls `{ me { id } }` with an access token whose payload was modified after signing
- **THEN** the response is HTTP 401 with a `WWW-Authenticate` header that starts with `Bearer`

#### Scenario: Expired token
- **WHEN** a client calls `{ me { id } }` with an access token whose expiry is in the past
- **THEN** the response is HTTP 401 with a `WWW-Authenticate` header that starts with `Bearer`

### Requirement: Refresh with rotation
`POST /api/auth/refresh` SHALL be public and accept `{"refreshToken": "..."}`. A valid refresh token SHALL be exchanged for a new access token and a new refresh token, and the presented refresh token SHALL stop being valid. An unknown, expired or revoked refresh token SHALL return HTTP 401 with body `{"error": "invalid_refresh_token"}`.

#### Scenario: Successful refresh
- **WHEN** a client posts a valid refresh token to `/api/auth/refresh`
- **THEN** the response is HTTP 200 with a new `accessToken` and a `refreshToken` different from the one posted

#### Scenario: Expired refresh token
- **WHEN** a client posts a refresh token whose expiry is in the past
- **THEN** the response is HTTP 401 with body `{"error": "invalid_refresh_token"}`

### Requirement: Detect refresh token reuse
Every refresh token SHALL belong to a session started by one sign-in. When a refresh token that was already exchanged is presented again, the system SHALL return HTTP 401 with body `{"error": "invalid_refresh_token"}` and SHALL revoke every refresh token of that session.

#### Scenario: Stolen token is replayed
- **WHEN** refresh token A was exchanged for refresh token B and a client posts A again
- **THEN** the response is HTTP 401 with body `{"error": "invalid_refresh_token"}`
- **AND** a later request that posts B also returns HTTP 401

### Requirement: Sign out
`POST /api/auth/logout` SHALL be public and accept `{"refreshToken": "..."}`. It SHALL revoke every refresh token of that token's session and return HTTP 204, also when the token is unknown or already revoked. Access tokens already issued SHALL stay valid until they expire.

#### Scenario: Sign out ends the session
- **WHEN** a client posts its refresh token to `/api/auth/logout` and then posts the same refresh token to `/api/auth/refresh`
- **THEN** the logout response is HTTP 204 and the refresh response is HTTP 401

#### Scenario: Unknown token
- **WHEN** a client posts an unknown refresh token to `/api/auth/logout`
- **THEN** the response is HTTP 204

### Requirement: Only explicit operations are public
Only these operations SHALL be reachable without an access token: `GET /actuator/health`, `POST /api/auth/login`, `POST /api/auth/google`, `POST /api/auth/refresh`, `POST /api/auth/logout`, the GraphQL operations `register` and `apiVersion`, and GraphiQL when it is enabled. Any other HTTP path SHALL return HTTP 401 without an access token.

#### Scenario: Public operations stay public
- **WHEN** an anonymous client calls `GET /actuator/health` and sends `{ apiVersion }` to `/graphql`
- **THEN** both succeed without an access token

#### Scenario: Unknown path requires a token
- **WHEN** an anonymous client calls `GET /api/anything`
- **THEN** the response is HTTP 401

### Requirement: Credentials never leak
Passwords, access tokens, refresh tokens and Google ID tokens SHALL NOT appear in logs or error responses. Refresh tokens SHALL be stored only as hashes, so a database copy cannot be used to refresh.

#### Scenario: Sign-in leaves no secrets in the logs
- **WHEN** a client signs in with a password and then refreshes its tokens
- **THEN** no log line contains the password, the access token or either refresh token

#### Scenario: Stored refresh tokens are hashes
- **WHEN** a client signs in and receives a refresh token
- **THEN** the database does not contain that refresh token value
