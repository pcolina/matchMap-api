# MatchMap API

![Java](https://img.shields.io/badge/Java-25-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1-6DB33F)
![GraphQL](https://img.shields.io/badge/API-GraphQL-E10098)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17%20%2B%20PostGIS%203.5-336791)
![Status](https://img.shields.io/badge/status-MVP%20in%20progress-blue)

Backend for **MatchMap**, a travel app where people pin the countries and cities they have **visited**, **want to visit** or have **lived in**, see them on a world map, and compare their map with friends: places in common, places only one of them knows, a shared wishlist, trip ideas, and a compatibility score.

This repository contains only the API and its database. The web and mobile clients live in [pcolina/MatchMap](https://github.com/pcolina/MatchMap) and talk to this API exclusively through the contracts published in [`contracts/`](contracts/).

> **Status:** MVP under active development. It runs locally with Docker; nothing is deployed. See [Roadmap](#roadmap) for what works today and what comes next.

---

## Table of contents

- [Features](#features)
- [Tech stack](#tech-stack)
- [Architecture](#architecture)
- [Getting started](#getting-started)
- [Using the API](#using-the-api)
- [Development](#development)
- [How this project is built](#how-this-project-is-built)
- [Project layout](#project-layout)
- [Configuration](#configuration)
- [Roadmap](#roadmap)

---

## Features

Available today:

- **GraphQL API** served from a schema-first contract (`contracts/graphql/`), with GraphiQL for exploring it.
- **Health endpoint** reporting the application and database status separately, with HTTP 503 when the database is down and no internal details exposed.
- **Request correlation**: every request gets an `X-Correlation-Id` (reused from the client when valid, generated otherwise), returned in the response and written to every log line of that request.
- **Spatial database**: PostgreSQL with PostGIS, schema managed with Flyway migrations.

In progress: user accounts and authentication (email and password, Google sign-in, JWT access tokens with rotating refresh tokens). See [Roadmap](#roadmap).

## Tech stack

| Area | Technology |
|---|---|
| Language | Java 25 |
| Framework | Spring Boot 4.1, Spring for GraphQL, Spring Boot Actuator |
| Database | PostgreSQL 17 with PostGIS 3.5 |
| Migrations | Flyway |
| Build | Gradle 9 (wrapper, Kotlin DSL) |
| Testing | JUnit 5, AssertJ, Spring MockMvc, Testcontainers |
| Quality gates | Spotless (google-java-format), `javac -Xlint:all -Werror`, JaCoCo (80% line coverage minimum) |
| Runtime | Docker, Docker Compose |
| Process | Spec-driven development with [OpenSpec](https://github.com/Fission-AI/OpenSpec), TDD |

## Architecture

```
        web / mobile clients  (github.com/pcolina/MatchMap)
                                  |
                                  |  GraphQL (default)  /  REST where GraphQL does not fit
                                  v
+--------------------------------------------------------------------------+
|  MatchMap API  (Spring Boot)                                             |
|                                                                          |
|  [ correlation ID filter ] --> every log line carries the request ID     |
|            |                                                             |
|            +--> /graphql            schema from contracts/graphql/       |
|            +--> /graphiql           interactive explorer (Docker only)   |
|            +--> /actuator/health    application + database status        |
|                                                                          |
+-----------------------------------|--------------------------------------+
                                    | JDBC
                                    v
                     +------------------------------+
                     |  PostgreSQL 17 + PostGIS 3.5 |
                     |  schema versioned by Flyway  |
                     +------------------------------+
```

Key principles:

- **Contracts first.** Every API change starts as a change to `contracts/graphql/*.graphqls` (or `contracts/openapi/*.yaml` for plain HTTP endpoints). The build copies the GraphQL contract into the application, so the schema that is served is exactly the one in the repository.
- **The database schema belongs to Flyway.** Migrations are append-only; an applied migration is never edited.
- **Tests run against the real engine.** Integration tests start PostGIS with Testcontainers, the same image used by Docker Compose.

## Getting started

### Prerequisites

- **Git**, to clone the repository.
- **JDK 25**, available through `JAVA_HOME` (the Gradle build uses a Java 25 toolchain). Check it with `& "$env:JAVA_HOME\bin\java" -version` in PowerShell, or `"$JAVA_HOME/bin/java" -version` on macOS and Linux.
- **Docker Desktop** (Windows and macOS) or Docker Engine with Docker Compose (Linux), **running**. Check it with `docker info`. It is required both to run the stack and to run the tests.
- **Node.js**, only if you want to use the OpenSpec CLI (`openspec --version`).

### Run everything with Docker Compose

Windows (PowerShell):

```powershell
git clone https://github.com/pcolina/matchMap-api.git
cd matchMap-api

Copy-Item infra\docker\.env.example infra\docker\.env
docker compose -f infra/docker/docker-compose.yml up -d --build
```

macOS and Linux:

```bash
git clone https://github.com/pcolina/matchMap-api.git
cd matchMap-api

cp infra/docker/.env.example infra/docker/.env
docker compose -f infra/docker/docker-compose.yml up -d --build
```

This builds the API image, starts PostGIS, applies the migrations and starts the API on `http://localhost:8080`. The first run downloads base images and Gradle dependencies, so it can take several minutes. Ports `8080` and `5433` must be free.

Check that it is up. In PowerShell, use `curl.exe` (plain `curl` is an alias for `Invoke-WebRequest` there):

```bash
curl -i http://localhost:8080/actuator/health
```

```json
{"components":{"application":{"status":"UP"},"database":{"status":"UP"}},"status":"UP"}
```

Stop it with:

```bash
docker compose -f infra/docker/docker-compose.yml down       # add -v to also delete the database volume
```

### Run the API from your IDE or Gradle

The project includes a test entry point that starts a throwaway PostGIS container with Testcontainers, so no database setup is needed. Docker must be running:

```bash
./gradlew bootTestRun            # Windows: .\gradlew.bat bootTestRun
```

### Troubleshooting

| Symptom | Cause and fix |
|---|---|
| `POSTGRES_DB is required` when running Compose | `infra/docker/.env` does not exist. Create it from `.env.example`. |
| `Cannot connect to the Docker daemon` | Docker is not running. Start Docker Desktop and wait until it reports ready. |
| `port is already allocated` on `8080` or `5433` | Another process uses the port. Stop it, or change `POSTGRES_PORT` in `infra/docker/.env` (for `8080`, the port is fixed in `docker-compose.yml`). |
| API container exits or stays unhealthy | Read the logs with `docker logs matchmap-backend-api-1`. |
| Start from an empty database | `docker compose -f infra/docker/docker-compose.yml down -v`, then `up -d --build`. This deletes all local data. |

## Using the API

Examples use `curl` as on macOS and Linux. In PowerShell, use `curl.exe`.

| Endpoint | Description |
|---|---|
| `GET /actuator/health` | Application and database status. HTTP 200 when both are up, 503 when the database is down. |
| `POST /graphql` | GraphQL endpoint. |
| `GET /graphiql` | GraphiQL explorer (enabled in Docker Compose). |

### GraphQL

Open [http://localhost:8080/graphiql](http://localhost:8080/graphiql) and run:

```graphql
{ apiVersion }
```

Or from the command line:

```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query":"{ apiVersion }"}'
```

```json
{"data":{"apiVersion":"0.1.0-SNAPSHOT"}}
```

### Correlation IDs

Send your own ID to follow a request through the logs, or let the API generate one:

```bash
curl -i -H "X-Correlation-Id: my-trace-1" http://localhost:8080/actuator/health
docker logs -f matchmap-backend-api-1
```

A client-supplied ID is accepted only if it has 1 to 64 characters from `A-Z`, `a-z`, `0-9` and `-`; anything else is replaced with a new UUID, so a client cannot inject content into the logs.

### Database

PostGIS is published on the host port set by `POSTGRES_PORT` (`5433` in `infra/docker/.env.example`, to avoid clashing with a local PostgreSQL on `5432`). Connect with any client (pgAdmin, DBeaver, IntelliJ) using the credentials in `infra/docker/.env`, or from the container:

```bash
docker exec -it matchmap-backend-db-1 psql -U matchmap -d matchmap
```

## Development

| Purpose | Command |
|---|---|
| Run all tests | `./gradlew test` |
| Full quality gate: format, compiler warnings, tests, coverage | `./gradlew check` |
| Fix formatting | `./gradlew spotlessApply` |
| Run the API with a throwaway database | `./gradlew bootTestRun` |
| Start the full stack | `docker compose -f infra/docker/docker-compose.yml up -d --build` |

On Windows, use `.\gradlew.bat` instead of `./gradlew`.

`./gradlew check` is the single gate a change must pass. It fails on:

- code not formatted with google-java-format,
- any compiler warning,
- any failing test,
- line coverage below 80%.

### Testing approach

- **Unit tests** for isolated logic, such as correlation ID validation.
- **Integration tests** (`*IT`) start the full Spring context against a real PostGIS container and exercise the API over HTTP with MockMvc.
- Failure scenarios are tested for real; for example, the health test stops its database container mid-test and checks the 503 response.
- Every requirement scenario in the specs must map to at least one named test; this is a rule of the process, see [`CLAUDE.md`](CLAUDE.md).

## How this project is built

The project follows **spec-driven development**: behaviour is specified before it is implemented, and the specifications live in the repository next to the code.

```
  explore  -->  propose  -->  review & approve  -->  implement (TDD)  -->  archive
                   |                                       |                  |
          proposal, specs,                         one task = one       specs merged into
          design, tasks                            commit, test first   openspec/specs/
```

1. **Propose.** Each change is planned in `openspec/changes/<change-name>/`:
   - `proposal.md`: why the change is needed, what is in and out of scope.
   - `specs/`: the behaviour, written as requirements with `WHEN / THEN` scenarios.
   - `design.md`: technical decisions, alternatives considered and risks.
   - `tasks.md`: ordered implementation steps.
2. **Approve.** Nothing is implemented until the plan is reviewed. Open questions are marked `[NEEDS CLARIFICATION]` instead of being guessed.
3. **Implement with TDD.** For every task: write the failing test, watch it fail, write the minimum code to pass, refactor. Each task is one commit.
4. **Archive.** When the change is merged, its specs become part of [`openspec/specs/`](openspec/specs/), the living description of what the system does. Completed changes are kept in [`openspec/changes/archive/`](openspec/changes/archive/) as a record of every decision.

Architecture decisions that outlive a single change are recorded as ADRs in [`docs/adr/`](docs/adr/). The first two come with the user accounts change, which is still in review, so they are marked `Proposed` until it is merged.

### Conventions

- Trunk-based development: `main` is always green; one branch per change, merged with a merge commit so the history graph shows every change.
- Commit subjects are short imperative sentences (`Add apiVersion query backed by build info`), with the task id in the body.
- All code, identifiers, commits and specifications are written in English.

### Working with AI agents

Most of this codebase is written with [Claude Code](https://claude.com/claude-code), an AI coding agent, under the rules in [`CLAUDE.md`](CLAUDE.md). The agent does not decide what gets built. Every change goes through the human gates described above, and the agent does not move past a gate without explicit approval. When something is missing or ambiguous, the agent marks it `[NEEDS CLARIFICATION]` instead of guessing.

Where things live:

| Path | What it is | Who reads it |
|---|---|---|
| `CLAUDE.md` | Operating manual for the agent: product scope, golden rules, stack, commands, Definition of Done | The agent, and anyone who wants the rules |
| `openspec/config.yaml` | Project context and the rules OpenSpec applies to each artifact it creates | OpenSpec and the agent |
| `openspec/specs/` | Current behaviour, one folder per capability. The source of truth. | Everyone |
| `openspec/changes/<name>/` | An open change: `proposal.md` (why, scope), `specs/` (WHEN/THEN scenarios), `design.md` (decisions and alternatives), `tasks.md` (TDD steps, one commit each) | Reviewers |
| `openspec/changes/archive/` | Finished changes, kept as the record of every decision | Anyone asking why something is the way it is |
| `docs/adr/` | Architecture Decision Records for decisions that outlive a single change | Anyone |
| `contracts/` | The API contract, written before the code that implements it | Web and mobile clients, and the API |
| `.claude/` | Skills and commands that give the agent the OpenSpec workflow | The agent |

Claude Code also keeps a personal memory on the developer's machine. It lives outside the repository and is not part of the project.

The full flow, step by step, with its gates and how to fix something after a merge, is described in [`docs/SDD-FLOW.md`](docs/SDD-FLOW.md).

**Example: `add-user-accounts`.** Its [proposal](openspec/changes/add-user-accounts/proposal.md) defines the scope and lists what is out of scope. Its [specs](openspec/changes/add-user-accounts/specs/) describe the behaviour as scenarios. Its [design](openspec/changes/add-user-accounts/design.md) records each decision with its alternatives, and it links the [persistence](docs/adr/0001-persistence-with-spring-data-jpa.md) and [token authentication](docs/adr/0002-token-based-authentication.md) ADRs. Its [tasks](openspec/changes/add-user-accounts/tasks.md) are the TDD steps the agent follows. When the change is merged, its specs move into `openspec/specs/` and the folder moves to the archive.

## Project layout

```
matchMap-api/
├── contracts/
│   └── graphql/                 GraphQL schema (source of truth for the API)
├── infra/
│   └── docker/                  docker-compose.yml and .env.example for local development
├── openspec/
│   ├── specs/                   Current behaviour of the system, one folder per capability
│   └── changes/                 Planned changes and the archive of completed ones
├── src/
│   ├── main/java/com/matchmap/api/
│   │   ├── correlation/         Correlation ID filter
│   │   ├── graphql/             GraphQL resolvers
│   │   └── health/              Health indicators
│   ├── main/resources/
│   │   └── db/migration/        Flyway migrations
│   └── test/java/com/matchmap/api/
├── Dockerfile                   Multi-stage build, runs as a non-root user
└── build.gradle.kts
```

## Configuration

Local configuration lives in `infra/docker/.env` (git-ignored), created from [`infra/docker/.env.example`](infra/docker/.env.example).

| Variable | Default in the example | Purpose |
|---|---|---|
| `POSTGRES_DB` | `matchmap` | Database name |
| `POSTGRES_USER` | `matchmap` | Database user |
| `POSTGRES_PASSWORD` | `change-me` | Database password |
| `POSTGRES_PORT` | `5433` | Host port where PostGIS is published |

Secrets are never committed. Every `.env` file has a committed `.env.example` with placeholder values.

## Roadmap

| Capability | Status |
|---|---|
| Walking skeleton: build, quality gates, PostGIS, health, GraphQL, correlation IDs, Docker | Done |
| User accounts and authentication: registration, email and password sign-in, Google sign-in, JWT with rotating refresh tokens, sign-in throttling | Planned, in review |
| User profile: avatar, home country, spoken languages | Planned |
| Pins: mark countries and cities as visited, want to go or lived | Planned |
| Friends: find and connect with other users | Planned |
| Map comparison: in common, only me, only them, shared wishlist, trip ideas, compatibility score | Planned |
| Continuous integration with GitHub Actions | Planned |
