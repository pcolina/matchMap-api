# username-moderation Specification

## Purpose
TBD - created by archiving change add-prohibited-word-matcher. Update Purpose after archive.

## Requirements

### Requirement: Prohibited and reserved word lists
The system SHALL store prohibited words and reserved words in two separate tables. Each entry SHALL have a word, a normalized form, a category, a language, an active flag and creation and update times. The categories SHALL be `CHILD_SAFETY`, `HATE`, `VIOLENCE`, `SEXUAL`, `IMPERSONATION` and `RESERVED`. The tables SHALL be created empty: the schema change SHALL NOT insert any word. Only entries with the active flag set SHALL be used for matching.

#### Scenario: Tables are created empty
- **WHEN** the schema is migrated on an empty database
- **THEN** the tables `prohibited_word` and `reserved_word` exist and contain no rows

#### Scenario: Inactive entries are ignored
- **WHEN** a candidate contains a word whose entry has the active flag unset
- **THEN** the matcher does not report a match for that word

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
