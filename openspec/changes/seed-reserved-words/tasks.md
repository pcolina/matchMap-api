# Tasks

Each task is one commit, with `Task: <id>` in the body. TDD: write the failing test, run it and show it failing, then the minimum code to pass, then refactor.

## 1. Migration

- [x] 1.1 Write `ReservedWordSeedIT.seedsApprovedNames` (scenario "Approved reserved names are seeded"): on a Testcontainers PostGIS database after the migrations, `reserved_word` holds the 13 approved names, each `RESERVED` and active. Show it failing; add `V5__seed_reserved_words.sql` with the 13 inserts (design D4).
- [x] 1.2 Write `ReservedWordSeedIT.storesNormalizedForms` (scenario "Seeded names are stored normalized"): the rows for `support`, `root` and `staff` have `suport`, `rot` and `staf`. Show it failing; correct the normalized values in V5 (design D2).
- [x] 1.3 Write `ReservedWordSeedIT.prohibitedTableIsEmptyAfterMigration` (scenario "Tables are created empty"): `prohibited_word` has no rows after the migrations. Show it failing; confirm it passes with V5 touching only `reserved_word`.

## 2. Sign-up

- [x] 2.1 Write `RegisterIT.seededNameGetsFallback` (scenario "Sign-up with a seeded name gets a fallback"): register `admin@example.com` and assert the username is a `user_` fallback, not `admin`. Show it failing; confirm it passes with the seed in place, with no code change.
- [x] 2.2 Write `RegisterIT.seededNamesWithRepeatedLettersGetFallback` for `suport@example.com` and `ro0t@example.com`, which normalize to the seeded forms. Show it failing; confirm it passes.

## 3. Verification

- [x] 3.1 Confirm every scenario of `specs/username-moderation/spec.md` (the five MODIFIED scenarios, including "Tables are created empty" and "Inactive entries are ignored", carried over from the current spec) is named by a test above.
- [ ] 3.2 Confirm the existing moderation tests from `add-prohibited-word-matcher`, `add-sign-up-username-check` and `complete-username-moderation` still pass with no change to their expectations.
