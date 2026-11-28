# Tasks

Every task follows TDD: write the failing test first, run it and show it failing, write the minimum code that makes it pass, then refactor. Each task is one commit with `Task: <id>` in the body.

## 1. Contract

- [ ] 1.1 Test: the schema has the type `ProhibitedWord`, the enum `ProhibitedCategory`, the query `prohibitedWords` and the mutations `createProhibitedWord` and `setProhibitedWordActive`. Then add them to `contracts/graphql/moderation.graphqls`.

## 2. Creating words

- [ ] 2.1 Test: an administrator creates a word, and the stored word keeps its text and has a normalized form and the active flag set. Covers "Administrator creates a prohibited word". Then add the service and the mutation.
- [ ] 2.2 Test: a second word with the same normalized form and category is rejected with `DUPLICATE_WORD`, the error message does not contain the word, and no row is added. Covers "Duplicate word is rejected". Then add the check.
- [ ] 2.3 Test: a word that is blank, or longer than 100 characters, is rejected with `VALIDATION_FAILED` and field `word`. Covers "Invalid word is rejected". Then add the validation.
- [ ] 2.4 Test: a created word blocks a matching username at once, with no wait for the hourly reload. Covers "New word applies at once". Then reload the cache after each write.

## 3. Reading and activating

- [ ] 3.1 Test: an administrator lists all words, active and inactive. Covers "Administrator lists the prohibited words". Then add the query.
- [ ] 3.2 Test: an administrator deactivates a word, the word stops blocking, and the row is kept. Covers "Administrator deactivates a word". Then add the mutation.
- [ ] 3.3 Test: an unknown id is rejected with `VALIDATION_FAILED` and field `id`. Covers "Unknown word id is rejected". Then add the check.

## 4. Access

- [ ] 4.1 Test: a user without the ADMIN role cannot create, list or change a word, and gets `FORBIDDEN`. Covers "Only administrators manage the list". Then add `@RequiresAdmin` to every operation.

## 5. Scenarios

- [ ] 5.1 Test: every scenario of `prohibited-word-admin` is named by a test with `@DisplayName`, checked by a coverage test. Then fix any scenario without a test.
