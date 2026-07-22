# Memory 2.0 Authoritative World-Event Ingestion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ingest successfully persisted server-observed NPC action WorldEvents into bounded actor-owned Memory 2.0 without LLM authority or duplicate side effects.

**Architecture:** Add a pure `WorldEventMemoryAdapter`, a small `Memory2WorldEventIngestor`, version-2-compatible Memory 2.0 config bounds, then invoke ingestion only after `WorldEventStore.append` succeeds inside `WorldEventRecorder`.

**Tech Stack:** Java 21, JUnit 5, Gson config, existing WorldEvent/MemoryEvent stores, Gradle/GitHub Actions.

## Global Constraints

- No prompt integration.
- No LLM/provider call.
- Source must be `SYSTEM_OBSERVED`.
- Memory owner is the acting NPC only.
- Reuse source WorldEvent UUID for idempotency.
- Existing `events.json` and `memory.json` behavior must remain intact.
- Memory 2.0 failure must not invalidate successful gameplay/action event persistence.

---

### Task 1: Pure WorldEvent → MemoryEvent adapter

**Files:**
- Create: `common/src/main/java/net/conczin/mca/livingworld/memory2/WorldEventMemoryAdapter.java`
- Create: `common/src/test/java/net/conczin/mca/livingworld/memory2/WorldEventMemoryAdapterTest.java`

- [ ] Write RED tests for exact field mapping, owner/participants, source UUID reuse and invalid source rejection.
- [ ] Confirm focused tests fail because adapter does not exist.
- [ ] Implement pure conversion with fixed action defaults:

```text
importance=60
emotionalWeight=0
confidence=100
```

- [ ] Confirm focused GREEN.

### Task 2: Idempotent persistence bridge

**Files:**
- Create: `common/src/main/java/net/conczin/mca/livingworld/memory2/Memory2WorldEventIngestor.java`
- Create: `common/src/test/java/net/conczin/mca/livingworld/memory2/Memory2WorldEventIngestorTest.java`

- [ ] Write RED tests proving duplicate ingestion remains one memory, distinct source IDs remain distinct, and retention is bounded.
- [ ] Confirm RED.
- [ ] Implement conversion + `MemoryEventStore.forWorld(worldRoot).append(...)` bridge.
- [ ] Confirm GREEN.

### Task 3: Safe config controls

**Files:**
- Modify: `common/src/main/java/net/conczin/mca/livingworld/LivingWorldConfig.java`
- Modify: `common/src/test/java/net/conczin/mca/livingworld/LivingWorldConfigTest.java`

- [ ] Write/update tests for:

```text
memory2Enabled=true
memory2MaxEventsPerNpc=256
normalization to 1..512
existing version=2 config compatibility
```

- [ ] Add fields without config version bump.
- [ ] Normalize max bound.
- [ ] Confirm config tests GREEN.

### Task 4: Server-observed integration

**Files:**
- Modify: `common/src/main/java/net/conczin/mca/livingworld/knowledge/WorldEventRecorder.java`

- [ ] Keep current safe-action lifecycle unchanged.
- [ ] After successful `WorldEventStore.append`, invoke Memory 2.0 ingestion only when `memory2Enabled`.
- [ ] Use `System.currentTimeMillis()` only as event metadata timestamp; event truth/order remains server game-time/source data.
- [ ] Catch Memory 2.0 ingestion failure separately so it cannot be reported as factual event persistence failure or affect gameplay.

### Task 5: Documentation/state

**Files:**
- Modify: `docs/livingworld/MEMORY_2.md`
- Modify: `docs/livingworld/CONFIGURATION.md`
- Update canonical `docs/PROJECT_STATE.md` in this PR or immediately after merge with actual merge SHA.

- [ ] Document automatic authoritative action-memory ingestion.
- [ ] Document enable/max config.
- [ ] Explicitly document that relationship-reason ingestion remains deferred because current numeric LLM delta has no separately verified reason.
- [ ] Set next slice to bounded Memory 2.0 context injection from ranked memories or a dedicated relationship-reason provenance contract, whichever architectural review selects after this merge.

### Task 6: Verification

- [ ] Require RED evidence from tests-only head.
- [ ] Require fresh `VillAIgence CI` on exact final head.
- [ ] Require fresh Fabric/NeoForge Gradle CI.
- [ ] Review final lifecycle ordering: action success → factual event persisted → Memory 2.0 ingestion.
- [ ] Confirm no failed/rejected action ingestion path exists.
- [ ] Confirm no unresolved review threads/comments before merge.
