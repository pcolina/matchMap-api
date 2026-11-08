# Design

## Context

Registration validates the password with Bean Validation on the `password` parameter of `RegistrationService` (`@Size(min = 12, max = 20)` and `@Pattern(regexp = "\\S*")`). A violation maps to `VALIDATION_FAILED` with `extensions.field` `password`. Sign-in uses its own limits (6 to 20) and is not affected. See proposal.md - Why and specs/user-accounts/spec.md.

## Goals / Non-Goals

**Goals:**
- Enforce the agreed password rule in the API, so that every client check has a matching server check.
- Document the rule in the GraphQL contract, so that the clients can copy it.

**Non-Goals:**
- No change to hashing or storage, and no change to sign-in.
- No new error code. `VALIDATION_FAILED` with `extensions.field` `password` is kept.

## Decisions

- **Keep Bean Validation on the registration parameter.** The existing mechanism already maps violations to `VALIDATION_FAILED`. Alternative: a custom validator class. Rejected: more code for the same mapping.
- **Length with `@Size(min = 6, max = 20)`, and the composition and whitespace rules in one `@Pattern`.** The pattern uses lookaheads for one lowercase letter (`a-z`), one uppercase letter (`A-Z`) and one special character from the OWASP list, and then `\S*` for no whitespace. Letters and digits are excluded from the special set by construction, because the special set is an explicit character class. Alternative: several `@Pattern` annotations. Rejected: one violation per rule would not map to one message, and the field is the same either way.
- **Use Unicode-aware whitespace in the pattern with the inline flag `(?U)`.** Java's `\S` by default is ASCII-only, so a non-breaking space would pass the rule. The inline flag makes `\S` match Unicode whitespace as the spec requires.
- **Special set written out as an explicit character class.** The OWASP list is `!"#$%&'()*+,-./:;<=>?@[\]^_`{|}~`. Writing it out keeps the rule visible in the code and in the contract.

## Risks / Trade-offs

- [Regex escaping errors in the special set] → The test for each special character group in the spec scenarios catches them. The tests list the characters explicitly.
- [Clients that still check 12 characters would reject passwords of 6 to 11 characters that the API now accepts] → The frontend must update its check to the contract in the same release. The contract change lands in the same PR.

## Migration Plan

None. The rule applies only to new registrations. Existing accounts keep their passwords. Rollback is reverting the commits.

## Open Questions

None.
