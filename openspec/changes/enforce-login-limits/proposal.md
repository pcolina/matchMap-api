# Proposal

Closes #7
Closes #18

## Why

The sign-in form validates the email and the password before it sends the request. `CLAUDE.md` section 2 requires the API to validate every request, whatever the client did, so `POST /api/auth/login` must enforce the same limits. Without this, any other client can bypass them.

## What Changes

- `contracts/openapi/auth.yaml`: `LoginRequest` declares the limits. `email` has `format: email`, `minLength: 1` and `maxLength: 254`. `password` has `minLength: 6` and `maxLength: 20`. These lines are already edited in the working tree and are committed with this change.
- `POST /api/auth/login` checks `email` and `password` against those limits before it looks up the account. A request that breaks a limit is rejected with an error response whose status and body are defined in the spec.
- A request within the limits follows the existing credentials flow of the `authentication` spec. The complexity rules of registration (uppercase, lowercase, special character) do not apply to login.
- The submitted email and password are not written to the logs when a limit is broken.
- `POST /api/auth/login` trims the email and the password, lower-cases the email, and rejects a password that contains whitespace between its characters.
- Registration password policy (`user-accounts`): the maximum goes from 128 to 20 characters, so that every registered password can be used to sign in. The minimum stays at 12. A password with any whitespace character is rejected. The registration error is the existing `VALIDATION_FAILED` with `extensions.field` `password`; no new error key is added for registration.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `authentication`: adds the input limits for password sign-in.
- `user-accounts`: changes the password policy of registration (maximum 20 characters, no whitespace).

## Impact

- `contracts/openapi/auth.yaml`: `LoginRequest` (listed above).
- The API code for `POST /api/auth/login`.
- The API code for the registration password policy (`RegistrationService`).
- Existing tests that use the password `correct horse battery` to sign in or register, which the new limits reject. They use a password of 12 to 20 characters without whitespace instead.
- No database schema change.
- Clients: the generated client types must match the contract (`pnpm codegen` in the client repository, which is not part of this change).

## Out of scope

- The sign-in page and its validation, which belong to `add-login-page` in the client repository.
- The complexity rules for passwords, which are in issue 005 and apply to registration.
- Trimming and lower-casing of the email on the server, until the open question in the spec is answered.
- The complexity rules of registration, which are in issue 005.

## Depends on

- `authentication` spec, requirements "Sign in with email and password" and "Sign-in failures do not reveal which accounts exist".
- `user-accounts` spec, requirement "Password policy" (changed by this change).
- Issue #7 "Enforce email and password limits on login (backend)".
