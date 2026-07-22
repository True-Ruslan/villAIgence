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

- [x] Add RED tests proving exact before/after/applied delta, saturation behavior, no-op `changed=false`, and compatibility of existing `applyDelta`.
- [x] Confirm valid RED on tests-only head `f4c6eaf59c79cb08621c8dcdd31e7317976c3ebd`; `VillAIgence CI` run `29931503379` failed at `:common:compileTestJava` for the missing result contract.
- [x] Implement immutable result with applied delta computed strictly as `after - before`.
- [x] Refactor store so `applyDelta(...)` delegates to `applyDeltaWithResult(...).after()`.
- [x] Preserve existing save-before-return behavior: changed result is returned only after successful `save()`.
- [x] Confirm GREEN on head `1e824314e1cde6fec933dc27df745e73de4bc59e`; `VillAIgence CI` `29931884301` and Java PR CI `29931884183` succeeded.

---

### Task 2: Pure relationship-change MemoryEvent adapter

**Files:**
- Create: `common/src/main/java/net/conczin/mca/livingworld/memory2/RelationshipChangeMemoryAdapter.java`
- Create: `common/src/test/java/net/conczin/mca/livingworld/memory2/RelationshipChangeMemoryAdapterTest.java`

- [x] Add RED tests for mapping, deterministic summary, `SYSTEM_OBSERVED`, scores, empty `relationshipReasons`, no-op rejection, wall-clock-independent ID, and transition-sensitive ID.
- [x] Confirm valid RED on head `f415fde9e52e45bd200f1b73fbde362a3b171e2a`; `VillAIgence CI` `29932123694` failed at `:common:compileTestJava` for the missing adapter.
- [x] Implement deterministic summary with exact applied delta and final state.
- [x] Implement UUID namespace `memory2-relationship-change-v1` using NPC/player IDs, game time, before tuple and after tuple.
- [x] Confirm GREEN on head `289a9327c3ddaeec9568baa820c743238559d449`; `VillAIgence CI` `29932432475` and Java PR CI `29932431905` succeeded.

---

### Task 3: Bounded idempotent persistence bridge

**Files:**
- Create: `common/src/main/java/net/conczin/mca/livingworld/memory2/Memory2RelationshipChangeIngestor.java`
- Create: `common/src/test/java/net/conczin/mca/livingworld/memory2/Memory2RelationshipChangeIngestorTest.java`

- [x] Add RED tests proving disabled/no-op creates no memory, duplicate replay is idempotent, distinct transitions remain distinct, and retention is bounded.
- [x] Confirm valid RED on head `38b614b8508d20f9085dc4290ab9cf95f9ff5ff6`; `VillAIgence CI` `29932750358` failed at `:common:compileTestJava` for the missing ingestor.
- [x] Implement adapter → `MemoryEventStore.forWorld(worldRoot).append(...)` with explicit enablement guard.
- [x] Confirm GREEN on head `4b4390863d6a36523cfeda84c5f5082cb133df3f`; `VillAIgence CI` `29933043431` and Java PR CI `29933043451` succeeded.

---

### Task 4: Post-persistence lifecycle integration

**Files:**
- Modify: `common/src/main/java/net/conczin/mca/entity/ai/chatAI/OpenAIChatAI.java`

**Boundary:** Modify only `applySnapshotRelationshipDelta(...)` and required imports. Do not touch provider request construction, response parser, retry policy, dialogue memory, command handling, or visible-answer logic.

- [x] Replace the existing direct `applyDelta(...)` call with `applyDeltaWithResult(...)` inside the existing relationship persistence try/catch.
- [x] Return immediately on relationship persistence failure.
- [x] If result is unchanged, perform no Memory 2.0 write.
- [x] If changed, invoke `Memory2RelationshipChangeIngestor.recordIfEnabled(...)` in a separate try/catch using `memory2Enabled`, snapshot IDs/gameTime/worldRoot and `memory2MaxEventsPerNpc`.
- [x] Log secondary Memory 2.0 failure separately without changing relationship state or visible response.
- [x] Review per-file patch: only two imports and the narrow relationship helper changed in `OpenAIChatAI`.

---

### Task 5: Documentation and final verification

**Files:**
- Modify: `docs/livingworld/MEMORY_2.md`
- Update: `docs/PROJECT_STATE.md` in an immediate post-merge state-sync PR using the actual merge SHA.

- [x] Document `RELATIONSHIP_CHANGE` server-observed numeric evidence.
- [x] Explicitly state that `relationshipReasons` remains empty and causal explanations are still unimplemented.
- [x] Document exact applied delta vs raw proposed delta.
- [x] Document deterministic event identity and fail-soft ordering.
- [ ] Require exact-final-head `VillAIgence CI` success: `:common:test`, Fabric build, distributable package verification.
- [ ] Require exact-final-head official Gradle CI success: NeoForge + Fabric.
- [ ] Final diff review: no `relationships.json` schema change, no provider/parser/retry/dialogue behavior change, no memory ingestion before successful relationship persistence.
- [ ] Confirm no unresolved review threads/comments.
- [ ] Merge with pinned expected head SHA only after all gates are green.
- [ ] Immediately synchronize canonical `docs/PROJECT_STATE.md` with the actual merge SHA in a separate PR.
