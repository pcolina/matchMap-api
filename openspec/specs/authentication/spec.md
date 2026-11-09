# authentication Specification

## Purpose

Lets people sign in with a password or with Google, keeps them signed in on web and mobile with access and refresh tokens, and ensures only signed-in users reach protected operations.

## Requirements

### Requirement: Sign in with email and password
`POST /api/auth/login` SHALL be public and accept a JSON body with `email` and `password`, and an `X-Client` header with the value `web` or `mobile`. The email SHALL be compared after trimming and lowercasing. On success for `X-Client: mobile` it SHALL return HTTP 200 with `tokenType` `Bearer`, `accessToken`, `expiresIn` (seconds until the access token expires) and `refreshToken` in the body. On success for `X-Client: web` it SHALL return HTTP 200 with `tokenType` `Bearer`, `accessToken` and `expiresIn` in the body, and SHALL set the refresh token in the cookie `refresh_token`, which SHALL be `HttpOnly`, `Secure`, have path `/api/auth` and `Max-Age` 2592000. The refresh token SHALL NOT appear in the body of a web response. A request without `X-Client`, or with any other value, SHALL be rejected with HTTP 400 and SHALL NOT sign in.

#### Scenario: Correct credentials from mobile
- **WHEN** an account `ana@example.com` exists with password `correct horse battery` and a mobile client posts `{"email": "Ana@Example.com", "password": "correct horse battery"}` to `/api/auth/login` with `X-Client: mobile`
- **THEN** the response is HTTP 200 with `tokenType` `Bearer`, a non-empty `accessToken`, a positive `expiresIn` and a non-empty `refreshToken` in the body

#### Scenario: Correct credentials from web
- **WHEN** an account `ana@example.com` exists with password `correct horse battery` and a web client posts `{"email": "Ana@Example.com", "password": "correct horse battery"}` to `/api/auth/login` with `X-Client: web`
- **THEN** the response is HTTP 200 with `tokenType` `Bearer`, a non-empty `accessToken` and a positive `expiresIn` in the body
- **AND** the response sets the cookie `refresh_token` with `HttpOnly`, `Secure`, path `/api/auth` and `Max-Age` 2592000
- **AND** the body does not contain a `refreshToken`

#### Scenario: Missing X-Client header
- **WHEN** a client posts correct credentials to `/api/auth/login` without the `X-Client` header
- **THEN** the response is HTTP 400 and no tokens are issued

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
`POST /api/auth/refresh` SHALL be public and accept an `X-Client` header with the value `web` or `mobile`. For `X-Client: mobile` it SHALL accept `{"refreshToken": "..."}` in the body. For `X-Client: web` it SHALL read the refresh token from the cookie `refresh_token` and SHALL ignore any body. A valid refresh token SHALL be exchanged for a new access token and a new refresh token, and the presented refresh token SHALL stop being valid. For mobile the new refresh token SHALL be returned in the body. For web it SHALL be set in the cookie `refresh_token` with the attributes defined for sign-in, and SHALL NOT appear in the body. An unknown, expired or revoked refresh token, or a web request without the cookie, SHALL return HTTP 401 with body `{"error": "invalid_refresh_token"}`.

#### Scenario: Successful refresh from mobile
- **WHEN** a mobile client posts a valid refresh token to `/api/auth/refresh` with `X-Client: mobile`
- **THEN** the response is HTTP 200 with a new `accessToken` and a `refreshToken` in the body different from the one posted

#### Scenario: Successful refresh from web
- **WHEN** a web client sends no body to `/api/auth/refresh` with `X-Client: web` and a valid `refresh_token` cookie
- **THEN** the response is HTTP 200 with a new `accessToken` in the body
- **AND** the response sets a new `refresh_token` cookie different from the one sent
- **AND** the body does not contain a `refreshToken`

#### Scenario: Web refresh without the cookie
- **WHEN** a web client calls `/api/auth/refresh` with `X-Client: web` and no `refresh_token` cookie
- **THEN** the response is HTTP 401 with body `{"error": "invalid_refresh_token"}`

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
`POST /api/auth/logout` SHALL be public and accept an `X-Client` header with the value `web` or `mobile`. For `X-Client: mobile` it SHALL accept `{"refreshToken": "..."}` in the body. For `X-Client: web` it SHALL read the refresh token from the cookie `refresh_token` and SHALL ignore any body, and SHALL clear that cookie in the response. It SHALL revoke every refresh token of that token's session and return HTTP 204, also when the token is unknown, already revoked or missing. Access tokens already issued SHALL stay valid until they expire.

#### Scenario: Sign out ends the session
- **WHEN** a mobile client posts its refresh token to `/api/auth/logout` with `X-Client: mobile` and then posts the same refresh token to `/api/auth/refresh` with `X-Client: mobile`
- **THEN** the logout response is HTTP 204 and the refresh response is HTTP 401

#### Scenario: Web sign out clears the cookie
- **WHEN** a web client calls `/api/auth/logout` with `X-Client: web` and its `refresh_token` cookie
- **THEN** the response is HTTP 204
- **AND** the response sets the cookie `refresh_token` expired, so the browser discards it
- **AND** a later refresh with that cookie returns HTTP 401

#### Scenario: Unknown token
- **WHEN** a client posts an unknown refresh token to `/api/auth/logout` with `X-Client: mobile`
- **THEN** the response is HTTP 204

#### Scenario: Web sign out without the cookie
- **WHEN** a web client calls `/api/auth/logout` with `X-Client: web` and no cookie
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

### Requirement: Client type header
Every sign-in, refresh and sign-out operation under `/api/auth` SHALL require the header `X-Client` with the value `web` or `mobile`. Any other value, or a missing header, SHALL return HTTP 400 with body `{"error": "invalid_client"}` and SHALL NOT perform the operation.

#### Scenario: Unknown X-Client value
- **WHEN** a client calls `/api/auth/refresh` with `X-Client: desktop`
- **THEN** the response is HTTP 400 with body `{"error": "invalid_client"}` and the refresh token is not exchanged

#### Scenario: Missing X-Client on refresh
- **WHEN** a client calls `/api/auth/refresh` without the `X-Client` header
- **THEN** the response is HTTP 400 with body `{"error": "invalid_client"}`

### Requirement: Refresh cookie SameSite setting
The `SameSite` attribute of the cookie `refresh_token` SHALL come from the environment variable `MATCHMAP_REFRESH_COOKIE_SAMESITE` and SHALL be one of `Strict`, `Lax` or `None`. When the variable is not set, the value SHALL be `Lax`. The match SHALL be exact and case-sensitive, so `none` and `LAX` are refused. The API SHALL refuse to start when the variable has any other value, including an empty value, and the startup error SHALL name the variable.

#### Scenario: Configured value is used
- **WHEN** the API starts with `MATCHMAP_REFRESH_COOKIE_SAMESITE=None` and a web client signs in
- **THEN** the cookie `refresh_token` in the response has the attribute `SameSite=None`

#### Scenario: Unset value defaults to Lax
- **WHEN** the API starts without `MATCHMAP_REFRESH_COOKIE_SAMESITE` and a web client signs in
- **THEN** the cookie `refresh_token` in the response has the attribute `SameSite=Lax`

#### Scenario: Unknown value stops startup
- **WHEN** the API starts with `MATCHMAP_REFRESH_COOKIE_SAMESITE=Lxa`
- **THEN** the application context fails to start and the error message contains `MATCHMAP_REFRESH_COOKIE_SAMESITE`

#### Scenario: Lowercase value is refused
- **WHEN** the API starts with `MATCHMAP_REFRESH_COOKIE_SAMESITE=none`
- **THEN** the application context fails to start and the error message contains `MATCHMAP_REFRESH_COOKIE_SAMESITE`

#### Scenario: Empty value is refused
- **WHEN** the API starts with `MATCHMAP_REFRESH_COOKIE_SAMESITE` set to an empty string
- **THEN** the application context fails to start and the error message contains `MATCHMAP_REFRESH_COOKIE_SAMESITE`
