## ADDED Requirements

### Requirement: Word lists are cached in memory

The system SHALL keep an in-memory snapshot of the active entries of `prohibited_word` and `reserved_word`. The snapshot SHALL contain only entries whose active flag is set. A reload SHALL replace the whole snapshot at once, so that a sign-up never sees a partly loaded list.

#### Scenario: Snapshot is loaded at startup
- **WHEN** the API starts with active and inactive entries in both tables
- **THEN** the snapshot contains only the active entries of both tables

#### Scenario: Reload replaces the whole snapshot
- **WHEN** a reload runs while a sign-up is checking a username
- **THEN** the sign-up is checked against either the old snapshot or the new one, never against a mix of both

### Requirement: Word lists are refreshed hourly

The system SHALL reload the snapshot every hour, so that a change to the tables takes effect within one hour, without a redeploy. A reload SHALL log the number of active entries per list and SHALL NOT log any word, candidate or username. If a reload fails, the snapshot SHALL keep the last successful load, and the system SHALL log a warning with the failure reason and no word, candidate or username. The API SHALL NOT accept sign-up requests until the first load has completed.

An empty snapshot is not an error: with no active entries no word matches, and the generated username is kept. An empty snapshot SHALL trigger an immediate reload attempt instead of waiting for the next hour.

#### Scenario: Added word takes effect after the next reload
- **WHEN** a word is added to `prohibited_word` after the snapshot was loaded
- **AND** the hourly reload runs
- **THEN** a sign-up with a username containing that word is replaced by a fallback username

#### Scenario: Failed reload keeps the last good snapshot
- **WHEN** an hourly reload fails because the database is unreachable
- **THEN** sign-ups are checked against the last successful snapshot
- **AND** a warning is logged with the failure reason and no word, candidate or username

#### Scenario: Sign-up waits for the first load
- **WHEN** the API has started but the first load has not completed
- **THEN** sign-up requests are not accepted

#### Scenario: Empty snapshot requests an immediate reload
- **WHEN** the snapshot has no active entries
- **THEN** a reload is requested immediately
- **AND** sign-ups in the meantime keep their generated usernames

#### Scenario: Reload log has counts only
- **WHEN** a reload completes
- **THEN** the log entry contains the number of active entries per list
- **AND** it contains no word, candidate or username

## MODIFIED Requirements

### Requirement: Sign-up applies the word check

At sign-up, the system SHALL check the generated username with the matcher, using the snapshot of active entries held by the cache. On a match, the account SHALL receive a fallback username, as defined in `user-accounts`, and the sign-up SHALL succeed without returning `PROHIBITED_WORD` or `RESERVED_USERNAME`. The matched word and the candidate SHALL NOT appear in the response or the logs. Sign-up SHALL NOT query the word tables.

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

#### Scenario: Sign-up reads the snapshot, not the tables
- **WHEN** a user signs up with an email whose prefix matches a word in the snapshot
- **THEN** the account receives a `user_` fallback username
- **AND** the sign-up does not query `prohibited_word` or `reserved_word`
