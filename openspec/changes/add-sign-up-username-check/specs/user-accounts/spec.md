## MODIFIED Requirements

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
