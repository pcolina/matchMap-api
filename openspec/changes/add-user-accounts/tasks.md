# Tasks

## 1. Foundations

- [x] 1.1 Write `PublicEndpointsIT.publicOperationsStayPublic` and `PublicEndpointsIT.unknownPathRequiresToken` (scenarios "Public operations stay public" and "Unknown path requires a token") and show the second failing; add the dependencies from design D14 and the `SecurityFilterChain` from D7, plus the test-only chain from D13; verify both tests and all existing tests pass
- [x] 1.2 Write `docs/adr/0001-persistence-with-spring-data-jpa.md` (D1) and `docs/adr/0002-token-based-authentication.md` (D4, D5); verify both files exist and state context, decision, alternatives and consequences
- [x] 1.3 Write `AccountSchemaIT.createsAccountAndRefreshTokenTables` and show it failing; add `V2__create_user_accounts.sql` (D2), the JPA settings from D1 and the `UserAccount` and `RefreshToken` entities; verify the test passes with Hibernate schema validation enabled

## 2. Contracts

- [x] 2.1 Write `AuthContractTest.declaresAuthEndpoints` (parses `contracts/openapi/auth.yaml` and asserts OpenAPI 3.1 with the four `/api/auth/` paths and their status codes) and show it failing; write `contracts/openapi/auth.yaml` and `contracts/graphql/account.graphqls` (D9); verify the test and `GraphqlSchemaIT` pass

## 3. Registration (specs/user-accounts)

- [x] 3.1 Write `RegisterIT.registersAccount` and `RegisterIT.responseNeverContainsPassword` (scenarios "Successful registration" and "Response never contains the password") and show them failing; add the `register` mutation and registration service; verify both tests pass
- [x] 3.2 Write `RegisterIT.normalizesEmail`, `RegisterIT.rejectsTakenEmail` and `RegisterIT.rejectsInvalidEmail` (scenarios "Email is normalized", "Email already in use" and "Invalid email") and show them failing; add email normalization, validation and the `EMAIL_TAKEN` and `VALIDATION_FAILED` error mapping (D10); verify the tests pass
  - Note: the red step was not recorded. The email code and tests were committed in 5151891 together with 3.3, without a failing run first. Accepted by the user as an exception.
- [x] 3.3 Write `RegisterIT.rejectsTakenUsername`, `RegisterIT.rejectsInvalidUsername` and `RegisterIT.rejectsBlankDisplayName` (scenarios "Username already in use", "Invalid username" and "Invalid display name") and show them failing; add the username rules (3 to 30 characters from `a-z`, `0-9` and `_`, lowercased, unique ignoring case) and display name rules (trimmed, 1 to 50 characters); verify the tests pass
- [x] 3.4 Write `RegisterIT.rejectsTooShortPassword` and `RegisterIT.rejectsTooLongPassword` (scenarios "Password too short" and "Password too long") and show them failing; add the password policy (12 to 128 characters, no composition rules); verify the tests pass
- [x] 3.5 Write `PasswordStorageIT.storesSaltedHashOnly` (scenario "Stored password is not the plain text") and `PasswordEncoderTest.defaultEncoderIsArgon2id` and show them failing; configure the `DelegatingPasswordEncoder` with Argon2id (D3); verify the tests pass

## 4. Password sign-in and access tokens (specs/authentication)

- [x] 4.1 Write `LoginIT.returnsTokensForCorrectCredentials` and `LoginIT.expiresInMatchesAccessTokenLifetime` (scenarios "Correct credentials" and "expiresIn matches the access token lifetime") and show them failing; add `POST /api/auth/login`, the access token issuer (D4) and refresh token creation (D5); verify the tests pass
- [x] 4.2 Write `LoginIT.rejectsWrongPassword`, `LoginIT.rejectsUnknownEmail` and `LoginIT.rejectsAccountWithoutPassword` (scenarios "Wrong password", "Unknown email" and "Account without password") and show them failing; return the generic `invalid_credentials` error (D10); verify the tests pass
- [x] 4.3 Write `MeQueryIT.returnsOwnAccount`, `MeQueryIT.anonymousClientIsUnauthorized`, `MeQueryIT.tamperedTokenIsRejected` and `MeQueryIT.expiredTokenIsRejected` (scenarios "Signed-in user reads their account", "Anonymous client reads me", "Valid token", "Tampered token" and "Expired token") and show them failing; add the `me` query protected with `@PreAuthorize` and the JWT decoder (D4, D7); verify the tests pass
- [x] 4.4 Write `LoginThrottlingIT.throttlesAfterFailedAttempts` and `LoginThrottlingIT.otherEmailsAreNotAffected` (scenarios "Too many failed attempts" and "Other emails are not affected") and show them failing; add the in-memory throttling with 5 failed attempts per email and IP in a 15-minute window (D8); verify the tests pass

## 5. Session lifecycle (specs/authentication)

- [x] 5.1 Write `RefreshIT.rotatesTokens` and `RefreshIT.rejectsExpiredToken` (scenarios "Successful refresh" and "Expired refresh token") and show them failing; add `POST /api/auth/refresh` with rotation and row locking (D5); verify the tests pass
- [x] 5.2 Write `RefreshIT.reuseRevokesSession` (scenario "Stolen token is replayed") and show it failing; add reuse detection (D5); verify the test passes
- [x] 5.3 Write `LogoutIT.endsSession` and `LogoutIT.unknownTokenReturnsNoContent` (scenarios "Sign out ends the session" and "Unknown token") and show them failing; add `POST /api/auth/logout`; verify the tests pass

## 6. Google sign-in (specs/authentication)

- [x] 6.1 Write `GoogleSignInIT.createsAccountOnFirstSignIn`, `GoogleSignInIT.signsInReturningUser` and `UsernameGeneratorTest` (scenarios "First Google sign-in creates an account" and "Returning Google user"), signing tokens with a local test RSA key, and show them failing; add `POST /api/auth/google`, the `GoogleIdTokenVerifier` and the username generator (D6); verify the tests pass
  - Username rules clarified in D6 and approved: fallback `user_<random>` (8 characters from `a-z0-9`), collision suffix `_<n>` starting at 2, base cut so that base plus suffix fits in 30 characters. Display name rules in D6 and approved: Google `name` trimmed, email local part when missing or blank, cut to 50 characters.
  - Note: the display name tests `usesEmailLocalPartAsDisplayNameWhenNameIsMissing` and `cutsDisplayNameToFiftyCharacters` were written after the implementation, so their red run was not recorded.
- [x] 6.2 Write `GoogleSignInIT.rejectsTokenForAnotherAudience` and `GoogleSignInIT.rejectsExpiredToken` (scenarios "Token for another audience" and "Expired token") and show them failing; add the issuer, audience and timestamp validators (D6); verify the tests pass
  - Note: `rejectsTokenFromAnotherIssuer` covers the issuer rule of the requirement, which has no own scenario. `rejectsExpiredToken` already passed before the validators, because the default timestamp check of the decoder rejects it; it stays as a regression guard.
  - Decided: when `MATCHMAP_GOOGLE_CLIENT_IDS` is empty, the API starts and rejects every Google sign-in with 401 `invalid_google_token`. Tested in `GoogleSignInWithoutClientIdsIT`.
- [x] 6.3 Write `GoogleSignInIT.linksPasswordAccountByVerifiedEmail` and `GoogleSignInIT.rejectsUnverifiedEmail` (scenarios "Linking a password account" and "Unverified Google email") and show them failing; add linking and the `google_email_not_verified` error; verify the tests pass
  - Note: the tests are `linksPasswordAccountByVerifiedEmail`, `rejectsUnverifiedEmailWithoutLinkingExistingAccount` and `rejectsUnverifiedEmailWithoutCreatingAccount`. The unverified check runs before account resolution, so an unverified token gets 403 even for an account already linked by subject. This follows the scenario literally ("a valid Google ID token whose email is marked as not verified").
  - Decided: when a verified Google email matches an account already linked to a different Google subject, the API answers 401 `invalid_google_token` and never overwrites the link. A 409 would reveal to the caller that the email belongs to another Google account, and the system cannot tell the legitimate owner from someone else. Tested in `rejectsVerifiedEmailLinkedToAnotherGoogleSubject`.

- [x] 6.4 Write `GoogleJwkRetryTest.retriesFetchUntilKeysAreAvailable` and `GoogleJwkRetryTest.givesUpAfterThreeAttempts` (D11: the Google JWK set fetch is retried up to 2 times with exponential backoff from 500 ms) and show them failing; add `ResilientJwkSource` with `@Retryable` and `@EnableResilientMethods(proxyTargetClass = true)`; verify the tests pass
  - Note: with the default JDK proxy the retry did not run, so the configuration uses class-based proxies.
  - Decided: when the Google JWK set is still unavailable after the 3 attempts, `POST /api/auth/google` answers 503 `google_unavailable`, not 401, because the token itself is valid. Tested in `GoogleSignInUnavailableIT`. The spec has no scenario for this case; it comes from D11.

## 7. Credential hygiene (specs/authentication)

- [x] 7.1 Write `CredentialLeakIT.signInLeavesNoSecretsInLogs` and `CredentialLeakIT.storesOnlyRefreshTokenHashes` (scenarios "Sign-in leaves no secrets in the logs" and "Stored refresh tokens are hashes") and show them failing if any secret leaks; fix any leak found; verify the tests pass

## 8. Configuration and local runtime

- [x] 8.1 Write `AuthPropertiesTest.refusesMissingOrShortJwtSecret` and show it failing; validate `matchmap.auth.*` properties at startup (D12); verify the test passes
- [x] 8.2 Add the new variables to `infra/docker/.env.example` and pass them to the `api` service; verify with `docker compose -f infra/docker/docker-compose.yml up -d --build` that register, login and `me` work end to end with `curl.exe`, and record the commands and results for the PR

## 9. Integration check

- [x] 9.1 From a clean clone, run `.\gradlew.bat check` with Docker running and confirm format, lint, tests and coverage pass; update `CLAUDE.md` if commands or environment variables changed

## Workflow follow-up

- Merge `add-user-accounts` into `main` with the user's approval.
- After the merge, run `/opsx:archive` to move the specs into `openspec/specs/`.
