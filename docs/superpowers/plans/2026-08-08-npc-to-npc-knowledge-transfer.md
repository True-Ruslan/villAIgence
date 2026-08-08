# NPC-to-NPC Knowledge Transfer Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a deterministic server-owned NPC-to-NPC knowledge-transfer primitive that can transfer one exact persisted Semantic Memory entry from speaker NPC A to listener NPC B as exact persisted `NPC_TOLD` DIALOGUE evidence followed by a listener-owned Semantic `BELIEF`, without weakening truth authority, player isolation, bounded retention, replay idempotency or release evidence boundaries.

**Architecture:** Resolve one exact speaker-owned `SemanticMemoryEntry`, reread it authoritatively before writing, construct canonical listener-owned `NPC_TOLD` DIALOGUE evidence with deterministic identity, persist and reread that exact evidence, then use the existing controlled BELIEF admission/ingestion path and verify the retained post-consolidation listener BELIEF by semantic compatibility plus exact evidence UUID containment. Keep the operation bounded, fail-closed and provider-independent; reuse existing Memory 2.0 retention, consolidation, player eligibility and long-horizon retrieval rather than introducing a new persistence model.

**Tech Stack:** Java 21, JUnit 5, Gradle multi-project build, Fabric 1.21.1 primary distribution, NeoForge compile compatibility, Gson-backed world-local `memory2.json` / `semantic-memory.json`, existing Memory 2.0 retention/consolidation/retrieval policies, GitHub Actions production/server acceptance workflows.

## Global Constraints

- Base branch is `1.21.1`; approved design base is `a20d6d0ebf5688e790fedeb3563f24f69e7e9c95`.
- Approved design is `docs/superpowers/specs/2026-08-08-npc-to-npc-knowledge-transfer-design.md` at approved spec head `bbb33022a86cc58ac429a1de991c5bcfc26fb972`.
- Runtime implementation branch is `feat/npc-to-npc-knowledge-transfer`, created only when execution begins from the final plan/spec commit.
- Strict staged TDD: no production behavior is added before the corresponding tests-only RED commit has been pushed and the intended failure has been observed in CI.
- Speaker knowledge must already exist as an exact persisted `SemanticMemoryEntry`; the transfer caller never supplies arbitrary claim text, provenance, semantic kind, subject scope, source-event IDs, importance or confidence.
- Speaker `FACT` and speaker `BELIEF` are both transferable, but listener output is always `BELIEF / NPC_TOLD`; transfer never copies FACT authority or the speaker's original provenance class.
- Transfer evidence uses existing `MemoryEvent.Type.DIALOGUE`; no new event type or persistence schema is introduced.
- Canonical transfer evidence is listener-owned with exact participants `[listenerNpcId, speakerNpcId]`, `NPC_TOLD`, authoritative Minecraft `gameTime`, `createdAtEpochMillis=0`, `importance=50`, `emotionalWeight=0`, `confidence=50`, empty relationship reasons, no structured dialogue/relationship payload, and summary `NPC told: <normalizedStatement>`.
- Evidence UUID namespace is exactly `npc-knowledge-transfer-v1`, derived from listener UUID, speaker UUID, speaker Semantic entry UUID and authoritative game time.
- Transferred statement uses the same current Semantic BELIEF normalization/bound as `SemanticMemoryIngestionAdapter`: at most `240` Unicode code points after whitespace/control normalization.
- Listener semantic `relatedEntities` preserves the speaker source's semantic subject scope as the same canonical UUID set; speaker identity is not automatically inserted into the subject scope.
- The direct listener BELIEF source is the exact transfer-evidence UUID only. Speaker upstream `sourceEventIds` are not copied into listener Semantic Memory in this slice.
- Transfer evidence must persist and be reread before semantic admission. Missing/evicted/invalid evidence is never reconstructed from summary prose.
- Exact retry at the same game time is idempotent. A later transfer at another authoritative game time creates a new evidence source and may consolidate into the same logical listener BELIEF through the existing Semantic consolidation/source-union behavior.
- Existing `MemoryEventRetentionPolicy`, `SemanticMemoryRetentionPolicy`, `LongHorizonCandidateSelector`, final rankers, candidate limit `32`, normal `24 recent + 8 durable` split and final result limit `6` remain authoritative and unchanged unless a new observed RED proves a concrete defect.
- Player-scoped eligibility remains a hard boundary before bounded candidate allocation. Foreign-player memory consumes zero prompt slots; NPC-global and current-player shared scope remain eligible.
- Current observed server facts remain authoritative and structurally outrank transferred BELIEF. Repetition, high confidence, retention or long-horizon recall never promote the BELIEF to FACT.
- No new provider/LLM call, autonomous NPC scheduler, visible NPC-to-NPC chat, TTS/STT, rumor propagation, trust weighting, public config, persistence file/version, legacy migration/backfill or unrelated refactor is part of this slice.
- Root `CHANGELOG.md` must be updated in the runtime PR only after behavior is GREEN.
- Official installed-release evidence remains `0.2.0+1.21.1` until a later immutable release candidate is explicitly built and installed-tested. This work is unreleased PR/candidate evidence only.

---

## File Structure

### New production files — create only after their RED gates

- `common/src/main/java/net/conczin/mca/livingworld/memory2/NpcToldDialogueAdapter.java`
  - Pure normalization, canonical transfer-event construction and deterministic evidence UUID generation.
- `common/src/main/java/net/conczin/mca/livingworld/memory2/NpcKnowledgeTransferPolicy.java`
  - Pure fail-closed validation for request/source snapshots, exact persisted evidence and retained Semantic BELIEF compatibility.
- `common/src/main/java/net/conczin/mca/livingworld/memory2/NpcKnowledgeTransferLifecycle.java`
  - Store orchestration only: exact source lookup/reread, evidence persist/reread, controlled BELIEF admission, Semantic persist/reread and explicit status mapping.
- `common/src/main/java/net/conczin/mca/livingworld/memory2/NpcKnowledgeTransferResult.java`
  - Immutable explicit operation result with status and exact retained evidence/semantic identifiers when available.

### Existing production files expected to change minimally

- `common/src/main/java/net/conczin/mca/livingworld/memory2/MemoryEventStore.java`
  - Add exact owner+UUID lookup; no persistence-format or retention change.
- `common/src/main/java/net/conczin/mca/livingworld/memory2/SemanticMemoryStore.java`
  - Add exact owner+UUID lookup and bounded owner-local predicate lookup for post-consolidation verification; no persistence-format or retention change.
- `common/src/main/java/net/conczin/mca/livingworld/memory2/SemanticMemoryIngestionAdapter.java`
  - Expose the existing statement normalization/bound as a package-visible pure helper so transfer evidence and BELIEF ingestion use one normalization implementation; existing FACT/BELIEF semantics remain unchanged.
- `CHANGELOG.md`
  - Record the verified server-owned NPC→NPC transfer behavior and explicit non-goals after runtime GREEN.

### New focused tests

- `common/src/test/java/net/conczin/mca/livingworld/memory2/NpcToldDialogueAdapterTest.java`
- `common/src/test/java/net/conczin/mca/livingworld/memory2/NpcKnowledgeTransferPolicyTest.java`
- `common/src/test/java/net/conczin/mca/livingworld/memory2/NpcKnowledgeTransferLifecycleTest.java`
- `common/src/test/java/net/conczin/mca/livingworld/memory2/NpcKnowledgeTransferPressureTest.java`
- `common/src/test/java/net/conczin/mca/livingworld/memory2/NpcKnowledgeTransferPersistenceTest.java`
- `common/src/test/java/net/conczin/mca/livingworld/memory2/NpcKnowledgeTransferSimulationTest.java`

### Existing tests to extend

- `common/src/test/java/net/conczin/mca/livingworld/memory2/MemoryEventStoreTest.java`
- `common/src/test/java/net/conczin/mca/livingworld/memory2/SemanticMemoryStoreTest.java`
- `common/src/test/java/net/conczin/mca/livingworld/memory2/SemanticMemoryIngestionAdapterTest.java`
- `common/src/test/java/net/conczin/mca/livingworld/memory2/Memory2DialogueHistoryTest.java`
- `common/src/test/java/net/conczin/mca/livingworld/memory2/SemanticMemoryRetrieverTest.java`
- existing prompt/snapshot authority tests selected by repository search during execution; do not create a duplicate prompt-composition abstraction if an existing one already covers current-observation precedence.

### Evidence / delivery docs

- Create during implementation: `docs/superpowers/evidence/2026-08-08-npc-to-npc-knowledge-transfer-tdd.md`
  - Exact tests-only RED SHAs, observed workflow/run IDs, GREEN SHAs, preservation evidence, exact-head gates, review result and release-boundary statement.
- Reconcile after product merge: `docs/PROJECT_STATE.md`, `docs/ROADMAP.md`
  - Record exact product merge SHA/evidence and advance the next slice to provenance-aware rumors only after this slice actually meets exit criteria.

---

### Task 1: Exact store authority lookup

**Files:**
- Modify test first: `common/src/test/java/net/conczin/mca/livingworld/memory2/MemoryEventStoreTest.java`
- Modify test first: `common/src/test/java/net/conczin/mca/livingworld/memory2/SemanticMemoryStoreTest.java`
- Modify after observed RED: `common/src/main/java/net/conczin/mca/livingworld/memory2/MemoryEventStore.java`
- Modify after observed RED: `common/src/main/java/net/conczin/mca/livingworld/memory2/SemanticMemoryStore.java`

**Interfaces:**
- Produces: `synchronized Optional<MemoryEvent> findById(UUID npcId, UUID eventId)`.
- Produces: `synchronized Optional<SemanticMemoryEntry> findById(UUID npcId, UUID entryId)`.
- Produces: `synchronized Optional<SemanticMemoryEntry> findMatching(UUID npcId, Predicate<SemanticMemoryEntry> predicate)`.
- All three are exact bounded-store reads only: no mutation, no prompt ranking, no newest-window approximation.

- [ ] **Step 1: Add exact MemoryEvent lookup tests only**

Add imports for `Optional` only if needed and tests equivalent to:

```java
@Test
void exactLookupFindsOwnerEventRegardlessOfRecentPositionAndRejectsWrongOwner() {
    UUID npc = UUID.fromString("00000000-0000-0000-0000-000000010001");
    UUID otherNpc = UUID.fromString("00000000-0000-0000-0000-000000010002");
    MemoryEventStore store = new MemoryEventStore(tempDir.resolve("memory2.json"));
    MemoryEvent old = event("authority-old", npc, 10L);
    store.append(old, 64);
    for (int i = 0; i < 40; i++) store.append(event("authority-new-" + i, npc, 100L + i), 64);

    assertEquals(Optional.of(old), store.findById(npc, old.id()));
    assertEquals(Optional.empty(), store.findById(otherNpc, old.id()));
    assertEquals(Optional.empty(), store.findById(npc, UUID.randomUUID()));
    assertEquals(Optional.empty(), store.findById(null, old.id()));
    assertEquals(Optional.empty(), store.findById(npc, null));
}
```

Also snapshot `getRecent(npc, 64)` before and after exact lookup and assert equality to prove lookup does not mutate ordering/state.

- [ ] **Step 2: Add exact Semantic lookup tests only**

Use one old source outside an arbitrary recent sub-window and add tests equivalent to:

```java
@Test
void exactSemanticLookupAndOwnerLocalPredicateLookupDoNotDependOnRecency() {
    SemanticMemoryStore store = new SemanticMemoryStore(tempDir.resolve("semantic-memory.json"));
    UUID npc = UUID.fromString("00000000-0000-0000-0000-000000010101");
    UUID otherNpc = UUID.fromString("00000000-0000-0000-0000-000000010102");
    SemanticMemoryEntry old = sourcedEntry(
            UUID.fromString("00000000-0000-0000-0000-000000010103"),
            npc, 10L, "old exact semantic", List.of(),
            UUID.fromString("00000000-0000-0000-0000-000000010104")
    );
    store.append(old, 64);
    for (int i = 0; i < 40; i++) {
        store.append(sourcedEntry(
                new UUID(0L, 20_000L + i), npc, 100L + i, "new semantic " + i,
                List.of(), new UUID(0L, 30_000L + i)
        ), 64);
    }

    assertEquals(Optional.of(old), store.findById(npc, old.id()));
    assertEquals(Optional.empty(), store.findById(otherNpc, old.id()));
    assertEquals(Optional.of(old), store.findMatching(npc, value -> value.id().equals(old.id())));
    assertEquals(Optional.empty(), store.findMatching(otherNpc, value -> true));
    assertEquals(Optional.empty(), store.findMatching(npc, null));
}
```

- [ ] **Step 3: Commit the tests-only RED**

Commit only the two test files:

```bash
git add common/src/test/java/net/conczin/mca/livingworld/memory2/MemoryEventStoreTest.java \
        common/src/test/java/net/conczin/mca/livingworld/memory2/SemanticMemoryStoreTest.java
git commit -m "test: require exact Memory 2.0 authority lookup"
```

Push the tests-only head and let the PR-triggered CI run.

- [ ] **Step 4: Verify intended RED**

Focused local command before push is optional sanity only:

```bash
./gradlew :common:test --tests 'net.conczin.mca.livingworld.memory2.MemoryEventStoreTest' \
                          --tests 'net.conczin.mca.livingworld.memory2.SemanticMemoryStoreTest'
```

Expected CI RED: test compilation fails specifically because `findById(...)` / `findMatching(...)` do not exist. Production sources for those APIs must still be unchanged at this RED head.

Record exact tests-only SHA, workflow/run ID and compile error in the TDD evidence ledger/PR notes before production changes.

- [ ] **Step 5: Implement minimal exact read APIs**

In `MemoryEventStore` add:

```java
public synchronized Optional<MemoryEvent> findById(UUID npcId, UUID eventId) {
    if (npcId == null || eventId == null) return Optional.empty();
    List<MemoryEvent> events = data.eventsByNpc.get(npcId.toString());
    if (events == null || events.isEmpty()) return Optional.empty();
    return events.stream().filter(event -> eventId.equals(event.id())).findFirst();
}
```

In `SemanticMemoryStore` add:

```java
public synchronized Optional<SemanticMemoryEntry> findById(UUID npcId, UUID entryId) {
    if (npcId == null || entryId == null) return Optional.empty();
    List<SemanticMemoryEntry> entries = data.entriesByNpc.get(npcId.toString());
    if (entries == null || entries.isEmpty()) return Optional.empty();
    return entries.stream().filter(entry -> entryId.equals(entry.id())).findFirst();
}

synchronized Optional<SemanticMemoryEntry> findMatching(
        UUID npcId,
        Predicate<SemanticMemoryEntry> predicate
) {
    if (npcId == null || predicate == null) return Optional.empty();
    List<SemanticMemoryEntry> entries = data.entriesByNpc.get(npcId.toString());
    if (entries == null || entries.isEmpty()) return Optional.empty();
    return entries.stream().filter(predicate).findFirst();
}
```

Use `java.util.Optional`; do not change load/save/retention logic.

- [ ] **Step 6: Verify GREEN and commit**

Run:

```bash
./gradlew :common:test --tests 'net.conczin.mca.livingworld.memory2.MemoryEventStoreTest' \
                          --tests 'net.conczin.mca.livingworld.memory2.SemanticMemoryStoreTest'
```

Expected: PASS, including all existing pressure/replay/recovery tests.

Then run `./gradlew :common:test` and commit production + tests only after GREEN:

```bash
git add common/src/main/java/net/conczin/mca/livingworld/memory2/MemoryEventStore.java \
        common/src/main/java/net/conczin/mca/livingworld/memory2/SemanticMemoryStore.java \
        common/src/test/java/net/conczin/mca/livingworld/memory2/MemoryEventStoreTest.java \
        common/src/test/java/net/conczin/mca/livingworld/memory2/SemanticMemoryStoreTest.java
git commit -m "feat: add exact Memory 2.0 authority lookup"
```

---

### Task 2: Canonical NPC_TOLD evidence adapter and validation policy

**Files:**
- Create test first: `common/src/test/java/net/conczin/mca/livingworld/memory2/NpcToldDialogueAdapterTest.java`
- Create test first: `common/src/test/java/net/conczin/mca/livingworld/memory2/NpcKnowledgeTransferPolicyTest.java`
- Modify test first: `common/src/test/java/net/conczin/mca/livingworld/memory2/SemanticMemoryIngestionAdapterTest.java`
- Create after observed RED: `common/src/main/java/net/conczin/mca/livingworld/memory2/NpcToldDialogueAdapter.java`
- Create after observed RED: `common/src/main/java/net/conczin/mca/livingworld/memory2/NpcKnowledgeTransferPolicy.java`
- Modify after observed RED: `common/src/main/java/net/conczin/mca/livingworld/memory2/SemanticMemoryIngestionAdapter.java`

**Interfaces:**
- Produces: package-visible `static String normalizeAndLimitStatement(String value)` in `SemanticMemoryIngestionAdapter` using the exact existing normalization logic.
- Produces: `static Optional<MemoryEvent> create(UUID speakerNpcId, UUID listenerNpcId, UUID speakerSemanticEntryId, long authoritativeGameTime, String statement)`.
- Produces: `static UUID deterministicEvidenceId(UUID speakerNpcId, UUID listenerNpcId, UUID speakerSemanticEntryId, long authoritativeGameTime)` for focused tests/policy reuse.
- Produces policy methods:
  - `static boolean validRequest(UUID speakerNpcId, UUID listenerNpcId, SemanticMemoryEntry source)`.
  - `static boolean sameSourceSnapshot(SemanticMemoryEntry selected, SemanticMemoryEntry reread)`.
  - `static boolean validEvidence(MemoryEvent event, UUID speakerNpcId, UUID listenerNpcId, UUID sourceEntryId, long authoritativeGameTime, String normalizedStatement)`.
  - `static boolean compatibleRetainedBelief(SemanticMemoryEntry expectedCandidate, SemanticMemoryEntry retained, UUID evidenceEventId)`.

- [ ] **Step 1: Lock the existing Semantic normalization contract in tests**

Add a focused test proving the new package-visible helper is exactly the one used by BELIEF ingestion:

```java
@Test
void exposedStatementNormalizationMatchesBeliefIngestionBoundary() {
    String raw = "  Village\n\t" + "x".repeat(300);
    String normalized = SemanticMemoryIngestionAdapter.normalizeAndLimitStatement(raw);
    SemanticBeliefSource source = new SemanticBeliefSource(
            UUID.randomUUID(), raw, List.of(), MemoryEvent.Provenance.NPC_TOLD,
            10L, 0L, 50, 50, List.of(UUID.randomUUID())
    );
    SemanticMemoryEntry entry = SemanticMemoryIngestionAdapter.toBelief(source);

    assertEquals(normalized, entry.statement());
    assertEquals(240, normalized.codePointCount(0, normalized.length()));
    assertFalse(normalized.contains("\n"));
    assertFalse(normalized.contains("\t"));
}
```

This is behavior-preserving extraction of an existing private helper, not a normalization redesign.

- [ ] **Step 2: Write adapter contract tests only**

Cover one deterministic fixture with fixed UUIDs. Assert exactly:

```java
MemoryEvent event = NpcToldDialogueAdapter.create(
        speaker, listener, sourceEntryId, 12_345L, "  Bridge\n  destroyed  "
).orElseThrow();

assertEquals(listener, event.ownerNpcId());
assertEquals(MemoryEvent.Type.DIALOGUE, event.type());
assertEquals(List.of(listener, speaker), event.participants());
assertEquals(MemoryEvent.Provenance.NPC_TOLD, event.provenance());
assertEquals(12_345L, event.gameTime());
assertEquals(0L, event.createdAtEpochMillis());
assertEquals(50, event.importance());
assertEquals(0, event.emotionalWeight());
assertEquals(50, event.confidence());
assertEquals(List.of(), event.relationshipReasons());
assertEquals(null, event.dialogue());
assertEquals(null, event.relationshipTransition());
assertEquals(null, event.relationshipCause());
assertEquals("NPC told: Bridge destroyed", event.summary());
```

Also assert:

- same inputs → same UUID and equal event;
- changed listener/speaker/sourceEntryId/gameTime each changes UUID;
- `speaker == listener`, null IDs and blank normalized statement → `Optional.empty()`;
- >240-code-point statement is bounded before summary; summary is at most 250 code points;
- no wall-clock value is sampled or represented.

- [ ] **Step 3: Write pure policy tests only**

Create helpers for a speaker Semantic source and canonical event. Tests must lock:

```text
valid request                    -> true
same speaker/listener            -> false
source owner != speaker          -> false
null source/identity             -> false
same immutable source snapshot   -> true even if relatedEntities/sourceEventIds insertion order differs
changed source id/owner/kind/provenance/statement/scope/source-event-set -> false
```

For `validEvidence`, mutate one field at a time and assert false for:

```text
owner
participant order
participant count
provenance
type
gameTime
createdAtEpochMillis
importance
emotionalWeight
confidence
relationshipReasons
dialogue
relationshipTransition
relationshipCause
summary
evidence UUID
```

For retained Semantic compatibility, create expected/retained `BELIEF / NPC_TOLD` entries and assert:

- compatible canonical statement/scope plus exact evidence source containment → true;
- missing evidence UUID → false;
- wrong owner/kind/provenance/statement/scope → false;
- relatedEntities insertion order alone does not make it false.

- [ ] **Step 4: Commit and observe RED**

Commit only the three test files. Expected CI RED is test compilation failure for missing `NpcToldDialogueAdapter`, `NpcKnowledgeTransferPolicy` and package-visible normalization helper.

Record exact RED SHA/run before production code.

- [ ] **Step 5: Expose the existing normalization helper without behavior change**

Rename/re-scope the existing private helper:

```java
static String normalizeAndLimitStatement(String value) {
    String normalized = normalizeWhitespace(value);
    if (normalized.codePointCount(0, normalized.length()) <= MAX_STATEMENT_CODE_POINTS) return normalized;
    int end = normalized.offsetByCodePoints(0, MAX_STATEMENT_CODE_POINTS);
    return normalized.substring(0, end);
}
```

Make existing `toFact(...)` and `toBelief(...)` call this exact helper.

- [ ] **Step 6: Implement the minimal pure adapter**

Use namespace:

```java
private static final String ID_NAMESPACE = "npc-knowledge-transfer-v1";
private static final String SUMMARY_PREFIX = "NPC told: ";
```

Canonical UUID input must be exactly:

```java
String canonical = ID_NAMESPACE
        + '\n' + listenerNpcId
        + '\n' + speakerNpcId
        + '\n' + speakerSemanticEntryId
        + '\n' + Math.max(0L, authoritativeGameTime);
UUID id = UUID.nameUUIDFromBytes(canonical.getBytes(StandardCharsets.UTF_8));
```

Construct the exact event shape specified in Global Constraints. Do not accept relatedEntities, provenance, importance/confidence or prebuilt event identity as arguments.

- [ ] **Step 7: Implement the minimal pure policy**

Use sorted unique UUID copies for canonical set comparison. `sameSourceSnapshot` must compare:

```text
entry UUID
owner
kind
provenance
normalized/bounded statement
canonical relatedEntities UUID set
canonical sourceEventIds UUID set
```

Do not include `gameTime`, `createdAt`, importance or confidence in the source identity-critical reread comparison unless a later observed RED proves they must be authority-bound; the approved spec deliberately names the identity-critical fields above.

`validEvidence` must recompute the canonical expected event through `NpcToldDialogueAdapter.create(...)` and compare every canonical persisted field, not parse the summary.

`compatibleRetainedBelief` should rely on the existing package-visible `SemanticMemoryConsolidator.compatible(expectedCandidate, retained)` plus exact `sourceEventIds().contains(evidenceEventId)`.

- [ ] **Step 8: Verify GREEN and commit**

Run:

```bash
./gradlew :common:test --tests 'net.conczin.mca.livingworld.memory2.SemanticMemoryIngestionAdapterTest' \
                          --tests 'net.conczin.mca.livingworld.memory2.NpcToldDialogueAdapterTest' \
                          --tests 'net.conczin.mca.livingworld.memory2.NpcKnowledgeTransferPolicyTest'
./gradlew :common:test
```

Commit pure production + tests after GREEN:

```bash
git commit -am "feat: define canonical NPC knowledge transfer evidence"
```

Include new untracked Java files in `git add` explicitly before committing.

---

### Task 3: Source-backed transfer lifecycle — primary behavioral RED→GREEN

**Files:**
- Create test first: `common/src/test/java/net/conczin/mca/livingworld/memory2/NpcKnowledgeTransferLifecycleTest.java`
- Create after observed RED: `common/src/main/java/net/conczin/mca/livingworld/memory2/NpcKnowledgeTransferResult.java`
- Create after observed RED: `common/src/main/java/net/conczin/mca/livingworld/memory2/NpcKnowledgeTransferLifecycle.java`

**Interfaces:**
- Produces:

```java
public record NpcKnowledgeTransferResult(
        Status status,
        UUID evidenceEventId,
        UUID semanticEntryId
) {
    public enum Status {
        ADMITTED,
        REJECTED,
        SOURCE_NOT_RETAINED,
        BELIEF_NOT_RETAINED
    }
}
```

- Produces:

```java
public static NpcKnowledgeTransferResult transfer(
        Path worldRoot,
        UUID speakerNpcId,
        UUID listenerNpcId,
        UUID speakerSemanticEntryId,
        long authoritativeGameTime,
        int memory2CapacityPerNpc,
        int semanticCapacityPerNpc
)
```

- Consumes Task 1 exact store lookups, Task 2 adapter/policy, `SemanticBeliefAdmissionPolicy.admit(...)`, `SemanticMemoryIngestionAdapter.toBelief(...)`, `ControlledSemanticMemoryIngestor.recordBelief(...)`.

- [ ] **Step 1: Write the primary FACT→BELIEF behavioral test only**

Persist an exact speaker FACT first:

```java
SemanticMemoryEntry source = new SemanticMemoryEntry(
        sourceId,
        speaker,
        SemanticMemoryEntry.Kind.FACT,
        "The bridge is destroyed",
        List.of(player),
        MemoryEvent.Provenance.SYSTEM_OBSERVED,
        100L,
        1_000L,
        90,
        100,
        List.of(sourceEvidenceId)
);
SemanticMemoryStore.forWorld(tempDir).append(source, 64);
```

Then call the lifecycle and assert `ADMITTED`. Reread exact evidence and listener Semantic store and assert:

```text
evidence owner = listener
evidence DIALOGUE / NPC_TOLD
participants exactly [listener, speaker]
evidence source fields are canonical
listener semantic kind = BELIEF
listener provenance = NPC_TOLD
listener statement = exact normalized source statement
listener relatedEntities = same source subject UUID set
listener sourceEventIds contains exact evidence UUID
listener importance/confidence = 50/50
no listener FACT with that statement exists
```

- [ ] **Step 2: Write the BELIEF→BELIEF provenance test only**

Persist a speaker `BELIEF / PLAYER_TOLD` with the same sort of source. Transfer it and assert listener result remains exactly `BELIEF / NPC_TOLD`; listener does not inherit `PLAYER_TOLD` and does not copy speaker upstream source-event IDs.

- [ ] **Step 3: Commit tests-only and observe behavioral RED**

Expected initial RED: compile failure because lifecycle/result do not exist. If a minimal interface stub is needed to turn this into an assertion RED, add only the smallest test-supporting result/interface shape in a separate API GREEN after recording the compile RED, then keep the behavior unimplemented and observe the assertion RED before implementing the lifecycle body.

The preferred evidence chain is:

```text
RED 3a: compile RED — lifecycle/result absent
minimal API shell, no successful behavior
RED 3b: behavioral RED — result is REJECTED/no evidence/no BELIEF
GREEN 3: real lifecycle flow
```

Do not jump from unobserved tests directly to a full implementation.

- [ ] **Step 4: Implement the minimal result type**

Keep it immutable. Normalize impossible optional identifiers by allowing `null` for unavailable IDs; do not invent placeholder UUIDs. Provide static factories only if they make status/ID combinations less error-prone.

- [ ] **Step 5: Implement lifecycle input/source fail-closed entry checks**

Start with:

```java
if (worldRoot == null || speakerNpcId == null || listenerNpcId == null
        || speakerSemanticEntryId == null || speakerNpcId.equals(listenerNpcId)) {
    return rejected();
}
```

Normalize capacities with existing store semantics: `Math.max(1, value)`.

- [ ] **Step 6: Resolve and authoritatively reread the speaker source**

Required sequence:

```java
SemanticMemoryStore semanticStore = SemanticMemoryStore.forWorld(worldRoot);
Optional<SemanticMemoryEntry> firstLookup = semanticStore.findById(speakerNpcId, speakerSemanticEntryId);
if (firstLookup.isEmpty() || !NpcKnowledgeTransferPolicy.validRequest(...)) return rejected();
SemanticMemoryEntry selected = firstLookup.get();

Optional<SemanticMemoryEntry> reread = semanticStore.findById(speakerNpcId, speakerSemanticEntryId);
if (reread.isEmpty() || !NpcKnowledgeTransferPolicy.sameSourceSnapshot(selected, reread.get())) {
    return rejected();
}
SemanticMemoryEntry source = reread.get();
```

No evidence write occurs before the second exact lookup succeeds.

- [ ] **Step 7: Construct, persist and reread exact evidence**

```java
MemoryEvent evidence = NpcToldDialogueAdapter.create(
        speakerNpcId,
        listenerNpcId,
        speakerSemanticEntryId,
        authoritativeGameTime,
        source.statement()
).orElse(null);
if (evidence == null) return rejected();

MemoryEventStore eventStore = MemoryEventStore.forWorld(worldRoot);
eventStore.append(evidence, Math.max(1, memory2CapacityPerNpc));
Optional<MemoryEvent> persisted = eventStore.findById(listenerNpcId, evidence.id());
if (persisted.isEmpty()) return sourceNotRetained(evidence.id());
```

Validate `persisted.get()` with `NpcKnowledgeTransferPolicy.validEvidence(...)`; impossible persisted mismatch returns `REJECTED` and no BELIEF.

- [ ] **Step 8: Admit and persist listener BELIEF through the existing boundary**

Use the exact reread evidence:

```java
Optional<SemanticBeliefSource> admitted = SemanticBeliefAdmissionPolicy.admit(
        persisted.get(),
        SemanticMemoryIngestionAdapter.normalizeAndLimitStatement(source.statement()),
        source.relatedEntities(),
        MemoryEvent.Provenance.NPC_TOLD,
        50,
        50
);
if (admitted.isEmpty()) return rejectedWithEvidence(evidence.id());

SemanticMemoryEntry expectedCandidate = SemanticMemoryIngestionAdapter.toBelief(admitted.get());
ControlledSemanticMemoryIngestor.recordBelief(
        worldRoot,
        admitted.get(),
        Math.max(1, semanticCapacityPerNpc)
);
```

Do not call a provider and do not create FACT.

- [ ] **Step 9: Verify retained post-consolidation Semantic result**

Do not look only for `expectedCandidate.id()`. Existing consolidation can replace the ID.

```java
Optional<SemanticMemoryEntry> retained = semanticStore.findMatching(
        listenerNpcId,
        candidate -> NpcKnowledgeTransferPolicy.compatibleRetainedBelief(
                expectedCandidate,
                candidate,
                evidence.id()
        )
);
if (retained.isEmpty()) return beliefNotRetained(evidence.id());
return admitted(evidence.id(), retained.get().id());
```

- [ ] **Step 10: Verify focused GREEN and full common GREEN**

Run:

```bash
./gradlew :common:test --tests 'net.conczin.mca.livingworld.memory2.NpcKnowledgeTransferLifecycleTest'
./gradlew :common:test
```

Commit only after both pass:

```bash
git add common/src/main/java/net/conczin/mca/livingworld/memory2/NpcKnowledgeTransferResult.java \
        common/src/main/java/net/conczin/mca/livingworld/memory2/NpcKnowledgeTransferLifecycle.java \
        common/src/test/java/net/conczin/mca/livingworld/memory2/NpcKnowledgeTransferLifecycleTest.java
git commit -m "feat: add source-backed NPC knowledge transfer lifecycle"
```

---

### Task 4: Fail-closed source identity and evidence integrity

**Files:**
- Extend test first: `NpcKnowledgeTransferPolicyTest.java`
- Extend test first: `NpcKnowledgeTransferLifecycleTest.java`
- Modify production only after an observed new failure if Task 2/3 is insufficient.

**Interfaces:**
- No new public production interface expected.
- This task proves the lifecycle cannot be driven with forged source identity or malformed transfer evidence.

- [ ] **Step 1: Add request/source rejection matrix as tests only**

Use table/parameterized tests or explicit assertions for:

```text
worldRoot null
speaker null
listener null
source id null
speaker == listener
unknown source id
source owned by another NPC
```

For every case assert:

```text
status = REJECTED
listener MemoryEventStore contains no new transfer evidence
listener SemanticMemoryStore contains no new BELIEF
unrelated speaker/third-NPC state unchanged
```

- [ ] **Step 2: Add source snapshot mismatch policy tests**

Create immutable selected/reread entries and mutate exactly one identity-critical field at a time:

```text
id
owner
kind
provenance
normalized statement
relatedEntities UUID set
sourceEventIds UUID set
```

Insertion-order differences in related/source UUID lists must remain equivalent.

- [ ] **Step 3: Add exact evidence tamper matrix tests**

Starting from `NpcToldDialogueAdapter.create(...)`, reconstruct invalid `MemoryEvent` variants mutating exactly one canonical field. Assert `validEvidence(...) == false` for every field listed in Task 2.

Include explicit tests that player-style `DialogueExchange` makes transfer evidence invalid and that summary parsing is never used to rescue invalid evidence.

- [ ] **Step 4: Commit tests-only and observe RED-or-preservation GREEN**

If all tests already pass from Tasks 2/3, record this task as **preservation GREEN / no production change** rather than manufacturing a failure.

If any new case fails, that test is a valid behavioral RED. Commit the tests-only failure, record CI, then make only the smallest policy/lifecycle correction.

- [ ] **Step 5: Run common suite and commit any minimal fix**

No fix may broaden generic `SemanticBeliefAdmissionPolicy` to trust arbitrary NPC_TOLD events. The stronger speaker/listener/source contract remains local to the NPC transfer lifecycle/policy.

---

### Task 5: Replay idempotency and Semantic consolidation

**Files:**
- Extend test first: `NpcKnowledgeTransferLifecycleTest.java`
- Modify production only after observed RED.

**Interfaces:**
- Reuses existing deterministic evidence ID and Semantic consolidation.

- [ ] **Step 1: Test exact retry idempotency**

Transfer the same source with identical speaker/listener/gameTime twice. Assert:

```text
both results = ADMITTED
same evidenceEventId
one exact evidence event in listener store
one logical listener BELIEF
listener BELIEF sourceEventIds contains that evidence UUID once
second call does not create another logical source
```

Where practical, capture `memory2.json` and `semantic-memory.json` bytes after the first stable transfer and assert exact retry does not change them.

- [ ] **Step 2: Test later corroborating transfer**

Transfer the same source again at a different authoritative game time. Assert:

```text
new evidence UUID != first evidence UUID
both evidence events may coexist if capacity retains them
listener semantic store contains one compatible logical NPC_TOLD BELIEF after consolidation
retained BELIEF sourceEventIds = sorted union of both transfer evidence UUIDs
result status = ADMITTED
result semanticEntryId is the retained consolidated entry id, not assumed candidate id
```

- [ ] **Step 3: Test canonical scope order through consolidation**

Seed equivalent listener NPC_TOLD BELIEF/source fixtures with the same subject UUID set but different insertion order, then transfer a corroborating source. Assert one consolidated entry and exact canonical scope/source union.

- [ ] **Step 4: Observe RED if present, then minimal fix**

Likely GREEN if Task 3 correctly validates post-consolidation state. If failure occurs, do not disable consolidation or add a second knowledge representation. Fix only result matching/idempotency logic.

- [ ] **Step 5: Verify full common suite**

Run `./gradlew :common:test`; commit the tests and any required minimal production correction.

---

### Task 6: Pressure outcomes and partial-retention semantics

**Files:**
- Create test first: `common/src/test/java/net/conczin/mca/livingworld/memory2/NpcKnowledgeTransferPressureTest.java`
- Modify lifecycle/policy only after observed RED if needed.

**Interfaces:**
- Proves exact distinction between `SOURCE_NOT_RETAINED` and `BELIEF_NOT_RETAINED`.

- [ ] **Step 1: Write `SOURCE_NOT_RETAINED` pressure test**

Set listener event capacity to `1`. Before transfer, persist a much stronger listener event, for example `SYSTEM_OBSERVED RELATIONSHIP_CAUSE` or another event whose existing retention score deterministically exceeds the canonical transfer DIALOGUE.

Then call transfer with `memory2CapacityPerNpc=1` and adequate Semantic capacity.

Assert:

```text
result.status = SOURCE_NOT_RETAINED
result.evidenceEventId = deterministic attempted evidence UUID
listener exact evidence lookup = empty
listener Semantic store contains no transferred BELIEF
the strong pre-existing event remains
speaker and third-NPC stores unchanged
```

Do not change `MemoryEventRetentionPolicy` merely to retain transfer events.

- [ ] **Step 2: Write `BELIEF_NOT_RETAINED` pressure test**

Use adequate MemoryEvent capacity, but listener Semantic capacity `1`. Seed listener with a deterministically much stronger Semantic FACT (`importance=100`, `confidence=100`, SYSTEM_OBSERVED, sourced) so the canonical 50/50 NPC_TOLD BELIEF loses existing retention pressure.

Assert:

```text
result.status = BELIEF_NOT_RETAINED
exact transfer evidence is still present
strong pre-existing Semantic entry remains
no compatible retained NPC_TOLD BELIEF containing evidence UUID exists
no rollback/delete of the legitimate transfer evidence occurs
```

- [ ] **Step 3: Add unrelated-state/no-rewrite assertions**

Capture unrelated NPC exact IDs/file content before each pressure case. Assert the failed transfer does not mutate unrelated NPC state. Existing store no-op rewrite tests remain GREEN.

- [ ] **Step 4: Commit tests-only and observe RED-or-preservation GREEN**

If lifecycle already reports both statuses correctly, record preservation GREEN. If it incorrectly returns REJECTED/ADMITTED or creates partial BELIEF, keep the failing tests-only commit as RED evidence and apply the smallest lifecycle status/readback fix.

- [ ] **Step 5: Verify**

Run:

```bash
./gradlew :common:test --tests 'net.conczin.mca.livingworld.memory2.NpcKnowledgeTransferPressureTest'
./gradlew :common:test
```

No retention coefficients, persistence formats or capacity defaults should change in this task.

---

### Task 7: Reload, scope/privacy isolation and player Working Memory

**Files:**
- Create test first: `common/src/test/java/net/conczin/mca/livingworld/memory2/NpcKnowledgeTransferPersistenceTest.java`
- Extend: `common/src/test/java/net/conczin/mca/livingworld/memory2/Memory2DialogueHistoryTest.java`
- Extend: `common/src/test/java/net/conczin/mca/livingworld/memory2/SemanticMemoryRetrieverTest.java`
- Modify production only after observed RED.

**Interfaces:**
- Reuses `SemanticMemoryContextProvider.load(...)`, `Memory2DialogueHistory.load(...)`, exact store reads and lifecycle.

- [ ] **Step 1: Add real persistence round-trip helper**

Do not rely on the same-path static store cache as restart evidence. Persist into `world-a`, then copy:

```text
world-a/livingworld/memory2.json
world-a/livingworld/semantic-memory.json
```

to a distinct `world-b/livingworld/` root and query `world-b` so new store instances load serialized state.

- [ ] **Step 2: Test retained transfer survives reload and replay remains idempotent**

Transfer once in `world-a`, copy to `world-b`, assert exact evidence and listener BELIEF/source UUID survive. Run the exact same transfer against `world-b`; assert no duplicate event or semantic source and status remains `ADMITTED`.

- [ ] **Step 3: Test semantic scope preservation**

Run independent transfers for:

```text
source relatedEntities = []                  -> listener global []
source relatedEntities = [playerA]           -> listener [playerA]
source relatedEntities = [playerA, entityX]  -> same canonical set
```

Assert speaker UUID is absent unless it was already genuinely part of the source subject scope.

- [ ] **Step 4: Test multi-NPC isolation**

Use pair A→B and independent D→C. Assert:

```text
A→B creates no transfer evidence/BELIEF for C
D→C does not alter B
source entries remain owned by original speakers
listener entries are owned only by their listener
```

- [ ] **Step 5: Test player prompt privacy on transferred BELIEF**

For a transferred BELIEF scoped to `playerA`:

```java
List<String> forA = SemanticMemoryContextProvider.load(world, listener, playerA, now);
List<String> forB = SemanticMemoryContextProvider.load(world, listener, playerB, now);
```

Assert statement present for A and absent for B. Add >32 foreign high-durability entries if needed to prove foreign-player data still consumes zero candidate slots before long-horizon allocation.

For NPC-global transfer, assert both players can receive it when competitively ranked. For shared scope containing playerA, assert A remains eligible.

- [ ] **Step 6: Prove raw NPC→NPC evidence never becomes player user/assistant history**

After a transfer, call:

```java
assertEquals(List.of(), Memory2DialogueHistory.load(world, listener, playerA));
```

for a world containing only the transfer DIALOGUE. This must remain true because transfer evidence has `dialogue == null`.

Then add a genuine structured PLAYER_TOLD dialogue and assert only that real player exchange is reconstructed.

- [ ] **Step 7: Observe RED-or-preservation GREEN and fix minimally**

Expected likely GREEN from the approved event shape and existing eligibility/history filters. Any privacy leak is blocking and must be fixed before proceeding; do not weaken `PlayerScopedMemoryEligibility` or add special prompt-side bypasses.

- [ ] **Step 8: Run full common tests**

Commit tests and any required minimal fix only after GREEN.

---

### Task 8: Long-horizon behavior, truth preservation and deterministic multi-NPC simulation

**Files:**
- Create: `common/src/test/java/net/conczin/mca/livingworld/memory2/NpcKnowledgeTransferSimulationTest.java`
- Extend: `SemanticMemoryRetrieverTest.java` if a focused transferred-BELIEF prompt assertion is clearer there.
- Extend the existing prompt/snapshot authority regression test found during execution only if needed to express current-observation precedence with a transferred BELIEF; do not create duplicate production prompt composition.
- No production change expected unless a real regression is exposed.

**Interfaces:**
- Consumes existing transfer lifecycle, retention policies, `LongHorizonCandidateSelector`, Semantic context provider and prompt authority layer.

- [ ] **Step 1: Prove a retained transferred BELIEF can use the existing durable recall tier**

Create one early transfer whose listener BELIEF is sufficiently competitive to remain persisted. Add more than 32 newer eligible weak Semantic entries for the same listener/player while store capacity is high enough to keep the transferred BELIEF. Round-trip to a fresh world root.

Assert `SemanticMemoryContextProvider.load(...)` can include the old `BELIEF | provenance=NPC_TOLD` line. Do not change 32/24+8/6 bounds.

- [ ] **Step 2: Prove transfer memory is not immortal**

Under sufficiently strong/large Semantic pressure, assert the older transferred BELIEF can be evicted according to the existing `SemanticMemoryRetentionPolicy`. No pinning or transfer-specific retention bonus is allowed.

Likewise, transfer evidence remains governed by ordinary `MemoryEventRetentionPolicy` and may eventually be evicted.

- [ ] **Step 3: Preserve truth-authority labels and conflict framing**

Add a focused formatter/context regression asserting transferred memory renders as:

```text
BELIEF | provenance=NPC_TOLD
```

never FACT. Keep the existing formatter warning that current observed factual context wins on conflict.

Locate the existing snapshot/prompt composition regression with repository search and add a transferred-conflict fixture only if doing so exercises an actual authority path not already covered. The assertion must preserve structural order:

```text
current observed facts
→ Operator Lore
→ Semantic Memory including NPC_TOLD BELIEF
→ episodic/social memory
```

No production prompt order change is expected.

- [ ] **Step 4: Build deterministic multi-NPC simulation**

Use fixed UUID namespaces/seeds and authoritative game times only. Include at minimum:

```text
two independent speaker/listener pairs
speaker FACT and speaker BELIEF sources
NPC-global, player-scoped and shared semantic subject scopes
repeated same-transfer replay
later corroborating transfers at different gameTime
hundreds of unrelated Semantic/episodic pressure records
multiple game-time distances crossing 36_000-tick decay steps
fresh-root persistence copies/reloads
forward and reversed fixture iteration where operation semantics are commutative
exact evidence IDs, retained source unions and survivor IDs
```

Do not use `Thread.sleep`, `Instant.now`, wall-clock expected values or random UUIDs for asserted identities.

- [ ] **Step 5: Assert exact deterministic outcomes**

At minimum assert:

```text
same logical fixture -> identical deterministic evidence UUIDs
same persisted inputs -> identical retained Semantic/event IDs after fresh reload
foreign-player transferred memory absent from foreign prompt
other-NPC memory absent
NPC-global/current-player shared eligibility preserved
corroboration source unions deterministic
all retained transferred entries remain BELIEF/NPC_TOLD
candidate/result bounds unchanged
```

- [ ] **Step 6: Treat any nondeterminism/privacy/authority failure as RED**

Commit the failing simulation before a production fix. Make the smallest correction. Do not reweight global rankers/retention policies unless the failing contract proves an existing policy defect independent of fixture construction.

- [ ] **Step 7: Run focused and full common suite**

```bash
./gradlew :common:test --tests 'net.conczin.mca.livingworld.memory2.NpcKnowledgeTransferSimulationTest'
./gradlew :common:test
```

---

### Task 9: TDD evidence ledger, changelog and exact-head delivery gates

**Files:**
- Create/update: `docs/superpowers/evidence/2026-08-08-npc-to-npc-knowledge-transfer-tdd.md`
- Modify after runtime GREEN: `CHANGELOG.md`
- PR body: continuously record exact staged evidence.
- After product merge only: `docs/PROJECT_STATE.md`, `docs/ROADMAP.md` in a dedicated reconciliation branch/PR if following current repository precedent.

**Interfaces:**
- Produces auditable delivery evidence; no new runtime behavior.

- [ ] **Step 1: Create the evidence ledger from recorded facts only**

Start with:

```markdown
# NPC-to-NPC Knowledge Transfer TDD Evidence

Date: 2026-08-08
Branch: `feat/npc-to-npc-knowledge-transfer`
Base: `<exact implementation base>`
PR: `<actual PR number after creation>`
```

Then record each actual stage, not planned values:

```text
RED SHA
whether production changed before RED: NO
exact failing test/compile symbol
workflow name + run ID
observed failure count/reason
GREEN production SHA
exact workflow/run ID
preservation-only GREEN stages
```

Do not write invented run IDs, test counts or PASS claims.

- [ ] **Step 2: Update `[Unreleased]` changelog after runtime GREEN**

Under `Added`, record:

```text
server-owned source-backed NPC→NPC Semantic knowledge transfer
listener-owned exact NPC_TOLD DIALOGUE evidence before BELIEF admission
FACT/BELIEF speaker source always becomes listener BELIEF/NPC_TOLD
exact replay idempotency and corroborating source union
explicit SOURCE_NOT_RETAINED / BELIEF_NOT_RETAINED pressure outcomes
```

Under `Changed` or same bullet, explicitly state unchanged boundaries:

```text
no new provider call
no new persistence file/schema/config
no autonomous visible NPC conversation
no rumor/multi-hop propagation
existing 32 / 24+8 / 6 retrieval bounds unchanged
```

Under `Validation`, summarize staged RED→GREEN and deterministic multi-NPC/reload/privacy/pressure simulation only after those tests are actually green.

- [ ] **Step 3: Open/maintain the implementation PR and run exact-head mandatory matrix**

For runtime/persistence changes require the repository's current fail-closed selected matrix. Expected categories include:

```text
Repository security policy
full common tests
deterministic mock-provider tests where selected
risk selector / required Fabric GameTests
Fabric build
NeoForge compile/build
production startup + restart acceptance
persistence recovery when selected
package/release-identity verification
constrained production soak/restart when selected
release dry-run with GitHub Release publication skipped unless explicitly authorized
```

Use the repository workflows rather than inventing replacement local acceptance. If a stage is legitimately not selected, record `SKIPPED`, not PASS.

- [ ] **Step 4: Verify exact-head behavior boundaries before merge**

Read the base→head diff and explicitly confirm:

```text
no new persistence format/version
no public config change
no provider/LLM request change
no client authority
no FACT admission path from NPC_TOLD
no upstream speaker source chain copied into listener BELIEF
no ranking/candidate-limit change
no legacy migration/backfill
no unrelated relationship/gameplay mutation
```

- [ ] **Step 5: Perform independent read-only review**

Review exact base→head for P0/P1/P2 issues with special focus on:

```text
source ownership / TOCTOU reread
speaker-listener orientation
post-consolidation success validation
cross-NPC isolation
foreign-player scope
retry duplicate behavior
pressure partial states
persistence schema stability
release claim accuracy
```

All blocking findings must be fixed with tests; rerun exact-head gates after any production change. Merge only with zero unresolved blocking review threads/findings.

- [ ] **Step 6: Merge only after final exact-head GREEN**

Do not describe the feature as installed `0.2.0` acceptance. Record the actual product merge SHA.

- [ ] **Step 7: Reconcile canonical state after merge**

Create a docs-only branch from the new `1.21.1` head. Update:

```text
docs/PROJECT_STATE.md
docs/ROADMAP.md
```

Record NPC-to-NPC knowledge transfer as complete with exact PR/merge/evidence, preserve the immutable installed 0.2.0 boundary, and advance the next product slice to **provenance-aware rumors** only if all exit criteria above are actually met.

---

## Implementation Order and Commit Discipline

Use the following commit/gate order. Do not squash the staged RED evidence out of the working PR before the evidence ledger captures exact SHAs/runs:

```text
plan/spec approved
→ create feat/npc-to-npc-knowledge-transfer
→ Task 1 tests-only RED
→ Task 1 minimal GREEN
→ Task 2 tests-only RED
→ Task 2 minimal GREEN
→ Task 3 API compile RED
→ Task 3 behavioral RED if needed
→ Task 3 minimal lifecycle GREEN
→ Tasks 4-8 tests-first preservation/RED cycles one contract at a time
→ changelog + evidence sync
→ exact-head full gates
→ independent review
→ final exact-head re-gate after any fixes/docs affecting gate selection
→ squash/merge according to repository policy
→ docs reconciliation PR
```

A later test that unexpectedly fails is not noise. First determine whether it exposes a production defect, a wrong fixture, or an invalid test assumption. Correct test defects without changing production and preserve that diagnosis in the evidence ledger when material.

---

## Self-Review Result

- **Spec coverage:** All approved design requirements are mapped: exact persisted speaker source, authoritative reread, canonical evidence shape/UUID, FACT→BELIEF and BELIEF→BELIEF truth boundary, fixed 50/50 policy, canonical semantic scope, evidence-before-BELIEF ordering, exact post-consolidation verification, four explicit result statuses, pressure partial states, retry/corroboration, fresh-root reload, player privacy, player Working Memory exclusion, long-horizon participation/evictability, current-truth precedence, deterministic multi-NPC simulation, changelog/evidence and release-boundary handling.
- **Placeholder scan:** The plan contains no implementation `TBD`, `TODO`, generic "handle errors" instruction or undefined future behavior. Runtime values and canonical fields are copied from the approved spec. Runtime-specific CI run IDs/PR numbers are intentionally recorded only after they exist and are explicitly marked as actual evidence, not implementation placeholders.
- **Type consistency:** Proposed method/type names are introduced before later tasks consume them. Store lookups use `Optional`; lifecycle result statuses exactly match `ADMITTED`, `REJECTED`, `SOURCE_NOT_RETAINED`, `BELIEF_NOT_RETAINED`; lifecycle calls existing `SemanticBeliefAdmissionPolicy`, `SemanticMemoryIngestionAdapter`, `ControlledSemanticMemoryIngestor` and `SemanticMemoryConsolidator` contracts.
- **Scope check:** This is one coherent Memory 2.0 slice. Autonomous conversations, visible dialogue, voice, multi-hop rumors, distortion and trust weighting remain separate future designs and are explicitly excluded from implementation tasks.
