# Memory 2.0 Server-Observed Relationship Change Design

## Context

Memory 2.0 now has persistent events, deterministic bounded retrieval, authoritative safe-action ingestion, bounded context integration, and controlled successful dialogue ingestion.

The remaining social-memory gap is relationship state. VillAIgence already persists server-owned player↔NPC relationship dimensions:

```text
trust
respect
fear
affinity
```

The current LLM proposes bounded numeric deltas. The server clamps and persists the actual resulting state.

The server can prove the numeric relationship transition that was persisted. It cannot prove a free-form psychological explanation for why the transition happened.

## Goal

Persist one deterministic `MemoryEvent.Type.RELATIONSHIP_CHANGE` only when a relationship delta produces a real persisted state change.

The memory records server-observed evidence of the actual applied numeric transition. It must not invent or promote an LLM-generated causal explanation.

## Chosen approach

Use an explicit immutable relationship mutation result:

```text
LivingWorldRelationshipChange
├── before
├── after
├── appliedDelta
└── changed
```

`LivingWorldRelationshipStore.applyDeltaWithResult(...)` performs the existing clamp/apply/save lifecycle and returns the exact persisted transition.

The existing public `applyDelta(...) -> LivingWorldRelationshipState` remains for compatibility and delegates to the richer method.

After a successful relationship persistence returns a changed result, a dedicated Memory 2.0 adapter converts that result into a server-observed relationship memory.

## Why not use the proposed LLM delta directly

The proposed delta is not necessarily the applied delta.

Example:

```text
current trust = 99
proposed trust = +5
maxDeltaPerTurn = 5
final trust = 100
actual applied trust delta = +1
```

Memory must record `+1`, not `+5`.

Therefore the Memory 2.0 event is derived from persisted `before → after`, not from raw model output.

## LivingWorldRelationshipChange

New immutable record:

```text
before: LivingWorldRelationshipState
after: LivingWorldRelationshipState
appliedDelta: LivingWorldRelationshipDelta
```

Contract:

- null states normalize to `NEUTRAL` only if ever constructed defensively outside the store;
- `appliedDelta` is computed from `after - before` for each dimension;
- `changed()` is true iff before and after differ;
- the result contains no model text, prompt text, player transcript, or provider metadata.

## Store compatibility

Existing method remains:

```java
LivingWorldRelationshipState applyDelta(
    UUID villagerId,
    UUID playerId,
    LivingWorldRelationshipDelta proposed,
    int maxDeltaPerTurn
)
```

It delegates to:

```java
LivingWorldRelationshipChange applyDeltaWithResult(
    UUID villagerId,
    UUID playerId,
    LivingWorldRelationshipDelta proposed,
    int maxDeltaPerTurn
)
```

Lifecycle:

```text
load current state
→ sanitize/clamp proposed delta through existing state.apply(...)
→ compute updated state
→ if changed: persist relationships.json
→ return before/after/appliedDelta
```

A changed result is returned only after the existing save succeeds. If save throws, no Memory 2.0 ingestion may occur.

## RelationshipChangeMemoryAdapter

Pure provider/Minecraft-independent mapping inputs:

```text
npcId
playerId
gameTime
LivingWorldRelationshipChange
createdAtEpochMillis
```

Returns no event when:

- NPC/player ID missing;
- change missing;
- `changed() == false`;
- applied delta is all zero.

### Mapping

```text
MemoryEvent.type                 = RELATIONSHIP_CHANGE
MemoryEvent.ownerNpcId           = npcId
MemoryEvent.participants         = [npcId, playerId]
MemoryEvent.provenance           = SYSTEM_OBSERVED
MemoryEvent.gameTime             = snapshot gameTime
MemoryEvent.createdAtEpochMillis = ingestion timestamp
MemoryEvent.importance           = 55
MemoryEvent.emotionalWeight      = 0
MemoryEvent.confidence           = 100
MemoryEvent.relationshipReasons  = []
```

`SYSTEM_OBSERVED` is valid because the event describes only the numeric state transition actually persisted by server code.

`relationshipReasons` remains empty. A numeric transition is evidence that the relationship state changed; it is not a validated causal explanation.

### Deterministic summary

Store exact applied deltas and final state in a fixed deterministic format:

```text
Relationship with player changed: trust +2, respect 0, fear -1, affinity +1; now trust=12, respect=4, fear=0, affinity=8.
```

All four dimensions are always present. Positive deltas include `+`; zero is `0`.

No free-form LLM text enters the summary.

### Deterministic identity

Canonical event ID input:

```text
memory2-relationship-change-v1
npcId
playerId
gameTime
before trust,respect,fear,affinity
after trust,respect,fear,affinity
```

Use `UUID.nameUUIDFromBytes(... UTF-8 ...)`.

Wall-clock timestamp is excluded.

This makes replay/redelivery of the exact same persisted transition idempotent while distinct transitions naturally produce distinct IDs.

## Memory2RelationshipChangeIngestor

Thin bridge:

```text
RelationshipChangeMemoryAdapter
→ MemoryEventStore.forWorld(worldRoot)
→ bounded append using memory2MaxEventsPerNpc
```

Provide `recordIfEnabled(...)` so `memory2Enabled=false` is an explicit no-op.

No Minecraft entity access, provider access, relationship mutation, or prompt logic belongs in this class.

## Lifecycle integration

The existing snapshot relationship helper in `OpenAIChatAI` is the narrowest correct integration point because it owns the parsed relationship delta today.

Do not modify provider request construction, retry logic, parser logic, visible-answer handling, command handling, or dialogue ingestion.

Final flow:

```text
usable structured answer
→ applySnapshotRelationshipDelta(...)
→ relationshipStateEnabled check
→ LivingWorldRelationshipStore.applyDeltaWithResult(...)
→ relationships.json save succeeds when changed
→ if changed and memory2Enabled:
   Memory2RelationshipChangeIngestor
   → memory2.json append
```

Use two separate fail-soft boundaries:

1. relationship persistence failure:
   - log existing relationship persistence warning;
   - return;
   - do not attempt Memory 2.0 ingestion.

2. secondary Memory 2.0 persistence failure:
   - log a Memory 2.0 warning;
   - do not alter or roll back the already persisted relationship state;
   - do not affect visible reply or other post-success effects.

## Truth/provenance boundary

Authoritative:

```text
before state
actual applied numeric delta
after state
```

Not authoritative in this slice:

```text
why the player/NPC felt that way
what sentence caused the change
an LLM explanation of motive
moral interpretation
```

Those future explanations may later be stored as `PLAYER_TOLD`, `NPC_TOLD`, or `INFERRED`, but must never be silently upgraded to `SYSTEM_OBSERVED`.

## Non-goals

- free-form relationship reasons;
- LLM-generated causal summaries;
- new structured-response fields;
- NPC↔NPC social graph;
- legacy MCA hearts migration;
- semantic consolidation;
- embeddings/vector search;
- changing relationship score policy or max-delta rules;
- changing `relationships.json` format version.

## Testing

Tests must prove:

1. `applyDeltaWithResult` reports exact before/after/applied delta;
2. saturation/bounds produce actual applied delta, not raw proposed delta;
3. zero/no-op transitions report `changed=false`;
4. existing `applyDelta` behavior remains compatible;
5. adapter maps changed transitions to `RELATIONSHIP_CHANGE` / `SYSTEM_OBSERVED` with confidence 100;
6. summary contains exact applied delta and final state in deterministic format;
7. `relationshipReasons` remains empty;
8. deterministic ID ignores wall-clock time but changes for a different transition/game time;
9. disabled Memory 2.0 or unchanged relationship creates no memory;
10. duplicate replay is idempotent;
11. retention remains bounded through existing `MemoryEventStore`;
12. integration diff proves Memory 2.0 ingestion occurs only after successful relationship persistence result;
13. provider/parser/retry/dialogue-memory behavior remains unchanged.

## Success criteria

- real persisted relationship changes produce bounded server-observed `RELATIONSHIP_CHANGE` memories;
- actual applied deltas are recorded accurately under clamping/saturation;
- no-op transitions produce no memory;
- duplicate replay cannot multiply the same relationship-change memory;
- no invented causal reason is stored as authoritative;
- existing `applyDelta` callers remain source-compatible;
- `relationships.json` schema/version remains unchanged;
- Memory 2.0 failure cannot roll back relationship persistence;
- exact-final-head unit tests, Fabric package verification, and Fabric/NeoForge CI pass.
