# Spec Delta

## ADDED Requirements

### Requirement: Registration error codes are documented in the contract
The GraphQL contract SHALL document every `extensions.code` that the `register` mutation can return: `EMAIL_TAKEN`, `USERNAME_TAKEN` and `VALIDATION_FAILED`. For `VALIDATION_FAILED` the contract SHALL document that `extensions.field` names the offending input field. The contract SHALL NOT document any other code for `register`. No registration behaviour changes in this requirement.

#### Scenario: Contract lists the register error codes
- **WHEN** a test reads `contracts/graphql/account.graphqls`
- **THEN** the description of the `register` mutation names `EMAIL_TAKEN`, `USERNAME_TAKEN` and `VALIDATION_FAILED`
- **AND** it states that `VALIDATION_FAILED` carries `extensions.field`

#### Scenario: Duplicate username returns its own code
- **WHEN** an account with username `ana_travels` exists and a client registers with a new email and username `Ana_Travels`
- **THEN** the response has an error whose `extensions.code` is `USERNAME_TAKEN`
- **AND** the response does not have the code `EMAIL_TAKEN`

#### Scenario: Taken email returns its own code
- **WHEN** an account with email `ana@example.com` exists and a client registers with that email and a new username
- **THEN** the response has an error whose `extensions.code` is `EMAIL_TAKEN`
