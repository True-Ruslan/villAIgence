# Provenance-Aware Rumors — TDD and Delivery Evidence

Date: 2026-08-09

Scope: PR #135, `feat/provenance-aware-rumors` → `1.21.1`.

Approved design:
- `docs/superpowers/specs/2026-08-09-provenance-aware-rumors-design.md`

Approved implementation plan:
- `docs/superpowers/plans/2026-08-09-provenance-aware-rumors.md`

Official installed-release boundary remains immutable:

```text
0.2.0+1.21.1
release commit: e426f588efefa6aa48a6e536c4a998421bbda241
installed JAR SHA-256: 56293f86634b50b2def044429aac6f2cf0d197eb16ac1e60224708f7b3333aee
```

Nothing in PR #135 is represented as installed `0.2.0` acceptance.

## Evidence policy

This ledger records only evidence that can be verified from the repository and connected GitHub Actions data. The active connector exposes the PR head, diff, files, tests and workflow runs, but does not expose the ordered list of the PR's 33 intermediate commits. Therefore exact historical tests-only RED SHA/run pairs for every staged task cannot be reconstructed in this session without inventing data. Those fields are explicitly marked `NOT RECONSTRUCTED` rather than fabricated.

The branch itself retains the staged implementation history. Final delivery is gated on fresh exact-head repository security, main CI, production soak and release dry-run after the last changelog/evidence commit.

## Contract implemented

```text
exact persisted Semantic source
→ authoritative exact reread
→ first-hop origin OR retained canonical direct provenance branch
→ cycle check
→ hop-limit check
→ deterministic npc-knowledge-transfer-v2 evidence identity
→ listener-owned DIALOGUE / NPC_TOLD evidence with immutable ancestry
→ exact persisted evidence reread and validation
→ listener BELIEF / NPC_TOLD
→ exact retained Semantic verification
```

Invariants:

- provider/client does not choose lineage, truth class, provenance, source UUIDs or authority;
- first-hop origins are only `FACT/SYSTEM_OBSERVED`, `BELIEF/PLAYER_TOLD`, `BELIEF/INFERRED`;
- `BELIEF/NPC_TOLD` never resets origin;
- downstream knowledge stays `BELIEF/NPC_TOLD`;
- ancestry is acyclic and hard-capped at 8 hops;
- canonical direct branch is `gameTime DESC → evidence UUID ASC`;
- resolver has no listener input;
- listener-specific cycle/limit checks occur after branch selection and never trigger lower-branch fallback;
- Semantic `sourceEventIds` stay direct-only;
- exact statement and semantic subject scope are preserved;
- no summary parsing creates authority;
- no new store/config/provider call/migration/backfill/version bump;
- existing retention/ranking/Working Memory/provider/gameplay authority remains unchanged.

## Task evidence

### Task 1 — persisted provenance model

Coverage:
- `KnowledgeTransferProvenanceModelTest`
- `KnowledgeTransferProvenance`
- additive nullable `MemoryEvent.knowledgeTransferProvenance`
- defensive immutable collection snapshots preserve order and nulls rather than silently repairing persisted authority data.

Historical tests-only RED SHA/run: `NOT RECONSTRUCTED`.
Current branch proof: common suite is included in exact-head VillAIgence CI.

### Task 2 — v2 identity, integrity policy and canonical factory

Coverage:
- `KnowledgeTransferProvenancePolicyTest`
- one `npc-knowledge-transfer-v2` deterministic identity authority;
- continuity, deterministic hop IDs, canonical scope, allowed origins, cycle and 8-hop policy;
- first-hop/append factory uses normalized statement and preserved subject scope.

Historical tests-only RED SHA/run: `NOT RECONSTRUCTED`.
Current branch proof: common suite is included in exact-head VillAIgence CI.

### Task 3 — canonical v2 evidence and first-hop lifecycle

Coverage:
- `NpcToldDialogueAdapterTest`
- `NpcKnowledgeTransferLifecycleTest`
- adapter delegates evidence identity to provenance policy;
- listener-owned `DIALOGUE/NPC_TOLD` evidence carries exact lineage;
- source is reread before evidence construction;
- evidence persists and rereads before BELIEF admission;
- listener Semantic source list contains the new direct evidence, not copied upstream ancestry.

Historical behavioral RED SHA/run: `NOT RECONSTRUCTED`.
Current branch proof: common suite is included in exact-head VillAIgence CI.

### Task 4 — exact resolver and A→B→C continuation

Coverage:
- `KnowledgeTransferProvenanceResolverTest`
- `NpcKnowledgeTransferLifecycleTest`
- resolver accepts only `(MemoryEventStore, SemanticMemoryEntry)` and therefore cannot use the proposed listener;
- only referenced retained canonical direct evidence participates;
- ordering is newest `gameTime`, then evidence UUID ascending;
- multi-hop continuation appends to the selected immutable lineage.

Historical compile/behavioral RED SHA/run: `NOT RECONSTRUCTED`.
Current branch proof: common suite is included in exact-head VillAIgence CI.

### Task 5 — branch/no-fallback/cycle/hop-limit policy

Coverage:
- `NpcKnowledgeTransferLifecyclePolicyTest`
- self-transfer returns `PROVENANCE_CYCLE`;
- return to any NPC already in selected path returns `PROVENANCE_CYCLE`;
- exactly 8 hops are admitted and the ninth returns `PROVENANCE_LIMIT_REACHED`;
- cycle wins when the proposed ninth listener is already in path;
- canonical higher-priority branch that cycles causes rejection with no listener-dependent fallback to a lower branch.

Historical RED or preservation-GREEN SHA/run: `NOT RECONSTRUCTED`.
Current branch proof: common suite is included in exact-head VillAIgence CI.

### Task 6 — fail-closed integrity and unavailable provenance

Coverage:
- `NpcKnowledgeTransferProvenanceIntegrityTest`
- `NpcKnowledgeTransferPolicyTest`
- field-by-field mutation of origin and hop payload invalidates canonical evidence;
- missing retained direct evidence returns `PROVENANCE_UNAVAILABLE` with zero listener writes;
- historical v1 provenance-less evidence cannot be upgraded into multi-hop lineage;
- wrong owner, unreferenced evidence, statement mismatch, scope mismatch and malformed payload fail closed.

Historical RED or preservation-GREEN SHA/run: `NOT RECONSTRUCTED`.
Current branch proof: common suite is included in exact-head VillAIgence CI.

### Task 7 — restart, replay, scope privacy and Working Memory isolation

Coverage:
- `NpcKnowledgeTransferRumorPersistenceTest`
- exact two-hop lineage survives copy to a fresh world root;
- exact replay after reload is byte-idempotent for `memory2.json` and `semantic-memory.json`;
- global/private/shared semantic scopes remain exact across hops;
- provenance participants do not pollute semantic `relatedEntities`;
- NPC rumor evidence is excluded from player dialogue reconstruction while real player dialogue still reconstructs normally.

Historical RED or preservation-GREEN SHA/run: `NOT RECONSTRUCTED`.
Current branch proof: common suite is included in exact-head VillAIgence CI.

### Task 8 — pressure, direct-evidence loss and bounded forgetting

Coverage:
- `NpcKnowledgeTransferRumorPressureTest`
- older physical ancestry may be forgotten while later direct evidence retains its immutable ancestry snapshot;
- loss of the current direct evidence blocks further propagation with `PROVENANCE_UNAVAILABLE` but does not erase the speaker BELIEF;
- rumor evidence and BELIEF remain evictable under the existing retention policies.

Historical RED or preservation-GREEN SHA/run: `NOT RECONSTRUCTED`.
Current branch proof: common suite is included in exact-head VillAIgence CI.

### Task 9 — deterministic multi-NPC simulation and truth preservation

Coverage:
- `NpcKnowledgeTransferRumorSimulationTest`
- 10 NPCs with a valid 8-hop chain;
- rejected ninth hop and cycle attempt;
- independent/corroborating origins retain separate direct lineage while Semantic consolidation unions direct evidence IDs;
- >200 Semantic and >200 episodic noise records are inserted in forward/reverse orders;
- snapshots remain equal across pressure order and two fresh-root reloads;
- private rumor remains player-isolated;
- deep rumor remains `BELIEF/NPC_TOLD` even when origin was FACT;
- conflicting `FACT/SYSTEM_OBSERVED` remains authoritative in prompt framing;
- bounded Semantic context remains within `MAX_RESULTS`.

Historical RED or preservation-GREEN SHA/run: `NOT RECONSTRUCTED`.
Current branch proof: common suite is included in exact-head VillAIgence CI.

## Runtime-head delivery proof before documentation sync

Runtime head reviewed before the final changelog/evidence commits:

```text
fd27513a16f5603ad9e6e956b569ac066c5a2860
fix: snapshot provenance collection inputs immutably
```

Fresh workflows observed on that exact source head:

```text
Repository security policy #1821 / run 31306245355 — SUCCESS
VillAIgence CI #2186 / run 31306245358 — SUCCESS
VillAIgence Production Soak #207 / run 31306245356 — SUCCESS
VillAIgence GitHub Release #541 / run 31306245352 — SUCCESS
```

Main CI covered:
- repository security policy;
- common + deterministic mock-provider tests;
- risk catalog and required server GameTests;
- Fabric + NeoForge builds;
- production acceptance contracts;
- staged production server acceptance;
- package verification.

Risk-selected persistence recovery was skipped in the normal main-CI profile on this head.

Release dry-run separately covered the complete release matrix:
- production acceptance;
- exact persistence recovery;
- risk/GameTests and supported loaders;
- package smoke;
- accepted-JAR/package byte identity;
- `github-release` publication job `SKIPPED` as required.

Production Soak covered:
- soak contracts and repository policy;
- authenticated concurrency under constrained heap;
- exact production staging under constrained fork heap;
- five-cycle production restart soak.

## Independent review checklist

Read-only review explicitly traces:

```text
provider/client cannot inject lineage
resolver has no listener input
NPC_TOLD cannot reset origin
cycle before limit
no listener-dependent fallback
no ancestry pollution of Semantic sourceEventIds
no retention/ranker/config changes
no unbounded provenance growth
no summary parsing authority
no migration/backfill/version bump
restart/replay/privacy/pressure paths
```

Final P0/P1/P2 verdict and unresolved review-thread count are recorded in the PR after the final documentation exact-head re-gate.

## Final delivery gate

After this evidence file and root changelog are the last content changes, the exact resulting PR head must freshly pass:

```text
Repository security policy
VillAIgence CI
VillAIgence Production Soak
VillAIgence GitHub Release dry-run
```

No GitHub Release is published by this PR. After merge, canonical `docs/PROJECT_STATE.md` and `docs/ROADMAP.md` are reconciled in a separate docs-only PR.
