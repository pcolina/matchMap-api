# Design

## Context

Issue #8 asks that the web client never receives the refresh token in a response body, because scripts in the page could read it. Mobile stores the refresh token in `expo-secure-store` and needs the body. The API must tell the two clients apart. The contract in `contracts/openapi/auth.yaml` is the source of the shape, and `CLAUDE.md` section 2 requires the schema to change first.

## Decisions

### D1: The client type comes from the `X-Client` header
Each sign-in, refresh and sign-out request sends `X-Client: web` or `X-Client: mobile`. The header is explicit, so the API does not infer the client from the user agent or the origin. Alternatives considered: a separate web path (duplicates every endpoint), and a field in the body (refresh has no body on web). Rejected both.

### D2: A missing or unknown `X-Client` is rejected with HTTP 400
The API never guesses the client type. Treating a missing header as mobile would return the refresh token in the body to a web client that forgot the header. Rejecting is the safe default. The body is `{"error": "invalid_client"}`.

### D3: Cookie attributes come from environment variables
The cookie name is `refresh_token`. Its `HttpOnly`, `Secure` and path `/api/auth` are fixed in code. `SameSite` and the allowed web origin for CORS are read from environment variables, `MATCHMAP_REFRESH_COOKIE_SAMESITE` and `MATCHMAP_CORS_ALLOWED_ORIGINS`, with local defaults. This follows the existing `MATCHMAP_JWT_SECRET` pattern, so a later deployment changes configuration and not code. `Secure` is always on: browsers accept `Secure` cookies on `http://localhost`, so local development does not need an exception.

### D4: `Max-Age` equals the refresh token lifetime
`Max-Age` is 2592000 seconds (30 days), the lifetime in the `authentication` spec. The cookie outlives nothing the server would accept.

### D5: The web refresh request ignores any body
For `X-Client: web`, `POST /api/auth/refresh` and `POST /api/auth/logout` read only the cookie. A `refreshToken` in the body is ignored, so the token cannot be supplied from a script that way. For mobile the body is required, as today.

### D6: Logout clears the cookie
For web, `POST /api/auth/logout` revokes the session and sends a `Set-Cookie` that expires `refresh_token`. Without a cookie it still returns 204, as the existing spec requires for unknown tokens.

### D7: Google sign-in follows the same rule as password sign-in
`POST /api/auth/google` is part of this change. It returns the same token response as login, so web must receive the cookie and not the body token.

### D8: No new ADR
The rule that the refresh token is not stored by the web client is a consequence of the existing token decision. If the review considers it an architecture decision that outlives the change, an ADR is added, and this is listed as an open question for the review.

## Risks

- A web client that sends the old body on refresh gets the body ignored and the cookie used. A web client without the cookie gets 401 `invalid_refresh_token`.
- CORS with credentials is needed for the front to send the cookie. A wrong origin in `MATCHMAP_CORS_ALLOWED_ORIGINS` blocks the request in the browser, not in the API, so the failure is hard to see. The CORS test covers it.
- Changing `SameSite` between environments can break the cookie. Its value is covered by the configuration test.
