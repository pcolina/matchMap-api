## 1. Tests for the email length rule

- [x] 1.1 Change `rejectsEmailTooLong` in `src/test/java/com/matchmap/api/auth/LoginIT.java` to send a 255-character email in standard format and expect HTTP 400 with `email_too_long`. Run it and show it failing. Covers scenario "Email too long". Commit: `Test that a 255 character login email returns email_too_long`
- [ ] 1.2 Add `rejectsEmailTooLongAndNotInStandardFormat` to `LoginIT`: a 255-character email that is not in standard format, expecting HTTP 400 with `email_too_long`. Run it and show it failing. Covers scenario "Email too long and not in standard format". Commit: `Test that a too long non-standard login email returns email_too_long`
- [ ] 1.3 Add `acceptsEmailWithSurroundingSpacesAt254Characters` to `LoginIT`: a 250-character standard email with 5 spaces before and after, expecting the request not to be rejected with `email_too_long` (any other response from the sign-in flow is acceptable for this test, so assert the error body is not `email_too_long`). Run it and show it failing. Covers scenario "Surrounding spaces do not count toward the email length". Commit: `Test that surrounding spaces do not count toward the login email length`

## 2. Contract

- [ ] 2.1 In `contracts/openapi/auth.yaml`, add `email_too_long` to the `Error` enum after `invalid_password`. Update the login 400 description so it says `invalid_email` for an empty or non-standard email and `email_too_long` for an email longer than 254 characters, and add `email_too_long` to its example list. Covers the contract part of the proposal. Commit: `Declare email_too_long in the login contract`

## 3. Backend

- [ ] 3.1 Add `AuthException.emailTooLong()` with status 400 and code `email_too_long`, next to `invalidEmail()`. Make `rejectsEmailTooLong` (1.1) pass with the minimum change in `AuthController.validateLoginLimits`: when the trimmed `email` is longer than 254 characters, throw `emailTooLong()` before the Bean Validation check. Run tests 1.1, 1.2 and 1.3 and show them passing. Commit: `Return email_too_long for a login email over 254 characters`
- [ ] 3.2 Refactor only: keep the empty and malformed email tests (`rejectsEmptyEmail`, `rejectsEmailNotInStandardFormat`) green and remove any duplication introduced in 3.1. Commit: `Tidy the login email limit check`
