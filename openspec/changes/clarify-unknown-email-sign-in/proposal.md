# Proposal

No GitHub issue is linked yet. The repository owner opened this change directly.

## Why

When someone signs in with an email that has no account, the API returns the same `401 invalid_credentials` as a wrong password. The screen only says the credentials are invalid, so the person cannot tell that the email is not registered and may keep retrying instead of creating an account.

The `authentication` spec keeps the same response for wrong passwords, unknown emails and Google-only accounts, so that the API does not reveal which emails exist. This change keeps that rule. The clearer message is shown by the client, from the same response, so the API stays as it is.

Decided: option B. The API does not change. The web and mobile clients change their text.

## What Changes

- After a failed sign-in with `invalid_credentials`, the client shows the message "No encontramos una cuenta con esos datos" together with a button or link "Crear cuenta". The wording is the client's decision and is not tied to the email being unknown.
- When the client registers and the API returns `EMAIL_TAKEN`, the client shows "Ya tienes una cuenta con este email" and a link to sign in. This uses the error the API already returns from `register`.
- No change in `contracts/`, no change in the API, no migration, no new dependency.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

None. The `authentication` and `user-accounts` specs stay as they are, so the change declares `skip_specs: true`.

## Impact

- Code: none in this repository. The work is in the web repository (pcolina/MatchMap) and in the mobile app, which consume the API.
- API: none. The responses from `/api/auth/login` and `register` are unchanged.
- Security: no new information is revealed. `EMAIL_TAKEN` on register already reveals that an email exists, as the `user-accounts` spec requires. This change does not extend that.

## Out of scope

- Option A (a distinct error code for unknown emails). Rejected for now because it reveals which emails exist.
- The "already registered" email sent to the owner, and password recovery. They need email delivery (#26) and are a separate change.
- The registration response that hides whether an email exists. It belongs with email verification (#26).
- Changes to the login throttle.

## Depends on

- `authentication` (requirement "Sign-in failures do not reveal which accounts exist", unchanged).
- `user-accounts` (requirement "Email is normalized and unique", unchanged; its `EMAIL_TAKEN` code is used by the client).
- Issue #26 is related for the later step, not a dependency.
