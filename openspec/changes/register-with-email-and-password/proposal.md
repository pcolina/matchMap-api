# Proposal

Closes #13

## Why

The registration form asks only for an email and a password. The API still requires `username` and `displayName` in `RegisterInput` and returns `displayName` on `User`, so the client cannot register until the contract matches the form. The backend must generate the username from the email and the client only sees the username.

## What Changes

- `RegisterInput` has exactly `email` and `password`, both required. **BREAKING**: `username` and `displayName` are removed from the input of `register`.
- `User` loses `displayName`. **BREAKING**: `register` and `me` no longer return `displayName`. The client identifies an account by `username`.
- The backend generates `username` from the part of the email before the `@`: the prefix is lowercased, characters outside `a-z`, `0-9` and `_` are replaced by `_`, and the result is cut at 30 characters. If the prefix has fewer than 3 characters, the username is `user_` followed by 8 random characters from `a-z` and `0-9`.
- If the generated username is already taken (ignoring case), the backend appends `_<n>`, with `n` counting up from 2 (assumption, as in the current generator). `ana` taken gives `ana_2`.
- `register` error codes: `EMAIL_TAKEN`, `USERNAME_TAKEN`, `VALIDATION_FAILED` with `extensions.field`, and `PASSWORD_INVALID` with `extensions.field: password`. **BREAKING** for clients that read `VALIDATION_FAILED` for the password.
- `contracts/graphql/account.graphqls` is updated first (schema-first), and the description of `register` is updated to match.

### Open points

- Decided: the `display_name` column is dropped by a new append-only Flyway migration (`V3__drop_display_name.sql`). The applied `V2__create_user_accounts.sql` is not edited.
- Decided: a Google account's username is generated from the Google profile name, sanitized as the email prefix is, with `_<n>` if taken. The profile name is not stored.
- Decided: a Google profile without a name falls back to the part of the email before the `@`, as the current code does. The sign-in succeeds.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `user-accounts`: the register input and response change, the username rules change, `displayName` is removed from `User` and from `me`, and the registration error codes change. The password requirement's code changes from `VALIDATION_FAILED` to `PASSWORD_INVALID`.
- `authentication`: the "Sign in with Google" requirement changes: the new account's username comes from the Google profile name instead of the email, with the same sanitization and suffix rules. The stored display name is removed.

## Out of scope

- Client changes: the `add-registration-page` change, `pnpm codegen`, and the web and mobile forms. Issue #13 refers to `openspec/changes/add-registration-page/`, which does not exist in this repository, so it is not used here.
- Password rule itself: already merged in PR #24 (issue #10).
- Stable error codes for sign-in: already merged (issue #9).
- Uniqueness of display names (issue #11): not part of this change, since `displayName` is removed.
- Accounts already stored: this change does not rewrite existing usernames unless a decision above requires it.
- The uncommitted `README.md` change (Swagger UI section for `contracts/openapi/auth.yaml`). It is not part of this change and stays uncommitted on this branch until the user decides where it goes.

## Depends on

- `user-accounts`: register, username rules, password policy, and the registration error codes.
- `authentication`: Google sign-in, which uses the same username generator.
- Issue #10 (password rule, merged in PR #24) and issue #9 (stable error codes, closed).

## Impact

- Contract: `contracts/graphql/account.graphqls` (`RegisterInput`, `User`, and the description of `register`).
- Code: `RegisterInput`, `RegistrationService`, `UserAccount`, `GoogleAccountService` (depends on the storage question), `UsernameGenerator`, `AccountException`, and `AccountGraphqlController`.
- Database: a new append-only Flyway migration `V3__drop_display_name.sql` drops `user_account.display_name`. The applied `V2__create_user_accounts.sql` is not edited.
- Tests: `RegisterIT`, `MeQueryIT`, `GoogleSignInIT`, and `GraphqlRequests` use `displayName` and must be updated.
- Clients: web and mobile consume the new contract in their own repositories. Nothing is deployed.
