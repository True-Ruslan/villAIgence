# Provenance-Aware Rumors Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add exact, bounded, acyclic multi-hop provenance to server-owned NPC-to-NPC knowledge transfer while every downstream claim remains `BELIEF / NPC_TOLD` and current server-observed truth remains authoritative.

**Architecture:** Keep Semantic Memory simple: `SemanticMemoryEntry.sourceEventIds` remains direct evidence only. Every new v2 NPC-to-NPC transfer `MemoryEvent` carries one immutable `KnowledgeTransferProvenance` path with an origin snapshot and at most eight exact hops. A pure policy owns canonical v2 identity/integrity, a pure factory builds lineage, a read-only resolver selects one retained direct branch without knowing the proposed listener, and `NpcKnowledgeTransferLifecycle` remains the evidence-before-BELIEF orchestration boundary.

**Tech Stack:** Java 21, JUnit 5, Gson world-local JSON persistence, Gradle, Fabric 1.21.1 primary distribution, NeoForge compile compatibility, GitHub Actions acceptance/security/soak/release-dry-run workflows.

## Global Constraints

- Approved design: `docs/superpowers/specs/2026-08-09-provenance-aware-rumors-design.md` at `d4c932425ff852dbcf9befe2e2763568f5cd743f`.
- Before runtime work, verify current `1.21.1` has not advanced unexpectedly; reconcile rather than silently building on stale state.
- Minecraft/server state is truth; provider/LLM is never authority.
- `FACT` requires `SYSTEM_OBSERVED`; downstream rumor remains `BELIEF / NPC_TOLD` at every hop.
- No new `RUMOR` kind/provenance in this slice.
- Statement uses existing `SemanticMemoryIngestionAdapter.normalizeAndLimitStatement`, hard bound 240 Unicode code points, with no paraphrase/distortion.
- Semantic subject scope is preserved exactly; provenance participants never expand `relatedEntities`.
- One direct evidence event carries one lineage. Semantic consolidation unions direct `sourceEventIds` only; no provenance DAG is stored in Semantic Memory.
- `MAX_PROVENANCE_HOPS = 8`; attempted ninth hop returns `PROVENANCE_LIMIT_REACHED`.
- Repeated NPC UUIDs in the path return `PROVENANCE_CYCLE`; for a selected lineage cycle is checked before hop limit.
- Direct branch ordering is `event.gameTime DESC`, then `event.id ASC`.
- Resolver must not receive the proposed listener. After canonical branch selection, cycle/limit rejection never falls back to another branch.
- First-hop origins: FACT/SYSTEM_OBSERVED, BELIEF/PLAYER_TOLD, BELIEF/INFERRED only. BELIEF/NPC_TOLD must inherit valid structured lineage and may never reset origin.
- New transfer identity namespace is exactly `npc-knowledge-transfer-v2`.
- `memory2.json` stays format version 1. No migration, backfill, dual reader, checkpoint ledger, summary parsing or compatibility bridge.
- Historical v1 transfer evidence may be readable but cannot continue multi-hop provenance.
- Fixed transfer values remain `importance=50`, `confidence=50`, `createdAtEpochMillis=0`.
- Retention coefficients, long-horizon bounds (`32` candidate / `6` result), rankers, provider protocol, public config, relationship, UI, voice, scheduler and gameplay authority remain unchanged.
- No second provider call.
- Exact retry is deterministic/idempotent and uses no wall-clock provenance input.
- Expected fail-closed outcomes create no partial listener BELIEF.
- Official installed release remains `0.2.0+1.21.1`; feature evidence is unreleased until a later immutable release is explicitly accepted.
- Runtime behavior follows strict observed tests-only RED → minimal GREEN. Preservation tests that already pass require no production change.
- Runtime PR updates root `CHANGELOG.md`. Canonical state/roadmap reconciliation happens after product merge in a separate docs-only PR.

---

## File Map

### New production files

- `common/src/main/java/net/conczin/mca/livingworld/memory2/KnowledgeTransferProvenance.java` — persisted `Origin` + ordered `Hop` model only.
- `common/src/main/java/net/conczin/mca/livingworld/memory2/KnowledgeTransferProvenancePolicy.java` — pure v2 deterministic identity and fail-closed integrity/cycle/limit/scope/statement checks.
- `common/src/main/java/net/conczin/mca/livingworld/memory2/KnowledgeTransferProvenanceFactory.java` — pure canonical first-hop and append-hop construction.
- `common/src/main/java/net/conczin/mca/livingworld/memory2/KnowledgeTransferProvenanceResolver.java` — read-only canonical retained-branch selection for an NPC_TOLD Semantic source.

### Existing production files

- `MemoryEvent.java` — nullable `knowledgeTransferProvenance` field; existing convenience constructors delegate null.
- `NpcToldDialogueAdapter.java` — new v2 evidence requires non-null lineage and delegates evidence UUID calculation to the pure policy.
- `NpcKnowledgeTransferPolicy.java` — canonical persisted evidence equality includes the exact lineage payload.
- `NpcKnowledgeTransferLifecycle.java` — first-hop derivation vs NPC_TOLD inheritance, branch resolution, cycle/limit, existing evidence-before-BELIEF flow.
- `NpcKnowledgeTransferResult.java` — add exact provenance statuses.
- `CHANGELOG.md` — unreleased behavior after GREEN.

### Tests

Create:
- `KnowledgeTransferProvenanceModelTest.java`
- `KnowledgeTransferProvenancePolicyTest.java`
- `KnowledgeTransferProvenanceResolverTest.java`
- `NpcKnowledgeTransferRumorSimulationTest.java`

Extend:
- `NpcToldDialogueAdapterTest.java`
- `NpcKnowledgeTransferPolicyTest.java`
- `NpcKnowledgeTransferLifecycleTest.java`
- `NpcKnowledgeTransferPersistenceTest.java`
- `NpcKnowledgeTransferPressureTest.java`
- `NpcKnowledgeTransferSimulationTest.java`
- `Memory2DialogueHistoryTest.java`

Evidence:
- `docs/superpowers/evidence/2026-08-09-provenance-aware-rumors-tdd.md`

---

### Task 1: Persisted Provenance Model

**Files:**
- Create test first: `common/src/test/java/net/conczin/mca/livingworld/memory2/KnowledgeTransferProvenanceModelTest.java`
- Create after RED: `common/src/main/java/net/conczin/mca/livingworld/memory2/KnowledgeTransferProvenance.java`
- Modify after RED: `common/src/main/java/net/conczin/mca/livingworld/memory2/MemoryEvent.java`

**Produces:**

```java
public record KnowledgeTransferProvenance(Origin origin, List<Hop> hops) {
    public record Origin(
            UUID originNpcId,
            UUID originSemanticEntryId,
            SemanticMemoryEntry.Kind originKind,
            MemoryEvent.Provenance originProvenance,
            String statement,
            List<UUID> relatedEntities
    ) {}

    public record Hop(
            UUID speakerNpcId,
            UUID listenerNpcId,
            UUID speakerSemanticEntryId,
            UUID evidenceEventId,
            long gameTime
    ) {}
}
```

`MemoryEvent` gains final canonical component:

```java
KnowledgeTransferProvenance knowledgeTransferProvenance
```

- [ ] **1.1 Write tests-only model contract**

```java
@Test
void ordinaryEventHasNoTransferProvenance() {
    MemoryEvent event = new MemoryEvent(
            UUID.randomUUID(), UUID.randomUUID(), MemoryEvent.Type.OBSERVATION,
            "observed", List.of(), MemoryEvent.Provenance.SYSTEM_OBSERVED,
            10L, 0L, 50, 0, 100, List.of());
    assertNull(event.knowledgeTransferProvenance());
}

@Test
void modelRetainsExactOriginAndHopOrder() {
    KnowledgeTransferProvenance value = new KnowledgeTransferProvenance(
            new KnowledgeTransferProvenance.Origin(
                    ORIGIN_NPC, ORIGIN_ENTRY, SemanticMemoryEntry.Kind.FACT,
                    MemoryEvent.Provenance.SYSTEM_OBSERVED,
                    "Bridge destroyed", List.of(PLAYER)),
            List.of(new KnowledgeTransferProvenance.Hop(
                    ORIGIN_NPC, LISTENER, ORIGIN_ENTRY, EVIDENCE, 100L)));
    assertEquals(ORIGIN_NPC, value.origin().originNpcId());
    assertEquals(EVIDENCE, value.hops().getFirst().evidenceEventId());
}
```

- [ ] **1.2 Observe compile RED**

```bash
./gradlew :common:test --tests 'net.conczin.mca.livingworld.memory2.KnowledgeTransferProvenanceModelTest' --no-daemon
```

Expected: `compileTestJava` missing-symbol errors for new model/accessor. Commit/push tests-only head and record exact CI failure before production changes.

- [ ] **1.3 Implement minimal model + source-compatible MemoryEvent constructors**

Do not perform authority repair/canonicalization in record constructors. If defensive list wrapping is added, preserve null elements/order rather than sorting, deduplicating or dropping invalid data. Authority validation belongs to Task 2.

Preserve the previous full constructor shape:

```java
public MemoryEvent(
        UUID id, UUID ownerNpcId, Type type, String summary,
        List<UUID> participants, Provenance provenance,
        long gameTime, long createdAtEpochMillis,
        int importance, int emotionalWeight, int confidence,
        List<String> relationshipReasons,
        DialogueExchange dialogue,
        RelationshipTransition relationshipTransition,
        RelationshipCause relationshipCause
) {
    this(id, ownerNpcId, type, summary, participants, provenance,
            gameTime, createdAtEpochMillis, importance, emotionalWeight,
            confidence, relationshipReasons, dialogue,
            relationshipTransition, relationshipCause, null);
}
```

- [ ] **1.4 Verify GREEN**

```bash
./gradlew :common:test --tests 'net.conczin.mca.livingworld.memory2.KnowledgeTransferProvenanceModelTest' --no-daemon
./gradlew :common:test --no-daemon
```

- [ ] **1.5 Commit**

```bash
git add common/src/main/java/net/conczin/mca/livingworld/memory2/KnowledgeTransferProvenance.java \
        common/src/main/java/net/conczin/mca/livingworld/memory2/MemoryEvent.java \
        common/src/test/java/net/conczin/mca/livingworld/memory2/KnowledgeTransferProvenanceModelTest.java
git commit -m 'feat: add transfer provenance data model'
```

---

### Task 2: Pure v2 Identity, Integrity Policy and Canonical Factory

**Files:**
- Create test first: `KnowledgeTransferProvenancePolicyTest.java`
- Create after RED: `KnowledgeTransferProvenancePolicy.java`
- Create after RED: `KnowledgeTransferProvenanceFactory.java`

**Produces:**

```java
final class KnowledgeTransferProvenancePolicy {
    static final int MAX_HOPS = 8;

    static UUID deterministicEvidenceId(
            UUID speakerNpcId,
            UUID listenerNpcId,
            UUID speakerSemanticEntryId,
            long authoritativeGameTime
    );

    static boolean valid(KnowledgeTransferProvenance provenance);
    static boolean originMatchesSource(KnowledgeTransferProvenance provenance, SemanticMemoryEntry source);
    static boolean directEvidenceMatches(
            KnowledgeTransferProvenance provenance,
            MemoryEvent evidence,
            SemanticMemoryEntry currentSpeakerSource
    );
    static boolean wouldCycle(KnowledgeTransferProvenance provenance, UUID proposedListenerNpcId);
    static boolean atHopLimit(KnowledgeTransferProvenance provenance);
    static List<UUID> canonicalIds(List<UUID> ids);
}

final class KnowledgeTransferProvenanceFactory {
    static Optional<KnowledgeTransferProvenance> firstHop(
            SemanticMemoryEntry source,
            UUID listenerNpcId,
            UUID evidenceEventId,
            long authoritativeGameTime
    );

    static Optional<KnowledgeTransferProvenance> appendHop(
            KnowledgeTransferProvenance current,
            SemanticMemoryEntry speakerSource,
            UUID listenerNpcId,
            UUID evidenceEventId,
            long authoritativeGameTime
    );
}
```

The policy is the single authority for v2 evidence ID computation. Task 3 makes `NpcToldDialogueAdapter.deterministicEvidenceId(...)` delegate to it; the algorithm must not be duplicated.

- [ ] **2.1 Write tests-only pure contract**

Manually verify namespace input:

```java
String canonical = "npc-knowledge-transfer-v2\n"
        + LISTENER + "\n" + SPEAKER + "\n" + SOURCE + "\n" + 100L;
UUID expected = UUID.nameUUIDFromBytes(canonical.getBytes(StandardCharsets.UTF_8));
assertEquals(expected, KnowledgeTransferProvenancePolicy.deterministicEvidenceId(
        SPEAKER, LISTENER, SOURCE, 100L));
```

Also require:

```java
assertTrue(KnowledgeTransferProvenancePolicy.valid(validTwoHop()));
assertFalse(KnowledgeTransferProvenancePolicy.valid(brokenContinuity()));
assertFalse(KnowledgeTransferProvenancePolicy.valid(repeatedNpc()));
assertFalse(KnowledgeTransferProvenancePolicy.valid(npcToldOriginReset()));
assertTrue(KnowledgeTransferProvenancePolicy.wouldCycle(validTwoHop(), ORIGIN_NPC));
assertFalse(KnowledgeTransferProvenancePolicy.wouldCycle(validTwoHop(), NEW_NPC));
```

First-hop factory cases must accept FACT/SYSTEM_OBSERVED, BELIEF/PLAYER_TOLD, BELIEF/INFERRED and reject BELIEF/NPC_TOLD. Build a valid 8-hop fixture and assert `atHopLimit` only at exactly 8.

- [ ] **2.2 Observe compile RED**

```bash
./gradlew :common:test --tests 'net.conczin.mca.livingworld.memory2.KnowledgeTransferProvenancePolicyTest' --no-daemon
```

- [ ] **2.3 Implement pure v2 identity/policy/factory**

Use `SemanticMemoryIngestionAdapter.normalizeAndLimitStatement` for canonical statement comparison. Canonical scope is sorted unique non-null UUIDs. Runtime factory emits canonical scope; persisted validation requires the stored scope already equals that canonical list rather than silently repairing it.

Path validation:

```text
1 <= hops <= 8
first speaker == origin NPC
first speaker Semantic ID == origin Semantic ID
previous listener == next speaker
no NPC appears twice
for each hop: evidenceEventId == deterministicEvidenceId(v2 tuple)
```

Origin validity:

```text
FACT → SYSTEM_OBSERVED only
BELIEF → PLAYER_TOLD or INFERRED only
BELIEF/NPC_TOLD origin → invalid
```

- [ ] **2.4 Verify focused/full GREEN**

```bash
./gradlew :common:test --tests 'net.conczin.mca.livingworld.memory2.KnowledgeTransferProvenancePolicyTest' --no-daemon
./gradlew :common:test --no-daemon
```

- [ ] **2.5 Commit**

```bash
git add common/src/main/java/net/conczin/mca/livingworld/memory2/KnowledgeTransferProvenancePolicy.java \
        common/src/main/java/net/conczin/mca/livingworld/memory2/KnowledgeTransferProvenanceFactory.java \
        common/src/test/java/net/conczin/mca/livingworld/memory2/KnowledgeTransferProvenancePolicyTest.java
git commit -m 'feat: validate bounded rumor provenance'
```

---

### Task 3: v2 Canonical Transfer Evidence + First-Hop Lifecycle

**Files:**
- Modify tests first: `NpcToldDialogueAdapterTest.java`, `NpcKnowledgeTransferLifecycleTest.java`
- Modify after RED: `NpcToldDialogueAdapter.java`, `NpcKnowledgeTransferPolicy.java`, `NpcKnowledgeTransferLifecycle.java`, `NpcKnowledgeTransferResult.java`

**Final adapter surface:**

```java
static Optional<MemoryEvent> create(
        UUID speakerNpcId,
        UUID listenerNpcId,
        UUID speakerSemanticEntryId,
        long authoritativeGameTime,
        String statement,
        KnowledgeTransferProvenance provenance
);

static UUID deterministicEvidenceId(...) {
    return KnowledgeTransferProvenancePolicy.deterministicEvidenceId(...);
}
```

**Statuses:**

```java
ADMITTED,
REJECTED,
SOURCE_NOT_RETAINED,
BELIEF_NOT_RETAINED,
PROVENANCE_UNAVAILABLE,
PROVENANCE_LIMIT_REACHED,
PROVENANCE_CYCLE
```

- [ ] **3.1 Write tests-only behavioral requirements**

Update adapter test to expect namespace v2 and exact non-null payload. Extend lifecycle for three first-hop origin types:

```text
FACT / SYSTEM_OBSERVED
BELIEF / PLAYER_TOLD
BELIEF / INFERRED
```

For each successful transfer assert listener is still BELIEF/NPC_TOLD, listener Semantic sources contain only the new direct evidence UUID, upstream Semantic source IDs are not copied, origin snapshot is exact, and evidence has exactly one hop.

- [ ] **3.2 Observe behavioral RED against current v1/no-payload path**

```bash
./gradlew :common:test \
  --tests 'net.conczin.mca.livingworld.memory2.NpcToldDialogueAdapterTest' \
  --tests 'net.conczin.mca.livingworld.memory2.NpcKnowledgeTransferLifecycleTest' \
  --no-daemon
```

Expected failures are v1 identity/payload/first-hop assertions. Record exact failing assertions/count before implementation.

- [ ] **3.3 Implement minimal first-hop GREEN**

For non-NPC_TOLD source:

```text
exact Semantic source lookup
→ authoritative exact reread/snapshot match
→ policy deterministic v2 evidence ID
→ factory firstHop
→ adapter canonical v2 event with lineage
→ append event
→ exact reread
→ NpcKnowledgeTransferPolicy canonical equality including lineage
→ generic SemanticBeliefAdmissionPolicy
→ ControlledSemanticMemoryIngestor
→ compatible retained listener BELIEF containing new direct evidence ID
```

At this stage an NPC_TOLD speaker BELIEF returns `PROVENANCE_UNAVAILABLE`; it must not be used as a fake first-hop origin.

- [ ] **3.4 Verify GREEN**

```bash
./gradlew :common:test \
  --tests 'net.conczin.mca.livingworld.memory2.NpcToldDialogueAdapterTest' \
  --tests 'net.conczin.mca.livingworld.memory2.NpcKnowledgeTransferLifecycleTest' \
  --tests 'net.conczin.mca.livingworld.memory2.NpcKnowledgeTransferPolicyTest' \
  --no-daemon
./gradlew :common:test --no-daemon
```

- [ ] **3.5 Commit**

```bash
git add common/src/main/java/net/conczin/mca/livingworld/memory2/NpcToldDialogueAdapter.java \
        common/src/main/java/net/conczin/mca/livingworld/memory2/NpcKnowledgeTransferPolicy.java \
        common/src/main/java/net/conczin/mca/livingworld/memory2/NpcKnowledgeTransferLifecycle.java \
        common/src/main/java/net/conczin/mca/livingworld/memory2/NpcKnowledgeTransferResult.java \
        common/src/test/java/net/conczin/mca/livingworld/memory2/NpcToldDialogueAdapterTest.java \
        common/src/test/java/net/conczin/mca/livingworld/memory2/NpcKnowledgeTransferLifecycleTest.java
git commit -m 'feat: persist v2 rumor origin evidence'
```

---

### Task 4: Exact Resolver + A→B→C Multi-Hop Continuation

**Files:**
- Create tests first: `KnowledgeTransferProvenanceResolverTest.java`
- Extend tests first: `NpcKnowledgeTransferLifecycleTest.java`
- Create after RED: `KnowledgeTransferProvenanceResolver.java`
- Modify after RED: `NpcKnowledgeTransferLifecycle.java`

**Resolver interface:**

```java
final class KnowledgeTransferProvenanceResolver {
    static Optional<ResolvedSource> resolve(
            MemoryEventStore eventStore,
            SemanticMemoryEntry speakerSource
    );

    record ResolvedSource(MemoryEvent evidence, KnowledgeTransferProvenance provenance) {}
}
```

It deliberately accepts no listener.

- [ ] **4.1 Create resolver compile RED**

Tests require exact retained source resolution and no bounded-recent approximation. Run:

```bash
./gradlew :common:test --tests 'net.conczin.mca.livingworld.memory2.KnowledgeTransferProvenanceResolverTest' --no-daemon
```

Expected: missing resolver symbols.

- [ ] **4.2 Add separate A→B→C behavioral RED**

After resolver API exists but before lifecycle continuation, create A FACT → B, then attempt B's retained NPC_TOLD BELIEF → C. Expected pre-GREEN status remains `PROVENANCE_UNAVAILABLE`; desired assertions require ADMITTED, two ordered hops, unchanged origin A, and C Semantic source list `[B→C evidence]` only.

- [ ] **4.3 Implement canonical retained resolver**

For each UUID in `speakerSource.sourceEventIds`:

```java
MemoryEventStore.findById(speakerSource.ownerNpcId(), sourceEventId)
```

Eligible event requirements:

```text
DIALOGUE/NPC_TOLD canonical v2 transfer shape
non-null valid provenance
last hop evidence ID == event.id
last hop listener == current speaker
origin statement == current Semantic normalized statement
origin canonical scope == current Semantic canonical scope
```

Sort eligible retained evidence:

```java
Comparator.comparingLong(MemoryEvent::gameTime).reversed()
        .thenComparing(event -> event.id().toString())
```

Return exactly one first result.

- [ ] **4.4 Wire NPC_TOLD continuation**

```text
resolve canonical branch
→ none: PROVENANCE_UNAVAILABLE
→ selected branch: cycle check with proposed listener
→ cycle: PROVENANCE_CYCLE
→ else hop-limit check
→ limit: PROVENANCE_LIMIT_REACHED
→ compute v2 ID
→ factory appendHop
→ normal evidence-before-BELIEF persistence
```

Never call resolver again after cycle/limit rejection.

- [ ] **4.5 Verify focused/full GREEN and commit**

```bash
./gradlew :common:test \
  --tests 'net.conczin.mca.livingworld.memory2.KnowledgeTransferProvenanceResolverTest' \
  --tests 'net.conczin.mca.livingworld.memory2.NpcKnowledgeTransferLifecycleTest' \
  --no-daemon
./gradlew :common:test --no-daemon
git commit -m 'feat: propagate exact rumor lineage'
```

---

### Task 5: Branch Selection, No-Fallback, Cycle and Hop-Limit Hardening

**Files:**
- Extend tests first: `KnowledgeTransferProvenanceResolverTest.java`, `NpcKnowledgeTransferLifecycleTest.java`
- Production changes only after observed failing behavior.

- [ ] **5.1 Multi-source branch-selection matrix**

Create one consolidated speaker BELIEF referencing at least three direct v2 evidence events. Prove:

```text
newest valid retained gameTime wins
same gameTime → smaller evidence UUID string wins
sourceEventIds insertion order has no effect
unreferenced retained evidence is ignored
evicted highest-priority branch falls to next retained valid branch
malformed branch is filtered before ordering
```

- [ ] **5.2 Listener-independent no-fallback regression**

Create two valid retained branches where canonical highest-priority branch would cycle with proposed listener but lower branch would not. Expected result is `PROVENANCE_CYCLE` and zero writes. This proves listener cannot influence ancestry selection.

- [ ] **5.3 Cycle matrix**

All return `PROVENANCE_CYCLE`:

```text
A→A
A→B→A
A→B→C→A
A→B→C→B
```

First-hop `speaker==listener` must map to the cycle status rather than generic REJECTED.

- [ ] **5.4 Hop-limit matrix**

Eight hops admitted. Ninth hop returns `PROVENANCE_LIMIT_REACHED`. If the ninth proposed listener is also already in path, `PROVENANCE_CYCLE` wins.

- [ ] **5.5 Observe RED or preservation GREEN**

```bash
./gradlew :common:test \
  --tests 'net.conczin.mca.livingworld.memory2.KnowledgeTransferProvenanceResolverTest' \
  --tests 'net.conczin.mca.livingworld.memory2.NpcKnowledgeTransferLifecycleTest' \
  --no-daemon
```

If failures occur, record behavioral RED before minimal production correction. If all pass, commit tests-only preservation evidence.

- [ ] **5.6 Full common + commit**

```bash
./gradlew :common:test --no-daemon
git commit -m 'test: harden rumor branch and cycle policy'
```

---

### Task 6: Fail-Closed Integrity Mutation + Provenance-Unavailable Matrix

**Files:**
- Extend: `NpcKnowledgeTransferPolicyTest.java`, `KnowledgeTransferProvenancePolicyTest.java`, `KnowledgeTransferProvenanceResolverTest.java`
- Production only after observed RED.

- [ ] **6.1 Extend canonical MemoryEvent mutation helper**

Test helper accepts the new final `KnowledgeTransferProvenance` field and constructs full canonical MemoryEvent so every old event-field mutation plus lineage mutation remains testable.

- [ ] **6.2 Mutate one provenance field at a time**

Required mutations:

```text
origin NPC
origin Semantic entry UUID
origin kind
origin provenance
origin statement
origin scope
hop speaker
hop listener
hop speaker Semantic entry UUID
hop evidence UUID
hop gameTime
hop order
missing hop
extra hop
```

Each invalidates provenance/canonical evidence.

- [ ] **6.3 Provenance-unavailable source matrix**

For an NPC_TOLD speaker source, no listener writes and exact result `PROVENANCE_UNAVAILABLE` when there is:

```text
no retained direct evidence
historical v1 evidence without payload
malformed payload
BELIEF/NPC_TOLD origin reset
wrong event owner
retained evidence not referenced by current Semantic source
last hop not ending at speaker
statement mismatch
scope mismatch
```

Candidate filtering may skip bad direct evidence and use another valid referenced retained branch; only no remaining valid branch yields `PROVENANCE_UNAVAILABLE`.

- [ ] **6.4 Run focused suite**

```bash
./gradlew :common:test \
  --tests 'net.conczin.mca.livingworld.memory2.KnowledgeTransferProvenancePolicyTest' \
  --tests 'net.conczin.mca.livingworld.memory2.KnowledgeTransferProvenanceResolverTest' \
  --tests 'net.conczin.mca.livingworld.memory2.NpcKnowledgeTransferPolicyTest' \
  --no-daemon
```

- [ ] **6.5 Minimal corrections only after RED; then full common and commit**

Never sort/repair malformed persisted ancestry into valid authority and never parse summary prose.

```bash
./gradlew :common:test --no-daemon
git commit -m 'test: enforce rumor provenance integrity'
```

---

### Task 7: Restart, Replay, Scope Privacy and Working-Memory Isolation

**Files:**
- Extend: `NpcKnowledgeTransferPersistenceTest.java`, `Memory2DialogueHistoryTest.java`
- Production only after observed preservation failure.

- [ ] **7.1 Fresh-root A→B→C round-trip**

Copy `memory2.json` and `semantic-memory.json` to a distinct world root. Assert exact C direct evidence UUID, origin, hop order/content, C direct Semantic source IDs. Replay B→C with same tuple/time after reload and assert deterministic same event/result identity and no duplicate logical state.

- [ ] **7.2 Exact global/private/shared scope through multiple hops**

For `[]`, `[playerA]`, `[playerA, entityX]`, assert downstream scope remains exact canonical set, speaker UUIDs are absent unless originally semantic subjects, playerA visibility is preserved, playerB private visibility remains excluded.

- [ ] **7.3 NPC rumor evidence stays out of player dialogue reconstruction**

```java
assertEquals(List.of(), Memory2DialogueHistory.load(world, rumorListener, playerA));
```

Then persist a genuine structured player dialogue and assert it reconstructs normally in the same store.

- [ ] **7.4 Focused/full GREEN and commit**

```bash
./gradlew :common:test \
  --tests 'net.conczin.mca.livingworld.memory2.NpcKnowledgeTransferPersistenceTest' \
  --tests 'net.conczin.mca.livingworld.memory2.Memory2DialogueHistoryTest' \
  --no-daemon
./gradlew :common:test --no-daemon
git commit -m 'test: preserve rumor lineage across restart and privacy scopes'
```

---

### Task 8: Pressure, Direct-Evidence Loss and Bounded Forgetting

**Files:**
- Extend: `NpcKnowledgeTransferPressureTest.java`, `NpcKnowledgeTransferSimulationTest.java`
- Production only after observed RED.

- [ ] **8.1 Preserve `SOURCE_NOT_RETAINED` and `BELIEF_NOT_RETAINED` under v2**

Existing capacity-pressure tests must still pass with provenance-bearing events and unchanged retention coefficients.

- [ ] **8.2 Older ancestry event may disappear without destroying later snapshot**

Create A→B→C, then evict physical A→B evidence from B's event store. C's B→C direct event must still contain exact two-hop immutable lineage and C may still recall its BELIEF.

- [ ] **8.3 Current direct evidence loss blocks further propagation**

Keep C BELIEF retained, evict its B→C direct evidence, attempt C→D:

```java
assertEquals(PROVENANCE_UNAVAILABLE, result.status());
assertTrue(MemoryEventStore.forWorld(world).getRecent(D, 64).isEmpty());
assertTrue(SemanticMemoryStore.forWorld(world).getRecent(D, 64).isEmpty());
```

- [ ] **8.4 Rumor evidence/BELIEF remain evictable**

At capacity 1, stronger existing SYSTEM_OBSERVED data may displace both classes according to unchanged policies; provenance provides no pinning.

- [ ] **8.5 Focused/full suite + commit**

```bash
./gradlew :common:test \
  --tests 'net.conczin.mca.livingworld.memory2.NpcKnowledgeTransferPressureTest' \
  --tests 'net.conczin.mca.livingworld.memory2.NpcKnowledgeTransferSimulationTest' \
  --no-daemon
./gradlew :common:test --no-daemon
git commit -m 'test: verify bounded rumor retention and forgetting'
```

---

### Task 9: Deterministic 10+ NPC Simulation + Truth Preservation

**Files:**
- Create: `NpcKnowledgeTransferRumorSimulationTest.java`
- Extend `NpcKnowledgeTransferSimulationTest.java` only if a shared fixture is genuinely reusable; do not move production behavior for test convenience.

- [ ] **9.1 Deterministic scenario**

Use fixed UUIDs/game times only. Required minimum:

```text
>=10 NPCs
one valid 8-hop chain
rejected ninth hop
multiple independent lineages
one corroborated final BELIEF
cycle attempts
>200 unrelated Semantic records
>200 unrelated episodic/social records
private/shared/global scope
forward pressure insertion
reverse pressure insertion
>=2 fresh-root reloads
```

No random UUIDs in expected state, no sleep, no system wall clock.

- [ ] **9.2 Compare exact snapshots**

```java
record ScenarioSnapshot(
        Map<UUID, List<UUID>> semanticIdsByNpc,
        Map<UUID, List<UUID>> eventIdsByNpc,
        Map<UUID, List<KnowledgeTransferProvenance>> provenanceByNpc,
        Map<UUID, List<String>> promptContextByNpc
) {}
```

Forward == reverse; each fresh reload == source snapshot.

- [ ] **9.3 Direct-source vs ancestry separation**

Corroborated BELIEF may contain several direct evidence UUIDs. Ancestry UUIDs remain only inside each direct event lineage unless separately direct evidence for that listener.

- [ ] **9.4 Truth/authority preservation**

At deep hop, context line remains `BELIEF | provenance=NPC_TOLD`; origin FACT does not change downstream kind/provenance. Existing formatter contract still contains:

```text
Current observed factual context wins on conflict.
Confidence never converts a BELIEF into a FACT.
```

Seed a conflicting SYSTEM_OBSERVED Semantic FACT and assert the rumor is still represented as BELIEF; full common suite continues to exercise the pre-existing snapshot authority-order regressions. This task does not modify prompt composition.

- [ ] **9.5 Bounded retrieval/privacy**

Every semantic context `<= SemanticMemoryContextProvider.MAX_RESULTS`; foreign-player private rumor remains absent; eligible global rumor remains subject to the existing deterministic rank/bounds.

- [ ] **9.6 Run and commit**

```bash
./gradlew :common:test --tests 'net.conczin.mca.livingworld.memory2.NpcKnowledgeTransferRumorSimulationTest' --no-daemon
./gradlew :common:test --no-daemon
git commit -m 'test: simulate bounded multi-hop rumors'
```

Any failure that requires production correction is preserved as observed RED before the correction.

---

### Task 10: Changelog, TDD Ledger, Exact-Head Review and Delivery

**Files:**
- Modify: `CHANGELOG.md`
- Create: `docs/superpowers/evidence/2026-08-09-provenance-aware-rumors-tdd.md`
- After runtime merge only: docs-only update to `docs/PROJECT_STATE.md`, `docs/ROADMAP.md`.

- [ ] **10.1 Update root `[Unreleased]`**

Record: structured bounded multi-hop provenance, v2 identity, deterministic one-branch selection, acyclic max-eight lineage, direct-only Semantic evidence IDs, exact text/scope, fail-closed missing/corrupt lineage, and explicit absence of uncertainty/distortion/trust/new config/store/migration.

- [ ] **10.2 Build exact TDD evidence ledger**

For each task record tests-only RED SHA, exact CI workflow/run, exact failure reason/count, minimal GREEN SHA, focused/full result, and whether preservation hardening required production changes. Include approved spec and plan SHAs. Keep official installed `0.2.0` boundary explicit.

- [ ] **10.3 Open/update draft runtime PR and run exact-head matrix**

Mandatory fresh exact-head workflows:

```text
Repository security policy
VillAIgence CI
VillAIgence Production Soak
VillAIgence GitHub Release dry-run
```

Main CI must cover selected common/mock, risk GameTests, Fabric+NeoForge, production contracts, staged startup/restart, persistence recovery and package verification. Soak covers constrained-heap concurrency/staging + restart cycles. Release dry-run covers acceptance/recovery/GameTests/package identity; publication job must be `SKIPPED`. Do not publish a release.

- [ ] **10.4 Independent base→head review**

Explicitly inspect:

```text
provider/client cannot inject lineage
resolver has no listener input
NPC_TOLD cannot reset origin
cycle before limit
no listener-dependent fallback
no ancestry pollution of Semantic sourceEventIds
no retention/ranker/config changes
no unbounded provenance growth
no summary parsing authority
no migration/backfill/version bump
```

Record P0/P1/P2 and unresolved review-thread count. Behavioral findings receive their own RED→GREEN before merge.

- [ ] **10.5 Final evidence-sync re-gate**

After last evidence/changelog commit, rerun/fetch all mandatory workflows for that exact commit. Previous runtime-head success is supplementary, not sufficient.

- [ ] **10.6 Squash merge exact verified head**

Use expected head SHA. No release publication.

- [ ] **10.7 Separate docs reconciliation**

From merged `1.21.1`, update exactly `docs/PROJECT_STATE.md` and `docs/ROADMAP.md`: mark exact multi-hop provenance-aware rumor lineage COMPLETE and advance next product slice to uncertainty/contradiction/bounded-distortion design. Preserve installed `0.2.0+1.21.1` evidence. Verify docs-only CI/security and expected skips, then squash merge.

---

## Self-Review Result

Spec coverage mapping:

```text
payload/origin/hops                     Task 1
v2 identity + pure integrity            Task 2
first-hop origin                        Task 3
A→B→C inheritance                       Task 4
canonical branches/no-fallback          Task 5
cycle/max-eight/status precedence       Task 5
corruption/mutation/unavailable          Task 6
restart/replay/scope/privacy             Task 7
pressure/forgetting/direct-source loss  Task 8
10+ NPC deterministic simulation        Task 9
truth class/prompt preservation          Task 9
changelog/evidence/full delivery         Task 10
post-merge state reconciliation          Task 10
```

Placeholder scan: no unresolved implementation placeholders are permitted in execution. Type/signature review: `KnowledgeTransferProvenancePolicy.deterministicEvidenceId(...)` is the single v2 identity authority; Task 3 adapter delegates to it, eliminating the earlier sequencing/duplication risk. Scope review: this plan remains one cohesive Memory 2.0 rumor-provenance primitive and does not include uncertainty, contradictions, distortion, trust weighting, autonomous spread or settlement simulation.
