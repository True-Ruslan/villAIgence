# Long-Horizon Recall TDD Evidence

Date: 2026-08-08
Branch: `feat/long-horizon-recall`
Base: `b09924d7297775baabf577ca50dbcb65c22f0516`

This ledger records observed RED→GREEN evidence. A stage is not marked GREEN until the corresponding CI run is inspected.

## Design / plan gates

- Design spec initial commit: `f08a631fe4c9aaeb2f283ec6506f05e70be67309`
- Design self-review clarification: `3b010c992687d2d0cfa39ba199ee3e33cb7eddc7`
- Implementation plan: `84f12b78ce4bdcf047c86c2025c08a52b8d49c0c`

## RED 1 — Semantic retained-but-starved recall

- Behavioral tests-only commit: `b13ac7a84cccc2ad5c50826db935129168f7cb95`
- PR-triggered observed RED head: `806d78862d8fc7ce559dd9c2ce4baaf7164db6e8` (test + documentation only relative to base; no `common/src/main/**` changes)
- Production code changed for this slice before RED observation: **NO**
- Failing contract: `SemanticMemoryRetrieverTest.contextProviderRecallsOldDurableSemanticMemoryAfterNewerEligibleWindowAndReload`
- Intended failure: old high-durability Semantic BELIEF is physically persisted, survives a real file round-trip into a fresh world root, but current `SemanticMemoryContextProvider` takes only the newest 32 eligible entries before ranking, so the old record cannot enter prompt context.
- VillAIgence CI: run `31256528526`, run number `2023` — **FAILURE as expected**.
- `:common:compileJava` — PASS.
- `:common:compileTestJava` — PASS.
- `:common:test` — **526 tests / exactly 1 failed**.
- Exact failure: `SemanticMemoryRetrieverTest > contextProviderRecallsOldDurableSemanticMemoryAfterNewerEligibleWindowAndReload() FAILED`, assertion at `SemanticMemoryRetrieverTest.java:161`.
- Acceptance-suite selector contract, release-identity preflight and repository-security step inside Main CI — PASS before the expected test failure.
- Independent Repository security policy run `31256528503` / #1658 — SUCCESS.
- Downstream GameTests/production/recovery/package stages in Main CI were correctly skipped after the expected common-test RED.

### RED 1 conclusion

The test compiles and fails for the intended missing behavior rather than for fixture/setup failure. The persistent old record is asserted present before the final context assertion. Semantic long-horizon production work became permitted, subject to the pure-selector API RED.

## RED 1b — pure bounded candidate-selector API

- Tests-only selector commit: `5d14c0ab7750e65287964424a12b59325a93377e`.
- VillAIgence CI: run `31256674878`, run number `2027` — **FAILURE as expected**.
- `:common:compileJava` — PASS.
- `:common:compileTestJava` — FAIL with six `cannot find symbol: LongHorizonCandidateSelector` errors from `LongHorizonCandidateSelectorTest`.
- Production selector did not exist before this compile RED.

Minimal selector implementation commit: `1e31923879962c2be652fb81f24723461435757d`.

The first implementation run, CI `31256810773` / #2029, compiled but exposed two selector-test failures plus the intentionally still-red Semantic provider test. Systematic debugging traced both selector failures to the test comparator expression `...thenComparingLong(...).reversed()`, which reverses the entire comparator constructed so far and therefore inverted the durability primary order. The production selector was not changed.

- Test-only comparator correction: `a5fd8831d27ae724bb766cc51d2761cf8190ba09`.
- CI `31256991307` / #2031: **531 tests / exactly 1 failed**.
- All five `LongHorizonCandidateSelectorTest` cases passed.
- The only remaining failure was the original Semantic behavioral RED, proving the pure selector GREEN while provider wiring was still absent.

## GREEN 1 — Semantic long-horizon recall

- Minimal provider wiring commit: `c4176cad8fd767c87d3a9f650e128dc4774b1c3d`.
- Semantic persistence policy unchanged.
- Candidate limit unchanged at `32`; prompt result limit unchanged at `6`.
- Existing semantic ranker weights unchanged.
- `SemanticMemoryContextProvider` now performs exact player/NPC eligibility over the already hard-bounded store, applies bounded dual-tier candidate selection, then uses the existing ranker/formatter.
- Normal selector allocation: `24` recent + `8` durable.
- Semantic durable ordering uses existing `SemanticMemoryRetentionPolicy.effectiveRetentionScore(entry, currentGameTime)` and deterministic tie-breakers.

Exact-head evidence:

- Repository security policy run `31257132977` / #1668 — SUCCESS.
- VillAIgence CI run `31257132985` / #2033 — SUCCESS.
  - common/deterministic mock-provider tests — SUCCESS;
  - risk catalog, required server GameTests and supported loader builds — SUCCESS;
  - production acceptance contract tests — SUCCESS;
  - production startup/restart acceptance — SUCCESS;
  - distributable Fabric package verification — SUCCESS;
  - persistence-recovery stage was not selected for this exact risk set and therefore SKIPPED, not represented as PASS.

### GREEN 1 conclusion

The old high-durability Semantic record remains persisted, survives a fresh-world file round-trip, can enter the bounded recall candidate set after more than one window of newer eligible memories, and all existing Semantic privacy/ranking/bounds tests remain green. Task 1 is complete.

## RED 2 — Episodic/social pressure retention

Pending. Production `MemoryEventStore` still uses chronological FIFO pressure; no `MemoryEventRetentionPolicy` exists yet.
