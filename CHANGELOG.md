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
- Snapshot prompt context is composed exactly once in deterministic authority order:
  - current observed world facts first and authoritative for the turn;
  - Operator Lore next as background context;
  - Semantic Memory next with FACT/BELIEF provenance labels preserved;
  - episodic and social-history Memory 2.0 last among memory layers;
  - structured-response/tool instructions remain after all context layers;
  - conflicting BELIEFs remain non-authoritative and stale relationship history does not override the current server-observed relationship state.

### Validation

- Controlled BELIEF admission was developed with a tests-first RED/GREEN boundary in PR #123.
- Bounded player-told BELIEF extraction was developed through explicit RED/GREEN contract tests in PR #125; exact-head CI/release evidence is recorded in that PR.
- Causal relationship memory in PR #127 was developed through staged RED/GREEN contracts for structured transition state, persisted-source cause admission, result-bearing ChatAI orchestration and restart/eviction-safe query behavior; a full-history test exposed and drove a deterministic retention-ordering fix before final verification.
- FACT-over-BELIEF retrieval precedence in PR #129 uses separate observed RED/GREEN gates for semantic player isolation, episodic/social-history player isolation, snapshot memory de-duplication, four-layer prompt composition and direct provider wiring.
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
