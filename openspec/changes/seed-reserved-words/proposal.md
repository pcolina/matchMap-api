# Proposal

Closes #34

## Why

The `reserved_word` table is created empty, so the names that belong to the platform are not blocked at sign-up. A sign-up with `admin@example.com` gets `admin` as its username today. The names are not harmful content and do not need to be private, so they can be loaded by a migration in the public repository. The harmful categories (`CHILD_SAFETY`, `HATE`, `VIOLENCE`, `SEXUAL`, `IMPERSONATION`) are not part of this change; they stay in #31.

## What Changes

- A new Flyway migration `V5__seed_reserved_words.sql` inserts the approved reserved names into `reserved_word`, with category `RESERVED` and the active flag set.
- The approved list is: `admin`, `administrator`, `administrador`, `root`, `superadmin`, `support`, `soporte`, `staff`, `moderator`, `moderador`, `matchmap`, `system`, `sistema`.
- Each row stores the normalized form computed with the same rules as the matcher (for example `support` is stored as `suport`, `root` as `rot`, `staff` as `staf`).
- `prohibited_word` stays empty. Its content is in #31.
- The requirement "Prohibited and reserved word lists" in `username-moderation` says the tables are created empty and that the schema change inserts no word. This change modifies it: the `reserved_word` table is seeded by V5, and `prohibited_word` stays empty.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `username-moderation`: the requirement "Prohibited and reserved word lists" changes from "created empty" to "reserved_word seeded with the approved names".

## Impact

- Data: one new migration. No applied migration is edited.
- Code: none. The matcher already reads active rows.
- Contract: none. Baseline for contract changes: `905c211` (main after PR #35). This change touches no file under `contracts/`.
- Dependencies: none.
- Sign-up behaviour: a sign-up whose generated username matches a seeded name, for example `admin`, gets a `user_` fallback username. This is the behaviour already specified in `username-moderation`.

## Out of scope

- The contents of `prohibited_word` and the harmful categories (#31).
- The blocklist cache and its hourly refresh (the `refresh-word-lists-hourly` change, to be proposed after this one is archived).
- The privacy policy and terms (#31).
- The username change path (#30).
- The ADR on normalization and matching (#6).

## Depends on

- `username-moderation`, as modified by this change.
- `user-accounts` (fallback username, unchanged).
- Issue #6 (matcher and `reserved_word` table, merged in V4).
