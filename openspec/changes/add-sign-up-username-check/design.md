# Design

## Context

Sign-up goes through `RegistrationService.register`. It asks `UsernameGenerator` for a username from the email prefix. The generator lowercases the prefix, replaces every character outside `a-z`, `0-9` and `_` with `_`, and cuts the result at 30 characters. For prefixes shorter than 3 characters it uses `user_` plus 8 random characters. On collision it appends `_2`, `_3` and so on. The generator is in `com.matchmap.api.account`.

The matcher from change 1 is in `com.matchmap.api.moderation`. Its classes are package-private, and the tables `prohibited_word` and `reserved_word` exist and are empty (migration `V4`).

Baseline for contract changes: `cd8bd1b` (main after PR #32). This change touches no file under `contracts/`.

## Goals / Non-Goals

**Goals**
- A generated username that contains an active prohibited or reserved word is replaced by a fallback, and sign-up still succeeds.
- The check runs on every sign-up, reading the current rows of the two tables.
- The fallback is unique, follows the username rules, and is checked by the same matcher.

**Non-Goals**
- No cache, no rate limiting and no refresh of the lists (change 3 of #6).
- No change to `RegisterInput`, to the GraphQL schema or to any error code.
- No change to the username-change path (#30).

## Decisions

**A public facade in `moderation`.** A new public class `UsernameModeration` exposes one method, `boolean isBlocked(String username)`. It reads the active rows of both tables through `JdbcTemplate`, builds a `ProhibitedWordMatcher` from them and returns whether it matches. The matcher stays package-private. The `account` package depends only on the facade.

**Lists are read on every sign-up.** The spec states this for change 2, as agreed. Building the matcher per call is acceptable for now, because the cache comes in change 3. The read is one query over both tables, with `active = true`.

**Check the base name, before the collision suffix.** The generator adds `_2`, `_3` to resolve collisions. The word check runs on the sanitized base, so a suffix added by the generator never decides a match. The base is the value the spec calls "the generated username". For a short prefix the base is `user_` plus random characters; it is checked too, and it does not match in practice.

**Fallback generation is separate and bounded.** A new method in `UsernameGenerator` builds `user_` plus 8 random characters from `a-z` and `0-9`, using the same `SecureRandom` and alphabet already in the class. The fallback is accepted when `isBlocked` is false and `taken` is false. The loop runs at most 5 attempts in total. When all 5 collide, the sign-up returns an internal error and creates no account. The owner chose these values.

**The random source is injectable.** `UsernameGenerator` gets a constructor with a `Random` parameter for tests, and the default constructor uses `SecureRandom`. Tests can then force a collision and check the bounded retry without depending on chance.

**No new error codes.** Nothing in `register` returns a new code. The `account` package maps only the existing `AccountException` values.

## Risks / Trade-offs

- **Database read per sign-up.** Two small queries per registration. Acceptable until change 3 adds the cache.
- **Empty lists in production.** Until #31 loads the words, this change has no effect. The spec states it, so nobody expects the filter to work before the lists are loaded.
- **The fallback changes the visible username.** A user whose email prefix matches a word gets `user_xxxxxxxx`. That is the intended behaviour; users can change it later in #30.
- **Repeat collapse false positives** (from change 1) also apply here. Names such as `rot` for `root` are blocked when `root` is an active reserved word. Accepted, as decided in change 1.

## Test strategy

- Unit test for the fallback generator: the form of the name, the retry loop with an injected random source that forces one collision, and the bound when every attempt collides.
- Unit test for `UsernameModeration.isBlocked` with rows inserted in a transactional test.
- Integration test for `register`: a prefix that matches an inserted prohibited word produces a `user_` username and no error; a clean prefix keeps its username; the same tests run with empty lists.
- Logs: a test asserts that no log line contains the generated username or the matched word.
