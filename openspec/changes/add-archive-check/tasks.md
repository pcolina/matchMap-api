# Tasks

## 1. Archive check workflow (specs/archive-gate)

- [ ] 1.1 Write the test for the scenario "Change still open" as a throwaway pull request (D4) and show it failing
- [ ] 1.2 Write the test for the scenario "Change archived" and show it failing
- [ ] 1.3 Write the test for the scenario "Archive missing" and show it failing
- [ ] 1.4 Write the test for the scenario "Pull request without a change" as a throwaway pull request (D4) and show it failing
- [ ] 1.5 Add `.github/workflows/archive-check.yml` (D1) and verify the four tests pass

## 2. Enforcement (specs/archive-gate)

- [ ] 2.1 Write the test for the scenario "Merge blocked while the check fails" and record how it is verified (D2)
- [ ] 2.2 Set the branch protection rule on `main` to require `archive-check` and verify the merge is blocked with a failing check
- [ ] 2.3 Write the test for the scenario "Merge allowed once the check passes" and verify it

## 3. Documentation

- [ ] 3.1 Describe the check and the branch protection rule in `CLAUDE.md` section 7 and in `docs/SDD-FLOW.md`
