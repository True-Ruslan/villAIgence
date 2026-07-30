# Memory 2.0 Controlled Semantic Ingestion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Persist provenance-safe semantic FACT entries from authoritative action and relationship events while exposing an explicit sourced BELIEF API and keeping ordinary dialogue episodic-only.

**Architecture:** A pure adapter converts eligible inputs to deterministic `SemanticMemoryEntry` values. A small persistence facade writes those values through the existing store. Existing episodic action and relationship ingestors append episodic memory first and then optionally append semantic FACT, preserving compatibility through overloads.

**Tech Stack:** Java 21, JUnit 5, Gson-backed world-local JSON persistence, Gradle multi-loader builds for Fabric and NeoForge.

## Global Constraints

- Minecraft remains exactly `1.21.1`.
- Java remains exactly `21`.
- Fabric remains the primary distributable target.
- NeoForge compile compatibility remains mandatory.
- No provider call, embeddings, vector database, or LLM truth classification.
- `DIALOGUE` must never be automatically converted to Semantic Memory.
- `FACT` requires `SYSTEM_OBSERVED`; BELIEF rejects it.
- Existing persistent file formats remain compatible.
- Production changes follow RED → GREEN → refactor.

---

### Task 1: Define controlled semantic conversion contracts

**Files:**
- Create: `common/src/test/java/net/conczin/mca/livingworld/memory2/SemanticMemoryIngestionAdapterTest.java`
- Create: `common/src/main/java/net/conczin/mca/livingworld/memory2/SemanticBeliefSource.java`
- Create: `common/src/main/java/net/conczin/mca/livingworld/memory2/SemanticMemoryIngestionAdapter.java`

**Interfaces:**
- Produces: `SemanticMemoryIngestionAdapter.toFact(MemoryEvent): Optional<SemanticMemoryEntry>`
- Produces: `SemanticMemoryIngestionAdapter.toBelief(SemanticBeliefSource): SemanticMemoryEntry`
- Produces immutable `SemanticBeliefSource` with explicit non-empty source event IDs.

- [ ] **Step 1: Write failing adapter tests**

Cover:

```java
Optional<SemanticMemoryEntry> fact = SemanticMemoryIngestionAdapter.toFact(sourceAction);
assertEquals(SemanticMemoryEntry.Kind.FACT, fact.orElseThrow().kind());
assertEquals(MemoryEvent.Provenance.SYSTEM_OBSERVED, fact.orElseThrow().provenance());
assertEquals(List.of(sourceAction.id()), fact.orElseThrow().sourceEventIds());
```

Also assert:

```java
assertTrue(SemanticMemoryIngestionAdapter.toFact(dialogue).isEmpty());
assertThrows(IllegalArgumentException.class, () -> new SemanticBeliefSource(... SYSTEM_OBSERVED ...));
assertThrows(IllegalArgumentException.class, () -> new SemanticBeliefSource(... List.of() source ids ...));
```

Verify normalized statements are bounded to 240 Unicode code points and repeated conversion yields the same UUID.

- [ ] **Step 2: Run RED CI**

Run through the repository PR workflows. Expected: compilation failure because `SemanticBeliefSource` and `SemanticMemoryIngestionAdapter` do not exist.

- [ ] **Step 3: Implement `SemanticBeliefSource`**

Constructor requirements:

```java
if (ownerNpcId == null) throw new IllegalArgumentException("ownerNpcId is required");
if (statement == null || statement.isBlank()) throw new IllegalArgumentException("statement is required");
if (provenance == null || provenance == MemoryEvent.Provenance.SYSTEM_OBSERVED) {
    throw new IllegalArgumentException("BELIEF requires told or inferred provenance");
}
if (sourceEventIds == null || sourceEventIds.stream().filter(Objects::nonNull).findAny().isEmpty()) {
    throw new IllegalArgumentException("BELIEF requires sourceEventIds");
}
```

Normalize IDs by insertion order, trim statement, clamp game time and numeric scores.

- [ ] **Step 4: Implement `SemanticMemoryIngestionAdapter`**

Eligibility:

```java
private static boolean eligibleFact(MemoryEvent source) {
    return source != null
            && source.provenance() == MemoryEvent.Provenance.SYSTEM_OBSERVED
            && source.type() != MemoryEvent.Type.DIALOGUE;
}
```

Limit accepted types explicitly to `ACTION`, `OBSERVATION`, and `RELATIONSHIP_CHANGE`.

Build deterministic IDs with `UUID.nameUUIDFromBytes` and UTF-8 canonical strings. Normalize whitespace/control characters and cap statements at 240 code points before ID generation and persistence.

- [ ] **Step 5: Run focused tests and full unit tests**

Expected: adapter tests pass and existing Semantic Memory tests remain green.

- [ ] **Step 6: Commit**

```bash
git commit -m "feat: add controlled semantic conversion"
```

---

### Task 2: Add the semantic persistence facade

**Files:**
- Create: `common/src/test/java/net/conczin/mca/livingworld/memory2/ControlledSemanticMemoryIngestorTest.java`
- Create: `common/src/main/java/net/conczin/mca/livingworld/memory2/ControlledSemanticMemoryIngestor.java`

**Interfaces:**

```java
recordFactIfEnabled(boolean enabled, Path worldRoot, MemoryEvent source, int maxEntriesPerNpc)
recordFact(Path worldRoot, MemoryEvent source, int maxEntriesPerNpc)
recordBeliefIfEnabled(boolean enabled, Path worldRoot, SemanticBeliefSource source, int maxEntriesPerNpc)
recordBelief(Path worldRoot, SemanticBeliefSource source, int maxEntriesPerNpc)
```

- [ ] **Step 1: Write failing persistence tests**

Prove:

```java
ControlledSemanticMemoryIngestor.recordFact(tempDir, source, 8);
ControlledSemanticMemoryIngestor.recordFact(tempDir, source, 8);
assertEquals(1, SemanticMemoryStore.forWorld(tempDir).getRecent(npc, 8).size());
```

Also prove disabled writes create no entry and explicit BELIEF persists with unchanged provenance/source IDs.

- [ ] **Step 2: Verify RED**

Expected: test compilation fails because the facade does not exist.

- [ ] **Step 3: Implement minimal facade**

Use the pure adapter and existing store only:

```java
SemanticMemoryIngestionAdapter.toFact(source)
        .ifPresent(entry -> SemanticMemoryStore.forWorld(worldRoot).append(entry, maxEntriesPerNpc));
```

No provider, prompt, or Minecraft entity dependency.

- [ ] **Step 4: Run focused and full unit tests**

Expected: all pass.

- [ ] **Step 5: Commit**

```bash
git commit -m "feat: persist controlled semantic entries"
```

---

### Task 3: Wire authoritative action events into semantic FACT

**Files:**
- Modify: `common/src/test/java/net/conczin/mca/livingworld/memory2/Memory2WorldEventIngestorTest.java`
- Modify: `common/src/main/java/net/conczin/mca/livingworld/memory2/Memory2WorldEventIngestor.java`
- Modify: `common/src/main/java/net/conczin/mca/livingworld/knowledge/WorldEventRecorder.java`

**Interfaces:**

Preserve:

```java
record(Path worldRoot, WorldEvent event, int maxEventsPerNpc, long createdAtEpochMillis)
```

Add:

```java
record(
    Path worldRoot,
    WorldEvent event,
    int maxEventsPerNpc,
    boolean semanticEnabled,
    int maxSemanticEntriesPerNpc,
    long createdAtEpochMillis
)
```

- [ ] **Step 1: Write failing integration tests**

The extended overload must persist one ACTION event in `memory2.json` and one FACT in `semantic-memory.json`, both linked by source ID. Replaying the event must keep one of each. With semantic disabled, episodic storage must remain unchanged and semantic storage empty.

Add a failure-order test where semantic persistence fails after episodic append; verify the episodic event remains readable.

- [ ] **Step 2: Verify RED**

Expected: overload is missing.

- [ ] **Step 3: Implement overload**

Flow:

```java
MemoryEvent memory = WorldEventMemoryAdapter.toMemoryEvent(...).orElse(null);
if (memory == null) return;
MemoryEventStore.forWorld(worldRoot).append(memory, maxEventsPerNpc);
ControlledSemanticMemoryIngestor.recordFactIfEnabled(
        semanticEnabled, worldRoot, memory, maxSemanticEntriesPerNpc);
```

The existing overload delegates with semantic disabled for source compatibility.

- [ ] **Step 4: Update `WorldEventRecorder`**

Pass configuration values into the extended overload. Keep its existing outer fail-soft boundary.

- [ ] **Step 5: Run tests**

Expected: action integration and all existing world-event tests pass.

- [ ] **Step 6: Commit**

```bash
git commit -m "feat: ingest authoritative actions as semantic facts"
```

---

### Task 4: Wire persisted relationship transitions into semantic FACT

**Files:**
- Modify: `common/src/test/java/net/conczin/mca/livingworld/memory2/Memory2RelationshipChangeIngestorTest.java`
- Modify: `common/src/main/java/net/conczin/mca/livingworld/memory2/Memory2RelationshipChangeIngestor.java`
- Modify: `common/src/main/java/net/conczin/mca/entity/ai/chatAI/OpenAIChatAI.java`

**Interfaces:**

Preserve existing methods. Add overloads accepting:

```java
boolean semanticEnabled,
int maxSemanticEntriesPerNpc
```

- [ ] **Step 1: Write failing relationship integration tests**

Prove a changed persisted transition produces one episodic RELATIONSHIP_CHANGE and one semantic FACT with the relationship event ID in `sourceEventIds`. Replay is idempotent. Unchanged/disabled paths write neither semantic nor episodic entries.

- [ ] **Step 2: Verify RED**

Expected: semantic overload is missing.

- [ ] **Step 3: Implement overloads**

Create the relationship `MemoryEvent` once, append episodic first, then call controlled FACT ingestion.

- [ ] **Step 4: Update snapshot relationship call site**

Pass `semanticMemoryEnabled` and `semanticMemoryMaxEntriesPerNpc` from `LivingWorldConfig`.

- [ ] **Step 5: Run tests**

Expected: all relationship, memory, and parser tests pass.

- [ ] **Step 6: Commit**

```bash
git commit -m "feat: ingest relationship transitions as semantic facts"
```

---

### Task 5: Add bounded semantic-ingestion configuration

**Files:**
- Modify: `common/src/test/java/net/conczin/mca/livingworld/Memory2ConfigTest.java`
- Modify: `common/src/main/java/net/conczin/mca/livingworld/LivingWorldConfig.java`
- Modify: `docs/livingworld/CONFIGURATION.md`

**Interfaces:**

```java
public boolean semanticMemoryEnabled = true;
public int semanticMemoryMaxEntriesPerNpc = 128;
```

- [ ] **Step 1: Write failing config tests**

Assert defaults and normalization to `[1, 512]`, plus explicit disabling in version-2 JSON.

- [ ] **Step 2: Verify RED**

Expected: fields are missing.

- [ ] **Step 3: Implement additive fields and normalization**

Do not bump config version.

- [ ] **Step 4: Document configuration**

Explain that disabling semantic ingestion does not disable episodic Memory 2.0 and that ordinary dialogue remains excluded from automatic semantic creation.

- [ ] **Step 5: Run tests**

Expected: configuration and all unit tests pass.

- [ ] **Step 6: Commit**

```bash
git commit -m "feat: configure semantic memory ingestion"
```

---

### Task 6: Document the completed semantic producer boundary

**Files:**
- Modify: `docs/livingworld/MEMORY_2.md`
- Modify: `docs/PROJECT_STATE.md`
- Modify: `docs/CHANGELOG.md`

- [ ] **Step 1: Update architecture documentation**

Record:

```text
ACTION / RELATIONSHIP_CHANGE + SYSTEM_OBSERVED
→ semantic FACT
```

State explicitly:

```text
DIALOGUE
→ episodic only
→ no automatic semantic entry
```

Document sourced BELIEF API as available but not automatically wired to dialogue.

- [ ] **Step 2: Update state and changelog**

Mark controlled semantic FACT ingestion implemented after `0.1.11`, but not live-server validated until a new checkpoint is tested.

- [ ] **Step 3: Commit**

```bash
git commit -m "docs: record controlled semantic ingestion"
```

---

### Task 7: Final verification and PR completion

**Files:**
- Review all changed files.

- [ ] **Step 1: Run focused unit tests**

```bash
./gradlew :common:test --tests '*SemanticMemoryIngestionAdapterTest' \
  --tests '*ControlledSemanticMemoryIngestorTest' \
  --tests '*Memory2WorldEventIngestorTest' \
  --tests '*Memory2RelationshipChangeIngestorTest' \
  --tests '*Memory2ConfigTest'
```

- [ ] **Step 2: Run full verification**

```bash
./gradlew test build
./gradlew :fabric:build
./gradlew :neoforge:build
```

- [ ] **Step 3: Inspect diff for authority regressions**

Confirm no automatic `DIALOGUE` semantic call, no LLM classification, no provider dependency, and no change to authoritative action/relationship commit ordering.

- [ ] **Step 4: Verify exact PR head in both GitHub workflows**

Required:

```text
VillAIgence CI → SUCCESS
Java Pull Request CI with Gradle → SUCCESS
```

- [ ] **Step 5: Merge only the verified exact head**

Use a merge commit and record the merge SHA in project continuity documentation after the feature PR.
