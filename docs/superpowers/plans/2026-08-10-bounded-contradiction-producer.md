# Bounded Contradiction Candidate/Producer Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Automatically feed ordinary controlled Semantic admission into a strictly bounded, deterministic contradiction producer without truth arbitration or all-pairs growth.

**Architecture:** Add a pure same-owner/same-scope candidate selector, a conservative explicit-negation classifier, and a producer that suppresses existing relation pairs before spending a hard eight-comparison budget and delegates persistence to the existing `SemanticContradictionLifecycle`. Wire the producer only after `ControlledSemanticMemoryIngestor` has persisted/consolidated and re-resolved the retained logical claim.

**Tech Stack:** Java 21, JUnit 5, existing Memory 2.0 JSON stores, GitHub Actions exact-head CI/soak/release-dry workflows.

## Global Constraints

- `MAX_CANDIDATES_PER_ADMISSION = 16`.
- `MAX_COMPARISONS_PER_ADMISSION = 8`.
- Candidate eligibility is same NPC owner + exact canonical `relatedEntities` scope before limiting.
- Equivalent logical/text claims are excluded before classifier allocation.
- Initial classifier recognizes only exactly one standalone `not` or `не` polarity insertion/removal.
- No provider request/schema, public config, new persistence store/version/field, migration or release identity change.
- Existing `semantic-contradiction-v1` lifecycle remains the only persistence authority.
- No winner, confidence mutation, deletion, provenance mutation or BELIEF→FACT promotion.
- Direct `SemanticMemoryStore.append(...)` remains storage-only.

---

### Task 1: Deterministic candidate selector and opposition classifier

**Files:**
- Create: `common/src/main/java/net/conczin/mca/livingworld/memory2/SemanticContradictionCandidateSelector.java`
- Create: `common/src/main/java/net/conczin/mca/livingworld/memory2/SemanticOppositionClassifier.java`
- Test: `common/src/test/java/net/conczin/mca/livingworld/memory2/SemanticContradictionCandidateSelectorTest.java`
- Test: `common/src/test/java/net/conczin/mca/livingworld/memory2/SemanticOppositionClassifierTest.java`

**Interfaces:**
- `SemanticContradictionCandidateSelector.select(SemanticMemoryStore store, SemanticMemoryEntry subject)` → `List<SemanticMemoryEntry>`.
- `SemanticOppositionClassifier.opposes(SemanticMemoryEntry first, SemanticMemoryEntry second)` → `boolean`.
- Selector exposes package-private constants `MAX_CANDIDATES_PER_ADMISSION = 16` and `MAX_COMPARISONS_PER_ADMISSION = 8` for policy tests.

- [ ] **Step 1: Write selector RED tests** proving exact scope filtering happens before the 16-result limit, same logical/equivalent claims are excluded, cross-owner claims are absent, and ordering is deterministic newest-first.
- [ ] **Step 2: Run PR CI** and verify expected compile RED because selector class is missing.
- [ ] **Step 3: Implement minimal selector** using `SemanticMemoryStore.getRecentMatching(...)` with same-owner implicit store key, exact canonical scope, logical-ID inequality and canonical-text inequality inside the predicate.
- [ ] **Step 4: Run PR CI** and verify selector tests pass.
- [ ] **Step 5: Write classifier RED tests** for English/Russian explicit negation symmetry, equal text, double negation, antonyms, trailing-sentence omission and numeric differences.
- [ ] **Step 6: Run PR CI** and verify expected compile RED because classifier class is missing.
- [ ] **Step 7: Implement minimal classifier** by canonical normalization, Unicode-whitespace tokenization and exact removal of one standalone `not`/`не` token from exactly one side.
- [ ] **Step 8: Run common regression** and commit GREEN.

---

### Task 2: Bounded producer, duplicate suppression and lifecycle delegation

**Files:**
- Create: `common/src/main/java/net/conczin/mca/livingworld/memory2/BoundedSemanticContradictionProducer.java`
- Test: `common/src/test/java/net/conczin/mca/livingworld/memory2/BoundedSemanticContradictionProducerTest.java`

**Interfaces:**
- `BoundedSemanticContradictionProducer.produce(Path worldRoot, SemanticMemoryEntry retainedClaim, int maxEventsPerNpc)` → `ProductionResult`.
- `ProductionResult(int eligibleCandidates, int comparisons, int oppositions, List<UUID> recordedEventIds)` is immutable and diagnostics-only.

- [ ] **Step 1: Write producer RED tests** proving max 8 classifier comparisons, max 8 new relations, existing retained relation suppression before comparison budget, replay duplicate suppression, and lifecycle fail-closed behavior when a source disappears.
- [ ] **Step 2: Run PR CI** and verify expected compile RED because producer is missing.
- [ ] **Step 3: Implement producer**: select max 16 eligible candidates; snapshot valid retained contradiction logical-ID pair keys; skip existing pairs without incrementing comparisons; stop at 8 classifications; classify conservatively; call `SemanticContradictionLifecycle.record(...)`; count only `RECORDED` event IDs.
- [ ] **Step 4: Run common regression** and verify all producer tests pass.
- [ ] **Step 5: Add deterministic 10-NPC pressure test** proving each admission remains bounded and one NPC/private scope cannot consume another NPC/scope candidate slots.
- [ ] **Step 6: Re-run common regression** and commit GREEN.

---

### Task 3: Automatic controlled-admission integration

**Files:**
- Modify: `common/src/main/java/net/conczin/mca/livingworld/memory2/ControlledSemanticMemoryIngestor.java`
- Test: `common/src/test/java/net/conczin/mca/livingworld/memory2/ControlledSemanticMemoryIngestorContradictionTest.java`

**Interfaces:**
- Existing public `recordFact(...)`, `recordFactIfEnabled(...)`, `recordBelief(...)`, `recordBeliefIfEnabled(...)` signatures remain unchanged.
- Add package-private helper `persistAndResolve(Path, SemanticMemoryEntry, int)` → `Optional<SemanticMemoryEntry>`.

- [ ] **Step 1: Write integration RED tests** proving a controlled FACT plus explicit-negation BELIEF automatically records one live disagreement, while both Semantic entries retain exact kind/provenance/confidence/source IDs.
- [ ] **Step 2: Add RED replay test** proving re-admission/consolidation does not create a second identical live contradiction relation.
- [ ] **Step 3: Add RED transformed-wording preservation test** proving trailing-sentence omission alone records no contradiction.
- [ ] **Step 4: Run PR CI** and verify behavioral RED with no automatic contradiction event.
- [ ] **Step 5: Implement `persistAndResolve(...)`**: compute logical claim ID, append to Semantic store, reread retained matching logical ID, return empty if pressure evicts it.
- [ ] **Step 6: Invoke `BoundedSemanticContradictionProducer.produce(...)` only after retained resolution**; use the existing `maxEntriesPerNpc` as the bounded Memory 2.0 event capacity because current controlled call sites already pass `memory2MaxEventsPerNpc`.
- [ ] **Step 7: Re-run common regression** and verify PlayerTold/NPC-transfer callers remain source-compatible and all integration tests pass.

---

### Task 4: Pressure/restart preservation and product documentation

**Files:**
- Test: `common/src/test/java/net/conczin/mca/livingworld/memory2/BoundedSemanticContradictionProducerPersistenceTest.java`
- Modify: `CHANGELOG.md`
- Create: `docs/superpowers/evidence/2026-08-10-bounded-contradiction-producer-tdd.md`

**Interfaces:** Existing persistence formats remain unchanged.

- [ ] **Step 1: Write pressure/forgetting test** proving forgotten claims stop resolving through existing `SemanticContradictionHistory` and producer work remains bounded under >200 Semantic records.
- [ ] **Step 2: Write fresh-root reload test** by copying persisted `livingworld` files to a second root and proving live disagreement resolution is stable without reconstructing claim prose from contradiction evidence.
- [ ] **Step 3: Run common regression** and fix only feature-caused failures.
- [ ] **Step 4: Update root `[Unreleased]`** with the bounded producer guarantee, conservative classifier boundary, hard limits and unchanged truth/persistence/provider contracts.
- [ ] **Step 5: Record staged RED/GREEN SHA/run evidence** in `docs/superpowers/evidence/2026-08-10-bounded-contradiction-producer-tdd.md`.

---

### Task 5: Exact-head review and delivery gates

**Files:** no runtime changes unless review finds a validated defect.

- [ ] **Step 1: Compare base `92e6eed585d41478033fdebc2591c4745efd7432` to feature head** and inspect every runtime/test/changelog file for P0/P1/P2 issues, unintended provider/config/persistence changes and changelog drift.
- [ ] **Step 2: Resolve any validated finding with tests-first RED/GREEN** before freezing the head.
- [ ] **Step 3: Run exact-head mandatory workflows:** Repository security policy, VillAIgence CI, VillAIgence Production Soak, VillAIgence GitHub Release dry-run; publication must remain skipped.
- [ ] **Step 4: Verify PR review threads/comments are clear** and update PR body with exact head SHA and run IDs.
- [ ] **Step 5: Squash merge only if all mandatory exact-head gates are green.**
- [ ] **Step 6: Reconcile `docs/PROJECT_STATE.md` and `docs/ROADMAP.md` in a follow-up docs-only PR, preserving installed `0.2.0+1.21.1` as a separate immutable acceptance boundary and advancing NEXT to settlement-scale information flow without omniscience.
