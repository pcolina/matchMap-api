# Design

## Context

Issue #21 asks that the API refuse a wrong `MATCHMAP_REFRESH_COOKIE_SAMESITE` value. Today the value is read with `@Value("${matchmap.auth.refresh-cookie.same-site}")` in `AuthController` and written into the cookie without a check. `AuthProperties` already validates `matchmap.auth.jwt-secret` when the application starts, and that is the pattern this change follows.

## Decisions

### D1: A configuration record validates the value at startup
A record `RefreshCookieProperties` under `@ConfigurationProperties("matchmap.auth.refresh-cookie")` holds `sameSite`. Its compact constructor accepts only `Strict`, `Lax` and `None`, and throws `IllegalArgumentException` with a message that names `MATCHMAP_REFRESH_COOKIE_SAMESITE` otherwise. Spring binds the record when the application starts, so a wrong value stops startup. This is the same mechanism as `AuthProperties`. Alternatives considered: checking on each sign-in, which hides the error until a web client signs in, and a bean validation annotation, which the project does not use for configuration.

### D2: The default stays in application.yml
`application.yml` keeps `same-site: ${MATCHMAP_REFRESH_COOKIE_SAMESITE:Lax}`. The unset case is therefore resolved before the record is built, and the record never sees a missing value from the environment.

### D3: AuthController receives the record
`AuthController` takes `RefreshCookieProperties` in its constructor and uses its `sameSite()` value where it currently uses the `@Value` string. The cookie code does not change otherwise.

### D4: Exact, case-sensitive match
The record compares the value exactly with `Strict`, `Lax` or `None`. `none` and `LAX` are refused, and so is an empty value, which is not in the set. The unset case is handled only by the default in `application.yml` (D2).

### D5: Tests follow the AuthPropertiesTest pattern
A unit test builds the record directly for each accepted value and each refused value, like `AuthPropertiesTest`. A test with `ApplicationContextRunner` binds the properties with an unknown value and asserts that the context fails and the message contains the variable name. This covers the scenario that stops startup without starting the database.

### D6: No new ADR
The rule is a configuration check that follows the existing `AuthProperties` pattern. It does not change an architecture decision.

## Risks

- A deployment that sets the variable to a lowercase value such as `none` fails to start. This is intended, and the value must be written exactly as in the spec.
