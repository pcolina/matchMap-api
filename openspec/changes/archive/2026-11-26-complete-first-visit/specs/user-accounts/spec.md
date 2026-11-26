# Spec Delta: user-accounts

## ADDED Requirements

### Requirement: Complete first visit

The GraphQL mutation `completeFirstVisit: User!` SHALL set `first_visit` to `false` for the account of the user identified by the access token, and SHALL return that account with `id`, `email`, `username`, `createdAt` and `isFirstVisit`. It SHALL take no arguments. Without an access token it SHALL fail with an error whose `extensions.classification` is `UNAUTHORIZED` and SHALL change nothing. Calling it when the flag is already `false` SHALL change nothing and SHALL NOT return an error.

#### Scenario: Signed-in user completes the first visit
- **WHEN** a client with a valid access token for `ana@example.com` and `isFirstVisit` `true` sends `mutation { completeFirstVisit { id isFirstVisit } }`
- **THEN** the response contains no `errors`
- **AND** `data.completeFirstVisit.isFirstVisit` is `false`
- **AND** the account in `user_account` has `first_visit = false`

#### Scenario: Completing again changes nothing
- **WHEN** a client with a valid access token for an account whose `isFirstVisit` is `false` sends `mutation { completeFirstVisit { isFirstVisit } }`
- **THEN** the response contains no `errors`
- **AND** `data.completeFirstVisit.isFirstVisit` is `false`

#### Scenario: Anonymous client cannot complete the first visit
- **WHEN** a client without an access token sends `mutation { completeFirstVisit { isFirstVisit } }`
- **THEN** the response has an error whose `extensions.classification` is `UNAUTHORIZED`
- **AND** no account changes

#### Scenario: Completion affects only the signed-in account
- **WHEN** account `ana@example.com` completes its first visit, and account `ben@example.com` still has `isFirstVisit` `true`
- **THEN** `ben@example.com` still has `first_visit = true`

#### Scenario: The mutation takes no arguments
- **WHEN** a client sends `mutation { completeFirstVisit(isFirstVisit: true) { isFirstVisit } }` with a valid access token
- **THEN** the response has a GraphQL validation error
- **AND** the account is unchanged
