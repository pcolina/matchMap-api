## Purpose

Lets administrators add, list and deactivate the prohibited words used by the username check, through the API and for the ADMIN role only.

## ADDED Requirements

### Requirement: Administrators create prohibited words

An administrator SHALL be able to create a prohibited word with a category and a language. The stored word SHALL keep the text as given. Its normalized form SHALL be computed by the same normalizer the username check uses. A new word SHALL be active.

#### Scenario: Administrator creates a prohibited word
- **WHEN** an administrator creates the word `Example` with category `HATE` and language `en`
- **THEN** the word is stored as `Example`, with its normalized form and the active flag set

### Requirement: Duplicate words are rejected

A word whose normalized form and category already exist SHALL be rejected with `extensions.code` `DUPLICATE_WORD`. The rejection SHALL add no row and SHALL NOT return the word in the error message.

#### Scenario: Duplicate word is rejected
- **WHEN** an administrator creates a word whose normalized form and category already exist
- **THEN** the request fails with `extensions.code` `DUPLICATE_WORD`
- **AND** no row is added and the error message does not contain the word

### Requirement: Invalid words are rejected

A word SHALL be between 1 and 100 characters after trimming, and a language SHALL be between 2 and 5 characters. An invalid word SHALL fail with `extensions.code` `VALIDATION_FAILED` and `extensions.field` `word`, and no row is added.

#### Scenario: Invalid word is rejected
- **WHEN** an administrator creates a word that is blank or longer than 100 characters
- **THEN** the request fails with `extensions.code` `VALIDATION_FAILED` and `extensions.field` `word`
- **AND** no row is added

### Requirement: New words apply at once

After each write the word lists SHALL be reloaded, so a change applies to the next username check without waiting for the scheduled reload.

#### Scenario: New word applies at once
- **WHEN** an administrator creates a word and a username containing it is checked
- **THEN** the username is blocked at once

### Requirement: Administrators list the prohibited words

An administrator SHALL be able to list every prohibited word, active and inactive, in a single list ordered by category and word.

#### Scenario: Administrator lists the prohibited words
- **WHEN** an administrator asks for the list
- **THEN** the response contains every prohibited word, with its category, language and active flag

### Requirement: Administrators deactivate a word

An administrator SHALL be able to set a word active or inactive. A word SHALL NOT be deleted. An inactive word SHALL NOT block usernames.

#### Scenario: Administrator deactivates a word
- **WHEN** an administrator sets an active word to inactive
- **THEN** the word is still stored with the active flag unset
- **AND** a username containing it is no longer blocked

#### Scenario: Unknown word id is rejected
- **WHEN** an administrator sets the active flag of an id that does not exist
- **THEN** the request fails with `extensions.code` `VALIDATION_FAILED` and `extensions.field` `id`

### Requirement: Only administrators manage the list

Every operation of this capability SHALL require the ADMIN role. A request from another account SHALL be rejected with `extensions.classification` `FORBIDDEN` and SHALL change nothing.

#### Scenario: Only administrators manage the list
- **WHEN** an account without the ADMIN role creates, lists or changes a word
- **THEN** the request is rejected with `extensions.classification` `FORBIDDEN`
