# username-moderation Specification

## Purpose
This capability decides which usernames are rejected for containing a prohibited or reserved word. It owns the two word lists, `prohibited_word` and `reserved_word`, and only their active rows are used. A candidate is normalized before matching: lowercase, diacritics removed, leetspeak and homoglyphs mapped to letters, separators and repeated letters removed. A word longer than four characters matches as a substring; a shorter word matches as the whole candidate or as one segment separated by `.`, `_`, `-` or a space. At sign-up, a generated username that matches is replaced by a fallback username, and the sign-up succeeds without returning an error.

## Requirements

### Requirement: Prohibited and reserved word lists

The system SHALL store prohibited words and reserved words in two separate tables. Each entry SHALL have a word, a normalized form, a category, a language, an active flag and creation and update times. The categories SHALL be `CHILD_SAFETY`, `HATE`, `VIOLENCE`, `SEXUAL`, `IMPERSONATION` and `RESERVED`. The `reserved_word` table SHALL be seeded by migration with the approved reserved names, each with category `RESERVED` and the active flag set. The `prohibited_word` table SHALL be created empty, and no migration SHALL insert a prohibited word. Only entries with the active flag set SHALL be used for matching.

#### Scenario: Tables are created empty
- **WHEN** the schema is migrated on an empty database
- **THEN** the tables `prohibited_word` and `reserved_word` exist
- **AND** `prohibited_word` contains no rows

#### Scenario: Inactive entries are ignored
- **WHEN** a candidate contains a word whose entry has the active flag unset
- **THEN** the matcher does not report a match for that word

#### Scenario: Approved reserved names are seeded
- **WHEN** the migrations run on an empty database
- **THEN** `reserved_word` contains the names `admin`, `administrator`, `administrador`, `root`, `superadmin`, `support`, `soporte`, `staff`, `moderator`, `moderador`, `matchmap`, `system` and `sistema`
- **AND** each row has category `RESERVED` and the active flag set

#### Scenario: Seeded names are stored normalized
- **WHEN** the migrations run on an empty database
- **THEN** the row for `support` has the normalized form `suport`
- **AND** the row for `root` has the normalized form `rot`
- **AND** the row for `staff` has the normalized form `staf`

#### Scenario: Sign-up with a seeded name gets a fallback
- **WHEN** a client registers with email `admin@example.com`
- **THEN** the account is created with a fallback username
- **AND** the account username is not `admin`

### Requirement: Candidate normalization
Before matching, the system SHALL normalize a candidate username as follows, in this order: apply Unicode NFKC, convert to lowercase, remove diacritics, replace each leetspeak or homoglyph character with the letter it imitates (`0` becomes `o`, `1` becomes `i`, `3` becomes `e`, `4` becomes `a`, `5` becomes `s`, `7` becomes `t`, `@` becomes `a`, `$` becomes `s`), remove the separators `.`, `_`, `-` and space, and then collapse each run of the same letter into one letter.

#### Scenario: Leetspeak is normalized
- **WHEN** the candidate is `naz1`
- **THEN** the normalized candidate is `nazi`

#### Scenario: Separators are removed
- **WHEN** the candidate is `n-a-z-i`
- **THEN** the normalized candidate is `nazi`

#### Scenario: Repeated letters are collapsed
- **WHEN** the candidate is `nnnazi`
- **THEN** the normalized candidate is `nazi`

#### Scenario: Case and diacritics are normalized
- **WHEN** the candidate is `NÁZI`
- **THEN** the normalized candidate is `nazi`

#### Scenario: Digits and letters are combined
- **WHEN** the candidate is `n4z1`
- **THEN** the normalized candidate is `nazi`

### Requirement: Matching rule by word length
A prohibited or reserved word longer than 4 characters SHALL match when it appears as a substring of the normalized candidate. A word of 4 characters or fewer SHALL match when it is the whole normalized candidate, or when it equals one of the segments of the candidate that are separated by `.`, `_`, `-` or a space. A segment is taken before the separators are removed, after the other normalization steps. Length is measured on the normalized form of the word.

#### Scenario: Long word inside a longer candidate
- **WHEN** an active prohibited word `nazis` (5 characters) exists and the candidate is `xnazisx`
- **THEN** the matcher reports a match

#### Scenario: Short word inside a longer candidate
- **WHEN** an active prohibited word `ass` (3 characters) exists and the candidate is `classic`
- **THEN** the matcher does not report a match

#### Scenario: Short word as a separated segment
- **WHEN** an active prohibited word `nazi` (4 characters) exists and the candidate is `ana_nazi`
- **THEN** the matcher reports a match with category `HATE`

#### Scenario: Short word split by separators
- **WHEN** an active prohibited word `nazi` (4 characters) exists and the candidate is `n-a-z-i`
- **THEN** the matcher reports a match with category `HATE`

#### Scenario: Short word as the whole candidate
- **WHEN** an active reserved word `root` (4 characters) exists and the candidate is `r00t`
- **THEN** the matcher reports a match with category `RESERVED`

### Requirement: Match result
The matcher SHALL report, for a candidate, whether it matched and, if it matched, the category of the matched entry. The result SHALL NOT be exposed to the client by this capability. The matcher SHALL NOT log the candidate.

#### Scenario: No match
- **WHEN** the candidate contains no active prohibited or reserved word
- **THEN** the matcher reports no match

#### Scenario: Match reports the category
- **WHEN** the candidate contains an active prohibited word of category `HATE`
- **THEN** the matcher reports a match with category `HATE`

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

