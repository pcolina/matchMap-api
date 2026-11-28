# user-accounts Specification

## Purpose

Lets people create a MatchMap account with email and password and read their own account, enforcing the rules that keep each account's identifiers unique and its password safe.

## Requirements

### Requirement: Register with email and password
The GraphQL mutation `register(input: RegisterInput!): User!` SHALL be public and SHALL create an account from `email` and `password` only. `RegisterInput` SHALL contain exactly `email` and `password`, both required. The system SHALL generate `username` from the part of `email` before the `@`. The response SHALL contain the new account's `id` (UUID), `email`, `username` and `createdAt`. It SHALL NOT sign the user in; the client signs in afterwards.

#### Scenario: Successful registration
- **WHEN** an anonymous client sends `register` with email `ana@example.com` and a valid password, and no account has username `ana`
- **THEN** the response contains no `errors`
- **AND** `data.register` contains a UUID `id`, email `ana@example.com`, username `ana` and a `createdAt` timestamp

#### Scenario: Input with a field that no longer exists
- **WHEN** a client sends `register` with a `displayName` or `username` field in the input
- **THEN** the response has a GraphQL validation error, because `RegisterInput` has no such field
- **AND** no account is created

#### Scenario: Generated username is already in use
- **WHEN** an account with username `ana` exists and a client registers with email `ana@other.example` and a valid password
- **THEN** `data.register.username` is `ana_2` (assumption: the suffix counts up from 2)
- **AND** the response does not have the code `USERNAME_TAKEN`

#### Scenario: Response never contains the password
- **WHEN** a client registers successfully
- **THEN** no field of the response contains the password or its hash, and the `User` type has no password field

### Requirement: Email is normalized and unique
The system SHALL trim and lowercase the email before storing and comparing it. An email SHALL be a syntactically valid address of at most 254 characters. Two accounts SHALL NOT share an email.

#### Scenario: Email is normalized
- **WHEN** a client registers with email `  Ana@Example.COM `
- **THEN** the returned `email` is `ana@example.com`

#### Scenario: Email already in use
- **WHEN** an account with email `ana@example.com` exists and a client registers with `ANA@example.com`
- **THEN** the response has an error whose `extensions.code` is `EMAIL_TAKEN`
- **AND** no new account is created

#### Scenario: Invalid email
- **WHEN** a client registers with email `not-an-email`
- **THEN** the response has an error whose `extensions.code` is `VALIDATION_FAILED` and whose `extensions.field` is `email`

### Requirement: Username rules
A username SHALL have 3 to 30 characters from `a-z`, `0-9` and `_`, SHALL be stored lowercased and SHALL be unique ignoring case. The system SHALL generate the username for each new account from the email prefix: the prefix is lowercased, every character outside `a-z`, `0-9` and `_` is replaced by `_`, and the result is cut at 30 characters. If the prefix has fewer than 3 characters, the username is `user_` followed by 8 random characters from `a-z` and `0-9`. If the generated username contains an active prohibited word or an active reserved word, as checked by the `username-moderation` capability, the username is a fallback: `user_` followed by 8 random characters from `a-z` and `0-9`, checked again with the same rule and for uniqueness. No client sends a username.

#### Scenario: Username already in use
- **WHEN** an account with username `ana` exists and a client registers with email `ana@other.example`
- **THEN** the stored username of the new account is `ana_2` (assumption: the suffix counts up from 2)

#### Scenario: Invalid username
- **WHEN** a client registers with email `a@example.com`, whose prefix is shorter than 3 characters
- **THEN** the generated username is `user_` followed by 8 random characters from `a-z` and `0-9`, and follows the username rules

#### Scenario: Generated username contains a prohibited word
- **WHEN** a client registers with email `nazi.fan@example.com` and `nazi` is an active prohibited word
- **THEN** the account is created with a username of `user_` followed by 8 random characters from `a-z` and `0-9`
- **AND** the response has no `errors`

#### Scenario: Generated username is a reserved word
- **WHEN** a client registers with email `admin@example.com` and `admin` is an active reserved word
- **THEN** the account is created with a username of `user_` followed by 8 random characters from `a-z` and `0-9`
- **AND** the response has no `errors`

#### Scenario: Fallback username is already in use
- **WHEN** a fallback username is generated and an account already has that username
- **THEN** a new fallback username is generated, up to 5 attempts in total
- **AND** if all 5 attempts collide, the response has an internal error and no account is created

#### Scenario: Username is lowercased and unique ignoring case
- **WHEN** an account is registered with email `Ana.Travels@Example.com`
- **THEN** the stored username is `ana_travels`

#### Scenario: Username follows the length limits
- **WHEN** a client registers with an email whose prefix is longer than 30 characters
- **THEN** the generated username has at most 30 characters

#### Scenario: Invalid display name
- **WHEN** a client sends `displayName` in `register`
- **THEN** the response has a GraphQL validation error, because the field no longer exists

### Requirement: Password policy
A password SHALL have at least 6 and at most 20 characters. A password SHALL contain at least one lowercase letter (`a-z`), at least one uppercase letter (`A-Z`) and at least one special character. A special character is one from the OWASP list of ASCII punctuation and symbols: `!"#$%&'()*+,-./:;<=>?@[\]^_`{|}~`. Letters and digits are not special characters. A password SHALL NOT contain any whitespace character, including spaces, tabs and line breaks, at any position. Any other character, including Unicode, SHALL be allowed. A password that breaks the policy SHALL be rejected with `extensions.code` `PASSWORD_INVALID` and `extensions.field` `password`.

#### Scenario: Password too short
- **WHEN** a client registers with a password of 5 characters that meets all the other rules
- **THEN** the response has an error whose `extensions.code` is `PASSWORD_INVALID` and whose `extensions.field` is `password`

#### Scenario: Password too long
- **WHEN** a client registers with a password of 21 characters that meets all the other rules
- **THEN** the response has an error whose `extensions.code` is `PASSWORD_INVALID` and whose `extensions.field` is `password`

#### Scenario: Password at the limits
- **WHEN** a client registers with a password of 6 characters and another with a password of 20 characters, both with a lowercase letter, an uppercase letter, a special character and no whitespace
- **THEN** both registrations succeed

#### Scenario: Password without a lowercase letter
- **WHEN** a client registers with `ABCDEF!1` (no lowercase letter)
- **THEN** the response has an error whose `extensions.code` is `PASSWORD_INVALID` and whose `extensions.field` is `password`

#### Scenario: Password without an uppercase letter
- **WHEN** a client registers with `abcdef!1` (no uppercase letter)
- **THEN** the response has an error whose `extensions.code` is `PASSWORD_INVALID` and whose `extensions.field` is `password`

#### Scenario: Password without a special character
- **WHEN** a client registers with `Abcdef12` (no special character)
- **THEN** the response has an error whose `extensions.code` is `PASSWORD_INVALID` and whose `extensions.field` is `password`

#### Scenario: Letters and digits are not special characters
- **WHEN** a client registers with `Abcdef12` and with `Abcdef1é` (neither has a special character)
- **THEN** both responses have an error whose `extensions.code` is `PASSWORD_INVALID` and whose `extensions.field` is `password`

#### Scenario: Special character from the list
- **WHEN** a client registers with `Abcdef1!` (a lowercase letter, an uppercase letter, a digit and `!`)
- **THEN** the registration succeeds

#### Scenario: Unicode characters are allowed
- **WHEN** a client registers with `Ñandú!Ab` (Unicode letters, a special character and no whitespace)
- **THEN** the registration succeeds

#### Scenario: Password with whitespace
- **WHEN** a client registers with a password that contains a space between its characters
- **THEN** the response has an error whose `extensions.code` is `PASSWORD_INVALID` and whose `extensions.field` is `password`

#### Scenario: Password with leading or trailing whitespace
- **WHEN** a client registers with a password that starts or ends with a space
- **THEN** the response has an error whose `extensions.code` is `PASSWORD_INVALID` and whose `extensions.field` is `password`

### Requirement: Passwords are stored only as salted hashes
The system SHALL store only a salted, adaptive hash of each password and SHALL never store, return or log the plain password.

#### Scenario: Stored password is not the plain text
- **WHEN** a client registers with password `correct horse battery`
- **THEN** the stored password value is not `correct horse battery` and does not contain it
- **AND** two accounts registered with the same password have different stored values

### Requirement: Read own account
The GraphQL query `me: User!` SHALL return the account of the user identified by the access token, with `id`, `email`, `username`, `createdAt`, `isFirstVisit`, `firstName`, `lastName`, `subscription`, `nationality`, `birthCountry` and `role`. Without an access token it SHALL fail with an error whose `extensions.classification` is `UNAUTHORIZED` and SHALL return no account data.

#### Scenario: Signed-in user reads their account
- **WHEN** a client sends `{ me { id email username isFirstVisit firstName lastName subscription nationality birthCountry } }` with a valid access token for `ana@example.com`
- **THEN** `data.me` contains the id, email, username, `isFirstVisit`, the profile fields and `subscription` of that account

#### Scenario: Signed-in user reads their role
- **WHEN** a client sends `{ me { role } }` with a valid access token for `ana@example.com`
- **THEN** `data.me.role` is `USER` for an account that has not been given the administrator role

#### Scenario: Anonymous client reads me
- **WHEN** a client sends `{ me { id } }` without an access token
- **THEN** the response has an error whose `extensions.classification` is `UNAUTHORIZED`
- **AND** the response contains no `data.me` account

#### Scenario: Me has no displayName field
- **WHEN** a client sends `{ me { displayName } }` with a valid access token
- **THEN** the response has a GraphQL validation error, because `User` has no `displayName` field

### Requirement: Registration error codes are documented in the contract
The GraphQL contract SHALL document every `extensions.code` that the `register` mutation can return: `EMAIL_TAKEN`, `USERNAME_TAKEN`, `VALIDATION_FAILED` and `PASSWORD_INVALID`. For `VALIDATION_FAILED` and `PASSWORD_INVALID` the contract SHALL document that `extensions.field` names the offending input field. The contract SHALL NOT document any other code for `register`.

#### Scenario: Contract lists the register error codes
- **WHEN** a test reads `contracts/graphql/account.graphqls`
- **THEN** the description of the `register` mutation names `EMAIL_TAKEN`, `USERNAME_TAKEN`, `VALIDATION_FAILED` and `PASSWORD_INVALID`
- **AND** it states that `VALIDATION_FAILED` and `PASSWORD_INVALID` carry `extensions.field`

#### Scenario: Duplicate username returns its own code
- **WHEN** two registrations race for the same generated username and the second one reaches the database after the first one has committed
- **THEN** the second response has an error whose `extensions.code` is `USERNAME_TAKEN`
- **AND** the response does not have the code `EMAIL_TAKEN`

#### Scenario: Taken email returns its own code
- **WHEN** an account with email `ana@example.com` exists and a client registers with that email
- **THEN** the response has an error whose `extensions.code` is `EMAIL_TAKEN`

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


### Requirement: Complete first visit

The GraphQL mutation `completeFirstVisit: User!` SHALL set `first_visit` to `false` for the account of the user identified by the access token, and SHALL return that account with `id`, `email`, `username`, `createdAt` and `isFirstVisit`. It SHALL take no arguments. Without an access token it SHALL fail with an error whose `extensions.classification` is `UNAUTHORIZED` and SHALL change nothing. Calling it when the flag is already `false` SHALL change nothing and SHALL NOT return an error.

#### Scenario: Signed-in user completes the first visit
- **WHEN** a client with a valid access token for `ana@example.com` and `isFirstVisit` `true` sends `mutation { completeFirstVisit { id isFirstVisit } }`
- **THEN** the response contains no `errors`
- **AND** `data.completeFirstVisit.isFirstVisit` is `false`
- **AND** the account in `user_account` has `first_visit = false`

#### Scenario: Completing again changes nothing
- **WHEN** a client with a valid access token for an account whose `isFirstVisit` is `false` sends `mutation { completeFirstVisit { isFirstVisit } }`
- **THEN** the response contains no `errors`
- **AND** `data.completeFirstVisit.isFirstVisit` is `false`

#### Scenario: Anonymous client cannot complete the first visit
- **WHEN** a client without an access token sends `mutation { completeFirstVisit { isFirstVisit } }`
- **THEN** the response has an error whose `extensions.classification` is `UNAUTHORIZED`
- **AND** no account changes

#### Scenario: Completion affects only the signed-in account
- **WHEN** account `ana@example.com` completes its first visit, and account `ben@example.com` still has `isFirstVisit` `true`
- **THEN** `ben@example.com` still has `first_visit = true`

#### Scenario: The mutation takes no arguments
- **WHEN** a client sends `mutation { completeFirstVisit(isFirstVisit: true) { isFirstVisit } }` with a valid access token
- **THEN** the response has a GraphQL validation error
- **AND** the account is unchanged

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

