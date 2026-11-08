## Why

`MATCHMAP_REFRESH_COOKIE_SAMESITE` accepts any text. A typo such as `Lxa` is written into the `refresh_token` cookie, and the browser ignores or rejects it without any error from the API. The failure shows up later, as a web client that cannot refresh its session. Closes #21.

## What Changes

- The API accepts `MATCHMAP_REFRESH_COOKIE_SAMESITE` only as `Strict`, `Lax` or `None`, and refuses to start with any other value. The default stays `Lax`.
- The API validates the value when it starts, not on each sign-in, so a wrong value is seen at deployment.
- No change to the cookie attributes, the cookie name, `Secure`, `HttpOnly`, the path or `Max-Age`.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `authentication`: adds one requirement, "Refresh cookie SameSite setting", which defines the accepted values and the startup refusal. The existing requirements do not change.

## Impact

- `src/main/java/com/matchmap/api/auth/`: a configuration record for the refresh cookie that validates the value, used by `AuthController` in place of the plain `@Value` string.
- `src/main/resources/application.yml`: unchanged, the default `Lax` already exists.
- `infra/docker/.env.example` and `docker-compose.yml`: unchanged, they already pass the variable.
- No new dependencies and no database or contract changes.

## Out of Scope

- Any other cookie attribute, including `Secure`, `HttpOnly`, the path and `Max-Age`.
- Validation of the CORS origin list (`MATCHMAP_CORS_ALLOWED_ORIGINS`).
- Changing the default value.
- Deployment configuration beyond the local environment.

## Depends On

- `authentication` spec: the refresh cookie attributes and the sign-in and refresh behaviour that the cookie serves.
- Change `set-web-refresh-cookie`, archived in `openspec/changes/archive/2026-11-08-set-web-refresh-cookie/`, which introduced the variable.

## Decisions Taken

- The match is exact and case-sensitive. `none` and `LAX` are refused, even though browsers accept them.
- An empty value is refused like any other value. It does not fall back to `Lax`; `Lax` applies only when the variable is unset.
