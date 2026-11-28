# Design

Contract baseline for the Definition of Done check: `defb537`, the commit where this change was proposed.

## Decisions

- **Roles.** Two values, `ADMIN` and `USER`, with `USER` as default. The subscription plan is a separate attribute (`subscription`), so a future `PRO` plan does not add roles.
- **Storage.** The role is a column `role varchar(10) NOT NULL DEFAULT 'USER'` on the account table, with a check constraint on `ADMIN` and `USER`, added by `V8_3__add_user_role.sql`.
- **Check.** The role is read from the database for each protected catalog operation, so a role change takes effect without a new token.
- **First administrator.** A documented database command sets `role = 'ADMIN'` for one account. No API operation does it.
- **Error for a missing role.** `extensions.classification` `FORBIDDEN`, next to the `UNAUTHORIZED` already defined by `authentication`.

## Risks

- A wrong default role would give every new account catalog rights. The default must be the lowest role.

## Dependencies

No new dependency is added.
