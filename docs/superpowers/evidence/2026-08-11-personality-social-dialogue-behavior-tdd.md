# Personality / Social Dialogue + Behavior Integration — TDD Evidence

Date: 2026-08-11
PR: #158
Base: `c2fe1249f19086aa7bae409cc21c05550f5b4785`

## Scope

This ledger records the staged TDD and preservation evidence for the deliberate 0.3 dialogue/behavior integration slice.

Accepted boundary:

```text
MCA Personality + optional exact directed NPC social state
→ closed server-owned influence policy
→ fixed-size dialogue guidance
→ centralized snapshot prompt composition

existing deterministic settlement pair selection
→ exact selected speaker→listener social read
→ conservative allow/suppress gate
→ existing NpcKnowledgeTransferLifecycle

capture-time optionalCommand allowlist
→ server-thread execution boundary
→ strict fresh NPC×player relationship authorization
→ existing command lookup/call
```

The slice deliberately adds no provider request/schema, public config, persistence file/version, graph enumeration/ranking, provider-authored social mutation, autonomous NPC↔NPC LLM conversation, truth-score mutation, migration/backfill or release publication.

## Task 1 — closed influence model

### RED

- Tests-only head: `444a51d2`.
- Observed result: `:common:compileTestJava` failed with 57 `cannot find symbol` errors for the absent `PersonalityDialogueStyle`, `NpcPairDisposition`, `PersonalitySocialInfluence` and `PersonalitySocialInfluencePolicy` types.

### GREEN

- Head: `80761bc4b1e3057210709037fab40ad0729363b3`.
- VillAIgence CI #2769 / run `31537018307`: common + deterministic mock-provider stage SUCCESS.
- Personality mapping is closed/deterministic; pair-disposition priority is `FEARFUL > DISTRUSTFUL > ANTIPATHETIC > AFFILIATIVE > RESPECTFUL > NEUTRAL`.

## Task 2 — fixed-size dialogue guidance

### RED

- Tests-only head: `8a60347d51a185aed4f05349c367f234b814810f`.
- Production Soak #465: `:common:compileTestJava` failed with exactly 8 missing-symbol errors for absent `PersonalitySocialDialogueGuidanceRenderer`.

### GREEN

- Head: `14563d740c624ae0b62ef6eaf227b8643250560a`.
- VillAIgence CI #2773: common + deterministic mock-provider stage SUCCESS.
- Renderer output is bounded to zero, one or two fixed server-authored guidance lines and cannot include arbitrary names, UUIDs or raw social numbers.

## Task 3 — centralized prompt integration

### RED

- Tests-only head: `3b417cd91bbdd42086dfb231e9c9cd2a88e4ae53`.
- Production Soak #467 / run `31537918957`: compile failed because no seven-layer `SnapshotContextPromptPolicy.compose(...)` existed.

### GREEN + regression correction

- Production wiring head: `0b6fc9783be688bb96b3d2928520cf5cd1a36363`.
- Initial CI #2779 correctly exposed two stale source-contract tests that still required the previous six-layer call; production compiled and the only failures were those old wiring expectations.
- Tests-only correction: `70335f4a693f9d0cc1ac211e29b53f6f9dfb2112`.
- VillAIgence CI #2781 / run `31543400750`: full common + deterministic provider stage SUCCESS.
- Final authority order: current world facts → descriptive personality/direct-social state → bounded dialogue guidance → Operator Lore → Semantic Memory → live disagreement context → episodic/social history.

## Task 4 — exact-pair settlement social gate

### RED

- Tests-only head: `c7ce3b79777fc0467ee3b71a4af8abc9c09129b7`.
- Production Soak #471: compile failed with exactly 10 intended errors: seven for absent `SettlementSocialKnowledgeSharingPolicy`, three for absent `sociallySuppressedTransfers()`.

### GREEN

- Head: `511281af03b4dabfb7e3939c7567beeda53930db`.
- VillAIgence CI #2785 / run `31543936898`: full common + deterministic provider stage SUCCESS.
- Existing deterministic selector remains unchanged. Only the selected directed speaker→listener edge is read; `FEARFUL`, `DISTRUSTFUL` or `ANTIPATHETIC` suppress that exact transfer; no fallback listener is selected.
- Every opportunity is now accounted for by the invariant `attemptedTransfers + sociallySuppressedTransfers == opportunities`.

## Task 5 — preservation / no enumeration

- Tests-only head: `c56ef6b8babe591bb7f3f9a070614c1a339b2454`.
- VillAIgence CI #2787 / run `31544223674`: full common + deterministic provider stage SUCCESS.
- Proven guarantees:
  - missing social graph behaves neutral and is not created by the read path;
  - adverse suppression leaves graph/semantic/memory persistence unchanged;
  - fresh-root reload repeats the same suppression;
  - positive social state cannot alter selector target, fan-out or opportunity bounds;
  - `SettlementKnowledgeFlowSelector` has no dependency on `NpcSocialGraphStore` or the influence/gate policy.

## Task 6 — fresh command relationship authorization

### RED

- Tests-only head: `2cbaa2b5df7f395c71943b326b3d90a0374fb669`.
- Production Soak #474 / run `31544565098`: `:common:compileTestJava` failed with exactly 8 missing-symbol errors, all for absent `SnapshotCommandRelationshipPolicy`.

### GREEN

- Head: `a0f049f730fc7e1930694cc0645d75ecf24090cf`.
- VillAIgence CI #2790 / run `31545078736`: common + deterministic provider stage SUCCESS.
- Authorization path is intentionally stricter than normal store recovery:
  - relationship-state disabled preserves legacy behavior;
  - missing canonical store is neutral/read-only;
  - current trust `< -25` or fear `> 60` revokes `follow-player` through the existing `LivingWorldRelationshipActionPolicy`;
  - malformed, symlinked or non-regular `relationships.json` fails closed for the gated command;
  - strict authorization read never invokes `JsonStoreRecovery`, never moves/writes a corrupt file and never fabricates neutral by repair;
  - unrelated safe commands do not touch relationship persistence.
- Runtime order is capture-time command allowlist → `server.execute` → liveness/world check → strict fresh relationship authorization → existing command lookup/active-state check → call.

## Task 7 — live MCA acceptance

- Acceptance-only head: `230295d515f64e5a6d009a9565cf17646d7c37e8`.
- VillAIgence CI #2793 / run `31545335450`:
  - common + deterministic mock-provider stage: SUCCESS;
  - risk catalog + server GameTests + supported loader builds: SUCCESS.
- Live Fabric GameTests use real MCA villagers and prove:
  - `Personality.FRIENDLY` becomes `WARM` bounded dialogue influence with no NPC counterpart/social edge;
  - directed A→B fear produces `FEARFUL` while independent B→A trust+affinity produces `AFFILIATIVE`;
  - source `FRIENDLY` and counterpart `CRABBY` remain unchanged;
  - personality-only influence creates no social graph;
  - directed influence leaves existing `npc-social-graph.json` byte-identical;
  - combined personality + non-neutral pair influence remains capped at two guidance lines;
  - the test path calls no provider API.

## Preserved invariants

The completed behavior slice does not:

- change MCA Personality authority or persistence;
- enumerate or rank the NPC social graph;
- use NPC×player `relationships.json` as NPC↔NPC social state;
- allow social state to select a settlement pair or fallback listener;
- allow the LLM/provider to author NPC social deltas or relationship authorization;
- change FACT/BELIEF/provenance/confidence/contradiction authority;
- add a provider request, provider response field, public config or persistence schema/version;
- add autonomous per-tick agents or visible NPC↔NPC LLM conversations;
- publish a release.

## Final delivery gate

After this evidence/CHANGELOG reconciliation, delivery is accepted only on one frozen PR head with:

- repository security policy SUCCESS;
- VillAIgence CI SUCCESS, including common tests, Fabric GameTests, supported loaders, production acceptance and package verification;
- Production Soak SUCCESS;
- GitHub Release dry-run SUCCESS with publication skipped;
- base→head readiness review with no unresolved P0/P1/P2/P3 findings.

Exact frozen-head workflow IDs belong to the final PR checks/body so this ledger does not require a post-verification code/doc commit that would invalidate its own evidence.
