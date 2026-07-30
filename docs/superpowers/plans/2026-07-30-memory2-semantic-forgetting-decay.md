# Memory 2.0 Deterministic Semantic Forgetting and Decay Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace oldest-only semantic retention with deterministic pressure-based forgetting that considers age, importance, confidence, provenance, and independent source evidence.

**Architecture:** Add a pure `SemanticMemoryRetentionPolicy` that calculates durability and effective retention scores, then selects a bounded retained set. Integrate it after semantic consolidation inside `SemanticMemoryStore.append`, preserving the version-1 JSON schema and avoiding file rewrites when a weak candidate is rejected.

**Tech Stack:** Java 21, JUnit 5, Gradle, Fabric 1.21.1, NeoForge compile compatibility, existing Gson persistence.

## Global Constraints

- Minecraft version remains exactly 1.21.1.
- Java version remains exactly 21.
- Fabric remains the primary distributable package.
- NeoForge compile compatibility remains required.
- `semantic-memory.json` format remains version 1 with no new fields.
- Existing `maxEntriesPerNpc` remains the only capacity control.
- No LLM, provider call, embedding, vector database, timer, background task, wall-clock decay, or confidence mutation.
- Forgetting occurs only when the per-NPC list exceeds capacity.
- Consolidation must run before retention selection.
- FACT/BELIEF and provenance boundaries must remain unchanged.

---

### Task 1: Define the retention-policy contract with failing tests

**Files:**
- Create: `common/src/test/java/net/conczin/mca/livingworld/memory2/SemanticMemoryRetentionPolicyTest.java`

**Interfaces:**
- Consumes: existing `SemanticMemoryEntry` and `MemoryEvent.Provenance`.
- Produces expected API:
  - `SemanticMemoryRetentionPolicy.durabilityScore(SemanticMemoryEntry): int`
  - `SemanticMemoryRetentionPolicy.effectiveRetentionScore(SemanticMemoryEntry, long): long`
  - `SemanticMemoryRetentionPolicy.selectRetained(List<SemanticMemoryEntry>, int, long): List<SemanticMemoryEntry>`

- [ ] **Step 1: Write a failing durability test**

Create entries with equal age but independently varied importance, confidence, provenance, and source count. Assert the exact formula:

```java
assertEquals(400, importanceOnlyDurability);
assertEquals(250, confidenceOnlyDurability);
assertEquals(200, systemObservedProvenanceContribution);
assertEquals(100, playerToldProvenanceContribution);
assertEquals(75, npcToldProvenanceContribution);
assertEquals(25, inferredProvenanceContribution);
assertEquals(150, sixOrMoreSourcesContribution);
```

- [ ] **Step 2: Write failing decay tests**

```java
assertEquals(baseScore, effectiveRetentionScore(entry, entry.gameTime()));
assertEquals(baseScore - 36_000L, effectiveRetentionScore(entry, entry.gameTime() + 36_000L));
assertEquals(baseScore, effectiveRetentionScore(entry, entry.gameTime() - 1L));
```

- [ ] **Step 3: Write failing selection tests**

Cover:

```text
older important FACT beats newer weak FACT
confidence independently changes the winner
SYSTEM_OBSERVED beats equal PLAYER_TOLD / NPC_TOLD / INFERRED durability inputs
corroborated entry beats single-source equivalent
under-capacity keeps all entries
input-order permutations produce the same retained UUIDs
exact-score ties choose lexicographically smaller UUID
```

- [ ] **Step 4: Publish RED commit**

Commit only the test file:

```text
test: define semantic forgetting policy
```

- [ ] **Step 5: Verify RED in CI**

Run the standard pull-request workflow and confirm `:common:compileTestJava` fails because `SemanticMemoryRetentionPolicy` does not exist. Record the exact RED head and run ID in the PR body.

---

### Task 2: Implement the pure retention policy

**Files:**
- Create: `common/src/main/java/net/conczin/mca/livingworld/memory2/SemanticMemoryRetentionPolicy.java`
- Test: `common/src/test/java/net/conczin/mca/livingworld/memory2/SemanticMemoryRetentionPolicyTest.java`

**Interfaces:**
- Produces:

```java
public final class SemanticMemoryRetentionPolicy {
    static final long DECAY_STEP_TICKS = 36_000L;

    static int durabilityScore(SemanticMemoryEntry entry);

    static long effectiveRetentionScore(
            SemanticMemoryEntry entry,
            long nowGameTime
    );

    public static List<SemanticMemoryEntry> selectRetained(
            List<SemanticMemoryEntry> entries,
            int maxEntries,
            long nowGameTime
    );
}
```

- [ ] **Step 1: Implement exact durability calculation**

```java
int provenance = switch (entry.provenance()) {
    case SYSTEM_OBSERVED -> 200;
    case PLAYER_TOLD -> 100;
    case NPC_TOLD -> 75;
    case INFERRED -> 25;
};
int sources = Math.min(entry.sourceEventIds().size(), 6) * 25;
return entry.importance() * 4
        + entry.confidence() * 5 / 2
        + provenance
        + sources;
```

- [ ] **Step 2: Implement effective score**

```java
long ageTicks = Math.max(0L, nowGameTime - entry.gameTime());
return (long) durabilityScore(entry) * DECAY_STEP_TICKS - ageTicks;
```

Use saturated subtraction if needed to avoid overflow, although current normalized game times and the maximum durability keep ordinary values far inside `long` range.

- [ ] **Step 3: Implement deterministic selection comparator**

Retain highest values by:

```text
effective score descending
importance descending
confidence descending
source count descending
gameTime descending
createdAtEpochMillis descending
UUID ascending
```

Normalize input by removing null entries and duplicate entry UUIDs. Clamp `maxEntries` to at least one. If size is under capacity, retain all valid unique entries.

- [ ] **Step 4: Return persistence-stable order**

After selecting the winners, sort them by the existing store order:

```text
gameTime ascending
createdAtEpochMillis ascending
UUID ascending
```

Return an immutable list.

- [ ] **Step 5: Run policy tests**

Expected: all `SemanticMemoryRetentionPolicyTest` tests pass.

- [ ] **Step 6: Commit GREEN policy**

```text
feat: add deterministic semantic forgetting policy
```

---

### Task 3: Integrate retention after consolidation with store-level RED→GREEN tests

**Files:**
- Modify: `common/src/test/java/net/conczin/mca/livingworld/memory2/SemanticMemoryStoreTest.java`
- Modify: `common/src/main/java/net/conczin/mca/livingworld/memory2/SemanticMemoryStore.java`

**Interfaces:**
- Consumes: `SemanticMemoryRetentionPolicy.selectRetained(...)` from Task 2.
- Preserves: `SemanticMemoryStore.append(SemanticMemoryEntry, int)` public signature.

- [ ] **Step 1: Replace oldest-only test expectation**

Replace `boundsRetentionPerNpcKeepingNewestEntries` with a policy test where an older important FACT survives and a newer weak BELIEF or FACT is forgotten.

- [ ] **Step 2: Add rejected-append byte-stability test**

```java
store.append(strongA, 2);
store.append(strongB, 2);
byte[] before = Files.readAllBytes(file);
store.append(weakCandidate, 2);
byte[] after = Files.readAllBytes(file);
assertArrayEquals(before, after);
```

- [ ] **Step 3: Add consolidation-before-retention test**

Create two independently sourced entries with the same consolidation key plus a competing entry at capacity two. Assert:

```text
one consolidated entry remains
both source IDs remain exactly once
consolidated evidence contribution affects retention
```

- [ ] **Step 4: Add NPC-isolation test under pressure**

Fill NPC A to capacity and append entries for NPC B. Assert each NPC retains only its own independently selected list.

- [ ] **Step 5: Publish store RED commit**

Commit the tests before changing `SemanticMemoryStore`:

```text
test: define semantic store forgetting behavior
```

Verify the store tests fail because the store still trims oldest-first and rewrites rejected candidates.

- [ ] **Step 6: Integrate retention selection**

In `append`:

```java
List<SemanticMemoryEntry> before = List.copyOf(entries);
entries.add(entry);
List<SemanticMemoryEntry> consolidated = SemanticMemoryConsolidator.consolidateAll(entries);
long nowGameTime = consolidated.stream()
        .mapToLong(SemanticMemoryEntry::gameTime)
        .max()
        .orElse(entry.gameTime());
List<SemanticMemoryEntry> retained = SemanticMemoryRetentionPolicy.selectRetained(
        consolidated,
        safeMax,
        nowGameTime
);
entries.clear();
entries.addAll(retained);
if (!before.equals(retained)) save();
```

Keep the exact UUID replay early return unchanged.

- [ ] **Step 7: Run store and full common tests**

Expected:

```text
SemanticMemoryStoreTest PASS
SemanticMemoryRetentionPolicyTest PASS
all existing common tests PASS
```

- [ ] **Step 8: Commit store GREEN**

```text
feat: apply semantic forgetting under retention pressure
```

---

### Task 4: Document behavior and verify the exact feature head

**Files:**
- Create: `docs/livingworld/SEMANTIC_FORGETTING_DECAY.md`
- Update: PR body only; canonical `PROJECT_STATE.md` and `CHANGELOG.md` are updated after merge in a separate continuity PR.

**Interfaces:**
- Documents the formula, safety boundary, persistence behavior, validation boundary, and live-test scenario.

- [ ] **Step 1: Write operator/developer documentation**

Document:

```text
pressure-based forgetting only
formula and provenance weights
36,000-tick decay step
consolidation before forgetting
no confidence mutation
no TTL or background cleanup
no JSON/config changes
rejected append does not rewrite file
```

- [ ] **Step 2: Review final diff**

Confirm the feature PR changes only:

```text
SemanticMemoryRetentionPolicy production class
retention policy tests
SemanticMemoryStore integration
store integration tests
design, plan, and feature documentation
```

- [ ] **Step 3: Run exact-head CI**

Require both:

```text
VillAIgence CI — SUCCESS
Java Pull Request CI with Gradle — SUCCESS
```

- [ ] **Step 4: Merge with exact-head protection**

Merge only if the PR head SHA matches the SHA that passed both workflows.

---

### Task 5: Synchronize canonical project state after merge

**Files:**
- Modify: `docs/PROJECT_STATE.md`
- Modify: `docs/CHANGELOG.md`

**Interfaces:**
- Records the feature merge, exact verified head, CI run IDs, live-validation boundary, and next roadmap gate.

- [ ] **Step 1: Create a docs-only branch from the feature merge**

- [ ] **Step 2: Update canonical state**

Record forgetting/decay as merged and CI-validated but not live-tested. Keep `0.1.13+1.21.1` as the latest live checkpoint.

- [ ] **Step 3: Define live validation**

The server test must create retention pressure and verify:

```text
older strong knowledge survives
new weak knowledge is rejected or evicted as predicted
source corroboration affects retention
NPC isolation remains intact
semantic-memory.json is byte-stable after rejected append and restart
Chat/STT/TTS/Voice Chat/Opus/monitor/ports remain healthy
```

- [ ] **Step 4: Run both CI gates and merge exact head**

- [ ] **Step 5: Preserve roadmap order**

After live validation of forgetting/decay, the next major design target is legacy `memory.json` migration, unless live evidence reveals a concrete retention defect.
