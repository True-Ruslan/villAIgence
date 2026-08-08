# Memory 2.0 Causal Relationship Memory Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Persist and query a deterministic server-authored causal link between an exact `RELATIONSHIP_CHANGE` event and the exact successful `DIALOGUE` turn during which that relationship transition occurred, without persisting model-generated reasons as authority.

**Architecture:** First retain the exact server-applied before/after relationship state as a typed payload. Then add a separate `RELATIONSHIP_CAUSE` event admitted only from two already-persisted matching Memory 2.0 source events. Preserve current relationship mutation timing and carry only the server-created relationship event through `OpenAIChatAI.SnapshotAnswer`; `ChatAI` creates the cause only after exact DIALOGUE persistence succeeds.

**Tech Stack:** Java 21, shared `common` module, JUnit 5, Gson `MemoryEventStore`, Gradle, GitHub Actions.

## Global Constraints

- `RELATIONSHIP_CAUSE` means only: this accepted transition occurred during this exact persisted dialogue turn.
- No free-form provider/player/NPC explanation becomes an authoritative cause.
- Cause kind is only `DIALOGUE_TURN` in this slice.
- Owner/player IDs, source IDs, transition snapshot, provenance, confidence, timestamps, and cause kind are server-owned.
- Both exact source events must already exist in the same world-local `MemoryEventStore` before cause admission.
- Retry/replay is deterministic and idempotent.
- Existing 0.2 entries remain readable; no backfill, dual reader, or legacy `memory.json` migration.
- `RELATIONSHIP_CAUSE` is not automatically projected into Semantic Memory.
- Query filtering by exact NPC/player happens before limiting; order is newest-first with existing deterministic Memory 2.0 ordering.
- Root `CHANGELOG.md` `[Unreleased]` changes in the runtime PR.
- No production code before an observed failing test for that behavior.

---

### Task 1: Retain exact relationship transitions

**Files:**
- Modify: `common/src/main/java/net/conczin/mca/livingworld/memory2/MemoryEvent.java`
- Modify: `common/src/main/java/net/conczin/mca/livingworld/memory2/RelationshipChangeMemoryAdapter.java`
- Test: `common/src/test/java/net/conczin/mca/livingworld/memory2/RelationshipChangeMemoryAdapterTest.java`

**Interfaces:**
- Produces: `MemoryEvent.RelationshipTransition(int beforeTrust, int beforeRespect, int beforeFear, int beforeAffinity, int afterTrust, int afterRespect, int afterFear, int afterAffinity)`.
- Produces: nullable `MemoryEvent.relationshipTransition()`; absent on historical/non-relationship events.
- Preserves: `relationshipReasons()` remains empty for authoritative relationship changes.

- [ ] **Step 1: Write failing tests** requiring:

```java
assertEquals(
        new MemoryEvent.RelationshipTransition(10, 4, 1, 7, 12, 3, 0, 8),
        event.relationshipTransition()
);
assertEquals(List.of(), event.relationshipReasons());
```

Also require the current source-compatible `MemoryEvent` constructor to produce `relationshipTransition() == null`.

- [ ] **Step 2: Verify RED**

```bash
./gradlew :common:test --tests net.conczin.mca.livingworld.memory2.RelationshipChangeMemoryAdapterTest
```

Expected: compile failure because `RelationshipTransition`/accessor are absent.

- [ ] **Step 3: Implement minimal production** by adding only `RelationshipTransition relationshipTransition` after `DialogueExchange dialogue` in `MemoryEvent`. Existing constructors delegate with `null`. Clamp each transition value to the same `[-100, 100]` relationship domain. `RelationshipChangeMemoryAdapter` constructs the payload only from `change.before()` / `change.after()`.

- [ ] **Step 4: Verify GREEN** with the same focused command, then commit:

```bash
git commit -am "feat: retain structured relationship transitions"
```

---

### Task 2: Admit deterministic causal events from persisted sources

**Files:**
- Modify: `common/src/main/java/net/conczin/mca/livingworld/memory2/MemoryEvent.java`
- Create: `common/src/main/java/net/conczin/mca/livingworld/memory2/RelationshipCauseMemoryAdapter.java`
- Create: `common/src/main/java/net/conczin/mca/livingworld/memory2/RelationshipCauseLifecycle.java`
- Create: `common/src/test/java/net/conczin/mca/livingworld/memory2/RelationshipCauseMemoryAdapterTest.java`
- Create: `common/src/test/java/net/conczin/mca/livingworld/memory2/RelationshipCauseLifecycleTest.java`

**Interfaces:**
- Add `MemoryEvent.Type.RELATIONSHIP_CAUSE`.
- Add `MemoryEvent.CauseKind.DIALOGUE_TURN`.
- Add `MemoryEvent.RelationshipCause(CauseKind kind, UUID relationshipChangeEventId, UUID evidenceEventId, RelationshipTransition transitionSnapshot)` and nullable `relationshipCause()` payload.
- Produce `RelationshipCauseMemoryAdapter.toDialogueTurnCause(MemoryEvent relationshipChange, MemoryEvent dialogue, UUID playerId) -> Optional<MemoryEvent>`.
- Produce `RelationshipCauseLifecycle.recordDialogueTurn(boolean enabled, Path worldRoot, MemoryEvent relationshipChange, MemoryEvent dialogue, UUID playerId, int maxEventsPerNpc) -> Optional<MemoryEvent>`.

- [ ] **Step 1: Write adapter RED tests**. A valid pair must yield `RELATIONSHIP_CAUSE`, `SYSTEM_OBSERVED`, confidence 100, exact source IDs and exact copied transition snapshot. Summary must be exactly generic and must not contain dialogue text. Reject wrong owner/player/type, missing transition, and missing owner/player participants.

```java
assertEquals("Relationship change occurred during dialogue with player.", cause.summary());
assertEquals(change.id(), cause.relationshipCause().relationshipChangeEventId());
assertEquals(dialogue.id(), cause.relationshipCause().evidenceEventId());
```

- [ ] **Step 2: Write lifecycle RED tests**. Persist matching sources, require one cause; replay must stay one. Pass an otherwise valid but unpersisted source event and require no cause.

- [ ] **Step 3: Verify RED**

```bash
./gradlew :common:test \
  --tests net.conczin.mca.livingworld.memory2.RelationshipCauseMemoryAdapterTest \
  --tests net.conczin.mca.livingworld.memory2.RelationshipCauseLifecycleTest
```

Expected: compile failure because cause types/classes are absent.

- [ ] **Step 4: Implement minimal production**. Deterministic UUID input is:

```text
memory2-relationship-cause-v1
ownerNpcId
relationshipChangeEventId
evidenceEventId
DIALOGUE_TURN
```

`RelationshipCauseLifecycle` verifies both exact UUIDs are currently persisted under the owner using `MemoryEventStore.getRecentMatching` before append. It never reconstructs missing sources from summaries or provider output.

- [ ] **Step 5: Verify GREEN**, then commit:

```bash
git add common/src/main/java/net/conczin/mca/livingworld/memory2 common/src/test/java/net/conczin/mca/livingworld/memory2
git commit -m "feat: persist validated relationship causes"
```

---

### Task 3: Carry the exact relationship event through ChatAI orchestration

**Files:**
- Modify: `common/src/main/java/net/conczin/mca/livingworld/memory2/Memory2RelationshipChangeIngestor.java`
- Modify: `common/src/main/java/net/conczin/mca/entity/ai/chatAI/OpenAIChatAI.java`
- Modify: `common/src/main/java/net/conczin/mca/entity/ai/chatAI/ChatAI.java`
- Modify: `common/src/test/java/net/conczin/mca/livingworld/memory2/Memory2RelationshipChangeIngestorTest.java`
- Modify/create focused `OpenAIChatAI` / `ChatAI` contract tests in their existing test package.

**Interfaces:**
- Add `recordAndReturnIfEnabled(...) -> Optional<MemoryEvent>` and `recordAndReturn(...) -> Optional<MemoryEvent>`; existing void methods delegate and ignore the result.
- `OpenAIChatAI.applySnapshotRelationshipDelta(...) -> Optional<MemoryEvent>`.
- `OpenAIChatAI.SnapshotAnswer` gains server-only `Optional<MemoryEvent> relationshipChangeEvent`, with compatibility constructor(s) for existing tests/callers.

- [ ] **Step 1: Write ingestion RED test**:

```java
MemoryEvent persisted = Memory2RelationshipChangeIngestor.recordAndReturn(
        tempDir, npc, player, 100L, change, 16, false, 16, 1000L
).orElseThrow();
assertEquals(persisted.id(), MemoryEventStore.forWorld(tempDir).getRecent(npc, 16).getFirst().id());
```

Disabled/unchanged/null-world paths return empty.

- [ ] **Step 2: Write orchestration RED tests** requiring this order:

```text
answerDetailed -> server-created RELATIONSHIP_CHANGE metadata
Memory2DialogueLifecycle.recordSuccessful -> exact DIALOGUE
RelationshipCauseLifecycle.recordDialogueTurn -> cause
PlayerToldBeliefLifecycle.recordCandidatesIfEnabled -> beliefs
```

Also assert there is no structured provider field for a cause UUID or free-form relationship reason.

- [ ] **Step 3: Verify RED**

```bash
./gradlew :common:test --tests net.conczin.mca.livingworld.memory2.Memory2RelationshipChangeIngestorTest
./gradlew :common:test --tests '*ChatAI*'
```

- [ ] **Step 4: Implement minimal production**. `Memory2RelationshipChangeIngestor` appends and returns the same adapter-created event; Semantic FACT ingestion reuses it. `OpenAIChatAI` returns the optional server-created event but does not change provider parsing. `ChatAI` calls `RelationshipCauseLifecycle` only after `rememberMemory2Dialogue` returns the exact persisted DIALOGUE. Cause-persistence failure is logged and does not roll back numeric relationship state.

- [ ] **Step 5: Verify GREEN**, then commit:

```bash
git add common/src/main/java/net/conczin/mca/entity/ai/chatAI \
        common/src/main/java/net/conczin/mca/livingworld/memory2 \
        common/src/test/java/net/conczin/mca
git commit -m "feat: link relationship changes to dialogue turns"
```

---

### Task 4: Query causal history and survive restart/source eviction

**Files:**
- Create: `common/src/main/java/net/conczin/mca/livingworld/memory2/ResolvedRelationshipCause.java`
- Create: `common/src/main/java/net/conczin/mca/livingworld/memory2/RelationshipCausalHistory.java`
- Create: `common/src/test/java/net/conczin/mca/livingworld/memory2/RelationshipCausalHistoryTest.java`

**Interfaces:**
- `RelationshipCausalHistory.getRecent(Path worldRoot, UUID npcId, UUID playerId, int maxResults) -> List<ResolvedRelationshipCause>`.
- `ResolvedRelationshipCause(MemoryEvent causeEvent, MemoryEvent.RelationshipTransition transition, UUID relationshipChangeEventId, Optional<MemoryEvent> relationshipChangeEvent, UUID evidenceEventId, Optional<MemoryEvent> evidenceEvent)`.

- [ ] **Step 1: Write RED tests** covering exact NPC/player isolation before limit, newest-first deterministic order, restart deserialization, and source eviction where source optionals become empty but IDs/transition snapshot remain intact.

- [ ] **Step 2: Verify RED**

```bash
./gradlew :common:test --tests net.conczin.mca.livingworld.memory2.RelationshipCausalHistoryTest
```

- [ ] **Step 3: Implement minimal query**. Read only `RELATIONSHIP_CAUSE` events containing exact owner/player participants, limit after filtering, and resolve referenced source events by exact UUID/type/owner. Missing references remain `Optional.empty()`; never synthesize prose.

- [ ] **Step 4: Verify GREEN**, then commit:

```bash
git add common/src/main/java/net/conczin/mca/livingworld/memory2 \
        common/src/test/java/net/conczin/mca/livingworld/memory2
git commit -m "feat: query causal relationship history"
```

---

### Task 5: Truth-boundary regression and exact-head verification

**Files:**
- Modify/create relevant relationship/semantic/chat structural tests.
- Modify: `CHANGELOG.md`

**Interfaces preserved:**
- provider response may propose bounded numeric `relationshipDelta`, but no cause/reason authority field;
- `relationshipReasons` remains empty for `RELATIONSHIP_CHANGE`;
- `RELATIONSHIP_CAUSE` is not automatically Semantic FACT eligible;
- classic/Inworld paths remain outside this slice.

- [ ] **Step 1: Add regression tests first** for those four boundaries. Any discovered regression gets its own observed RED before production hardening.

- [ ] **Step 2: Run focused regressions**

```bash
./gradlew :common:test --tests '*Relationship*' --tests '*Semantic*' --tests '*ChatAI*'
```

- [ ] **Step 3: Update root `CHANGELOG.md` `[Unreleased]`** with deterministic dialogue-to-relationship causal history, source UUID linkage, retry/restart behavior, and explicit rejection of model-generated authoritative reasons.

- [ ] **Step 4: Run complete verification**

```bash
./gradlew :common:test
./gradlew build
```

Then require exact PR head success for repository security, selected server GameTests, Fabric + NeoForge builds, startup/restart acceptance, five-store persistence recovery, package smoke, production soak, and GitHub Release dry-run with publication skipped.

- [ ] **Step 5: Independent exact-head review** for authority leakage, source mismatch, replay duplication, persistence compatibility, accidental prompt behavior changes, and P0/P1/P2 findings. Fix valid findings through a new RED/GREEN cycle.

- [ ] **Step 6: Commit final changelog/hardening**

```bash
git add CHANGELOG.md common/src/test/java
git commit -m "docs: record causal relationship memory"
```

- [ ] **Step 7: Merge only the verified exact head SHA**, then reconcile `docs/PROJECT_STATE.md` / `docs/ROADMAP.md` in a separate docs-only handoff PR if current repository governance remains unchanged.
