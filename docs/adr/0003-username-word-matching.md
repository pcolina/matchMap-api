# ADR 0003: Username word matching with normalization, length rules and a cached word list

- Status: Proposed
- Date: 2026-11-15
- Change: `add-prohibited-word-matcher`, `add-sign-up-username-check`, `complete-username-moderation`, `refresh-word-lists-hourly`
- Issue: #6 (the ADR item)

## Context

Sign-up generates a username from the email prefix. Some generated usernames contain a prohibited word (hate, violence, sexual, child-safety, impersonation) or a name reserved for the platform (`admin`, `support`, `matchmap`). The API must replace such a username with a fallback instead of showing it. The check must resist common evasions: leetspeak (`n4zi`), separators (`n-a-z-i`), case, accents and repeated letters (`nnnazi`). No third-party moderation service is used. The MVP runs as a single API instance.

The word lists are stored in `prohibited_word` and `reserved_word` (Flyway V4). Their content is not in the public repository (#31).

## Decision

**Normalization.** A candidate is normalized in this fixed order before any comparison:

1. Unicode NFKC, then lowercase.
2. Remove diacritics (NFD, then drop combining marks).
3. Map leetspeak and homoglyphs to letters: `0` to `o`, `1` to `i`, `3` to `e`, `4` to `a`, `5` to `s`, `7` to `t`, `@` to `a`, `$` to `s`.
4. Remove the separators `.`, `_`, `-` and space.
5. Collapse each run of the same letter into one letter.

The word stored in the table is normalized with the same steps, so `support` is stored as `suport` and `root` as `rot`.

**Matching.** The length of the normalized word decides how it matches:

- A word longer than 4 characters matches as a substring of the normalized candidate. It is found with an Aho-Corasick automaton (`org.ahocorasick:ahocorasick:0.6.3`), built once per list.
- A word of 4 characters or fewer matches only when it is the whole normalized candidate, or when it equals one segment of the candidate. Segments are taken from the raw candidate split on `.`, `_`, `-` and space, and each segment is normalized.

**Result.** The matcher reports whether a candidate matched and the category of the matched entry. It does not log the candidate or the matched word. Sign-up uses only the result: a match gives a `user_` fallback username, and sign-up succeeds.

**Word list in memory.** The active rows of both tables are loaded into an in-memory snapshot at startup and reloaded every hour. A reload replaces the whole snapshot at once. A failed reload keeps the last loaded snapshot. An empty snapshot is not an error: no word matches, and a reload is requested at once. Sign-up never queries the word tables. The design is in `openspec/changes/archive/2026-11-15-refresh-word-lists-hourly/design.md`.

## Alternatives considered

- **Regular expressions per word.** Easy to read, but one pattern per word is slow as the list grows, and each evasion rule needs its own pattern.
- **A third-party moderation API.** Better coverage of languages, but it sends user data to an external service and needs a contract and costs. Rejected for the MVP.
- **Database query per sign-up.** Simplest to keep correct, but it adds two queries to every sign-up. Replaced by the cached snapshot.
- **Word-boundary matching for all lengths.** Fewer false positives for short words, but it misses words inside longer names, such as `xnazix` or `mynazi`. Substring matching is kept for words longer than 4 characters only.
- **Rejecting the user instead of a fallback.** Clearer, but it reveals which part of the email matched the list and gives a way to probe the list. Sign-up gets a fallback instead.

## Consequences

- **False positives.** Substring matching on long words can block innocent names that contain a listed word. Words of 4 characters or fewer are matched only as a whole or as a segment, which limits this. The owner of the lists (#31) reviews each entry and can deactivate it.
- **Collapsing repeated letters** can make two different words equal after normalization. The stored form of each entry is computed with the same rule, so both sides compare equal.
- **Changes take up to one hour to apply.** A new word is enforced after the next hourly reload. Urgent changes will need the on-demand reload (#36).
- **Several API instances** would each keep their own snapshot and reload on their own schedule. This is accepted for the MVP, which runs one instance.
- **No logs of usernames or matched words.** Operators cannot see which names were replaced, which is the intended privacy trade-off.
- **Fallback usernames** are `user_` followed by 8 random characters, checked against the matcher and for uniqueness.
