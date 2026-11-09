# Tasks

Tasks marked [BLOCKED] wait for the [NEEDS CLARIFICATION] answers in proposal.md and design.md. They are not final until those answers are in.

## 1. Contract

- [x] 1.1 Write the failing contract test `AccountContractIT.registerDescriptionListsRegisterErrorCodes` that reads `contracts/graphql/account.graphqls` and expects the `register` description to name `EMAIL_TAKEN`, `USERNAME_TAKEN`, `VALIDATION_FAILED` and `PASSWORD_INVALID`, and to state that `VALIDATION_FAILED` and `PASSWORD_INVALID` carry `extensions.field`. Run it and show it failing. Then update the description in `contracts/graphql/account.graphqls`. Verify the test passes.
- [x] 1.2 Write the failing contract test `AccountContractIT.registerInputHasOnlyEmailAndPassword` that reads the schema and expects `RegisterInput` to contain exactly `email` and `password`, both non-null. Run it and show it failing. Then remove `username` and `displayName` from `RegisterInput` in `contracts/graphql/account.graphqls`. Verify the test passes.
- [x] 1.3 Write the failing contract test `AccountContractIT.userTypeHasNoDisplayName` that reads the schema and expects the `User` type to contain `id`, `email`, `username` and `createdAt`, and no `displayName`. Run it and show it failing. Then remove `displayName` from `User` in `contracts/graphql/account.graphqls`. Verify the test passes.

## 2. Registration

- [x] 2.0 Write the failing Testcontainers test `PersistenceIT.userAccountHasNoDisplayNameColumn` that checks `information_schema.columns` for `user_account` and expects no `display_name` column, run it and show it failing. Then add `V3__drop_display_name.sql` with `ALTER TABLE user_account DROP COLUMN display_name;` and remove the `displayName` field and constructor argument from `UserAccount`. Verify the test passes. (Moved before 2.1: the NOT NULL column blocks any registration insert.)
- [x] 2.1 Write the failing test `RegisterIT.successfulRegistrationGeneratesUsernameFromEmail` for the scenario "Successful registration", and run it and show it failing. Then change `RegisterInput` to `(email, password)`, make `RegistrationService` call the username generator, and remove `username` and `displayName` from the `Registration` record. Verify the test passes.
- [x] 2.2 Write the failing test `RegisterIT.inputWithRemovedFieldIsRejected` for the scenario "Input with a field that no longer exists", and run it and show it failing. Then verify the test passes with the contract change from 1.2.
- [x] 2.3 Write the failing test `RegisterIT.takenGeneratedUsernameGetsCountingSuffix` for the scenario "Generated username is already in use", expecting `ana_2`, and run it and show it failing. Then implement the counting suffix. Verify the test passes.
- [x] 2.4 Write the failing test `RegisterIT.shortEmailPrefixGetsRandomUsername` for the scenario "Short email prefix", and run it and show it failing. Then implement the `user_` rule. Verify the test passes.
- [x] 2.5 Write the failing test `RegisterIT.uppercaseAndDotsAreSanitizedInUsername` for the scenario "Username is lowercased and unique ignoring case", and run it and show it failing. Then implement the sanitization. Verify the test passes.
- [x] 2.6 Write the failing test `RegisterIT.longPrefixIsCutAt30Characters` for the scenario "Username follows the length limits", and run it and show it failing. Then implement the cut. Verify the test passes.
- [x] 2.7 Write the failing test `RegisterIT.passwordRuleBreakReturnsPasswordInvalid` for the password scenarios in the "Password policy" requirement, run it and show it failing, then map the password violations to `PASSWORD_INVALID` with `extensions.field` `password` in `AccountException` and `RegistrationService.validate`. Verify the test passes.

## 3. Read own account

- [x] 3.1 Write the failing test `MeQueryIT.meReturnsIdEmailAndUsername` for the scenario "Signed-in user reads their account", and run it and show it failing. Then make `me` return `id`, `email` and `username`. Verify the test passes.
- [x] 3.2 Update `MeQueryIT` and `GraphqlRequests` so that no test sends or reads `displayName` for `register` or `me`. Verify the affected tests pass.

## 4. Google sign-in

- [ ] 4.2 Write the failing test `GoogleSignInIT.newAccountUsernameComesFromGoogleName` for the scenario "First Google sign-in creates an account", expecting `carla`, and run it and show it failing. Then make `GoogleAccountService` pass the Google name to `UsernameGenerator` and stop storing a display name. Verify the test passes.
- [ ] 4.3 Write the failing tests `GoogleSignInIT.nameWithSpacesIsSanitized` and `GoogleSignInIT.takenGoogleNameGetsSuffix` for the scenarios "Google name with characters outside the username set" and "Google name already used as username", run them and show them failing, then verify they pass.
- [ ] 4.4 Write the failing test `GoogleSignInIT.nameLessProfileUsesEmailPrefix` for the scenario "Google profile without a name", run it and show it failing. Then make `GoogleAccountService` fall back to the email prefix when the name is empty. Verify the test passes.

## 5. Full check

- [ ] 5.1 Run `.\gradlew.bat spotlessApply` and then `.\gradlew.bat check`. Verify that both pass, and paste the summary in the pull request. `ArchiveCheckTest` runs in the pull request workflow, not as a task here.

## Workflow follow-up

- Archive the change after the project's review requirements are satisfied.
- Open the pull request with `Closes #13` in its description.
