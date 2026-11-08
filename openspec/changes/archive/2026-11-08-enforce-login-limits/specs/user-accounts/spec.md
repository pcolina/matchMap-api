# Spec Delta

## Purpose

Aligns the password limits of registration with the limits of sign-in, so that every password an account can be created with can also be used to sign in.

## MODIFIED Requirements

### Requirement: Password policy
A password SHALL have at least 12 and at most 20 characters. A password SHALL NOT contain any whitespace character, including spaces, tabs and line breaks, at any position. Any other character SHALL be allowed, including Unicode, and no composition rules SHALL apply. A password that breaks the policy SHALL be rejected with `VALIDATION_FAILED` and `extensions.field` `password`.

#### Scenario: Password too short
- **WHEN** a client registers with a password of 11 characters
- **THEN** the response has an error whose `extensions.code` is `VALIDATION_FAILED` and whose `extensions.field` is `password`

#### Scenario: Password too long
- **WHEN** a client registers with a password of 21 characters
- **THEN** the response has an error whose `extensions.code` is `VALIDATION_FAILED` and whose `extensions.field` is `password`

#### Scenario: Password at the limits
- **WHEN** a client registers with a password of 12 characters and another with a password of 20 characters, both without whitespace
- **THEN** both registrations succeed

#### Scenario: Password with whitespace
- **WHEN** a client registers with a password that contains a space between its characters
- **THEN** the response has an error whose `extensions.code` is `VALIDATION_FAILED` and whose `extensions.field` is `password`

#### Scenario: Password with leading or trailing whitespace
- **WHEN** a client registers with a password that starts or ends with a space
- **THEN** the response has an error whose `extensions.code` is `VALIDATION_FAILED` and whose `extensions.field` is `password`
