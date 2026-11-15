## ADDED Requirements

### Requirement: Sign-up applies the word check
At sign-up, the system SHALL check the generated username with the matcher, using the active entries read from the database at sign-up time. On a match, the account SHALL receive a fallback username, as defined in `user-accounts`, and the sign-up SHALL succeed without returning `PROHIBITED_WORD` or `RESERVED_USERNAME`. The matched word and the candidate SHALL NOT appear in the response or the logs.

#### Scenario: Clean generated username is kept
- **WHEN** a client registers with email `ana@example.com` and no active word matches `ana`
- **THEN** the account username is `ana`

#### Scenario: Prohibited prefix gets a fallback
- **WHEN** a client registers with email `nazi.fan@example.com` and `nazi` is an active prohibited word
- **THEN** the account is created with a fallback username
- **AND** the response does not contain the word `nazi` in any field

#### Scenario: Reserved prefix gets a fallback
- **WHEN** a client registers with email `support@example.com` and `support` is an active reserved word
- **THEN** the account is created with a fallback username
- **AND** the response has no `errors`

#### Scenario: Matched word is not logged
- **WHEN** a sign-up is changed to a fallback username because the generated username matched a word
- **THEN** no log line contains the generated username or the matched word

#### Scenario: Empty word lists
- **WHEN** no active entries exist in `prohibited_word` and `reserved_word`
- **THEN** every sign-up keeps its generated username
