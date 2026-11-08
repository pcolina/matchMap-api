## MODIFIED Requirements

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

### Requirement: Sign in with Google
`POST /api/auth/google` SHALL be public and accept `{"idToken": "<Google ID token>"}` and an `X-Client` header with the value `web` or `mobile`. The system SHALL accept the token only if its signature verifies against Google's published keys, its issuer is Google, its audience is one of the configured Google client IDs and it has not expired. On success it SHALL return the same token response as password sign-in for the same `X-Client` value. An invalid token SHALL return HTTP 401 with body `{"error": "invalid_google_token"}`. When Google's published keys cannot be fetched, the system SHALL retry the fetch up to two more times with exponential backoff starting at 500 ms; if it still fails, the response SHALL be HTTP 503 with body `{"error": "google_unavailable"}`.

#### Scenario: First Google sign-in creates an account
- **WHEN** a client posts a valid Google ID token for subject `g-123`, verified email `carla@gmail.com` and name `Carla`, and no account has that email or subject
- **THEN** the response is HTTP 200 with tokens
- **AND** an account exists with email `carla@gmail.com`, display name `Carla`, no password and a generated username that follows the username rules

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

## ADDED Requirements

### Requirement: Client type header
Every sign-in, refresh and sign-out operation under `/api/auth` SHALL require the header `X-Client` with the value `web` or `mobile`. Any other value, or a missing header, SHALL return HTTP 400 with body `{"error": "invalid_client"}` and SHALL NOT perform the operation.

#### Scenario: Unknown X-Client value
- **WHEN** a client calls `/api/auth/refresh` with `X-Client: desktop`
- **THEN** the response is HTTP 400 with body `{"error": "invalid_client"}` and the refresh token is not exchanged

#### Scenario: Missing X-Client on refresh
- **WHEN** a client calls `/api/auth/refresh` without the `X-Client` header
- **THEN** the response is HTTP 400 with body `{"error": "invalid_client"}`
