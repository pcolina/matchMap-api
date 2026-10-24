# Proposal

## Why

The repository has no code yet. Before building any MatchMap feature we need a thin, end-to-end slice that proves a request can travel through the API to PostGIS and back, with build, test, formatting, coverage and local runtime already working. Every later change builds on this foundation.

## What Changes

- Create the backend project: Java 25, Spring Boot, Spring for GraphQL, Gradle wrapper with Kotlin DSL.
- Add PostgreSQL with PostGIS for local development through Docker Compose, plus a Dockerfile that builds the API image.
- Add Flyway with a first migration that only enables the `postgis` extension.
- Add a health endpoint that reports the status of the application and of the database.
- Add a GraphQL endpoint at `/graphql` with one minimal query that returns the API version, defined first in `contracts/graphql/`.
- Add request correlation: every HTTP request carries an `X-Correlation-Id`, reused when the client sends one and generated otherwise, and it appears in every log line for that request.
- Make `.\gradlew.bat check` run formatting verification (Spotless), compiler warnings as errors, tests (JUnit and Testcontainers) and the coverage threshold (JaCoCo).

## Capabilities

### New Capabilities

- `service-health`: Reports whether the application and its database are available.
- `api-version`: Exposes the running API version through the GraphQL contract.
- `request-correlation`: Assigns a correlation ID to every HTTP request, returns it to the client and includes it in the logs.

### Modified Capabilities

None.

## Out of scope

- CI with GitHub Actions.
- Architecture tests with ArchUnit.
- Any business functionality: pins, countries, cities, users, friends, map comparison.
- Authentication and authorization.
- Deployment to any server. The MVP runs locally only.

## Depends on

None. This is the first change in the repository.

## Impact

- New project files at the repository root: Gradle build, wrapper, `src/`, `contracts/graphql/`, `infra/docker/`, `Dockerfile`.
- New runtime dependencies: Spring Boot, Spring for GraphQL, Spring Boot Actuator, Flyway, PostgreSQL driver.
- New test and build dependencies: JUnit, Testcontainers, Spotless, JaCoCo.
- Local development requires Docker Desktop running and JDK 25 available through `JAVA_HOME`.
