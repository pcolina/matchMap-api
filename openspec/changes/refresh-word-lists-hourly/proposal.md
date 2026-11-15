# Proposal

Refs #6. This change does not close #6: the architecture decision record (ADR) on normalization and matching is still open there. The on-demand reload is #36 and is out of scope.

## Why

Sign-up checks the generated username against the active words in `prohibited_word` and `reserved_word`, and the current spec reads those rows from the database on every sign-up. The word lists are read on every request, and nothing keeps a copy in memory. A cache removes that query from each sign-up. The cache must still pick up changes to the lists without a redeploy, so it reloads them on a schedule.

Decided: the cache reloads every hour. A word added to the table takes effect within one hour.

## What Changes

- At startup, the API loads the active rows of `prohibited_word` and `reserved_word` into memory.
- The API reloads both lists every hour. Each reload replaces the whole snapshot at once, so a request never sees a partly loaded list.
- Sign-up checks usernames against the in-memory snapshot instead of querying the database.
- The acceptance criterion "a new word is enforced without a redeploy" in #6 becomes "a new word is enforced within one hour of the change".
- **BREAKING** for the `username-moderation` requirement "Sign-up applies the word check": it currently says the active entries are read from the database at sign-up time. That sentence is replaced by the cache requirement.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `username-moderation`: the sign-up requirement changes from a database read per sign-up to a cached snapshot, and a new requirement defines the cache load and refresh.

## Impact

- Code: a word list cache inside the `username-moderation` code, a scheduled reload, and the sign-up check reading from the cache. The registration path changes only in where it reads the lists.
- Data: no migration and no schema change. The tables are read only.
- Contract: no change in `contracts/`. Baseline for contract changes: `905c211` (main after PR #35).
- Dependencies: none new. The scheduled reload uses the framework's existing scheduling support.
- Logs: a reload is logged with its counts only (number of active words per list), never the words, the candidates or usernames.

## Out of scope

- The on-demand reload and the way operators trigger it (#36).
- The content of the lists and their storage option (#31). This change works with empty lists and with any storage option.
- The normalization and matching rules, which are already in `username-moderation`.
- The ADR on normalization and matching (#6).
- The username change path (#30).
- Multiple API instances. The MVP runs one instance; the cache is local to each instance.

## Decided

- A failed reload keeps the last successful snapshot and logs a warning without words, candidates or usernames.
- The API does not accept sign-up requests until the first load has completed.

- An empty snapshot is not an error. With no active entries no word matches, and the username is kept. An empty snapshot triggers an immediate reload instead of waiting for the next hour.

## Open decisions

None.

## Depends on

- `username-moderation` (requirements "Prohibited and reserved word lists", "Candidate normalization", "Matching rule by word length", "Match result" and "Sign-up applies the word check", the last of which this change modifies).
- `user-accounts` (fallback username, unchanged).
- Issue #6 (matcher and tables, already merged).
