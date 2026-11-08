# Tasks

Each task is one commit with `Task: <id>` in the body. Every task follows TDD: write the failing test, run it and show it failing, then write the minimum code.

## 1. Lock the duplicate-username and taken-email codes

- [ ] 1.1 Write the failing integration test `RegistrationErrorCodesIT.duplicateUsernameReturnsUsernameTaken` (covers scenario "Duplicate username returns its own code"): create an account with `ana_travels`, then register with a new email and `Ana_Travels`, and assert `extensions.code` is `USERNAME_TAKEN`. Run it and show the result. If it already passes, record that the issue's claim that a duplicate username returns `email taken` is not reproduced, and do not change production code.
- [ ] 1.2 Write the failing integration test `RegistrationErrorCodesIT.takenEmailReturnsEmailTaken` (covers scenario "Taken email returns its own code"). Run it and show the result.
- [ ] 1.3 Make 1.1 and 1.2 pass only if they fail. Write the minimum production code needed, and only if a test shows a wrong code. Otherwise make no production change and commit only the tests.

## 2. Document the codes in the contract

- [x] 2.1 Write the failing test `AccountContractTest.registerDescriptionNamesTheStableCodes` (covers scenario "Contract lists the register error codes"): read `contracts/graphql/account.graphqls` and assert that the `register` description names `EMAIL_TAKEN`, `USERNAME_TAKEN`, `VALIDATION_FAILED` and `extensions.field`. Run it and show it failing.
- [x] 2.2 Add the description to `register` in `contracts/graphql/account.graphqls` with the three codes and the `extensions.field` rule. Run the test until it passes.
- [x] 2.3 Refactor: keep the contract text and the test assertions in one place, with no duplicated lists.

## 3. Full check

- [ ] 3.1 Run `.\gradlew.bat spotlessApply`, then `.\gradlew.bat check`, and paste the summary in the PR.
- [ ] 3.2 Confirm that `contracts/` and the implementation are in sync, and that no `[NEEDS CLARIFICATION]` remains in the artifacts.
