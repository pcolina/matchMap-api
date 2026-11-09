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
A username SHALL have 3 to 30 characters from `a-z`, `0-9` and `_`, SHALL be stored lowercased and SHALL be unique ignoring case. The system SHALL generate the username for each new account from the email prefix: the prefix is lowercased, every character outside `a-z`, `0-9` and `_` is replaced by `_`, and the result is cut at 30 characters. If the prefix has fewer than 3 characters, the username is `user_` followed by 8 random characters from `a-z` and `0-9`. No client sends a username.

#### Scenario: Username already in use
- **WHEN** an account with username `ana` exists and a client registers with email `ana@other.example`
- **THEN** the stored username of the new account is `ana_2` (assumption: the suffix counts up from 2)

#### Scenario: Invalid username
- **WHEN** a client registers with email `a@example.com`, whose prefix is shorter than 3 characters
- **THEN** the generated username is `user_` followed by 8 random characters from `a-z` and `0-9`, and follows the username rules

#### Scenario: Invalid display name
- **WHEN** a client sends `displayName` in `register`
- **THEN** the response has a GraphQL validation error, because the field no longer exists

#### Scenario: Username is lowercased and unique ignoring case
- **WHEN** an account is registered with email `Ana.Travels@Example.com`
- **THEN** the stored username is `ana_travels`

#### Scenario: Username follows the length limits
- **WHEN** a client registers with an email whose prefix is longer than 30 characters
- **THEN** the generated username has at most 30 characters

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
The GraphQL query `me: User!` SHALL return the account of the user identified by the access token, with `id`, `email` and `username`. Without an access token it SHALL fail with an error whose `extensions.classification` is `UNAUTHORIZED` and SHALL return no account data.

#### Scenario: Signed-in user reads their account
- **WHEN** a client sends `{ me { id email username } }` with a valid access token for `ana@example.com`
- **THEN** `data.me` contains the id, email and username of that account

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
