# Design

## Context

`add-first-visit-flag` added the column `first_visit`, the `UserAccount.isFirstVisit()` getter and the `isFirstVisit` field on `User`. The account is read in `AccountGraphqlController.me`, which takes the account id from the `Principal` set by the access token. The `register` mutation is also in that controller. The API does not keep the Open Session in View, so a change to a managed entity must happen inside a transaction. See proposal.md for the motivation and specs/ for the required behaviour.

## Goals / Non-Goals

**Goals:**

- Set `first_visit` to `false` for the signed-in account, and return that account in the same response.
- Keep the operation idempotent and limited to the signed-in account.

**Non-Goals:**

- Any argument, any other write to `User`, and any change to the flag by any other path.
- Any change to the migration, the dependencies or the authentication setup.

## Decisions

### D1. Contract first

In `contracts/graphql/account.graphqls`, the existing `type Mutation` gains:

```graphql
  completeFirstVisit: User!
```

with a description that states the effect and the idempotence, in the same style as the `register` description. The field takes no arguments, which keeps the scenario "The mutation takes no arguments" true by construction.

### D2. Domain method on the entity

`UserAccount` gets:

```java
void completeFirstVisit() {
  this.firstVisit = false;
}
```

It is package-private, like `linkGoogleSubject`, so only the account package can call it. Calling it twice has the same effect as calling it once, which gives the idempotence in the spec without a branch.

### D3. A transactional service method

The change happens in `FirstVisitService`, a package-private class in `com.matchmap.api.account`:

```java
@Transactional
UserAccount complete(UUID accountId) {
  UserAccount account = accounts.findById(accountId).orElseThrow();
  account.completeFirstVisit();
  return account;
}
```

- Why a service: with the Open Session in View disabled (ADR 0001), a change made in the controller would not be flushed. The `@Transactional` boundary makes the dirty checking write the update when the method returns.
- The controller calls this method, so the controller has no persistence logic.

### D4. The controller mutation

```java
@MutationMapping
@PreAuthorize("isAuthenticated()")
UserAccount completeFirstVisit(Principal principal) {
  return firstVisit.complete(UUID.fromString(principal.getName()));
}
```

It follows the same pattern as `me`: the account id comes from the token, never from the request. Without a token, `@PreAuthorize` rejects the call with `UNAUTHORIZED`, as in `me`.

### D5. No new dependency

Nothing is added to `build.gradle.kts`.

## Risks

- **Two concurrent calls** both set `false`. The result is the same, so no conflict handling is needed.
- **Account removed between authentication and the call**: `orElseThrow()` fails, as `me` already does. This is not covered by a scenario in the spec, and it is the same behaviour as `me`.

## Test approach

- An integration test calls the mutation with a valid token, and checks the response and the row in `user_account`.
- A test calls it twice and checks that the second call returns `false` with no error.
- A test calls it without a token and checks `UNAUTHORIZED` and an unchanged row.
- A test checks that a second account keeps `first_visit = true`.
- A test sends the mutation with an argument and checks the validation error.
- Each test is named in tasks.md, one per scenario in the spec delta.
