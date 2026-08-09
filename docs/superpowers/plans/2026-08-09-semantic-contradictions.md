# Semantic Contradiction Representation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add deterministic, bounded, server-owned contradiction process evidence between two retained Semantic Memory claims without changing claim truth class, confidence, ranking or prompt authority.

**Architecture:** Reuse `memory2.json` with a structured `SEMANTIC_CONTRADICTION` event. Extract shared Semantic logical identity from the existing consolidation key so contradiction references survive source-union consolidation. Resolve contradiction history only while both logical claims remain retained and player-eligible; explicitly exclude contradiction events from generic episodic prompt retrieval until dedicated prompt semantics are designed.

**Tech Stack:** Java 21, JUnit 5, Gson-backed Memory 2.0 stores, Gradle, Fabric/NeoForge CI.

## Global Constraints

- `memory2.json` format version remains `1`.
- `semantic-memory.json` format version remains `1`.
- No new store, migration/backfill, config, provider schema/call, UI, scheduler or autonomous propagation.
- FACT/BELIEF/provenance/confidence/ranking remain unchanged by contradiction recording.
- Contradiction evidence duplicates no claim text and never enters generic episodic prompt context.
- Existing `32` candidate / `24+8` / `6` Semantic prompt bounds remain unchanged.
- Existing rumor provenance remains acyclic and capped at 8 hops.
- Runtime behavior follows RED → minimal GREEN → focused regression → complete selected gates.

---

### Task 1: Shared stable Semantic logical identity

**Files:**
- Create test: `common/src/test/java/net/conczin/mca/livingworld/memory2/SemanticMemoryIdentityTest.java`
- Create after RED: `common/src/main/java/net/conczin/mca/livingworld/memory2/SemanticMemoryIdentity.java`
- Modify after RED: `common/src/main/java/net/conczin/mca/livingworld/memory2/SemanticMemoryConsolidator.java`

**Interfaces:**

```java
final class SemanticMemoryIdentity {
    static LogicalClaimKey key(SemanticMemoryEntry entry);
    static UUID logicalClaimId(SemanticMemoryEntry entry);
    static String canonicalStatement(String value);
    static List<UUID> canonicalIds(List<UUID> values);

    record LogicalClaimKey(
        UUID ownerNpcId,
        SemanticMemoryEntry.Kind kind,
        MemoryEvent.Provenance provenance,
        String statement,
        List<UUID> relatedEntities
    ) {}
}
```

- [ ] Write tests proving source-event changes do not change `logicalClaimId`, case/NFKC/whitespace and scope ordering match existing consolidation semantics, and kind/provenance do change identity.
- [ ] Add a preservation assertion that existing `SemanticMemoryConsolidator.merge(...)` output ID remains identical to the pre-refactor `semantic-consolidated-v1` canonical formula.
- [ ] Run focused test and observe compile RED for missing identity API.
- [ ] Implement `SemanticMemoryIdentity`; refactor consolidator to delegate canonical statement/scope/key logic without changing behavior.
- [ ] Run identity + consolidator tests and full `:common:test` GREEN.
- [ ] Commit: `refactor: expose stable semantic claim identity`.

---

### Task 2: Structured contradiction payload in MemoryEvent

**Files:**
- Create test: `common/src/test/java/net/conczin/mca/livingworld/memory2/SemanticContradictionModelTest.java`
- Create after RED: `common/src/main/java/net/conczin/mca/livingworld/memory2/SemanticContradiction.java`
- Modify after RED: `MemoryEvent.java`, `MemoryEventRetentionPolicy.java`, `Memory2ContextProvider.java`

**Interfaces:**

```java
public record SemanticContradiction(ClaimSnapshot first, ClaimSnapshot second) {
    public record ClaimSnapshot(
        UUID logicalClaimId,
        UUID detectedSemanticEntryId,
        SemanticMemoryEntry.Kind kind,
        MemoryEvent.Provenance provenance,
        List<UUID> relatedEntities
    ) {}
}
```

Claim text is deliberately absent from this payload; live text is resolved from currently retained Semantic entries by `logicalClaimId`.

Add `MemoryEvent.Type.SEMANTIC_CONTRADICTION` and final optional `semanticContradiction` record field. Preserve all historical constructor call sites with source-compatible overloads that default the new field to null.

- [ ] RED tests require immutable normalized snapshot lists, canonical first/second ordering, rejection of null/same logical claim snapshots, and historical constructors producing null contradiction payload.
- [ ] RED retention test requires the new event type contribution to equal `OBSERVATION`/`ACTION`.
- [ ] RED prompt-isolation test requires `Memory2ContextProvider.load(...)` to omit retained contradiction events so they cannot render as generic `VERIFIED` episodic lines.
- [ ] Implement the minimal record, MemoryEvent field/constructors, retention switch branch and explicit generic-context exclusion.
- [ ] Assert `SemanticMemoryIngestionAdapter.toFact(contradictionEvent)` is empty.
- [ ] Run focused/full common GREEN.
- [ ] Commit: `feat: add structured semantic contradiction evidence`.

---

### Task 3: Canonical contradiction factory and integrity policy

**Files:**
- Create tests: `SemanticContradictionPolicyTest.java`, `SemanticContradictionAdapterTest.java`
- Create after RED: `SemanticContradictionPolicy.java`, `SemanticContradictionAdapter.java`

**Interfaces:**

```java
final class SemanticContradictionPolicy {
    static UUID deterministicEventId(UUID ownerNpcId, UUID firstClaimId, UUID secondClaimId, long gameTime);
    static boolean valid(MemoryEvent event);
}

final class SemanticContradictionAdapter {
    static Optional<MemoryEvent> create(
        SemanticMemoryEntry first,
        SemanticMemoryEntry second,
        long authoritativeGameTime
    );
}
```

- [ ] RED: A/B and B/A produce the same ordered payload and same event ID at same game time.
- [ ] RED: same concrete entry, same logical content, owner mismatch and scope mismatch reject.
- [ ] RED: FACT/BELIEF and BELIEF/BELIEF pairs preserve original kind/provenance without mutation.
- [ ] RED: mutate owner/type/provenance/gameTime/summary/snapshot claim ID/entry ID/kind/provenance/scope and prove `valid` fails.
- [ ] Implement canonical ID namespace `semantic-contradiction-v1`, exact canonical event shape and full fail-closed validation.
- [ ] Run focused/full common GREEN.
- [ ] Commit: `feat: construct canonical semantic contradictions`.

---

### Task 4: Exact-ID lifecycle with retention outcomes

**Files:**
- Create tests: `SemanticContradictionLifecycleTest.java`
- Create after RED: `SemanticContradictionLifecycle.java`, `SemanticContradictionResult.java`

**Interfaces:**

```java
public record SemanticContradictionResult(Status status, UUID eventId) {
    public enum Status {
        RECORDED,
        REJECTED,
        SOURCE_NOT_RETAINED,
        SCOPE_MISMATCH,
        SAME_CLAIM,
        EVENT_NOT_RETAINED
    }
}

public final class SemanticContradictionLifecycle {
    public static SemanticContradictionResult record(
        Path worldRoot,
        UUID npcId,
        UUID firstSemanticEntryId,
        UUID secondSemanticEntryId,
        long authoritativeGameTime,
        int maxEventsPerNpc
    );
}
```

- [ ] RED: valid pair expected `RECORDED`; missing sources expected `SOURCE_NOT_RETAINED`; wrong owner/null inputs `REJECTED`; same logical claim `SAME_CLAIM`; scope mismatch `SCOPE_MISMATCH`.
- [ ] RED: exact replay same tuple/time returns same event ID and byte-identical `memory2.json`.
- [ ] RED: later game time returns distinct event ID.
- [ ] RED: capacity pressure may produce `EVENT_NOT_RETAINED`, with both Semantic entries exactly unchanged.
- [ ] Implement exact Semantic `findById`, canonical adapter, append, exact event reread + `valid`, and post-retention status only.
- [ ] Run focused/full common GREEN.
- [ ] Commit: `feat: record source-backed semantic contradictions`.

---

### Task 5: Resolved contradiction history without memory resurrection

**Files:**
- Create tests: `SemanticContradictionHistoryTest.java`
- Create after RED: `SemanticContradictionHistory.java`

**Interfaces:**

```java
public final class SemanticContradictionHistory {
    public static List<ResolvedSemanticContradiction> load(
        Path worldRoot,
        UUID npcId,
        UUID playerId,
        int maxResults
    );

    public record ResolvedSemanticContradiction(
        MemoryEvent evidence,
        SemanticMemoryEntry first,
        SemanticMemoryEntry second
    ) {}
}
```

- [ ] RED: relation resolves while both logical claims remain retained.
- [ ] RED: source-union consolidation changing a concrete entry ID still resolves through `logicalClaimId`.
- [ ] RED: removing either live logical claim under Semantic pressure hides the relation even if contradiction evidence remains.
- [ ] RED: resolved kind/provenance/scope must match the stored snapshots; malformed evidence fails closed.
- [ ] RED: global/private/shared scope eligibility matches current player rules; foreign-player relations are filtered before limiting.
- [ ] RED: newest-first by event gameTime, then event UUID ascending, with `maxResults` applied after resolution/eligibility.
- [ ] Implement exact event filtering and current Semantic claim lookup by stable logical identity. Do not parse summaries and do not read claim text from contradiction evidence.
- [ ] Run focused/full common GREEN.
- [ ] Commit: `feat: resolve live semantic contradiction history`.

---

### Task 6: Restart, pressure and truth-preservation simulation

**Files:**
- Create tests: `SemanticContradictionPersistenceTest.java`, `SemanticContradictionSimulationTest.java`
- Extend where useful: existing Semantic prompt/rumor preservation tests only; production changes only after observed RED.

- [ ] Persist two claims + contradiction, copy `memory2.json` and `semantic-memory.json` into a fresh world root, and assert exact payload/result equality.
- [ ] Exercise multiple NPCs, current/foreign players, global/private/shared scope, >200 unrelated Semantic records and >200 unrelated MemoryEvents with deterministic IDs/game times.
- [ ] Prove forward/reverse pressure insertion produces the same relevant contradiction state where existing retention policy promises deterministic ordering.
- [ ] Prove a conflicting current FACT and a contradictory BELIEF keep their original kinds/provenance/confidence.
- [ ] Prove contradiction evidence itself stores no claim text and is absent from generic `Memory2ContextProvider` output.
- [ ] Prove prompt formatter still contains `Current observed factual context wins on conflict.` and `Confidence never converts a BELIEF into a FACT.`
- [ ] Prove existing provenance-aware 8-hop rumor simulation remains green.
- [ ] Run complete `:common:test` GREEN.
- [ ] Commit: `test: verify bounded semantic contradictions`.

---

### Task 7: Changelog, evidence, review and exact-head delivery

**Files:**
- Modify: `CHANGELOG.md`
- Create: `docs/superpowers/evidence/2026-08-09-semantic-contradictions-tdd.md`
- After product merge only: update exactly `docs/PROJECT_STATE.md`, `docs/ROADMAP.md` in a separate docs PR.

- [ ] Update root `[Unreleased]`: structured contradiction process evidence, exact-ID authority, stable logical identity, no duplicated claim text, no truth promotion, query only while claims remain live, generic prompt isolation, and no provider/config/store/migration change.
- [ ] Record observed tests-only RED SHA/run/failure and minimal GREEN evidence for every behavior-changing task; do not invent unavailable intermediate evidence.
- [ ] Independent base→head review checks provider/client authority, no summary parsing, no duplicate claim prose, no claim mutation, no truth winner, no resurrection after forgetting, consolidation compatibility, bounded retention, privacy, persistence versions and existing rumor invariants.
- [ ] Run fresh exact-head mandatory workflows: Repository security policy, VillAIgence CI, Production Soak, GitHub Release dry-run. Publication job must be `SKIPPED`.
- [ ] Require P0/P1/P2 = 0/0/0 and zero unresolved review threads before merge.
- [ ] Squash merge with expected exact head SHA. Do not publish a release.
- [ ] Create separate docs-only reconciliation PR marking semantic contradiction representation COMPLETE and advancing NEXT to contradiction-aware prompt preservation / uncertainty design according to the final accepted boundary.

## Self-review

Spec coverage: all persistence, identity, lifecycle, query, privacy, forgetting, generic-prompt isolation, truth and delivery requirements map to Tasks 1-7.

Placeholder scan: no TBD/TODO/implementation placeholders.

Type consistency: `SemanticMemoryIdentity`, `SemanticContradiction`, `SemanticContradictionPolicy`, `SemanticContradictionAdapter`, `SemanticContradictionLifecycle`, `SemanticContradictionResult`, and `SemanticContradictionHistory` signatures are defined once and reused consistently.

Scope remains one cohesive representation/lifecycle/query slice. Automatic contradiction detection, dedicated prompt injection, uncertainty, distortion and trust weighting are explicitly excluded.