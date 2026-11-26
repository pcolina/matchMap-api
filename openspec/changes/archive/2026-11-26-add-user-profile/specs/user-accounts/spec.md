# Spec Delta: user-accounts

## ADDED Requirements

### Requirement: Subscription defaults to free

Every account SHALL have a subscription plan, stored in `user_account.subscription` and exposed as `subscription: SubscriptionPlan!`. The only plan is `FREE`. Every account created by any path SHALL start with `FREE`, and every existing account SHALL have `FREE` after migration `V7__add_user_profile.sql`. No operation in this capability changes the plan.

#### Scenario: Account created by register has the free plan
- **WHEN** an anonymous client sends `register` with email `ana@example.com` and a valid password
- **THEN** `user_account` has `subscription = 'FREE'` for that account
- **AND** `{ me { subscription } }` with a valid access token returns `FREE`

#### Scenario: Existing accounts have the free plan after the migration
- **WHEN** the migration `V7__add_user_profile.sql` runs on a database that already has accounts
- **THEN** every existing row has `subscription = 'FREE'`

#### Scenario: The plan is limited to free in the database
- **WHEN** a row is inserted with `subscription = 'PREMIUM'`
- **THEN** the database rejects the row

### Requirement: Profile values start empty

The account SHALL store `first_name`, `last_name`, `nationality` and `birth_country`. None of them is asked at registration, so each SHALL be `null` for an account created by `register` or by Google sign-in. The `User` type SHALL expose them as `firstName: String`, `lastName: String`, `nationality: String` and `birthCountry: String`, all nullable.

#### Scenario: New account has empty profile values
- **WHEN** an anonymous client sends `register` with email `ana@example.com` and a valid password, and then a signed-in client sends `{ me { firstName lastName nationality birthCountry } }`
- **THEN** every field in `data.me` is `null`

#### Scenario: Existing accounts have empty profile values after the migration
- **WHEN** the migration `V7__add_user_profile.sql` runs on a database that already has accounts
- **THEN** every existing row has `first_name`, `last_name`, `nationality` and `birth_country` equal to `NULL`

### Requirement: Stored profile values are returned by me

`me` SHALL return the stored values of `first_name`, `last_name`, `nationality` and `birth_country` for the signed-in account, unchanged.

#### Scenario: Stored profile values appear in me
- **WHEN** the row of `ana@example.com` has `first_name = 'Ana'`, `last_name = 'Pérez'`, `nationality = 'ES'` and `birth_country = 'FR'`, and a client with a valid access token for that account sends `{ me { firstName lastName nationality birthCountry } }`
- **THEN** `data.me` contains `Ana`, `Pérez`, `ES` and `FR`

### Requirement: Country codes are two uppercase letters

`nationality` and `birth_country` SHALL be either `NULL` or exactly two uppercase ASCII letters, as in ISO 3166-1 alpha-2. The database SHALL reject any other value.

#### Scenario: A country code with lowercase letters is rejected
- **WHEN** a row is updated with `nationality = 'es'`
- **THEN** the database rejects the update

#### Scenario: A country name is rejected
- **WHEN** a row is updated with `birth_country = 'Spain'`
- **THEN** the database rejects the update

### Requirement: Password is never returned

No field of the `User` type SHALL return the password or its hash. A client that asks for the password hash SHALL receive a GraphQL validation error.

#### Scenario: The password hash cannot be queried
- **WHEN** a client with a valid access token sends `{ me { passwordHash } }`
- **THEN** the response has a GraphQL validation error, because `User` has no `passwordHash` field

#### Scenario: The User type has no password field
- **WHEN** the contract `contracts/graphql/account.graphqls` is parsed
- **THEN** the `User` type has no field whose name contains `password`

## MODIFIED Requirements

### Requirement: Read own account

The GraphQL query `me: User!` SHALL return the account of the user identified by the access token, with `id`, `email`, `username`, `createdAt`, `isFirstVisit`, `firstName`, `lastName`, `subscription`, `nationality` and `birthCountry`. Without an access token it SHALL fail with an error whose `extensions.classification` is `UNAUTHORIZED` and SHALL return no account data.

#### Scenario: Signed-in user reads their account
- **WHEN** a client sends `{ me { id email username isFirstVisit firstName lastName subscription nationality birthCountry } }` with a valid access token for `ana@example.com`
- **THEN** `data.me` contains the id, email, username, `isFirstVisit`, the profile fields and `subscription` of that account

#### Scenario: Anonymous client reads me
- **WHEN** a client sends `{ me { id } }` without an access token
- **THEN** the response has an error whose `extensions.classification` is `UNAUTHORIZED`
- **AND** the response contains no `data.me` account

#### Scenario: Me has no displayName field
- **WHEN** a client sends `{ me { displayName } }` with a valid access token
- **THEN** the response has a GraphQL validation error, because `User` has no `displayName` field
