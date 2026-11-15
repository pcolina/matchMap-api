## MODIFIED Requirements

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
