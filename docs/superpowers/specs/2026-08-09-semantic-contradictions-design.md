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

`SemanticMemoryConsolidator` delegates to this helper while preserving the existing `semantic-consolidated-v1` deterministic output exactly. The logical claim ID deliberately ignores source-event IDs so a claim remains identifiable after deterministic source-union consolidation replaces its concrete Semantic entry ID.

## Contradiction payload

Add an optional structured payload to `MemoryEvent`:

```java
record SemanticContradiction(ClaimSnapshot first, ClaimSnapshot second) {}

record ClaimSnapshot(
    UUID logicalClaimId,
    UUID detectedSemanticEntryId,
    SemanticMemoryEntry.Kind kind,
    MemoryEvent.Provenance provenance,
    List<UUID> relatedEntities
) {}
```

The payload deliberately does **not** duplicate claim text. `logicalClaimId` commits to canonical claim content, while live query text always comes from the currently retained Semantic entry. This avoids duplicating potentially sensitive/large remembered text into a second persistence location and prevents historical contradiction evidence from resurrecting forgotten claim prose.

Snapshots are ordered by `logicalClaimId` ascending. Both claims must belong to the same NPC, have identical canonical semantic subject scope, and have different canonical statement content.

## Event identity and shape

The deterministic event UUID commits to the full canonical stored snapshots, not merely their logical claim IDs:

```text
semantic-contradiction-v1
ownerNpcId
first.logicalClaimId
first.detectedSemanticEntryId
first.kind
first.provenance
first.relatedEntities...
second.logicalClaimId
second.detectedSemanticEntryId
second.kind
second.provenance
second.relatedEntities...
authoritativeGameTime
```

Because `SemanticContradiction` canonicalizes first/second ordering by logical claim UUID, A/B and B/A produce the same event identity for the same authoritative game time. Binding the concrete detected entry ID, kind, provenance, and canonical scope into the UUID makes accidental persisted snapshot mutation fail closed during offline event validation as well as during live resolution.

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

The generic episodic/social prompt path explicitly excludes `SEMANTIC_CONTRADICTION`. Otherwise its `SYSTEM_OBSERVED` provenance would be rendered as a generic `VERIFIED` memory line even though dedicated contradiction prompt semantics have not been designed yet. This slice exposes contradiction state only through `SemanticContradictionHistory`; prompt integration remains a later slice.

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

The caller supplies IDs only. The lifecycle exact-reads both retained Semantic entries, rereads the same exact immutable snapshots authoritatively before construction, validates owner/scope/content, constructs canonical payload, persists it, rereads exact evidence, and verifies retention. Missing/wrong sources fail closed with zero writes.

Exact replay at the same canonical snapshots/time is idempotent. Reversing A/B does not change identity. A later detection at another authoritative game time is distinct bounded evidence.

## Query

`SemanticContradictionHistory.load(worldRoot, npcId, playerId, maxResults)` returns resolved contradictions newest-first by event `gameTime DESC`, then event UUID ascending.

A relation is returned only when:

- contradiction evidence is retained and passes canonical integrity validation;
- both logical claims are still retained by the NPC;
- each current claim resolves by stable logical claim identity;
- resolved kind/provenance/scope still match the stored snapshot;
- the shared semantic scope is eligible for the current player under the existing Semantic Memory eligibility contract;
- filtering/resolution occurs before `maxResults`.

The concrete `detectedSemanticEntryId` is audit evidence for the detection turn, not a permanent live pointer: source-union consolidation may replace that concrete entry while retaining the same logical claim. If either logical claim is actually forgotten, historical contradiction evidence must not resurrect its statement into live resolved memory.

This slice does not inject contradiction prose into the LLM prompt. `Memory2ContextProvider` explicitly filters contradiction events from the generic episodic context. Existing `32`, `24+8`, `6` retrieval bounds and current prompt formatting remain unchanged here.

## Invariants

- Contradiction is metadata, never a third claim and never FACT.
- Neither side wins.
- Recording a contradiction changes no Semantic entry field.
- Claim text is not duplicated into contradiction evidence.
- Provider/client cannot submit contradiction prose or source UUIDs through any provider/network schema in this slice.
- FACT stays FACT; BELIEF stays BELIEF; current observed world state remains authoritative.
- Generic episodic prompt formatting never labels contradiction evidence as a standalone verified claim.
- Existing rumor lineage stays immutable, acyclic and capped at 8 hops.
- `memory2.json` and `semantic-memory.json` remain format version 1.
- No new store, migration/backfill, public config, provider call, scheduler, UI or autonomous propagation.

## TDD acceptance

1. Logical identity remains stable across source-union consolidation, Unicode/case/whitespace normalization and scope ordering; kind/provenance remain part of identity.
2. Existing consolidated Semantic IDs remain byte-compatible after identity extraction.
3. A/B and B/A build identical canonical contradiction payloads and IDs for the same game time.
4. The event UUID binds exact detected-entry/kind/provenance/scope snapshots so persisted field mutation fails closed.
5. Same claim and different semantic scopes fail closed.
6. Missing/wrong-owner sources create no event.
7. Exact replay is byte/idempotent; later game time creates distinct evidence.
8. Pressure may evict contradiction evidence without changing Semantic claims.
9. Resolved history survives restart and source-union consolidation.
10. Global/private/shared scope stays exact and foreign-player data consumes zero result slots.
11. Forgotten live claims are not resurrected by historical contradiction evidence.
12. No contradiction event is converted to Semantic FACT or generic episodic prompt content.
13. Existing prompt truth-preservation, long-horizon, privacy and 8-hop rumor regressions remain green.
14. Final exact-head security, CI, production soak and release dry-run must pass; publication stays skipped.

## Non-goals

No automatic contradiction detector, truth arbitration, uncertainty/confidence decay, text distortion, trust weighting, autonomous spread, new UI, provider schema, config, or settlement graph.

## Exit criterion

VillAIgence can persist and deterministically query a bounded server-owned contradiction relation between two exact retained Semantic claims, survive replay/restart/consolidation/pressure/privacy boundaries, stop resolving it when either live claim is forgotten, avoid duplicating forgotten claim text into historical contradiction evidence, and prove that disagreement never promotes, rewrites, ranks or resolves either claim.