# Tasks

## 1. Configuration (specs/authentication, "Refresh cookie SameSite setting")

- [x] 1.1 Write the unit test "Accepted SameSite values" in `RefreshCookieSettingsTest` for `Strict`, `Lax` and `None`, and show it failing (the record `RefreshCookieProperties` does not exist yet)
- [x] 1.2 Write the unit test "Refused SameSite value" in `RefreshCookieSettingsTest` for `Lxa`, `none`, `LAX` and the empty string, and show it failing, checking that the message names `MATCHMAP_REFRESH_COOKIE_SAMESITE`
- [x] 1.3 Write the test "Unknown value stops startup" in `RefreshCookieSettingsStartupTest` with `ApplicationContextRunner` and show it failing, asserting that the context fails and the message contains `MATCHMAP_REFRESH_COOKIE_SAMESITE`
- [ ] 1.4 Implement `RefreshCookieProperties` with the compact constructor check (D1) and inject it into `AuthController` in place of the `@Value` string (D3), and verify all tests pass
- [ ] 1.5 Write the test "Configured value is used" in `ClientTypeHeaderIT` that starts the context with `MATCHMAP_REFRESH_COOKIE_SAMESITE=None`, signs in from web and asserts `SameSite=None` in the cookie, and show it passing. It guards the behaviour already in place
- [ ] 1.6 Write the test "Unset value defaults to Lax" in `ClientTypeHeaderIT` that signs in from web with the default configuration and asserts `SameSite=Lax`, and show it passing. It guards the default in `application.yml`

## 2. Full check

- [ ] 2.1 Run `.\gradlew.bat check` and paste the summary. Coverage thresholds must be met. `ArchiveCheckTest` only runs in the pull request workflow and is covered by the archive step
