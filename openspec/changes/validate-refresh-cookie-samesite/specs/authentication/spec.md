## ADDED Requirements

### Requirement: Refresh cookie SameSite setting
The `SameSite` attribute of the cookie `refresh_token` SHALL come from the environment variable `MATCHMAP_REFRESH_COOKIE_SAMESITE` and SHALL be one of `Strict`, `Lax` or `None`. When the variable is not set, the value SHALL be `Lax`. The match SHALL be exact and case-sensitive, so `none` and `LAX` are refused. The API SHALL refuse to start when the variable has any other value, including an empty value, and the startup error SHALL name the variable.

#### Scenario: Configured value is used
- **WHEN** the API starts with `MATCHMAP_REFRESH_COOKIE_SAMESITE=None` and a web client signs in
- **THEN** the cookie `refresh_token` in the response has the attribute `SameSite=None`

#### Scenario: Unset value defaults to Lax
- **WHEN** the API starts without `MATCHMAP_REFRESH_COOKIE_SAMESITE` and a web client signs in
- **THEN** the cookie `refresh_token` in the response has the attribute `SameSite=Lax`

#### Scenario: Unknown value stops startup
- **WHEN** the API starts with `MATCHMAP_REFRESH_COOKIE_SAMESITE=Lxa`
- **THEN** the application context fails to start and the error message contains `MATCHMAP_REFRESH_COOKIE_SAMESITE`

#### Scenario: Lowercase value is refused
- **WHEN** the API starts with `MATCHMAP_REFRESH_COOKIE_SAMESITE=none`
- **THEN** the application context fails to start and the error message contains `MATCHMAP_REFRESH_COOKIE_SAMESITE`

#### Scenario: Empty value is refused
- **WHEN** the API starts with `MATCHMAP_REFRESH_COOKIE_SAMESITE` set to an empty string
- **THEN** the application context fails to start and the error message contains `MATCHMAP_REFRESH_COOKIE_SAMESITE`
