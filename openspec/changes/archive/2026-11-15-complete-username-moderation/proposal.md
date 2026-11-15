# Proposal

Refs #6. This change closes two gaps left by changes 1 and 2 of #6. It does not close the issue.

## Why

Two items from the first two changes of #6 are still open:

- The `Purpose` section of the `username-moderation` spec is still the placeholder created by archiving change 1 (`TBD - created by archiving change add-prohibited-word-matcher`).
- The fallback path of sign-up (`RegistrationService`) returns an internal error when all five fallback attempts collide, but no test covers that path.

## What Changes

- The `Purpose` section of `openspec/specs/username-moderation/spec.md` is replaced with the real purpose of the capability. A sync only updates requirements, so this change edits that section directly, as a task. It is not a delta.
- A unit test covers `RegistrationService`: when the generated username matches a word and the fallback generator finds no free name, `register` throws `IllegalStateException` and does not save an account.
- No requirement changes. The change is marked `skip_specs: true`.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

None. No requirement changes. The purpose text is documentation of an existing capability.

## Impact

- `openspec/specs/username-moderation/spec.md`: the `Purpose` section.
- Tests: one new unit test in `src/test/java/com/matchmap/api/account/`.
- No production code changes, no contract, no schema, no dependency.

## Out of scope

- Any requirement or behaviour change.
- The username change path (issue #30).
- Cache refresh of the word lists (change 3 of #6).
- The contents of the word lists (issue #31).

## Depends on

- `username-moderation` (archived by change 1 of #6).
- `user-accounts` and the sign-up wiring of change 2 of #6 (archived).
