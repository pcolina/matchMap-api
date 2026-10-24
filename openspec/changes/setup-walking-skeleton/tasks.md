# Tasks

## 1. Project scaffolding

- [x] 1.1 Generate the project from Spring Initializr as described in design D1 and D2, pin every version, declare the Java 25 toolchain and the `0.1.0-SNAPSHOT` version; verify that the generated `ApplicationContextTest.contextLoads` runs and passes with `.\gradlew.bat test`
- [x] 1.2 Add Spotless with google-java-format and `-Xlint:all -Werror` to `check` (design D9); verify `.\gradlew.bat spotlessCheck` fails on a deliberately misformatted file, then passes after `.\gradlew.bat spotlessApply`
- [x] 1.3 Add the JaCoCo report and `jacocoTestCoverageVerification` with an 80% line coverage threshold to `check` (design D9); verify `.\gradlew.bat check` runs the verification and prints the coverage result

## 2. Database and migrations

- [x] 2.1 Write `PostgisMigrationIT.enablesPostgisExtension` against a shared PostGIS Testcontainers container and show it failing; add Flyway with `V1__enable_postgis.sql` (design D7); verify the test passes

## 3. Service health (specs/service-health)

- [x] 3.1 Write `HealthEndpointIT.reportsUpWhenDatabaseAvailable` (scenario "Application and database available") and show it failing; expose only the health endpoint with the `application` and `database` indicators (design D5); verify the test passes
- [x] 3.2 Write `HealthEndpointIT.exposesOnlyStatuses` (scenario "Response contains only statuses") and show it failing if details are visible; hide details and connection data; verify the test passes
- [x] 3.3 Write `HealthEndpointDatabaseDownIT.reportsDatabaseDown` (scenario "Database unavailable") with its own container stopped after startup, and show it failing; confirm the overall status is `DOWN` with HTTP 503; verify the test passes

## 4. API version (specs/api-version)

- [x] 4.1 Write `GraphqlSchemaIT.rejectsUnknownField` (scenario "Unknown field is rejected") and show it failing; add `contracts/graphql/schema.graphqls` with `apiVersion: String!` and copy it into the build (design D3); verify the test passes
- [x] 4.2 Write `ApiVersionQueryIT.returnsBuildVersion` (scenario "Client requests the API version") and show it failing; add build info and the `apiVersion` resolver (design D4); verify the test passes

## 5. Request correlation (specs/request-correlation)

- [ ] 5.1 Write `CorrelationIdFilterTest.reusesClientId` (scenario "Client sends a correlation ID") and show it failing; add the correlation filter (design D6); verify the test passes
- [ ] 5.2 Write `CorrelationIdFilterTest.generatesUuidWhenAbsent` and `CorrelationIdFilterTest.generatesDifferentIdsPerRequest` (scenarios "Client sends no correlation ID" and "Each request gets its own ID") and show them failing; generate UUIDs for missing headers; verify both tests pass
- [ ] 5.3 Write `CorrelationIdFilterTest.replacesInvalidId` and `CorrelationIdFilterTest.replacesOverLongId` (scenarios "Client sends an invalid correlation ID" and "Client sends an over-long correlation ID") and show them failing; accept only 1 to 64 characters from `A-Z`, `a-z`, `0-9` and `-`; verify both tests pass
- [ ] 5.4 Write `CorrelationIdLoggingIT.logLinesContainId` and `CorrelationIdLoggingIT.idDoesNotLeakToNextRequest` (scenarios "Log lines carry the request's ID" and "ID does not leak into the next request") and show them failing; add the MDC key to the log pattern and clear it after each request; verify both tests pass

## 6. Local runtime

- [ ] 6.1 Add `infra/docker/docker-compose.yml` with the `db` service, `infra/docker/.env.example`, and git-ignore `infra/docker/.env` (design D7, D10); verify `docker compose -f infra/docker/docker-compose.yml up -d db` reports the database healthy
- [ ] 6.2 Add the root `Dockerfile` and the `api` service with GraphiQL enabled (design D10); verify `docker compose -f infra/docker/docker-compose.yml up -d --build` starts both services, `GET http://localhost:8080/actuator/health` returns `UP` with an `X-Correlation-Id` header, and `/graphiql` runs `{ apiVersion }`
- [ ] 6.3 Verify scenario "Application not running" manually: stop the `api` service and confirm `GET http://localhost:8080/actuator/health` is refused; record the result in the PR description

## 7. Integration check

- [ ] 7.1 From a clean clone, run `.\gradlew.bat check` with Docker running and confirm format, lint, tests and coverage pass; update the Commands table in `CLAUDE.md` if any command differs from what was built

## Workflow follow-up

- Open the PR for branch `setup-walking-skeleton` with the test summary and the result of task 6.3.
- After the PR is merged, run `/opsx:archive` to move the specs into `openspec/specs/`.
