# Persistence Recovery Hardening Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make all VillAIgence world-local JSON stores recover safely from malformed persisted content and document the final 0.1.x manual validation gate before Memory 2.0.

**Architecture:** Preserve each store's existing public API, version-1 schema and atomic write path. Change only load-time exception handling for memory/events/relationships to match the existing fail-open voice-store convention, then prove recovery with per-store regression tests. Keep live multiplayer/voice validation as an explicit operator checklist rather than pretending unit CI simulates a Minecraft server.

**Tech Stack:** Java 21, JUnit 5, Gson, Gradle multi-module build, GitHub Actions, Fabric 1.21.1 with NeoForge compile compatibility.

## Global Constraints

- Minecraft target remains `1.21.1`.
- Java remains `21`.
- Preserve mod id `mca`.
- Preserve package root `net.conczin.mca`.
- Preserve `config/livingworld.json` and `<world>/livingworld/` paths.
- No new dependency, config version, persistence schema version or migration.
- Existing atomic temp-file + replace writes remain unchanged.
- Corrupt/unreadable auxiliary JSON must fail soft to an empty store; the next successful mutation may replace it with valid version-1 JSON.

---

### Task 1: Conversation memory corruption recovery

**Files:**
- Modify: `common/src/test/java/net/conczin/mca/livingworld/memory/ConversationMemoryStoreTest.java`
- Modify: `common/src/main/java/net/conczin/mca/livingworld/memory/ConversationMemoryStore.java`

**Interfaces:**
- Consumes: package-private `ConversationMemoryStore(Path file)`, `getMessages(UUID, UUID)`, `appendExchange(...)`.
- Produces: unchanged public/store API with fail-open load semantics.

- [ ] **Step 1: Write the failing regression test**

Add a JUnit test that writes `{broken` to `memory.json`, constructs `ConversationMemoryStore`, verifies `getMessages(...)` is empty, appends one exchange, reconstructs the store, and verifies the two persisted messages are readable.

- [ ] **Step 2: Run the focused test and confirm RED**

Run:

```bash
./gradlew :common:test --tests net.conczin.mca.livingworld.memory.ConversationMemoryStoreTest
```

Expected before implementation: FAIL because construction throws `IllegalStateException` from `load()`.

- [ ] **Step 3: Implement minimal fail-open load behavior**

Change only the `catch (IOException | RuntimeException e)` branch in `ConversationMemoryStore.load()` from throwing to:

```java
return new MemoryFile();
```

Do not modify format version checks or atomic save behavior.

- [ ] **Step 4: Re-run focused test and confirm GREEN**

Expected: all `ConversationMemoryStoreTest` tests pass.

### Task 2: Factual event journal corruption recovery

**Files:**
- Modify: `common/src/test/java/net/conczin/mca/livingworld/knowledge/WorldEventStoreTest.java`
- Modify: `common/src/main/java/net/conczin/mca/livingworld/knowledge/WorldEventStore.java`

**Interfaces:**
- Consumes: package-private `WorldEventStore(Path file)`, `queryRecent(...)`, `append(WorldEvent, int)`.
- Produces: unchanged API with fail-open load semantics.

- [ ] **Step 1: Write the failing regression test**

Write `{broken` to `events.json`, construct the store, assert an empty query result, append a valid event, reconstruct the store, and assert the event is returned.

- [ ] **Step 2: Confirm RED with focused test**

```bash
./gradlew :common:test --tests net.conczin.mca.livingworld.knowledge.WorldEventStoreTest
```

Expected before implementation: constructor failure from `load()`.

- [ ] **Step 3: Implement minimal fail-open load behavior**

Change the load exception branch to `return new EventFile();` and leave validation/bounds/write semantics untouched.

- [ ] **Step 4: Confirm GREEN**

Expected: all `WorldEventStoreTest` tests pass.

### Task 3: Relationship store corruption recovery

**Files:**
- Modify: `common/src/test/java/net/conczin/mca/livingworld/relationship/LivingWorldRelationshipStoreTest.java`
- Modify: `common/src/main/java/net/conczin/mca/livingworld/relationship/LivingWorldRelationshipStore.java`

**Interfaces:**
- Consumes: package-private `LivingWorldRelationshipStore(Path file)`, `get(UUID, UUID)`, `applyDelta(...)`.
- Produces: unchanged API with fail-open load semantics.

- [ ] **Step 1: Write the failing regression test**

Write `{broken` to `relationships.json`, construct the store, assert `NEUTRAL`, apply a bounded delta, reconstruct the store, and assert the new relationship state persists.

- [ ] **Step 2: Confirm RED**

```bash
./gradlew :common:test --tests net.conczin.mca.livingworld.relationship.LivingWorldRelationshipStoreTest
```

Expected before implementation: constructor failure from `load()`.

- [ ] **Step 3: Implement minimal fail-open load behavior**

Change the exception branch to `return new RelationshipFile();`; do not alter clamping, schema or write path.

- [ ] **Step 4: Confirm GREEN**

Expected: all `LivingWorldRelationshipStoreTest` tests pass.

### Task 4: Final 0.1.x operator validation checklist and canonical state

**Files:**
- Create: `docs/livingworld/PLAYTEST_CHECKLIST.md`
- Modify: `docs/PROJECT_STATE.md`

**Interfaces:**
- Consumes: released `0.1.7+1.21.1`, `/villaigence ai status`, admission settings, four persistent JSON stores.
- Produces: repeatable manual gate for closing 0.1.x and accurate session handoff state.

- [ ] **Step 1: Add the playtest checklist**

Document prerequisites, expected evidence and pass/fail criteria for:

```text
multiplayer concurrent Chat/STT/TTS
repeated voice-dialogue soak
429/provider cooldown recovery
restart/reconnect persistence
backup/restore persistence
/villaigence ai status diagnostics
```

Explicitly state that CI cannot replace live-server evidence for these scenarios.

- [ ] **Step 2: Update canonical release/project state**

Record:

```text
release: 0.1.7+1.21.1
release commit: 8f3095c6e8489e077246d652be51ec3c0ff57cd8
release workflow run: 29913854688 (success)
```

Mark core 0.1.x architecture as implemented, persistence corruption recovery as part of the reliability foundation after this PR, and leave only live multiplayer/voice/restart validation as the exit gate before beginning `0.2 Memory 2.0`.

### Task 5: Full verification and PR

**Files:**
- Review all changed files in this branch.

- [ ] **Step 1: Run common tests**

```bash
./gradlew :common:test
```

Expected: PASS.

- [ ] **Step 2: Run Fabric build**

```bash
./gradlew :fabric:build
```

Expected: PASS.

- [ ] **Step 3: Open PR against `1.21.1`**

PR summary must describe the asymmetric persistence failure found, fail-open recovery semantics, regression coverage, project-state synchronization and remaining manual validation gate.

- [ ] **Step 4: Require fresh GitHub Actions on exact final head**

Required:

```text
VillAIgence CI -> SUCCESS
Java Pull Request CI with Gradle -> SUCCESS
```

- [ ] **Step 5: Final review**

Check no unresolved review threads/comments, no compatibility-path changes, and no accidental schema/config changes before merge.
