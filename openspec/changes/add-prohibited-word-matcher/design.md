# Design

## Context

The backend is Java 25 with Spring Boot, Flyway and PostgreSQL. Existing migrations are `V1__enable_postgis.sql`, `V2__create_user_accounts.sql` and `V3__drop_display_name.sql`, so the next one is `V4`. Feature code lives in packages under `com.matchmap.api` (`account`, `auth`, `graphql`, `health`). The `username-moderation` capability is new; the username rules it builds on are in `openspec/specs/user-accounts/spec.md`.

Baseline for contract changes: `66d2e63` (main after PR #29). This change touches no file under `contracts/`.

The repository is public. The migration in this change creates tables only. The words themselves are loaded outside git, as decided in #31.

## Goals / Non-Goals

**Goals**
- Two tables for prohibited and reserved words, empty after migration.
- A matcher that normalizes a candidate and reports a match with its category.
- Unit tests for each normalization rule and each matching rule.

**Non-Goals**
- Calling the matcher from sign-up, the fallback username, caches, rate limiting, error codes, contracts and admin tooling. Those belong to later changes of #6 and to #30.
- Any word in the repository, including seed data.

## Decisions

**Two tables, one per list.** `prohibited_word` and `reserved_word` have the same columns: `id`, `word`, `normalized`, `category`, `language`, `active`, `created_at`, `updated_at`. A separate table keeps reserved words apart from prohibited ones, which matches the decision in #6 to use separate lists. `category` is a text column with a check constraint on the six values in the spec, so an unknown category cannot be stored.

**The normalized form is stored.** The matcher works on normalized text, so each row keeps its normalized form, computed by the same code that normalizes candidates. The matcher reads `normalized` and never recomputes it from `word`. A unique constraint on `(normalized, category)` prevents duplicates within a category.

**Normalization in a dedicated class.** One class applies the steps in the order given in the spec: NFKC, lowercase, diacritics, leetspeak map, separator removal, repeat collapse. The same class normalizes words and candidates, so both sides always match.

**Matching with Aho-Corasick.** The matcher builds an Aho-Corasick automaton from the active normalized words that are longer than 4 characters, and finds them as substrings in one pass. Words of 4 characters or fewer are matched by a whole-word comparison against the normalized candidate. This split follows the rule in the spec: Aho-Corasick finds substrings, and short words need the whole-word check.

**Library.** Aho-Corasick is a new dependency approved in #6. It is `org.ahocorasick:ahocorasick:0.6.3`, the latest release on Maven Central. It is licensed under Apache 2.0 and has no dependencies of its own. Its last release is from 2021, so it is not actively maintained; this is accepted for the MVP, and the matcher is isolated behind one class so it can be replaced.

**Automaton built in memory.** The matcher receives the list of active words when it is constructed. It does not query the database per candidate. Reloading the list is a later change (cache refresh), so this change exposes a constructor that takes the words and does not read the database itself.

**Empty lists are valid.** With no words, the matcher reports no match for every candidate. Tests can construct the matcher with a small list defined in the test code, never in the repository seed.

## Risks / Trade-offs

- **Short-word boundaries.** A word of 4 characters or fewer matches when it is the whole normalized candidate, or one segment separated by `.`, `_`, `-` or a space. Checking only the whole candidate would miss `ana_nazi`. Checking only segments would miss `n-a-z-i`. Both checks together block both, and the spec states them as one rule.
- **False positives.** Substring matching for longer words can block harmless names that contain a listed word. The owner of the lists (#31) controls the words, so a false positive is fixed by removing a word, with no code change.
- **Normalization maps.** The leetspeak map is short and fixed. Other homoglyphs are not covered in this change. A bypass with an unmapped character is possible, and is fixed by adding a mapping in a later change.
- **No database check in tests.** Migration tests use Testcontainers PostgreSQL, as the rest of the project does. Matcher tests run without a database.

## Test strategy

- Unit tests for normalization: each mapping, separator removal, repeat collapse, NFKC and diacritics.
- Unit tests for matching: a long word as substring, a short word as whole word, inactive words ignored, the category reported.
- Integration test for the migration: tables exist, are empty, and reject an unknown category and a duplicate `(normalized, category)`. It runs against PostgreSQL in Testcontainers.
