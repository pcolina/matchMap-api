# Design

## Context

`UserAccount` holds the account (`V2`, `V3` and `V6`: `first_visit`). `me` returns the entity, and the `User` type fields are read from its getters, except `isFirstVisit`, which has an explicit `@SchemaMapping`. Both creation paths, `RegistrationService` and `GoogleAccountService`, call the same constructor, so a value set there applies to both. See proposal.md for the motivation and specs/ for the required behaviour.

## Goals / Non-Goals

**Goals:**

- Store the profile values and the subscription plan, default `FREE`, and return them from `me`.
- Keep every value that the API cannot yet set as `null`, without inventing data.

**Non-Goals:**

- Any write to the profile, and any change of plan.
- Translated country names.

## Decisions

### D1. Migration `V7__add_user_profile.sql`

```sql
ALTER TABLE user_account
    ADD COLUMN first_name varchar(100),
    ADD COLUMN last_name varchar(100),
    ADD COLUMN nationality varchar(2),
    ADD COLUMN birth_country varchar(2),
    ADD COLUMN subscription varchar(20) NOT NULL DEFAULT 'FREE',
    ADD CONSTRAINT ck_user_account_subscription CHECK (subscription IN ('FREE')),
    ADD CONSTRAINT ck_user_account_nationality CHECK (nationality ~ '^[A-Z]{2}$'),
    ADD CONSTRAINT ck_user_account_birth_country CHECK (birth_country ~ '^[A-Z]{2}$');
```

- Existing rows get `subscription = 'FREE'` and `NULL` in the other columns, because of the default and the absence of a default for the rest.
- `NULL` passes the regular-expression check in PostgreSQL, so an empty country is allowed.
- Length 100 for names is a decision of this change, since the names are not yet written by any operation. The later write change may tighten it.
- No existing migration is edited.

### D2. The entity maps the columns and sets the plan

`UserAccount` gets:

- `firstName`, `lastName`, `nationality`, `birthCountry`, as `String` fields with `@Column` names `first_name`, `last_name`, `nationality`, `birth_country`. None is set by the constructors, so they are `null`.
- `subscription` of type `SubscriptionPlan`, mapped with `@Enumerated(EnumType.STRING)` and `@Column(name = "subscription", nullable = false)`. The constructor sets it to `SubscriptionPlan.FREE`, so both creation paths start with `FREE`, as with `firstVisit`.
- Public getters `getFirstName()`, `getLastName()`, `getNationality()`, `getBirthCountry()` and `getSubscription()`. Spring GraphQL resolves the `User` fields from them without an explicit mapping.

Why set `FREE` in the constructor and not rely on the column default: JPA inserts every mapped column, so a `null` or an unset enum would overwrite the database default. This is the same reason as for `firstVisit`.

### D3. Enum `SubscriptionPlan`

`src/main/java/com/matchmap/api/account/SubscriptionPlan.java`:

```java
public enum SubscriptionPlan { FREE }
```

It is public because the entity getter is public and the GraphQL mapping resolves the enum by name. The GraphQL schema declares `enum SubscriptionPlan { FREE }`.

### D4. Contract first

In `contracts/graphql/account.graphqls`:

- `type User` gains `firstName: String`, `lastName: String`, `subscription: SubscriptionPlan!`, `nationality: String` and `birthCountry: String`.
- `enum SubscriptionPlan { FREE }` is added.

The field order follows the order of the spec. No description is needed beyond the enum values, since the rules are in the spec and the database.

### D5. Tests run against a real database

- `ProfileMigrationIT` migrates a PostGIS container to `V6`, inserts an account, migrates to `V7`, and checks `subscription = 'FREE'` and `NULL` profile columns, as in `FirstVisitMigrationIT`.
- The check constraints are tested with JDBC: a lowercase code, a name and an unknown plan are rejected.
- The GraphQL behaviour is tested with `MeQueryIT` and `RegisterIT`, plus a test that sets the profile columns with JDBC and checks that `me` returns them.
- `AccountContractTest` is updated to the new `User` field list, and it gets a check that no `User` field contains `password`.
- `GraphqlContractIT` already compares `User` with the served schema, so it checks the new fields in both directions.

### D6. No new dependency

Nothing is added to `build.gradle.kts`.

## Risks

- **Empty profile on `me`**: clients must handle `null` for every profile field until a write change exists. This is intended and stated in the spec.
- **Plan values**: only `FREE` exists. Adding a plan later needs a migration that changes the check constraint, which is a new Flyway version and not an edit of `V7`.

## Test approach

- One test per scenario of the spec delta, named in tasks.md.
- Every migration test runs on a fresh PostGIS container, so existing rows are created at the earlier version before the migration runs.
