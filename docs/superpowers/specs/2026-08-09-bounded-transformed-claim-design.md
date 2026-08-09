# Bounded transformed-claim representation — design

## Status

Approved canonical next slice after PR #141. Base: `48c19d3ae520a14fe1448f93c3ef782269733190`.

## Goal

Introduce the first actual social-information distortion primitive while preserving server authority, exact source provenance, deterministic replay, bounded state growth and the FACT/BELIEF truth boundary.

The initial operation is intentionally narrow:

```text
OMIT_TRAILING_SENTENCE
```

A server-owned transfer may remove exactly one trailing sentence from an eligible source claim. No provider-supplied rewrite text is accepted in this slice.

## Product contract

```text
retained source Semantic entry
+ exact server-owned transfer request
+ canonical provenance/fallibility
→ optional deterministic omission transform
→ listener DIALOGUE / NPC_TOLD evidence
→ listener BELIEF / NPC_TOLD
```

Every transformed result remains BELIEF. Current `SYSTEM_OBSERVED` facts remain authoritative on conflict.

## Transformation semantics

### Allowed kind

`OMIT_TRAILING_SENTENCE` only.

The server:

1. normalizes the exact source statement with the existing Semantic 240-code-point policy;
2. finds the final sentence boundary represented by `. `, `! ` or `? `;
3. retains the exact prefix ending at that boundary;
4. rejects the transform if there is no non-empty trailing sentence to omit;
5. never inserts, substitutes, reorders or invents tokens.

Examples:

```text
"The bridge is closed. Repairs finish tomorrow."
→ "The bridge is closed."

"The bridge is closed"
→ NOT_APPLICABLE
```

This is controlled information loss, not open-ended paraphrasing.

## Evidence ownership

Existing `npc-knowledge-transfer-v2` provenance identity and origin snapshot remain unchanged.

`MemoryEvent` gains an optional structured `KnowledgeTransferTransformation` snapshot on NPC transfer DIALOGUE evidence. It is process evidence, not truth evidence.

The transformation snapshot is cumulative and copied forward on later unchanged transfers so the current direct evidence remains sufficient to audit the transformed lineage while retained.

```text
KnowledgeTransferTransformation
  currentStatement
  steps[]   // hard max 1 in this slice

Step
  kind = OMIT_TRAILING_SENTENCE
  sourceStatement
  transformedStatement
  speakerNpcId
  listenerNpcId
  sourceSemanticEntryId
  evidenceEventId
  gameTime
```

The one step must bind to an exact hop already present in the same canonical v2 provenance snapshot.

## Transformation budget

Hard maximum:

```text
MAX_TRANSFORMATIONS = 1
```

Rules:

- a lineage with zero transformations may apply one omission;
- a lineage with one retained transformation may only be transferred unchanged;
- a second transformation request returns `TRANSFORMATION_LIMIT_REACHED`;
- consolidation, corroboration, retry and restart do not reset the budget;
- if direct evidence is unavailable, transformed provenance is not reconstructed from prose and further transfer already fails through the existing provenance-unavailable boundary.

## Replay and identity

The existing transfer evidence UUID remains server-owned and unchanged:

```text
speaker + listener + speakerSemanticEntryId + authoritativeGameTime
```

The transformation snapshot is deterministic from the exact source, transform kind and that evidence ID.

Consequences:

- exact transformed replay at the same game time is byte-idempotent;
- transformed vs non-transformed attempts for the same transfer identity cannot coexist: whichever exact evidence already exists must match or the conflicting retry is rejected;
- no provider/client can choose event IDs.

## Provenance/content validation

For an untransformed lineage:

```text
current Semantic statement == provenance.origin.statement
```

For a transformed lineage:

```text
current Semantic statement == transformation.currentStatement
transformation first sourceStatement == provenance.origin.statement
transformation step is valid for one exact provenance hop
```

`KnowledgeTransferProvenance` itself is not extended or reinterpreted.

## Fallibility integration

PR #141 `sourceDistanceHops` remains derived from canonical provenance.

Resolved retained direct evidence now yields:

```text
transformationsUsed = 0 | 1
```

If the direct evidence is missing, transformation count is unknown rather than falsely reported as zero:

```text
sourcePath=UNRESOLVED
transformationsUsed=UNKNOWN
```

This intentionally tightens the previous unresolved rendering so missing evidence never fabricates a no-transformation claim.

## Prompt and privacy

- transformed Semantic entries consume the same existing Semantic slot;
- no new prompt domain or result slot;
- existing player eligibility, `32 / 24+8 / 6` selection and ranking occur before fallibility rendering;
- transformed text uses the same existing Semantic sanitizer/escaping;
- transformation metadata is server-authored and cannot be forged by statement prose;
- current observed facts remain first and authoritative;
- contradiction remains no-winner metadata.

## Persistence

Additive optional JSON only:

- no new world file;
- `memory2.json` format version remains 1;
- old events deserialize with `knowledgeTransferTransformation = null`;
- no migration/backfill/dual reader;
- `semantic-memory.json` schema remains unchanged.

## Failure states

Add explicit transfer results:

```text
TRANSFORMATION_NOT_APPLICABLE
TRANSFORMATION_LIMIT_REACHED
```

Existing `REJECTED`, `PROVENANCE_UNAVAILABLE`, pressure and retention outcomes remain unchanged.

## Non-goals

This slice does not add:

- provider-supplied transformed wording;
- a second provider request;
- random corruption;
- synonym substitution or semantic rewrite;
- automatic rumor propagation;
- automatic contradiction detection;
- trust/relationship weighting;
- confidence decay or truth scoring;
- new config/UI/scheduler;
- release publication.

## Acceptance criteria

1. An eligible multi-sentence source can be transferred with exactly one deterministic trailing-sentence omission.
2. The listener receives `BELIEF / NPC_TOLD`; confidence and authority are not promoted.
3. Exact original origin statement and transformation step remain inspectable in retained direct evidence.
4. Existing v2 provenance identity/hop rules remain valid and unchanged.
5. A transformed rumor can propagate unchanged through later hops while preserving the one-step transformation snapshot.
6. A second transform request on that lineage is rejected explicitly.
7. Exact retry is byte-idempotent; same-identity conflicting transformed/non-transformed evidence is rejected.
8. Missing direct evidence produces unresolved fallibility with unknown transformation count; no ancestry or transform is reconstructed from prose.
9. Privacy-before-allocation, `32 / 24+8 / 6`, max-8 hops and max-4 contradiction relations remain unchanged.
10. Prompt rendering remains injection-safe and current observed FACT remains authoritative.
11. Pressure/restart/fresh-root tests preserve exact retained transformation state where evidence survives and fail closed where it does not.
12. No provider/config/release identity changes are introduced.