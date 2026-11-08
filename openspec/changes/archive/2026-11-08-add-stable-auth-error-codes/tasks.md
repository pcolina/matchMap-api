# Tasks

Each task is one commit with `Task: <id>` in the body. Every task follows TDD: write the failing test, run it and show it failing, then write the minimum code.

## 1. Lock the duplicate-username and taken-email codes

- [x] 1.1 Covers scenario "Duplicate username returns its own code" with the existing test `RegisterIT.rejectsTakenUsername` (`src/test/java/com/matchmap/api/account/RegisterIT.java`). It registers `taken_<suffix>` and then `TAKEN_<suffix>` and asserts `extensions.code` is `USERNAME_TAKEN`. It passes; the issue's claim that a duplicate username returns `email taken` is not reproduced. No new test file and no production change.
- [x] 1.2 Covers scenario "Taken email returns its own code" with the existing test `RegisterIT.rejectsTakenEmail`. It asserts `extensions.code` is `EMAIL_TAKEN` and that no second account is stored. It passes.
- [x] 1.3 No production change is needed, because 1.1 and 1.2 already pass. Nothing new is committed for this group.

## 2. Document the codes in the contract

- [x] 2.1 Write the failing test `AccountContractTest.registerDescriptionNamesTheStableCodes` (covers scenario "Contract lists the register error codes"): read `contracts/graphql/account.graphqls` and assert that the `register` description names `EMAIL_TAKEN`, `USERNAME_TAKEN`, `VALIDATION_FAILED` and `extensions.field`. Run it and show it failing.
- [x] 2.2 Add the description to `register` in `contracts/graphql/account.graphqls` with the three codes and the `extensions.field` rule. Run the test until it passes.
- [x] 2.3 Refactor: keep the contract text and the test assertions in one place, with no duplicated lists.

## 3. Full check

- [x] 3.1 Run `.\gradlew.bat spotlessApply`, then `.\gradlew.bat test`. The only failure accepted is `ArchiveCheckTest.pullRequestCheckout`, which needs `GITHUB_HEAD_REF` and the archived change, and no other test may fail. Record the test counts in the PR description. The full `.\gradlew.bat check`, including `ArchiveCheckTest`, runs in the pull request workflow after archiving.
- [x] 3.2 Confirm that `contracts/` and the implementation are in sync, and that no `[NEEDS CLARIFICATION]` remains in the artifacts.
