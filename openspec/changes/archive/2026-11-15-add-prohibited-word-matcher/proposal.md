# Proposal

Refs #6. This change is the first of three that implement #6. It does not close the issue.

## Why

Usernames are checked only for format and uniqueness. Users can pick names that contain prohibited words, or evade a simple check with leetspeak, separators, accents or repeated letters. This is a store-review and legal requirement for the MVP. Before any sign-up path can use a blocklist, the backend needs the data model and a matcher that normalizes a candidate and finds prohibited and reserved words in it.

## What Changes

- A new Flyway migration creates two tables, `prohibited_word` and `reserved_word`. Each row has the word, its normalized form, a category, a language, an active flag and timestamps. The migration inserts no words.
- A new Java matcher service normalizes a candidate username and reports whether it contains an active prohibited word or an active reserved word, and in which category.
- Normalization: Unicode NFKC, lowercase, strip diacritics, map leetspeak and homoglyphs to letters (`0`→`o`, `1`→`i`, `3`→`e`, `4`→`a`, `5`→`s`, `7`→`t`, `@`→`a`, `$`→`s`), remove separators (`.`, `_`, `-`, spaces), and a second pass that collapses repeated letters.
- Matching: a word longer than 4 characters after normalization matches as a substring. A word of 4 characters or fewer matches only as a whole word.
- Search uses the Aho-Corasick algorithm, through a new library approved in #6. Its version is set in `design.md`.
- No words are committed. The lists are loaded outside the repository, as decided in #31.

## Capabilities

### New Capabilities

- `username-moderation`: which usernames are rejected for containing a prohibited or reserved word, how candidates are normalized and matched, and the data the matcher reads.

### Modified Capabilities

None. `user-accounts` is unchanged in this change. Sign-up does not call the matcher until change 2.

## Impact

- Database: two new tables through a Flyway migration. No change to existing tables.
- Code: a matcher service and its unit tests. Nothing calls it yet.
- Dependencies: one new library for Aho-Corasick, approved in #6.
- Contracts: none. Error codes are part of later changes.

## Out of scope

- Calling the matcher from sign-up, and the fallback username (change 2 of #6).
- Cache refresh of the lists, and rate limiting (change 3 of #6).
- The username change path and its error codes `PROHIBITED_WORD` and `RESERVED_USERNAME` (#30).
- The content of the lists, their owners and their storage (#31).
- An admin API for the lists.
- Display name.

## Depends on

- `user-accounts` (spec "Username rules"): the matcher applies to usernames that follow those rules.
