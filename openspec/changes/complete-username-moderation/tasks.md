## 1. Purpose of the capability

- [x] 1.1 Replace the `## Purpose` section of `openspec/specs/username-moderation/spec.md`, which reads `TBD - created by archiving change add-prohibited-word-matcher. Update Purpose after archive.`, with the purpose text from `design.md`. Edit the file directly: the archive sync does not change the Purpose section. No requirement changes. Commit: `Describe the purpose of the username-moderation capability`

## 2. Internal error at sign-up

- [ ] 2.1 Write the failing unit test `RegistrationServiceTest` in `src/test/java/com/matchmap/api/account/`. It builds `RegistrationService` with Mockito mocks for `UserAccountRepository`, `PasswordEncoder` and `UsernameGenerator`, a mock `UsernameModeration` whose `isBlocked` returns true, and the default validator. `fallbackUsername(any())` returns `Optional.empty()`. It asserts that `register` throws `IllegalStateException` and that `saveAndFlush` is never called. Run it and show the result. Covers the internal-error branch of "Username rules" in `user-accounts`. Commit: `Test that sign-up fails when no fallback username is free`
- [ ] 2.2 If 2.1 fails because of production code, make the minimum change in `RegistrationService` or `UsernameGenerator` that makes it pass, and nothing else. If 2.1 already passes, mark this task done with no production change and say so in the commit message. Commit: `Keep the sign-up internal error branch covered`
