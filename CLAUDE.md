# matchmap-api

Operating manual for any AI coding agent working in this repository. Read it before doing anything.

## 1. Product

MatchMap is a travel app for web and mobile. Users pin countries and cities (`VISITED`, `WANT_TO_GO`, `LIVED`), see them on a world map, find friends, and compare maps with them: in common, only me, only them, shared wishlist, trip ideas and a Jaccard compatibility score.

This repository is the **backend only**: the API and its database. Web and mobile live in https://github.com/pcolina/MatchMap and consume this API only through its published contracts.

Anything not covered by an approved spec in `openspec/specs/` or an approved change in `openspec/changes/` is out of scope.

## 2. Golden rules

- **Do only what the user asks.** Never take an action the user did not explicitly request. If something else seems necessary or useful, ask first and wait for the answer.
- **Never touch the remote repository unless explicitly asked.** No `push`, `fetch`, `pull`, `ls-remote`, pull requests or any other interaction with GitHub, not even read-only, unless the user explicitly requests that exact action.
- **No code without an approved change.** Every change to code belongs to an OpenSpec change in `openspec/changes/<change-name>/`. Exceptions are typo fixes and dependency bumps, and those still need the user's OK.
- **One change at a time.** Only one change is open in the repository. Do not start another until the current one is merged into `main` and archived.
- **Stop at every gate.** After proposing a change, stop and wait for explicit approval. After implementing it, stop and wait before archiving. Never chain phases.
- **Never guess.** If a requirement, behaviour, name, value or decision is missing or ambiguous, add `[NEEDS CLARIFICATION: <question>]` to the artifact, ask the user and stop. This also applies during `/opsx:apply`. Do not pick a "reasonable default".
- **TDD always.** For every task: write the failing test, run it and show it failing, write the minimum code to make it pass, then refactor. Never write production code before its test.
- **Schema-first API.** Any API change starts in `contracts/` (`graphql/` for domain operations, `openapi/` for HTTP endpoints where GraphQL does not fit). The implementation follows the contract.
- **Data validation on both sides.** Required fields, length, format and the other data rules are validated in the API and in the clients (web and mobile). The API is the authority: it validates every request, whatever the client did. A client never enforces a rule the API does not enforce, and a rule the API lacks is added to the API change.
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

## 5. Development environment (Windows, no WSL)

Write every shell command for PowerShell: `.\gradlew.bat`, `\` paths, `$env:VAR`. Do not use bash-only syntax.

Prerequisites, checked once at the start of the walking-skeleton change:

- JDK 25 in `JAVA_HOME` (`& "$env:JAVA_HOME\bin\java" -version`). Gradle uses `JAVA_HOME`; `java` on `PATH` may point to another JDK.
- Docker Desktop running (`docker info`). Local PostGIS and Testcontainers need it. If it is missing, stop and ask the user.
- Node, only for the OpenSpec CLI (`openspec --version`).

Secrets go in `.env` files, which are git-ignored. Every `.env` has a committed `.env.example` with dummy values. The MVP runs locally only: nothing is deployed.

## 6. Commands

Run from the repo root.

| Purpose | Command |
|---|---|
| Create the local env file (once) | `Copy-Item infra\docker\.env.example infra\docker\.env` |
| Start DB, API and contract viewer | `docker compose -f infra/docker/docker-compose.yml up -d --build` |
| Stop | `docker compose -f infra/docker/docker-compose.yml down` |
| Run API with a throwaway PostGIS (Testcontainers) | `.\gradlew.bat bootTestRun` |
| Test | `.\gradlew.bat test` |
| Full check (format, lint, tests, coverage) | `.\gradlew.bat check` |
| Format | `.\gradlew.bat spotlessApply` |

Once running, the API listens on `http://localhost:8080`: health at `/actuator/health`, GraphQL at `/graphql`, and GraphiQL at `/graphiql` (Docker Compose only). PostGIS is published on the host port set by `POSTGRES_PORT` in `infra/docker/.env`. The API needs `MATCHMAP_JWT_SECRET` in the same file: a base64 value of at least 256 bits, which it refuses to start without. Generate one once with `$b = New-Object byte[] 32; [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); [Convert]::ToBase64String($b)`. Tests set their own fixed secret in `build.gradle.kts`.

If a command is missing or fails, report it. Do not improvise an alternative.

Before marking any task done, run the full check and paste a summary of the result.

## 7. Spec-Driven Development workflow (OpenSpec)

| Step | Command | Output | Gate |
|---|---|---|---|
| 1. Explore | `/opsx:explore` | Clarified requirements. No files written without confirmation. | None |
| 2. Propose | `/opsx:propose` | `openspec/changes/<name>/`: `proposal.md`, `specs/` (deltas), `design.md`, `tasks.md` | Stop for approval |
| 3. Update | `/opsx:update` | Revised artifacts after feedback | Stop for approval |
| 4. Apply | `/opsx:apply` | One task at a time, TDD, one commit per task | Stop when `tasks.md` is done |
| 5. Archive | `/opsx:archive` | Deltas merged into `openspec/specs/`, change moved to `archive/`, committed on the change branch | Stop before the PR is opened |
| 6. Pull request | none (manual) | PR to `main` with code, tests, specs and archive together | Stop for review and merge |

Rules:

- Change names are kebab-case and start with a verb (`add-pin-country`, `setup-walking-skeleton`). The branch name equals the change name.
- `proposal.md` lists what is out of scope and which existing specs the change depends on.
- Every requirement scenario (WHEN/THEN) maps to at least one test, and `tasks.md` names that test.
- If implementation shows that a spec, design or task is wrong, stop, propose the correction with `/opsx:update` and wait for approval.
- The `archive-check` workflow (`.github/workflows/archive-check.yml`) runs on every pull request to `main`. It fails while `openspec/changes/<change-name>/` exists, and fails when no `openspec/changes/archive/<date>-<change-name>/` exists. Branches starting with `fix-` or `bump-` pass without a change. Once branch protection requires `archive-check`, GitHub does not allow the merge while it fails. The repository owner may bypass the rule, and the agent warns the user before any action outside this flow.

## 8. API, database and observability

- Country names are returned in the language requested through `Accept-Language`.
- Every request is logged with a correlation ID. No personal data, tokens or secrets in logs.
- Flyway migrations are append-only. A schema change is always a new migration.

## 9. Git, commits and PRs

- Trunk-based: `main` is always green. One branch per change, merged via PR with a merge commit (no squash), so the history graph shows every change and its task commits.
- Commit subjects are short imperative sentences in English, with no prefix (`Add upsertPin mutation`). One commit per task, with the task id in the body (`Task: 2.3`).
- The agent may commit locally on the change branch. It never pushes, opens pull requests, merges, force-pushes, rewrites `main` or deletes branches unless the user explicitly asks for that action.
- Commit messages and PR descriptions never include AI attribution (no `Co-Authored-By` trailers, no "Generated with" lines).
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
