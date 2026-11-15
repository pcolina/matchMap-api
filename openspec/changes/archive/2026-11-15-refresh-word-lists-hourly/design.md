# Design

Contract baseline: `905c211` (main after PR #35). This change touches no file under `contracts/`. Contract changes since the baseline are checked with `git log 905c211..HEAD -- contracts/`.

## Context

The moderation code is in `src/main/java/com/matchmap/api/moderation/`:
- `UsernameModeration` (Spring component) runs two queries, `select normalized, category, active from prohibited_word where active` and the same for `reserved_word`, and builds a `ProhibitedWordMatcher` on every `isBlocked` call. `RegistrationService` calls `isBlocked` once per sign-up.
- `ProhibitedWordMatcher` is package-private. It builds an Aho-Corasick style automaton from the entries and then matches the normalized candidate.

So each sign-up pays two queries and a matcher build. The tables are read only. The lists are changed by migration or by an external load (decision pending in #31). See `proposal.md` for the decision to refresh every hour.

## Goals / Non-Goals

**Goals:**
- Sign-up makes no query to the word tables.
- The matcher is built once per snapshot, not once per sign-up.
- A reload never exposes a partly loaded list.
- Tests do not wait an hour.

**Non-Goals:**
- An on-demand reload (#36).
- Changing normalization or matching rules.
- Cross-instance consistency. The MVP runs one instance, so each instance keeps its own snapshot.

## Decisions

**D1. Immutable snapshot behind an atomic reference.** A new class holds a snapshot: the built `ProhibitedWordMatcher` (or the list it is built from), plus the load time. The cache keeps one reference to it. A reload builds a new snapshot off to the side and swaps the reference in one step. Readers take the reference once per check, so a sign-up sees either the old snapshot or the new one. Alternative: a read-write lock around a mutable list. Rejected: more code to get right, and the reader cost is not needed for a list this small.

**D2. Schedule with the framework's built-in scheduler.** A `@Scheduled(fixedRate = 3600000)` method on the cache, with `@EnableScheduling` on the configuration. No new dependency. The interval is a constant in code for the MVP; making it configurable is not part of this change.

**D3. Initial load before requests.** The cache loads at startup, and the API does not accept sign-up requests until that first load has completed. The load runs before the application is marked ready, so the readiness state is the gate.

**D4. Failed reload.** A failed reload keeps the last successful snapshot and logs a warning with the failure reason and no words, candidates or usernames. A failure before the first successful load leaves the API not ready (D3).

**D5. Testability without waiting.** The reload is a method that tests can call directly. The scheduled method only calls it. Time-based scenarios use a fixed clock or call the reload directly; no test sleeps for an hour.

**D6. Logs.** A reload logs two counts, the active entries of `prohibited_word` and of `reserved_word`. No words, candidates or usernames.

**D7. Sign-up path.** `UsernameModeration.isBlocked` reads the current snapshot and no longer queries the tables. Its public signature stays the same, so `RegistrationService` does not change.

## Risks / Trade-offs

- **[A new word is enforced up to one hour late]** → Accepted by decision. The on-demand reload (#36) covers urgent cases.
- **[A failed reload leaves the snapshot stale or empty]** → Depends on the open decision. Either way, the failure is logged without words.
- **[The first sign-up after startup runs before the first load]** → Depends on the open decision on the first load.
- **[Snapshot memory]** → The lists are small; the snapshot is a few kilobytes. No mitigation needed.
- **[More than one instance]** → Out of scope; each instance refreshes on its own schedule.

## Migration Plan

1. No schema change. No data is moved.
2. Deploy. The first load happens at startup, so the lists in the tables apply from the first start.
3. Rollback: revert the code. The tables are unchanged.

**D8. Empty snapshot.** An empty snapshot is not an error. No word matches, so the generated username is kept (the existing "Empty word lists" scenario). An empty snapshot triggers an immediate reload attempt instead of waiting for the next hour. In the MVP the snapshot combines the two tables and is never empty, because the reserved names are seeded (#34).

## Open Questions

None.
