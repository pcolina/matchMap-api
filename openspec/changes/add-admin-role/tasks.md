# Tasks

Every task follows TDD: write the failing test first, run it and show it failing, write the minimum code that makes it pass, then refactor. Each task is one commit with `Task: <id>` in the body.

## 1. Role storage

- [x] 1.1 Test: a new account has the role `USER`. Covers "New account gets the default role". Then add `V8_3__add_user_role.sql` with the `role` column, its default and its check constraint.
- [x] 1.2 Test: `me` returns the role of the signed-in account. Covers "Me returns the role". Then expose `role` on `User` in `contracts/graphql/account.graphqls` and return it.

## 2. Role check

- [x] 2.1 Test: an `ADMIN` account can call a catalog-changing operation. Covers "Administrator changes the catalog". Then add the role check.
- [ ] 2.2 Test: a non-administrator is rejected with `FORBIDDEN` and no row changes. Covers "Non-administrator changes the catalog". Then return the error defined in the spec.

## 3. First administrator

- [ ] 3.1 Test: the documented database command sets `ADMIN` on one account. Covers "First administrator is set by the database command". Then write the command in the operator documentation.
