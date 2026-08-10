# Changelog

All notable VillAIgence product, runtime, persistence, release and delivery changes are recorded here.

This is the **canonical changelog** for the project.

- `docs/PROJECT_STATE.md` describes the current verified state.
- `docs/ROADMAP.md` describes future direction.
- `docs/CHANGELOG.md` is the older detailed engineering-history ledger and is retained for historical evidence; new release/product history belongs here.

## Changelog policy

1. Every PR that changes runtime behavior, persistent data, public configuration, release semantics, security guarantees, or permanent CI guarantees must update `[Unreleased]` in the same PR.
2. Test-only and documentation-only PRs update the changelog only when they change a user-visible or delivery guarantee.
3. Release PRs move shipped `[Unreleased]` entries into the exact version section instead of duplicating them.
4. Automated, exact-candidate, exact-release, and installed/manual evidence remain distinct claims.
5. Failed or deferred acceptance is written explicitly and is never silently promoted to PASS.
6. Pre-1.0 breaking data boundaries are called out directly.

---

## [Unreleased]

### Added

- Controlled Semantic Memory BELIEF admission contract:
  - `PLAYER_TOLD` requires matching `PLAYER_TOLD` DIALOGUE evidence;
  - `NPC_TOLD` requires matching `NPC_TOLD` DIALOGUE evidence;
  - `INFERRED` remains a non-authoritative BELIEF with explicit source-event evidence;
  - `SYSTEM_OBSERVED` is rejected by the BELIEF path and remains reserved for FACT;
  - admitted BELIEFs are replay-idempotent and use the existing deterministic consolidation/source-union pipeline.
- Opt-in bounded `PLAYER_TOLD` BELIEF candidate extraction in the existing structured OpenAI/OpenRouter chat response:
  - no second provider request;
  - the model may propose statement strings only;
  - server-owned code fixes NPC owner, player identity, `PLAYER_TOLD` provenance and persisted source DIALOGUE event;
  - DIALOGUE is persisted before any BELIEF candidate can be admitted;
  - malformed/empty candidate metadata fails soft and writes nothing;
  - duplicate candidates are normalized/deduplicated and bounded to at most 8 hard-limit candidates, with a default configured limit of 3;
  - the extraction path cannot create or promote FACT.
- Trustworthy causal relationship memory in Memory 2.0:
  - each new `RELATIONSHIP_CHANGE` retains the exact server-applied before/after trust, respect, fear and affinity state as structured data;
  - a separate deterministic `RELATIONSHIP_CAUSE(DIALOGUE_TURN)` can link that exact transition to the exact persisted DIALOGUE event during which it occurred;
  - source NPC/player identities and source event UUIDs are server-owned and validated against persisted world-local evidence;
  - causal events are replay-idempotent and survive restart with their transition snapshot/source UUIDs intact even when bounded retention later removes referenced source events;
  - recent causal history can be queried with exact NPC/player isolation before limiting and without fabricating missing evidence;
  - free-form model explanations are never persisted as authoritative relationship causes, and causal events are not automatically promoted to Semantic FACT.
- Bounded long-horizon Memory 2.0 recall:
  - the normal 32-candidate prompt window reserves 24 slots for recent eligible memory and 8 for older durable eligible memory before the existing final ranker chooses at most 6 prompt entries;
  - Semantic Memory reuses its existing deterministic durability/decay policy rather than introducing a second persistence policy;
  - episodic/social Memory 2.0 now has deterministic game-time durability under capacity pressure so important older observations, actions and relationship history can survive weaker newer dialogue;
  - episodic durability uses server-owned importance, confidence, absolute emotional weight, provenance, event type and Minecraft `gameTime`; no wall-clock age or provider decision participates;
  - ordinary dialogue remains the least durable event tier, while `RELATIONSHIP_CAUSE` and `RELATIONSHIP_CHANGE` receive stronger bounded retention without becoming immortal;
  - a weak append that is immediately rejected under pressure does not rewrite `memory2.json` when the retained state is unchanged.
- Source-backed server-owned NPC-to-NPC Semantic knowledge transfer:
  - the caller selects an exact persisted speaker Semantic entry by server-owned IDs and cannot inject arbitrary claim text, provenance, truth class, scope, source IDs, importance or confidence;
  - the speaker source is reread authoritatively before transfer evidence is constructed;
  - transfer first persists exact listener-owned `DIALOGUE / NPC_TOLD` evidence with deterministic identity, then rereads and validates that exact evidence before BELIEF admission;
  - speaker FACT or BELIEF always becomes listener `BELIEF / NPC_TOLD`, never listener FACT, and the speaker's upstream provenance/source chain is not copied as listener authority;
  - semantic subject scope is preserved without automatically adding the speaker to `relatedEntities`;
  - exact retry is idempotent, while a later transfer at another authoritative `gameTime` creates distinct evidence that may consolidate into the same logical BELIEF with deterministic source union;
  - bounded pressure reports explicit `SOURCE_NOT_RETAINED` and `BELIEF_NOT_RETAINED` outcomes without fabricating evidence or rolling back a legitimate persisted transfer event.
- Provenance-aware bounded multi-hop rumors for NPC-to-NPC knowledge transfer:
  - each new v2 transfer evidence event stores one immutable origin snapshot plus an ordered ancestry path under the deterministic `npc-knowledge-transfer-v2` identity namespace;
  - first-hop origins are limited to `FACT/SYSTEM_OBSERVED`, `BELIEF/PLAYER_TOLD`, or `BELIEF/INFERRED`; downstream `BELIEF/NPC_TOLD` must inherit retained structured lineage and cannot reset its origin;
  - lineage is acyclic and capped at eight hops, with explicit `PROVENANCE_CYCLE`, `PROVENANCE_LIMIT_REACHED`, and `PROVENANCE_UNAVAILABLE` outcomes;
  - consolidated Semantic BELIEFs retain only direct transfer evidence IDs, while each direct evidence event carries its own bounded ancestry snapshot;
  - canonical direct ancestry is selected by `gameTime DESC` then evidence UUID ascending before listener-specific cycle/limit checks, preventing listener-dependent fallback;
  - exact statement and semantic subject scope are preserved across hops, provider/client input cannot inject provenance authority, and downstream knowledge always remains `BELIEF / NPC_TOLD`.
- Deterministic server-owned Semantic contradiction representation:
  - a new structured `SEMANTIC_CONTRADICTION / SYSTEM_OBSERVED` process-evidence event records that two exact retained Semantic claims were classified as contradictory without selecting a winner or changing either claim;
  - shared `SemanticMemoryIdentity` exposes the existing logical consolidation dimensions so contradiction history survives source-union consolidation while existing `semantic-consolidated-v1` IDs remain byte-compatible;
  - each contradiction snapshot stores logical claim identity, exact detected Semantic entry ID, original kind/provenance and canonical semantic subject scope, but deliberately stores no duplicate claim prose;
  - deterministic `semantic-contradiction-v1` identity binds the owner, both complete canonical snapshots and authoritative `gameTime`, and A/B versus B/A ordering is canonical;
  - recording accepts exact server-owned Semantic entry IDs only, rereads both sources authoritatively and exposes explicit `SOURCE_NOT_RETAINED`, `SCOPE_MISMATCH`, `SAME_CLAIM`, `EVENT_NOT_RETAINED` and rejection outcomes;
  - resolved contradiction history returns a relation only while both logical claims remain live, canonically match the stored kind/provenance/scope and are eligible for the current player before limiting, so old process evidence cannot resurrect forgotten claim text;
  - contradiction evidence is excluded from the generic episodic prompt path and cannot be converted into Semantic FACT; FACT/BELIEF/provenance/confidence/ranking of both source claims remain unchanged.
- Dedicated bounded contradiction-aware snapshot prompt context:
  - only currently resolvable, current-player-eligible contradiction relations are loaded, with a hard maximum of four relations per prompt;
  - each relation renders both live Semantic claims with their original FACT/BELIEF kind, provenance and confidence, using the same 240-code-point normalization, reserved-template neutralization and escaping as ordinary Semantic Memory;
  - disagreement is rendered in a separate server-authored layer after Semantic Memory and before episodic/social history, explicitly as remembered data rather than instructions or a truth verdict;
  - current server-observed facts remain authoritative on conflict, and confidence, repetition, corroboration count or rumor depth never promotes a BELIEF to FACT;
  - forgotten, malformed or foreign-player relations disappear before prompt allocation and historical contradiction evidence cannot restore missing claim prose;
  - the disagreement layer is captured immutably on the server thread before asynchronous AI processing and fails soft to empty when unavailable.
- Deterministic server-owned rumor fallibility metadata for retained `BELIEF / NPC_TOLD` Semantic memory:
  - fallibility is derived from retained canonical `npc-knowledge-transfer-v2` provenance after existing player eligibility, bounded candidate selection and rank-to-6 retrieval;
  - resolvable rumors expose `sourcePath=RESOLVED` plus the exact canonical source distance from one through eight hops and `transformationsUsed=0`;
  - a retained rumor whose direct provenance evidence is forgotten or no longer valid exposes `sourcePath=UNRESOLVED` without fabricating a hop distance or reconstructing ancestry;
  - the annotation is rendered inline in the already-selected Semantic slot, so no additional prompt result slot is created and the existing `32 / 24+8 / 6` bounds remain unchanged;
  - FACT, PLAYER_TOLD and INFERRED lines retain their previous rendering, while fallibility guidance is emitted only when selected rumor fallibility metadata is actually present;
  - source distance is process metadata only: it does not rank claims, select a contradiction winner, mutate confidence, promote BELIEF to FACT or weaken current server-observed FACT authority.
- Bounded transformed-claim representation for sourced NPC rumors:
  - the first distortion primitive is server-deterministic `OMIT_TRAILING_SENTENCE`; it may remove one trailing sentence but cannot insert, replace, reorder or invent tokens;
  - a rumor lineage has a hard maximum of one transformation across the existing eight-hop provenance path, and later ordinary transfers carry the exact same immutable transformation snapshot without resetting the budget;
  - original `npc-knowledge-transfer-v2` origin statement and hop ancestry remain intact and inspectable while listener knowledge remains `BELIEF / NPC_TOLD` with unchanged transfer confidence;
  - exact transformed replay is idempotent, a second transformation request returns `TRANSFORMATION_LIMIT_REACHED`, non-applicable single-sentence input returns `TRANSFORMATION_NOT_APPLICABLE`, and conflicting transformed/plain payloads under one existing transfer identity are rejected;
  - retained direct evidence exposes `transformationsUsed=0|1`; forgotten or invalid direct evidence exposes `transformationsUsed=UNKNOWN` rather than reconstructing history from surviving prose;
  - transformed fallibility stays in the already-selected Semantic prompt slot, uses the existing safe statement renderer and never becomes a truth score, authority signal or instruction.
- Bounded automatic Semantic contradiction candidate production after controlled FACT/BELIEF admission:
  - the newly retained post-consolidation logical claim is reread before production and direct low-level `SemanticMemoryStore.append(...)` remains storage-only;
  - candidate eligibility is enforced before allocation using same NPC owner, exact canonical Semantic scope, distinct logical claim identity and non-equivalent normalized text;
  - at most 16 eligible candidates are materialized and at most 8 non-duplicate pairs are classified per admission, preventing all-pairs growth;
  - retained valid contradiction pairs are suppressed before comparison budget and exact replay does not create another live relation;
  - the initial classifier is deliberately conservative and deterministic: it recognizes only one standalone English `not` or Russian `не` polarity insertion/removal, while antonyms, numeric conflicts, free-form paraphrase and trailing-sentence omission remain unclassified;
  - opposing pairs delegate to the existing exact-ID `SemanticContradictionLifecycle`; no provider request, client authority, truth winner, FACT promotion, confidence/importance/provenance mutation or claim deletion is introduced;
  - no provider schema/call, public config, world file, persistence version/field, migration/backfill or release identity changes are required.
- Bounded settlement-scale information flow without omniscience:
  - MCA home-village membership is the only settlement boundary in this slice; no shared village knowledge store or cross-village broadcast is introduced;
  - dissemination runs from the existing staggered loaded-village update path through a minimal `MixinVillage`, rather than a per-NPC tick or new scheduler;
  - one cycle materializes at most 16 residents, considers at most 4 speakers, reads at most 2 retained Semantic source candidates per speaker and attempts at most 4 transfer opportunities;
  - a retained source must predate the current 1200-tick cycle start, preventing newly received claims from cascading through multiple NPCs inside the same cycle;
  - normalized statement plus exact canonical Semantic scope is the per-cycle knowledge key, so equivalent knowledge carried by multiple NPCs has a hard fan-out of one opportunity per cycle;
  - each selected source/cycle maps to one deterministic listener with no fallback retargeting when that target already knows the scoped claim or transfer validation fails;
  - every mutation delegates to the existing exact `NpcKnowledgeTransferLifecycle`; listener knowledge remains local `BELIEF/NPC_TOLD`, source scope is unchanged, v2 ancestry/transformation state is preserved and existing automatic contradiction production runs after listener admission;
  - no provider request/schema/call, public config, world file, persistence version/field, migration/backfill, trust weighting, FACT promotion, confidence/provenance mutation or release identity change is introduced.

### Changed

- Repository governance was simplified after `0.2.0`:
  - ad-hoc commit artifact builds became manual-only in PR #121;
  - the redundant PR Gradle workflow was removed in PR #122;
  - wrapper validation remains in supply-chain verification;
  - the permanent Actions surface is now the fail-closed canonical eight-workflow set.
- New semantic extraction configuration is deliberately safe-by-default:
  - `semanticBeliefExtractionEnabled=false` by default;
  - `semanticBeliefMaxCandidatesPerTurn=3` by default and normalized to the bounded parser limit;
  - existing version-2 configs require no migration because missing fields receive safe defaults.
- Player-scoped Memory 2.0 prompt retrieval now enforces exact current-player-or-NPC-global eligibility before bounded candidate selection:
  - foreign-player Semantic Memory entries cannot consume candidate slots or enter another player's prompt;
  - foreign-player episodic, relationship-change and causal-history events are likewise excluded before ranking;
  - eligible current-player and NPC-global memories retain the existing deterministic ranking and hard bounds.
- Long-horizon candidate selection preserves that same visibility boundary before **both** recent and durable allocation:
  - foreign-player high-durability records consume zero durable slots;
  - another NPC's memory consumes zero slots;
  - NPC-global and shared current-player-plus-other-entity memory remain eligible;
  - candidate/result limits remain `32` / `6`, and existing Semantic/episodic ranker weights are unchanged.
- Snapshot prompt context is composed exactly once in deterministic authority order:
  - current observed world facts first and authoritative for the turn;
  - Operator Lore next as background context;
  - Semantic Memory next with FACT/BELIEF provenance labels preserved;
  - dedicated live contradiction/disagreement context next without truth arbitration;
  - episodic and social-history Memory 2.0 last among memory layers;
  - structured-response/tool instructions remain after all context layers;
  - conflicting BELIEFs remain non-authoritative and stale relationship history does not override the current server-observed relationship state.
- Long-horizon recall changes no persistence format/version, public configuration, provider request/retry behavior, relationship mutation authority or release identity contract; it adds no legacy `memory.json` reader, embeddings/vector database, background summarizer or extra LLM memory-management call.
- NPC-to-NPC knowledge transfer reuses the existing `memory2.json` / `semantic-memory.json` formats, retention policies, Semantic consolidation, player-visibility eligibility and `32` / `24+8` / `6` long-horizon bounds; it adds no provider call, public config, client authority, autonomous visible NPC conversation, multi-hop rumor propagation or legacy migration.
- Provenance-aware multi-hop rumors keep `memory2.json` format version 1 and the current Semantic persistence schema, retention coefficients, retrieval/ranking bounds, provider protocol, public configuration, voice/UI/scheduler/gameplay authority and release identity unchanged; there is no migration, backfill, dual reader, new store, second provider call, uncertainty model, distortion model or autonomous rumor-spread scheduler in this slice.
- Semantic contradiction representation reuses `memory2.json` and `semantic-memory.json` format version 1, existing bounded retention and current player-scope eligibility; it adds no automatic contradiction detector, provider call/schema, public config, new store, migration/backfill, uncertainty/distortion/trust weighting, UI, scheduler or autonomous propagation.
- Contradiction-aware prompt context adds no provider request/schema, public config, new persistence store/version, migration/backfill, automatic detector, winner selection, uncertainty/confidence mutation, distortion, trust weighting, UI, scheduler or autonomous propagation.
- Rumor fallibility is a derived runtime view over retained v2 provenance; it adds no world file, JSON field, config value, migration/backfill, provider request/schema, evidence-ID namespace, confidence decay, trust weighting, automatic detector, transformed wording, UI, scheduler or autonomous rumor propagation.
- Bounded transformed claims keep `memory2.json` at format version 1 and `semantic-memory.json` unchanged; the new transformation field is nullable structured process evidence on transfer events, while provider schema/call count, public config, v2 evidence identity, retrieval bounds, contradiction bounds, FACT admission, release identity, UI and autonomous propagation remain unchanged.
- Settlement information flow reuses current `memory2.json` / `semantic-memory.json`, existing Memory 2.0 capacity, home-village membership and the existing village update cadence; it adds no durable scheduler ledger, settlement-global memory, new client authority or relationship/trust-based epistemic weighting.

### Validation

- Controlled BELIEF admission was developed with a tests-first RED/GREEN boundary in PR #123.
- Bounded player-told BELIEF extraction was developed through explicit RED/GREEN contract tests in PR #125; exact-head CI/release evidence is recorded in that PR.
- Causal relationship memory in PR #127 was developed through staged RED/GREEN contracts for structured transition state, persisted-source cause admission, result-bearing ChatAI orchestration and restart/eviction-safe query behavior; a full-history test exposed and drove a deterministic retention-ordering fix before final verification.
- FACT-over-BELIEF retrieval precedence in PR #129 uses separate observed RED/GREEN gates for semantic player isolation, episodic/social-history player isolation, snapshot memory de-duplication, four-layer prompt composition and direct provider wiring.
- Long-horizon recall in PR #131 uses separate observed RED/GREEN gates for retained-but-starved Semantic recall, the pure bounded candidate selector, episodic FIFO pressure loss, the pure episodic retention policy, no-op rejected persistence writes and retained-but-starved episodic recall.
- Long-horizon preservation evidence additionally exercises multi-day Minecraft game time, repeated persistence reloads, exact survivor/context equality, mixed two-NPC/two-player/shared-scope pressure and deterministic hundreds-of-record simulations without sleeps or wall-clock-dependent assertions.
- NPC-to-NPC knowledge transfer in PR #133 uses observed compile RED gates for exact store authority lookup, canonical evidence/policy APIs and lifecycle API; an additional behavioral RED compiled successfully and failed exactly the two source-backed transfer assertions before the lifecycle implementation was added.
- PR #133 preservation coverage exercises fail-closed ownership/input boundaries, byte-idempotent replay, corroborating Semantic source union, explicit partial-retention outcomes, fresh-root reload, global/private/shared scope, player Working Memory isolation, independent NPC pairs and deterministic long-horizon multi-NPC pressure without wall-clock-dependent expected behavior.
- Provenance-aware rumor coverage in PR #135 exercises immutable persisted lineage, deterministic v2 identity, first-hop origin restrictions, exact retained-branch resolution, listener-independent no-fallback behavior, cycle-before-limit precedence, eight-hop bounds, field-by-field provenance mutation rejection, historical-v1/missing-direct-evidence fail-closed behavior, byte-idempotent replay after fresh-root reload, global/private/shared scope preservation, player Working Memory isolation, bounded forgetting/direct-evidence loss and a deterministic 10-NPC pressure/reload simulation.
- Semantic contradiction representation in PR #137 uses separate observed compile RED gates for stable logical claim identity, structured event model/prompt isolation, canonical adapter/integrity policy, exact-ID lifecycle and live resolved history. Preservation-only coverage then exercises fresh-root restart, source-union consolidation, forgetting without claim resurrection, global/private/shared privacy before limiting, exact replay, bounded event rejection, malformed-evidence filtering, no duplicate claim prose, 240 Semantic + 240 episodic pressure records and forward/reverse deterministic snapshots without requiring a preservation production correction.
- Contradiction-aware prompt context in PR #139 uses observed RED gates for shared safe claim rendering, bounded live disagreement formatting/provider loading, immutable snapshot capture and five-layer prompt wiring; preservation coverage adds 240 Semantic + 240 episodic pressure records, fresh-root reload, prompt-injection escaping and unchanged current-observed-fact authority without a preservation production correction.
- Deterministic rumor fallibility in PR #141 uses observed RED gates for the pure fallibility state, retained-source resolver, inline selected-Semantic annotation and conditional prompt guidance. Preservation coverage exercises an exact eight-hop chain, >200 Semantic and >200 episodic pressure records, high-score foreign-player private noise, fresh-root reload equality, direct-evidence forgetting to explicit `UNRESOLVED`, existing prompt-injection escaping and unchanged current FACT authority without a preservation production correction.
- Bounded transformed-claim work in PR #143 uses separate observed RED gates for deterministic transformation state, additive evidence persistence, canonical provenance propagation, server-owned lifecycle admission, honest fallibility state and prompt rendering. Preservation coverage exercises one interior transformation across an exact eight-hop chain, >200 Semantic and >200 episodic pressure records, foreign-player privacy-before-limit, safe prompt escaping, fresh-root replay/equality, lineage-wide budget persistence, unchanged downstream propagation, direct-evidence forgetting and fail-closed `PROVENANCE_UNAVAILABLE` without a preservation production correction.
- Bounded contradiction candidate/producer work in PR #145 uses separate observed RED gates for bounded candidate selection, conservative opposition classification, producer/replay semantics and automatic controlled-admission integration. Preservation coverage exercises foreign-scope filtering before allocation, 16-candidate/8-comparison hard bounds, 10-NPC/>240-record pressure, forgetting without contradiction-prose resurrection, exact fresh-root reload and transformed trailing-sentence non-opposition; fixture-only ordering/API mistakes were corrected without production changes.
- Settlement-scale information flow in PR #147 uses observed compile RED gates for bounded opportunity selection, exact transfer lifecycle orchestration and the runtime adapter. A strengthened behavioral RED then proved that equivalent scoped knowledge carried by multiple NPCs could over-fan-out in one cycle and drove canonical statement+scope knowledge-key suppression. Preservation coverage exercises same-cycle anti-cascade, no-fallback replay, exact private scope, v2 ancestry, transformation carry-forward, contradiction production, fresh-root replay, later-cycle gradual spread, 12 settlements × 24 residents under hundreds of retained claims and supported-loader production startup; invalid PLAYER_TOLD/scope fixtures were corrected without weakening production authority policies.
- Existing current-FACT/current-relationship-state precedence tests remain green with long-horizon retrieval; transferred entries remain explicitly `BELIEF | provenance=NPC_TOLD` and retain the existing current-observed-fact-wins prompt framing.
- Real two-graphical-client Operator Lore acceptance `VAI-CONCUR-004` remains `NOT TESTED / DEFERRED` until two graphical clients are available.

---

## [0.2.0+1.21.1] — 2026-08-07

### Added

- Memory 2.0 became the sole persistent dialogue-memory source.
- Structured `DialogueExchange(playerMessage, npcReply)` payloads are persisted in DIALOGUE events.
- Exact NPC/player dialogue retrieval filters eligible events before limiting and reconstructs chronological user/assistant history.
- Current auxiliary corruption/recovery coverage uses five stores:
  - `memory2.json`;
  - `semantic-memory.json`;
  - `relationships.json`;
  - `voices.json`;
  - `operator-lore.json`.

### Changed

- The experimental pre-0.2 `memory.json` conversation store was removed from the current runtime and recovery matrix.
- The pre-1.0 rollout boundary is intentionally clean-state: no importer, dual reader, checkpoint ledger, or legacy conversation migration is provided.
- Release recovery is version-aware so immutable historical releases keep their own persistence contracts.

### Validation

Exact installed Memory 2.0 acceptance for the byte-identified candidate:

```text
VAI-M2-INST-001  PASS
VAI-M2-INST-002  PASS
VAI-M2-INST-003  PASS
VAI-M2-INST-004  PASS
VAI-M2-INST-006  PASS
VAI-M2-INST-007  PASS
VAI-M2-INST-008  PASS

Required total: 7 PASS / 0 FAIL
VAI-M2-INST-005: NOT TESTED / AUTOMATED EVIDENCE ONLY
VAI-CONCUR-004:  NOT TESTED / DEFERRED
```

The physical voice seed `silver-fox-482` was accepted by STT as `SilverFox482`; Memory 2.0 persisted and recalled the accepted transcript across restart. This is retained as a non-blocking STT-normalization observation rather than a memory failure.

Candidate/runtime JAR SHA-256 used by the installed evidence:

```text
56293f86634b50b2def044429aac6f2cf0d197eb16ac1e60224708f7b3333aee
```

---

## [0.1.26+1.21.1] — 2026-08-06

### Added

- Completed M11 Phase E automation across production staging, identity lifecycle, persistence recovery, authenticated sessions, voice transport, gameplay/navigation GameTests, fail-closed risk selection, and constrained-heap soak.
- Added immutable release-recovery automation after a GitHub Actions publication outage.

### Validation

Installed canaries on the exact accepted release bytes:

```text
VAI-BOOT-002    PASS
VAI-NAV-001     PASS
VAI-GAME-001    PASS
VAI-GAME-003    PASS
VAI-AI-006      PASS
VAI-CONCUR-004  NOT TESTED / DEFERRED

Total: 5 PASS / 0 FAIL / 1 NOT TESTED
```

Release JAR SHA-256:

```text
5728f0f1a57b4c268df9b73603539f09ca30945a2ba251e72a5169ab45ae0a53
```

---

## Earlier history

Detailed implementation/TDD/security history for the earlier `0.1.x` development line remains preserved in `docs/CHANGELOG.md` and version-specific validation documents under `docs/livingworld/` and `docs/security/`.
