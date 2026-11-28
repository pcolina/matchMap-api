## Purpose

Gives each account a role, and restricts the catalog-changing operations to accounts with the `ADMIN` role.

## ADDED Requirements

### Requirement: Accounts have a role

Every account SHALL have exactly one role, either `ADMIN` or `USER`. A new account SHALL have the role `USER`. The subscription plan SHALL NOT be a role.

#### Scenario: New account gets the default role
- **WHEN** a new account is registered
- **THEN** the account has the role `USER`

### Requirement: Catalog changes require the ADMIN role

The operations that change the geographic catalog SHALL require an account with the `ADMIN` role. A request from an account without that role SHALL be rejected with `extensions.classification` `FORBIDDEN` and SHALL change nothing. The `me` query SHALL return the `role` of the signed-in account.

#### Scenario: Administrator changes the catalog
- **WHEN** an account with the `ADMIN` role calls a catalog-changing operation
- **THEN** the operation is performed

#### Scenario: Non-administrator changes the catalog
- **WHEN** an account without the `ADMIN` role calls a catalog-changing operation
- **THEN** the request is rejected with `extensions.classification` `FORBIDDEN` and no catalog row changes

#### Scenario: Me returns the role
- **WHEN** a signed-in account calls `me`
- **THEN** the response contains its `role`

### Requirement: First administrator

The first `ADMIN` account SHALL be set by a database command that the operator runs and documents. No API operation SHALL grant or revoke the `ADMIN` role in this change.

#### Scenario: First administrator is set by the database command
- **WHEN** the operator runs the documented command for an existing account
- **THEN** that account has the `ADMIN` role
