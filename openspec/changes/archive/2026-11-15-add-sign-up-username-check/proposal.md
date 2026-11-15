# Proposal

Refs #6. This change is the second of three that implement #6. It does not close the issue. Change 1 (`add-prohibited-word-matcher`) is merged in PR #32.

## Why

Sign-up assigns a username generated from the email prefix. Today that username is not checked against the prohibited and reserved word lists, so an account can be created with a name such as `nazi` or `admin`. The matcher exists since change 1, but nothing calls it yet. This change connects the matcher to sign-up. The user never picks a username at sign-up, so a flagged name is replaced by a fallback instead of producing an error.

## What Changes

- At sign-up, after the username is generated from the email prefix, the matcher checks it against the active rows of `prohibited_word` and `reserved_word`.
- If the generated username contains an active prohibited or reserved word, the account gets a fallback username: `user_` followed by 8 random characters from `a-z` and `0-9`, the same form already used for short prefixes. The fallback is checked with the matcher and for uniqueness.
- If the fallback also collides, the system tries again, up to 5 attempts in total. If all 5 collide, the response is an internal error and no account is created.
- The `register` mutation never returns `PROHIBITED_WORD` or `RESERVED_USERNAME`. Those codes are used only by the username change, in issue #30.
- The lists are read from the database on each sign-up. The cache that refreshes the lists is change 3.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `user-accounts`: the requirement "Username rules" changes. The generated username can be the fallback when the email prefix contains a prohibited or reserved word.
- `username-moderation`: a new requirement states that sign-up applies the matcher and assigns the fallback. The matcher itself is unchanged.

## Impact

- Code: the sign-up path in `RegistrationService` calls the matcher. Word lists are read through a repository.
- Database: no schema change. The tables are created in change 1.
- Contracts: none. `RegisterInput` keeps `email` and `password` only, and no error code is added to `register`.
- Dependencies: none.

## Out of scope

- Cache refresh and rate limiting (change 3 of #6).
- The username change path and its codes `PROHIBITED_WORD` and `RESERVED_USERNAME` (issue #30).
- The content of the lists (issue #31). Until those lists are loaded, sign-up behaves as before.
- Display name, admin API and error message wording.

## Depends on

- `username-moderation` (change 1 of #6, archived): the matcher and the tables.
- `user-accounts` (requirement "Username rules", requirement "Register with email and password").
