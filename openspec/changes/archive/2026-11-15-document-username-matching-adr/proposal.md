# Proposal

Refs #6

## Why

Issue #6 has an open item: an architecture decision record (ADR) on normalization and matching, with the false-positive trade-offs. The decisions are already implemented and specified in `username-moderation`, but they are not written down in one place for the next reader.

## What Changes

- Adds `docs/adr/0003-username-word-matching.md`, which records the normalization order, the length-based matching rule, the cached word list and the alternatives considered.
- No code, contract, schema or specification changes.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

None. This change documents existing behaviour and changes no requirement, so it sets `skip_specs: true`.

## Impact

- Documentation only: `docs/adr/`.
- No test, dependency or runtime change.

## Out of scope

- The privacy and terms policy for #6, which depends on the decisions in #31.
- Any change to the matching rules or the word lists.

## Depends on

- `username-moderation` (describes the behaviour the ADR records).
- Issue #6 (the ADR item stays open until the policy item is also done).
