# user-accounts Specification

## Purpose

Lets people create a MatchMap account with email and password and read their own account, enforcing the rules that keep each account's identifiers unique and its password safe.

## Requirements

### Requirement: Register with email and password
The GraphQL mutation `register(input: RegisterInput!): User!` SHALL be public and SHALL create an account from `email`, `username`, `displayName` and `password`. It SHALL return the new account's `id` (UUID), `email`, `username`, `displayName` and `createdAt`. It SHALL NOT sign the user in; the client signs in afterwards.

#### Scenario: Successful registration
- **WHEN** an anonymous client sends `register` with email `ana@example.com`, username `ana_travels`, display name `Ana` and a valid password
- **THEN** the response contains no `errors`
- **AND** `data.register` contains a UUID `id`, email `ana@example.com`, username `ana_travels`, display name `Ana` and a `createdAt` timestamp

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

### Requirement: Username and display name rules
A username SHALL have 3 to 30 characters from `a-z`, `0-9` and `_`, SHALL be stored lowercased and SHALL be unique ignoring case. A display name SHALL be trimmed and SHALL have 1 to 50 characters. A username already in use SHALL be rejected with `extensions.code` `USERNAME_TAKEN`; a username or display name that breaks the rules SHALL be rejected with `VALIDATION_FAILED` and the offending `extensions.field`.

#### Scenario: Username already in use
- **WHEN** an account with username `ana_travels` exists and a client registers with username `Ana_Travels`
- **THEN** the response has an error whose `extensions.code` is `USERNAME_TAKEN`

#### Scenario: Invalid username
- **WHEN** a client registers with username `a`
- **THEN** the response has an error whose `extensions.code` is `VALIDATION_FAILED` and whose `extensions.field` is `username`

#### Scenario: Invalid display name
- **WHEN** a client registers with a display name made only of spaces
- **THEN** the response has an error whose `extensions.code` is `VALIDATION_FAILED` and whose `extensions.field` is `displayName`

### Requirement: Password policy
A password SHALL have at least 12 and at most 128 characters. Any character SHALL be allowed, including spaces and Unicode, and no composition rules SHALL apply. A password that breaks the policy SHALL be rejected with `VALIDATION_FAILED` and `extensions.field` `password`.

#### Scenario: Password too short
- **WHEN** a client registers with a password of 11 characters
- **THEN** the response has an error whose `extensions.code` is `VALIDATION_FAILED` and whose `extensions.field` is `password`

#### Scenario: Password too long
- **WHEN** a client registers with a password of 129 characters
- **THEN** the response has an error whose `extensions.code` is `VALIDATION_FAILED` and whose `extensions.field` is `password`

### Requirement: Passwords are stored only as salted hashes
The system SHALL store only a salted, adaptive hash of each password and SHALL never store, return or log the plain password.

#### Scenario: Stored password is not the plain text
- **WHEN** a client registers with password `correct horse battery`
- **THEN** the stored password value is not `correct horse battery` and does not contain it
- **AND** two accounts registered with the same password have different stored values

### Requirement: Read own account
The GraphQL query `me: User!` SHALL return the account of the user identified by the access token. Without an access token it SHALL fail with an error whose `extensions.classification` is `UNAUTHORIZED` and SHALL return no account data.

#### Scenario: Signed-in user reads their account
- **WHEN** a client sends `{ me { id email username displayName } }` with a valid access token for `ana@example.com`
- **THEN** `data.me` contains the id, email, username and display name of that account

#### Scenario: Anonymous client reads me
- **WHEN** a client sends `{ me { id } }` without an access token
- **THEN** the response has an error whose `extensions.classification` is `UNAUTHORIZED`
- **AND** the response contains no `data.me` account
