# VillAIgence Roadmap

> **Canonical product roadmap.** Read `docs/PROJECT_STATE.md` first for exact implementation and validation state.
>
> Last reconciled: **2026-08-07**. M11 Phases A–E remain complete at the automation layer. `0.1.26+1.21.1` remains the latest published release with verified immutable assets and installed acceptance of `5 PASS / 0 FAIL / 1 NOT TESTED`; `VAI-CONCUR-004` remains explicitly deferred. The current unreleased 0.2 package is the Memory 2.0 persistent-dialogue clean cutover in PR #119. The previously planned legacy `memory.json` migration is cancelled by design because the supported deployment is a clean-reset pre-1.0 test environment.

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
10. **Compatibility work must have a supported-data reason.** Pre-1.0 test data is not automatically entitled to a migration layer when a clean reset is an accepted deployment boundary.
11. **Release recovery preserves immutable identity.** Recovery may restore metadata/assets only from an existing verified release tag commit and may never create, delete or move that tag.
12. **Release recovery is version-aware.** The recovery controller must validate the matrix defined by the immutable target release, not impose the current branch's persistence-store count on historical tags.

---

# Current execution track

```text
0.1.x reliability/security baseline                    COMPLETE
Memory 2.0 foundation                                  SUBSTANTIALLY IMPLEMENTED
Memory 2.0 persistent-dialogue clean cutover            COMPLETE AT AUTOMATION LAYER / PR #119
legacy memory.json migration                            CANCELLED BY DESIGN
MCA selective synchronization S1-S8                    COMPLETE
Operator Lore S9-S10c                                  COMPLETE
M11 Phases A-E                                          MERGED / AUTOMATION COMPLETE
0.1.26 exact release gates                              COMPLETE
0.1.26 installed canaries                              5 PASS / 0 FAIL / 1 NOT TESTED
0.1.26 publication                                      COMPLETE
release-recovery automation                             COMPLETE / VERSION-AWARE
clean-world installed cutover acceptance                NEXT RELEASE-CANDIDATE BOUNDARY
controlled BELIEF producers                             NEXT PRODUCT SLICE
```

Immediate sequence:

```text
finish exact-head PR #119 gates and independent review
→ merge clean Memory 2.0 cutover without publishing
→ build the next exact candidate when release work is authorized
→ install on a clean test-world/LivingWorld state
→ verify text + voice dialogue recall and same-world restart
→ retain VAI-CONCUR-004 as deferred until two graphical clients exist
→ implement controlled BELIEF producers
→ implement trustworthy causal relationship reasons
→ extend long-horizon recall
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

The 0.1.26 release boundary passed:

- version/tag and Minecraft-version contract;
- repository security policy;
- common and deterministic provider tests;
- required Fabric GameTests;
- Fabric and NeoForge builds;
- exact production startup, stop/save and restart;
- identity/inventory lifecycle evidence;
- the historical six-case destructive persistence recovery matrix present at that immutable commit;
- production Simple Voice Chat transport evidence;
- distributable package smoke;
- byte identity between production-accepted and packaged JAR.

## Installed canary result

The exact 0.1.26 candidate bytes installed on the operator server/client produced:

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

Current recovery control remains compatible with historical releases by validating that the target release produced a non-empty all-PASS matrix while the target commit's own tests define its exact store/case coverage.

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

This deferred canary should be executed when two graphical clients are available, but it does not block current development because the limitation is explicit and server-side concurrency semantics are already automated.

## 0.2 — Memory 2.0

Goal: move from raw chat-history storage to bounded, layered, provenance-aware memory.

```text
Working memory       recent turn-local context
Episodic memory      meaningful events and dialogue
Semantic memory      sourced FACT/BELIEF knowledge
Relationship memory causal social history
```

### Implemented foundation

- episodic events and explicit provenance;
- text/voice DIALOGUE parity;
- deterministic retrieval;
- bounded Working Memory;
- semantic FACT/BELIEF model;
- controlled server-observed FACT ingestion;
- consolidation/source union;
- deterministic forgetting;
- restart-safe world-local stores.

### Current package — clean persistent-dialogue cutover

The old migration plan has been replaced by a direct clean cutover because the supported deployment is an operator-only pre-1.0 test server that can be rebuilt without preserving experimental conversation history.

Target storage flow:

```text
successful text/voice turn
→ one post-success Memory2DialogueLifecycle write
→ structured DIALOGUE MemoryEvent
→ memory2.json

next turn
→ exact NPC/player DIALOGUE retrieval
→ filter before limit
→ chronological user/assistant reconstruction
→ Working Memory hard bounds
→ prompt
```

Implemented cutover properties:

- `memory2.json` is the only persistent dialogue-memory source;
- new DIALOGUE events carry structured `DialogueExchange(playerMessage, npcReply)` data;
- human-readable summaries remain episodic/diagnostic only and are never parsed back into prompt roles;
- exact NPC and player isolation;
- non-dialogue events cannot starve dialogue retrieval before the result limit;
- old summary-only DIALOGUE events are ignored rather than guessed;
- deterministic IDs keep replay idempotent;
- legacy `ConversationMemoryStore`/`MemoryMessage` code and dedicated tests are removed;
- the remaining `PersistentChatMemory` class is only a no-storage inherited-call-surface adapter and never opens `memory.json` or performs the persistent write;
- production corruption/recovery moves to five current auxiliary stores: Memory 2.0, semantic memory, relationships, voices and Operator Lore;
- nightly and release gates use the same five-store current matrix;
- immutable historical release recovery remains target-version-aware.

Explicit non-goals:

```text
NO legacy memory.json importer
NO migration checkpoint ledger
NO dual persistent reads
NO summary parsing
NO destructive legacy conversion
NO claim that automated exact-JAR evidence equals installed clean-world acceptance
```

Installed exit boundary for this package:

1. build an exact candidate after merge/release work is authorized;
2. deploy it on a clean test-world/LivingWorld state;
3. verify first text conversation is recalled on the next turn;
4. verify voice produces the same DIALOGUE model;
5. restart the same world and verify recall remains correct;
6. verify NPC/player isolation;
7. record installed evidence separately.

### NEXT — controlled BELIEF producers

Goal: allow NPCs to learn claims without confusing them with server-observed facts.

Required contract:

```text
PLAYER_TOLD → BELIEF
NPC_TOLD    → BELIEF
INFERRED    → BELIEF
SYSTEM_OBSERVED only → FACT
```

Recommended slices:

1. **Admission contract**
   - define which conversational claims are eligible for semantic belief extraction;
   - require explicit provenance and source IDs;
   - keep extraction bounded and inspectable;
   - never let confidence alone upgrade a claim to FACT.

2. **Deterministic consolidation**
   - identical/similar claims merge sources rather than multiplying blindly;
   - conflicting claims remain representable;
   - source union and confidence rules remain deterministic.

3. **Retrieval boundary**
   - current observed world facts outrank conflicting beliefs;
   - relevant beliefs can affect dialogue without becoming authoritative actions.

4. **Failure/replay safety**
   - provider retry does not duplicate semantic entries;
   - failed/empty dialogue does not create beliefs;
   - malformed extraction fails soft.

### NEXT — causal relationship memory

Goal: relationship history should record a trustworthy reason when the server actually has one.

Required properties:

- distinguish numeric relationship transition from causal explanation;
- accept only bounded reasons tied to a validated server event or controlled conversational source;
- do not invent authoritative reasons from free-form LLM text;
- retain exact before/after relationship state and source event IDs;
- make reason history queryable for future dialogue and personality systems.

### Later 0.2 work

```text
long-horizon / multi-day recall evidence
→ NPC-to-NPC knowledge transfer
→ rumor propagation with provenance and uncertainty
→ bounded distortion / contradiction handling
→ scaling evidence for larger populations
```

0.2 exit criterion: persistent NPC memory is layered, bounded, provenance-aware, restart-safe, supports controlled non-authoritative knowledge transfer, and no longer depends on the experimental raw conversation store.

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
