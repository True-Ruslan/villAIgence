# NPC-to-NPC Knowledge Transfer TDD Evidence

Date: 2026-08-08
Branch: `feat/npc-to-npc-knowledge-transfer`
Implementation base: `8d8c833d6cdf69d31265eeb9bab925ad010db033`
Base product branch at design time: `a20d6d0ebf5688e790fedeb3563f24f69e7e9c95`
Approved spec head: `bbb33022a86cc58ac429a1de991c5bcfc26fb972`
PR: #133

This ledger separates intentional RED evidence, minimal GREEN production changes, preservation-only GREEN test stages, and final exact-head delivery evidence. Official installed-release evidence remains `0.2.0+1.21.1`; everything in this PR is unreleased development/candidate evidence until a later immutable release is explicitly accepted.

## Design / plan gates

- Approved design: `docs/superpowers/specs/2026-08-08-npc-to-npc-knowledge-transfer-design.md`.
- Final approved spec head: `bbb33022a86cc58ac429a1de991c5bcfc26fb972`.
- Implementation plan: `docs/superpowers/plans/2026-08-08-npc-to-npc-knowledge-transfer.md`.
- Plan commit / implementation branch base: `8d8c833d6cdf69d31265eeb9bab925ad010db033`.
- User explicitly approved the written spec and the recommended execution approach before runtime work.

## RED 1 — exact authority lookup APIs

Tests-only commit: `be7c40e0afaa536c572eefb9a176bac753c1f3e4`.

Production exact-lookup API changed before RED: **NO**.

Observed CI:

- VillAIgence CI #2083 / run `31281198937` — **FAILURE as expected**.
- Release-identity preflight and repository-security steps preceding common tests — PASS.
- `:common:compileTestJava` — **FAILURE as expected**.
- Exact failure class: 13 `cannot find symbol` errors for absent `MemoryEventStore.findById(...)`, `SemanticMemoryStore.findById(...)`, and `SemanticMemoryStore.findMatching(...)`.

The tests prove authority reads must use exact owner+UUID/predicate lookup and must not depend on a newest-window approximation.

## GREEN 1 — exact authority lookup APIs

Production commits:

- `ee1d2861b63e31216e19ef45b14cd6d8a90e8bbc` — exact `MemoryEventStore.findById`.
- `d190068d26cb81759075855ad58169162cea2e31` — exact `SemanticMemoryStore.findById/findMatching`.

No load/save format, retention rule, ranking or capacity behavior changed.

Observed CI:

- VillAIgence CI #2087 / run `31281360267`.
- `common + deterministic mock-provider tests` — **SUCCESS**.

## RED 2 — canonical transfer evidence and pure policy contracts

Tests-only chain:

- `c2802fba5720ef04f4cf205ef7b0d8fca30a3d0d` — require shared semantic statement normalization boundary.
- `6be0d7dd15ec20b4909f0f19f0b5dc82d4694ace` — require canonical deterministic NPC_TOLD evidence adapter.
- `b18551e1a75562d672c056d8c89f8daded7135dd` — require exact fail-closed transfer policy.

Production adapter/policy/helper changed before RED: **NO**.

Observed CI:

- VillAIgence CI #2093 / run `31281533934` — **FAILURE as expected**.
- `:common:compileTestJava` failed with 39 expected missing-symbol errors for `normalizeAndLimitStatement`, `NpcToldDialogueAdapter`, and `NpcKnowledgeTransferPolicy`.
- Production compile and preflight/security checks preceding test compilation were not the failure source.

## GREEN 2 — canonical adapter and fail-closed policy

Production commits:

- `294886fb156f0198116125c827131c6905a3285e` — expose the existing semantic normalization/bound as a package-visible pure helper without changing normalization behavior.
- `c65c0dac77a79d5abb65d506564315617e4d4b53` — canonical listener-owned NPC_TOLD DIALOGUE evidence and versioned deterministic UUID.
- `b0b67a56646c4892d6f1446b62463d4f0b29ac51` — exact request/source/evidence/retained-belief policy checks.

Observed CI:

- VillAIgence CI #2099 / run `31281700786`.
- `common + deterministic mock-provider tests` — **SUCCESS**.

The mutation matrix covers event UUID, owner, participant order/count, type, provenance, game time, created-at metadata, fixed importance/emotion/confidence, relationship fields, structured dialogue payload and canonical summary. Invalid variants are rejected rather than reparsed from prose.

## RED 3a — lifecycle/result API absent

Tests-only commit: `1af91132abbdb9ce264f20d98ba7d9be897d3896`.

Lifecycle/result production classes changed before RED: **NO**.

Observed CI:

- VillAIgence CI #2101 / run `31281822282` — **FAILURE as expected**.
- `:common:compileTestJava` — 6 expected missing-symbol errors for `NpcKnowledgeTransferResult` and `NpcKnowledgeTransferLifecycle`.

## RED 3b — behavioral source-backed transfer

Minimal API-shell commits:

- `f0038439376497f8449165444a0242a1188cb4a4` — immutable result/status surface only.
- `135dae1212ef9694819dd6d01b97945d55c70ca6` — lifecycle shell intentionally returning `REJECTED` only.

No successful transfer behavior existed at this head.

Observed CI:

- VillAIgence CI #2105 / run `31281965108` — **FAILURE as expected**.
- Production/test compilation — PASS.
- `:common:test` — **557 tests / exactly 2 failed**.
- Exact failures:
  - `transfersPersistedSpeakerFactAsListenerNpcToldBeliefWithExactEvidence`;
  - `transfersSpeakerPlayerToldBeliefAsListenerNpcToldWithoutUpstreamProvenanceOrSources`.
- Both failed at the expected `ADMITTED` assertion because the shell returned `REJECTED`; the other 555 common tests passed.

This is the behavioral RED proving the implementation was not inferred from a compile-only failure.

## GREEN 3 — source-backed authoritative transfer lifecycle

Minimal production commit: `c1136a2f46f686f20e7aff55e0e46ea411e0937d`.

Implemented order:

```text
exact speaker Semantic lookup
→ authoritative exact reread/snapshot validation
→ canonical NPC_TOLD evidence construction
→ MemoryEventStore append
→ exact evidence reread + full canonical validation
→ generic SemanticBeliefAdmissionPolicy
→ existing controlled Semantic BELIEF ingestion
→ retained post-consolidation listener BELIEF reread
→ explicit result status
```

Preserved boundaries:

- speaker FACT/BELIEF always becomes listener BELIEF/NPC_TOLD;
- no upstream speaker provenance/source chain copied to listener BELIEF;
- no provider/LLM call;
- no new persistence schema/file/config;
- no autonomous conversation behavior.

Observed CI:

- VillAIgence CI #2107 / run `31282094517`.
- `common + deterministic mock-provider tests` — **SUCCESS**.

## Preservation GREEN 4 — fail-closed request/owner boundaries

Tests-only commit: `ef75715ee9cb7fd1979782a3bd7d5b5911afdd80`.

Added null/same-NPC/unknown-source/wrong-owner lifecycle rejection checks and asserted no partial listener evidence/BELIEF plus unrelated-source preservation.

Observed CI:

- VillAIgence CI #2109 / run `31282233512`.
- `common + deterministic mock-provider tests` — **SUCCESS**.
- Production change required: **NO**.

## Preservation GREEN 5 — replay idempotency and Semantic consolidation

Tests-only commit: `59eeb2e4d5b94bfb131c988e66df3cb4a1c7071b`.

Proved:

- exact retry keeps the same evidence/result identity;
- `memory2.json` and `semantic-memory.json` are byte-identical across exact retry after the first stable transfer;
- later transfer at a new authoritative game time creates a distinct evidence UUID;
- compatible listener BELIEF consolidates to one logical entry with deterministic exact source union;
- semantic subject UUID insertion order does not alter consolidation identity/scope.

Observed CI:

- VillAIgence CI #2111 / run `31282362578`.
- `common + deterministic mock-provider tests` — **SUCCESS**.
- Production change required: **NO**.

## Preservation GREEN 6 — pressure outcomes / partial retention

Tests-only commit: `c4137734970471e39d7412f3fbccd16bfa411885`.

Proved without changing retention coefficients:

- canonical transfer evidence losing event pressure returns `SOURCE_NOT_RETAINED` and creates no listener BELIEF;
- retained evidence with listener BELIEF losing Semantic pressure returns `BELIEF_NOT_RETAINED`;
- the legitimate retained evidence is not rolled back in the second case;
- stronger pre-existing data and unrelated NPC state remain intact.

Observed CI:

- VillAIgence CI #2113 / run `31282473776`.
- `common + deterministic mock-provider tests` — **SUCCESS**.
- Production change required: **NO**.

## Preservation GREEN 7 — reload, scope/privacy and Working Memory isolation

Tests-only commit: `26e03fde38fb9cba264ee21d15bce84ab3d4c0b0`.

Proved:

- exact evidence/BELIEF survive a real copy to a distinct world root and fresh store load;
- exact replay after fresh-root reload remains idempotent;
- NPC-global, player-private and shared subject scope are preserved;
- speaker identity is not injected into semantic subject scope;
- independent A→B and D→C pairs remain isolated;
- private transferred BELIEF is visible only to the matching player through existing semantic eligibility;
- raw NPC→NPC DIALOGUE evidence with `dialogue == null` does not become player `user/assistant` Working Memory;
- genuine structured player dialogue still reconstructs normally alongside transfer evidence.

Observed CI:

- VillAIgence CI #2115 / run `31282616238`.
- `common + deterministic mock-provider tests` — **SUCCESS**.
- Production change required: **NO**.

## Preservation GREEN 8 — long horizon, evictability and deterministic simulation

Tests-only commit: `50693433ccae5150c3a36471795e50e7c8d7810c`.

The deterministic simulation uses two independent speaker/listener pairs, source FACT and source BELIEF, private and NPC-global scope, exact replay, a later transfer after a 36,000-tick boundary, more than 200 unrelated Semantic/episodic records per scenario, forward/reversed pressure insertion order, and a fresh-root persistence copy. It uses fixed UUIDs/game times and no sleeps/wall-clock expected behavior.

Proved:

- old retained NPC_TOLD BELIEF can enter existing long-horizon recall beyond >32 newer eligible weak Semantic records;
- foreign-player private transfer remains absent before candidate allocation;
- global transfer remains eligible;
- forward/reverse pressure order and fresh reload produce exact deterministic persisted IDs/context;
- context remains bounded by the existing max-results contract;
- transferred memory renders as `BELIEF | provenance=NPC_TOLD` and retains current-observed-fact-wins prompt framing;
- transferred BELIEF and evidence can both be evicted under existing stronger pressure, so neither is immortal.

Observed CI:

- VillAIgence CI #2117 / run `31282766360`.
- `common + deterministic mock-provider tests` — **SUCCESS**.
- Production change required: **NO**.

## Pre-final exact-head delivery evidence

Runtime/changelog head: `2361d78369b00a013c6d108e0de21c57b3011d14`.

Observed mandatory gates on that exact head:

- Repository security policy #1756 / run `31282948687` — **SUCCESS**.
- VillAIgence CI #2121 / run `31282948682` — **SUCCESS**.
  - release-identity/preflight — PASS;
  - full common + deterministic mock-provider tests — PASS;
  - risk selector — PASS;
  - selected server GameTests — Fabric + NeoForge PASS;
  - selected Fabric build — PASS;
  - selected NeoForge build — PASS;
  - production acceptance contract tests — PASS;
  - exact staged production startup/restart acceptance — PASS;
  - selected persistence recovery matrix — PASS, not skipped;
  - runtime-flow guardrails — PASS;
  - release artifact identity check — PASS;
  - distributable package verification — PASS.
- Production Soak #177 / run `31282948675` — **SUCCESS**.
  - constrained-heap runtime concurrency/staging — PASS;
  - five production restart cycles — PASS.
- GitHub Release dry-run #511 / run `31282948679` — **SUCCESS**.
  - common tests, exact production acceptance, exact persistence recovery, server/client GameTests, Fabric + NeoForge builds, package construction and accepted-JAR/package byte-identity checks — PASS;
  - `github-release` publication job — **SKIPPED as required**; no release was published.

Independent base-to-head read-only review of `a20d6d0ebf5688e790fedeb3563f24f69e7e9c95..2361d78369b00a013c6d108e0de21c57b3011d14` found:

- P0: 0;
- P1: 0;
- P2: 0;
- unresolved PR review threads: 0;
- no persistence-format/config/provider-authority/client-authority/ranking-boundary scope creep found.

Because this evidence synchronization itself creates a new PR head, these pre-final results are not used as a substitute for the final exact-head re-gate. The final merge decision must use the workflow results attached to the evidence-sync head.

## Release boundary

Nothing above is installed `0.2.0` acceptance. The immutable installed release remains `0.2.0+1.21.1` with its previously accepted artifact/evidence. This feature remains unreleased until a later release candidate is explicitly built, byte-identified and accepted.
