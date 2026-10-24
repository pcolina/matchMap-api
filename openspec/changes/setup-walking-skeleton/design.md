# Design

## Context

The repository contains only `CLAUDE.md`, `.gitignore` and `openspec/`. There is no build, no source and no local infrastructure. See proposal.md for motivation and specs/ for the required behaviour.

JDK 25 is installed at the path in `JAVA_HOME`. Another JDK appears earlier in `PATH`, so the build must rely on `JAVA_HOME` or a Gradle toolchain, never on `java` from `PATH`. Docker Desktop is installed.

## Goals / Non-Goals

**Goals:**

- `.\gradlew.bat check` passes on a clean clone with Docker running.
- `docker compose -f infra/docker/docker-compose.yml up -d --build` starts PostGIS and the API, and `GET http://localhost:8080/actuator/health` returns `UP`.
- Every later change can add a GraphQL type, a Flyway migration and a Testcontainers test without touching the build.

**Non-Goals:**

- Package layering or architecture rules beyond one base package. Layers arrive with the first business change.
- Production hardening: TLS, secrets management, resource limits.

## Decisions

### D1. Bootstrap from Spring Initializr, then trim

Generate the project with Spring Initializr (Gradle Kotlin DSL, Java 25, latest stable Spring Boot) including Spring Web, Spring for GraphQL, Actuator, Flyway, PostgreSQL driver and Testcontainers. Initializr resolves a mutually compatible set of Spring Boot, Gradle wrapper and Testcontainers versions. Every version is then pinned in the build exactly as generated.

- Alternative: write the build by hand. More control, but versions must be matched manually and Gradle must be installed to create the wrapper.

### D2. Base package and coordinates

Group `com.matchmap`, artifact `matchmap-api`, base package `com.matchmap.api`. The initial project version is `0.1.0-SNAPSHOT`.

### D3. Contracts stay in `contracts/graphql/` and are copied into the build

`contracts/graphql/schema.graphqls` is the single source of the schema. Gradle `processResources` copies `contracts/graphql/**` into `graphql/` on the classpath, which is Spring for GraphQL's default schema location. The jar and the Docker image therefore contain the schema without a runtime file path.

- Alternative: point `spring.graphql.schema.locations` at `file:contracts/graphql/`. Rejected because the path does not exist inside the Docker image.

### D4. API version comes from the build

Spring Boot's `bootBuildInfo` writes `META-INF/build-info.properties` with the project version. The `apiVersion` resolver reads it through `BuildProperties`. The version therefore has a single source: `version` in `build.gradle.kts`.

### D5. Health uses Actuator with two named indicators

Only the `health` endpoint is exposed over HTTP. The default indicators are replaced by exactly two: `application` (always `UP` while the process can answer) and `database` (Actuator's datasource check registered under the name `database`). Details are hidden (`show-details: never`, `show-components: always`), which satisfies the no-internal-details requirement. When the database is down, the overall status is `DOWN` with HTTP 503, which is Actuator's default aggregation.

### D6. Correlation ID with a servlet filter and MDC

A `OncePerRequestFilter` with the highest precedence reads, validates or generates the ID, puts it in the SLF4J MDC under `correlationId`, sets the response header and clears the MDC in `finally`. The console log pattern includes `%X{correlationId}`. Clearing in `finally` prevents an ID from leaking to the next request on the same thread.

- Alternative: Micrometer Tracing with `traceId`. Rejected for now: it adds dependencies and a W3C trace format the specs do not ask for.

### D7. Database and migrations

PostgreSQL 17 with PostGIS 3.5, using the `postgis/postgis` image pinned to an exact tag, both in Docker Compose and in Testcontainers so tests run against the same engine as local development. Flyway migration `V1__enable_postgis.sql` runs `CREATE EXTENSION IF NOT EXISTS postgis;`. Credentials come from `infra/docker/.env` (git-ignored) with a committed `infra/docker/.env.example`.

### D8. Tests

- Integration tests use one shared PostGIS Testcontainers container through Spring Boot's `@ServiceConnection`.
- The "database unavailable" scenario uses its own container, stopped after the application context has started, so other tests are not affected.
- Log assertions use Spring Boot's `OutputCaptureExtension`.

### D9. `check` runs format, lint, tests and coverage

- Format: Spotless with google-java-format; `spotlessCheck` is wired into `check`, `spotlessApply` fixes it.
- Lint: `javac` with `-Xlint:all -Werror`. No extra linter dependency.
- Coverage: JaCoCo report plus `jacocoTestCoverageVerification` wired into `check`, with a minimum of 80% line coverage, excluding the Spring Boot main class.

### D10. Local runtime

- `Dockerfile` at the repository root: multi-stage build, builds the jar with the Gradle wrapper on a JDK 25 image and runs it on a JRE 25 image as a non-root user.
- `infra/docker/docker-compose.yml`: services `db` (PostGIS, with a healthcheck) and `api` (built from the root `Dockerfile`, starts after `db` is healthy, port 8080).
- Contract viewer: Spring for GraphQL's built-in GraphiQL at `/graphiql`, enabled only through an environment variable in Docker Compose. No extra container or dependency.

## Risks / Trade-offs

- [Spring Initializr may not offer Java 25 with the latest Spring Boot] → Use the newest Spring Boot that supports Java 25; if none does, stop and ask.
- [Stopping a container mid-test is slow and can be flaky] → Isolate that test in its own class with its own container and generous timeouts.
- [`-Werror` can break the build on warnings from generated code] → Apply it only to `compileJava` and `compileTestJava` of project sources.
- [The build picks the wrong JDK from `PATH`] → Declare a Java 25 toolchain in `build.gradle.kts`.
