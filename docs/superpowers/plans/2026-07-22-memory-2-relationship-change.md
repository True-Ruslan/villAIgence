# Memory 2.0 Server-Observed Relationship Change Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Persist deterministic server-observed `RELATIONSHIP_CHANGE` MemoryEvents only for real successfully persisted player↔NPC relationship transitions.

**Architecture:** Preserve existing `applyDelta(...)` compatibility and add `applyDeltaWithResult(...)` returning exact `before/after/appliedDelta`. Convert only changed persisted results through a pure Memory 2.0 adapter and append through a thin ingestor. Integrate after successful relationship persistence in the existing snapshot relationship helper with a separate fail-soft Memory 2.0 boundary.

**Tech Stack:** Java 21, JUnit 5, existing `LivingWorldRelationshipStore`, `LivingWorldRelationshipState`, `LivingWorldRelationshipDelta`, `MemoryEvent`, `MemoryEventStore`, Gradle/GitHub Actions.

## Global Constraints

- Existing `applyDelta(...) -> LivingWorldRelationshipState` remains source-compatible.
- `relationships.json` format/version remains unchanged.
- Memory event type: `RELATIONSHIP_CHANGE`.
- Memory provenance: `SYSTEM_OBSERVED` only for actual persisted numeric transition.
- `relationshipReasons = []`; no free-form causal reason in this slice.
- Fixed metadata: `importance=55`, `emotionalWeight=0`, `confidence=100`.
- Deterministic event UUID excludes wall-clock timestamp.
- No memory event for no-op/saturated-zero result, disabled Memory 2.0, or relationship persistence failure.
- Memory 2.0 failure must not roll back successful relationship persistence.
- No provider/parser/retry/dialogue schema changes.

---

### Task 1: Exact relationship mutation result

**Files:**
- Create: `common/src/main/java/net/conczin/mca/livingworld/relationship/LivingWorldRelationshipChange.java`
- Modify: `common/src/main/java/net/conczin/mca/livingworld/relationship/LivingWorldRelationshipStore.java`
- Modify/Test: `common/src/test/java/net/conczin/mca/livingworld/relationship/LivingWorldRelationshipStoreTest.java`

**Interfaces:**
- Produces: `LivingWorldRelationshipChange(before, after, appliedDelta)` and `boolean changed()`.
- Produces: `LivingWorldRelationshipStore.applyDeltaWithResult(UUID, UUID, LivingWorldRelationshipDelta, int)`.
- Preserves: existing `applyDelta(...)` returning `LivingWorldRelationshipState`.

- [ ] Add RED tests proving exact before/after/applied delta, saturation behavior, no-op `changed=false`, and compatibility of existing `applyDelta`.
- [ ] Run `VillAIgence CI` on tests-only head and confirm expected compile/test failure because the new result contract does not exist.
- [ ] Implement immutable result with applied delta computed strictly as `after - before`.
- [ ] Refactor store so `applyDelta(...)` delegates to `applyDeltaWithResult(...).after()`.
- [ ] Preserve existing save-before-return behavior: changed result is returned only after successful `save()`.
- [ ] Run focused/full tests to GREEN.

Expected core logic:

```java
public synchronized LivingWorldRelationshipChange applyDeltaWithResult(
        UUID villagerId,
        UUID playerId,
        LivingWorldRelationshipDelta proposed,
        int maxDeltaPerTurn
) {
    LivingWorldRelationshipState before = get(villagerId, playerId);
    if (villagerId == null || playerId == null || proposed == null) {
        return LivingWorldRelationshipChange.between(before, before);
    }
    LivingWorldRelationshipState after = before.apply(proposed, maxDeltaPerTurn);
    if (!after.equals(before)) {
        data.relationships.put(key(villagerId, playerId), after);
        save();
    }
    return LivingWorldRelationshipChange.between(before, after);
}
```

---

### Task 2: Pure relationship-change MemoryEvent adapter

**Files:**
- Create: `common/src/main/java/net/conczin/mca/livingworld/memory2/RelationshipChangeMemoryAdapter.java`
- Create: `common/src/test/java/net/conczin/mca/livingworld/memory2/RelationshipChangeMemoryAdapterTest.java`

**Interfaces:**

```java
Optional<MemoryEvent> toMemoryEvent(
    UUID npcId,
    UUID playerId,
    long gameTime,
    LivingWorldRelationshipChange change,
    long createdAtEpochMillis
)
```

- [ ] Add RED tests for mapping, deterministic summary, `SYSTEM_OBSERVED`, scores, empty `relationshipReasons`, no-op rejection, wall-clock-independent ID, and transition-sensitive ID.
- [ ] Confirm RED because adapter does not exist.
- [ ] Implement deterministic summary:

```text
Relationship with player changed: trust +2, respect 0, fear -1, affinity +1; now trust=12, respect=4, fear=0, affinity=8.
```

- [ ] Implement UUID namespace `memory2-relationship-change-v1` using NPC/player IDs, game time, before tuple and after tuple.
- [ ] Confirm GREEN.

---

### Task 3: Bounded idempotent persistence bridge

**Files:**
- Create: `common/src/main/java/net/conczin/mca/livingworld/memory2/Memory2RelationshipChangeIngestor.java`
- Create: `common/src/test/java/net/conczin/mca/livingworld/memory2/Memory2RelationshipChangeIngestorTest.java`

**Interfaces:**

```java
recordIfEnabled(
    boolean enabled,
    Path worldRoot,
    UUID npcId,
    UUID playerId,
    long gameTime,
    LivingWorldRelationshipChange change,
    int maxEventsPerNpc,
    long createdAtEpochMillis
)
```

- [ ] Add RED tests proving disabled/no-op creates no memory, duplicate replay is idempotent, distinct transitions remain distinct, and retention is bounded.
- [ ] Confirm RED.
- [ ] Implement adapter → `MemoryEventStore.forWorld(worldRoot).append(...)`.
- [ ] Confirm GREEN.

---

### Task 4: Post-persistence lifecycle integration

**Files:**
- Modify: `common/src/main/java/net/conczin/mca/entity/ai/chatAI/OpenAIChatAI.java`

**Boundary:** Modify only `applySnapshotRelationshipDelta(...)` and required imports. Do not touch provider request construction, response parser, retry policy, dialogue memory, command handling, or visible-answer logic.

- [ ] Replace the existing direct `applyDelta(...)` call with `applyDeltaWithResult(...)` inside the existing relationship persistence try/catch.
- [ ] Return immediately on relationship persistence failure.
- [ ] If result is unchanged, perform no Memory 2.0 write.
- [ ] If changed, invoke `Memory2RelationshipChangeIngestor.recordIfEnabled(...)` in a separate try/catch using `memory2Enabled`, snapshot IDs/gameTime/worldRoot and `memory2MaxEventsPerNpc`.
- [ ] Log secondary Memory 2.0 failure separately without changing relationship state or visible response.
- [ ] Review diff to prove only the narrow relationship helper changed in `OpenAIChatAI`.

Target flow:

```java
LivingWorldRelationshipChange change;
try {
    change = LivingWorldRelationshipStore.forWorld(snapshot.worldRoot()).applyDeltaWithResult(...);
} catch (RuntimeException e) {
    log relationship persistence failure;
    return;
}
if (!change.changed()) return;
try {
    Memory2RelationshipChangeIngestor.recordIfEnabled(...);
} catch (RuntimeException e) {
    log secondary Memory 2.0 failure;
}
```

---

### Task 5: Documentation and final verification

**Files:**
- Modify: `docs/livingworld/MEMORY_2.md`
- Modify: `docs/superpowers/specs/2026-07-22-memory-2-relationship-change-design.md` only if implementation review changes the final architecture.
- Update: `docs/PROJECT_STATE.md` in an immediate post-merge state-sync PR using the actual merge SHA.

- [ ] Document `RELATIONSHIP_CHANGE` server-observed numeric evidence.
- [ ] Explicitly state that `relationshipReasons` remains empty and causal explanations are still unimplemented.
- [ ] Document exact applied delta vs raw proposed delta.
- [ ] Document deterministic event identity and fail-soft ordering.
- [ ] Require exact-final-head `VillAIgence CI` success: `:common:test`, Fabric build, distributable package verification.
- [ ] Require exact-final-head official Gradle CI success: NeoForge + Fabric.
- [ ] Final diff review: no `relationships.json` schema change, no provider/parser/retry/dialogue behavior change, no memory ingestion before successful relationship persistence.
- [ ] Confirm no unresolved review threads/comments.
- [ ] Merge with pinned expected head SHA only after all gates are green.
