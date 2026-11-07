# Spec-Driven Development flow

This document explains how a change to this project goes from an idea to the `main` branch, step by step. It is written for someone who has never seen this process before.

The project's rules for the AI assistant live in [`CLAUDE.md`](../CLAUDE.md), in sections 7 (the flow) and 9 (git and pull requests). This document describes the same flow in plain language.

## Contents

- [Glossary](#glossary)
- [Changes and specs](#changes-and-specs)
- [The cycle at a glance](#the-cycle-at-a-glance)
- [Step by step](#step-by-step)
- [Where is a change?](#where-is-a-change)
- [Fixing something after a change is merged](#fixing-something-after-a-change-is-merged)
- [Why this matters](#why-this-matters)
- [Example: add-user-accounts](#example-add-user-accounts)

## Glossary

Words used in this document, in the order they usually appear.

| Term | Meaning |
|---|---|
| **SDD (Spec-Driven Development)** | A way of working where the expected behaviour is written down as specifications before the code is written. |
| **OpenSpec** | The tool and folder layout that keeps changes and specifications inside the repository (`openspec/`). |
| **Change** | One unit of work that goes through the whole flow: proposed, approved, implemented, merged and archived. It has its own folder, its own branch and its own pull request. |
| **Artifact** | One of the documents that describe a change: the proposal, the specs, the design and the tasks. |
| **Proposal** | The file `proposal.md` of a change. It explains why the change exists, what it includes and what it leaves out. |
| **Specification (spec)** | A file that describes how one part of the system must behave. Finished specs live in `openspec/specs/`. |
| **Capability** | One part of the system that a spec describes, for example `authentication`. Each capability has its own spec. |
| **Requirement** | One rule inside a spec. The word **SHALL** means the system must do it. |
| **Scenario** | One concrete example of a requirement, written as **WHEN** (what someone does) and **THEN** (what must happen). |
| **Delta** | The part of a spec that a change adds, changes or removes. It is marked `ADDED`, `MODIFIED` or `REMOVED`, and it is merged into the main spec when the change is archived. |
| **Design** | The file `design.md` of a change. It records the technical decisions, the options that were rejected and the risks. |
| **Task** | One step of the implementation, listed in `tasks.md`. Each task names the test to write first and the scenario it covers. |
| **Test** | Code that checks the system behaves as a scenario says. |
| **TDD (Test-Driven Development)** | Writing the test first, watching it fail, then writing the code that makes it pass. |
| **Refactor** | Improving the structure of the code without changing what it does. The tests must still pass afterwards. |
| **Coverage** | The share of the code that the tests actually run. The project requires at least 80%. |
| **Approval gate** | A point where work stops until a person approves it. |
| **Slash command** | A command typed into Claude Code that starts with `/`, such as `/opsx:propose`. |
| **Archive** | Moving a finished change to `openspec/changes/archive/` and merging its deltas into `openspec/specs/`. |
| **ADR (Architecture Decision Record)** | A short document in `docs/adr/` that records one architecture decision and why it was taken. |
| **Contract** | The written description of an API, in `contracts/`. The code must follow it. GraphQL and OpenAPI are two formats used to write these descriptions. |
| **Schema** | The structure of the database: its tables and columns. |
| **Branch** | A separate line of work in git. Here, each change has one branch with the same name as the change. |
| **Commit** | A saved snapshot of changes in git. Here, each task gets one commit. |
| **Pull request (PR)** | A request to bring a branch into `main`, reviewed before it is accepted. |
| **Merge commit** | A git commit that joins a branch into `main`. It keeps the branch visible in the history graph. |
| **`NEEDS CLARIFICATION`** | A marker written where information is missing. The agent writes it instead of guessing. |

## Changes and specs

A **change** is one unit of work that goes through the whole flow: it is proposed, approved, implemented, merged and archived. It is a folder in `openspec/changes/<change-name>/` and a git branch with the same name.

A **spec** describes the behaviour of one capability of the system, such as `authentication` or `service-health`. It lives in `openspec/specs/<capability>/spec.md`.

The two are related like this:

- A change **adds or modifies** specs. One change can touch several specs.
- A spec **outlives** the changes that shape it. Many changes can modify the same spec over time.
- A spec is **finished** when the change that delivered it is archived, because only then its deltas are merged into `openspec/specs/`.

Current examples:

| Change | State | Specs it delivers |
|---|---|---|
| `setup-walking-skeleton` | Archived, finished | `service-health`, `request-correlation`, `api-version` |
| `add-user-accounts` | Merged into `main`, archive pending | `user-accounts`, `authentication` (their specs are not in `openspec/specs/` until the archive is merged) |

## The cycle at a glance

```
explore  ->  propose  ->  review  ->  implement (TDD)  ->  archive  ->  pull request  ->  merge
                            |                 |                                        |
                    approve or update    one task = one commit                 specs move into
                                                                               openspec/specs/
```

Every change is one folder in `openspec/changes/`, one git branch with the same name, and one pull request. Only one change is open at a time.

## Step by step

In the table, the **Command** column is a slash command typed into Claude Code, the AI assistant used in this project. The **Gate** column says whether work stops there until a person approves it.

| Step | Command | What it produces | Gate |
|---|---|---|---|
| 1. Explore | `/opsx:explore` | Clarified requirements. No files are written without confirmation. | None |
| 2. Propose | `/opsx:propose` | `proposal.md`, `specs/`, `design.md` and `tasks.md` in the change folder | Stop for approval |
| 3. Update | `/opsx:update` | Revised artifacts, kept consistent with each other | Stop for approval |
| 4. Apply | `/opsx:apply` | Code and tests, one task at a time, one commit per task | Stop when every task is done |
| 5. Archive | `/opsx:archive` | The change moves to `openspec/changes/archive/` and its deltas are applied to `openspec/specs/`, committed on the change branch | Stop before the PR is opened |
| 6. Pull request | none (manual) | A PR to `main` with the change name, the tasks, the test summary, any changes to the database schema or the API contract, the archive, and open questions | Review and merge |

### 1. Explore

This is the step for thinking a problem through before anything is written. The agent asks questions and does not create files until the direction is confirmed. No change exists yet: nothing is written to `openspec/changes/` and no branch is created. Exploration can end with a clear direction, which leads to a proposal, or with no change at all.

### 2. Propose

A change starts from a GitHub issue. When the assistant works on a change, the issue is opened first, it stays open until the change is merged into `main`, and the proposal and the pull request cite it with `Closes #<number>`. The repository owner may create a change without an issue.

This is the step that creates the change. The agent creates the change folder, which is what makes it a change, with four artifacts:

- `proposal.md`: why the change is needed, what changes, what is out of scope, and which existing specs or changes it depends on.
- `specs/`: the behaviour as requirements with `WHEN` / `THEN` scenarios. A new capability has only new requirements. A change to an existing capability is a **delta**: only the requirements that change, marked `ADDED`, `MODIFIED` or `REMOVED`.
- `design.md`: the technical decisions, the alternatives considered, the new dependencies and the risks. Architecture decisions that outlive the change are also written as ADRs in `docs/adr/`.
- `tasks.md`: ordered implementation steps. Each step names the failing test to write first and the scenario it covers.

Anything missing or ambiguous is written as `[NEEDS CLARIFICATION: <question>]`. The agent does not choose a default.

### 3. Update

If a person reviewing the change finds a problem, the artifacts are revised together, so that the proposal, the specs, the design and the tasks still agree with each other. The change is not implemented until the revised artifacts are approved.

### 4. Apply

For each task, in order:

1. Write the test for the scenario and run it. It must fail. A test that already passes before any new code exists does not check the new behaviour, so it must be rewritten.
2. Write the minimum code that makes the test pass, and run it again to see it pass.
3. Refactor the code if needed, and run the tests again to check nothing broke.
4. Commit, with the task id in the commit body (`Task: 2.3`).
5. Mark the task's checkbox in `tasks.md`. The checkbox is marked only after steps 1 to 4 are done.

A task is therefore not "finished" when its code is written. It is finished when its test was seen failing, then passing, and its commit exists. The failing run leaves no trace in the final code, so the commit history and the record of the session are the proof that the order was followed.

When every task is checked, the agent runs the full check, `.\gradlew.bat check` (run from the project folder in PowerShell). It covers formatting, tests and coverage, and the agent pastes the summary of the result.

If implementation shows that a spec, design or task is wrong, the agent stops, proposes the correction with `/opsx:update`, and waits for approval. Nothing is corrected silently inside a task commit.

### 5. Archive

The archive is done on the change's own branch, after the tasks are finished and before the pull request is opened. Its deltas are applied to the matching files in `openspec/specs/`, which hold the current behaviour of the system, and the change folder moves to `openspec/changes/archive/` as a permanent record. The result is committed on the same branch.

#### Why the archive goes in the same pull request

Specs describe what the system does today, so they must match the code that lands in `main`. Archiving in the same pull request as the code means `main` never has code without its specs, or specs without their code. It also keeps one pull request per change, so no second branch or review is needed for the archive.

If the review asks for changes to the code or the specs, they are made on the same branch and the archive is updated in the same commits.

#### Why not after the merge

Archiving after the merge would leave `main` with new code and old specs for a while, and would require a second pull request for a step that has no separate review. `add-user-accounts` was merged before its archive, so its archive needs a second pull request. This flow avoids that.

### 6. Pull request

The branch has the same name as the change. The PR description contains the change name, the completed tasks, the test summary, any changes to the database schema or the API contract, and the open questions. The PR is merged into `main` with a merge commit, not a squash. That means the history graph shows each change as a branch joined to `main`, with its task commits still visible.

### The archive check

Every pull request to `main` runs the `archive-check` workflow (`.github/workflows/archive-check.yml`). It reads the name of the pull request's branch as the change name, and checks the folders of the pull request:

| Situation | Result |
|---|---|
| `openspec/changes/<change-name>/` still exists | Fails: the change is still open |
| `openspec/changes/archive/<date>-<change-name>/` exists and the open folder does not | Passes: the change is archived |
| Neither folder exists, and the branch starts with `fix-` or `bump-` | Passes: a pull request without a change, such as a typo fix or a dependency bump |
| Neither folder exists, and the branch has any other name | Fails: the change is not archived |

The workflow only reports the result. The rule that stops a merge is a branch protection rule on `main` that requires the `archive-check` check to pass. That rule is set in the repository settings on GitHub, not in the code, and it is documented here so that it is not lost. Without it, GitHub allows the merge even when the check fails.

The repository owner may bypass the branch protection rule. The agent warns the user before any action outside this flow.

A branch with a `fix-` or `bump-` prefix is treated as a pull request without a change, so a real change must not be named with those prefixes.

## Where is a change?

There is no status file to check. A change's stage is read from the signals below, which the flow itself produces:

| Signal | Where to look | What it means |
|---|---|---|
| Change folder | `openspec/changes/<change-name>/` | The change is open: being proposed, reviewed or implemented |
| Archived folder | `openspec/changes/archive/<date>-<change-name>/` | The change is finished and its specs are in `openspec/specs/` |
| Task checkboxes | `tasks.md` of the change | How much of the implementation is done. `[x]` means checked (done), `[ ]` means not yet |
| Commits on the branch | `git log main..<change-name>` (lists the branch's commits that are not in `main`) | Implementation has started. No commits means it has not |
| Pull request | GitHub | Under review while open; merged when integrated into `main` |
| ADR status | `docs/adr/` | `Proposed` means the decision is still under review. It becomes `Accepted` when the change is merged |
| Current behaviour | `openspec/specs/` | What the system does today, not what is planned |

Reading them together gives the stage. For example, a change folder with some checked tasks, commits on its branch and no pull request is being implemented and has not been reviewed yet.

### One task list for all the specs of a change

A change has one task list, `tasks.md`, and that list is shared by all the specs of the change. Each task says which scenarios it covers, and those scenarios can belong to different specs. A scenario is one concrete example of how the system must behave, written as `WHEN` (what the user does) and `THEN` (what must happen).

So the status of a spec is not written inside the spec. To find it:

1. Open the spec and list its scenarios, which are the lines that start with `#### Scenario:`.
2. For each scenario, search `tasks.md` for the task that names it.
3. Check that task's box. A scenario is done when its task is checked and its test exists in the code.

In `add-user-accounts`, task 4.3 covers "Signed-in user reads their account" from `user-accounts` and also "Valid token" from `authentication`. Checking that one task advances both specs.

**Example: `add-user-accounts`.** Its code and tests were merged into `main` before the archive, so its archive is a second pull request. This is the exception to the flow, not the rule for later changes.

## Fixing something after a change is merged

A merged change is not reopened. A bug found later is handled as a new change. In the diagram, `M0`, `M1`, `M2` and `M3` are commits on `main`, in the order they were added:

```
main:  M0 ── M1 (add-user-accounts) ── M2 (next change) ── M3 (fix-…)
```

1. Stop and describe the problem: the behaviour, the spec that says what is correct, and the test that shows the failure.
2. Propose a new change with `/opsx:propose`. If the spec itself is wrong, the change has a delta for it. If the spec is right and the code is not, the change has no spec delta and adds a test that fails against the current code.
3. Get the proposal approved, then create the branch from the current `main`.
4. Implement with TDD, archive, open the PR and merge it with a merge commit.

A branch that was already merged still points to its old commits. Reusing it would drag those commits into the next pull request, so each change gets a fresh branch.

## Why this matters

- The specs in `openspec/specs/` always describe what the system does today.
- Each decision is traceable: from the specification to the design, the ADR, the tests and the commit that implemented it.
- The approval gates are where a person decides. The agent proposes and implements; it does not decide scope on its own.

## Example: `add-user-accounts`

| Artifact | Where | What it shows |
|---|---|---|
| Proposal | [`proposal.md`](../openspec/changes/add-user-accounts/proposal.md) | Scope, out-of-scope items and dependencies |
| Specs | [`specs/`](../openspec/changes/add-user-accounts/specs/) | Behaviour as scenarios, for user accounts and authentication |
| Design | [`design.md`](../openspec/changes/add-user-accounts/design.md) | Numbered decisions D1 to D15, with alternatives and risks |
| ADRs | [`0001`](adr/0001-persistence-with-spring-data-jpa.md), [`0002`](adr/0002-token-based-authentication.md) | The persistence and token decisions that outlive the change |
| Tasks | [`tasks.md`](../openspec/changes/add-user-accounts/tasks.md) | The TDD steps, one commit each |
| Contracts | [`contracts/`](../contracts/) | The GraphQL and OpenAPI descriptions of the API, written before the code |
