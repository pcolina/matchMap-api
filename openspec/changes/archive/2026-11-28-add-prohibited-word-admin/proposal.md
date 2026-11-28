# Proposal

Closes #57 (https://github.com/pcolina/matchMap-api/issues/57)

## Why

The control panel needs to manage the prohibited word list used by the username check. Today the list can only be changed by writing to the database. Administrators need to add words, see them, and deactivate them without losing their history.

## What Changes

- Administrators create a prohibited word with its category and language. The word is stored as typed, and its normalized form is computed with the same normalizer the username check uses.
- A word whose normalized form and category already exist is rejected with error code `DUPLICATE_WORD`. The error message does not contain the word.
- Administrators list the prohibited words. The list is not paginated.
- Administrators set a word active or inactive. Words are never deleted.
- After each write the word cache is reloaded, so the change applies at once.

## Capabilities

### New Capabilities

- `prohibited-word-admin`: creating, listing and activating prohibited words through the API, for the ADMIN role only.

### Modified Capabilities

None. The matching rules in `username-moderation` do not change.

## Out of scope

- The reserved word list (`reserved_word`).
- Deleting words.
- Pagination of the list.
- Changes to the control panel, which lives in `matchmap-control-panel`.

## Depends on

- `add-admin-role` (issue #53, merged): the ADMIN role that restricts these operations.
- `username-moderation`: the tables and the normalizer.

## Open points

None.

## Impact

- Contracts: a new type `ProhibitedWord`, a new enum `ProhibitedCategory`, a query and two mutations in `contracts/graphql/moderation.graphqls`.
- Database: no migration. The table `prohibited_word` already has the columns and the unique constraint on (normalized, category). Migration V4 is not edited.
- Code: a service and a GraphQL controller in the `moderation` package, and an exception resolver for the moderation error codes.
- Dependencies: none.
