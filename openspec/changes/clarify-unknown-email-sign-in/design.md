# Design

Contract baseline: `ca97e41` (main after PR #40). This change touches no file under `contracts/`. Contract changes since the baseline are checked with `git log ca97e41..HEAD -- contracts/`.

## Context

`AuthController.login` returns `401 invalid_credentials` for a wrong password, an unknown email and a Google-only account. `register` returns `EMAIL_TAKEN` when the email is already in use. Both responses are already documented in the contracts and in the specs, and neither changes here.

## Decisions

**D1. The API does not change.** The clients decide what to show from the codes they already receive. This keeps the `authentication` rule that sign-in failures do not reveal which accounts exist.

**D2. Sign-in failure shows a create-account hint.** For `invalid_credentials`, the client shows a neutral message and a "Crear cuenta" action. The message does not claim the email is unknown, because the response cannot tell that.

**D3. Registration with a taken email shows a sign-in hint.** For `EMAIL_TAKEN`, the client shows "Ya tienes una cuenta con este email" and a link to sign in.

**D4. Wording is in the clients.** The API returns codes, not text. Each client owns its translations.

## Risks

- A person with a wrong password also sees the "Crear cuenta" hint. This is accepted: the hint is neutral and the create-account screen will reject the email if it is taken.
- Until email verification (#26) exists, a person who forgot the password has no in-app recovery. The sign-in screen can say so, but the wording is a client decision and is not specified here.

## Later steps (not in this change)

- #26: registration responds the same for new and taken emails, and sends the owner a notice when the email already exists.
- Password recovery, as a separate change that needs email delivery.
