# Proposal

Issue: #10 "Enforce password complexity on registration (backend)" (label `in progress`).

## Why

The registration form checks a password rule that the API does not enforce, and the API rule that exists today (12 to 20 characters, no composition rules) differs from the rule the team has agreed. The contract (`contracts/openapi/auth.yaml`) sets 6 to 20 characters for the password. The agreed rule adds composition requirements so that the API enforces what the clients check, as CLAUDE.md §2 requires.

## What Changes

- **BREAKING** for new registrations: the minimum length goes from 12 to 6 characters, and a password must contain at least one lowercase letter (`a-z`), one uppercase letter (`A-Z`) and one special character.
- Special characters are the OWASP list of ASCII punctuation and symbols: `!"#$%&'()*+,-./:;<=>?@[\]^_`{|}~`. Letters and digits are not special characters.
- No whitespace character is allowed at any position. This is unchanged.
- Unicode characters are allowed. They do not count as special characters. This is unchanged.
- The register description in `contracts/graphql/account.graphqls` documents the password rule, so the clients can copy it. Schema-first: the contract changes before the implementation.
- Sign-in is not affected. Login keeps the contract's 6 to 20 characters and does not apply the composition rules, as the `authentication` spec already states.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `user-accounts`: the requirement "Password policy" changes from 12 to 20 characters with no composition rules to 6 to 20 characters with the composition rules above.

## Out of scope

- Sign-in password rules. The login limits (6 to 20) are already in `auth.yaml` and the `authentication` spec.
- Removing `username` and `displayName` from registration, issue #13.
- Password hashing, storage and the existing "Passwords are stored only as salted hashes" requirement.
- Changes to the frontend and to the client codegen.

## Depends on

- `user-accounts` (the "Password policy" requirement and the `VALIDATION_FAILED` code, both in the main spec).
- `authentication` (login password limits, unchanged).
- Issue #9 and PR #23 (stable registration error codes), already merged.

## Impact

- `contracts/graphql/account.graphqls`: documents the password rule on `register`.
- Registration validation in the backend, and the tests that currently expect 11 and 12 characters to be the limits (`RegisterIT`).
- No database migration. No new dependency. Existing accounts are not affected: the rule applies only at registration.
