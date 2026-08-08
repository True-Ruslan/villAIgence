# VillAIgence Roadmap

> **Canonical product roadmap.** Read `docs/PROJECT_STATE.md` first for exact implementation and validation state. Read root `CHANGELOG.md` for release/product history.
>
> Last reconciled: **2026-08-08**, after PR #123 added the controlled Semantic Memory BELIEF admission boundary.

## Product vision

VillAIgence is evolving from an MCA-derived AI conversation mod into a **persistent living-society simulation layer for Minecraft**.

The target world contains NPCs that:

- retain stable identity, memory, personality, voice and relationships;
- know only what they observed, learned or were explicitly told;
- distinguish authoritative facts from fallible beliefs and rumors;
- communicate naturally by text and voice;
- act only through server-authoritative policy;
- form families, settlements, factions and social histories;
- exchange information with provenance and uncertainty;
- generate durable emergent stories rather than isolated AI tricks.

> **VillAIgence — Giving villagers a mind of their own.**

Compatibility-sensitive internal naming remains `mca`, `LivingWorld` and `livingworld` until an explicit migration is justified and designed.

---

# Architecture principles

1. **LLM is not authority.** Server state is truth; the model proposes bounded dialogue, claims or intent.
2. **Identity outlives providers.** Changing model/provider must not regenerate NPC identity, memory, relationships or voice.
3. **Fail soft without corruption.** Provider, voice, packet and auxiliary-store failures become controlled states.
4. **Persistence is explicit and world-local.** Important state lives under `<world>/livingworld/`.
5. **Provenance layers stay separate.** Observation, operator lore, episodic memory, FACT, BELIEF and rumor are not interchangeable.
6. **Confidence is not authority.** BELIEF never becomes FACT because a model is confident.
7. **Current observations outrank recollection.** Current server-observed facts override conflicting lore or beliefs.
8. **Client convenience never becomes authority.** Permissions, identities, targets, revisions and mutations remain server-owned.
9. **Simulation before spectacle.** Prefer durable causal systems over one-off generated text.
10. **Evidence layers remain explicit.** Unit, integration, GameTest, production candidate, exact release and installed evidence are separate claims.
11. **Unknown CI changes fail closed.** Protected, unsafe and unclassified changes select the complete required matrix.
12. **Compatibility work requires a supported-data reason.** Experimental pre-1.0 state is not automatically entitled to migration code.
13. **Release identity is immutable.** Recovery may restore assets/metadata only from an existing verified tag commit and never moves the tag.
14. **Changelog is part of delivery.** Notable runtime, persistence, config, release, security and permanent-CI changes update root `CHANGELOG.md` in the same PR.

---

# Current execution track

```text
0.1.x reliability/security/compatibility               COMPLETE
MCA selective synchronization S1-S8                    COMPLETE
Operator Lore S9-S10c                                  COMPLETE
M11 Phases A-E                                         COMPLETE AT AUTOMATION LAYER
release/recovery automation                            COMPLETE / VERSION-AWARE

0.2.0 Memory 2.0 persistent-dialogue clean cutover      RELEASED / INSTALLED ACCEPTED
legacy memory.json migration                           CANCELLED BY DESIGN
controlled BELIEF admission contract                   COMPLETE / PR #123
bounded inspectable claim extraction                   NEXT
causal relationship reasons                            NEXT
long-horizon recall                                    LATER 0.2
NPC-to-NPC knowledge transfer                          LATER 0.2
provenance-aware rumors                                LATER 0.2
```

Immediate sequence:

```text
bounded claim-candidate schema
→ tests-first extraction contract
→ fail-soft optional extractor integration
→ exactly-once BELIEF persistence under retry/replay
→ current FACT > conflicting BELIEF retrieval tests
→ trustworthy causal relationship reasons
→ long-horizon recall
→ NPC-to-NPC knowledge transfer
→ rumors with provenance, uncertainty and bounded distortion
```

`VAI-CONCUR-004` remains deferred until two real graphical clients are available. It must remain `NOT TESTED`, but it does not block current product development because server-side concurrency semantics are already automated.

---

# Completed milestone — 0.1.x reliability and M11 automation

The 0.1 line established the reliability/security platform on which later simulation work depends.

Implemented and verified across the line:

- provider parsing and transport hardening;
- bounded retries/deadlines/backpressure and exactly-once effects;
- endpoint/credential/redirect policy;
- deterministic text, voice and Operator Lore acceptance;
- world-local persistence recovery;
- selective MCA gameplay/navigation corrections;
- exact production startup/restart and package identity;
- risk-based Fabric GameTests;
- Fabric + NeoForge build compatibility;
- constrained-heap soak;
- immutable release artifact verification;
- version-aware recovery of incomplete GitHub Release publication.

M11 Phase E completed the deterministic automation program:

```text
34 catalog scenarios
28 AUTOMATED
6 MANUAL_CANARY
0 PLANNED
```

The remaining manual scenarios require installed graphical clients, a physical microphone/UDP path or subjective audible/spatial judgment rather than missing deterministic unit coverage.

Historical exact details remain in root `CHANGELOG.md`, `docs/CHANGELOG.md`, and version-specific validation documents.

---

# Current official release — 0.2.0+1.21.1

`0.2.0` begins the Memory 2.0 release line.

```text
tag:                    0.2.0+1.21.1
release commit:         e426f588efefa6aa48a6e536c4a998421bbda241
installed candidate SHA:56293f86634b50b2def044429aac6f2cf0d197eb16ac1e60224708f7b3333aee
```

Installed clean-state result:

```text
required:          7 PASS / 0 FAIL
VAI-M2-INST-005:   NOT TESTED / AUTOMATED EVIDENCE ONLY
VAI-CONCUR-004:    NOT TESTED / DEFERRED
```

The release intentionally removed the experimental raw `memory.json` conversation store from current runtime/recovery. The accepted pre-1.0 rollout boundary is a clean LivingWorld state; no legacy conversation importer or dual-reader is planned.

---

# 0.2 — Memory 2.0

## Goal

Move from raw conversation history to bounded, layered, provenance-aware memory that can support social simulation without making the LLM omniscient or authoritative.

```text
Working Memory        recent bounded prompt context
Episodic Memory       meaningful events and dialogue
Semantic Memory       sourced FACT/BELIEF knowledge
Relationship Memory   causal social history
```

## Implemented foundation

- immutable episodic MemoryEvents;
- DIALOGUE / OBSERVATION / ACTION / RELATIONSHIP_CHANGE;
- structured text/voice DIALOGUE payloads;
- exact NPC/player isolation;
- deterministic retrieval and idempotency;
- bounded Working Memory;
- typed FACT/BELIEF semantic entries;
- controlled `SYSTEM_OBSERVED` FACT ingestion;
- deterministic consolidation/source union;
- deterministic pressure-based forgetting;
- restart-safe world-local persistence;
- current observed facts outrank conflicting recalled context.

## Completed — persistent-dialogue clean cutover

Released in `0.2.0+1.21.1`.

```text
successful text/voice turn
→ one post-success Memory2DialogueLifecycle write
→ structured DIALOGUE MemoryEvent
→ memory2.json

next turn
→ exact NPC/player DIALOGUE retrieval
→ filter before limit
→ chronological user/assistant reconstruction
→ Working Memory bounds
→ prompt
```

Explicitly not part of current architecture:

```text
NO legacy memory.json importer
NO migration checkpoint ledger
NO dual persistent reads
NO summary parsing to recover dialogue roles
```

## Completed — controlled BELIEF admission contract

Merged through PR #123.

Required truth contract:

```text
SYSTEM_OBSERVED → FACT path only
PLAYER_TOLD     → BELIEF only
NPC_TOLD        → BELIEF only
INFERRED        → BELIEF only
```

Implemented admission boundary:

- `PLAYER_TOLD` requires a matching `PLAYER_TOLD` DIALOGUE source;
- `NPC_TOLD` requires a matching `NPC_TOLD` DIALOGUE source;
- `INFERRED` remains non-authoritative and retains explicit persisted source evidence;
- `SYSTEM_OBSERVED` is rejected through the BELIEF API;
- missing/blank/unsupported input fails closed;
- source-event identity is derived from the persisted MemoryEvent;
- exact replay is idempotent;
- equivalent corroborating claims use existing deterministic source-union consolidation;
- ordinary dialogue still creates no semantic entry unless a controlled producer supplies a claim candidate.

TDD evidence is recorded in PR #123 and `docs/livingworld/SEMANTIC_INGESTION.md`.

## NEXT — bounded inspectable claim extraction

### Goal

Produce candidate semantic claims from dialogue/evidence without allowing an extractor or LLM to write authoritative knowledge directly.

Required flow:

```text
persisted dialogue/evidence
→ bounded candidate extractor
→ inspectable candidate schema
→ SemanticBeliefAdmissionPolicy
→ BELIEF persistence
```

### Required contract

The extraction layer must:

- return an explicit bounded schema, never arbitrary storage mutations;
- identify proposed statement, provenance and relevant related entities;
- preserve the source MemoryEvent identity supplied by the server;
- create no entry for empty/failed dialogue;
- create no entry for provider failure;
- fail soft on malformed extractor output;
- remain safe under retry/replay;
- never emit or promote FACT;
- never bypass admission policy;
- keep current observed world facts authoritative.

### TDD slices

1. **RED — pure extraction contract**
   - valid single candidate;
   - no candidate;
   - malformed candidate;
   - overlong/unbounded candidate;
   - unsupported provenance;
   - duplicate/replayed response.
2. **GREEN — minimal provider-independent candidate model/parser**
3. **RED/GREEN — optional provider adapter**
   - null/empty/error/timeout;
   - bounded response;
   - no hidden reasoning persistence.
4. **Integration**
   - candidate → admission → one semantic entry;
   - retry does not duplicate;
   - rejected candidate writes nothing;
   - unrelated dialogue remains episodic only.
5. **Retrieval precedence**
   - current `SYSTEM_OBSERVED` FACT/context wins over a conflicting BELIEF.

### Exit criterion

VillAIgence can learn a bounded non-authoritative claim from controlled dialogue/evidence, preserve where that claim came from, survive retries/restart without duplication, and never confuse the learned claim with server truth.

## NEXT — trustworthy causal relationship memory

### Goal

Relationship history should record a causal reason only when the server has trustworthy evidence for one.

Required properties:

- numeric relationship transition and explanation remain distinct;
- reason is bounded;
- reason is tied to a validated server event or controlled conversational source;
- exact before/after relationship state and source event IDs remain available;
- free-form LLM text never becomes an authoritative causal explanation by itself;
- replay does not duplicate relationship history;
- reasons are queryable for future dialogue/personality behavior.

### Exit criterion

An NPC can explain a relationship change using inspectable source-backed history rather than invented retrospective reasoning.

## Later 0.2 — long-horizon recall

Add deterministic scenarios proving important memories survive realistic time, pressure and restart while weak memories decay as designed.

Required evidence should include:

- multi-session recall;
- multi-day game-time ordering;
- retention under capacity pressure;
- relationship-memory retrieval;
- current observations outranking stale belief;
- no cross-NPC/player leakage.

## Later 0.2 — NPC-to-NPC knowledge transfer

Use the existing `NPC_TOLD` admission contract.

Target flow:

```text
NPC A owns sourced knowledge
→ bounded social exchange
→ NPC B receives explicit told claim
→ NPC_TOLD BELIEF with source chain
→ later retrieval
```

No global/omniscient knowledge distribution.

## Later 0.2 — rumors

Build on NPC-to-NPC transfer with explicit uncertainty and distortion.

Possible fields/semantics:

```text
origin source
speaker chain
confidence
uncertainty
contradiction state
distortion count / bounded transformation
```

A rumor remains non-authoritative even when repeated by many NPCs.

## 0.2 exit criterion

Memory 2.0 is complete when persistent NPC memory is:

- layered;
- bounded;
- provenance-aware;
- restart-safe;
- deterministic under replay;
- able to learn controlled non-authoritative claims;
- able to retain causal relationship history;
- able to transfer knowledge between NPCs without omniscience;
- able to represent rumors/contradictions without turning them into FACT;
- independent of the removed raw conversation store.

---

# 0.3 — Personality and NPC↔NPC social graph

## Goal

Persistent bounded personality plus pairwise social state that changes dialogue and behavior.

Potential personality dimensions:

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

Pair state may include:

```text
friendship
trust
respect
fear
family
rivalry
romance
grudges
```

Personality is persistent game state, not a fresh LLM-generated profile on every conversation.

### Exit criterion

Two NPCs retain durable relationship/personality history that affects dialogue, decisions and information exchange after restart.

---

# 0.4 — Knowledge ecosystem and rumors

## Goal

Expand the 0.2 transfer primitives into settlement-scale provenance-aware information flow.

Target knowledge classes may include:

```text
OBSERVED
TOLD_BY_PLAYER
TOLD_BY_NPC
OFFICIAL
INFERRED
RUMOR
UNKNOWN
```

### Exit criterion

Information moves through a settlement without omniscient distribution, conflicts remain representable, and source history remains inspectable.

---

# 0.5 — Autonomous NPC agents

## Goal

Budgeted server-authoritative behavior based on needs, goals, social context and remembered information.

```text
perceive
→ evaluate needs/goals/social context
→ choose bounded intent
→ server policy validation
→ act
→ observe result
→ remember
```

The LLM may propose intent but never arbitrary Minecraft commands.

Required controls:

- event-driven scheduling rather than per-tick LLM calls;
- per-NPC/global budgets;
- action whitelist/policy;
- server-side target revalidation;
- bounded retry/backpressure;
- exactly-once effects.

### Exit criterion

NPCs can pursue simple persistent goals autonomously without compromising server authority or performance.

---

# 0.6 — Settlement simulation

## Goal

Villages become persistent social/economic systems.

Potential state:

- population and households;
- professions and work capacity;
- resources and shortages;
- safety and threats;
- morale;
- public knowledge/memory;
- shared projects and needs.

### Exit criterion

Settlement state changes over time and meaningfully affects individual NPC goals and behavior.

---

# 0.7 — Factions and politics

## Goal

Persistent alliances, disputes, leadership, rules and inter-settlement relations with server-owned consequences.

### Exit criterion

Faction/political state survives restart, is causally grounded in simulation events, and changes NPC/settlement behavior.

---

# 0.8 — Emergent stories

## Goal

Multi-session narratives grounded in persistent events, memories, relationships, settlements and factions.

The system should not generate a story first and retrofit state afterward. Story is the human-readable consequence of simulation history.

### Exit criterion

Players can return after multiple sessions and encounter explainable ongoing social narratives rooted in recorded world history.

---

# 0.9 — Performance, large servers and local models

## Goal

Scale the living society without turning AI into a per-NPC-per-tick cost center.

Work includes:

- event-driven scheduling;
- global/per-NPC model budgets;
- backpressure and cancellation;
- cache/retrieval profiling;
- large-population simulation soak;
- multi-day stability evidence;
- optional local models;
- provider replacement without identity migration.

### Exit criterion

Large populations remain bounded in CPU, memory, provider calls and persistence growth under realistic server workloads.

---

# 1.0 — Persistent Living Society

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

1.0 means the systems above form one coherent persistent simulation, not merely a collection of AI features.

---

# Delivery and TDD governance

A milestone is not complete because code compiles or one CI job is green.

Runtime behavior follows:

```text
specification
→ RED regression/contract test
→ observe intended RED
→ minimal GREEN implementation
→ focused tests
→ relevant regression suite
→ Fabric + NeoForge where applicable
→ production/server acceptance
→ security policy
→ soak/release dry-run when selected
→ independent diff review
→ exact candidate / installed acceptance when required
→ root CHANGELOG.md update
→ PROJECT_STATE / ROADMAP reconciliation when delivery boundary changes
```

Rules:

1. Do not write production behavior before the intended RED has been observed.
2. Do not weaken assertions merely to make CI green.
3. Exact-release and installed evidence are separate from unit/automation evidence.
4. Deferred manual evidence remains explicitly deferred.
5. Significant product/runtime/persistence/config/release/security/permanent-CI changes update root `[Unreleased]` in the same PR.
6. Release PRs move shipped `[Unreleased]` items into the exact version section rather than duplicating them.
7. Before starting new work, reconcile these documents against live GitHub state.
