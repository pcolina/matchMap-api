# Proposal

Closes #8

## Why

The web client must not be able to read the refresh token, because scripts running in the page could steal it. Today `TokenResponse` returns the refresh token in the body, so the web app would have to keep it in `localStorage`. The web client needs the refresh token in an `HttpOnly` cookie set by the API. Mobile keeps receiving it in the body, because it stores it in `expo-secure-store`.

Issue #8 is P1: it blocks task 4.7 (session restore) of `add-login-page` in the client repository.

## What Changes

- Every sign-in, refresh and sign-out request declares its client with the header `X-Client`: `web` or `mobile`. A request without the header, or with any other value, is rejected with HTTP 400.
- For `X-Client: web`, `POST /api/auth/login` and `POST /api/auth/google` set the refresh token in the cookie `refresh_token` and return the access token in the body. The refresh token does not appear in the body.
- For `X-Client: web`, `POST /api/auth/refresh` reads the refresh token from the cookie. The request has no body. A new refresh token replaces the cookie.
- For `X-Client: web`, `POST /api/auth/logout` reads the refresh token from the cookie, revokes the session and clears the cookie.
- For `X-Client: mobile`, the three endpoints keep the current behaviour: the refresh token travels in the body.
- The cookie has `HttpOnly`, `Secure`, path `/api/auth` and `Max-Age` of 30 days (the refresh token lifetime in the `authentication` spec). Its `SameSite` value and the allowed web origin for CORS come from environment variables, with local defaults, so they can change with the deployment without a code change.
- Contract: `contracts/openapi/auth.yaml` declares the `X-Client` header, makes `RefreshTokenRequest.refreshToken` optional, and documents the `Set-Cookie` response for web.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `authentication`: token delivery depends on the `X-Client` header (cookie for web, body for mobile).

## Impact

- `contracts/openapi/auth.yaml`: the changes listed above.
- The API code for `login`, `google`, `refresh` and `logout`, and the CORS configuration for the web origin.
- New environment variables, documented in `infra/docker/.env.example`.
- No database schema change. No new dependency expected.
- Clients: `pnpm codegen` in the client repository after the contract change. Not part of this change.

## Out of scope

- The web client code that reads or sends the cookie, which belongs to `add-login-page` in the client repository.
- Mobile token storage, which is unchanged.
- Changes to token lifetimes or to refresh token rotation.
- Any change to GraphQL operations.
- Production or deployment settings. The MVP runs locally only, and the values come from environment variables so they can change later.

## Open questions

- Decided: a missing or unknown `X-Client` returns HTTP 400 with body `{"error": "invalid_client"}`.
- Decided for local development: the web origin is `http://localhost:4300`. It is the default of `MATCHMAP_CORS_ALLOWED_ORIGINS` and may change when the web app is deployed.

## Depends on

- `authentication` spec, requirements about sign-in, Google sign-in, refresh, sign-out and token lifetimes.
- Issue #8 "Set the refresh token as an httpOnly cookie for web (backend)".
- Issue #8 names `add-user-accounts` as the owning change. That change is archived (`openspec/changes/archive/2026-11-03-add-user-accounts`), so this change is proposed as its own change, as `CLAUDE.md` section 9 requires.
