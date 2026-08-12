# Query-aware Memory Recall — implementation plan

Date: 2026-08-13
Target: `0.3.1+1.21.1`
Base: `0.3.0+1.21.1` / `d42141511c0c61f10256fd06576f977f2a784d1c`

## Problem

Installed acceptance exposed a real `VAI-PCM-MULTI-001` recall failure: an older NPC-local dialogue marker remained persisted but was not available to the answering turn after the NPC accumulated a denser history.

The current episodic retrieval path is bounded and deterministic, but its relevance signal is structural only (participant/type). The current player question is absent from `MemoryQuery`, so an older question-relevant dialogue can be excluded by the 32-candidate selector and/or outranked inside the six-result prompt window.

Separately, `VAI-PROX-MULTI-001` was specified against unsupported behavior. VillAIgence selects the current voice conversation target explicitly through NPC interaction; proximity alone must not silently retarget the player.

## Constraints

- TDD: observe a real RED before production changes.
- Preserve `candidateLimit=32` and `maxResults=6`; do not solve recall by expanding prompt windows.
- Apply NPC/player eligibility before any relevance ranking.
- Never introduce cross-NPC or cross-player fallback.
- Retrieval remains pure/deterministic/provider-independent; no LLM relevance authority, embeddings, or vector database.
- Preserve VERIFIED/BELIEF provenance and prompt ordering.
- Keep old retrieval APIs source-compatible where practical.
- No persistence/config schema migration.
- Correct `VAI-PROX-MULTI-001` to the actual interaction-target UX.
- `0.3.0+1.21.1` remains immutable; ship the runtime correction as `0.3.1+1.21.1` only after repository gates pass.

## Tasks

1. Add regression tests proving an old query-relevant dialogue survives a crowded newer history while owner/player isolation remains strict.
2. Observe expected RED on the tests-only head.
3. Add a bounded normalized query-text signal to episodic retrieval.
4. Reserve bounded candidate capacity for query-relevant eligible events before final deterministic ranking; keep the existing recent/durable guarantees for the remainder.
5. Thread the current player message into both classic text and snapshot/voice server-thread memory capture without moving store reads into async provider code.
6. Add/adjust source and integration tests for both dialogue routes, hard bounds, determinism, and isolation.
7. Correct installed-acceptance documentation for `VAI-PROX-MULTI-001`; record the observed `VAI-PCM-MULTI-001` failure without claiming a post-fix installed PASS.
8. Update Memory 2.0 docs and CHANGELOG.
9. Run exact-head CI/security/compatibility gates and independent review.
10. Prepare `0.3.1+1.21.1`; after publication, require a focused real-server re-test before closing installed acceptance.

## Acceptance

Automated acceptance is GREEN only when:

- an old relevant NPC/player dialogue can displace a less relevant newer candidate without exceeding 32 candidates / 6 prompt results;
- blank/no-query behavior remains deterministic and bounded;
- foreign-player and foreign-NPC memories are never eligible;
- classic and snapshot/voice paths both supply the current message to episodic retrieval on the server-owned capture path;
- no persistence/config/provider authority boundary changes;
- repository security and loader compatibility gates pass.

Installed acceptance remains incomplete until the official `0.3.1+1.21.1` artifact passes the affected real-server recall scenario.
