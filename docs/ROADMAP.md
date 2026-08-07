# VillAIgence Roadmap

> **Canonical product roadmap.** Read `docs/PROJECT_STATE.md` first for exact implementation and validation state.
>
> Last reconciled: **2026-08-07**. M11 Phases A–E are merged and complete at the automation layer. `0.1.26+1.21.1` is published with verified immutable assets and installed acceptance of `5 PASS / 0 FAIL / 1 NOT TESTED`; `VAI-CONCUR-004` remains explicitly deferred. The immediate development package is additive legacy `memory.json` migration.

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
10. **Release recovery preserves immutable identity.** Recovery may restore metadata/assets only from an existing verified release tag commit and may never create, delete or move that tag.

---

# Current execution track

```text
0.1.x reliability/security baseline                    COMPLETE
Memory 2.0 foundation                                  SUBSTANTIALLY IMPLEMENTED
MCA selective synchronization S1-S8                    COMPLETE
Operator Lore S9-S10c                                  COMPLETE
M11 Phases A-E                                          MERGED / AUTOMATION COMPLETE
0.1.26 exact release gates                              COMPLETE
0.1.26 installed canaries                              5 PASS / 0 FAIL / 1 NOT TESTED
0.1.26 publication                                      COMPLETE
release-recovery automation                             COMPLETE
legacy memory.json migration                            NEXT
```

Immediate sequence:

```text
inventory existing legacy memory.json data
→ define migration schema/checkpoint/version contract
→ deterministic dry-run parser/report
→ RED migration safety/idempotency tests
→ bounded additive import
→ backup + rollback verification
→ same-world restart acceptance
→ retain legacy reads
→ cut over only after explicit acceptance evidence
```

---

# Completed milestone — M11 Phase E

Merged through PR #114 at:

```text
c51201d7a37b9d09c9a8cb490d1c56f3f6921c1f
```

Phase E moved deterministic release risks into repeatable CI:

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

The six manual scenarios are not missing deterministic tests; they require an installed environment, graphical rendering, physical microphone/UDP or subjective audible/spatial judgment.

---

# Completed release milestone — 0.1.26+1.21.1

## Immutable identity

```text
tag:                         0.1.26+1.21.1
release commit:              40ce7cb77e9b9178fd96fd91025cee22ba686dc0
release PR:                  #115
JAR SHA-256:                 5728f0f1a57b4c268df9b73603539f09ca30945a2ba251e72a5169ab45ae0a53
dependency manifest SHA-256: b16a7b842776d44ed21cad1b56cee63aadc782ada457c108c5107c483aab5816
```

The final GitHub Release contains:

```text
villaigence-fabric-0.1.26+1.21.1.jar
villaigence-fabric-0.1.26+1.21.1.jar.sha256
villaigence-dependencies-0.1.26+1.21.1.txt
```

## Automated release evidence

The release boundary passed:

- version/tag and Minecraft-version contract;
- repository security policy;
- common and deterministic provider tests;
- required Fabric GameTests;
- Fabric and NeoForge builds;
- exact production startup, stop/save and restart;
- identity/inventory lifecycle evidence;
- six-case destructive persistence recovery;
- production Simple Voice Chat transport evidence;
- distributable package smoke;
- byte identity between production-accepted and packaged JAR.

## Installed canary result

The exact candidate bytes installed on the operator server/client produced:

```text
VAI-BOOT-002    PASS
VAI-NAV-001     PASS
VAI-GAME-001    PASS
VAI-GAME-003    PASS
VAI-AI-006      PASS after Chat model switched to google/gemini-2.5-flash-lite
VAI-CONCUR-004  NOT TESTED — no second graphical client available

Total: 5 PASS / 0 FAIL / 1 NOT TESTED
```

`VAI-CONCUR-004` is a recorded release limitation, not a hidden PASS. Automated authenticated two-session acceptance covers server authority, revision, response ownership, retained draft and reviewed retry, but not graphical two-client presentation.

Canonical evidence:

```text
docs/livingworld/VALIDATION_0.1.26_INSTALLED_CANARIES.md
docs/livingworld/VALIDATION_0.1.26_RELEASE_COMPLETE.md
```

## Publication outage and recovery

A GitHub Actions service outage interrupted the original post-merge publication after the tag/Release record existed but before assets were complete. PR #116 introduced fail-closed recovery without moving the tag.

```text
recovery PR:             #116
recovery control commit: ae551b81d221ce88ceebfce96b1038afa718da50
recovery workflow:       VillAIgence Release Recovery #4
recovery run id:         31154864224
result:                  PASS
```

Recovery #4 rebuilt and tested the immutable release commit, restored the existing Release, downloaded the published assets again and compared them byte-for-byte. The recovered JAR reproduced the installed candidate SHA exactly.

The published JAR is byte-identical to the exact installed candidate that already passed startup/restart and grave/restart/resurrection acceptance. No separate temporal claim is made that another operator restart occurred only after assets appeared on GitHub.

---

# Versioned product roadmap

## 0.1.x — Reliability, security and compatibility

Status: **0.1.26 release boundary complete**.

Implemented and release-verified:

- provider parsing and transport hardening;
- bounded retries/deadlines and exactly-once effects;
- endpoint/credential/redirect policy;
- deterministic text, voice and Operator Lore acceptance;
- world-local persistence recovery;
- selective MCA gameplay corrections;
- exact production startup/restart and package identity;
- risk-based GameTests and bounded soak;
- immutable release artifact verification;
- fail-closed recovery of incomplete Release assets.

Deferred installed boundary:

- `VAI-CONCUR-004` real two-graphical-client Operator Lore conflict presentation.

This deferred canary should be executed when two graphical clients are available, but it does not block beginning the next additive development package because the limitation is explicit and server-side concurrency semantics are already automated.

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

### NEXT — additive legacy `memory.json` migration

The first 0.2 package is a migration foundation, not a destructive cutover.

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

Recommended implementation slices:

1. **Inventory and contract**
   - enumerate real legacy `memory.json` shapes and ownership semantics;
   - define supported/unsupported records;
   - define schema/checkpoint/version and report format;
   - define deterministic event-ID derivation.

2. **Dry-run parser/report**
   - no canonical mutation;
   - bounded record count/bytes;
   - deterministic classification and stable report ordering;
   - explicit skipped/rejected reasons.

3. **RED safety matrix**
   - duplicate source rows;
   - rerun after successful import;
   - partial destination state;
   - malformed/oversized legacy data;
   - wrong/unknown NPC ownership;
   - interrupted write/backup failure;
   - dialogue cannot become FACT.

4. **Bounded additive import**
   - backup before mutation;
   - atomic destination writes;
   - deterministic IDs and idempotency;
   - preserve existing Memory 2.0 records;
   - preserve NPC isolation.

5. **Rollback and restart acceptance**
   - byte-preserving backup evidence;
   - rollback verification;
   - same-world restart;
   - second run produces no duplicate semantic/episodic effects.

6. **Cutover decision later**
   - keep legacy reads during migration rollout;
   - do not delete or reinterpret legacy source data;
   - remove legacy dependency only after explicit installed evidence and rollback plan.

Remaining Memory 2.0 capabilities after migration foundation:

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

Release limitations must remain explicit. Deferred manual evidence must never be silently promoted to PASS by automated logical substitutes.
