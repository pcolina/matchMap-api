# Proposal

Issue: #9 "Return stable error codes for sign-in and registration (backend)" (label `blocks-frontend`).

## Why

The frontend shows a translated message for each sign-in and registration failure, and maps the error code returned by the API to that message. The codes must be stable and documented in the contract so the clients can rely on them.

Reading the current code and specs shows that most of the codes already exist:

- Sign-in: `invalid_credentials` (401) and `too_many_attempts` (429) are implemented and declared in `contracts/openapi/auth.yaml`. The `authentication` spec already requires that a wrong password, an unknown email and an account without password return the same `invalid_credentials`. The open question in issue #9 about this is therefore already answered by the existing spec.
- Registration: `EMAIL_TAKEN`, `USERNAME_TAKEN` and `VALIDATION_FAILED` (with `extensions.field`) are implemented in `RegistrationService` and `AccountException`, and required by the `user-accounts` spec. They are not documented in `contracts/graphql/account.graphqls`, which is what the issue asks for.
- Issue #9 says a duplicate username currently returns `email taken`. Reading `RegistrationService` (line 44) shows that a duplicate username returns `USERNAME_TAKEN`. This claim is not confirmed. It must be reproduced by a test before the change is accepted, and it may be stale.

So the gap this change closes is the contract documentation and the tests that lock the codes, not the codes themselves.

## What Changes

- Document in `contracts/graphql/account.graphqls` the error codes that `register` can return: `EMAIL_TAKEN`, `USERNAME_TAKEN` and `VALIDATION_FAILED` with `extensions.field`.
- Add tests that fail if a duplicate username does not return `USERNAME_TAKEN`, and that fail if any documented code changes.
- Keep `contracts/openapi/auth.yaml` as the source of the sign-in codes. Add no new sign-in code unless a test shows that one is missing.
- No new error code and no change to the error format. The format already in use is kept: `extensions.code` for GraphQL, `error` for REST.
- No change to `RegisterInput` or `User` fields. Those belong to issue #13.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `user-accounts`: adds a requirement that the `register` error codes are documented in the GraphQL contract. Confirmed by the user: the change is contract and tests only. No behaviour changes, and the duplicate-username scenario only locks existing behaviour.

## Out of scope

- Password complexity rule and its `VALIDATION_FAILED` code, issue #10. The current `user-accounts` spec (12 to 20 characters, no whitespace) conflicts with the rule in issue #10 (6 to 20 characters, lowercase, uppercase and special character). That conflict is for #10 to resolve.
- Removing `username` and `displayName` from `RegisterInput` and `User`, issue #13.
- Sign-in behaviour: no change to `invalid_credentials`, `too_many_attempts` or throttling. These are already specified and implemented.
- Google sign-in error codes.
- Mobile-specific behaviour.
- Changes to the frontend or to the client codegen.

## Depends on

- `user-accounts` (register and its error codes).
- `authentication` (sign-in error codes and throttling).
- Related, not blocking: issue #10 (password rule) and issue #13 (field removal), which depend on this change according to issue #13.

## Impact

- `contracts/graphql/account.graphqls`: documents the error codes for `register`.
- Tests in `src/test` for registration errors (duplicate username, duplicate email, validation). Testcontainers is needed only if the test reaches the database.
- No database migration. No new dependency.
- Frontend: unblocks the sign-in and registration error messages once the contract is merged and the client codegen is updated.
