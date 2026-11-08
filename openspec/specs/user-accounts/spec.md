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
A password SHALL have at least 6 and at most 20 characters. A password SHALL contain at least one lowercase letter (`a-z`), at least one uppercase letter (`A-Z`) and at least one special character. A special character is one from the OWASP list of ASCII punctuation and symbols: `!"#$%&'()*+,-./:;<=>?@[\]^_`{|}~`. Letters and digits are not special characters. A password SHALL NOT contain any whitespace character, including spaces, tabs and line breaks, at any position. Any other character, including Unicode, SHALL be allowed. A password that breaks the policy SHALL be rejected with `VALIDATION_FAILED` and `extensions.field` `password`.

#### Scenario: Password too short
- **WHEN** a client registers with a password of 5 characters that meets all the other rules
- **THEN** the response has an error whose `extensions.code` is `VALIDATION_FAILED` and whose `extensions.field` is `password`

#### Scenario: Password too long
- **WHEN** a client registers with a password of 21 characters that meets all the other rules
- **THEN** the response has an error whose `extensions.code` is `VALIDATION_FAILED` and whose `extensions.field` is `password`

#### Scenario: Password at the limits
- **WHEN** a client registers with a password of 6 characters and another with a password of 20 characters, both with a lowercase letter, an uppercase letter, a special character and no whitespace
- **THEN** both registrations succeed

#### Scenario: Password without a lowercase letter
- **WHEN** a client registers with `ABCDEF!1` (no lowercase letter)
- **THEN** the response has an error whose `extensions.code` is `VALIDATION_FAILED` and whose `extensions.field` is `password`

#### Scenario: Password without an uppercase letter
- **WHEN** a client registers with `abcdef!1` (no uppercase letter)
- **THEN** the response has an error whose `extensions.code` is `VALIDATION_FAILED` and whose `extensions.field` is `password`

#### Scenario: Password without a special character
- **WHEN** a client registers with `Abcdef12` (no special character)
- **THEN** the response has an error whose `extensions.code` is `VALIDATION_FAILED` and whose `extensions.field` is `password`

#### Scenario: Letters and digits are not special characters
- **WHEN** a client registers with `Abcdef12` and with `Abcdef1é` (neither has a special character)
- **THEN** both responses have an error whose `extensions.code` is `VALIDATION_FAILED` and whose `extensions.field` is `password`

#### Scenario: Special character from the list
- **WHEN** a client registers with `Abcdef1!` (a lowercase letter, an uppercase letter, a digit and `!`)
- **THEN** the registration succeeds

#### Scenario: Unicode characters are allowed
- **WHEN** a client registers with `Ñandú!Ab` (Unicode letters, a special character and no whitespace)
- **THEN** the registration succeeds

#### Scenario: Password with whitespace
- **WHEN** a client registers with a password that contains a space between its characters
- **THEN** the response has an error whose `extensions.code` is `VALIDATION_FAILED` and whose `extensions.field` is `password`

#### Scenario: Password with leading or trailing whitespace
- **WHEN** a client registers with a password that starts or ends with a space
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

### Requirement: Registration error codes are documented in the contract
The GraphQL contract SHALL document every `extensions.code` that the `register` mutation can return: `EMAIL_TAKEN`, `USERNAME_TAKEN` and `VALIDATION_FAILED`. For `VALIDATION_FAILED` the contract SHALL document that `extensions.field` names the offending input field. The contract SHALL NOT document any other code for `register`. No registration behaviour changes in this requirement.

#### Scenario: Contract lists the register error codes
- **WHEN** a test reads `contracts/graphql/account.graphqls`
- **THEN** the description of the `register` mutation names `EMAIL_TAKEN`, `USERNAME_TAKEN` and `VALIDATION_FAILED`
- **AND** it states that `VALIDATION_FAILED` carries `extensions.field`

#### Scenario: Duplicate username returns its own code
- **WHEN** an account with username `ana_travels` exists and a client registers with a new email and username `Ana_Travels`
- **THEN** the response has an error whose `extensions.code` is `USERNAME_TAKEN`
- **AND** the response does not have the code `EMAIL_TAKEN`

#### Scenario: Taken email returns its own code
- **WHEN** an account with email `ana@example.com` exists and a client registers with that email and a new username
- **THEN** the response has an error whose `extensions.code` is `EMAIL_TAKEN`
