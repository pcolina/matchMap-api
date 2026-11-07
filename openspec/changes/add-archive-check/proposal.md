# Proposal

## Why

On `add-user-accounts` the change was merged into `main` before it was archived, so its specs stayed in `openspec/changes/` and a second pull request was needed. Nothing stopped the merge. The flow in `CLAUDE.md` requires the archive before the pull request, and that rule relies on memory. A check in GitHub makes the rule enforced rather than remembered.

## What Changes

- Add a GitHub Actions check that runs on every pull request to `main`. It fails when the change named after the pull request's branch is still open in `openspec/changes/<change-name>/`, or when no archived folder `openspec/changes/archive/<date>-<change-name>/` exists.
- Add a branch protection rule on `main` that requires this check to pass before a pull request can be merged.
- Document the check in the flow rules of `CLAUDE.md` and `docs/SDD-FLOW.md`.

## Capabilities

### New Capabilities
- `archive-gate`: the rule that a change is archived in its own branch before its pull request is merged into `main`, and the automated check that enforces it.

### Modified Capabilities
None.

## Impact

- New file `.github/workflows/archive-check.yml`.
- Repository setting: a branch protection rule on `main` that requires the check (GitHub configuration, not code).
- `CLAUDE.md` section 7 and `docs/SDD-FLOW.md` describe the check.
- No change to the API, the database or the contracts.

## Out of scope

- Validating the content of the specs, which is done by `openspec validate --specs` and not by this check.
- Checking that the archived specs match the code.
- Enforcing rules for branches without a change, such as dependency bumps or typo fixes, beyond the rule in the spec that they pass the check.
- Changing how `openspec archive` works.

## Depends on

- The archive flow in `CLAUDE.md` section 7 (step 5, archive before the pull request) and section 9 (pull requests to `main`).
- The archived change `2026-11-03-add-user-accounts`, which is the first change that needed the rule.
