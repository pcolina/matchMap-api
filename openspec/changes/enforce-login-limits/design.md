# Design

## Context

`CLAUDE.md` section 2 requires the API to validate every request, and the contract in `contracts/` is the source for the API shape. The limits are already written in `contracts/openapi/auth.yaml` in the working tree, and issue #7 says they must be enforced on login.

## Decisions

### D1: The contract is the source of the limits
The limits are copied from `LoginRequest` into the spec, and the code follows the contract. The contract change is committed with this change, which keeps the schema-first rule.

### D2: Limits are checked before the account lookup
The check runs before `POST /api/auth/login` looks up an account, so that a rejected request never reaches the database or the login throttle. Alternative considered: checking after the lookup, rejected because it does work for values that are invalid anyway.

### D3: The error codes of a limit breach are one per field
A request whose `email` breaks a limit is rejected with `error: invalid_email`. A request whose `password` breaks a limit is rejected with `error: invalid_password`. Both codes are returned with HTTP 400 Bad Request, and both are added to the `Error` schema and to the `400` response of `POST /api/auth/login` in `contracts/openapi/auth.yaml`. The frontend maps them to i18n keys (frontend issue pcolina/MatchMap#1). `invalid_credentials` is unchanged.

### D4: Registration uses the same password maximum as sign-in
The registration password policy changes from 12–128 characters to 12–20 characters, and it rejects any whitespace character. Sign-in allows 6–20, so every password that registration accepts can be used to sign in. Registration keeps its own minimum of 12. The registration error stays the existing GraphQL `VALIDATION_FAILED` with `extensions.field` `password`, because that is the mechanism `user-accounts` already defines.

### D5: Existing test passwords change
The password `correct horse battery` has 21 characters and contains spaces, so the new limits reject it. The tests that use it are changed to a password of 12 to 20 characters without whitespace. The change replaces the value and does not remove or skip any test. `RegisterIT.rejectsTooLongPassword` uses 129 characters and moves to 21, the new maximum.

## Risks

- A client that sends values outside the limits gets a new error response. Clients must handle it once the code is defined.
- Logging: a rejected request must not write the submitted values to the logs. Covered by the scenario "Limit breach is not logged with the submitted values".
