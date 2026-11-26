# Tasks

One task is one commit, with the task id in the body. Each task writes its failing test first, shows it failing, then writes the minimum code to make it pass. Every scenario in `specs/user-accounts/spec.md` is covered by the test named here.

## 1. Tests

- [x] 1.1 Write the tests for the new data, all failing before the implementation:
  - `ProfileMigrationIT.existingAccountsGetFreeSubscriptionAndEmptyProfile` (scenario "Existing accounts have the free plan after the migration" and "Existing accounts have empty profile values after the migration"): migrates a PostGIS container to `V6`, inserts an account, migrates to `V7`, and asserts `subscription = 'FREE'` and the four profile columns are `NULL`.
  - `ProfileMigrationIT.rejectsInvalidPlanAndCountryCodes` (scenarios "The plan is limited to free in the database", "A country code with lowercase letters is rejected" and "A country name is rejected"): after `V7`, each invalid value is rejected by the database. The test first inserts a valid row, so it fails for the right reason before `V7`.
  - `RegisterIT.registeredAccountHasFreeSubscriptionAndEmptyProfile` (scenario "Account created by register has the free plan"): reads the account through `UserAccountRepository`, and asserts `getSubscription()` is `FREE` and the four profile getters return `null`.
  - `MeQueryIT.meReturnsEmptyProfileAndFreePlan` (scenarios "New account has empty profile values" and "Account created by register has the free plan"): `{ me { firstName lastName subscription nationality birthCountry } }` returns `null` for the four fields and `FREE`.
  - `MeQueryIT.storedProfileValuesAppearInMe` (scenario "Stored profile values appear in me"): sets the four columns with JDBC for an account and asserts `me` returns them.
  - `MeQueryIT.passwordHashIsNotQueryable` (scenario "The password hash cannot be queried"): a guard. It passes from the start, because `User` has no such field; it is a guard against a future field, and the commit message says so.
  - `AccountContractTest.userTypeHasNoPasswordField` (scenario "The User type has no password field"): a guard, as above.
  - `AccountContractTest.userTypeHasNoDisplayName` is updated to the new `User` field list; it fails until the contract changes.

## 2. Implementation

- [x] 2.1 Add `V7__add_user_profile.sql` as in design D1. Add the `SubscriptionPlan` enum, the four profile fields and the `subscription` field to `UserAccount` with their getters, and set `subscription` to `FREE` in both constructors (design D2 and D3). Verify `ProfileMigrationIT` and `RegisterIT.registeredAccountHasFreeSubscriptionAndEmptyProfile` pass.
- [x] 2.2 Add the profile fields and `enum SubscriptionPlan { FREE }` to `contracts/graphql/account.graphqls` (contract first, design D4). Verify `MeQueryIT`, `AccountContractTest` and `GraphqlContractIT` pass.

## 3. Verification

- [x] 3.1 Run `.\gradlew.bat check` from `C:\workspace\pablo\matchmap-api` and paste the summary of the result (tests, coverage, format).
- [x] 3.2 Note in the pull request that the web and mobile repositories must run `pnpm codegen` to regenerate their types, and that the profile values are `null` for every account until a later change sets them.
