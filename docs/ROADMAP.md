# VillAIgence Roadmap

> **Canonical product roadmap.** For exact implementation and validation state, read `docs/PROJECT_STATE.md` first.

> **Repository maintenance note (2026-08-01):** README canonical portfolio-link rollout completed through PR #90 (`19e397e` → squash `6209130`). VillAIgence CI #1154 / run `30714996167`, Java PR CI #654 / run `30714996171`, and security policy #505 / run `30714996231` all passed. Roadmap sequencing is unchanged; S10c remains next.


## Product vision

VillAIgence is evolving from an MCA-derived AI conversation mod into a **persistent living-society simulation layer for Minecraft**.

The goal is not merely NPCs that call an LLM. The target world contains NPCs that:

- retain stable identity, memory, personality, voice and relationships;
- know only what they observed, learned or were explicitly told;
- communicate naturally by text and voice;
- act through server-authoritative policies;
- form families, settlements, factions and social histories;
- exchange facts, beliefs and rumors with provenance;
- generate durable emergent stories rather than isolated AI tricks.

> **VillAIgence — Giving villagers a mind of their own.**

The compatible internal engine/data namespace remains `LivingWorld` / `livingworld`.

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
 episodic memory             remembered events/dialogue
```

Current observations win conflicts.

## 6. Client convenience never becomes authority

UI may request operations. The server resolves permissions, identities, targets, revisions and mutations.

## 7. Simulation before spectacle

Prefer systems that produce durable behavior and causality over one-off generated text.

---

# Current execution track

As of 2026-08-01:

```text
0.1.x reliability/security baseline                    COMPLETE
Memory 2.0 foundation                                  SUBSTANTIALLY IMPLEMENTED
MCA selective synchronization S1–S8                    AUTOMATED PASS
operator lore store S9                                 AUTOMATED PASS
immutable lore context S10a                            AUTOMATED PASS
server-authoritative lore API S10b                     AUTOMATED PASS
client editor UI S10c                                  NEXT
cumulative installed-server acceptance                 AFTER S10c
legacy memory.json migration                           AFTER RELEASE GATE
```

Immediate sequence:

```text
S10c client editor UI
→ exact synchronized release candidate
→ cumulative backed-up-world live acceptance
→ release promotion only on PASS
→ additive legacy memory.json migration
→ remaining Memory 2.0 exit criteria
→ 0.3 personality and NPC↔NPC social graph
```

---

# Versioned development roadmap

## 0.1.x — Reliability, security and compatibility baseline

### Goal

Make provider, voice, persistence and inherited MCA gameplay behavior safe enough for continued simulation development.

### Implemented baseline

- hardened OpenAI-compatible response handling;
- controlled `content:null`, malformed JSON and provider errors;
- bounded retries with idempotent side effects;
- endpoint/credential policy and redirect rejection;
- bounded Chat/STT/TTS/error/verification responses;
- voice-duration and aggregate PCM limits;
- verified supply-chain and deterministic security CI;
- Fabric and NeoForge build gates;
- selective MCA fixes for tombstones, entity conversion, HOME POIs, navigation, mourning, gifts, fishing and mounted archers;
- world-local operator lore, immutable prompt integration and server-authoritative editor API.

### Remaining exit gate

- S10c editor UI;
- one cumulative installed-server acceptance on an exact candidate JAR;
- restart/hash validation and normal Text/STT/Chat/TTS smoke;
- release promotion with recorded SHA-256 only after PASS.

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

NPCs recall important events days later without full raw dialogue history, while provenance, bounds, restart safety and rollback behavior remain proven.

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

The LLM proposes intent, never arbitrary Minecraft commands.

Candidate behaviors:

- flee danger;
- seek food, shelter or help;
- report threats;
- visit family and friends;
- investigate important events;
- pursue role-specific tasks;
- avoid feared or disliked entities.

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

# Immediate S10c specification boundary

S10c is a client experience over the already-complete S10b authority API.

Required UI capabilities:

- operator-only entry point;
- WORLD / VILLAGER / PLAYER / VILLAGE scope selector;
- nearby MCA villager selection for entity-bound scopes;
- canonical server read before editing;
- multiline text editor;
- code-point and UTF-8 budget indicators;
- revision-protected save and clear;
- explicit status feedback;
- conflict reload/review without blind overwrite;
- keyboard, mouse, resize and localization-safe layout.

The UI must never:

- access world files directly;
- accept arbitrary UUID/dimension/village ID;
- decide permissions locally;
- bypass S10b revision checks;
- send unbounded text;
- modify AI transport or semantic-memory rules.

---

# Milestone governance

A milestone is not complete merely because code compiles.

Required progression:

```text
specification
→ RED regression boundary
→ minimal implementation
→ unit tests
→ Fabric + NeoForge
→ package verification
→ security policy
→ scope review
→ exact candidate artifact
→ live acceptance when runtime behavior is involved
→ canonical state update
```

Automated validation and live-server validation must always be reported separately.
