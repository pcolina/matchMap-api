# matchmap-api

Operating manual for any AI coding agent working in this repository. Read it before doing anything.

## 1. Product

MatchMap is a travel app for web and mobile. Users pin countries and cities (`VISITED`, `WANT_TO_GO`, `LIVED`), see them on a world map, find friends, and compare maps with them: in common, only me, only them, shared wishlist, trip ideas and a Jaccard compatibility score.

This repository is the **backend only**: the API and its database. Web and mobile live in other repositories and consume this API only through its published contracts.

Anything not covered by an approved spec in `openspec/specs/` or an approved change in `openspec/changes/` is out of scope.

## 2. Golden rules

- **No code without an approved change.** Every change to code belongs to an OpenSpec change in `openspec/changes/<change-name>/`. Exceptions are typo fixes and dependency bumps, and those still need the user's OK.
- **One change at a time.** Only one change is open in the repository. Do not start another until the current one is merged into `main` and archived.
- **Stop at every gate.** After proposing a change, stop and wait for explicit approval. After implementing it, stop and wait before archiving. Never chain phases.
- **Never guess.** If a requirement, behaviour, name, value or decision is missing or ambiguous, add `[NEEDS CLARIFICATION: <question>]` to the artifact, ask the user and stop. This also applies during `/opsx:apply`. Do not pick a "reasonable default".
- **TDD always.** For every task: write the failing test, run it and show it failing, write the minimum code to make it pass, then refactor. Never write production code before its test.
- **Schema-first API.** Any API change starts in `contracts/` (`graphql/` for domain operations, `openapi/` for HTTP endpoints where GraphQL does not fit). The implementation follows the contract.
- **Never** commit secrets, disable or skip tests, lower coverage thresholds, edit an applied Flyway migration, or add a dependency that the approved `design.md` does not list.
- **No comments** in any file except Markdown, unless the user asks for them.
- **Everything technical is in English:** code, identifiers, commits, PRs and OpenSpec artifacts. Conversation with the user may be in Spanish.

## 3. Stack

- Java 25, Spring Boot, Spring for GraphQL.
- Gradle (wrapper, `.\gradlew.bat`).
- PostgreSQL with PostGIS. Schema managed with Flyway.
- Tests: JUnit, Testcontainers for anything that touches the database.
- Docker: the API produces its own Docker image. Local development uses Docker Compose (PostGIS, API, contract viewer).

Versions and libraries are fixed in the `design.md` of the change that introduces them. Architectural decisions not covered by a spec get an ADR in `docs/adr/NNNN-title.md`, proposed in `design.md`.

## 4. Repository layout

```
matchmap-api/
├── CLAUDE.md
├── openspec/
│   ├── config.yaml          # Project context and per-artifact rules for OpenSpec
│   ├── specs/               # Source of truth: approved behaviour
│   └── changes/             # Open changes, and archive/ for finished ones
├── contracts/
│   ├── graphql/             # *.graphqls: domain operations (default)
│   └── openapi/             # *.yaml (OpenAPI 3.1): HTTP endpoints where GraphQL does not fit
├── docs/
│   └── adr/                 # Architecture Decision Records
├── infra/
│   └── docker/              # docker-compose.yml for local development
└── src/                     # Application code and tests
```

The layout is created by the walking-skeleton change. Until then, only `openspec/` and this file exist.

## 5. Development environment (Windows, no WSL)

Write every shell command for PowerShell: `.\gradlew.bat`, `\` paths, `$env:VAR`. Do not use bash-only syntax.

Prerequisites, checked once at the start of the walking-skeleton change:

- JDK 25 (`java -version`).
- Docker Desktop running (`docker info`). Local PostGIS and Testcontainers need it. If it is missing, stop and ask the user.
- Node, only for the OpenSpec CLI (`openspec --version`).

Secrets go in `.env` files, which are git-ignored. Every `.env` has a committed `.env.example` with dummy values. The MVP runs locally only: nothing is deployed.

## 6. Commands

Run from the repo root.

| Purpose | Command |
|---|---|
| Start DB, API and contract viewer | `docker compose -f infra/docker/docker-compose.yml up -d --build` |
| Stop | `docker compose -f infra/docker/docker-compose.yml down` |
| Run | `.\gradlew.bat bootRun` |
| Test | `.\gradlew.bat test` |
| Full check (format, lint, tests, coverage) | `.\gradlew.bat check` |
| Format | `.\gradlew.bat spotlessApply` |

The walking-skeleton change creates these commands. If one is missing or fails, report it. Do not improvise an alternative.

Before marking any task done, run the full check and paste a summary of the result.

## 7. Spec-Driven Development workflow (OpenSpec)

| Step | Command | Output | Gate |
|---|---|---|---|
| 1. Explore | `/opsx:explore` | Clarified requirements. No files written without confirmation. | None |
| 2. Propose | `/opsx:propose` | `openspec/changes/<name>/`: `proposal.md`, `specs/` (deltas), `design.md`, `tasks.md` | Stop for approval |
| 3. Update | `/opsx:update` | Revised artifacts after feedback | Stop for approval |
| 4. Apply | `/opsx:apply` | One task at a time, TDD, one commit per task | Stop when `tasks.md` is done, then open a PR |
| 5. Archive | `/opsx:archive` | Deltas merged into `openspec/specs/`, change moved to `archive/` | After the PR is merged |

Rules:

- Change names are kebab-case and start with a verb (`add-pin-country`, `setup-walking-skeleton`). The branch name equals the change name.
- `proposal.md` lists what is out of scope and which existing specs the change depends on.
- Every requirement scenario (WHEN/THEN) maps to at least one test, and `tasks.md` names that test.
- If implementation shows that a spec, design or task is wrong, stop, propose the correction with `/opsx:update` and wait for approval.

## 8. API, database and observability

- Country names are returned in the language requested through `Accept-Language`.
- Every request is logged with a correlation ID. No personal data, tokens or secrets in logs.
- Flyway migrations are append-only. A schema change is always a new migration.

## 9. Git, commits and PRs

- Trunk-based: `main` is always green. One branch per change, squash-merged via PR.
- Commit subjects are short imperative sentences in English, with no prefix (`Add upsertPin mutation`). One commit per task, with the task id in the body (`Task: 2.3`).
- The agent may commit on the change branch and open the PR. The agent never merges, force-pushes, rewrites `main` or deletes branches.
- The PR description includes the change name, the completed tasks, the test summary, any contract or schema changes, and open questions.

## 10. Definition of Done (per change)

- [ ] Every requirement scenario has a passing test.
- [ ] Every task in `tasks.md` is checked.
- [ ] Full check is green and coverage thresholds are met.
- [ ] Contracts in `contracts/` and the implementation are in sync.
- [ ] New ADRs are written.
- [ ] No `[NEEDS CLARIFICATION]` or TODO left.
- [ ] The PR is open.

## 11. When to stop and ask

- A requirement, validation rule, limit or error behaviour is not written down.
- You need a new dependency, module, or a change to the architecture.
- A spec, design or task turns out to be wrong or contradictory.
- A command fails in a way the current task does not fix, or a prerequisite is missing.
- Something would touch data privacy, security, or another user's data.
