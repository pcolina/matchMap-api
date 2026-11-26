# Proposal

No GitHub issue is linked. The repository owner opened this change directly.

## Why

`add-first-visit-flag` (merged in PR #49) stores the flag `isFirstVisit` and sets it to `true` on every account creation, but nothing can set it back to `false`. The web and mobile apps show a welcome screen to new accounts, and they need to tell the API when the user has left it, so the flag stops asking for the welcome screen again.

## What Changes

- **New mutation `completeFirstVisit: User!`** in `contracts/graphql/account.graphqls`. It takes no arguments and requires an access token. It sets `first_visit` to `false` for the signed-in account and returns the account, so the client's cache of `me` is updated without a second query.
- **Idempotent.** Calling it when the flag is already `false` changes nothing and returns the account without an error.
- **No other field is writable.** `isFirstVisit` stays read-only on `User`; only this mutation changes the value, and only from `true` to `false`.
- No migration, no new dependency.

[NEEDS CLARIFICATION: Issue #1 "Onboarding after first login with minimal friction" was referenced by issue #17 as the owner of the rule that sets the flag to `false`. Its text has not been read in this change. The proposal assumes the client calls `completeFirstVisit` when the welcome screen is left. If #1 says the API clears the flag on the first successful login instead, this change is replaced by that rule. Confirm which applies before the proposal is approved.]

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `user-accounts`: a new requirement "Complete first visit" with the mutation, its idempotence and its authorization. The "First visit flag starts as true" requirement is unchanged.

## Out of scope

- When the client calls the mutation: the web and mobile screens are changed in the clients.
- Any other write to `User`.
- Tracking which steps of the welcome screen were shown.
- Any change to `me`, `register` or the migration.

## Depends on

- `user-accounts` (`openspec/specs/user-accounts`): the account, the `User` type and the flag `isFirstVisit`, added by `add-first-visit-flag`.
- `authentication` (`openspec/specs/authentication`): the access token that identifies the account.
