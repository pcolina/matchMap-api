# Design

## Context

Sign-in and registration already return the stable codes that the clients need. The sign-in codes (`invalid_credentials`, `too_many_attempts`) are implemented and declared in `contracts/openapi/auth.yaml`. The registration codes (`EMAIL_TAKEN`, `USERNAME_TAKEN`, `VALIDATION_FAILED`) are implemented in `RegistrationService` and `AccountException`, and specified in the `user-accounts` spec. `contracts/graphql/account.graphqls` does not document them. See proposal.md - Why.

## Goals / Non-Goals

**Goals:**
- Document the registration error codes in the GraphQL contract, where the frontend reads the operation.
- Lock the codes with automated tests, so that a change to a code fails the build.
- Confirm or refute the reported duplicate-username case with a test.

**Non-Goals:**
- No new error code, no new error format, no change to the sign-in behaviour.
- No change to the registration input or the `User` type (issue #13).
- No change to the password rule (issue #10).

## Decisions

- **Document in the GraphQL schema description of `register`.** The contract is schema-first and the frontend codegen reads it. Alternative: a separate markdown file. Rejected, because the codegen does not read it and it could drift from the schema.
- **Test the codes through the GraphQL endpoint, not through the service.** The clients see the `extensions.code` of the response, so the test asserts that. Alternative: unit tests on `AccountException`. Rejected, because they would not catch a broken mapping in the GraphQL layer.
- **Test that the contract text names the codes.** A small test reads `account.graphqls` and checks the codes are present in the `register` description. Alternative: generate the codes from the Java enum. Rejected, because it adds a generation step and no new dependency is needed for a string check.
- **Use the existing Testcontainers setup for the registration tests**, because they reach the database.

## Risks / Trade-offs

- [Duplicate-username race: two requests pass the `existsByUsername` check at the same time and the database constraint fails] → The existing code path at `RegistrationService` lines 58 to 61 maps the constraint failure to `USERNAME_TAKEN` or `EMAIL_TAKEN`. The test covers the sequential case; the race is not tested, and that is accepted for the MVP.
- [The contract text and the code drift apart] → The contract test in the tasks fails when a documented code is missing from the description.

## Migration Plan

None. No schema or data change. Rollback is reverting the commits.

## Open Questions

None. The change is contract and tests only, as confirmed by the user.
