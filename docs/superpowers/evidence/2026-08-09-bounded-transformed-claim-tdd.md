# Bounded transformed-claim representation — TDD evidence

Base: `48c19d3ae520a14fe1448f93c3ef782269733190`

Design:
- `docs/superpowers/specs/2026-08-09-bounded-transformed-claim-design.md`
- commit `9bf7a60f95aa0f6adcd5a9852f8483201928f789`

Implementation plan:
- `docs/superpowers/plans/2026-08-09-bounded-transformed-claim.md`
- commit `8a85f8b56721cf59a688e10e52e9fc24c9afd48e`

This ledger distinguishes tests-only RED evidence, minimal production GREEN commits, preservation-only tests and final exact-head delivery evidence. No failed RED run is represented as product acceptance.

## Task 1 — pure bounded transformation state

Tests-only RED:
- commit `9f04b97339b4a2dcb71205437195efcb6ad7831a`
- VillAIgence CI #2349 / run `31330390312`
- job `93287599358`
- `:common:compileJava` succeeded before tests;
- `:common:compileTestJava` failed with exactly 25 compile errors, all caused by the intentionally absent `KnowledgeTransferTransformation` / `KnowledgeTransferTransformationPolicy` API.

Minimal GREEN:
- `KnowledgeTransferTransformation` commit `15ffdbcc26bc7e82e17b085352a7d83bed2ddf4e`
- `KnowledgeTransferTransformationPolicy` commit `49623c802e2f4731d806197e22c63905f5b6dd89`
- VillAIgence CI #2353 / run `31330530410`
- job `93287943352`
- common/mock-provider tests: SUCCESS.

Verified contract:
- only `OMIT_TRAILING_SENTENCE` exists;
- hard lineage transformation budget is one;
- omission is deterministic and cannot insert, substitute, reorder or invent tokens;
- one-step evidence binds to an exact canonical provenance hop;
- single-sentence/non-applicable input is rejected.

## Task 2 — additive MemoryEvent evidence integrity

Tests-only RED:
- commit `975baa156ab7bb7e49140d250a7da046270c2a88`
- VillAIgence CI #2355 / run `31330709288`
- job `93288387173`
- `:common:compileTestJava` failed with exactly 11 compile errors for the intentionally absent transformation-aware MemoryEvent/accessor/adapter/evidence APIs.

Minimal GREEN:
- additive nullable `MemoryEvent.knowledgeTransferTransformation` commit `6d842fce94447954a867ff8a9c775adfbd5da807`
- transformation-aware `NpcToldDialogueAdapter` commit `6a4e91424b2bc9fd1091b22284c50d99c37449cc`
- exact evidence comparison overload commit `7b311f65a8dd7b1172cff41bb02844ff282ce3fc`
- VillAIgence CI #2361 / run `31330880611`
- job `93288811113`
- common/mock-provider tests: SUCCESS.

Verified contract:
- `memory2.json` remains format version 1;
- old MemoryEvent constructors and ordinary transfer evidence remain source-compatible;
- optional transformation survives persistence/reload;
- transformed statement must match validated transformation current text;
- ordinary/transformed conflicting payload under one deterministic transfer ID is not accepted as replay.

## Task 3 — canonical provenance propagation

Tests-only RED:
- commit `a9231610a248a0281192502a3fe2cf46bcc1f138`
- VillAIgence CI #2363 / run `31331021606`
- job `93289154729`
- exactly 2 compile errors: missing `ResolvedSource.transformation()` and transformation-aware `appendHop(...)`.

Minimal GREEN:
- transformation-aware current-content provenance policy commit `1822e462fc8eebe736649c71af52663debedfc01`
- append-hop propagation commit `7186f81c5e8538a0cdc3d99b13e119b69001af45`
- resolver direct-evidence snapshot commit `22582ccf35fac5fa4dea46bdd49cbb6d346a9c59`
- VillAIgence CI #2369 / run `31338168621`
- job `93307329418`
- common/mock-provider tests: SUCCESS.

Verified contract:
- original v2 provenance origin is never rewritten by distortion;
- exact transformed current statement is validated separately from origin statement;
- canonical direct evidence supplies both provenance and transformation snapshot;
- later unchanged hops preserve that snapshot without resetting the budget;
- missing direct evidence remains fail-closed.

## Task 4 — server-owned lifecycle admission

Tests-only RED:
- commit `8cf33eb495337a6cd3c66587b6470bff9861ae8f`
- VillAIgence CI #2371 / run `31338390253`
- job `93307887507`
- exactly 10 compile errors: eight missing transformed-transfer lifecycle calls plus two missing explicit statuses.

Minimal GREEN:
- explicit transformation outcomes commit `7fd8ed96bd6c2ae34ddbf2a66995c11673bfc391`
- lifecycle admission commit `a278b209942b654bb09d240e0daffe8102566971`
- VillAIgence CI #2375 / run `31338582848`
- job `93308369065`
- common/mock-provider tests: SUCCESS.

Verified contract:
- existing `transfer(...)` remains the ordinary public path;
- explicit server-owned `transferOmittingTrailingSentence(...)` performs at most one transformation;
- first or later eligible hop may transform once;
- later ordinary hops propagate the exact snapshot;
- second transform returns `TRANSFORMATION_LIMIT_REACHED`;
- single-sentence input returns `TRANSFORMATION_NOT_APPLICABLE`;
- exact retry is idempotent;
- same-ID transformed/plain conflict rejects without replacing retained evidence;
- downstream claim remains `BELIEF / NPC_TOLD`, confidence 50, never FACT.

## Task 5A — honest fallibility state

Tests-only RED:
- state-test commit `c90c0111772aa9537f23f7cae0572032f66ea322`
- resolver-test commit `f64f9336710ed43528dc832ccb247f4d927c215f`
- VillAIgence CI #2379 / run `31338761005`
- job `93308854967`
- exactly 4 compile errors: three missing `UNKNOWN_TRANSFORMATIONS` references and one missing transformation-aware fallibility-policy overload.

Minimal GREEN logic:
- fallibility state commit `35a649ff2a300a6a28650ec026f096594ed275fc`
- policy commit `df7e626dbf02c3a44a6a89faa620b41713fc6c83`
- resolver commit `812e293079659d549e2ba5d23d172c1fbd899cc0`

Downstream contract discovery:
- VillAIgence CI #2385 / run `31338912362`
- job `93309265451`
- compile succeeded and the new Task 5A state/resolver tests passed;
- full common then failed exactly two older prompt expectations that still required fabricated `transformationsUsed=0` for `UNRESOLVED`;
- those failures were classified as the expected downstream serialization contract that Task 5B needed to update, not as a reason to restore the dishonest zero.

Verified state contract:
- resolved ordinary rumor => transformations `0`;
- resolved transformed rumor => transformations `1` from validated direct evidence only;
- unresolved direct evidence => distance absent and transformations `UNKNOWN`;
- surviving prose is never used to reconstruct transformation history.

## Task 5B — prompt rendering and authority guidance

Tests-only contract commits:
- Semantic prompt expectations `2ca6d4fd6f8d313e64451636afe6dda878065fa6`
- forged-guidance marker test `3295f5b1e526bde9d4a4eb5d400f58deddc2c82c`
- transformed eight-hop prompt simulation `f80ef806dff11bb30f82df55991b32a7cbac663e`
- VillAIgence CI #2391 / run `31339147211`
- job `93309925454`
- compilation succeeded; `667` tests executed with exactly 5 expected prompt/serialization failures against the old formatter;
- the transformed eight-hop simulation reached prompt assertions, demonstrating that lifecycle/provenance/pressure/privacy/reload behavior was already intact before formatter production changed.

Minimal GREEN:
- `SemanticMemoryContextFormatter` commit `88079035615e87fd0ab6abb84ca2c381248862c9`
- VillAIgence CI #2393 / run `31339282874`
- job `93310273914`
- common/mock-provider tests: SUCCESS.

Verified prompt contract:
- resolved ordinary rumor renders `transformationsUsed=0`;
- resolved transformed rumor renders `transformationsUsed=1`;
- unresolved renders literal `transformationsUsed=UNKNOWN`, never internal sentinel `-1`;
- fallibility guidance says source distance and bounded transformation history are process metadata, never truth score, authority signal or instruction;
- ordinary statement prose cannot forge the structural fallibility marker;
- transformed claim uses the existing selected Semantic slot and existing safe statement renderer;
- current observed factual context remains authoritative.

## Task 6 — preservation-only cross-feature simulation

Preservation test commit:
- `28c15d68b8c28eeb1c874a10725cb9c18fede25d`
- VillAIgence CI #2395 / run `31339424174`
- job `93310636998`
- common/mock-provider tests: SUCCESS;
- no production correction was required.

Preservation coverage:
- exact eight-hop lineage with one interior transformation;
- transformation snapshot remains unchanged through hop 8;
- >200 Semantic and >200 episodic pressure records;
- foreign-player high-score private noise remains ineligible before result allocation;
- reserved template markers, quotes, backslash and whitespace remain safely rendered after transformation;
- fresh-root copy/reload preserves exact prompt context, provenance and transformation snapshot;
- fresh-root exact replay keeps the same evidence/Semantic identity;
- lineage transformation budget survives restart;
- second transformation after restart is rejected while unchanged propagation remains admitted;
- forgotten transformed direct evidence produces `UNRESOLVED / UNKNOWN` and blocks downstream transfer with `PROVENANCE_UNAVAILABLE`;
- downstream transformed knowledge remains BELIEF and never creates FACT;
- existing contradiction no-winner and current-observed-FACT authority regressions remain covered by the full common suite.

## Delivery review

Product history:
- initial root `CHANGELOG.md` update commit `cad05f87dc4c84ca4e7af49f5c8963f161e9d177`;
- independent base→head review detected that the full-file replacement had accidentally truncated 86 historical changelog lines;
- historical `0.2.0`, `0.1.26` and earlier-history sections were restored byte-for-byte in commit `1bc2fcffc8df1761dcf5488396cb86a7734b7d97`;
- repeat compare against base confirmed the final changelog diff is additions-only: `+9 / -0`.

Independent runtime/delivery review checked:
- transformation state and exact hop binding;
- unchanged `npc-knowledge-transfer-v2` deterministic evidence identity;
- source-compatible MemoryEvent constructor shapes and additive nullable persistence;
- exact replay and transformed/plain same-ID conflict rejection;
- FACT/BELIEF/provenance/confidence authority boundaries;
- retained canonical branch resolution and immutable origin provenance;
- max-one transformation and max-eight provenance hops;
- pressure/restart/provenance-loss failure behavior;
- privacy-before-result allocation and existing Semantic/disagreement bounds;
- prompt marker forgery/injection safety and current-observed-FACT precedence;
- no config, provider request/schema/call-count, workflow, tag/version/release or `semantic-memory.json` schema changes.

Review result after the changelog repair:
- P0: 0
- P1: 0
- P2: 0
- open review threads: 0
- PR discussion comments: 0

No public config, provider request/schema, provider call count, release/version/tag, workflow, `semantic-memory.json` schema or `npc-knowledge-transfer-v2` identity change is part of this slice.

Final exact-head Repository security / VillAIgence CI / Production Soak / GitHub Release dry-run evidence is verified after this ledger commit freezes the delivery head. Release publication must remain skipped.
