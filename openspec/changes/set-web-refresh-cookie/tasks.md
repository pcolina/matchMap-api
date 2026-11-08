# Tasks

## 1. Contract

- [x] 1.1 Update `contracts/openapi/auth.yaml`: declare the `X-Client` header on login, google, refresh and logout (D1); make `RefreshTokenRequest.refreshToken` optional; make `TokenResponse.refreshToken` optional; document the `Set-Cookie` header of `refresh_token` for web responses and the 400 response with `invalid_client` for a missing or unknown `X-Client`. Add `invalid_client` to the `Error` schema

## 2. Configuration (D3)

- [x] 2.1 Write the test "Cookie SameSite comes from configuration" and show it failing
- [x] 2.2 Write the test "CORS allows credentials from the configured web origin" and show it failing
- [x] 2.3 Implement the environment variables `MATCHMAP_REFRESH_COOKIE_SAMESITE` and `MATCHMAP_CORS_ALLOWED_ORIGINS` with local defaults, add them to `infra/docker/.env.example`, and verify all tests pass

## 3. Client type header (specs/authentication, "Client type header")

- [x] 3.0 Send `X-Client: mobile` from the existing auth tests (LoginIT, LogoutIT, RefreshIT, CredentialLeakIT, LoginThrottlingIT, GoogleSignInIT, GoogleSignInUnavailableIT, GoogleSignInWithoutClientIdsIT, MeQueryIT and AccountSchemaIT where they call `/api/auth/*`). The header is now required, so their behaviour does not change. Run them and show they pass before the new tests are written
- [x] 3.1 Write the test "Unknown X-Client value" and show it failing
- [x] 3.2 Write the test "Missing X-Client on refresh" and show it failing
- [x] 3.3 Implement the header check on the four `/api/auth` operations and verify all tests pass

## 4. Sign in (specs/authentication, "Sign in with email and password" and "Sign in with Google")

- [x] 4.1 Write the test "Correct credentials from mobile" and show it passing. The behaviour already exists once the header is set, so the test guards it
- [x] 4.2 Write the test "Correct credentials from web" and show it failing
- [x] 4.3 Implement the cookie for web sign-in and the body without `refreshToken` (D1, D3, D4), and verify all tests pass
- [x] 4.4 Write the test "Missing X-Client header" for login and show it passing. The header check from 3.3 already runs before anything else in login, so the test guards it
- [x] 4.5 Write the test "Google sign-in from web" and show it failing
- [x] 4.6 Implement the same web behaviour for `POST /api/auth/google` (D7), and verify all tests pass

## 5. Refresh (specs/authentication, "Refresh with rotation")

- [x] 5.1 Write the test "Successful refresh from mobile" and show it passing. It guards the mobile behaviour
- [x] 5.2 Write the test "Successful refresh from web" and show it failing
- [x] 5.3 Implement cookie reading and writing for web refresh, ignoring any body (D5), and verify all tests pass
- [ ] 5.4 Write the test "Web refresh without the cookie" and show it failing
- [ ] 5.5 Implement the 401 for a missing cookie and verify all tests pass

## 6. Sign out (specs/authentication, "Sign out")

- [ ] 6.1 Write the test "Web sign out clears the cookie" and show it failing
- [ ] 6.2 Implement the revocation and the expired `Set-Cookie` for web logout (D6), and verify all tests pass
- [ ] 6.3 Write the test "Web sign out without the cookie" and show it failing
- [ ] 6.4 Implement the 204 for a missing cookie and verify all tests pass

## 7. Full check

- [ ] 7.1 Run `.\gradlew.bat check` and paste the summary. Coverage thresholds must be met
