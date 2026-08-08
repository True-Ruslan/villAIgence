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

- Tests-only head: `b13ac7a84cccc2ad5c50826db935129168f7cb95`
- Production code changed for this slice before RED observation: **NO**
- Added failing contract: `SemanticMemoryRetrieverTest.contextProviderRecallsOldDurableSemanticMemoryAfterNewerEligibleWindowAndReload`
- Intended failure: old high-durability Semantic BELIEF is physically persisted, survives a real file round-trip into a fresh world root, but current `SemanticMemoryContextProvider` takes only the newest 32 eligible entries before ranking, so the old record cannot enter prompt context.
- CI run/result: pending observation.

All later RED/GREEN stages will be appended after their exact run evidence is inspected.
