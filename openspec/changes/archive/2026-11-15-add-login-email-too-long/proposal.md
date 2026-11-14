# Proposal

Closes #27

## Why

Login returns `invalid_email` for every email failure: an empty email, a malformed email and an email longer than 254 characters. The client cannot show the length message next to the email field, because it cannot tell the too-long case apart from the others. The client change `add-login-input-errors` in `matchMap` needs its own error code for this case.

## What Changes

- The login contract in `contracts/openapi/auth.yaml` adds `email_too_long` to the `error` enum and describes it in the 400 response: the email is longer than 254 characters after trimming and lower-casing.
- `invalid_email` stays for an empty email and for an email that is not in standard format, when it is not longer than 254 characters.
- `POST /api/auth/login` returns HTTP 400 with `{"error": "email_too_long"}` when the trimmed, lower-cased email is longer than 254 characters. This check runs before the format check, so a too-long email returns `email_too_long` whatever its format.
- Login behaviour for every other input is unchanged.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `authentication`: the requirement "Sign-in input limits" changes. A too-long email is rejected with `email_too_long` instead of `invalid_email`.

## Impact

- `contracts/openapi/auth.yaml`: new enum value `email_too_long` and a description in the login 400 response.
- Backend: the login input check in `src/main/java/com/matchmap/api/auth/AuthController.java` and the error factories in `AuthException.java`. The `LoginLimits` constraints and the registration rules are not changed.
- Tests: login tests for a 255-character email (400 with `email_too_long`) and for an empty or malformed email (400 with `invalid_email`).
- Clients: the web and mobile clients map `email_too_long` to `login.signIn.errorEmailTooLong`, in the separate client change `add-login-input-errors` in the `matchMap` repository. No change in this repository.

## Out of scope

- Registration errors. The register flow keeps its own email rules.
- The password rules and `invalid_password`.
- Any change to the client repository.

## Depends on

- `authentication` (spec "Sign-in input limits").
