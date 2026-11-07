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

### D4: Test approach is a throwaway pull request
Each scenario of the spec is tested by opening a throwaway pull request into `main` that matches the scenario, and recording the result of `archive-check` in the task. No new tooling is added. Alternatives considered: running the workflow locally with `act` (an external dependency not approved in this design), and moving the logic into a script tested by Gradle (adds code that only exists to test the workflow).

## Risks

- A repository owner can change the branch protection rule, so the rule is only as strong as the repository settings. Mitigation: documented in `docs/SDD-FLOW.md`.
- The check trusts the branch name. A branch with a different name than its change bypasses the check. Mitigation: the rule to name the branch after the change is already in `CLAUDE.md` section 7.
