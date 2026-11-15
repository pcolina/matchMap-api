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
