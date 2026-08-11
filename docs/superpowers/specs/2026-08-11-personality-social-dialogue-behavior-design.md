# Personality + Social Dialogue/Behavior Integration Design

Date: 2026-08-11
Track: 0.3 Personality + NPC↔NPC Social Graph
Base: `c2fe1249f19086aa7bae409cc21c05550f5b4785`

## Goal

Make the already-proven read-only `PersonalitySocialSnapshot` affect real runtime behavior without transferring authority to the LLM, without enumerating the NPC social graph, and without creating a new autonomous NPC-agent loop.

This slice has two deliberately small consumers:

1. canonical MCA Personality influences the style/stance guidance used by the existing player↔NPC dialogue request;
2. the exact directed NPC→NPC social edge for an already-selected settlement knowledge-transfer pair may conservatively suppress that one transfer opportunity.

The second consumer is intentionally **not** a social-neighbor selector. Existing deterministic settlement selection chooses speaker/listener first. Only then may VillAIgence read that exact source→target edge. There is no fallback retargeting.

## Non-goals

This slice does not add:

- NPC↔NPC LLM conversations;
- autonomous per-NPC or per-tick AI scheduling;
- graph-neighbor enumeration, best-friend/enemy search or social-topology ranking;
- provider-authored NPC↔NPC trust/respect/fear/affinity deltas;
- new provider calls or a provider response-schema change;
- FACT/BELIEF authority, confidence, provenance, importance or retrieval-ranking changes;
- a new persistence file, format version, migration, backfill or public config;
- relationship-based truth scoring;
- a new general behavior planner.

## Authority laws

1. MCA `VillagerBrain.getPersonality()` remains the only persistent personality authority.
2. `npc-social-graph.json` remains the only persistent NPC↔NPC social-state authority.
3. `PersonalitySocialSnapshot`, influence classification and prompt guidance are derived read-only views.
4. Current server-observed world facts and hard gameplay safety constraints outrank personality/social preference.
5. The provider may propose existing structured player-facing commands only; it never receives NPC↔NPC graph-write authority.
6. Any NPC↔NPC social mutation still requires exact server-owned cause evidence and `NpcSocialMutationLifecycle` admission.
7. NPC×player `relationships.json` remains separate from NPC↔NPC `npc-social-graph.json`.
8. Social influence may affect whether one already-selected NPC tells another NPC something; it may not change whether the claim is FACT/BELIEF, its confidence, provenance, rank or statement.

## Considered approaches

### A. Prompt-only influence

Add deterministic personality style text to player dialogue and stop there.

Pros: very small and low-risk.

Rejected as the complete slice because ordinary player dialogue captures `counterpart=null`, so direct NPC↔NPC social state would still have no real behavior consumer.

### B. Bounded dual-consumer integration — selected

Use one closed influence policy for prompt guidance, and add a direct-pair social gate after the existing settlement speaker/listener selection.

Pros:

- gives Personality a real dialogue effect now;
- gives NPC↔NPC social state a real server-side behavior effect now;
- reuses existing bounded settlement cadence/lifecycle;
- no extra provider call, scheduler or graph enumeration;
- no truth-authority change;
- neutral state preserves current behavior.

Cons: settlement behavior uses the directed social state only; it does not resolve live MCA Personality for background knowledge transfer. This is intentional to avoid pulling live-entity authority into a UUID/path-based lifecycle solely for this slice.

### C. Full NPC↔NPC AI conversation runtime

Create live NPC pair capture, provider dialogue, scheduling, actions and persistence around NPC conversations.

Rejected because it introduces provider cost, scheduling, target lifecycle, replay and action-semantics questions that belong to the later autonomous-agent track.

## Influence model

Create a loader-independent immutable influence model under `livingworld/context`.

### Personality dialogue style

`PersonalityDialogueStyle` is a closed enum derived only from the canonical snapshot token:

```text
unassigned   → NEUTRAL
friendly     → WARM
flirty       → CHARMING
playful      → PLAYFUL
gloomy       → GLOOMY
sensitive    → GENTLE
greedy       → TRANSACTIONAL
odd          → ECCENTRIC
crabby       → GRUFF
extroverted  → OUTGOING
introverted  → RESERVED
relaxed      → CALM
anxious      → ANXIOUS
peaceful     → PEACEFUL
upbeat       → CHEERFUL
```

These values are dialogue-style preferences only. `CHARMING` must not weaken existing child/relative anti-flirt safety. The renderer explicitly states that safety, factual authority and action validation take precedence.

### Direct-pair disposition

`NpcPairDisposition` is a closed enum derived from the exact directed `NpcSocialState` only:

```text
FEARFUL        when fear >= 75
DISTRUSTFUL    otherwise when trust <= -75
ANTIPATHETIC   otherwise when affinity <= -75
AFFILIATIVE    otherwise when trust >= 60 AND affinity >= 60
RESPECTFUL     otherwise when respect >= 70
NEUTRAL        otherwise
```

Priority is intentional and deterministic. Strong fear wins because it is a hard interaction inhibitor; strong distrust/antipathy then block cooperative sharing. Positive states are descriptive preference only.

These thresholds are fixed code constants in this slice. They intentionally require extreme/strong state so weak social noise does not alter existing behavior.

### `PersonalitySocialInfluence`

A fixed-size immutable record:

```java
public record PersonalitySocialInfluence(
        PersonalityDialogueStyle personalityStyle,
        NpcPairDisposition pairDisposition
) {}
```

`PersonalitySocialInfluencePolicy.evaluate(snapshot)` is pure and deterministic. Null input returns `NEUTRAL/NEUTRAL`.

No UUID, names, free-form strings, store handles or persistence objects are carried in the influence object.

## Dialogue guidance

Add a dedicated deterministic `PersonalitySocialDialogueGuidanceRenderer` rather than expanding the descriptive `PersonalitySocialContextRenderer` contract.

Output bounds:

- zero lines when personality style and pair disposition are both neutral;
- at most one personality-style line;
- at most one direct-pair-stance line;
- no UUID/name/free-form input is rendered;
- output is server-authored closed text only.

Representative lines:

```text
Dialogue style preference: warm and welcoming. This affects tone only; current-world facts, safety rules, permissions, and structured action validation take precedence.

Current counterpart stance: guarded and distrustful. This affects interpersonal tone only; it does not change factual truth, memory authority, or server action validation.
```

`SnapshotContextPromptPolicy` gains a source-compatible overload that places dialogue guidance in the same bounded personality/social authority layer after observed facts and descriptive personality/social state, but before Operator Lore and memory layers.

Normal player↔NPC dialogue has no NPC counterpart. Therefore it gets only the personality-derived style line; direct NPC social state is not fabricated for the player.

No provider request count/schema changes.

## Existing command execution hardening

The snapshot's `availableActions` remains the capture-time eligibility whitelist, but execution occurs later on the server thread. Therefore `applySnapshotCommand(...)` must re-check the current server-owned NPC×player relationship policy immediately before invoking the command:

```text
snapshot allowed command
→ server-thread liveness/same-level checks
→ fresh relationships.json read when relationship state is enabled
→ LivingWorldRelationshipActionPolicy.isAllowed(command,currentState)
→ TriggerCommandInfos active-state revalidation
→ execute
```

If the fresh relationship store cannot be read safely, relationship-gated execution fails closed for that command.

This does not reinterpret NPC↔NPC social state as NPC×player relationship state.

## Pairwise behavior integration

The existing settlement knowledge-flow selector remains unchanged and owns deterministic bounded speaker/listener selection.

Current flow:

```text
bounded residents
→ deterministic speaker/source/listener opportunity
→ NpcKnowledgeTransferLifecycle.transfer(...)
```

New flow:

```text
bounded residents
→ deterministic speaker/source/listener opportunity
→ read exactly speaker→listener NpcSocialState
→ classify exact pair disposition
→ conservative knowledge-sharing gate
   → blocked: record social suppression in cycle result; NO fallback listener; NO transfer mutation
   → allowed: existing NpcKnowledgeTransferLifecycle.transfer(...) unchanged
```

Knowledge-sharing policy:

```text
FEARFUL      → BLOCK
DISTRUSTFUL  → BLOCK
ANTIPATHETIC → BLOCK
AFFILIATIVE  → ALLOW
RESPECTFUL   → ALLOW
NEUTRAL      → ALLOW
```

The selector never reads social state and never changes its listener ranking. This is important: the graph cannot become an implicit enumeration/ranking API.

### Cycle observability

Extend package-private `SettlementKnowledgeFlowLifecycle.CycleResult` with `sociallySuppressedTransfers`.

Invariants:

- `0 <= sociallySuppressedTransfers <= opportunities`;
- `attemptedTransfers + sociallySuppressedTransfers <= opportunities`;
- status list still corresponds exactly to attempted transfer lifecycle calls;
- a socially blocked opportunity has no `NpcKnowledgeTransferResult.Status` because the transfer lifecycle was not invoked.

No persisted suppression ledger is added.

## Read-only and persistence guarantees

Influence evaluation/rendering performs no I/O.

Direct pair lookup during settlement flow may open the existing graph store only for the exact already-selected pair. Tests must prove:

- missing graph/pair yields neutral/ALLOW and does not create `npc-social-graph.json`;
- existing graph bytes remain byte-identical after blocked social-gate evaluation;
- blocked transfer does not create/modify listener Memory 2.0 or Semantic Memory;
- allowed neutral transfer preserves current lifecycle behavior;
- no `relationships.json` access is introduced into NPC↔NPC settlement flow;
- prompt guidance evaluation does not mutate graph, relationships, Memory 2.0, Semantic Memory or Personality state.

The action-execution recheck may read `relationships.json`; that read is intentionally separate and player-scoped.

## Error handling

- Null influence snapshot → neutral influence.
- Null social state → neutral pair disposition.
- Graph read failure in settlement pair gating fails soft to **no transfer for that opportunity**, not to fabricated neutral/allow. A corrupt/unreadable social authority source must not silently become cooperative permission.
- One failed/suppressed opportunity does not retarget to another NPC.
- Existing transfer lifecycle errors/statuses remain unchanged for opportunities that pass the social gate.
- Fresh player relationship-policy read failure during command execution fails closed for the command and logs a warning.

## TDD requirements

### Pure influence

Observe RED before production types exist for:

- every canonical personality token → exact closed style;
- unknown/null token cannot inject style text;
- pair-disposition threshold boundaries and priority;
- A→B and B→A asymmetric social states produce asymmetric disposition;
- null/no-counterpart → neutral social disposition.

### Dialogue guidance

Observe RED for:

- fixed zero/one/two-line bounds;
- exact closed wording;
- no UUID/name/free-form leakage;
- player snapshot (`counterpart=null`) gets personality guidance only;
- prompt order remains current facts → descriptive personality/social → dialogue guidance → Operator Lore → Semantic → disagreement → episodic;
- current factual authority statement remains earlier/stronger;
- structured provider schema/call count unchanged.

### Pairwise settlement behavior

Observe RED for:

- neutral pair preserves current transfer attempt/admission;
- FEARFUL/DISTRUSTFUL/ANTIPATHETIC pair suppresses exactly the chosen opportunity;
- no fallback listener/source selection after suppression;
- positive dispositions do not increase fan-out or opportunity bounds;
- selector remains graph-independent;
- missing pair/graph neutral behavior is write-free;
- graph bytes remain identical on blocked evaluation;
- blocked transfer leaves listener Memory 2.0/Semantic state unchanged;
- fresh-root/restart behavior is deterministic.

### Command revalidation

Observe RED proving:

- an action allowed in the snapshot but disallowed by a changed fresh relationship state cannot execute;
- fresh policy ALLOW still executes through existing TriggerCommand active-state checks;
- relationship store read failure fails closed;
- this recheck does not consult NPC↔NPC social state.

### Runtime gates

After focused GREEN:

- complete common + deterministic provider tests;
- Fabric GameTests, including personality dialogue guidance on a live MCA villager and pairwise settlement social gating where runtime fixture allows;
- Fabric + NeoForge builds;
- production startup/restart acceptance;
- selected persistence recovery matrix;
- repository security policy;
- constrained production soak;
- GitHub Release dry-run with publication SKIPPED;
- base→head review P0/P1/P2/P3 = 0 and unresolved threads = 0.

## Compatibility and release boundary

- existing official installed `0.2.0+1.21.1` claims remain unchanged;
- this is a post-release `[Unreleased]` 0.3 source capability;
- root `CHANGELOG.md` must be updated in the runtime PR;
- no version/tag/release publication in this slice;
- after merge, reconcile `PROJECT_STATE.md` / `ROADMAP.md`, then perform 0.3 convergence/release-candidate planning.

## Exit criterion

The slice is complete when:

1. canonical Personality deterministically affects player↔NPC dialogue style through bounded server-authored guidance;
2. the exact directed social state of an already-selected NPC pair can conservatively suppress that pair's settlement knowledge-sharing behavior without graph enumeration or fallback retargeting;
3. neutral state preserves existing behavior;
4. snapshot-time player command eligibility is revalidated against fresh server-owned relationship policy before execution;
5. truth authority, persistence ownership, provider schema/call count and NPC social mutation authority remain unchanged;
6. full selected automated delivery gates and independent review pass on one frozen exact head.
