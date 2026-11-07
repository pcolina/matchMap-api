# Design

## Context

Changes are merged into `main` through pull requests (`CLAUDE.md` section 9). The archive step moves the change folder and syncs the specs (`CLAUDE.md` section 7, step 5). Nothing checks that the archive happened before a merge. The repository is public, so GitHub branch protection rules are available.

## Decisions

### D1: The check is a GitHub Actions workflow named `archive-check`
The workflow runs on `pull_request` events whose base branch is `main`. It reads the head branch name, which is the change name by the rule in `CLAUDE.md` section 7, and checks the two conditions in the spec. It runs on `ubuntu-latest`, the default GitHub runner. Alternatives considered: a local git hook (not enforced on the remote), and a check written in PowerShell (the runner would need `pwsh`, which adds a dependency with no benefit on Linux).

### D2: Branch protection enforces the check
A branch protection rule on `main` marks `archive-check` as a required status check. Without it, the workflow only reports a failure and the merge stays allowed. The rule is set in the repository settings, so it is not stored in the repository. The rule is documented in `docs/SDD-FLOW.md`.

### D3: Folder checks use the file system, not the git history
The workflow checks out the pull request and tests the folders that exist in that checkout. This matches what the merge will produce.

### D4: The check is a JUnit test in the test source set
The check logic lives in `ArchiveCheckTest` under `src/test/java`, with one test per scenario of the spec that reads the change name from the environment variable `GITHUB_HEAD_REF` and inspects the folders of the checkout. No code is added to the application under `src/main`, and no new dependency is added. The workflow of D1 runs `./gradlew test --tests ArchiveCheckTest` on the pull request checkout, so the same tests protect the rule locally and in GitHub. The merge scenarios (D2) depend on the GitHub branch protection rule, which Gradle cannot test, so they are verified by hand on GitHub and the result is recorded in the task. Alternatives considered: a throwaway pull request per scenario (creates remote branches and pull requests for each test), and `act` (an external dependency not approved in this design).

### D5: Pull requests without a change are recognised by branch prefix
A head branch whose name starts with `fix-` or `bump-` is a pull request without a change. The check passes for it when no change folder exists, as the spec says. Alternatives considered: a `no-change` label set by the repository owner (a manual step on every such pull request), and failing every pull request without a change (rejected by the owner). Known limit: a real change whose name starts with `fix-` is treated as a pull request without a change, so the owner must not name a change with those prefixes.

### D6: The workflow checks the real pull request
The check runs on the checkout of the pull request, not only on fixtures. A test named `ArchiveCheckTest.pullRequestCheckout` reads `GITHUB_HEAD_REF` and evaluates the real repository root, so the workflow fails on an open change in the pull request. When `GITHUB_HEAD_REF` is not set (a local run), `pullRequestCheckout` fails with a message that it only runs in the pull request workflow. It is not skipped, as CLAUDE.md requires. Consequence: a local `.\gradlew.bat test` and `.\gradlew.bat check` fail on this test in every local run.

## Risks

- A repository owner can change the branch protection rule, so the rule is only as strong as the repository settings. Mitigation: documented in `docs/SDD-FLOW.md`.
- The check trusts the branch name. A branch with a different name than its change bypasses the check. Mitigation: the rule to name the branch after the change is already in `CLAUDE.md` section 7.
