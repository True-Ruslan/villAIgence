# VillAIgence Roadmap

> **Canonical product roadmap.** For exact implementation and validation state, read `docs/PROJECT_STATE.md` first.
>
> Last reconciled: **2026-08-05**. S10c and M11 Phases A-C are complete. M11 Phase D is the immediate implementation target.

## Product vision

VillAIgence is evolving from an MCA-derived AI conversation mod into a **persistent living-society simulation layer for Minecraft**.

The target world contains NPCs that:

- retain stable identity, memory, personality, voice and relationships;
- know only what they observed, learned or were explicitly told;
- communicate naturally by text and voice;
- act through server-authoritative policies;
- form families, settlements, factions and social histories;
- exchange facts, beliefs and rumors with provenance;
- generate durable emergent stories rather than isolated AI tricks.

> **VillAIgence — Giving villagers a mind of their own.**

Compatibility-sensitive internal naming remains `mca`, `LivingWorld` and `livingworld` until an explicit migration is designed.

---

# Architecture principles

## 1. LLM is not authority

```text
server world state
→ immutable bounded context
→ LLM proposal
→ validation and policy
→ server-owned action
```

## 2. Identity outlives providers

Changing OpenAI, OpenRouter or a future local model must not regenerate NPC identity, memories, relationships or voice.

## 3. Fail soft without corrupting state

Provider, voice, packet and auxiliary-store failures become controlled statuses. They must not crash conversations, duplicate actions, leak reasoning or damage world data.

## 4. Persistent state is explicit and world-local

Important data belongs under `<world>/livingworld/`, remains inspectable, bounded, backup-safe and migration-aware.

## 5. Provenance layers remain separate

```text
observed current facts       authoritative for this turn
operator-authored lore       explicit background setting
semantic FACT/BELIEF         learned persistent knowledge
episodic memory              remembered events/dialogue
```

Current observations win conflicts.

## 6. Client convenience never becomes authority

UI may request operations. The server resolves permissions, identities, targets, revisions and mutations.

## 7. Simulation before spectacle

Prefer systems that produce durable behavior and causality over one-off generated text.

## 8. Evidence layers remain explicit

```text
unit/source policy
→ server GameTest
→ production candidate startup/restart
→ exact release workflow
→ installed server/client canary
→ real multi-client canary
```

Passing one layer never silently upgrades another.

---

# Current execution track

As of 2026-08-05:

```text
0.1.x reliability/security baseline                    COMPLETE
Memory 2.0 foundation                                  SUBSTANTIALLY IMPLEMENTED
MCA selective synchronization S1-S8                    COMPLETE
Operator Lore S9-S10c                                  COMPLETE
M11 Phase A risk catalog and GameTests                 COMPLETE
M11 Phase B exact production-JAR restart harness       COMPLETE
M11 Phase C provider/voice orchestration               COMPLETE
M11 Phase D concurrency and client acceptance          NEXT
0.1.25 installed inventory canary                      PENDING
legacy memory.json migration                           AFTER ACCEPTANCE/RELEASE GATE
```

Immediate sequence:

```text
M11 Phase D
→ exact-head review and all required CI
→ focused installed inventory/grave/resurrection canary
→ next sequential release containing post-0.1.25 work
→ additive legacy memory.json migration
→ remaining Memory 2.0 exit criteria
→ personality and NPC↔NPC social graph
```

---

# Immediate milestone — M11 Phase D

## Goal

Automate the concurrency and client-state boundaries that still require manual confidence, without weakening the S10b/S10c server-authority model.

Primary scenario:

```text
client A reads revision R
client B reads revision R
client A writes value A with R → OK, revision R2
client B writes value B with stale R → CONFLICT, canonical value A/R2
client B reloads/reviews
client B writes with R2 → OK, revision R3
```

Required assertions:

- exactly one stale writer is rejected;
- canonical value is never silently overwritten;
- conflict response includes the current canonical value and revision;
- no duplicate persistence mutation occurs;
- exact replay remains `UNCHANGED`;
- permissions and server target resolution are evaluated independently per logical client;
- client request generation rejects stale responses after scope or target changes.

## Client acceptance boundary

Automate as much of the following as the supported test runtime permits:

- operator-only editor opening;
- WORLD, PLAYER, VILLAGER and VILLAGE scopes;
- read before edit;
- edit/save/reopen;
- Clear through the same revision-protected write path;
- dirty close and scope-switch confirmation;
- explicit loading, saving, conflict and failure states;
- Save response retained while a confirmation modal is open;
- FORBIDDEN and NOT_FOUND handling;
- keyboard and resize-safe state behavior through deterministic model/controller tests;
- no global mailbox or unowned response path.

A full visual rendering claim is not required if the harness cannot run a real Minecraft client deterministically. State/model and network authority must still be automated; one real installed two-client UI canary remains separate.

## Security and authority invariants

Phase D must not:

- add arbitrary player/villager UUID fields to C2S;
- add arbitrary dimension or village identifiers;
- decide permissions on the client;
- bypass SHA-256 revision checks;
- silently overwrite on conflict;
- access `operator-lore.json` from client code;
- ingest Operator Lore into semantic memory;
- add unbounded packet payloads;
- call a public AI provider from CI;
- mix unrelated gameplay or Memory 2.0 migration work.

## Exit criteria

Phase D is complete only when:

- deterministic RED demonstrates the missing stale-writer/client-state acceptance;
- the minimal harness or test seam is implemented;
- logical two-client conflict acceptance is green;
- editor state acceptance is green;
- common tests, Fabric GameTests/build, NeoForge build, package verification and repository security pass;
- exact-head review finds no unresolved P0/P1/P2 issue;
- `docs/PROJECT_STATE.md`, this roadmap and the acceptance catalog are synchronized.

Canonical implementation boundary:

```text
docs/livingworld/VALIDATION_M11_PHASE_D_PLAN.md
```

---

# Versioned development roadmap

## 0.1.x — Reliability, security and compatibility baseline

### Goal

Make provider, voice, persistence and inherited MCA gameplay behavior safe enough for continued simulation development.

### Implemented

- hardened OpenAI-compatible response handling;
- controlled `content:null`, malformed JSON and provider errors;
- bounded retries with idempotent side effects;
- endpoint/credential policy and redirect rejection;
- bounded Chat/STT/TTS/error/verification responses;
- voice-duration and aggregate PCM limits;
- one monotonic complete voice-turn deadline;
- verified supply chain and deterministic security CI;
- Fabric and NeoForge gates;
- selective MCA fixes for tombstones, entity conversion, HOME POIs, navigation, mourning, gifts, fishing and mounted archers;
- Operator Lore store, immutable context, server-authoritative API and client editor;
- risk-based GameTests;
- exact production-JAR startup/shutdown/restart;
- exact-production release publication.

### Remaining exit gate

- M11 Phase D;
- focused installed inventory/grave/resurrection canary;
- next sequential release containing post-`0.1.25` work;
- retain short physical voice and real-client canaries.

---

## 0.2 — Memory 2.0

### Goal

Move from stored raw chat history to bounded layered memory.

```text
Working memory       recent turn-local context
Episodic memory      meaningful events and dialogue
Semantic memory      sourced FACT/BELIEF knowledge
Relationship memory causal social history
```

### Implemented

- persistent bounded episodic events;
- explicit event types and provenance;
- text/voice DIALOGUE parity;
- deterministic episodic retrieval;
- Working Memory bounds;
- semantic FACT/BELIEF model;
- controlled server-observed FACT ingestion;
- deterministic semantic retrieval, consolidation and source union;
- pressure-based forgetting/decay;
- source durability and NPC isolation;
- restart-safe world-local persistence.

### Remaining capabilities

- additive migration from `memory.json`;
- controlled BELIEF producers;
- explicit relationship-change reasons;
- long-horizon recall and multi-day soak;
- NPC-to-NPC knowledge transfer;
- rumor propagation with uncertainty and distortion.

### Migration requirements

```text
additive, never destructive
backup before mutation
explicit version/checkpoint
bounded import
deterministic event IDs
idempotent rerun
NPC ownership preserved
DIALOGUE never becomes automatic FACT
atomic writes
dry-run and rollback evidence
legacy reads retained until cutover acceptance
```

### Exit criteria

NPCs recall important events days later without requiring full raw dialogue history, while provenance, bounds, restart safety and rollback behavior remain proven.

---

## 0.3 — Personality and NPC↔NPC social graph

### Goal

Make each NPC a persistent individual and extend social state beyond player↔NPC.

Stable bounded personality dimensions may include:

```text
temperament
values
goals
fears
speech style
morality
ambition
curiosity
sociability
aggression
loyalty
```

NPC-pair state may include:

```text
friendship
trust
respect
fear
family
rivalry
romance
grudge
```

Generated biography must not replace deterministic identity or server-owned social state.

### Exit criteria

Two NPCs retain a persistent relationship history that affects dialogue, behavior and information exchange.

---

## 0.4 — Knowledge propagation and rumors

### Goal

Create a real information ecosystem.

Knowledge provenance distinguishes:

```text
OBSERVED
TOLD_BY_PLAYER
TOLD_BY_NPC
OFFICIAL
INFERRED
RUMOR
UNKNOWN
```

Propagation depends on trust, social proximity, emotional importance, community membership, memory and time. Rumors may lose detail or distort, but source history remains inspectable.

### Exit criteria

Information moves through a settlement without every NPC receiving omniscient server knowledge.

---

## 0.5 — Autonomous NPC agents

### Goal

Move from purely reactive conversation to budgeted server-authoritative behavior.

```text
perceive event/state
→ evaluate needs/goals/social context
→ choose bounded intent
→ server policy validation
→ act
→ observe result
→ remember
```

Candidate behaviors:

- flee danger;
- seek food, shelter or help;
- report threats;
- visit family and friends;
- investigate important events;
- pursue role-specific tasks;
- avoid feared or disliked entities.

The LLM proposes intent, never arbitrary Minecraft commands.

### Exit criteria

NPCs perform meaningful off-dialogue behavior without uncontrolled LLM calls or unsafe mutation.

---

## 0.6 — Settlement simulation

### Goal

Treat villages as social and economic systems.

Potential state:

- population and households;
- resources and shortages;
- professions and labor demand;
- safety, morale and reputation;
- public events and local priorities;
- bounded settlement memory.

### Exit criteria

Settlement conditions influence NPC behavior and dialogue through authoritative simulation state.

---

## 0.7 — Factions and politics

### Goal

Add persistent alliances, disputes, leadership, laws and inter-settlement relations.

### Exit criteria

Factions produce observable consequences through server-owned state rather than improvised dialogue alone.

---

## 0.8 — Emergent stories

### Goal

Turn accumulated memory, relationships, events and faction state into unscripted but causally grounded narratives.

### Exit criteria

The world produces multi-session stories whose causes can be reconstructed from persistent state and event history.

---

## 0.9 — Performance, large servers and local models

### Goal

Scale simulation safely.

- event-driven AI scheduling;
- global and per-NPC budgets;
- backpressure and admission control;
- cache and retrieval profiling;
- large multiplayer soak tests;
- optional local-model support without identity migration;
- observability without private-content leakage.

### Exit criteria

Large worlds remain bounded, diagnosable and restart-safe under sustained load.

---

## 1.0 — Persistent Living Society

### Goal

Ship a coherent system where identity, memory, relationships, knowledge, autonomy, settlements and factions form one persistent simulation layer.

```text
NPC Identity
+ Personality
+ Memory
+ Relationships
+ Knowledge
+ Voice
+ Goals
+ Autonomous Actions
+ Settlements
+ Factions
+ Emergent History
= VillAIgence
```

---

# Milestone governance

A milestone is not complete merely because code compiles.

Required progression:

```text
specification
→ RED regression boundary
→ minimal implementation
→ focused tests
→ relevant regression tests
→ Fabric + NeoForge
→ package verification
→ security policy
→ scope review
→ exact candidate artifact
→ live acceptance when runtime behavior requires it
→ canonical state update
```

Automated validation, exact-production candidate validation and installed-server/client validation must always be reported separately.
