# Design

Contract baseline: `905c211` (main after PR #35). This change touches no file under `contracts/`.

## Context

`V4__create_prohibited_word_lists.sql` created `prohibited_word` and `reserved_word` with the same columns: `id uuid`, `word`, `normalized`, `category`, `language varchar(5)`, `active`, `created_at`, `updated_at`. Both tables have a unique constraint on `(normalized, category)`. The category check allows `RESERVED`. The database image is `postgis/postgis:17-3.5`, so `gen_random_uuid()` is available without an extension.

The matcher reads only active rows (`select normalized, category, active from %s where active`), so a seeded row is used at once, with no code change.

## Goals / Non-Goals

**Goals:**
- Seed exactly the approved reserved names.
- Store each normalized form exactly as the matcher would compute it.
- Keep the migration append-only and free of harmful content.

**Non-Goals:**
- Any prohibited word (#31).
- Any code, contract or dependency change.
- The cache (`refresh-word-lists-hourly`, to be proposed after this change is archived).

## Decisions

**D1. Migration, not a loader script.** The approved names are not private, so they belong in the repository and run with every fresh database. Alternative: an operations script. Rejected for these names, because a script would leave a fresh database without them.

**D2. Normalized forms computed with the matcher rules.** The rules in `username-moderation` (NFKC, lowercase, remove diacritics, leetspeak mapping, remove separators, collapse repeated letters) give these forms:

| Word | Normalized |
|---|---|
| admin | admin |
| administrator | administrator |
| administrador | administrador |
| root | rot |
| superadmin | superadmin |
| support | suport |
| soporte | soporte |
| staff | staf |
| moderator | moderator |
| moderador | moderador |
| matchmap | matchmap |
| system | system |
| sistema | sistema |

The normalized forms were checked with a script that applies those rules in the same order.

**D3. Language.** The English names are stored with `en` and the Spanish names with `es`. The approved list did not state a language. This is an assumption; the owner can correct it in the migration before the change is applied.

**D4. Ids and timestamps.** `id` is `gen_random_uuid()`, `created_at` and `updated_at` are `now()`. `active` is `true`, `category` is `RESERVED`.

**D5. Idempotence on a fresh database only.** The migration runs once per database under Flyway. It does not use `ON CONFLICT`, because Flyway guarantees it runs once and a conflict would hide an error.

## Risks / Trade-offs

- **[A wrong normalized form silently misses a name]** → Each seeded name is covered by a test that signs up with it (see tasks) and by the migration test that checks the stored forms.
- **[A seeded name blocks a real user]** → Accepted: the names are platform names, and a blocked name gets a fallback username, not an error.
- **[Language value is wrong]** → Column is informational for matching; the owner can correct it before apply (D3).

## Migration Plan

1. Add `V5__seed_reserved_words.sql` after V4.
2. A fresh database gets the rows on first start. An existing database gets them on its next start.
3. Rollback: a new forward migration that deletes the seeded rows. The applied V5 is never edited.

## Open Questions

None. The list is approved; the language assumption (D3) is recorded above.
