# Tasks

One task is one commit, with the task id in the body. Each task writes its failing test first, shows it failing, then writes the minimum code to make it pass. Every scenario in `specs/user-accounts/spec.md` is covered by the test named here.

## 1. Migration

- [ ] 1.1 Write `FirstVisitMigrationIT.existingAccountsStartTrue` (scenario "Existing accounts start with the flag true"). It migrates a PostGIS container to `V5`, inserts an account row without `first_visit`, migrates to `V6`, and asserts `first_visit = true` for that row. Show it failing: `V6` does not exist yet.
- [ ] 1.2 Add `src/main/resources/db/migration/V6__add_first_visit_to_user_accounts.sql` with `ALTER TABLE user_account ADD COLUMN first_visit boolean NOT NULL DEFAULT true;`. Verify `FirstVisitMigrationIT` passes.

## 2. Account creation

- [ ] 2.1 Write `RegisterIT.registeredAccountStartsWithFirstVisitTrue` (scenario "Account created by register starts with the flag true") and `GoogleSignInIT.newGoogleAccountStartsWithFirstVisitTrue` (scenario "Account created by Google sign-in starts with the flag true"). Both read the account through `UserAccountRepository` and call `isFirstVisit()`. Show them failing: the method does not exist yet.
- [ ] 2.2 Add `@Column(name = "first_visit", nullable = false) private boolean firstVisit;` and `public boolean isFirstVisit()` to `UserAccount`. Both constructors set `firstVisit = true`. Verify both tests pass.

## 3. Read the flag through `me`

- [ ] 3.1 Write `MeQueryIT.signedInUserReadsFirstVisitTrue` (scenario "Signed-in user reads the flag true for a new account") and update `MeQueryIT` for scenario "Signed-in user reads their account" so that `{ me { id email username isFirstVisit } }` is asserted. Show them failing: the contract has no `isFirstVisit` field, so the query fails validation.
- [ ] 3.2 Add `isFirstVisit: Boolean!` to the `User` type in `contracts/graphql/account.graphqls` (contract first). Add `@SchemaMapping(typeName = "User", field = "isFirstVisit") boolean isFirstVisit(UserAccount account)` to `AccountGraphqlController`, returning `account.isFirstVisit()`. Verify 3.1 passes.
- [ ] 3.3 Write `AccountSchemaIT.firstVisitIsNotWritable` (scenario "The flag is not writable through the API"). It sends a mutation with `isFirstVisit` as an argument and asserts a validation error. This test passes from the start, because no mutation accepts the field; it is a guard against a future writable field, and the commit message says so. No code change is needed.

## 4. Verification

- [ ] 4.1 Run `.\gradlew.bat check` from `C:\workspace\pablo\matchmap-api` and paste the summary of the result (tests, coverage, format, lint).
- [ ] 4.2 Confirm the `User` type in `contracts/graphql/account.graphqls` and the schema served by the API match, and note in the pull request that the client repositories must run `pnpm codegen` to regenerate their types. `GraphqlSchemaIT` only checks unknown fields, so the match is covered by task 4.3.
- [ ] 4.3 Write `GraphqlContractIT.servedSchemaMatchesContract`: parse every `contracts/graphql/*.graphqls` file and compare the field names and types of `Query`, `Mutation`, `User` and `RegisterInput` with the schema served by the API (`GraphQlSource.schema()`), in both directions. Show it failing by adding a field to the contract temporarily, then revert that change and verify it passes.
