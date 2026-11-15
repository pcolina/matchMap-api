# Tasks

This change is documentation only. It has no test to write first, so the TDD rule does not apply, and it sets `skip_specs: true`.

- [x] 1.1 Check each rule in `docs/adr/0003-username-word-matching.md` against `CandidateNormalizer`, `ProhibitedWordMatcher`, `WordCache`, `WordSnapshot` and `build.gradle.kts`, and fix the ADR where it differs.
- [x] 1.2 Confirm the ADR lists the four changes it came from and the rejected alternatives.
- [x] 1.3 Run `openspec validate document-username-matching-adr` and `.\gradlew.bat test --tests com.matchmap.api.archive.ArchiveCheckTest --rerun` with `GITHUB_HEAD_REF` set to this branch.
