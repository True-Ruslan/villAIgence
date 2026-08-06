# VillAIgence Roadmap

> **Canonical product roadmap.** Read `docs/PROJECT_STATE.md` first for exact implementation and validation state.
>
> Last reconciled: **2026-08-06**. M11 Phases A–E are merged and complete at the automation layer. The immediate delivery boundary is the exact `0.1.26+1.21.1` candidate plus six installed canaries. Additive legacy-memory migration follows release verification.

## Product vision

VillAIgence is evolving from an MCA-derived AI conversation mod into a **persistent living-society simulation layer for Minecraft**.

The target world contains NPCs that:

- retain stable identity, memory, personality, voice and relationships;
- know only what they observed, learned or were explicitly told;
- communicate naturally by text and voice;
- act through server-authoritative policy;
- form families, settlements, factions and social histories;
- exchange facts, beliefs and rumors with provenance;
- generate durable emergent stories rather than isolated AI tricks.

> **VillAIgence — Giving villagers a mind of their own.**

Compatibility-sensitive internal naming remains `mca`, `LivingWorld` and `livingworld` until an explicit migration is designed.

---

# Architecture principles

1. **LLM is not authority.** Server state is truth; the LLM proposes bounded dialogue or intent.
2. **Identity outlives providers.** Changing provider/model must not regenerate NPC identity, memories, relationships or voice.
3. **Fail soft without corruption.** Provider, voice, packet and auxiliary-store failures become controlled states.
4. **Persistence is explicit and world-local.** Important data belongs under `<world>/livingworld/`.
5. **Provenance layers stay separate.** Observations, operator lore, semantic knowledge and episodic memory are not interchangeable.
6. **Client convenience never becomes authority.** Permissions, identities, targets, revisions and mutations are server-owned.
7. **Simulation before spectacle.** Prefer durable causal systems over one-off generated text.
8. **Evidence layers remain explicit.** Unit, integration, GameTest, production candidate, exact release and installed evidence are separate claims.
9. **Unknown CI changes fail closed.** Protected, unsafe, unclassified and persistence-store changes select the complete required matrix.

---

# Current execution track

```text
0.1.x reliability/security baseline                    COMPLETE
Memory 2.0 foundation                                  SUBSTANTIALLY IMPLEMENTED
MCA selective synchronization S1-S8                    COMPLETE
Operator Lore S9-S10c                                  COMPLETE
M11 Phases A-E                                          MERGED / AUTOMATION COMPLETE
0.1.26 exact release-request dry run                    NEXT
six installed graphical/physical canaries              AFTER DRY RUN
0.1.26 publication                                      ONLY AFTER INSTALLED PASS
legacy memory.json migration                            AFTER RELEASE VERIFICATION
```

Immediate sequence:

```text
open release/0.1.26+1.21.1 PR
→ exact non-publishing release dry run
→ preserve candidate JAR + checksum + dependency manifest
→ install the exact candidate
→ execute six minimal installed canaries
→ merge release PR only on PASS
→ verify official assets and one restart
→ additive memory.json migration
```

---

# Completed milestone — M11 Phase E

Merged through PR #114 at:

```text
c51201d7a37b9d09c9a8cb490d1c56f3f6921c1f
```

Phase E moved all deterministic release risks into repeatable CI:

- configuration-cache-safe production staging;
- duplicate UUID resurrection/replay rejection;
- real death, portable grave, resurrection and restart lifecycle;
- corrupt persistence backup/regeneration/idempotency matrix;
- authenticated text ownership and exactly-once effects;
- authenticated two-session Operator Lore conflict/retry;
- real Simple Voice Chat Opus/loss/order/resource evidence;
- gifts, fishing, mounted archer, water, obstacle, ladder and door GameTests;
- fail-closed path-to-risk selector;
- constrained-heap repeated concurrency and five-JVM production soak.

Acceptance catalog:

```text
34 total
28 AUTOMATED
6 MANUAL_CANARY
0 PLANNED
```

The remaining six scenarios are not missing deterministic tests; they specifically require an installed environment, graphical rendering, physical microphone/UDP or subjective audible/spatial judgment.

---

# Immediate release milestone — 0.1.26+1.21.1

## Goal

Promote all post-`0.1.25` work through the exact-production release gate without treating CI as installed-client evidence.

Candidate branch:

```text
release/0.1.26+1.21.1
```

Release-request files:

```text
docs/releases/NEXT_RELEASE.txt
docs/releases/0.1.26+1.21.1.md
```

## Automated candidate requirements

The release-request PR must pass:

- version/tag availability and Minecraft-version contract;
- repository security and supply-chain policy;
- common and deterministic provider tests;
- sixteen required Fabric GameTests;
- Fabric and NeoForge builds;
- exact production candidate startup, stop/save and restart;
- identity/inventory lifecycle evidence;
- six-case destructive persistence recovery;
- production Simple Voice Chat transport evidence;
- distributable package smoke;
- byte identity between production-accepted and packaged JAR.

The PR workflow is non-publishing.

## Installed canaries

Use the exact dry-run JAR, not a local or snapshot build.

1. exact candidate starts on the operator server and a client connects;
2. two ordinary MCA NPC brains escape reachable water and retain land movement;
3. an installed client addresses the selected NPC and renders exactly one text response;
4. real Silk Touch grave pickup, placement, restart and resurrection preserve UUID, name and exact inventory once;
5. one physical microphone turn reaches the NPC and one spatial reply is audible without duplicate playback;
6. two graphical clients visibly expose and resolve an Operator Lore stale-revision conflict while preserving both drafts.

## Exit criteria

`0.1.26` is complete only when:

- exact PR dry-run passes;
- all six installed canaries pass on the same candidate JAR;
- the release PR is merged only after that installed PASS;
- the merge-commit release workflow passes and publishes immutable assets;
- the official JAR hash and asset identity are recorded;
- one post-release restart passes;
- `PROJECT_STATE.md`, this roadmap and release validation evidence are synchronized.

## Out of scope

- legacy-memory migration;
- BELIEF producers;
- personality/social graph;
- rumor propagation;
- unrelated provider redesign;
- unrelated MCA synchronization.

---

# Versioned product roadmap

## 0.1.x — Reliability, security and compatibility

Status: **release boundary in progress**.

Implemented:

- provider parsing and transport hardening;
- bounded retries/deadlines and exactly-once effects;
- endpoint/credential/redirect policy;
- deterministic text, voice and Operator Lore acceptance;
- world-local persistence recovery;
- selective MCA gameplay corrections;
- exact production startup/restart and package identity;
- risk-based GameTests and bounded soak.

Remaining exit gate:

- exact `0.1.26` candidate;
- six installed canaries;
- immutable release and post-release restart.

## 0.2 — Memory 2.0

Goal: move from stored raw chat history to bounded layered memory.

```text
Working memory       recent turn-local context
Episodic memory      meaningful events and dialogue
Semantic memory      sourced FACT/BELIEF knowledge
Relationship memory causal social history
```

Already implemented:

- episodic events and explicit provenance;
- text/voice DIALOGUE parity;
- deterministic retrieval;
- bounded Working Memory;
- semantic FACT/BELIEF model;
- controlled server-observed FACT ingestion;
- consolidation/source union;
- deterministic forgetting;
- restart-safe world-local stores.

### Next package — additive legacy `memory.json` migration

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

Remaining Memory 2.0 capabilities:

- controlled BELIEF producers;
- explicit relationship-change reasons;
- long-horizon/multi-day recall;
- NPC-to-NPC knowledge transfer;
- rumor propagation with uncertainty and distortion.

## 0.3 — Personality and NPC↔NPC social graph

Goal: persistent bounded personality and social state between NPC pairs.

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

NPC-pair state may include friendship, trust, respect, fear, family, rivalry, romance and grudges.

Exit criterion: two NPCs retain relationship history that affects dialogue, behavior and information exchange.

## 0.4 — Knowledge propagation and rumors

Goal: a provenance-aware information ecosystem.

```text
OBSERVED
TOLD_BY_PLAYER
TOLD_BY_NPC
OFFICIAL
INFERRED
RUMOR
UNKNOWN
```

Exit criterion: information moves through a settlement without omniscient distribution and source history remains inspectable.

## 0.5 — Autonomous NPC agents

Goal: budgeted server-authoritative behavior.

```text
perceive
→ evaluate needs/goals/social context
→ choose bounded intent
→ server policy validation
→ act
→ observe result
→ remember
```

The LLM proposes intent, never arbitrary Minecraft commands.

## 0.6 — Settlement simulation

Goal: villages become social/economic systems with population, households, resources, professions, safety, morale and bounded public memory.

## 0.7 — Factions and politics

Goal: persistent alliances, disputes, leadership, laws and inter-settlement relations with server-owned consequences.

## 0.8 — Emergent stories

Goal: multi-session narratives grounded in persistent events, relationships, settlements and faction state.

## 0.9 — Performance, large servers and local models

Goal: event-driven scheduling, global/per-NPC budgets, backpressure, profiling, large multiplayer soak and optional local models without identity migration.

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
→ independent review
→ exact candidate artifact
→ installed acceptance where required
→ canonical state update
```

Automated validation, exact-production candidate validation, exact-release validation and installed-server/client validation must always be reported separately.
