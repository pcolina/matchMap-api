# ADR 0002: Token-based authentication with JWT access tokens and rotating refresh tokens

- Status: Proposed
- Date: 2026-10-24
- Change: `add-user-accounts`

## Context

The API serves a web client and a mobile client. Both need users to sign in with a password or with Google and to stay signed in. The MVP runs as a single API instance, locally.

## Decision

Authenticate requests with bearer tokens instead of server-side sessions.

- **Access token:** a JWT signed by the API with HS256 and a secret of at least 256 bits from `MATCHMAP_JWT_SECRET`. Claims: `iss` `matchmap-api`, `sub` the account id, `iat`, `exp`, `jti`. It expires after 15 minutes and is sent as `Authorization: Bearer <token>`.
- **Refresh token:** 32 random bytes, base64url encoded. Only its SHA-256 hash is stored. It expires after 30 days.
- **Rotation:** every refresh returns a new refresh token and invalidates the one presented. All refresh tokens issued from one sign-in share a session id.
- **Reuse detection:** presenting a refresh token that was already exchanged revokes every refresh token of its session.
- **Sign-out:** revokes the session's refresh tokens. Access tokens already issued stay valid until they expire.
- **Google sign-in:** the client obtains a Google ID token and sends it to the API, which verifies it against Google's published keys and then issues its own tokens. No Google client library is used.

## Alternatives considered

- **Server-side session with a cookie.** Simple for the web client, but awkward on mobile, and cookies require CSRF protection.
- **RS256 with a key pair.** Lets other services verify tokens without being able to issue them. No other service needs that today; HS256 keeps key management to one secret.
- **Long-lived access tokens without refresh tokens.** Fewer moving parts, but a stolen or signed-out token stays valid for a long time.
- **Server-side OAuth2 redirect flow for Google.** Natural on the web, but complex on mobile; verifying the ID token works the same on both.

## Consequences

- Web and mobile use exactly the same flow.
- The API stays stateless for access tokens; only refresh tokens touch the database.
- A database copy cannot be used to refresh sessions, because only hashes are stored.
- Signing out does not invalidate access tokens immediately; the 15-minute lifetime bounds that window.
- Rotating `MATCHMAP_JWT_SECRET` invalidates every access token; refresh tokens keep working and issue new access tokens.
