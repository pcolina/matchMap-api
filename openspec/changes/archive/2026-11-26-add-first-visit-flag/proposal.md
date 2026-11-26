# Proposal

No GitHub issue is linked. The repository owner opened this change directly.

## Why

The web and mobile apps need to know whether an account is new, so they can show the welcome screen. The API does not store that state yet. This change stores it, sets it when an account is created, and returns it in `me`.

## What Changes

- **Database.** A new Flyway migration `V6__add_first_visit_to_user_accounts.sql` adds the column `first_visit boolean not null default true` to `user_account`. No existing migration is edited. Every account creation path, `register` and Google sign-in, starts with `true`.
- **Contract.** The `User` type in `contracts/graphql/account.graphqls` gains `isFirstVisit: Boolean!`, so `me` returns the flag.
- **Code.** The account mapping reads `first_visit` and returns it as `isFirstVisit` in `me`.

Nothing in this change sets the flag back to `false`. How and when it changes is decided by a later change.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `user-accounts`: the `User` type exposed by `me` gains the field `isFirstVisit`. Existing fields do not change.

## Out of scope

- Any way to change the flag: no mutation, no sign-in logic, no client behaviour.
- Map data, statistics, friends and `initialView`. Separate changes.
- Any tracking of which screens the welcome shows.

## Depends on

- `user-accounts` (`openspec/specs/user-accounts`): the account and its `me` query, which gains `isFirstVisit`.
- `authentication` (`openspec/specs/authentication`): the access token used to call `me`.
