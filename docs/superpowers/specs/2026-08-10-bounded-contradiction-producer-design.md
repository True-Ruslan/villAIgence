# Bounded Contradiction Candidate/Producer Design

Date: 2026-08-10
Status: approved for implementation by the project owner
Base: `1.21.1` at `92e6eed585d41478033fdebc2591c4745efd7432`

## Problem

VillAIgence can already persist a server-owned `SEMANTIC_CONTRADICTION` relation and expose live disagreements to prompts, but ordinary controlled Semantic Memory admission does not discover candidate disagreement pairs automatically.

The missing slice must add automatic production without turning contradiction detection into truth arbitration, without letting foreign-player data consume bounded work, and without introducing all-pairs growth.

## Options considered

### A. Conservative deterministic producer — selected

Use server-owned candidate selection plus a deliberately narrow deterministic opposition classifier. The first classifier recognizes only explicit polarity inversion where two normalized statements are otherwise identical and differ by exactly one standalone negation token (`not` or `не`).

Advantages:
- no new provider request, schema or prompt coupling;
- deterministic replay and stable CI;
- no model authority over IDs, scope, truth class or winner;
- easy to bound and reason about;
- false positives are strongly constrained.

Trade-off: recall is intentionally incomplete. Antonyms, temporal disagreement, numeric conflicts and free-form semantic opposition remain undetected until a separately justified classifier extension exists.

### B. Provider-assisted bounded pair classification — rejected for this slice

The server could preselect bounded pairs and ask the provider to classify them. This could improve recall but would expand provider protocol, latency/cost and failure modes. A second provider call is explicitly unjustified at this stage; folding classification into the existing reply schema would couple memory maintenance to dialogue availability.

### C. Large deterministic antonym/rule catalogue — rejected

A broad multilingual rule catalogue would look deterministic but would become a fragile language-specific semantic engine with unclear false-positive behavior. It is more complexity than this slice needs.

## Architecture

```text
controlled Semantic FACT/BELIEF admission
→ persist/consolidate/retain Semantic entry
→ resolve the retained logical claim
→ bounded candidate selection
→ duplicate/live-relation suppression
→ max 8 opposition classifications
→ conservative deterministic opposition classifier
→ existing SemanticContradictionLifecycle.record(...)
→ existing memory2.json contradiction evidence
```

The new producer is downstream of Semantic admission. It never participates in deciding whether a claim is admitted and never changes the admitted claim.

### Integration boundary

`ControlledSemanticMemoryIngestor` remains the central controlled Semantic admission boundary. After a FACT or BELIEF is appended, it resolves the retained logical claim (including post-consolidation identity) and invokes the bounded producer.

This automatically covers existing controlled routes including:
- server-observed FACT ingestion;
- `PLAYER_TOLD` BELIEF extraction;
- `NPC_TOLD` knowledge transfer, including transformed claims;
- controlled INFERRED BELIEF admission.

Direct low-level `SemanticMemoryStore.append(...)` remains storage-only and does not gain hidden process side effects. Tests and low-level recovery code can continue to construct store state explicitly.

## Candidate policy

Constants are internal implementation policy, not public configuration:

```text
MAX_CANDIDATES_PER_ADMISSION = 16
MAX_COMPARISONS_PER_ADMISSION = 8
```

Candidate selection is deterministic newest-first using the existing Semantic store order and applies eligibility before result limiting.

A candidate is eligible only when all are true:
- same `ownerNpcId` as the newly retained claim;
- exact canonical `relatedEntities` scope match;
- distinct logical claim identity;
- normalized statement is non-empty;
- normalized statement is not equivalent to the new claim.

Because exact scope filtering happens inside the store predicate before `.limit(...)`, foreign-player/private claims consume zero candidate slots. Cross-owner claims consume zero slots.

At most 16 eligible candidates are materialized. Existing identical contradiction relations are then removed before classifier allocation. At most 8 remaining pairs reach the opposition classifier.

This means one admission can create at most 8 new contradiction relations, so relation-production work grows linearly with admissions rather than through an all-pairs pass.

## Existing-relation suppression

Before classification, the producer snapshots retained valid `SEMANTIC_CONTRADICTION` evidence for the same NPC into canonical unordered logical-claim pair keys.

A pair that already has valid retained contradiction evidence is skipped without spending a comparison slot and without creating a later-game-time duplicate relation.

The existing lifecycle remains the final authority and rereads exact Semantic IDs before persistence. Concurrent forgetting/consolidation therefore fails closed through existing lifecycle statuses.

## Opposition classifier

The initial deterministic classifier is intentionally conservative.

1. Normalize each statement using the existing `SemanticMemoryIdentity` display/canonical rules.
2. Tokenize on Unicode whitespace while preserving all non-whitespace token content.
3. Treat only standalone `not` and `не` as negation tokens.
4. The pair is opposing only if removing exactly one negation token from exactly one side produces the exact token sequence of the other side.
5. Multiple negations, different remaining tokens, reordering, antonyms, numeric differences and temporal wording are `NOT_OPPOSING`.

Examples:

```text
"The gate is open"       ↔ "The gate is not open"   => opposing
"Ворота открыты"         ↔ "Ворота не открыты"      => opposing
"The gate is open"       ↔ "The gate is closed"     => not classified in this slice
"The gate is open. A guard is here." ↔ "The gate is open." => not opposing
```

This classifier records disagreement only. It does not decide which claim is true.

## Transformation interaction

A transformed `BELIEF/NPC_TOLD` is an ordinary live Semantic claim for eligibility. Transformation metadata, source distance and `transformationsUsed` never affect candidate order or truth likelihood.

`OMIT_TRAILING_SENTENCE` cannot by itself satisfy the explicit-negation rule, so source-vs-derived wording difference alone cannot produce a contradiction. The producer does not inspect prose-difference magnitude as evidence of disagreement.

## Persistence and compatibility

No new world file, persistence version, JSON schema field, migration, provider call/schema, public config or release identity change is introduced.

Existing stores remain:
- `semantic-memory.json` format 1;
- `memory2.json` format 1.

Contradictions continue to use existing `semantic-contradiction-v1` evidence identity and `SemanticContradictionLifecycle` validation.

## Truth and authority boundaries

The producer MUST NOT:
- promote BELIEF to FACT;
- demote or delete FACT;
- mutate confidence, importance, provenance or source IDs;
- select a winner;
- use disagreement count as truth likelihood;
- let client/provider choose claim IDs or relation identity.

Current server-observed FACT remains authoritative regardless of how many disagreement relations exist.

## Failure behavior

All invalid input and concurrent-retention failures fail closed or soft:
- missing retained newly admitted claim: producer returns without evidence;
- source disappears between selection and lifecycle reread: existing `SOURCE_NOT_RETAINED` behavior;
- event rejected/evicted: existing lifecycle result is reported in the ephemeral production result only;
- classifier failure is not expected because it is pure deterministic code; no provider/network failure surface is added.

No producer failure rolls back a valid Semantic admission.

## Observability

The producer returns an immutable in-memory result containing:
- eligible candidate count;
- classifier comparison count;
- opposing pair count;
- recorded event IDs.

This exists for deterministic tests and diagnostics only and is not persisted as a new format.

## TDD / acceptance requirements

Implementation must preserve the roadmap progression:

1. RED — bounded candidate selector.
2. RED — privacy/scope/equivalence filtering before allocation.
3. RED — hard comparison budget.
4. RED — explicit-negation classifier behavior.
5. RED — deterministic replay / duplicate suppression.
6. RED — automatic controlled-admission integration with no claim mutation/winner.
7. RED — transformed wording difference alone is not contradiction.
8. RED — pressure/forgetting/restart preservation.
9. Deterministic multi-NPC pressure simulation.
10. Full exact-head repository security, main CI, production soak and release dry-run gates.

## Exit criterion

Ordinary controlled Semantic admission automatically feeds a strictly bounded, deterministic contradiction producer. Foreign-player and incompatible-scope claims consume zero candidate/comparison slots; at most eight pairs are classified per admission; only conservative validated opposition reaches the existing contradiction lifecycle; replay is duplicate-safe; and no claim authority, confidence, provenance or persistence contract is changed.
