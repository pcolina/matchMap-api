# Tasks

## 1. Archive check workflow (specs/archive-gate)

- [ ] 1.1 Write `ArchiveCheckTest.changeStillOpen` for the scenario "Change still open" (D4) and show it failing
- [ ] 1.2 Write `ArchiveCheckTest.changeArchived` for the scenario "Change archived" (D4) and show it failing
- [ ] 1.3 Write `ArchiveCheckTest.archiveMissing` for the scenario "Archive missing" (D4) and show it failing
- [ ] 1.4 Write `ArchiveCheckTest.pullRequestWithoutChange` for the scenario "Pull request without a change" (D4) and show it failing
- [ ] 1.5 Add `.github/workflows/archive-check.yml` (D1) that runs `./gradlew test --tests ArchiveCheckTest` with `GITHUB_HEAD_REF` set, and verify the four tests pass

## 2. Enforcement (specs/archive-gate)

- [ ] 2.1 Verify the scenario "Merge blocked while the check fails" by hand on GitHub and record the result in this task (D2, D4)
- [ ] 2.2 Set the branch protection rule on `main` to require `archive-check` and verify the merge is blocked with a failing check
- [ ] 2.3 Verify the scenario "Merge allowed once the check passes" by hand on GitHub and record the result in this task (D2, D4)

## 3. Documentation

- [ ] 3.1 Describe the check and the branch protection rule in `CLAUDE.md` section 7 and in `docs/SDD-FLOW.md`
