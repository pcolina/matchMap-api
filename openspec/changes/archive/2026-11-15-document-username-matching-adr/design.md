# Design

Contract baseline: `ca97e41` (main after PR #40). This change touches no file under `contracts/`.

## Context

The ADR is the deliverable: `docs/adr/0003-username-word-matching.md`. The decisions it records are in `openspec/specs/username-moderation/spec.md` and in the code under `src/main/java/com/matchmap/api/moderation/`. This design does not repeat the ADR text; it says what the ADR must cover and how it is checked.

## Goals / Non-Goals

**Goals:**
- One ADR that states the normalization order, the matching rule by length, the cached word list and the rejected alternatives.
- The ADR states the false-positive trade-off, as issue #6 requires.

**Non-Goals:**
- Changing any rule, test or word list.
- The privacy and terms policy.

## Decisions

**D1. ADR location and number.** `docs/adr/0003-username-word-matching.md`, the next free number after `0002`.

**D2. Status.** `Proposed`, as the existing ADRs. Its status is not changed by this change.

**D3. Accuracy check.** Each rule in the ADR is checked against the code before the change is archived: normalization order in `CandidateNormalizer`, matching by length in `ProhibitedWordMatcher`, the snapshot and reload in `WordCache` and `WordSnapshot`, and the Aho-Corasick dependency in `build.gradle.kts`.

## Risks / Trade-offs

- **[The ADR drifts from the code later]** → The ADR cites the change names it came from. A later change that alters a rule must update it.

## Migration Plan

None. The file is added to `docs/adr/`; nothing else depends on it.

## Open Questions

None.
