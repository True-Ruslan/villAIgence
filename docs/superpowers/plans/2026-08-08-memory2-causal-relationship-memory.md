# Memory 2.0 Causal Relationship Memory Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Persist and query a deterministic server-authored causal link between an exact `RELATIONSHIP_CHANGE` event and the exact successful `DIALOGUE` turn during which that relationship transition occurred, without persisting model-generated reasons as authority.

**Architecture:** Extend `MemoryEvent` with optional typed relationship-transition and relationship-cause payloads, then create a provider-independent cause lifecycle that validates persisted source events before appending a deterministic `RELATIONSHIP_CAUSE`. Preserve the existing numeric relationship mutation timing; carry the exact server-created relationship event through the internal snapshot result so `ChatAI` can create the cause only after exact DIALOGUE persistence succeeds.

**Tech Stack:** Java 21, Fabric/NeoForge shared `common` module, JUnit 5, Gson-backed `MemoryEventStore`, Gradle, GitHub Actions.

## Global Constraints

- `RELATIONSHIP_CAUSE` records only the server-observed association that a validated relationship transition occurred during an exact persisted dialogue turn.
- No free-form LLM reason text, provider reasoning, hidden chain-of-thought, player prose, or NPC prose may become an authoritative cause.
- `RELATIONSHIP_CHANGE` remains `SYSTEM_OBSERVED` numeric server truth; causal history stays a separate event type.
- Only `DIALOGUE_TURN` exists as a cause kind in this slice.
- Source NPC/player IDs, source event IDs, transition snapshot, provenance, confidence, timestamps, and cause kind are server-owned.
- Cause admission requires both exact source events to already exist in the current world-local `MemoryEventStore`.
- Retry/replay must be deterministic and idempotent.
- Existing 0.2 Memory 2.0 entries remain readable; no backfill, dual reader, or legacy `memory.json` migration is introduced.
- `RELATIONSHIP_CAUSE` is not projected into Semantic Memory in this slice.
- Query isolation is exact NPC/player filtering before limiting; results are newest-first with deterministic `gameTime`, `createdAtEpochMillis`, UUID ordering.
- Root `CHANGELOG.md` `[Unreleased]` is updated in the runtime PR.
- Every production change is preceded by an observed failing test.

---

## File map

- Modify `common/src/main/java/net/conczin/mca/livingworld/memory2/MemoryEvent.java` — add optional typed transition/cause payloads and `RELATIONSHIP_CAUSE`.
- Modify `common/src/main/java/net/conczin/mca/livingworld/memory2/RelationshipChangeMemoryAdapter.java` — populate exact before/after transition payload while preserving empty `relationshipReasons`.
- Modify `common/src/main/java/net/conczin/mca/livingworld/memory2/Memory2RelationshipChangeIngestor.java` — add result-bearing ingestion API returning the exact persisted relationship event.
- Create `common/src/main/java/net/conczin/mca/livingworld/memory2/RelationshipCauseMemoryAdapter.java` — pure deterministic validated source-pair to cause-event mapping.
- Create `common/src/main/java/net/conczin/mca/livingworld/memory2/RelationshipCauseLifecycle.java` — verify exact persisted sources and append idempotently.
- Create `common/src/main/java/net/conczin/mca/livingworld/memory2/ResolvedRelationshipCause.java` — immutable query result containing cause, transition snapshot, and optional resolved sources.
- Create `common/src/main/java/net/conczin/mca/livingworld/memory2/RelationshipCausalHistory.java` — exact NPC/player causal-history retrieval.
- Modify `common/src/main/java/net/conczin/mca/entity/ai/chatAI/OpenAIChatAI.java` — return the exact server-created relationship event in internal snapshot metadata.
- Modify `common/src/main/java/net/conczin/mca/entity/ai/chatAI/ChatAI.java` — persist DIALOGUE first, then create causal link from exact returned source events.
- Modify/add tests under `common/src/test/java/net/conczin/mca/livingworld/memory2/` and `common/src/test/java/net/conczin/mca/entity/ai/chatAI/`.
- Modify `CHANGELOG.md` — document the new causal relationship-memory behavior under `[Unreleased]`.

---

### Task 1: Typed relationship transition payload

**Files:**
- Modify: `common/src/main/java/net/conczin/mca/livingworld/memory2/MemoryEvent.java`
- Modify: `common/src/main/java/net/conczin/mca/livingworld/memory2/RelationshipChangeMemoryAdapter.java`
- Test: `common/src/test/java/net/conczin/mca/livingworld/memory2/RelationshipChangeMemoryAdapterTest.java`

**Interfaces:**
- Produces: `MemoryEvent.RelationshipTransition.from(LivingWorldRelationshipState before, LivingWorldRelationshipState after)` represented by eight bounded integer fields.
- Produces: nullable `MemoryEvent.relationshipTransition()` for new relationship events; old deserialized events may return `null`.
- Preserves: `relationshipReasons()` remains `List.of()` for authoritative relationship transitions.

- [ ] **Step 1: Write the failing tests**

Add assertions equivalent to:

```java
assertEquals(new MemoryEvent.RelationshipTransition(10, 4, 1, 7, 12, 3, 0, 8), event.relationshipTransition());
assertEquals(List.of(), event.relationshipReasons());
```

Also construct a compatibility `MemoryEvent` through the existing source-compatible constructor and assert `relationshipTransition() == null`.

- [ ] **Step 2: Run tests to verify RED**

Run:

```bash
./gradlew :common:test --tests net.conczin.mca.livingworld.memory2.RelationshipChangeMemoryAdapterTest
```

Expected: compile/test failure because `MemoryEvent.RelationshipTransition` and `relationshipTransition()` do not exist.

- [ ] **Step 3: Implement the minimal typed payload**

Extend the record tail to:

```java
DialogueExchange dialogue,
RelationshipTransition relationshipTransition,
RelationshipCause relationshipCause
```

Keep source-compatible constructors that pass `null` for new payloads. Add:

```java
public record RelationshipTransition(
        int beforeTrust, int beforeRespect, int beforeFear, int beforeAffinity,
        int afterTrust, int afterRespect, int afterFear, int afterAffinity
) {
    public RelationshipTransition {
        beforeTrust = clampRelationship(beforeTrust);
        beforeRespect = clampRelationship(beforeRespect);
        beforeFear = clampRelationship(beforeFear);
        beforeAffinity = clampRelationship(beforeAffinity);
        afterTrust = clampRelationship(afterTrust);
        afterRespect = clampRelationship(afterRespect);
        afterFear = clampRelationship(afterFear);
        afterAffinity = clampRelationship(afterAffinity);
    }
}
```

`RelationshipChangeMemoryAdapter` derives this exclusively from `change.before()` and `change.after()`.

- [ ] **Step 4: Run focused tests GREEN**

```bash
./gradlew :common:test --tests net.conczin.mca.livingworld.memory2.RelationshipChangeMemoryAdapterTest
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add common/src/main/java/net/conczin/mca/livingworld/memory2/MemoryEvent.java \
        common/src/main/java/net/conczin/mca/livingworld/memory2/RelationshipChangeMemoryAdapter.java \
        common/src/test/java/net/conczin/mca/livingworld/memory2/RelationshipChangeMemoryAdapterTest.java
git commit -m "feat: retain structured relationship transitions"
```

---

### Task 2: Deterministic cause adapter and persisted-source lifecycle

**Files:**
- Create: `common/src/main/java/net/conczin/mca/livingworld/memory2/RelationshipCauseMemoryAdapter.java`
- Create: `common/src/main/java/net/conczin/mca/livingworld/memory2/RelationshipCauseLifecycle.java`
- Modify: `common/src/main/java/net/conczin/mca/livingworld/memory2/MemoryEvent.java`
- Create: `common/src/test/java/net/conczin/mca/livingworld/memory2/RelationshipCauseMemoryAdapterTest.java`
- Create: `common/src/test/java/net/conczin/mca/livingworld/memory2/RelationshipCauseLifecycleTest.java`

**Interfaces:**
- Produces: `MemoryEvent.Type.RELATIONSHIP_CAUSE`.
- Produces: `MemoryEvent.RelationshipCause(CauseKind kind, UUID relationshipChangeEventId, UUID evidenceEventId, RelationshipTransition transitionSnapshot)`.
- Produces: `MemoryEvent.CauseKind.DIALOGUE_TURN` only.
- Produces: `RelationshipCauseLifecycle.recordDialogueTurn(boolean enabled, Path worldRoot, MemoryEvent relationshipChange, MemoryEvent dialogue, UUID playerId, int maxEventsPerNpc) -> Optional<MemoryEvent>`.

- [ ] **Step 1: Write adapter RED tests**

Tests must require a valid pair to create exactly:

```java
MemoryEvent cause = RelationshipCauseMemoryAdapter.toDialogueTurnCause(change, dialogue, player).orElseThrow();
assertEquals(MemoryEvent.Type.RELATIONSHIP_CAUSE, cause.type());
assertEquals(MemoryEvent.Provenance.SYSTEM_OBSERVED, cause.provenance());
assertEquals(100, cause.confidence());
assertEquals(MemoryEvent.CauseKind.DIALOGUE_TURN, cause.relationshipCause().kind());
assertEquals(change.id(), cause.relationshipCause().relationshipChangeEventId());
assertEquals(dialogue.id(), cause.relationshipCause().evidenceEventId());
assertEquals(change.relationshipTransition(), cause.relationshipCause().transitionSnapshot());
assertFalse(cause.summary().contains(dialogue.dialogue().playerMessage()));
assertFalse(cause.summary().contains(dialogue.dialogue().npcReply()));
```

Reject wrong owner, wrong player, non-`RELATIONSHIP_CHANGE`, non-`DIALOGUE`, missing transition payload, null IDs, or source events that do not contain both owner NPC and player.

- [ ] **Step 2: Write lifecycle RED tests**

Persist matching sources into `MemoryEventStore`, call `recordDialogueTurn`, and require one idempotent cause. Then pass an otherwise valid but unpersisted source object and require no write.

- [ ] **Step 3: Run tests to verify RED**

```bash
./gradlew :common:test \
  --tests net.conczin.mca.livingworld.memory2.RelationshipCauseMemoryAdapterTest \
  --tests net.conczin.mca.livingworld.memory2.RelationshipCauseLifecycleTest
```

Expected: compile failure because cause types/classes are absent.

- [ ] **Step 4: Implement minimal adapter**

Use deterministic ID namespace `memory2-relationship-cause-v1` and canonical input:

```text
namespace\nownerNpcId\nrelationshipChangeEventId\nevidenceEventId\nDIALOGUE_TURN
```

Create generic summary only:

```text
Relationship change occurred during dialogue with player.
```

Copy only the structured transition snapshot from the relationship event.

- [ ] **Step 5: Implement lifecycle persisted-source validation**

Use `MemoryEventStore.forWorld(worldRoot).getRecentMatching(...)` to prove both exact source UUIDs are currently stored under the same owner before appending the cause. Never reconstruct missing sources from summaries/provider output.

- [ ] **Step 6: Run focused tests GREEN**

Run the Task 2 command again. Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add common/src/main/java/net/conczin/mca/livingworld/memory2/MemoryEvent.java \
        common/src/main/java/net/conczin/mca/livingworld/memory2/RelationshipCauseMemoryAdapter.java \
        common/src/main/java/net/conczin/mca/livingworld/memory2/RelationshipCauseLifecycle.java \
        common/src/test/java/net/conczin/mca/livingworld/memory2/RelationshipCauseMemoryAdapterTest.java \
        common/src/test/java/net/conczin/mca/livingworld/memory2/RelationshipCauseLifecycleTest.java
git commit -m "feat: persist validated relationship causes"
```

---

### Task 3: Result-bearing relationship ingestion and exact ChatAI orchestration

**Files:**
- Modify: `common/src/main/java/net/conczin/mca/livingworld/memory2/Memory2RelationshipChangeIngestor.java`
- Modify: `common/src/main/java/net/conczin/mca/entity/ai/chatAI/OpenAIChatAI.java`
- Modify: `common/src/main/java/net/conczin/mca/entity/ai/chatAI/ChatAI.java`
- Modify: `common/src/test/java/net/conczin/mca/livingworld/memory2/Memory2RelationshipChangeIngestorTest.java`
- Modify/create focused `OpenAIChatAI`/`ChatAI` structural or orchestration tests following existing package conventions.

**Interfaces:**
- Produces: `Memory2RelationshipChangeIngestor.recordAndReturnIfEnabled(...) -> Optional<MemoryEvent>`.
- Produces: `Memory2RelationshipChangeIngestor.recordAndReturn(...) -> Optional<MemoryEvent>`.
- Preserves existing `void record*` methods by delegating and ignoring the return.
- Extends internal `OpenAIChatAI.SnapshotAnswer` with `Optional<MemoryEvent> relationshipChangeEvent` while preserving compatibility constructors used by current tests.

- [ ] **Step 1: Write ingestion RED test**

Require:

```java
MemoryEvent persisted = Memory2RelationshipChangeIngestor.recordAndReturn(
        tempDir, npc, player, 100L, change, 16, false, 16, 1000L
).orElseThrow();
assertEquals(persisted.id(), MemoryEventStore.forWorld(tempDir).getRecent(npc, 16).getFirst().id());
```

Disabled/unchanged/null-world cases return `Optional.empty()`.

- [ ] **Step 2: Write orchestration RED tests**

Require the snapshot path to carry only the server-created relationship event; no structured provider field may supply relationship source UUID/cause reason. Require `ChatAI` source ordering:

```text
answerDetailed -> exact relationship event
Memory2DialogueLifecycle.recordSuccessful -> exact dialogue event
RelationshipCauseLifecycle.recordDialogueTurn -> cause
PlayerToldBeliefLifecycle.recordCandidatesIfEnabled -> beliefs
```

No relationship event or failed DIALOGUE persistence means no cause admission.

- [ ] **Step 3: Run tests to verify RED**

```bash
./gradlew :common:test --tests net.conczin.mca.livingworld.memory2.Memory2RelationshipChangeIngestorTest
./gradlew :common:test --tests '*ChatAI*'
```

Expected: failures because result-bearing APIs and causal orchestration do not yet exist.

- [ ] **Step 4: Implement result-bearing ingestion**

Append first, then return exactly the event produced by `RelationshipChangeMemoryAdapter`. Semantic FACT ingestion keeps using that same event. Existing void methods delegate to the new result-bearing methods.

- [ ] **Step 5: Implement OpenAI snapshot metadata**

Change `applySnapshotRelationshipDelta(...)` to return `Optional<MemoryEvent>`. It returns empty for disabled/null/no-change/persistence failure. Put this optional into `SnapshotAnswer`; provider parsing remains unchanged.

- [ ] **Step 6: Implement ChatAI causal ordering**

After `rememberMemory2Dialogue(...)` returns the exact DIALOGUE event, call:

```java
if (sourceEvent.isPresent() && snapshotAnswer.relationshipChangeEvent().isPresent()) {
    RelationshipCauseLifecycle.recordDialogueTurn(
            config.memory2Enabled,
            snapshot.worldRoot(),
            snapshotAnswer.relationshipChangeEvent().orElseThrow(),
            sourceEvent.orElseThrow(),
            snapshot.playerId(),
            config.memory2MaxEventsPerNpc
    );
}
```

Wrap auxiliary persistence failure using the existing bounded warning pattern. Do not roll back the already-persisted numeric relationship state.

- [ ] **Step 7: Run focused tests GREEN**

Run the Task 3 commands again. Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add common/src/main/java/net/conczin/mca/livingworld/memory2/Memory2RelationshipChangeIngestor.java \
        common/src/main/java/net/conczin/mca/entity/ai/chatAI/OpenAIChatAI.java \
        common/src/main/java/net/conczin/mca/entity/ai/chatAI/ChatAI.java \
        common/src/test/java/net/conczin/mca/livingworld/memory2/Memory2RelationshipChangeIngestorTest.java \
        common/src/test/java/net/conczin/mca/entity/ai/chatAI
git commit -m "feat: link relationship changes to dialogue turns"
```

---

### Task 4: Queryable causal history and restart behavior

**Files:**
- Create: `common/src/main/java/net/conczin/mca/livingworld/memory2/ResolvedRelationshipCause.java`
- Create: `common/src/main/java/net/conczin/mca/livingworld/memory2/RelationshipCausalHistory.java`
- Create: `common/src/test/java/net/conczin/mca/livingworld/memory2/RelationshipCausalHistoryTest.java`
- Modify if needed: `common/src/test/java/net/conczin/mca/livingworld/memory2/MemoryEventStoreTest.java`

**Interfaces:**
- Produces: `RelationshipCausalHistory.getRecent(Path worldRoot, UUID npcId, UUID playerId, int maxResults) -> List<ResolvedRelationshipCause>`.
- Produces `ResolvedRelationshipCause(MemoryEvent causeEvent, MemoryEvent.RelationshipTransition transition, UUID relationshipChangeEventId, Optional<MemoryEvent> relationshipChangeEvent, UUID evidenceEventId, Optional<MemoryEvent> evidenceEvent)`.

- [ ] **Step 1: Write query RED tests**

Create causes for multiple NPC/player pairs and require exact filtering before limit. Require newest-first deterministic order. After source eviction, require returned optionals empty while IDs and transition snapshot remain intact.

- [ ] **Step 2: Write restart RED test**

Persist source/cause data, instantiate a fresh store view from the same file path using the package-visible constructor or existing recovery-test pattern, then assert the cause payload and source IDs deserialize unchanged.

- [ ] **Step 3: Run tests to verify RED**

```bash
./gradlew :common:test --tests net.conczin.mca.livingworld.memory2.RelationshipCausalHistoryTest
```

Expected: compile failure because query types are absent.

- [ ] **Step 4: Implement query**

Filter `RELATIONSHIP_CAUSE` events by exact owner + player participant first, then newest-first limit. Resolve referenced events only by exact UUID/type/owner from the current store. Missing references remain `Optional.empty()`.

- [ ] **Step 5: Run focused tests GREEN**

Run the Task 4 command again. Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add common/src/main/java/net/conczin/mca/livingworld/memory2/ResolvedRelationshipCause.java \
        common/src/main/java/net/conczin/mca/livingworld/memory2/RelationshipCausalHistory.java \
        common/src/test/java/net/conczin/mca/livingworld/memory2/RelationshipCausalHistoryTest.java
git commit -m "feat: query causal relationship history"
```

---

### Task 5: Truth-boundary regression, changelog, and complete verification

**Files:**
- Modify/create structural regression tests in `common/src/test/java/net/conczin/mca/livingworld/memory2/` and `common/src/test/java/net/conczin/mca/entity/ai/chatAI/`.
- Modify: `CHANGELOG.md`

**Interfaces:**
- Preserves: structured provider response contains numeric `relationshipDelta` but no free-form relationship-cause/reason field.
- Preserves: `RelationshipChangeMemoryAdapter` writes `List.of()` into legacy `relationshipReasons`.
- Preserves: `ControlledSemanticMemoryIngestor` does not admit `RELATIONSHIP_CAUSE` as an automatic Semantic FACT.

- [ ] **Step 1: Add regression tests before any hardening production edit**

Use structural/source contract tests already established in the repository to assert:

```text
no relationshipReason / causeReason provider field
relationshipReasons remains empty for RELATIONSHIP_CHANGE
RELATIONSHIP_CAUSE is not Semantic FACT eligible
classic/Inworld answer paths do not invoke RelationshipCauseLifecycle
```

- [ ] **Step 2: Run focused regression tests**

```bash
./gradlew :common:test --tests '*Relationship*' --tests '*Semantic*' --tests '*ChatAI*'
```

Expected: PASS unless a real boundary regression is exposed; any RED here must be fixed minimally with another explicit TDD cycle.

- [ ] **Step 3: Update canonical changelog**

Under root `[Unreleased]`, add one concise product entry describing deterministic server-authored dialogue-to-relationship causal history, source UUID linkage, retry/restart persistence, and the explicit prohibition on model-generated authoritative reasons.

- [ ] **Step 4: Run complete local/shared verification**

```bash
./gradlew :common:test
./gradlew build
```

Then use repository CI selectors for runtime + Memory 2.0 persistence changes and require the exact PR head to pass security, required GameTests, Fabric/NeoForge, startup/restart, persistence recovery, package smoke, production soak, and release dry-run with publication skipped.

- [ ] **Step 5: Independent diff review**

Review exact-head diff for authority leakage, source mismatches, retry duplication, persistence compatibility, accidental prompt behavior changes, and unresolved P0/P1/P2 findings. Fix any valid finding through a new RED/GREEN cycle.

- [ ] **Step 6: Commit final docs/hardening**

```bash
git add CHANGELOG.md common/src/test/java
git commit -m "docs: record causal relationship memory"
```

- [ ] **Step 7: Merge only exact verified head**

Before squash merge, verify the PR head SHA has not moved since all required gates/review completed. Merge with exact expected head SHA and then reconcile `docs/PROJECT_STATE.md` / `docs/ROADMAP.md` in a separate documentation-only handoff PR if the repository governance continues to prefer that pattern.
