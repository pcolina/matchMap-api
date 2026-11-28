# Proposal

Closes #53 (https://github.com/pcolina/matchMap-api/issues/53)

## Why

The catalog of countries and cities is changed only by administrators (`geo-catalog` in `add-country-city-pins`). The API has no notion of a role yet, so nothing can restrict those changes. This change adds the `ADMIN` role that authorises them. It is a separate change because `add-country-city-pins` depends on it for its batch import (task 3.2), and the team agreed to build it first.

## What Changes

- Each account has one role: `ADMIN` or `USER`. `USER` is the default. The subscription plan (`subscription`, which will include `PRO`) is not a role and is not changed by this change.
- The role is stored as a column `role` on the account table, `NOT NULL`, with the values `ADMIN` and `USER` and `USER` as default. The `me` query returns the role, so clients can hide administration actions.
- Protected operations that change the catalog require the `ADMIN` role. The check runs on the server for every request, whatever the client sent.
- The first administrator is set by a database command that the operator runs and documents.

## Capabilities

### New Capabilities

- `admin-role`: the role of each account and the rule that only `ADMIN` accounts can use the catalog-changing operations.

### Modified Capabilities

- `user-accounts`: the requirement "Read own account" changes, because `me` returns the `role` of the account.

## Out of scope

- The administration panel and its pages, which live in `matchmap-control-panel`.
- The catalog operations themselves (`importCatalog`, the admin mutations). They are in `add-country-city-pins`.
- Mutations to grant or revoke the role. The first `ADMIN` is set by a database command that the operator runs and documents.
- Roles used only by `matchmap-control-panel`. They are not stored in the API's account table.
- Subscription plans such as `PRO`. They are the existing `subscription` attribute and belong to a later change.

## Depends on

- `user-accounts`: the accounts that receive the role.
- `authentication`: the access token that identifies the signed-in user.

## Open points

- The text of issue #53 was not available when this proposal was written. Its scope is taken from the `ADMIN` description in this proposal. Confirm it against the issue before the PR.

## Impact

- Database: a new Flyway migration `V8_3__add_user_role.sql` adds the role to the account table. No applied migration is edited.
- Contracts: the `User` type gains a `role` field of type `UserRole` (`ADMIN`, `USER`) in `contracts/graphql/account.graphqls`.
- Code: the role check for protected operations.
- Dependencies: none.
