# Tasks

Each task is one commit with `Task: <id>` in the body. Every task follows TDD: write the failing test, run it and show it failing, then write the minimum code.

## 1. Password length and composition in registration

- [x] 1.1 Replace the length tests in `RegisterIT` with failing tests for scenario "Password too short" (a password of 5 characters that meets the other rules is rejected) and scenario "Password too long" (21 characters, rejected). The existing `rejectsTooShortPassword` uses 11 characters and must change. Run them and show them failing.
- [x] 1.2 Write the failing test `RegisterIT.acceptsPasswordsAtTheLimits`, updated to scenario "Password at the limits": 6 and 20 characters, each with a lowercase letter, an uppercase letter, a special character and no whitespace, both succeed. The existing test uses `a` repeated 12 and 20 times, which no longer satisfies the rule. Run it and show it failing.
- [x] 1.3 Write the failing tests for scenarios "Password without a lowercase letter" (`ABCDEF!1`), "Password without an uppercase letter" (`abcdef!1`) and "Password without a special character" (`Abcdef12`). Run them and show them failing.
- [x] 1.4 Write the failing test for scenario "Letters and digits are not special characters": `Abcdef12` and `Abcdef1é` are both rejected. Run it and show it failing.
- [x] 1.5 Write the failing test for scenario "Special character from the list": `Abcdef1!` succeeds. Write one assertion per group of the OWASP list, so that a typo in the character class fails the test. Run it and show it failing.
- [x] 1.6 Write the failing test for scenario "Unicode characters are allowed": `Ñandú!Ab` succeeds. Run it and show it failing.
- [x] 1.7 Make 1.1 to 1.6 pass. Change `RegistrationService` so that the `password` parameter has `@Size(min = 6, max = 20)` and one `@Pattern` with the lookaheads for lowercase, uppercase and the OWASP special set, followed by `\S*`, with the inline flag `(?U)`. Write the minimum code, and run the tests until they pass.
- [x] 1.8 Refactor: keep the special character set in one place, so that the pattern and the contract text use the same list.

## 2. Whitespace rule (unchanged behaviour)

- [x] 2.1 Write the failing test `RegisterIT.rejectsPasswordWithNonBreakingSpace`: a password that contains U+00A0 between valid characters is rejected with `VALIDATION_FAILED` and `extensions.field` `password`. Run it and show it failing before 1.7 is applied, because the current `\S*` does not treat U+00A0 as whitespace. The existing whitespace tests must keep passing.

## 3. Document the rule in the contract

- [x] 3.1 Write the failing test `AccountContractTest.registerDescriptionDocumentsThePasswordRule`: the `register` description in `contracts/graphql/account.graphqls` names the 6 to 20 length, the lowercase, uppercase and special requirements, and the no-whitespace rule. Run it and show it failing.
- [x] 3.2 Add the password rule to the `register` description, with the same wording as the spec. Run the test until it passes.

## 4. Full check

- [x] 4.1 Run `.\gradlew.bat spotlessApply`, then `.\gradlew.bat test`. The only failure accepted is `ArchiveCheckTest.pullRequestCheckout`, which needs `GITHUB_HEAD_REF` and the archived change. Record the test counts in the PR description. The full `.\gradlew.bat check`, including `ArchiveCheckTest`, runs in the pull request workflow after archiving.
- [x] 4.2 Confirm that `contracts/` and the implementation are in sync, and that no `[NEEDS CLARIFICATION]` remains in the artifacts.
