# Provenance-Aware Rumors Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add exact, bounded, acyclic multi-hop provenance to server-owned NPC-to-NPC knowledge transfer while every downstream claim remains `BELIEF / NPC_TOLD` and current server-observed truth remains authoritative.

**Architecture:** Keep Semantic Memory simple: `SemanticMemoryEntry.sourceEventIds` continues to contain direct evidence only. Every new v2 NPC-to-NPC transfer `MemoryEvent` carries one immutable `KnowledgeTransferProvenance` path containing an origin snapshot and at most eight exact hops. A pure policy validates lineage, a pure/store-backed resolver deterministically selects one retained direct branch for `NPC_TOLD` continuation, and `NpcKnowledgeTransferLifecycle` remains the single orchestration boundary for evidence-before-BELIEF persistence.

**Tech Stack:** Java 21, JUnit 5, Gson-backed world-local JSON stores, Gradle, Fabric 1.21.1 primary distribution, NeoForge compile compatibility, GitHub Actions acceptance/security/soak/release-dry-run workflows.

## Global Constraints

- Base design: `docs/superpowers/specs/2026-08-09-provenance-aware-rumors-design.md` at approved spec head `d4c932425ff852dbcf9befe2e2763568f5cd743f`.
- Runtime implementation starts from the approved spec branch head; before runtime work verify current `1.21.1` has not advanced unexpectedly and reconcile rather than silently implementing on a stale base.
- Minecraft/server state is truth; provider/LLM is never authority.
- `FACT` requires `SYSTEM_OBSERVED`; downstream rumor knowledge is always `BELIEF / NPC_TOLD`.
- No new `RUMOR` Semantic kind or provenance enum in this slice.
- Exact statement text is preserved through existing `SemanticMemoryIngestionAdapter.normalizeAndLimitStatement`, hard bound 240 Unicode code points.
- Canonical Semantic scope is preserved exactly; provenance speaker/listener UUIDs never expand `relatedEntities`.
- One direct transfer evidence carries exactly one provenance path; Semantic consolidation unions only direct `sourceEventIds` and does not merge provenance DAGs.
- `MAX_PROVENANCE_HOPS = 8`; attempted ninth hop is rejected with `PROVENANCE_LIMIT_REACHED`.
- Repeated NPC UUIDs in a path are rejected with `PROVENANCE_CYCLE`; cycle rejection is evaluated before hop-limit rejection for an already selected lineage.
- Canonical direct-branch selection is `event.gameTime DESC`, then `event.id ASC`.
- Branch selection is independent of the proposed listener; after selecting the highest-priority valid lineage, cycle/hop-limit rejection must not fall back to a lower-priority branch.
- A first-hop origin may be `FACT / SYSTEM_OBSERVED`, `BELIEF / PLAYER_TOLD`, or `BELIEF / INFERRED`; `BELIEF / NPC_TOLD` must inherit an existing valid lineage and may never reset ancestry.
- New rumor-capable transfer evidence uses deterministic namespace `npc-knowledge-transfer-v2`.
- `memory2.json` remains format version `1`; no migration, backfill, dual reader, summary parsing, checkpoint ledger, or compatibility bridge is introduced.
- Historical v1 `NPC_TOLD` evidence may remain readable but is not valid for multi-hop continuation; no lineage is reconstructed from it.
- Existing fixed transfer `importance = 50`, `confidence = 50`, `createdAtEpochMillis = 0` remain unchanged.
- Existing MemoryEvent/Semantic retention coefficients, long-horizon quotas (`32` candidates / at most `6` results), ranking weights, provider protocol, config, relationship system, UI, voice, scheduler and gameplay authority remain unchanged.
- No second provider/LLM call.
- Exact retry remains idempotent; persistence and selection use no wall-clock assertion/input for transfer provenance.
- Expected fail-closed outcomes do not create partial BELIEF writes.
- Official installed release remains `0.2.0+1.21.1`; all work in this plan is unreleased source/candidate evidence until a later immutable release is explicitly accepted.
- Runtime behavior follows strict TDD: tests-only RED commit and observed intended failure before the corresponding production behavior.
- Root `CHANGELOG.md` is updated in the runtime PR after GREEN. `docs/PROJECT_STATE.md` / `docs/ROADMAP.md` are reconciled only after product merge in the established docs-only follow-up.

---

## File Structure

### New production files

- `common/src/main/java/net/conczin/mca/livingworld/memory2/KnowledgeTransferProvenance.java`
  - Immutable persisted data model only: `Origin` + ordered `Hop` list.
  - Must tolerate structurally malformed decoded values long enough for the authority policy to reject them; constructors must not silently repair malformed provenance into authority.

- `common/src/main/java/net/conczin/mca/livingworld/memory2/KnowledgeTransferProvenancePolicy.java`
  - Pure canonical/integrity checks: origin validity, path continuity, deterministic evidence IDs, exact statement/scope equality, cycle detection, hop-limit check, exact direct-evidence binding.

- `common/src/main/java/net/conczin/mca/livingworld/memory2/KnowledgeTransferProvenanceFactory.java`
  - Pure construction of canonical first-hop and appended-hop provenance payloads from already authoritative Semantic state.

- `common/src/main/java/net/conczin/mca/livingworld/memory2/KnowledgeTransferProvenanceResolver.java`
  - Store-backed read-only resolver for one canonical retained direct branch of an `NPC_TOLD` speaker BELIEF. It must not accept the proposed listener as input.

### Existing production files to modify

- `common/src/main/java/net/conczin/mca/livingworld/memory2/MemoryEvent.java`
  - Add nullable `KnowledgeTransferProvenance knowledgeTransferProvenance` to the canonical record.
  - Preserve source-compatible convenience constructors by delegating `null` for ordinary events.

- `common/src/main/java/net/conczin/mca/livingworld/memory2/NpcToldDialogueAdapter.java`
  - Switch new transfer identity to v2 and require a canonical non-null provenance payload when constructing new transfer evidence.

- `common/src/main/java/net/conczin/mca/livingworld/memory2/NpcKnowledgeTransferPolicy.java`
  - Extend canonical evidence equality to include exact provenance payload.

- `common/src/main/java/net/conczin/mca/livingworld/memory2/NpcKnowledgeTransferLifecycle.java`
  - Derive first-hop origin or resolve/inherit canonical direct lineage, enforce cycle/limit after branch selection, persist v2 evidence, and preserve existing evidence-before-BELIEF ordering.

- `common/src/main/java/net/conczin/mca/livingworld/memory2/NpcKnowledgeTransferResult.java`
  - Add `PROVENANCE_UNAVAILABLE`, `PROVENANCE_LIMIT_REACHED`, `PROVENANCE_CYCLE`.

- `CHANGELOG.md`
  - Record the new unreleased multi-hop provenance behavior and its explicit non-goals after all behavioral GREEN gates.

### New tests

- `common/src/test/java/net/conczin/mca/livingworld/memory2/KnowledgeTransferProvenanceModelTest.java`
- `common/src/test/java/net/conczin/mca/livingworld/memory2/KnowledgeTransferProvenancePolicyTest.java`
- `common/src/test/java/net/conczin/mca/livingworld/memory2/KnowledgeTransferProvenanceResolverTest.java`
- `common/src/test/java/net/conczin/mca/livingworld/memory2/NpcKnowledgeTransferRumorSimulationTest.java`

### Existing tests to extend

- `common/src/test/java/net/conczin/mca/livingworld/memory2/NpcToldDialogueAdapterTest.java`
- `common/src/test/java/net/conczin/mca/livingworld/memory2/NpcKnowledgeTransferPolicyTest.java`
- `common/src/test/java/net/conczin/mca/livingworld/memory2/NpcKnowledgeTransferLifecycleTest.java`
- `common/src/test/java/net/conczin/mca/livingworld/memory2/NpcKnowledgeTransferPersistenceTest.java`
- `common/src/test/java/net/conczin/mca/livingworld/memory2/NpcKnowledgeTransferPressureTest.java`
- `common/src/test/java/net/conczin/mca/livingworld/memory2/NpcKnowledgeTransferSimulationTest.java`
- `common/src/test/java/net/conczin/mca/livingworld/memory2/Memory2DialogueHistoryTest.java`

### Evidence

- `docs/superpowers/evidence/2026-08-09-provenance-aware-rumors-tdd.md`
  - Exact tests-only RED SHAs, observed CI run IDs/failure reasons, minimal GREEN SHAs, preservation-only GREEN stages, final exact-head delivery matrix, review verdict, and explicit unreleased boundary.

---

### Task 1: Persisted Provenance Data Model

**Files:**
- Create: `common/src/test/java/net/conczin/mca/livingworld/memory2/KnowledgeTransferProvenanceModelTest.java`
- Create after RED: `common/src/main/java/net/conczin/mca/livingworld/memory2/KnowledgeTransferProvenance.java`
- Modify after RED: `common/src/main/java/net/conczin/mca/livingworld/memory2/MemoryEvent.java`

**Interfaces:**
- Produces:

```java
public record KnowledgeTransferProvenance(
        Origin origin,
        List<Hop> hops
) {
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

- `MemoryEvent` canonical record gains:

```java
KnowledgeTransferProvenance knowledgeTransferProvenance
```

- All existing shorter `MemoryEvent` constructors continue to compile and delegate `null` for the new field.

- [ ] **Step 1: Add tests-only model contract**

Create tests that require the new class and accessor while proving an ordinary existing event still has no rumor payload:

```java
@Test
void ordinaryMemoryEventKeepsRumorPayloadAbsent() {
    MemoryEvent event = new MemoryEvent(
            UUID.randomUUID(), UUID.randomUUID(), MemoryEvent.Type.OBSERVATION,
            "observed", List.of(), MemoryEvent.Provenance.SYSTEM_OBSERVED,
            10L, 0L, 50, 0, 100, List.of()
    );
    assertNull(event.knowledgeTransferProvenance());
}

@Test
void provenanceModelRetainsExactOriginAndOrderedHops() {
    KnowledgeTransferProvenance.Origin origin = new KnowledgeTransferProvenance.Origin(
            ORIGIN_NPC, ORIGIN_ENTRY, SemanticMemoryEntry.Kind.FACT,
            MemoryEvent.Provenance.SYSTEM_OBSERVED,
            "Bridge destroyed", List.of(PLAYER)
    );
    KnowledgeTransferProvenance.Hop hop = new KnowledgeTransferProvenance.Hop(
            ORIGIN_NPC, LISTENER, ORIGIN_ENTRY, EVIDENCE, 100L
    );
    KnowledgeTransferProvenance value = new KnowledgeTransferProvenance(origin, List.of(hop));
    assertEquals(origin, value.origin());
    assertEquals(List.of(hop), value.hops());
}
```

- [ ] **Step 2: Observe compile RED before production model exists**

Run:

```bash
./gradlew :common:test --tests 'net.conczin.mca.livingworld.memory2.KnowledgeTransferProvenanceModelTest' --no-daemon
```

Expected: `compileTestJava` fails because `KnowledgeTransferProvenance` and/or `MemoryEvent.knowledgeTransferProvenance()` do not exist. Commit/push this tests-only RED and record the exact CI failure in the evidence ledger before production changes.

- [ ] **Step 3: Implement the minimal persisted model**

Implement the record and add the nullable field to `MemoryEvent`. Do not add authority normalization here. Defensive list wrapping may preserve values but must not sort, deduplicate, drop nulls, or reinterpret malformed persisted ancestry.

Preserve the prior full constructor shape with an overload:

```java
public MemoryEvent(
        UUID id,
        UUID ownerNpcId,
        Type type,
        String summary,
        List<UUID> participants,
        Provenance provenance,
        long gameTime,
        long createdAtEpochMillis,
        int importance,
        int emotionalWeight,
        int confidence,
        List<String> relationshipReasons,
        DialogueExchange dialogue,
        RelationshipTransition relationshipTransition,
        RelationshipCause relationshipCause
) {
    this(id, ownerNpcId, type, summary, participants, provenance, gameTime,
            createdAtEpochMillis, importance, emotionalWeight, confidence,
            relationshipReasons, dialogue, relationshipTransition, relationshipCause, null);
}
```

- [ ] **Step 4: Run focused and common tests**

```bash
./gradlew :common:test --tests 'net.conczin.mca.livingworld.memory2.KnowledgeTransferProvenanceModelTest' --no-daemon
./gradlew :common:test --no-daemon
```

Expected: PASS; existing MemoryEvent call sites remain source-compatible.

- [ ] **Step 5: Commit Task 1 GREEN**

```bash
git add common/src/main/java/net/conczin/mca/livingworld/memory2/KnowledgeTransferProvenance.java \
        common/src/main/java/net/conczin/mca/livingworld/memory2/MemoryEvent.java \
        common/src/test/java/net/conczin/mca/livingworld/memory2/KnowledgeTransferProvenanceModelTest.java
git commit -m 'feat: add transfer provenance data model'
```

---

### Task 2: Pure Canonical Provenance Policy and Factory

**Files:**
- Create: `common/src/test/java/net/conczin/mca/livingworld/memory2/KnowledgeTransferProvenancePolicyTest.java`
- Create after RED: `common/src/main/java/net/conczin/mca/livingworld/memory2/KnowledgeTransferProvenancePolicy.java`
- Create after RED: `common/src/main/java/net/conczin/mca/livingworld/memory2/KnowledgeTransferProvenanceFactory.java`

**Interfaces:**

```java
final class KnowledgeTransferProvenancePolicy {
    static final int MAX_HOPS = 8;

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

- `firstHop` accepts only FACT/SYSTEM_OBSERVED, BELIEF/PLAYER_TOLD, BELIEF/INFERRED.
- `firstHop` rejects BELIEF/NPC_TOLD so ancestry cannot reset.
- `appendHop` requires current source to be BELIEF/NPC_TOLD and requires source statement/scope to match origin.
- Policy validates canonical statement, canonical sorted unique scope, v2 deterministic hop IDs, continuity, uniqueness, direct-source binding and max-eight bound.

- [ ] **Step 1: Write policy/factory tests before implementation**

Include exact cases:

```java
assertTrue(KnowledgeTransferProvenancePolicy.valid(validTwoHopLineage()));
assertFalse(KnowledgeTransferProvenancePolicy.valid(lineageWithRepeatedNpc()));
assertFalse(KnowledgeTransferProvenancePolicy.valid(lineageWithBrokenContinuity()));
assertFalse(KnowledgeTransferProvenancePolicy.valid(lineageWithNpcToldOrigin()));
assertTrue(KnowledgeTransferProvenancePolicy.wouldCycle(validTwoHopLineage(), ORIGIN_NPC));
assertFalse(KnowledgeTransferProvenancePolicy.wouldCycle(validTwoHopLineage(), NEW_NPC));
```

First-hop construction must preserve canonical statement/scope and exact origin identity. Build an eight-hop valid fixture and assert `atHopLimit(...)` becomes true only at eight.

- [ ] **Step 2: Observe compile RED**

```bash
./gradlew :common:test --tests 'net.conczin.mca.livingworld.memory2.KnowledgeTransferProvenancePolicyTest' --no-daemon
```

Expected: compile failure because policy/factory do not exist. Record exact RED SHA/run.

- [ ] **Step 3: Implement minimal pure policy/factory**

Use `SemanticMemoryIngestionAdapter.normalizeAndLimitStatement(...)` for statement equality and one shared UUID comparator:

```java
private static final Comparator<UUID> UUID_ORDER = Comparator.comparing(UUID::toString);
```

Canonical scope validation must compare the persisted list to `canonicalIds(list)`, not silently canonicalize malformed persistence into validity.

Path uniqueness must evaluate the ordered NPC path (`first speaker`, then every listener). `wouldCycle` returns true when the proposed listener is already anywhere in that path.

- [ ] **Step 4: Verify focused GREEN and common regressions**

```bash
./gradlew :common:test --tests 'net.conczin.mca.livingworld.memory2.KnowledgeTransferProvenancePolicyTest' --no-daemon
./gradlew :common:test --no-daemon
```

- [ ] **Step 5: Commit Task 2 GREEN**

```bash
git add common/src/main/java/net/conczin/mca/livingworld/memory2/KnowledgeTransferProvenancePolicy.java \
        common/src/main/java/net/conczin/mca/livingworld/memory2/KnowledgeTransferProvenanceFactory.java \
        common/src/test/java/net/conczin/mca/livingworld/memory2/KnowledgeTransferProvenancePolicyTest.java
git commit -m 'feat: validate bounded rumor provenance'
```

---

### Task 3: v2 Canonical Transfer Evidence and First-Hop Lifecycle

**Files:**
- Modify tests first: `common/src/test/java/net/conczin/mca/livingworld/memory2/NpcToldDialogueAdapterTest.java`
- Modify tests first: `common/src/test/java/net/conczin/mca/livingworld/memory2/NpcKnowledgeTransferLifecycleTest.java`
- Modify after RED: `common/src/main/java/net/conczin/mca/livingworld/memory2/NpcToldDialogueAdapter.java`
- Modify after RED: `common/src/main/java/net/conczin/mca/livingworld/memory2/NpcKnowledgeTransferPolicy.java`
- Modify after RED: `common/src/main/java/net/conczin/mca/livingworld/memory2/NpcKnowledgeTransferLifecycle.java`
- Modify after RED: `common/src/main/java/net/conczin/mca/livingworld/memory2/NpcKnowledgeTransferResult.java`

**Interfaces:**

Final adapter construction surface:

```java
static Optional<MemoryEvent> create(
        UUID speakerNpcId,
        UUID listenerNpcId,
        UUID speakerSemanticEntryId,
        long authoritativeGameTime,
        String statement,
        KnowledgeTransferProvenance provenance
);

static UUID deterministicEvidenceId(
        UUID speakerNpcId,
        UUID listenerNpcId,
        UUID speakerSemanticEntryId,
        long authoritativeGameTime
);
```

`deterministicEvidenceId` namespace becomes exactly `npc-knowledge-transfer-v2`.

`NpcKnowledgeTransferPolicy.validEvidence(...)` gains the expected provenance payload and reconstructs the entire canonical event including that payload before equality comparison.

Result statuses become:

```java
ADMITTED,
REJECTED,
SOURCE_NOT_RETAINED,
BELIEF_NOT_RETAINED,
PROVENANCE_UNAVAILABLE,
PROVENANCE_LIMIT_REACHED,
PROVENANCE_CYCLE
```

- [ ] **Step 1: Add tests-only v2/first-hop requirements**

Adapter test computes expected ID manually from `npc-knowledge-transfer-v2` and asserts the payload is non-null and exactly equal to the supplied canonical first-hop lineage.

Lifecycle tests cover all allowed first-hop origin classes:

```text
FACT / SYSTEM_OBSERVED
BELIEF / PLAYER_TOLD
BELIEF / INFERRED
```

For each, assert:

```java
assertEquals(SemanticMemoryEntry.Kind.BELIEF, transferred.kind());
assertEquals(MemoryEvent.Provenance.NPC_TOLD, transferred.provenance());
assertEquals(List.of(result.evidenceEventId()), transferred.sourceEventIds());
assertEquals(source.relatedEntities(), transferred.relatedEntities());
assertEquals(1, evidence.knowledgeTransferProvenance().hops().size());
```

Origin must record exact source `id/kind/provenance`, canonical statement and canonical scope. The listener must not inherit the upstream source-event list.

- [ ] **Step 2: Observe behavioral RED on current v1 evidence**

Run:

```bash
./gradlew :common:test \
  --tests 'net.conczin.mca.livingworld.memory2.NpcToldDialogueAdapterTest' \
  --tests 'net.conczin.mca.livingworld.memory2.NpcKnowledgeTransferLifecycleTest' \
  --no-daemon
```

Expected: tests compile after Task 1/2 but fail because current evidence uses v1 identity and has no structured provenance / first-hop origin snapshot. Record exact failure count and assertions.

- [ ] **Step 3: Implement minimal v2 first-hop GREEN**

Lifecycle order for non-NPC_TOLD sources:

```text
exact source lookup
→ authoritative reread/snapshot equality
→ compute v2 evidence UUID
→ KnowledgeTransferProvenanceFactory.firstHop(...)
→ canonical v2 evidence construction
→ append/reread/exact equality including provenance
→ existing SemanticBeliefAdmissionPolicy
→ existing ControlledSemanticMemoryIngestor
→ retained compatible BELIEF containing direct evidence UUID
```

For `BELIEF / NPC_TOLD` at this stage, return `PROVENANCE_UNAVAILABLE`; do not fabricate a first-hop origin. Multi-hop success belongs to Task 4.

- [ ] **Step 4: Run first-hop and full common GREEN**

```bash
./gradlew :common:test \
  --tests 'net.conczin.mca.livingworld.memory2.NpcToldDialogueAdapterTest' \
  --tests 'net.conczin.mca.livingworld.memory2.NpcKnowledgeTransferLifecycleTest' \
  --tests 'net.conczin.mca.livingworld.memory2.NpcKnowledgeTransferPolicyTest' \
  --no-daemon
./gradlew :common:test --no-daemon
```

- [ ] **Step 5: Commit Task 3 GREEN**

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

### Task 4: Exact Multi-Hop Resolver and A→B→C Continuation

**Files:**
- Create tests first: `common/src/test/java/net/conczin/mca/livingworld/memory2/KnowledgeTransferProvenanceResolverTest.java`
- Extend tests first: `common/src/test/java/net/conczin/mca/livingworld/memory2/NpcKnowledgeTransferLifecycleTest.java`
- Create after RED: `common/src/main/java/net/conczin/mca/livingworld/memory2/KnowledgeTransferProvenanceResolver.java`
- Modify after RED: `common/src/main/java/net/conczin/mca/livingworld/memory2/NpcKnowledgeTransferLifecycle.java`

**Interfaces:**

```java
final class KnowledgeTransferProvenanceResolver {
    static Optional<ResolvedSource> resolve(
            MemoryEventStore eventStore,
            SemanticMemoryEntry speakerSource
    );

    record ResolvedSource(
            MemoryEvent evidence,
            KnowledgeTransferProvenance provenance
    ) {}
}
```

Important: `resolve` has no listener argument. It may read only direct event IDs contained in `speakerSource.sourceEventIds()` and must require owner = `speakerSource.ownerNpcId()`.

- [ ] **Step 1: Add tests-only simple multi-hop behavioral RED**

Build:

```text
A FACT → B
B's exact retained NPC_TOLD BELIEF → C
```

Assert current code returns `PROVENANCE_UNAVAILABLE` for B→C, then specify desired result:

```java
assertEquals(NpcKnowledgeTransferResult.Status.ADMITTED, bc.status());
KnowledgeTransferProvenance lineage = evidenceC.knowledgeTransferProvenance();
assertEquals(2, lineage.hops().size());
assertEquals(A, lineage.origin().originNpcId());
assertEquals(B, lineage.hops().get(1).speakerNpcId());
assertEquals(C, lineage.hops().get(1).listenerNpcId());
assertEquals(List.of(bc.evidenceEventId()), cBelief.sourceEventIds());
```

- [ ] **Step 2: Observe behavioral RED**

```bash
./gradlew :common:test \
  --tests 'net.conczin.mca.livingworld.memory2.KnowledgeTransferProvenanceResolverTest' \
  --tests 'net.conczin.mca.livingworld.memory2.NpcKnowledgeTransferLifecycleTest' \
  --no-daemon
```

Expected: resolver compile RED if introduced first, followed by behavioral RED for B→C until lifecycle wiring exists. Keep compile and behavioral RED evidence distinct if they occur as separate commits.

- [ ] **Step 3: Implement deterministic direct-source resolver**

For each `sourceEventId`:

```java
MemoryEventStore.findById(speakerSource.ownerNpcId(), sourceEventId)
```

Filter only events for which:

```text
canonical DIALOGUE/NPC_TOLD v2 shape is valid
knowledgeTransferProvenance is valid
last hop evidence ID == event.id
last hop listener == current speaker
origin statement == current speaker Semantic normalized statement
origin scope == current speaker canonical relatedEntities
```

Sort valid retained candidates:

```java
Comparator.comparingLong(MemoryEvent::gameTime).reversed()
        .thenComparing(event -> event.id().toString())
```

Return first only.

- [ ] **Step 4: Wire `NPC_TOLD` continuation into lifecycle**

For an `NPC_TOLD` source:

```text
resolve canonical branch
→ no branch: PROVENANCE_UNAVAILABLE
→ selected branch: evaluate cycle against proposed listener
→ if cycle: PROVENANCE_CYCLE
→ else if 8 hops: PROVENANCE_LIMIT_REACHED
→ compute new v2 evidence ID
→ append canonical new hop via factory
→ persist/reread/validate evidence
→ admit direct listener BELIEF
```

Do not pass listener into resolver, and do not re-run resolver after cycle/limit rejection.

- [ ] **Step 5: Run focused and common GREEN**

```bash
./gradlew :common:test \
  --tests 'net.conczin.mca.livingworld.memory2.KnowledgeTransferProvenanceResolverTest' \
  --tests 'net.conczin.mca.livingworld.memory2.NpcKnowledgeTransferLifecycleTest' \
  --no-daemon
./gradlew :common:test --no-daemon
```

- [ ] **Step 6: Commit Task 4 GREEN**

```bash
git add common/src/main/java/net/conczin/mca/livingworld/memory2/KnowledgeTransferProvenanceResolver.java \
        common/src/main/java/net/conczin/mca/livingworld/memory2/NpcKnowledgeTransferLifecycle.java \
        common/src/test/java/net/conczin/mca/livingworld/memory2/KnowledgeTransferProvenanceResolverTest.java \
        common/src/test/java/net/conczin/mca/livingworld/memory2/NpcKnowledgeTransferLifecycleTest.java
git commit -m 'feat: propagate exact rumor lineage'
```

---

### Task 5: Consolidation Branch Selection, No-Fallback, Cycle and Hop-Limit Matrix

**Files:**
- Extend tests first: `common/src/test/java/net/conczin/mca/livingworld/memory2/KnowledgeTransferProvenanceResolverTest.java`
- Extend tests first: `common/src/test/java/net/conczin/mca/livingworld/memory2/NpcKnowledgeTransferLifecycleTest.java`
- Modify production only if RED proves required: resolver/policy/factory/lifecycle files from Tasks 2–4.

**Interfaces:** No new public/runtime interface is expected. This is a preservation/hardening task around the contracts already produced.

- [ ] **Step 1: Add canonical multi-source branch-selection tests**

Create a consolidated B BELIEF referencing three direct v2 sources. Build them with controlled game times so expected branch ordering is unambiguous.

Required assertions:

```text
newest retained valid gameTime wins
same gameTime → lexicographically smaller evidence UUID wins
sourceEventIds insertion order does not matter
unreferenced retained evidence is ignored
evicted highest-priority source falls through to next retained valid source
malformed highest-priority source is skipped before canonical selection
```

- [ ] **Step 2: Add listener-independent no-fallback regression**

Create two valid retained branches where the canonical highest-priority branch would cycle with the proposed listener but the lower branch would not.

Expected:

```java
assertEquals(PROVENANCE_CYCLE, result.status());
```

and no new evidence/BELIEF. This proves listener-specific rejection cannot bias ancestry selection.

- [ ] **Step 3: Add cycle matrix**

Required cases:

```text
A → A
A → B → A
A → B → C → A
A → B → C → B
```

All return `PROVENANCE_CYCLE`. First-hop `A→A` is handled by request-level cycle validation and should use the same status rather than generic `REJECTED`.

- [ ] **Step 4: Add hop-limit matrix**

Construct exactly eight successful hops; assert hop 8 is admitted and attempt 9 returns `PROVENANCE_LIMIT_REACHED`. If the ninth listener would also cause a cycle, assert `PROVENANCE_CYCLE` wins because cycle is checked first.

- [ ] **Step 5: Run tests and observe whether production changes are needed**

```bash
./gradlew :common:test \
  --tests 'net.conczin.mca.livingworld.memory2.KnowledgeTransferProvenanceResolverTest' \
  --tests 'net.conczin.mca.livingworld.memory2.NpcKnowledgeTransferLifecycleTest' \
  --no-daemon
```

If all new assertions pass, commit tests-only as preservation GREEN. If any intended contract fails, record that behavioral RED first, then make only the minimal corresponding production correction and rerun.

- [ ] **Step 6: Run full common suite and commit**

```bash
./gradlew :common:test --no-daemon
```

Commit message:

```bash
git commit -m 'test: harden rumor branch and cycle policy'
```

---

### Task 6: Fail-Closed Provenance Integrity Mutation Matrix

**Files:**
- Extend tests first: `common/src/test/java/net/conczin/mca/livingworld/memory2/NpcKnowledgeTransferPolicyTest.java`
- Extend tests first: `common/src/test/java/net/conczin/mca/livingworld/memory2/KnowledgeTransferProvenancePolicyTest.java`
- Extend tests first: `common/src/test/java/net/conczin/mca/livingworld/memory2/KnowledgeTransferProvenanceResolverTest.java`
- Modify production only after an observed RED.

- [ ] **Step 1: Extend canonical evidence copy/mutation helper to carry provenance**

Update the test-only `copy(...)` helper to accept the final field:

```java
KnowledgeTransferProvenance knowledgeTransferProvenance
```

and construct the canonical 16-field `MemoryEvent`.

- [ ] **Step 2: Add one-field-at-a-time provenance mutation tests**

Mutate:

```text
origin NPC
origin Semantic-entry UUID
origin kind
origin provenance
origin statement
origin scope
hop speaker
hop listener
hop speaker Semantic-entry UUID
hop evidence UUID
hop gameTime
hop order
missing hop
extra hop
```

For each, assert `KnowledgeTransferProvenancePolicy.valid(...) == false` or canonical current evidence validation fails as appropriate.

- [ ] **Step 3: Add `PROVENANCE_UNAVAILABLE` source matrix**

For an `NPC_TOLD` Semantic source, assert no new writes when:

```text
no retained direct source evidence
historical v1 direct evidence without payload
malformed payload
origin BELIEF/NPC_TOLD reset payload
wrong event owner
direct event not referenced by Semantic source
last hop does not end at current speaker
statement mismatch
scope mismatch
```

When no valid candidate remains, result is exactly `PROVENANCE_UNAVAILABLE`, not a reconstructed/reset lineage.

- [ ] **Step 4: Run focused suite and observe RED if present**

```bash
./gradlew :common:test \
  --tests 'net.conczin.mca.livingworld.memory2.KnowledgeTransferProvenancePolicyTest' \
  --tests 'net.conczin.mca.livingworld.memory2.KnowledgeTransferProvenanceResolverTest' \
  --tests 'net.conczin.mca.livingworld.memory2.NpcKnowledgeTransferPolicyTest' \
  --no-daemon
```

- [ ] **Step 5: Apply only fail-closed corrections required by observed failures**

Do not repair/sort malformed persisted payload into authority. Runtime canonical construction may create sorted unique scope; validation of persisted payload must require it already equals canonical form.

- [ ] **Step 6: Full common GREEN and commit**

```bash
./gradlew :common:test --no-daemon
git commit -m 'test: enforce rumor provenance integrity'
```

---

### Task 7: Restart, Exact Replay, Scope/Privacy and Working-Memory Isolation

**Files:**
- Extend tests first: `common/src/test/java/net/conczin/mca/livingworld/memory2/NpcKnowledgeTransferPersistenceTest.java`
- Extend tests first: `common/src/test/java/net/conczin/mca/livingworld/memory2/Memory2DialogueHistoryTest.java`
- Modify production only if an observed preservation regression requires it.

- [ ] **Step 1: Add fresh-root multi-hop persistence round-trip**

Create A→B→C, copy both `memory2.json` and `semantic-memory.json` to a distinct fresh world root, reload stores, and assert exact equality of:

```text
C direct evidence UUID
C KnowledgeTransferProvenance origin
ordered hops
every hop evidence ID / source Semantic ID / gameTime
C Semantic direct source IDs
```

Then exact-replay B→C on the fresh root with the same gameTime and assert files/IDs remain idempotent.

- [ ] **Step 2: Extend global/private/shared scope cases through at least two hops**

For private `[playerA]` and shared `[playerA, entityX]` origin scope, assert every downstream Semantic BELIEF retains the exact canonical set, excludes provenance speakers, is visible to playerA, and remains absent for playerB.

- [ ] **Step 3: Prove raw NPC→NPC rumor evidence stays out of player dialogue history**

Persist a multi-hop rumor and assert:

```java
assertEquals(List.of(), Memory2DialogueHistory.load(world, listener, player));
```

Then add a genuine structured player DIALOGUE and assert it still reconstructs normally beside the rumor evidence.

- [ ] **Step 4: Run focused preservation tests**

```bash
./gradlew :common:test \
  --tests 'net.conczin.mca.livingworld.memory2.NpcKnowledgeTransferPersistenceTest' \
  --tests 'net.conczin.mca.livingworld.memory2.Memory2DialogueHistoryTest' \
  --no-daemon
```

If they pass with no production change, commit the tests-only preservation gate. Otherwise record the intended RED before minimal correction.

- [ ] **Step 5: Full common GREEN and commit**

```bash
./gradlew :common:test --no-daemon
git commit -m 'test: preserve rumor lineage across restart and privacy scopes'
```

---

### Task 8: Pressure, Direct-Evidence Loss and Bounded Forgetting

**Files:**
- Extend tests first: `common/src/test/java/net/conczin/mca/livingworld/memory2/NpcKnowledgeTransferPressureTest.java`
- Extend tests first: `common/src/test/java/net/conczin/mca/livingworld/memory2/NpcKnowledgeTransferSimulationTest.java`
- Modify production only after observed RED.

- [ ] **Step 1: Preserve existing partial-retention statuses with v2 payload**

Re-run and adapt existing `SOURCE_NOT_RETAINED` and `BELIEF_NOT_RETAINED` fixtures to assert provenance-bearing evidence obeys the same retention coefficients and is not pinned.

- [ ] **Step 2: Prove ancestry survives loss of older physical events**

Construct A→B→C. Evict A→B from B's event store only after C's direct evidence exists. Assert C's retained direct evidence still contains exact `[A→B, B→C]` snapshot and C can personally recall the BELIEF.

- [ ] **Step 3: Prove current direct evidence loss blocks further propagation**

Keep C Semantic BELIEF retained but evict its B→C direct evidence. Attempt C→D.

Expected:

```java
assertEquals(NpcKnowledgeTransferResult.Status.PROVENANCE_UNAVAILABLE, result.status());
assertTrue(MemoryEventStore.forWorld(world).getRecent(D, 64).isEmpty());
assertTrue(SemanticMemoryStore.forWorld(world).getRecent(D, 64).isEmpty());
```

- [ ] **Step 4: Prove rumor BELIEF and evidence remain evictable**

Use stronger existing SYSTEM_OBSERVED facts/events at capacity 1 and assert the rumor entry/evidence can both be displaced under unchanged retention policies.

- [ ] **Step 5: Run focused/full tests and commit**

```bash
./gradlew :common:test \
  --tests 'net.conczin.mca.livingworld.memory2.NpcKnowledgeTransferPressureTest' \
  --tests 'net.conczin.mca.livingworld.memory2.NpcKnowledgeTransferSimulationTest' \
  --no-daemon
./gradlew :common:test --no-daemon
git commit -m 'test: verify bounded rumor retention and forgetting'
```

---

### Task 9: Deterministic 10+ NPC Rumor Simulation and Truth-Preservation Regression

**Files:**
- Create: `common/src/test/java/net/conczin/mca/livingworld/memory2/NpcKnowledgeTransferRumorSimulationTest.java`
- Extend only if needed: `common/src/test/java/net/conczin/mca/livingworld/memory2/NpcKnowledgeTransferSimulationTest.java`
- No production change expected; any behavioral failure must be recorded as RED before correction.

- [ ] **Step 1: Build deterministic simulation fixture**

Use fixed UUIDs and authoritative game times only. Minimum data set:

```text
>= 10 NPCs
one valid 8-hop chain
rejected ninth hop
at least two independent lineages that corroborate one final listener BELIEF
multiple cycle attempts
> 200 unrelated Semantic records
> 200 unrelated episodic/social records
private + shared + NPC-global scopes
forward pressure insertion order
reverse pressure insertion order
at least two fresh-root reloads
```

No `Thread.sleep`, `System.currentTimeMillis`, random UUID generation, or wall-clock expected values.

- [ ] **Step 2: Compare exact deterministic snapshots**

Scenario result should contain at least:

```java
record ScenarioSnapshot(
        Map<UUID, List<UUID>> semanticIdsByNpc,
        Map<UUID, List<UUID>> eventIdsByNpc,
        Map<UUID, List<KnowledgeTransferProvenance>> provenanceByNpc,
        Map<UUID, List<String>> promptContextByNpc
) {}
```

Assert forward == reverse and fresh reload snapshots are equal.

- [ ] **Step 3: Assert Semantic direct-source / provenance separation**

For a corroborated final BELIEF, require multiple direct `sourceEventIds` only. For each direct source event, inspect its independent lineage; do not allow ancestry IDs to appear as Semantic direct sources unless they are independently direct evidence for that listener.

- [ ] **Step 4: Assert truth/authority preservation**

At an 8-hop listener, assert prompt lines still render:

```text
BELIEF | provenance=NPC_TOLD
```

and `SemanticMemoryContextFormatter.promptSection(...)` still contains:

```text
Current observed factual context wins on conflict.
Confidence never converts a BELIEF into a FACT.
```

Seed a conflicting current/system-observed FACT in the same scenario and assert no rumor entry becomes `FACT` and no origin FACT metadata alters downstream `kind/provenance`.

- [ ] **Step 5: Assert bounded retrieval/privacy**

Every context remains `<= SemanticMemoryContextProvider.MAX_RESULTS`; foreign-player private rumor remains absent before candidate allocation; global rumor remains eligible subject to existing ranking/limits.

- [ ] **Step 6: Run simulation and full common suite**

```bash
./gradlew :common:test \
  --tests 'net.conczin.mca.livingworld.memory2.NpcKnowledgeTransferRumorSimulationTest' \
  --no-daemon
./gradlew :common:test --no-daemon
```

If the simulation reveals an implementation defect, preserve the failing test as observed RED, apply the smallest correction, and rerun before commit.

- [ ] **Step 7: Commit deterministic simulation gate**

```bash
git add common/src/test/java/net/conczin/mca/livingworld/memory2/NpcKnowledgeTransferRumorSimulationTest.java \
        common/src/test/java/net/conczin/mca/livingworld/memory2/NpcKnowledgeTransferSimulationTest.java
git commit -m 'test: simulate bounded multi-hop rumors'
```

---

### Task 10: Changelog, TDD Evidence, Exact-Head Review and Delivery Gates

**Files:**
- Modify: `CHANGELOG.md`
- Create: `docs/superpowers/evidence/2026-08-09-provenance-aware-rumors-tdd.md`
- No `PROJECT_STATE.md` / `ROADMAP.md` update in the runtime PR; reconcile them after merge in a docs-only PR.

**Interfaces:** No runtime API additions. This task closes the delivery boundary only.

- [ ] **Step 1: Update root `[Unreleased]`**

Record exactly that:

```text
NPC_TOLD transfer evidence now carries bounded structured multi-hop provenance
new transfer identity uses npc-knowledge-transfer-v2
one lineage is selected deterministically per transfer
lineages are acyclic and capped at 8 hops
Semantic entries keep direct evidence only
statement/scope remain exact
missing/corrupt direct provenance fails closed
no uncertainty/distortion/trust model is added
no config/new store/migration is added
```

Do not describe the feature as installed `0.2.0` acceptance.

- [ ] **Step 2: Build canonical TDD evidence ledger**

The evidence document must enumerate, for every runtime task:

```text
tests-only RED commit SHA
exact CI workflow/run number
exact expected failure class/count
minimal GREEN production SHA
focused/full common result
whether a later preservation stage required production changes
```

Also list the approved spec and plan SHAs.

- [ ] **Step 3: Open/update a draft runtime PR**

PR summary must state the authority contract, v2 clean cutover, no-migration boundary, new statuses, tests, and official installed-release boundary.

- [ ] **Step 4: Run the exact final-head mandatory matrix**

For the exact final runtime/evidence head require fresh observed results for:

```text
Repository security policy
VillAIgence CI
VillAIgence Production Soak
VillAIgence GitHub Release dry-run
```

Main CI must actually cover, when selected:

```text
common + deterministic mock-provider
risk-selected server GameTests
Fabric + NeoForge builds
production acceptance contracts
exact staged startup/restart
selected persistence recovery
package verification
```

Soak must cover constrained-heap concurrency/staging and restart cycles. Release dry-run must cover exact production acceptance/recovery/GameTests/package identity and its publication job must be `SKIPPED`; do not publish a release from this feature PR.

- [ ] **Step 5: Perform independent base→head review**

Review the complete production diff, not just tests. Explicitly check:

```text
provider/client cannot inject lineage
branch resolver accepts no listener
NPC_TOLD cannot reset origin
cycle/limit order is exact
no fallback after listener-specific rejection
no sourceEventIds ancestry pollution
no retention/ranker/config changes
no unbounded collection growth
no summary parsing authority
no migration/backfill/store-version change
```

Record P0/P1/P2 counts and unresolved review threads. Any finding is resolved with its own RED→GREEN when behavioral.

- [ ] **Step 6: Fresh exact-head verification before merge**

After the final evidence/changelog commit, rerun/fetch all mandatory workflows for that exact head. Do not rely only on a previously green runtime head.

- [ ] **Step 7: Merge only exact verified head**

Use squash merge with expected head SHA. Do not publish a release.

- [ ] **Step 8: Reconcile canonical state docs after product merge**

Create a separate docs branch from the merged `1.21.1` head. Update exactly:

```text
docs/PROJECT_STATE.md
docs/ROADMAP.md
```

Mark exact multi-hop provenance-aware rumors complete and advance the next slice to uncertainty/contradiction/bounded-distortion design. Preserve `0.2.0+1.21.1` as the installed-release boundary. Run docs-only CI/security, verify heavy runtime stages are correctly skipped by classification, then squash-merge the docs PR.

---

## Plan Self-Review Checklist

Before execution, verify all of the following against the approved spec:

- [ ] Structured origin + hop model is persisted only in transfer evidence.
- [ ] Semantic `sourceEventIds` remain direct-only.
- [ ] v2 namespace is explicit and no v1 lineage reconstruction exists.
- [ ] FACT/PLAYER_TOLD/INFERRED first-hop origins are covered.
- [ ] NPC_TOLD origin reset is forbidden.
- [ ] A→B→C continuation has a distinct behavioral RED.
- [ ] Branch selection is deterministic and listener-independent.
- [ ] No fallback after cycle/limit rejection is tested.
- [ ] Cycle and eight-hop limit matrices are covered with exact status precedence.
- [ ] Every integrity mutation required by the spec is mapped to Task 6.
- [ ] Statement and scope preservation are covered across restart and deep chains.
- [ ] Direct evidence loss versus older ancestry loss are distinguished under pressure.
- [ ] Working Memory isolation and foreign-player privacy are preserved.
- [ ] Deterministic simulation uses >=10 NPCs, >200 records in both memory domains, reverse order and multiple reloads.
- [ ] Current observed truth remains authoritative and downstream rumor never becomes FACT.
- [ ] No provider/config/retention/ranker/new-store/migration scope creep exists.
- [ ] Root changelog, TDD ledger, exact-head CI/security/soak/release dry-run and independent review are explicit.
- [ ] Post-merge state/roadmap reconciliation remains a separate docs-only boundary.
