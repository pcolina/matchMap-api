# Design

## Context

Change 2 of #6 added the sign-up check. `RegistrationService.register` assigns a username with `assignUsername`. If the base name matches a word, it asks `UsernameGenerator.fallbackUsername(taken)` for a free fallback. That method makes at most five attempts and returns an empty `Optional` when all of them collide. `assignUsername` then throws `IllegalStateException("No free fallback username")`, which GraphQL reports as an internal error. No test exercises that branch.

The `username-moderation` spec in `openspec/specs/` has a `Purpose` section that still reads as the placeholder written when change 1 was archived.

Baseline for contract changes: `f3f3d67` (main after PR #33). This change touches no file under `contracts/`.

## Goals / Non-Goals

**Goals**
- Replace the placeholder `Purpose` with the real purpose of the capability.
- Cover the internal-error branch of sign-up with one unit test.

**Non-Goals**
- No requirement, scenario or behaviour change, so no delta spec.
- No change to `RegistrationService`, `UsernameGenerator` or any contract.

## Decisions

**Purpose is edited directly, not through a delta.** The archive sync only applies requirement operations (`ADDED`, `MODIFIED`, `REMOVED`, `RENAMED`). It does not touch the `Purpose` section of an existing main spec. The task therefore edits `openspec/specs/username-moderation/spec.md` by hand. `skip_specs: true` tells the CLI that the change has no delta.

**Purpose text.** It states what the capability owns:
- which usernames are rejected: those containing an active prohibited or reserved word;
- how a candidate is normalized: lowercase, diacritics removed, leetspeak and homoglyphs mapped to letters, separators and repeated letters removed;
- how words are matched: a word longer than four characters matches as a substring, a shorter word matches as the whole candidate or as one segment separated by `.`, `_`, `-` or a space;
- where the words come from: the active rows of `prohibited_word` and `reserved_word`;
- how sign-up uses the result: a fallback username, never an error.

**Test with Mockito, default validator.** `RegistrationService` takes its collaborators in its constructor, so the test builds it directly:
- `UserAccountRepository` is a mock. `existsByEmail` returns `false`.
- `UsernameGenerator` is a mock. `baseFor` returns a base name, and `fallbackUsername(any())` returns `Optional.empty()`.
- `UsernameModeration` is a mock. `isBlocked` returns `true`.
- `PasswordEncoder` is a mock.
- `Validator` is `Validation.buildDefaultValidatorFactory().getValidator()`.

The test asserts that `register` throws `IllegalStateException` and that `saveAndFlush` is never called. Mockito is already available through `spring-boot-starter-test`, so no dependency is added.

## Risks / Trade-offs

- **Purpose text can drift** from the requirements. It is short and describes the capability at a high level. It is reviewed when the capability changes.
- **A Mockito test does not prove the GraphQL error shape.** The test covers the service branch only. The GraphQL error mapping is the default for unhandled exceptions and is not changed here.

## Test strategy

- One unit test in `RegistrationServiceTest` for the internal-error branch.
- The existing `RegisterWordCheckIT` and `RegisterIT` keep passing unchanged.
