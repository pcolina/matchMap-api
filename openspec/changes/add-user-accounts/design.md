# Design

## Context

The API has GraphQL (`contracts/graphql/schema.graphqls`, `apiVersion`), Actuator health, a correlation ID filter that runs first, Flyway with `V1__enable_postgis.sql`, and Testcontainers tests that share one PostGIS container. There is no security, no persistence layer beyond JDBC, and no REST controllers. See proposal.md for motivation and specs/ for the required behaviour.

## Goals / Non-Goals

**Goals:**

- One security setup that serves web and mobile identically: bearer tokens, no cookies, no server-side HTTP session.
- Every new behaviour in specs/ is covered by a test that runs in `.\gradlew.bat check` without network access, including Google sign-in.
- Existing public behaviour (health, `apiVersion`, correlation IDs) is unchanged.

**Non-Goals:**

- Contract-conformance tooling that validates responses against `contracts/openapi/` automatically. It belongs with the CI change.
- An OpenAPI viewer such as Swagger UI.
- Distributed throttling or token revocation lists for multiple API instances.

## Decisions

### D1. Persistence with Spring Data JPA (ADR 0001)

Use Spring Data JPA with Hibernate. Flyway owns the schema; Hibernate only validates it (`spring.jpa.hibernate.ddl-auto=validate`) and `spring.jpa.open-in-view=false`.

- Why: the core of MatchMap is geographic. Hibernate Spatial maps PostGIS geometry types directly, and JPA is the most widely known option for future contributors.
- Alternatives: Spring Data JDBC (simpler, but no spatial type mapping) and jOOQ (excellent SQL control, but code generation and a commercial licence for some databases). Recorded in `docs/adr/0001-persistence-with-spring-data-jpa.md`.

### D2. Schema (`V2__create_user_accounts.sql`)

- `user_account`: `id uuid` primary key, `email varchar(254)` unique, `username varchar(30)` unique, `display_name varchar(50)`, `password_hash varchar(255)` nullable, `google_subject varchar(255)` unique nullable, `created_at timestamptz`. Email and username are stored already lowercased, so plain unique constraints enforce case-insensitive uniqueness.
- `refresh_token`: `id uuid` primary key, `account_id uuid` referencing `user_account`, `session_id uuid` (indexed), `token_hash char(64)` unique, `created_at`, `expires_at`, `used_at` nullable, `revoked_at` nullable.
- Future tables such as a user profile reference `user_account.id`.

### D3. Password hashing with Argon2id

Spring Security's `DelegatingPasswordEncoder` with `Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()` as the default. Stored values carry an `{argon2@SpringSecurity_v5_8}` prefix, so the algorithm can be changed later without invalidating existing hashes.

- Why not BCrypt: BCrypt only uses the first 72 bytes of a password, which conflicts with a 128-character maximum.
- Requires the new dependency `org.bouncycastle:bcprov-jdk18on:1.86`.

### D4. Access tokens: HS256 JWT signed by the API (ADR 0002)

- Claims: `iss` `matchmap-api`, `sub` the account id, `iat`, `exp`, `jti`.
- Key: a base64 secret of at least 256 bits from the environment variable `MATCHMAP_JWT_SECRET`. The application refuses to start if it is missing or shorter.
- Validation: Spring Security OAuth2 Resource Server with a `NimbusJwtDecoder` for the same key, checking signature, issuer and expiry. It also produces the RFC 6750 `401` with `WWW-Authenticate: Bearer` for invalid tokens.
- Alternative: RS256 with a key pair. Only useful when other services must verify tokens without being able to issue them; not the case today. Recorded in `docs/adr/0002-token-based-authentication.md` together with D5 and the choice of tokens over cookie sessions.

### D5. Refresh tokens: opaque, hashed, rotating, grouped by session

- Value: 32 bytes from `SecureRandom`, base64url encoded. Only its SHA-256 hex is stored.
- Sign-in starts a new `session_id`. Refresh marks the presented token `used_at`, issues a new token in the same session and returns it.
- Presenting a token with `used_at` or `revoked_at` set revokes every token of the session (reuse detection). Logout revokes the session.
- The row is read with `SELECT ... FOR UPDATE` so two parallel refreshes of the same token cannot both succeed.

### D6. Google ID token verification without Google libraries

- A second `NimbusJwtDecoder` built from Google's JWK set (`https://www.googleapis.com/oauth2/v3/certs`) with validators for issuer (`accounts.google.com` or `https://accounts.google.com`), audience (one of `MATCHMAP_GOOGLE_CLIENT_IDS`, a comma-separated list of the web, Android and iOS client IDs) and timestamps.
- The decoder sits behind a small `GoogleIdTokenVerifier` interface. Tests replace the JWK source with a locally generated RSA key and sign their own Google-shaped tokens, so the validators are tested for real and no test calls Google.
- Account resolution order: by `google_subject`, then by verified email (link), otherwise create. A new account gets the Google `name` as display name, and a username generated from the email's local part: lowercased, invalid characters replaced with `_`, cut to fit, and a numeric suffix added on collision.

### D7. Security configuration

- One `SecurityFilterChain`: stateless sessions, CSRF disabled because no cookies are used, Spring Security's default security headers, CORS from `matchmap.cors.allowed-origins` (empty by default).
- HTTP rules: permit `GET /actuator/health`, `POST /api/auth/**`, `/graphql` and `/graphiql/**`; everything else requires authentication. The correlation ID filter keeps running before Spring Security.
- GraphQL operations are protected per resolver with `@PreAuthorize("isAuthenticated()")`. Spring for GraphQL maps a missing authentication to `UNAUTHORIZED`. `register` and `apiVersion` stay unannotated.
- Unauthenticated REST calls get `401` from the resource server's entry point.

### D8. Sign-in throttling in memory

A component keeps a fixed-window counter per `(normalized email, client IP)` in a `ConcurrentHashMap`, with expired windows removed on access. The client IP is `request.getRemoteAddr()`; there is no proxy in the MVP. The counter is checked before verifying the password.

- Trade-off: counters reset when the API restarts and are not shared between instances. Acceptable for a local, single-instance MVP; revisit with the deployment change.

### D9. Contracts first

- `contracts/openapi/auth.yaml` (OpenAPI 3.1) defines the four `/api/auth/` endpoints, their request and response bodies, and the error body `{"error": "<code>"}`.
- `contracts/graphql/account.graphqls` adds `type User`, `input RegisterInput`, `extend type Mutation { register }` and `extend type Query { me }`. The existing `schema.graphqls` gains an empty root `type Mutation` if needed. `createdAt` is an ISO-8601 UTC `String`, to avoid adding a scalar library.

### D10. Error mapping

- GraphQL: a `DataFetcherExceptionResolver` maps domain exceptions to errors with `extensions.code` (`EMAIL_TAKEN`, `USERNAME_TAKEN`, `VALIDATION_FAILED` plus `extensions.field`). Input validation uses Bean Validation on the input record.
- REST: a `@RestControllerAdvice` turns domain exceptions into `{"error": "<code>"}` with the status from the spec. Exception messages are never copied into responses.

### D11. Packages

- `com.matchmap.api.account`: entity, repository, registration service, GraphQL controller, validation rules, username generator.
- `com.matchmap.api.auth`: REST controller, token service, refresh token entity and repository, Google verifier, throttling, security configuration, error handling.

### D12. Configuration and secrets

- New properties: `matchmap.auth.jwt-secret` (from `MATCHMAP_JWT_SECRET`), `matchmap.auth.access-token-ttl` (default `15m`), `matchmap.auth.refresh-token-ttl` (default `30d`), `matchmap.auth.google.client-ids` (from `MATCHMAP_GOOGLE_CLIENT_IDS`), and the throttling limits (defaults: 5 attempts, `15m` window).
- `infra/docker/.env.example` gets dummy values; Docker Compose passes them to the `api` service.
- Tests set a fixed test secret and test client IDs.

### D13. Keeping existing tests green

`CorrelationIdLoggingIT` uses a test-only endpoint under `/test/`, which the new rules would block. Its test configuration adds a higher-priority `SecurityFilterChain` that permits `/test/**`, so production rules are not loosened for tests.

### D14. New dependencies

- `spring-boot-starter-security`, `spring-boot-starter-security-oauth2-resource-server`, `spring-boot-starter-validation`, `spring-boot-starter-data-jpa`.
- Test starters: `spring-boot-starter-security-test`, `spring-boot-starter-security-oauth2-resource-server-test`, `spring-boot-starter-validation-test`, `spring-boot-starter-data-jpa-test`.
- `org.bouncycastle:bcprov-jdk18on:1.86` (D3).

## Risks / Trade-offs

- [Registration reveals whether an email is taken (`EMAIL_TAKEN`)] → Unavoidable without email verification; the sign-in endpoint stays non-revealing. Revisit when email verification is added.
- [Access tokens stay valid after logout until they expire] → The access token lifetime is 15 minutes, which bounds that window; a deny-list is out of scope.
- [In-memory throttling resets on restart] → Accepted for the local MVP (D8).
- [Argon2id is CPU- and memory-heavy, slowing tests] → Tests may use a cheaper encoder configuration for the test profile only; the production encoder is covered by one dedicated test.
- [Google rotates its signing keys] → The JWK set is fetched and cached by Nimbus, which refreshes it when an unknown key id appears.
- [Hibernate validation fails on schema drift] → Intended: it stops the application instead of running against a mismatched schema.
