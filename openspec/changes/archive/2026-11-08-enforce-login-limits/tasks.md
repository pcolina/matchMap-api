# Tasks

## 1. Contract

- [x] 1.1 Verify that `contracts/openapi/auth.yaml` declares the limits of `LoginRequest` as the spec says (D1). Regenerating the client types happens in the client repository and is not checked here

## 2. Open questions

- [x] 2.1 Decided: a limit breach returns HTTP 400 with `invalid_email` or `invalid_password` (D3)
- [x] 2.2 Decided: the server trims the email and the password, and lower-cases the email before the lookup
- [x] 2.3 Decided: a login password with whitespace between its characters is rejected with `invalid_password` (scenario "Whitespace inside the password")

## 3. Input limits (specs/authentication)

- [x] 3.0 Replace the test password `correct horse battery` (D5) with a password of 12 to 20 characters without whitespace in the tests that use it: LoginIT, LogoutIT, RefreshIT, CredentialLeakIT, LoginThrottlingIT, MeQueryIT, GoogleSignInIT, PasswordStorageIT and RegisterIT. Run the tests and show that they pass before the new tests are written

- [x] 3.1 Write the test for the scenario "Empty email" and show it failing
- [x] 3.2 Write the test for the scenario "Email too long" and show it failing
- [x] 3.3 Write the test for the scenario "Email not in standard format" and show it failing
- [x] 3.4 Write the test for the scenario "Password too short" and show it failing
- [x] 3.5 Write the test for the scenario "Password too long" and show it failing
- [x] 3.6 Write the test for the scenario "Password without uppercase is not rejected for that reason" and show it passing. This behaviour already exists, so the test guards it against the new limits and must pass from the start
- [x] 3.7 Write the test for the scenario "Limit breach is not logged with the submitted values" and show it failing
- [x] 3.8 Implement the limits before the account lookup (D2) and verify all tests pass
- [x] 3.9 Write the test for the scenario "Whitespace inside the password" (login) and show it failing
- [x] 3.10 Write the test for the scenario "Surrounding spaces" (login) and show it failing
- [x] 3.11 Write the test for the scenario "Email letter case" and show it passing. This behaviour already exists (`AuthController` lower-cases the email), so the test guards it and must pass from the start
- [x] 3.12 Implement the whitespace rule and the trimming for login and verify all tests pass

## 5. Registration password policy (specs/user-accounts)

- [x] 5.1 Check the scenario "Password too short" with 11 characters. The existing test `RegisterIT.rejectsTooShortPassword` already covers it and passes with the 12 minimum, so no new test is written; it guards the minimum against the new maximum
- [x] 5.2 Write the test for the scenario "Password too long" with 21 characters (update `RegisterIT.rejectsTooLongPassword`) and show it failing
- [x] 5.3 Write the test for the scenario "Password at the limits" (12 and 20 characters) and show it passing. The limits already allow both lengths, so the test guards them and must pass from the start
- [x] 5.4 Write the test for the scenario "Password with whitespace" and show it failing
- [x] 5.5 Write the test for the scenario "Password with leading or trailing whitespace" and show it failing
- [x] 5.6 Implement the registration policy (D4) and verify all tests pass

## 4. Documentation

- [x] 4.1 Record in the pull request whether `CLAUDE.md` or `docs/SDD-FLOW.md` changed (none expected)
