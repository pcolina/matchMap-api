# Tasks

Each task is one commit, with `Task: <id>` in the body. TDD: write the failing test, run it and show it failing, then the minimum code to pass, then refactor.

## 1. Snapshot

- [ ] 1.1 Write `WordSnapshotTest.containsOnlyActiveEntries` (scenario "Snapshot is loaded at startup"), with entries of both tables, active and inactive. Show it failing; add the snapshot class that holds the active entries of both lists and the built matcher.
- [ ] 1.2 Write `WordSnapshotTest.matchesTheSameCandidatesAsBeforeTheCache` for one leetspeak candidate, one separator candidate and one short-word candidate, from the existing matcher tests. Show it failing; build the matcher inside the snapshot.

## 2. Cache and reload

- [ ] 2.1 Write `WordCacheIT.loadsActiveWordsAtStartup` (scenario "Snapshot is loaded at startup") against Testcontainers PostGIS. Show it failing; add the cache that loads through the snapshot class.
- [ ] 2.2 Write `WordCacheIT.reloadPicksUpNewWord` (scenario "Added word takes effect after the next reload"): add a word to `prohibited_word`, call the reload method directly (design D5), and assert a candidate with that word is matched. Show it failing; implement the reload method.
- [ ] 2.3 Write `WordCacheTest.reloadReplacesWholeSnapshot` (scenario "Reload replaces the whole snapshot"): a reader that holds the old snapshot keeps its result, and a new reader gets the new one, with no mix. Show it failing; implement the atomic swap (design D1).
- [ ] 2.4 Write `WordCacheTest.reloadLogsCountsOnly` (scenario "Reload log has counts only"), capturing log output and asserting it has the two counts and no word, candidate or username. Show it failing; add the count-only log line (design D6).
- [ ] 2.5 Add the scheduled method with `@Scheduled(fixedRate = 3600000)` and `@EnableScheduling` on the configuration (design D2). Write `WordCacheTest.scheduledMethodCallsReload` with a mocked reload, checking the scheduled method calls it. Show it failing first.

## 3. Sign-up path

- [ ] 3.1 Write `RegisterIT.signUpDoesNotQueryWordTables` (scenario "Sign-up reads the snapshot, not the tables"): count the statements sent to `prohibited_word` and `reserved_word` during a sign-up, expecting zero. Show it failing; make `UsernameModeration.isBlocked` read the snapshot (design D7).
- [ ] 3.2 Write `RegisterIT.matchedWordIsNotLogged` (scenario "Matched word is not logged"), capturing log output during a sign-up that matches. Show it failing; remove the generated username and matched word from the log lines.

## 4. Startup and failure behaviour

- [ ] 4.1 Write `WordCacheIT.signUpWaitsForFirstLoad` (scenario "Sign-up waits for the first load"): readiness is not reported until the first load completes, and sign-up is refused before that. Show it failing; gate readiness on the first load (design D3).
- [ ] 4.2 Write `WordCacheTest.failedReloadKeepsLastGoodSnapshot` (scenario "Failed reload keeps the last good snapshot"), with a failing reader: the previous entries still match, and a warning is logged with no word, candidate or username. Show it failing; implement the catch around the reload (design D4).
- [ ] 4.3 Write `WordCacheTest.emptySnapshotKeepsUsernameAndRequestsReload` (scenario "Empty snapshot requests an immediate reload", design D8): with no active entries the username is kept and a reload is requested. Show it failing; implement the immediate reload request.

## 5. Verification

- [ ] 5.1 Confirm every scenario of `specs/username-moderation/spec.md` (the six ADDED scenarios and the six MODIFIED scenarios) is named by a test above.
- [ ] 5.2 Confirm the existing moderation tests from `add-prohibited-word-matcher` and `add-sign-up-username-check` still pass, with no change to their expectations.
