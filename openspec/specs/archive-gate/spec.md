# archive-gate Specification

## Purpose

Makes sure every change is archived in its own branch before its pull request is merged into `main`, so that the specs in `openspec/specs/` always describe the code that is in `main`.

## Requirements

### Requirement: Archive check on pull requests to main
The repository SHALL run a check named `archive-check` on every pull request whose base branch is `main`. The check SHALL read the name of the pull request's head branch as the change name. It SHALL fail when `openspec/changes/<change-name>/` exists and SHALL fail when no folder `openspec/changes/archive/<date>-<change-name>/` exists, where `<date>` is a `YYYY-MM-DD` date. A head branch whose name starts with `fix-` or `bump-` is a pull request without a change: the check SHALL pass when neither folder exists for it. It SHALL pass otherwise.

#### Scenario: Change still open
- **WHEN** a pull request from branch `add-x` to `main` contains the folder `openspec/changes/add-x/`
- **THEN** the `archive-check` check fails
- **AND** the check's output names the folder that is still open

#### Scenario: Change archived
- **WHEN** a pull request from branch `add-x` to `main` contains no folder `openspec/changes/add-x/` and contains `openspec/changes/archive/2026-11-03-add-x/`
- **THEN** the `archive-check` check passes

#### Scenario: Archive missing
- **WHEN** a pull request from branch `add-x` to `main` contains neither `openspec/changes/add-x/` nor any folder matching `openspec/changes/archive/<date>-add-x/`
- **THEN** the `archive-check` check fails
- **AND** the check's output says that the change is not archived

#### Scenario: Pull request without a change
- **WHEN** a pull request from branch `fix-typo` to `main` contains neither `openspec/changes/fix-typo/` nor any archived folder for `fix-typo`
- **THEN** the `archive-check` check passes
- **AND** the check's output says that no change is involved

#### Scenario: Dependency bump without a change
- **WHEN** a pull request from branch `bump-dependencies` to `main` contains neither `openspec/changes/bump-dependencies/` nor any archived folder for `bump-dependencies`
- **THEN** the `archive-check` check passes
- **AND** the check's output says that no change is involved

### Requirement: Merging requires a passing archive check
The repository SHALL require the `archive-check` check to pass before a pull request into `main` can be merged. The rule SHALL apply to every pull request. The repository owner MAY bypass it, and the agent SHALL warn the user before any action outside the flow in `CLAUDE.md` section 7.

#### Scenario: Merge blocked while the check fails
- **WHEN** a pull request into `main` has a failing `archive-check`
- **THEN** GitHub does not allow the merge of that pull request

#### Scenario: Merge allowed once the check passes
- **WHEN** the change of a pull request into `main` is archived and its `archive-check` passes
- **THEN** GitHub allows the merge of that pull request
