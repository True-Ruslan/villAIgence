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

The test compiles and fails for the intended missing behavior rather than for fixture/setup failure. The persistent old record is asserted present before the final context assertion. Semantic long-horizon production work is now permitted, subject to the next pure-selector API RED.

All later RED/GREEN stages will be appended after their exact run evidence is inspected.
