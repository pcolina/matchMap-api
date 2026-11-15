## 1. Word check facade

- [x] 1.1 Write the failing integration test `UsernameModerationIT` in `src/test/java/com/matchmap/api/moderation/`. In a transactional test it inserts an active prohibited word `nazi` and asserts that `UsernameModeration.isBlocked("nazi_fan")` is true, and that with no rows `isBlocked("nazi_fan")` is false. Run it and show it failing. Covers "Sign-up applies the word check". Commit: `Test the word check facade reads the active word lists`
- [x] 1.2 Add public class `UsernameModeration` with `boolean isBlocked(String username)`. It reads active rows of `prohibited_word` and `reserved_word` with `JdbcTemplate` and delegates to `ProhibitedWordMatcher`. Make 1.1 pass. Commit: `Expose the word check to the account package`

## 2. Fallback username generator

- [x] 2.1 Write the failing unit test `FallbackUsernameTest` in `src/test/java/com/matchmap/api/account/`. It asserts that the fallback has the form `user_` followed by 8 characters from `a-z` and `0-9`, and that it is at most 30 characters. Run it and show it failing. Covers "Generated username contains a prohibited word". Commit: `Test the fallback username form`
- [ ] 2.2 Add a fallback method to `UsernameGenerator` that uses the existing alphabet and `SecureRandom`, and a constructor that accepts an injected `Random` for tests. Make 2.1 pass. Commit: `Generate fallback usernames`
- [ ] 2.3 Write the failing test "fallback retries after a collision" with an injected random source that forces the first fallback to be taken. It asserts that the second fallback is returned. Run it and show it failing. Commit: `Test that a colliding fallback is retried`
- [ ] 2.4 Make 2.3 pass by retrying while `taken` is true. Commit: `Retry a colliding fallback username`
- [ ] 2.5 Write the failing test "fallback gives up after 5 attempts": with an injected random source that makes every fallback taken, the generator stops after 5 attempts and reports failure. Run it and show it failing. Commit: `Test the retry limit for fallback usernames`
- [ ] 2.6 Make 2.5 pass: after 5 attempts the sign-up returns an internal error and creates no account. Commit: `Stop after the fallback retry limit`

## 3. Sign-up wiring

- [ ] 3.1 Write the failing integration test `RegisterWordCheckIT` in `src/test/java/com/matchmap/api/account/`. It inserts an active prohibited word `nazi`, registers with email `nazi.fan@example.com` and asserts that the username starts with `user_`, has 13 characters, and that the response has no error. Run it and show it failing. Covers "Prohibited prefix gets a fallback". Commit: `Test that a prohibited sign-up prefix gets a fallback`
- [ ] 3.2 Make 3.1 pass: `RegistrationService` calls `UsernameModeration.isBlocked` on the base name and uses the fallback method when it matches. Commit: `Use a fallback username when the generated name matches`
- [ ] 3.3 Add the test "Reserved prefix gets a fallback" (`support@example.com` with reserved `support`). Run it and show it failing if the reserved path is not covered yet; otherwise show it passing and record that. Commit: `Test that a reserved sign-up prefix gets a fallback`
- [ ] 3.4 Add the test "Clean generated username is kept" (`ana@example.com` with no words). It must pass before and after the change. Commit: `Test that clean sign-up usernames are kept`
- [ ] 3.5 Add the test "Matched word is not logged": capture the log output during a sign-up that gets a fallback and assert that the generated username and the matched word do not appear. Run it and show it failing if logging does not yet comply, otherwise show it passing. Commit: `Test that the word check does not log the matched word`

## 4. Decisions

- [x] 4.1 Retry limit: 5 attempts in total. When all collide: internal error, no account created. Recorded in `specs/user-accounts/spec.md`, `proposal.md` and `design.md`.
