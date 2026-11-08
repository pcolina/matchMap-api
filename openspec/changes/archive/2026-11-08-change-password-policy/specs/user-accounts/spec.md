# Spec Delta

## MODIFIED Requirements

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
