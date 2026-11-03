# Proposal

## Why

Every MatchMap feature (pins, friends, map comparison) belongs to a user, but the API has no notion of users yet. People need to create an account, sign in with a password or with their Google account, stay signed in on web and mobile, and see their own account. This must follow standard security practice from the first release, because retrofitting security onto existing data is costly.

## What Changes

- Add user accounts identified by a UUID, with a unique email, a unique username, a display name and an optional password.
- Add registration with email and password through the GraphQL mutation `register`.
- Add reading the signed-in user's own account through the GraphQL query `me`.
- Add password sign-in, Google sign-in, token refresh and sign-out as REST endpoints under `/api/auth/`, defined first in `contracts/openapi/`.
- Google sign-in creates the account on first use, or links Google to an existing account with the same email when Google reports the email as verified.
- Issue a short-lived signed access token and a rotating refresh token; detect reuse of a rotated refresh token and revoke its whole session.
- Protect the API: every operation requires a valid access token unless it is explicitly public (health, `apiVersion`, `register` and the sign-in, refresh and sign-out endpoints).
- Apply standard protections: password hashing, password length policy, throttling of failed sign-ins, generic sign-in errors that do not reveal whether an email exists, and no passwords or tokens in logs.
- Add the persistence layer for the domain (Spring Data JPA) and the first business tables through Flyway.

## Capabilities

### New Capabilities

- `user-accounts`: Creating an account with email and password, the rules for email, username, display name and password, and reading one's own account.
- `authentication`: Signing in with a password or with Google, linking Google to an existing account, access and refresh tokens, refresh rotation and reuse detection, sign-out, throttling and access control on protected operations.

### Modified Capabilities

None. Health, `apiVersion` and request correlation keep their current behaviour and stay public.

## Out of scope

- Updating any account field (display name, username, password, email) and deleting accounts. They come in a later change.
- Profile data such as avatar, country of origin and spoken languages. It will live in its own table that references the account's id, in a later change.
- Email verification and password reset, which need email delivery.
- Searching for or reading other users' accounts. It belongs to the friends feature.
- Sign-in providers other than Google, and multi-factor authentication.
- Roles and administration.
- HTTPS termination, which belongs to a deployment change; the MVP runs locally only.

## Depends on

- `service-health`, `api-version` and `request-correlation` (archived change `setup-walking-skeleton`): they must remain public and unchanged.

## Impact

- New contracts: `contracts/openapi/auth.yaml` and new types in `contracts/graphql/`.
- New Flyway migration for accounts and refresh tokens.
- New dependencies: Spring Security, Spring Security OAuth2 Resource Server (JWT signing and Google ID token verification), Bean Validation, Spring Data JPA, and their test starters.
- New configuration secrets in `infra/docker/.env`: the access token signing key and the Google client IDs.
- Existing tests must keep passing with security enabled.
- Two new ADRs: persistence approach and token-based authentication.
