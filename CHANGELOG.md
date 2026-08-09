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
  - episodic and social-history Memory 2.0 last among memory layers;
  - structured-response/tool instructions remain after all context layers;
  - conflicting BELIEFs remain non-authoritative and stale relationship history does not override the current server-observed relationship state.
- Long-horizon recall changes no persistence format/version, public configuration, provider request/retry behavior, relationship mutation authority or release identity contract; it adds no legacy `memory.json` reader, embeddings/vector database, background summarizer or extra LLM memory-management call.
- NPC-to-NPC knowledge transfer reuses the existing `memory2.json` / `semantic-memory.json` formats, retention policies, Semantic consolidation, player-visibility eligibility and `32` / `24+8` / `6` long-horizon bounds; it adds no provider call, public config, client authority, autonomous visible NPC conversation, multi-hop rumor propagation or legacy migration.
- Provenance-aware multi-hop rumors keep `memory2.json` format version 1 and the current Semantic persistence schema, retention coefficients, retrieval/ranking bounds, provider protocol, public configuration, voice/UI/scheduler/gameplay authority and release identity unchanged; there is no migration, backfill, dual reader, new store, second provider call, uncertainty model, distortion model or autonomous rumor-spread scheduler in this slice.

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