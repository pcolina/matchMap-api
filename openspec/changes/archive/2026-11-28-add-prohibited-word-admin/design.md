# Design

Contract baseline for the Definition of Done check: the commit that proposes this change, recorded when it is committed.

## Decisions

- **Word and normalized form.** The `word` column keeps the text as the administrator typed it. The `normalized` column is computed by `CandidateNormalizer.normalize`, the same function the username check uses, so duplicate detection and matching agree.
- **Duplicates.** The unique constraint on (normalized, category) is the rule. The service checks it first and returns `DUPLICATE_WORD` with no word in the message, so the error is clear without a database exception.
- **Deactivation.** A single mutation `setProhibitedWordActive(id: ID!, active: Boolean!)` sets the flag. Words are never deleted, so the history stays.
- **Reload.** After each successful write the service calls `WordCache.reload()`. The reload is already safe: if it fails, the last list is kept.
- **No pagination.** The list is returned in full, ordered by category and word.
- **Language.** Stored as given, between 2 and 5 characters, matching the column size.
- **Access.** Every operation requires the ADMIN role through `@RequiresAdmin`, as the catalog changes do.

## Risks

- The list is returned in full. If the list grows to thousands of words, this query needs pagination; that is a new change.

## Dependencies

No new dependency is added.
