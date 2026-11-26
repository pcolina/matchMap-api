# Proposal

No GitHub issue is linked. The repository owner opened this change directly.

## Why

The web and mobile apps need the profile of the signed-in user: name, surnames, email, the subscription plan, nationality and country of birth. Today `me` returns only `id`, `email`, `username`, `createdAt` and `isFirstVisit`. The client cannot show a profile, and it cannot tell which plan the account is on.

The owner asked for this change to be completed without questions. The decisions below were taken by the author and are listed so the reviewer can check them.

## What Changes

- **Profile fields on `me`.** The `User` type in `contracts/graphql/account.graphqls` gains:
  - `firstName: String` and `lastName: String`, nullable.
  - `subscription: SubscriptionPlan!`, with the enum `SubscriptionPlan { FREE }`.
  - `nationality: String` and `birthCountry: String`, nullable, holding ISO 3166-1 alpha-2 codes such as `ES`.
- **Default plan.** Every account starts with `subscription` `FREE`. Existing accounts start with `FREE` too.
- **Stored in `user_account`.** A new Flyway migration `V7__add_user_profile.sql` adds the columns `first_name`, `last_name`, `nationality`, `birth_country` (nullable) and `subscription` (not null, default `FREE`, limited to `FREE`), and a check that country codes are two uppercase letters.
- **The first visit flag stays on `me`.** `isFirstVisit` is already part of `User`, so the client reads it from the same query. Nothing changes for it.
- **Password never returned.** `User` has no password field, and this is tested.

## Decisions taken by the author

- **`me` is the endpoint.** The data is the account's own, so it is read from the query that already returns it. No new query is added.
- **Read only.** This change exposes the data. No mutation changes the name, the country or the plan. Those changes need their own proposal, because they include validation rules that the API has to enforce.
- **Nullable profile values.** Names and countries are not asked at registration, so they stay `null` until a later change sets them. This is why the fields are nullable.
- **Country codes, not names.** The API returns ISO codes. Translated country names come from the clients, which send `Accept-Language`, as the API rules already require for country names.
- **Plan name `subscription`.** The owner wrote "método de subscripción", and the plan is the value that is "free" by default, so the field is `subscription` with the enum `SubscriptionPlan`.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `user-accounts`: "Read own account" gains the profile fields. Two requirements are added: the subscription default, and the profile values that are nullable and never include the password.

## Out of scope

- Setting or changing any profile value, including the plan. Separate change.
- Subscription billing or any plan other than `FREE`.
- Translated country names.
- Email changes and username changes.

## Depends on

- `user-accounts` (`openspec/specs/user-accounts`): the account, the `User` type and `me`.
- `authentication` (`openspec/specs/authentication`): the access token that identifies the account.
- `add-first-visit-flag` (archived): the `isFirstVisit` field that `me` already returns.
