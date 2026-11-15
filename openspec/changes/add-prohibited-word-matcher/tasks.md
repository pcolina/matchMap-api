## 1. Schema

- [x] 1.1 Write the failing integration test `ProhibitedWordSchemaIT` in `src/test/java/com/matchmap/api/moderation/`. It migrates an empty PostgreSQL (Testcontainers) and asserts that `prohibited_word` and `reserved_word` exist and have no rows. Run it and show it failing. Covers scenario "Tables are created empty". Commit: `Test that the prohibited and reserved word tables are created empty`
- [x] 1.2 Add `V4__create_prohibited_word_lists.sql` with both tables: `id`, `word`, `normalized`, `category` (check constraint on `CHILD_SAFETY`, `HATE`, `VIOLENCE`, `SEXUAL`, `IMPERSONATION`, `RESERVED`), `language`, `active`, `created_at`, `updated_at`, and a unique constraint on `(normalized, category)`. No `INSERT`. Make 1.1 pass. Commit: `Create the prohibited and reserved word tables`
- [x] 1.3 Extend `ProhibitedWordSchemaIT` first: an insert with an unknown category fails, and a duplicate `(normalized, category)` fails. Run it and show the new assertions failing. Covers the constraints in the "Prohibited and reserved word lists" requirement. Commit: `Test the word table constraints`
- [x] 1.4 Make 1.3 pass. The constraints are already in V4, so the change is limited to what the test needs. Commit: `Enforce the word table constraints`

## 2. Normalization

- [x] 2.1 Write the failing unit test `CandidateNormalizerTest` with one assertion per scenario in "Candidate normalization": `naz1` → `nazi`, `n-a-z-i` → `nazi`, `nnnazi` → `nazi`, `NÁZI` → `nazi`, `n4z1` → `nazi`. Run it and show it failing. Commit: `Test the candidate normalization scenarios`
- [x] 2.2 Implement `CandidateNormalizer` with the steps in the spec, in order: NFKC, lowercase, diacritics, leetspeak map, separator removal, repeat collapse. Make 2.1 pass. Commit: `Normalize candidate usernames for word matching`
- [x] 2.3 Add a failing test for each leetspeak mapping in the spec (`0`, `1`, `3`, `4`, `5`, `7`, `@`, `$`) and for space as a separator. Run them and show them failing. Commit: `Test every leetspeak mapping and the space separator`
- [x] 2.4 Make 2.3 pass, with the minimum change in `CandidateNormalizer`. Commit: `Map every leetspeak character and remove spaces`

## 3. Dependency (blocks section 4)

- [ ] 3.1 Add `implementation("org.ahocorasick:ahocorasick:0.6.3")` to `build.gradle.kts`, the version fixed in `design.md`. Run `.\gradlew.bat check` and show the result. Commit: `Add the Aho-Corasick dependency for the word matcher`

## 4. Matching

- [ ] 4.1 Write the failing unit test `ProhibitedWordMatcherTest` for "Matching rule by word length": a long word `nazis` matches inside `xnazisx`, and a short word `ass` does not match inside `classic`. Run it and show it failing. Covers scenarios "Long word inside a longer candidate" and "Short word inside a longer candidate". Commit: `Test substring matching for long words and whole-word matching for short ones`
- [ ] 4.2 Implement `ProhibitedWordMatcher`: build an Aho-Corasick automaton from the active normalized words longer than 4 characters, and whole-word comparison for the others. Make 4.1 pass. Commit: `Match prohibited words by length`
- [ ] 4.3 Write the failing test "Short word as the whole candidate": reserved word `root` matches `r00t` with category `RESERVED`. Run it and show it failing. Commit: `Test that a short reserved word matches the whole candidate`
- [ ] 4.4 Make 4.3 pass. Commit: `Match short reserved words as whole candidates`
- [ ] 4.5 Write the failing tests "Inactive entries are ignored", "No match" and "Match reports the category" (`HATE`). Run them and show them failing. Commit: `Test inactive entries, no match and the reported category`
- [ ] 4.6 Make 4.5 pass. Commit: `Report the category of the matched word`

## 5. Short-word boundary

- [ ] 5.1 Write the failing test "Short word as a separated segment" (`ana_nazi` matches `nazi`, category `HATE`) and "Short word split by separators" (`n-a-z-i` matches `nazi`). Run them and show them failing. Commit: `Test segment and separator matching for short words`
- [ ] 5.2 Make 5.1 pass in `ProhibitedWordMatcher`: a short word matches when it is the whole normalized candidate or one segment separated by `.`, `_`, `-` or a space. Commit: `Match short words by segment as well as whole candidate`
