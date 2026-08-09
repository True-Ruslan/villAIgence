# Semantic Contradiction Representation Design

## Goal

Persist and query that one NPC holds two contradictory Semantic Memory claims without choosing a winner, changing FACT/BELIEF, changing confidence, or adding semantic inference.

## Chosen representation

Use a structured `SEMANTIC_CONTRADICTION` MemoryEvent in the existing `memory2.json`. This follows the `RELATIONSHIP_CAUSE` precedent: the event records a server-observed process relation, not the truth of either statement.

Do not add contradiction fields to `SemanticMemoryEntry`: consolidation can replace concrete entry IDs and would make bidirectional metadata fragile. Do not add a new JSON store: it would expand recovery/migration/CI surface without need.

## Stable logical claim identity

Extract a shared `SemanticMemoryIdentity` helper from the existing consolidation rules. A logical claim key is:

```text
owner NPC
+ kind
+ provenance
+ canonical NFKC/lowercase/whitespace-normalized statement
+ canonical sorted unique semantic subject scope
```

`SemanticMemoryConsolidator` must delegate to this helper and preserve its current deterministic output exactly.

## Contradiction payload

Add an optional structured payload to `MemoryEvent`:

```java
record SemanticContradiction(ClaimSnapshot first, ClaimSnapshot second) {}

record ClaimSnapshot(
    UUID logicalClaimId,
    UUID detectedSemanticEntryId,
    SemanticMemoryEntry.Kind kind,
    MemoryEvent.Provenance provenance,
    String statement,
    List<UUID> relatedEntities
) {}
```

Snapshots are ordered by `logicalClaimId` ascending. Both claims must belong to the same NPC, have identical canonical semantic subject scope, and have different canonical statement content.

## Event identity and shape

Deterministic ID:

```text
semantic-contradiction-v1
ownerNpcId
min(logicalClaimIdA, logicalClaimIdB)
max(logicalClaimIdA, logicalClaimIdB)
authoritativeGameTime
```

Canonical event:

```text
owner:          NPC
participants:   [NPC]
type:           SEMANTIC_CONTRADICTION
provenance:     SYSTEM_OBSERVED
gameTime:       authoritative Minecraft game time
createdAt:      0
importance:     60
confidence:     100
emotion:        0
summary:        Semantic contradiction recorded
other payloads: null
```

`SYSTEM_OBSERVED` describes only the process relation. It does not alter the claims and this event is never eligible for Semantic FACT ingestion.

Retention reuses the existing MemoryEvent policy. The new type gets the same type contribution as `OBSERVATION`/`ACTION`, so it remains bounded and evictable.

## Lifecycle

```java
SemanticContradictionResult SemanticContradictionLifecycle.record(
    Path worldRoot,
    UUID npcId,
    UUID firstSemanticEntryId,
    UUID secondSemanticEntryId,
    long authoritativeGameTime,
    int maxEventsPerNpc
)
```

Statuses:

```text
RECORDED
REJECTED
SOURCE_NOT_RETAINED
SCOPE_MISMATCH
SAME_CLAIM
EVENT_NOT_RETAINED
```

The caller supplies IDs only. The lifecycle exact-reads both retained Semantic entries, validates owner/scope/content, constructs canonical payload, persists it, rereads exact evidence, and verifies retention. Missing/wrong sources fail closed with zero writes.

Exact replay at the same tuple/time is idempotent. A later detection at another game time is distinct bounded evidence.

## Query

`SemanticContradictionHistory.load(worldRoot, npcId, playerId, maxResults)` returns resolved contradictions newest-first.

A relation is returned only when:

- contradiction evidence is retained;
- both logical claims are still retained by the NPC;
- each current claim resolves by stable logical claim identity;
- the shared semantic scope is eligible for the current player;
- filtering/resolution occurs before `maxResults`.

If either claim is forgotten, historical contradiction evidence must not resurrect its statement into live resolved memory.

This slice does not inject contradiction prose into the LLM prompt. Prompt integration is the next preservation step, so existing `32`, `24+8`, `6` retrieval bounds and current prompt formatting remain unchanged here.

## Invariants

- Contradiction is metadata, never a third claim and never FACT.
- Neither side wins.
- Recording a contradiction changes no Semantic entry field.
- Provider/client cannot submit contradiction prose or source UUIDs through any provider/network schema in this slice.
- FACT stays FACT; BELIEF stays BELIEF; current observed world state remains authoritative.
- Existing rumor lineage stays immutable, acyclic and capped at 8 hops.
- `memory2.json` and `semantic-memory.json` remain format version 1.
- No new store, migration/backfill, public config, provider call, scheduler, UI or autonomous propagation.

## TDD acceptance

1. Logical identity remains stable across source-union consolidation, Unicode/case/whitespace normalization and scope ordering; kind/provenance remain part of identity.
2. A/B and B/A build identical canonical contradiction payloads and IDs for the same game time.
3. Same claim and different semantic scopes fail closed.
4. Missing/wrong-owner sources create no event.
5. Exact replay is byte/idempotent; later game time creates distinct evidence.
6. Pressure may evict contradiction evidence without changing Semantic claims.
7. Resolved history survives restart and source-union consolidation.
8. Global/private/shared scope stays exact and foreign-player data consumes zero result slots.
9. Forgotten live claims are not resurrected by historical contradiction evidence.
10. No contradiction event is converted to Semantic FACT.
11. Existing prompt truth-preservation, long-horizon, privacy and 8-hop rumor regressions remain green.
12. Final exact-head security, CI, production soak and release dry-run must pass; publication stays skipped.

## Non-goals

No automatic contradiction detector, truth arbitration, uncertainty/confidence decay, text distortion, trust weighting, autonomous spread, new UI, provider schema, config, or settlement graph.

## Exit criterion

VillAIgence can persist and deterministically query a bounded server-owned contradiction relation between two exact retained Semantic claims, survive replay/restart/consolidation/pressure/privacy boundaries, stop resolving it when either live claim is forgotten, and prove that disagreement never promotes, rewrites, ranks or resolves either claim.