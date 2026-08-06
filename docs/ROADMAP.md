# VillAIgence Roadmap

> **Canonical product roadmap.** For exact implementation and validation state, read `docs/PROJECT_STATE.md` first.
>
> Last reconciled: **2026-08-06**. M11 Phases A–E are complete at the automation layer. The immediate delivery boundary is final PR #114 review, an exact versioned candidate and six minimal installed canaries. Additive legacy-memory migration follows that boundary.

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
→ common integration
→ server GameTest
→ production candidate startup/restart
→ exact release dry-run
→ installed server/client canary
→ real graphical multi-client/physical voice canary
```

Passing one layer never silently upgrades another.

## 9. CI optimization fails closed

Only reviewed path classifications may narrow pull-request work. Empty, unknown, unsafe, workflow, build, release and production-fixture changes select the complete mandatory matrix. Release mode always selects all suites.

---

# Current execution track

As of 2026-08-06:

```text
0.1.x reliability/security baseline                    COMPLETE
Memory 2.0 foundation                                  SUBSTANTIALLY IMPLEMENTED
MCA selective synchronization S1-S8                    COMPLETE
Operator Lore S9-S10c                                  COMPLETE
M11 Phases A-D                                          COMPLETE
M11 Phase E automation completion E0-E9                 COMPLETE AT AUTOMATION LAYER
acceptance catalog                                      28 AUTOMATED / 6 MANUAL / 0 PLANNED
PR #114                                                 DRAFT / UNMERGED
installed graphical and physical canaries               PENDING
next release containing post-0.1.25 work               NOT REQUESTED
legacy memory.json migration                           AFTER RELEASE BOUNDARY
```

Immediate sequence:

```text
final exact-head CI, soak and change review
→ resolve next free sequential version
→ build exact versioned dry-run candidate
→ run six minimal installed canaries
→ publish only on PASS
→ verify published identity and one restart
→ additive legacy memory.json migration
→ remaining Memory 2.0 exit criteria
→ personality and NPC↔NPC social graph
```

---

# Completed milestone — M11 automation program

## Phases A–D

Completed foundations:

- risk-based acceptance catalog;
- real Fabric GameTests;
- exact remapped production-JAR startup, stop, save and restart;
- deterministic provider integration;
- one complete voice-turn deadline;
- exactly-once dialogue and relationship effects;
- authenticated text ownership;
- two-session Operator Lore conflict and response isolation.

## Phase E — automation completion

### Goal

Move every deterministic release risk into repeatable CI and reduce routine manual testing to boundaries that require a real graphical client, physical device, audible perception or exact operator environment.

### Completed workstreams

```text
E0  configuration-cache-safe production staging
E1  duplicate identity and resurrection replay guard
E2  real lifecycle evidence across two JVMs
E3  six-case corrupt persistence recovery matrix
E4  authenticated text transport
E5  owner-bound two-session Operator Lore transport
E6  real Opus codec/loss/order/resource acceptance
E7  gifts, fishing and mounted archer gameplay tests
E8  obstacle, ladder, door, mount and ranged navigation matrix
E9  fail-closed suite selection and constrained-heap production soak
```

### Acceptance catalog result

```text
34 total scenarios
28 AUTOMATED
6 MANUAL_CANARY
0 PLANNED
```

### E9 suite model

```text
fast       common tests and deterministic Python contracts
server     risk catalog, 16 Fabric GameTests, Fabric and NeoForge
production exact candidate startup/restart and lifecycle
recovery   six destructive store-recovery cases
package    distributable smoke and identity checks
```

Release validation always selects all five.

### Production soak result

Validated implementation head `20fb7fd741916ffcb3f7f4630fd6d0bb046efba7` passed:

- three clean authenticated text/Operator Lore repetitions under 512 MiB test JVM heap;
- exact candidate staging under constrained Gradle workers;
- five production JVM cycles under 512 MiB server heap;
- clean startup/ready/stop/save/exit in every cycle;
- one lifecycle NPC in every cycle;
- real voice transport `PASS` in every cycle;
- identical hashes for all six persistent stores.

Canonical evidence:

```text
docs/livingworld/VALIDATION_M11_PHASE_E_E9.md
```

### Manual boundary retained deliberately

Automation does not claim:

- operator-machine startup of the exact published JAR;
- visible selected-NPC response rendering;
- real-player Silk Touch pickup/placement UI interaction;
- two graphical-client conflict presentation;
- OS microphone permission and client UDP routing;
- audible/spatial subjective output quality;
- ordinary autonomous NPC-brain behavior in the operator world.

These are represented by six small `MANUAL_CANARY` scenarios rather than broad repeated manual regression.

---

# Immediate delivery milestone — exact candidate and installed acceptance

## Goal

Promote PR #114 and accumulated post-`0.1.25` work through one exact versioned candidate. Publish only after the six remaining installed canaries pass.

## Required sequence

```text
complete final PR #114 change review
→ confirm all exact-head workflows are green
→ resolve next free sequential version from current tags
→ open release-request PR
→ build exact versioned dry-run artifact
→ verify tag/filename/embedded metadata/manifest identity
→ install exact candidate on operator server and clients
→ execute the six minimal installed canaries
→ merge release request only on PASS
→ verify published asset byte identity
→ one post-release restart smoke
```

`0.1.26+1.21.1` is only an expectation. It must be confirmed free from current repository/tag evidence before use.

## Six installed canaries

1. Exact candidate reaches full operator-server startup without Mixin/refmap failure.
2. Two ordinary MCA NPCs visibly escape reachable water and retain land movement.
3. An installed client visibly addresses one selected NPC and renders one response.
4. A real player completes the Silk Touch grave break/pickup/placement/restart interaction without loss or duplication.
5. One physical microphone turn reaches the NPC and one spatial response is audibly correct.
6. Two graphical clients visibly expose Operator Lore conflict, retain/review the stale draft and complete an explicit retry.

Automated internals must not be manually re-tested unless an installed canary exposes contradictory evidence.

## Exit criteria

The release boundary is complete only when:

- final PR #114 exact-head automation and review pass;
- the exact versioned dry-run passes the complete release matrix;
- all six installed canaries pass;
- persistent data remains valid across restart;
- release publication occurs only after installed PASS;
- published JAR is byte-identical to the accepted candidate;
- canonical state, roadmap and release evidence are synchronized.

## Out of scope

- legacy-memory migration;
- BELIEF producers;
- personality/social graph;
- rumor propagation;
- provider redesign;
- unrelated MCA synchronization.

---

# Versioned development roadmap

## 0.1.x — Reliability, security and compatibility baseline

### Implemented

- hardened provider parsing and error handling;
- bounded retries and exactly-once effects;
- endpoint/credential/redirect policy;
- bounded Chat/STT/TTS and PCM;
- one monotonic complete voice-turn deadline;
- deterministic provider and real Opus acceptance;
- verified supply chain and security CI;
- Fabric/NeoForge gates;
- selective MCA tombstone, conversion, HOME, navigation, mourning, gift, fishing and mounted-archer corrections;
- Operator Lore store/API/editor/concurrency acceptance;
- exact production startup/restart and release publication gates;
- fail-closed path-to-risk CI selection;
- corrupt-store recovery matrix;
- five-cycle constrained-heap production soak;
- 28 automated catalog scenarios with zero planned gaps.

### Remaining exit gate

- final PR #114 review/merge boundary;
- exact post-`0.1.25` candidate;
- six installed canaries;
- next sequential release and post-release restart verification.

---

## 0.2 — Memory 2.0

### Goal

Move from raw chat history to bounded layered memory.

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
- deterministic retrieval;
- Working Memory bounds;
- semantic FACT/BELIEF model;
- controlled server-observed FACT ingestion;
- deterministic consolidation and source union;
- pressure-based forgetting;
- source durability, NPC isolation and restart safety.

### Next implementation package — additive legacy migration

After the release boundary, migrate useful legacy `memory.json` history without deleting or reinterpreting the source.

Required properties:

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

Recommended slices:

1. migration inventory and schema/checkpoint design;
2. deterministic dry-run parser/report;
3. RED duplicate/rerun/partial-failure tests;
4. bounded additive import;
5. backup and rollback verification;
6. same-world restart acceptance;
7. optional cutover only after installed evidence.

### Remaining capabilities

- controlled BELIEF producers;
- explicit relationship-change reasons;
- long-horizon recall and multi-day simulation;
- NPC-to-NPC knowledge transfer;
- rumor propagation with uncertainty and distortion.

---

## 0.3 — Personality and NPC↔NPC social graph

### Goal

Make each NPC a persistent individual and extend social state beyond player↔NPC.

Potential bounded dimensions:

```text
temperament, values, goals, fears, speech style,
morality, ambition, curiosity, sociability, aggression, loyalty
```

NPC-pair state may include friendship, trust, respect, fear, family, rivalry, romance and grudges.

Generated biography must not replace deterministic identity or server-owned social state.

---

## 0.4 — Knowledge propagation and rumors

### Goal

Create a provenance-aware information ecosystem.

```text
OBSERVED
TOLD_BY_PLAYER
TOLD_BY_NPC
OFFICIAL
INFERRED
RUMOR
UNKNOWN
```

Propagation depends on trust, social proximity, emotional importance, community membership, memory and time. Distortion is permitted only with inspectable source history.

---

## 0.5 — Autonomous NPC agents

```text
perceive
→ evaluate needs/goals/social context
→ choose bounded intent
→ server validation
→ act
→ observe result
→ remember
```

The LLM proposes intent, never arbitrary Minecraft commands.

---

## 0.6 — Settlement simulation

Population, households, resources, professions, safety, morale, reputation, public events and bounded settlement memory influence authoritative NPC behavior.

---

## 0.7 — Factions and politics

Persistent alliances, disputes, leadership, laws and inter-settlement relations produce server-owned consequences rather than improvised dialogue alone.

---

## 0.8 — Emergent stories

Accumulated memory, relationships, events and faction state produce multi-session stories whose causes remain reconstructable.

---

## 0.9 — Performance, large servers and local models

- event-driven scheduling;
- global/per-NPC budgets;
- backpressure and admission control;
- profiling and observability;
- multi-day/large-world soak;
- optional local models without identity migration.

The five-cycle Phase E soak is a bounded release-regression gate, not the final large-server scalability milestone.

---

## 1.0 — Persistent Living Society

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

A milestone is not complete because code compiles.

Required progression:

```text
specification
→ meaningful RED
→ minimal implementation
→ focused tests
→ regression tests
→ Fabric + NeoForge
→ security and supply-chain policy
→ production candidate
→ recovery/package/soak evidence as applicable
→ exact release dry-run
→ installed canary when physical/visual behavior requires it
→ canonical documentation
→ independent change review
```

Automated validation, production-candidate validation, exact-release validation and installed evidence must always be reported separately.
