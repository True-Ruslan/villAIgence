# Settlement-Scale Information Flow Without Omniscience Implementation Plan

> Executed inline in PR #147 using staged TDD. This plan records the implemented sequence and review/delivery gates.

**Goal:** Add bounded deterministic settlement-scale NPC knowledge propagation that reuses exact source-backed transfer and never creates shared omniscient settlement memory.

**Final architecture:** MCA home-village membership + existing loaded/staggered `Village.tick` cadence → bounded `SettlementKnowledgeFlowSelector` → `SettlementKnowledgeFlowLifecycle` → exact `NpcKnowledgeTransferLifecycle.transfer(...)`. Runtime wiring is a minimal `MixinVillage` injection at the already gated village update branch.

## Final hard constraints

```text
CYCLE_TICKS = 1200
MAX_RESIDENTS_PER_CYCLE = 16
MAX_SPEAKERS_PER_CYCLE = 4
MAX_SOURCE_CANDIDATES_PER_SPEAKER = 2
MAX_OPPORTUNITIES_PER_CYCLE = 4
MAX_FANOUT_PER_SOURCE_PER_CYCLE = 1
```

Additional invariants discovered/confirmed during TDD:
- eligible source `gameTime` must be before the current cycle start, preventing same-cycle cascades;
- equivalent scoped knowledge across carriers uses one normalized-statement + exact-scope cycle key;
- one source/cycle has one deterministic listener and no fallback;
- listener-equivalent suppression is exact normalized statement + exact canonical scope;
- every mutation delegates to existing `NpcKnowledgeTransferLifecycle`;
- no provider/config/persistence/release identity expansion.

---

## Task 1 — bounded deterministic selector — COMPLETE

**Production:** `SettlementKnowledgeFlowSelector.java`
**Tests:** `SettlementKnowledgeFlowSelectorTest.java`

- [x] Tests-only RED for absent selector API — commit `e2e0f79`, CI #2449 / run `31379579060`, 18 expected missing-symbol errors.
- [x] Minimal deterministic resident window, speaker/source bounds, target selection and membership isolation — commit `82cf518`.
- [x] GREEN — CI #2451 / run `31379822397` common/mock-provider SUCCESS.
- [x] Same-cycle no-fallback target behavior covered.
- [x] Source-age anti-cascade boundary added after replay semantics exposed the need — commit `0d7ab01`.

## Task 2 — exact transfer lifecycle orchestration — COMPLETE

**Production:** `SettlementKnowledgeFlowLifecycle.java`
**Tests:** `SettlementKnowledgeFlowLifecycleTest.java`

- [x] Tests-only RED for absent lifecycle API — commit `49b648f`, CI #2453 / run `31380048563`, 6 expected missing-symbol errors.
- [x] Minimal lifecycle delegates only to `NpcKnowledgeTransferLifecycle.transfer(...)` — commit `5774458`.
- [x] GREEN — CI #2457 / run `31380538643` common/mock-provider SUCCESS.
- [x] Source FACT stays FACT; listener becomes `BELIEF/NPC_TOLD`; exact evidence path asserted.
- [x] Same-cycle replay cannot create another target/evidence.

## Task 3 — preservation — COMPLETE

**Tests:** `SettlementKnowledgeFlowPreservationTest.java`

- [x] Player-private scope preserved; foreign-player retrieval remains closed.
- [x] v2 NPC_TOLD lineage extends rather than resetting origin.
- [x] Existing `OMIT_TRAILING_SENTENCE` transformation snapshot propagates unchanged; one-transform budget does not reset.
- [x] Settlement-delivered explicit opposition triggers existing truth-neutral contradiction producer with exact matching scope.
- [x] Initial direct `PLAYER_TOLD` fixture without persisted source DIALOGUE recorded as invalid fixture, not product RED.
- [x] Initial contradiction scope mismatch recorded as fixture error and corrected without weakening exact-scope policy.

## Task 4 — replay / pressure / carrier fan-out — COMPLETE

**Tests:** `SettlementKnowledgeFlowPersistenceTest.java`, strengthened `SettlementKnowledgeFlowSelectorTest.java`

- [x] Fresh-root copy of `memory2.json` + `semantic-memory.json` cannot expand same-cycle fan-out.
- [x] A later authoritative cycle may progress the knowledge to another target.
- [x] 12 settlements × 24 residents and hundreds of retained Semantic claims retain the constant 16-resident / 4-speaker / ≤4-opportunity bounds with membership isolation.
- [x] Strengthened test proved a real defect: equivalent scoped knowledge held by multiple carriers could allocate >1 opportunity in a cycle — CI #2477 / run `31382255372`.
- [x] Production fix: canonical statement + exact canonical scope `KnowledgeKey`, consumed before target/fallback evaluation — commit `36ee459`.
- [x] Fan-out regression and later-cycle progression turned GREEN in CI #2479 / run `31382550847`.
- [x] Final full domain/preservation GREEN — commit `b662f65`, CI #2481 / run `31382862171`.

## Task 5 — runtime adapter and MCA village wiring — COMPLETE

**Production:** `SettlementKnowledgeFlowRuntime.java`, `MixinVillage.java`, `mca.mixins.json`
**Tests:** `SettlementKnowledgeFlowRuntimeTest.java`

- [x] Tests-only RED for absent runtime adapter API — commit `4336d93`, CI #2463 / run `31380844904`, 2 expected missing-symbol errors.
- [x] Runtime adapter is strict no-op when disabled, uses existing Memory 2.0 capacity and fails soft — commit `b4ac1b1`.
- [x] `MixinVillage` injects at the existing `VillageGuardsManager.spawnGuards(ServerLevel)` invocation, inheriting MCA's staggered 1200-tick / move-in / loaded-chunk branch — commits `35b76f8`, `00eb3a2`.
- [x] Original authoritative `gameTime` is passed rather than the local village-ID scheduling offset.
- [x] Fabric/NeoForge loaders, GameTests and production startup verified in CI #2485 / run `31383129185`.

## Task 6 — documentation / evidence — COMPLETE

- [x] Root `[Unreleased]` changelog records home-village boundary, 16/4/2/4/1 bounds, anti-cascade, carrier-level fan-out, no-fallback, exact lifecycle reuse and unchanged provider/persistence/truth contracts.
- [x] Staged TDD evidence: `docs/superpowers/evidence/2026-08-10-settlement-knowledge-flow-tdd.md`.
- [x] Invalid fixtures are distinguished from genuine product RED.
- [x] Installed `0.2.0+1.21.1` acceptance remains immutable and separate.

## Task 7 — exact-head review / delivery — PENDING FINAL HEAD FREEZE

- [ ] Compare base `455d2ea36a393b2521346107fa6351f0a89ee0cd` to final feature head and inspect every changed file for P0/P1/P2 issues, unbounded pair scans, scope leakage, duplicate fan-out, direct listener Semantic writes, Mixin injection risk, provider/config/persistence drift and changelog truncation.
- [ ] Verify PR discussion/review threads are clear.
- [ ] Freeze exact feature SHA.
- [ ] Require fresh exact-head SUCCESS on Repository security policy, VillAIgence CI, VillAIgence Production Soak and VillAIgence GitHub Release dry-run.
- [ ] Require `github-release` publication job SKIPPED.
- [ ] Update PR #147 body with exact evidence and merge only after all mandatory gates are green.
- [ ] Follow with docs-only `PROJECT_STATE.md` / `ROADMAP.md` reconciliation, preserving official installed `0.2.0+1.21.1` and advancing NEXT to relationship/trust social epistemology.
