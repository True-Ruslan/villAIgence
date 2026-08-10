# Settlement-Scale Information Flow Without Omniscience Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add bounded deterministic settlement-scale NPC knowledge propagation that reuses exact source-backed transfer and never creates shared omniscient settlement memory.

**Architecture:** Reuse MCA `Village` home-village membership and existing staggered village update cadence. A pure/testable `SettlementKnowledgeFlowSelector` produces at most four deterministic no-fallback opportunities from a bounded resident/speaker/source window; `SettlementKnowledgeFlowLifecycle` delegates each opportunity only to `NpcKnowledgeTransferLifecycle.transfer(...)`; `Village.tick(...)` provides the server-owned membership/runtime hook.

**Tech Stack:** Java 21, JUnit 5, existing Memory 2.0 JSON stores, MCA `Village`/`Residency`, GitHub Actions exact-head CI/soak/release-dry workflows.

## Global Constraints

- `CYCLE_TICKS = 1200`.
- `MAX_RESIDENTS_PER_CYCLE = 16`.
- `MAX_SPEAKERS_PER_CYCLE = 4`.
- `MAX_SOURCE_CANDIDATES_PER_SPEAKER = 2`.
- `MAX_OPPORTUNITIES_PER_CYCLE = 4`.
- `MAX_FANOUT_PER_SOURCE_PER_CYCLE = 1`.
- Home-village membership is the only settlement boundary in this slice.
- Same source/cycle maps to one deterministic listener and never falls back to another target.
- Listener-equivalent suppression uses exact canonical Semantic scope + normalized statement before opportunity allocation.
- Every successful mutation delegates to existing `NpcKnowledgeTransferLifecycle.transfer(...)`.
- No provider call/schema, public config, new world file/store/schema/version, migration/backfill, trust weighting, cross-village propagation or release identity change.
- Listener knowledge remains local `BELIEF/NPC_TOLD`; FACT authority/confidence/provenance rules remain unchanged.
- Root `CHANGELOG.md` must be updated in the product PR.

---

### Task 1: Bounded deterministic settlement opportunity selector

**Files:**
- Create: `common/src/main/java/net/conczin/mca/livingworld/memory2/SettlementKnowledgeFlowSelector.java`
- Test: `common/src/test/java/net/conczin/mca/livingworld/memory2/SettlementKnowledgeFlowSelectorTest.java`

**Interfaces:**
- `SettlementKnowledgeFlowSelector.select(SemanticMemoryStore store, int villageId, long gameTime, Collection<UUID> residentIds)` → `SelectionResult`.
- `SelectionResult(List<UUID> residentWindow, int speakersConsidered, List<Opportunity> opportunities)` is immutable.
- `Opportunity(UUID speakerNpcId, UUID listenerNpcId, UUID sourceSemanticEntryId)` uses exact persisted IDs only.
- Package-private constants expose the six hard policy bounds for tests.

- [ ] **Step 1: Write selector RED tests** proving non-null/distinct UUID normalization, stable UUID ordering, deterministic cycle rotation, at most 16 materialized residents and at most 4 speakers.
- [ ] **Step 2: Add RED source/target tests** with persisted speaker Semantic entries proving at most two source candidates are considered, exactly one source is deterministically selected per speaker, one source/cycle maps to one listener and total opportunities are at most four.
- [ ] **Step 3: Add RED no-broadcast/equivalence tests** proving a chosen listener that already knows exact normalized statement + canonical scope yields no opportunity and does not fall back to another listener in the same cycle.
- [ ] **Step 4: Run PR CI** and verify compile RED only because selector API is absent.
- [ ] **Step 5: Implement minimal selector** using bounded `SemanticMemoryStore.getRecentMatching(...)`, canonical Semantic identity helpers and deterministic UUID/cycle arithmetic without randomness or provider calls.
- [ ] **Step 6: Run common regression** and verify selector GREEN.

---

### Task 2: Settlement flow lifecycle and exact transfer delegation

**Files:**
- Create: `common/src/main/java/net/conczin/mca/livingworld/memory2/SettlementKnowledgeFlowLifecycle.java`
- Test: `common/src/test/java/net/conczin/mca/livingworld/memory2/SettlementKnowledgeFlowLifecycleTest.java`

**Interfaces:**
- `SettlementKnowledgeFlowLifecycle.runCycle(Path worldRoot, int villageId, long gameTime, Collection<UUID> residentIds, int maxEventsPerNpc, int maxSemanticEntriesPerNpc)` → `CycleResult`.
- `CycleResult(int residentWindowSize, int speakersConsidered, int opportunities, int attemptedTransfers, int successfulTransfers, List<NpcKnowledgeTransferResult.Status> statuses)` is immutable diagnostics only.

- [ ] **Step 1: Write lifecycle RED** proving selected opportunity delegates through the real `NpcKnowledgeTransferLifecycle`, persists listener DIALOGUE/NPC_TOLD evidence and listener Semantic result is `BELIEF/NPC_TOLD`.
- [ ] **Step 2: Add RED truth/scope preservation assertions** proving source FACT remains FACT, listener confidence/provenance are those of existing transfer policy, and exact `relatedEntities` scope is unchanged.
- [ ] **Step 3: Add RED same-cycle replay test**: run identical village/cycle twice; second run may attempt the deterministic same opportunity or suppress it, but must create no second transfer evidence and must not target another listener.
- [ ] **Step 4: Run PR CI** and observe behavioral/compile RED with lifecycle absent.
- [ ] **Step 5: Implement minimal cycle runner**: call selector, iterate at most four opportunities, invoke only `NpcKnowledgeTransferLifecycle.transfer(...)`, collect statuses, count `TRANSFERRED` only.
- [ ] **Step 6: Re-run common regression** and verify lifecycle GREEN.

---

### Task 3: Privacy, provenance, transformation and contradiction preservation

**Files:**
- Test: `common/src/test/java/net/conczin/mca/livingworld/memory2/SettlementKnowledgeFlowPreservationTest.java`

**Interfaces:** No new production API unless a failing preservation test demonstrates a feature defect.

- [ ] **Step 1: Add private-scope preservation test** with two player identities proving transferred scope is byte/logically unchanged and foreign-player Working Memory retrieval still excludes the claim before allocation.
- [ ] **Step 2: Add provenance propagation test** starting from a valid v2 `BELIEF/NPC_TOLD` source and proving the settlement transfer appends the next bounded ancestry hop rather than resetting origin.
- [ ] **Step 3: Add transformed-source test** proving an existing `OMIT_TRAILING_SENTENCE` snapshot propagates unchanged and does not reset the one-transformation budget.
- [ ] **Step 4: Add contradiction interaction test** where a settlement-delivered explicit-negation BELIEF reaches a listener that retains the opposite claim; verify existing automatic contradiction producer records one truth-neutral live relation with no FACT/confidence mutation.
- [ ] **Step 5: Run common regression**; production changes are permitted only for feature-caused failures and require a focused RED/GREEN correction.

---

### Task 4: Fresh-root replay and multi-settlement pressure

**Files:**
- Test: `common/src/test/java/net/conczin/mca/livingworld/memory2/SettlementKnowledgeFlowPersistenceTest.java`

**Interfaces:** Existing `memory2.json` / `semantic-memory.json` formats remain unchanged.

- [ ] **Step 1: Add fresh-root test** copying `livingworld/memory2.json` and `livingworld/semantic-memory.json` to a second root after one cycle, rerunning the same village/cycle and proving no additional target/fan-out is created.
- [ ] **Step 2: Add later-cycle progression test** proving a new authoritative cycle may choose a different target while same-cycle replay cannot.
- [ ] **Step 3: Add multi-settlement pressure simulation** with at least 12 villages, at least 24 residents each and hundreds of Semantic entries; prove every cycle remains at 16 residents / 4 speakers / 4 opportunities and no village receives another village's resident IDs.
- [ ] **Step 4: Add high-noise equivalence/privacy pressure** proving equivalent known claims and unrelated village records consume zero transfer opportunities for the tested village.
- [ ] **Step 5: Run common regression** and preserve deterministic no-wall-clock assertions.

---

### Task 5: MCA Village runtime integration

**Files:**
- Modify: `common/src/main/java/net/conczin/mca/server/world/data/Village.java`
- Test: use existing common/loader compile plus a focused policy/integration test if Minecraft runtime glue can be exercised without brittle mocks.

**Interfaces:**
- Existing `Village.tick(ServerLevel world, long time)` signature remains unchanged.
- Integration executes only inside existing `isVillageUpdateTime(time)` branch.
- Gate with existing `LivingWorldConfig.memory2Enabled`.
- Use `Village.getId()`, `Village.getResidents()`, authoritative `time`, server world root and existing `memory2MaxEventsPerNpc`.

- [ ] **Step 1: Add source-level/compile RED where practical** proving `Village` has no settlement flow invocation before modification; do not introduce a brittle Minecraft mock solely to manufacture RED.
- [ ] **Step 2: Implement minimal runtime hook** in the existing staggered village update branch; no new scheduler, thread, provider call or persistence state.
- [ ] **Step 3: Run common + supported loader compile/GameTests** and verify Minecraft API/world-root integration.
- [ ] **Step 4: Verify existing village tax/building/report behavior remains unchanged outside the additive bounded call.

---

### Task 6: Product documentation and TDD evidence

**Files:**
- Modify: `CHANGELOG.md`
- Create: `docs/superpowers/evidence/2026-08-10-settlement-knowledge-flow-tdd.md`

- [ ] **Step 1: Update root `[Unreleased]`** with home-village settlement boundary, hard 16/4/2/4/1 bounds, no-fallback same-cycle semantics, exact lifecycle reuse, no omniscient store and unchanged provider/persistence/truth contracts.
- [ ] **Step 2: Record every test-only RED SHA/run and corresponding GREEN SHA/run**; classify fixture/policy mistakes honestly rather than counting them as product RED.
- [ ] **Step 3: Record preservation/multi-settlement evidence and any production correction it drove.
- [ ] **Step 4: Self-review plan/spec/changelog consistency and ensure installed `0.2.0+1.21.1` boundary is not expanded.

---

### Task 7: Exact-head review and delivery

**Files:** no runtime changes unless review finds a validated defect.

- [ ] **Step 1: Compare base `455d2ea36a393b2521346107fa6351f0a89ee0cd` to feature head** and inspect every changed runtime/test/doc file for P0/P1/P2 issues, unbounded scans, scope leakage, duplicate fan-out, direct Semantic writes, provider/config/persistence drift and changelog truncation.
- [ ] **Step 2: Resolve validated findings tests-first** before freezing the head.
- [ ] **Step 3: Verify PR threads/comments are clear** and freeze the exact feature SHA.
- [ ] **Step 4: Run exact-head mandatory workflows:** Repository security policy, VillAIgence CI, VillAIgence Production Soak, VillAIgence GitHub Release dry-run; release publication must remain skipped.
- [ ] **Step 5: Squash merge only if exact-head gates are green and review has no unresolved P0/P1/P2 blocker.
- [ ] **Step 6: Reconcile `docs/PROJECT_STATE.md` and `docs/ROADMAP.md` in a docs-only follow-up PR, preserving installed `0.2.0+1.21.1` as immutable evidence and advancing NEXT to relationship/trust social epistemology.
