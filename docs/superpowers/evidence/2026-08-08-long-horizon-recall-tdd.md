# Long-Horizon Recall TDD Evidence

Date: 2026-08-08
Branch: `feat/long-horizon-recall`
Base: `b09924d7297775baabf577ca50dbcb65c22f0516`
PR: #131

This ledger distinguishes observed RED failures, GREEN implementation evidence, preservation tests that required no production change, and final exact-head delivery evidence. The official installed-release claim remains `0.2.0+1.21.1`; evidence here is unreleased PR/candidate evidence.

## Design / plan gates

- Design spec initial commit: `f08a631fe4c9aaeb2f283ec6506f05e70be67309`.
- Design self-review clarification: `3b010c992687d2d0cfa39ba199ee3e33cb7eddc7`.
- Implementation plan: `84f12b78ce4bdcf047c86c2025c08a52b8d49c0c`.
- User approved the design/spec and authorized implementation before runtime work.

## RED 1 — Semantic retained-but-starved recall

Behavioral tests-only commit: `b13ac7a84cccc2ad5c50826db935129168f7cb95`.
PR-triggered observed RED head: `806d78862d8fc7ce559dd9c2ce4baaf7164db6e8`.

Production code for this behavior changed before RED: **NO**.

Failing contract:

`SemanticMemoryRetrieverTest.contextProviderRecallsOldDurableSemanticMemoryAfterNewerEligibleWindowAndReload`

Expected and observed defect:

- an old high-durability eligible Semantic BELIEF remains physically persisted;
- it survives a real persistence-file round-trip into a fresh world root;
- current newest-only candidate retrieval takes the newest 32 eligible entries before ranking;
- the old durable record therefore cannot enter prompt context.

Observed CI:

- VillAIgence CI #2023 / run `31256528526` — **FAILURE as expected**;
- `:common:compileJava` — PASS;
- `:common:compileTestJava` — PASS;
- `:common:test` — **526 tests / exactly 1 failed**;
- exact failure: `contextProviderRecallsOldDurableSemanticMemoryAfterNewerEligibleWindowAndReload`;
- preflight/release-identity/security steps preceding tests — PASS;
- downstream runtime stages correctly skipped after the intentional RED.

Conclusion: behavioral starvation RED confirmed independently of fixture/setup failure.

## RED 1b — pure bounded candidate-selector API

Tests-only selector commit: `5d14c0ab7750e65287964424a12b59325a93377e`.

Observed CI:

- VillAIgence CI #2027 / run `31256674878` — **FAILURE as expected**;
- production compile — PASS;
- test compile — FAIL with expected `cannot find symbol: LongHorizonCandidateSelector` errors.

Minimal selector implementation: `1e31923879962c2be652fb81f24723461435757d`.

The first selector implementation run #2029 / `31256810773` compiled but exposed two selector-test failures plus the intentionally still-red Semantic provider test. Systematic debugging proved the two selector failures were caused by a test comparator expression whose terminal `.reversed()` inverted the whole comparator chain. Production selector code was not changed.

Test-only comparator correction: `a5fd8831d27ae724bb766cc51d2761cf8190ba09`.

Observed CI after correction:

- VillAIgence CI #2031 / run `31256991307`;
- **531 tests / exactly 1 failed**;
- all `LongHorizonCandidateSelectorTest` cases PASS;
- only the original Semantic behavioral RED remained FAIL.

Conclusion: pure selector GREEN before provider wiring.

## GREEN 1 — Semantic long-horizon recall

Minimal provider wiring commit: `c4176cad8fd767c87d3a9f650e128dc4774b1c3d`.

Production change:

```text
hard-bounded Semantic store
→ exact current-player/NPC-global/shared eligibility
→ 24 recent + 8 durable candidates at normal limit 32
→ existing SemanticMemoryRetriever
→ existing formatter / max 6
```

Preserved:

- Semantic persistence policy unchanged;
- candidate/result bounds remain `32` / `6`;
- existing semantic ranking weights unchanged;
- provenance/truth classes unchanged.

Exact-head evidence:

- Repository security policy #1668 / run `31257132977` — SUCCESS;
- VillAIgence CI #2033 / run `31257132985` — SUCCESS;
- common/mock-provider tests — SUCCESS;
- risk-selected server GameTests + Fabric/NeoForge — SUCCESS;
- production startup/restart acceptance — SUCCESS;
- package verification — SUCCESS;
- persistence recovery was not selected by that exact risk set and is therefore SKIPPED, not PASS.

## RED 2 — episodic FIFO pressure loss

Behavioral tests-only commit: `00538d57416d5c5aa2e5e0244dc7c91230805eac`.

Production episodic retention change before RED: **NO**; `MemoryEventStore` still evicted oldest-first.

Failing contract:

`MemoryEventStoreTest.pressureKeepsOldImportantObservationOverNewerWeakDialogueAfterReload`

Observed CI:

- VillAIgence CI #2037 / run `31257438044` — **FAILURE as expected**;
- **532 tests / exactly 1 failed**;
- old important SYSTEM_OBSERVED event was displaced solely because two weaker dialogue events were newer.

## RED 2b — pure episodic retention-policy API

Tests-only policy commit: `b104698386ae0a68ea3a4ce9fad02a2d5cfe904b`.

Contract tests lock:

- importance/confidence/absolute-emotion monotonicity;
- SYSTEM_OBSERVED not less durable than otherwise-equal told/inferred provenance;
- `RELATIONSHIP_CAUSE > RELATIONSHIP_CHANGE > OBSERVATION == ACTION > DIALOGUE`;
- authoritative Minecraft game-time decay and future-age clamp;
- old important memory may survive newer weak dialogue;
- no event type is immortal;
- input-order independence, deterministic UUID tie break and stable persistence order.

Observed CI:

- VillAIgence CI #2039 / run `31257560621` — **FAILURE as expected**;
- production compile — PASS;
- test compile — FAIL only because `MemoryEventRetentionPolicy` did not yet exist.

Minimal pure policy commit: `34d240c9eb41aea6dcf5ae160c287bd618fc276a`.

CI #2041 / run `31257682161` then showed the pure policy tests GREEN while the original FIFO store behavioral test remained the only failure.

## RED 2c — rejected weak append must not rewrite persistence

Tests-only store contract: `0713bfd88d0cab556041308a4ae797957cfc7fe6`.

Added:

`MemoryEventStoreTest.rejectedWeakAppendDoesNotRewriteEventFile`

Observed CI:

- VillAIgence CI #2043 / run `31257852327` — **540 tests / exactly 2 failed**;
- failures were exactly the two expected store behaviors:
  - old important event lost by FIFO;
  - immediately rejected weak append rewrote `memory2.json`;
- pure episodic retention policy remained GREEN.

## GREEN 2 — episodic durability-aware bounded persistence

Store wiring commit: `6e7cceea6ef6099a413fdb749d55584b72b9acbc`.

`MemoryEventStore.append(...)` now performs:

```text
exact duplicate check
→ add candidate
→ max authoritative persisted gameTime
→ MemoryEventRetentionPolicy.selectRetained(...)
→ stable chronological persistence order
→ save only when retained state changed
```

No JSON format/version or public config changed.

Exact-head evidence:

- VillAIgence CI #2045 / run `31257990249` — SUCCESS;
- common suite — SUCCESS;
- server GameTests + Fabric/NeoForge — SUCCESS;
- production startup/restart — SUCCESS;
- selected persistence-recovery matrix — SUCCESS;
- package verification — SUCCESS.

Both earlier store REDs became GREEN.

## RED 3 — episodic retained-but-starved recall

Tests-only commit: `a17bdbbd166c9ae2961fd3f6783c11c2f0fd373e`.

Failing contract:

`MemoryRetrieverTest.contextProviderRecallsOldImportantEventAfterNewerEligibleWindowAndReload`

Fixture proves before the final assertion that the old high-durability current-player `RELATIONSHIP_CHANGE` remains persisted under the new retention policy and survives a real fresh-world file round-trip. Forty newer eligible weak DIALOGUE events then fill the newest-only retrieval window.

Observed CI:

- VillAIgence CI #2047 / run `31258323146` — **FAILURE as expected**;
- **541 tests / exactly 1 failed**;
- only final prompt-context recall was missing;
- store survival assertion already passed.

Conclusion: retrieval starvation isolated from persistence retention.

## GREEN 3 — episodic long-horizon recall

Provider wiring commit: `50e0da2339b360f72c3ec147150c72a4b324c087`.

Production change:

```text
hard-bounded MemoryEventStore
→ exact current-player/NPC-global/shared eligibility
→ 24 recent + 8 durable candidates at normal limit 32
→ existing MemoryRetriever
→ existing formatter / max 6
```

Preserved:

- `MemoryRetriever` ranking weights unchanged at this stage;
- `32` / `6` bounds unchanged;
- persistence format/config unchanged;
- current-truth prompt authority unchanged.

Exact-head evidence:

- VillAIgence CI #2049 / run `31258445014` — SUCCESS;
- common suite — SUCCESS;
- server GameTests + Fabric/NeoForge — SUCCESS;
- production startup/restart — SUCCESS;
- persistence recovery — SUCCESS;
- package verification — SUCCESS.

## Preservation package — multi-session, pressure, privacy and deterministic simulation

Initial test-only simulation commit: `c95eb767fa8ca39f58481641f746c69bf032936a`.

No production code changed.

Coverage:

1. multi-day Semantic + episodic pressure over many `36_000`-tick decay steps;
2. exact retained IDs and exact prompt-context equality across `session-a → session-b → session-c` fresh-world persistence-file round-trips;
3. old durable Semantic and old important relationship memory remain recallable after >1 candidate window of newer weak history;
4. foreign high-durability memory cannot consume recent/durable prompt-candidate slots for the current player;
5. 240 Semantic + 240 episodic synthetic records exercise deterministic retention and candidate selection with forward vs reversed input order;
6. no sleep or wall-clock-dependent assertion is used.

Exact-head evidence for this package:

- Repository security policy #1686 / run `31258939810` — SUCCESS;
- VillAIgence CI #2051 / run `31258939814` — SUCCESS;
- Production Soak #144 / run `31258939792` — SUCCESS;
- GitHub Release dry-run #478 / run `31258939812` — SUCCESS;
- common tests, selected GameTests/loaders, startup/restart, persistence recovery and package verification all SUCCESS;
- release evidence remains dry-run/candidate evidence and does not change the installed `0.2.0` claim.

Mixed-scope privacy strengthening is test-only commit `1c4cef219f3821f7c8a7e98abf4c8207f7b83dd2` and adds:

- two NPCs;
- current player + foreign player;
- shared current-player-plus-other-entity Semantic memory;
- shared current-player-plus-other-entity episodic ACTION;
- foreign high-durability Semantic/relationship history;
- other-NPC high-durability records.

Observed on VillAIgence CI #2053 / run `31259388903`:

- common/mock-provider tests — SUCCESS;
- no production change was required;
- full run was still executing when later evidence/test commits advanced the branch, so later exact-head gates supersede it.

## RED 4 — NPC-global memory was eligible but under-ranked in player-scoped recall

The mixed-scope simulation intentionally keeps NPC-global memory eligible. A test-only modeling correction then made the global Semantic fixture a genuine `SYSTEM_OBSERVED` FACT instead of an empty-scope told BELIEF:

- test-only commit `91335ab4f6fb1b6b9a57d0f0386d9397d0fefb60`;
- production source changed by this commit: **NO**.

Observed CI #2069 / run `31259874079`:

- production compile and test compile — PASS;
- repository security before tests — PASS;
- **544 tests / exactly 1 failed**;
- exact failure: `LongHorizonMemorySimulationTest.foreignHighDurabilityMemoryConsumesNoRecentOrDurableSlotsAcrossMixedScopes`;
- the failure showed an NPC-global Semantic memory was admitted by the eligibility boundary but could still lose final prompt selection because the player-scoped relevance calculation gave empty `relatedEntities` zero relevance.

A second tests-only commit made the ranker contract explicit for both memory domains:

- `997b5134c519d4f5cd2a200aed7f95cba55b6ae2` — `test: expose NPC-global relevance starvation`;
- added only `NpcGlobalMemoryRelevanceTest`.

Observed CI #2071 / run `31260072459`:

- production compile and test compile — PASS;
- repository security before tests — PASS;
- **546 tests / exactly 3 failed**;
- exact failures:
  - mixed-scope long-horizon simulation;
  - `npcGlobalSemanticMemoryHasFullRelevanceForPlayerScopedQuery`;
  - `npcGlobalEpisodicMemoryHasFullParticipantRelevanceForPlayerScopedQuery`.

Minimal Semantic-only fix:

- commit `575d272b249dea8fbc5fe91f9fa7dd04957b15df`;
- changed only `SemanticMemoryRetriever.relevanceScore(...)` so an entry with empty `relatedEntities` has full relevance for a player-scoped query.

Observed CI #2073 / run `31260197193`:

- Semantic NPC-global unit contract became GREEN;
- **546 tests / exactly 2 failed**;
- remaining failures were exactly the mixed-scope simulation and episodic NPC-global relevance contract.

Minimal episodic-only fix:

- commit `37af57a19374aee501a74b4f5f2146978f00dbd3`;
- changed only `MemoryRetriever.relevanceScore(...)` so an event whose external participants are empty after removing its owner NPC is treated as NPC-global and fully relevant for a player-scoped query.

This preserves the already-established eligibility boundary: foreign-only memory remains excluded before both candidate pools; the relevance change applies only to already-eligible NPC-global memory.

## GREEN 4 — NPC-global relevance and final runtime head

Exact runtime head before final evidence-only synchronization: `37af57a19374aee501a74b4f5f2146978f00dbd3`.

Required workflows on that exact runtime head all completed successfully:

- Repository security policy #1710 / run `31260204824` — SUCCESS;
- VillAIgence CI #2075 / run `31260204839` — SUCCESS;
- Production Soak #156 / run `31260204817` — SUCCESS;
- GitHub Release dry-run #490 / run `31260204845` — SUCCESS;
- release publication job — SKIPPED as required.

Main CI #2075 verified:

- common/mock-provider tests — SUCCESS;
- risk catalog + required server GameTests — SUCCESS;
- Fabric and NeoForge builds — SUCCESS;
- production startup/restart acceptance — SUCCESS;
- selected persistence recovery — SUCCESS, exactly five cases;
- distributable package verification — SUCCESS;
- production-accepted candidate JAR and final CI package were byte-identical at SHA256 `de6d5bf8fcae9f32214b813ccc499edd7289278bfcabb5c19d16f278cdd4d15d`.

That SHA is **CI candidate evidence only** and must not be represented as the accepted official `0.2.0+1.21.1` release JAR.

Production Soak #156 verified constrained authenticated concurrency, constrained staging and five production restart cycles. Repository security #1710 passed all security-policy stages.

## Authority / provenance preservation

No new production prompt-authority code was required. Existing tests remained GREEN and assert:

- current observed facts structurally precede conflicting Operator Lore, Semantic BELIEF and episodic history;
- current relationship state precedes stale `RELATIONSHIP_CHANGE` / `RELATIONSHIP_CAUSE` history;
- conflicting high-confidence BELIEFs remain BELIEF;
- `RELATIONSHIP_CAUSE` does not promote linked DIALOGUE prose to FACT;
- memory summaries remain data, never instructions.

Retention/retrieval inputs remain server-owned persisted fields. No provider/model call controls truth class, visibility, retention score, persistence capacity or candidate quota.

## Independent base→runtime-head review

Read-only review scope: base `b09924d7297775baabf577ca50dbcb65c22f0516` → runtime head `37af57a19374aee501a74b4f5f2146978f00dbd3`.

Reviewed production surfaces include:

- `LongHorizonCandidateSelector`;
- `MemoryEventRetentionPolicy`;
- `MemoryEventStore`;
- `SemanticMemoryContextProvider`;
- `Memory2ContextProvider`;
- `SemanticMemoryRetriever`;
- `MemoryRetriever`;
- unchanged authority-critical `PlayerScopedMemoryEligibility` and persisted `MemoryEvent` invariants;
- dialogue/relationship/cause adapters that author retention-relevant fields.

Verdict:

- P0: 0;
- P1: 0;
- P2: 0;
- no blocking scope-creep, persistence-schema, public-config, provider-authority or privacy-boundary issue found;
- unresolved review threads at review time: 0;
- submitted PR reviews at review time: 0.

Review limitation: this is source + exact GitHub automation evidence, not installed-client/manual evidence. Existing installed-evidence gaps therefore remain unchanged.

## Final exact-head delivery gate

This evidence synchronization commit intentionally advances the branch after the fully-green runtime head above. Therefore the final merge gate is **not inherited** from `37af57a...`: the new evidence-only head must independently satisfy the same exact-head checks before PR #131 can be merged.

Required before merge:

- Repository security policy — SUCCESS on the new exact head;
- full VillAIgence CI — SUCCESS on the new exact head;
- Production Soak — SUCCESS on the new exact head;
- GitHub Release dry-run — SUCCESS on the new exact head with publication skipped;
- base→new-head review confirms the delta after `37af57a...` is evidence/documentation only;
- no unresolved review thread;
- PR body records the complete observed RED→GREEN chain;
- official installed release remains `0.2.0+1.21.1`.
