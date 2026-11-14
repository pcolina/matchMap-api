# Design

## Context

`POST /api/auth/login` trims and lower-cases the email in `AuthController`, then runs `validateLoginLimits`. That method validates a `LoginRequest`-shaped record (`LoginLimits`) with Bean Validation. When the `email` field has any violation, it throws `AuthException.invalidEmail()`, which returns `invalid_email`. So an empty email, a malformed email and a too-long email all share one code today.

The `Error` schema in `contracts/openapi/auth.yaml` holds one `error` enum shared by all auth operations. The login 400 response lists `invalid_email` and `invalid_password` as its input-limit codes. See proposal.md for the motivation.

Baseline for contract changes: `4002597` (main after PR #28). Contract changes since the baseline are checked with `git log 4002597..HEAD -- contracts/`.

## Goals / Non-Goals

**Goals:**
- Return `email_too_long` (HTTP 400) when the trimmed, lower-cased email is longer than 254 characters, whatever its format.
- Keep `invalid_email` for an empty email and for an email of 1 to 254 characters that is not in standard format.
- Declare `email_too_long` in the contract before the code returns it.

**Non-Goals:**
- No change to `invalid_password`, to the password limits or to registration (`RegistrationService`, GraphQL `register`).
- No change to the `LoginLimits` record's constraints. The `@Size(max = 254)` stays, so the Bean Validation result still says `email` is invalid for a too-long value.
- No change to the throttling, the sign-in flow or the error body beyond the code.

## Decisions

**Length is checked before Bean Validation in `validateLoginLimits`.** The email is already trimmed and lower-cased in the controller, so the length check runs on the same value the rest of login uses, and the spec's "spaces do not count" scenario holds. If the email is non-null and its length is greater than 254, the method throws `AuthException.emailTooLong()`. Otherwise it keeps the current Bean Validation check, which throws `invalidEmail()`. The check runs first so that a too-long email returns `email_too_long` whatever its format, as decided in the proposal review.

**New factory `AuthException.emailTooLong()`** next to `invalidEmail()`, with status 400 and code `email_too_long`, matching the other input-limit codes.

**Contract first.** `contracts/openapi/auth.yaml` gets `email_too_long` in the `Error` enum, and the login 400 description and its `invalid_email` mention are updated to say which code is returned for which case. The implementation follows the contract.

**Alternative considered: a second Bean Validation constraint on `LoginLimits` that reports a separate field.** Rejected. Both the `@Size` and the `@Email` violations are reported on the same `email` field, so the controller cannot tell them apart without extra mapping. A plain length check on the trimmed value the controller already has is simpler.

## Risks / Trade-offs

- A null email (missing from the JSON body) is not longer than 254 characters, so it still goes to Bean Validation and returns `invalid_email`. This keeps the current behaviour for missing email.
- The contract's shared `Error` enum gets one more value. Clients that switch over the enum must handle `email_too_long`. The matchMap client change `add-login-input-errors` does that.
- The existing test `rejectsEmailTooLong` in `src/test/java/com/matchmap/api/auth/LoginIT.java` sends a 255-character email and expects `invalid_email`. It must change to expect `email_too_long`. The tests `rejectsEmptyEmail` and `rejectsEmailNotInStandardFormat` stay as they are.
