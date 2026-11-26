# Tasks

One task is one commit, with the task id in the body. Each task writes its failing test first, shows it failing, then writes the minimum code to make it pass. Every scenario in `specs/user-accounts/spec.md` is covered by the test named here.

## 1. Tests

- [x] 1.1 Write `CompleteFirstVisitIT` in `src/test/java/com/matchmap/api/account/` with one test per scenario of "Complete first visit":
  - `signedInUserCompletesFirstVisit`: a valid token for a new account; the response has no errors, `isFirstVisit` is `false`, and `user_account` has `first_visit = false`.
  - `completingAgainChangesNothing`: the mutation is sent twice; the second response has no errors and `isFirstVisit` is `false`.
  - `anonymousClientCannotComplete`: no token; the error has `extensions.classification` `UNAUTHORIZED`, and the row is unchanged.
  - `completionAffectsOnlySignedInAccount`: two accounts; only the first is completed, and the second keeps `first_visit = true`.
  - `mutationTakesNoArguments`: first sends `mutation { completeFirstVisit { isFirstVisit } }` with a token and expects `isFirstVisit` `false`, then sends the same mutation with an argument and expects a `ValidationError`.

  Show them failing: the mutation is not in the contract, so every call fails with `FieldUndefined`.

## 2. Mutation

- [x] 2.1 Add `completeFirstVisit: User!` with its description to the `Mutation` type in `contracts/graphql/account.graphqls` (contract first). Add `completeFirstVisit()` to `UserAccount` (package-private), `FirstVisitService.complete(UUID)` annotated `@Transactional`, and the `@MutationMapping @PreAuthorize("isAuthenticated()")` method in `AccountGraphqlController` that calls the service with the id from the `Principal`. Verify all five tests in 1.1 pass, and that `GraphqlContractIT` still passes.

## 3. Verification

- [x] 3.1 Run `.\gradlew.bat check` from `C:\workspace\pablo\matchmap-api` and paste the summary of the result (tests, coverage, format).
- [x] 3.2 Note in the pull request that the web and mobile repositories must run `pnpm codegen` to regenerate their types, and that the client calls `completeFirstVisit` when it chooses.
