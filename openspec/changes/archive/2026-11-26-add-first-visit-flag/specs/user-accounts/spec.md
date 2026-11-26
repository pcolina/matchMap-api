# Spec Delta: user-accounts

## ADDED Requirements

### Requirement: First visit flag starts as true

Every account SHALL have a boolean flag `first_visit`, stored in `user_account`, that is `true` when the account is created, whichever path creates it: `register` or Google sign-in. The `User` type SHALL expose it as `isFirstVisit: Boolean!`. The flag is not changed by this capability: no operation in this change sets it to `false`.

#### Scenario: Account created by register starts with the flag true
- **WHEN** an anonymous client sends `register` with email `ana@example.com` and a valid password
- **THEN** the account created in `user_account` has `first_visit = true`

#### Scenario: Account created by Google sign-in starts with the flag true
- **WHEN** a client signs in with a valid Google ID token for an email that has no account
- **THEN** the account created in `user_account` has `first_visit = true`

#### Scenario: Signed-in user reads the flag true for a new account
- **WHEN** a client sends `{ me { isFirstVisit } }` with a valid access token for the account created in the previous scenario
- **THEN** `data.me.isFirstVisit` is `true`

#### Scenario: Existing accounts start with the flag true
- **WHEN** the migration `V6__add_first_visit_to_user_accounts.sql` runs on a database that already has accounts
- **THEN** every existing row has `first_visit = true`

#### Scenario: The flag is not writable through the API
- **WHEN** a client sends `{ me { isFirstVisit } }` after signing in, and then sends any mutation with `isFirstVisit` as an argument
- **THEN** the mutation fails with a GraphQL validation error, because no mutation accepts the flag

## MODIFIED Requirements

### Requirement: Read own account

The GraphQL query `me: User!` SHALL return the account of the user identified by the access token, with `id`, `email`, `username` and `isFirstVisit`. Without an access token it SHALL fail with an error whose `extensions.classification` is `UNAUTHORIZED` and SHALL return no account data.

#### Scenario: Signed-in user reads their account
- **WHEN** a client sends `{ me { id email username isFirstVisit } }` with a valid access token for `ana@example.com`
- **THEN** `data.me` contains the id, email, username and `isFirstVisit` of that account

#### Scenario: Anonymous client reads me
- **WHEN** a client sends `{ me { id } }` without an access token
- **THEN** the response has an error whose `extensions.classification` is `UNAUTHORIZED`
- **AND** the response contains no `data.me` account

#### Scenario: Me has no displayName field
- **WHEN** a client sends `{ me { displayName } }` with a valid access token
- **THEN** the response has a GraphQL validation error, because `User` has no `displayName` field
