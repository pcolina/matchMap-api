# Design

## Context

Accounts are stored in `user_account` (`V2__create_user_accounts.sql`, `V3__drop_display_name.sql`) and mapped by the JPA entity `UserAccount`. Two code paths create accounts: `RegistrationService.register` and `GoogleAccountService`. `me` is resolved in `AccountGraphqlController` and returns the entity; the `User` type in `contracts/graphql/account.graphqls` is resolved from its getters. See proposal.md for the motivation and specs/ for the required behaviour.

## Goals / Non-Goals

**Goals:**

- Store the flag in the database, set it when an account is created, and return it in `me`.
- Keep the contract, the migration and the mapping in one change, so the field is never exposed without its data.

**Non-Goals:**

- Any operation that changes the flag. Covered by a later change.
- Any change to `register`'s input or response fields other than the new `isFirstVisit` on `User`.

## Decisions

### D1. Migration `V6__add_first_visit_to_user_accounts.sql`

```sql
ALTER TABLE user_account ADD COLUMN first_visit boolean NOT NULL DEFAULT true;
```

- `ADD COLUMN ... DEFAULT true` fills the existing rows with `true` in the same statement, so the two accounts already in the database also start with `true`, as the proposal states.
- No existing migration is edited (`V1` to `V5` stay as they are).

### D2. The entity sets the value in its constructor

`UserAccount` gets `@Column(name = "first_visit", nullable = false) private boolean firstVisit;`. Both constructors set `this.firstVisit = true`, so every account created by the application starts with `true` without relying on the database default.

- Why not rely on the database default: JPA inserts every mapped column, so a primitive `boolean` left at `false` would overwrite the default. Setting it in the constructor keeps the rule in the code that creates the account.
- The getter is `public boolean isFirstVisit()`. There is no setter. Nothing in this change changes the value after creation.

Every account creation sets the flag to `true`, both `RegistrationService` (`register`) and `GoogleAccountService` (Google sign-in). Both go through the constructor, so no creation path needs its own code.

### D3. GraphQL field `isFirstVisit` via an explicit schema mapping

The GraphQL field is `isFirstVisit`, but the Java property is `firstVisit`, so the default property lookup would not find it. The field is mapped explicitly in `AccountGraphqlController`:

```java
@SchemaMapping(typeName = "User", field = "isFirstVisit")
boolean isFirstVisit(UserAccount account)
```

- The contract type is `isFirstVisit: Boolean!`, which is non-null, so the mapping returns a primitive `boolean`.
- Why explicit: it does not depend on how Spring GraphQL resolves names, and the test for `me` fails if the mapping is removed.

### D4. Dependencies

No new dependency. `spring-boot-starter-data-jpa`, Flyway and the GraphQL starter are already in `build.gradle.kts`.

## Risks

- **The contract is consumed by the clients**: after `contracts/graphql/account.graphqls` changes, the client repositories must regenerate their types with `pnpm codegen`. That is a client task and is not part of this change.

## Test approach

- A Testcontainers test runs V6 on a database that already has accounts, and checks that the existing rows are `true`.
- A `RegisterIT` test checks that an account created by `register` has `first_visit = true` in the table and that `me` returns `isFirstVisit: true`.
- A GraphQL test checks that `me` exposes `isFirstVisit`, and that a mutation cannot write it.
- Each test is named in tasks.md, one per scenario in the spec delta.
