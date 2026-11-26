# Tasks

This change has no tasks in this repository: the API does not change (`skip_specs: true`). The work is done in the client repositories, which consume the API through `contracts/`. Each task there follows TDD in that repository.

## Web (pcolina/MatchMap)

- [ ] 1.1 Write a component test: after `invalid_credentials` the sign-in screen shows "No encontramos una cuenta con esos datos" and a "Crear cuenta" action that opens the register screen. Show it failing; implement it.
- [ ] 1.2 Write a component test: when `register` returns `EMAIL_TAKEN`, the register screen shows "Ya tienes una cuenta con este email" with a link to sign in. Show it failing; implement it.

## Mobile

- [ ] 2.1 Write the same two tests for the mobile app, with the same wording. Show them failing; implement them.

## Verification

- [ ] 3.1 Each client repository runs its own full check and reports the summary.
- [ ] 3.2 Confirm the API responses used by the tests are the ones in `contracts/openapi/auth.yaml` and `contracts/graphql/account.graphqls`.
