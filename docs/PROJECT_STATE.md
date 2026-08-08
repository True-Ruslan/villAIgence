# VillAIgence Project State

> **Canonical current-state handoff.** Read this file before `docs/ROADMAP.md` when resuming work. Read root `CHANGELOG.md` for product/release history.
>
> Last reconciled: **2026-08-08**, after controlled Semantic Memory BELIEF admission merged through PR #123.
>
> Always distinguish source/unit evidence, common integration, server GameTests, production-candidate evidence, exact-release evidence, and installed operator server/client evidence.

## Executive snapshot

VillAIgence is a Minecraft 1.21.1 MCA-derived mod evolving from AI-assisted villager dialogue into a persistent living-society simulation layer.

```text
repository:                         True-Ruslan/villAIgence
primary branch:                     1.21.1
Java:                               21
primary distribution:               Fabric
NeoForge:                           compile compatibility required

latest product merge:               PR #123
latest product merge commit:        fd7e9a1099cd73876acce8aaf99705b3763a28c6
latest official release:            0.2.0+1.21.1
latest release commit:              e426f588efefa6aa48a6e536c4a998421bbda241
installed 0.2.0 candidate JAR SHA:   56293f86634b50b2def044429aac6f2cf0d197eb16ac1e60224708f7b3333aee

next product slice:                 bounded inspectable BELIEF candidate extraction
then:                               trustworthy causal relationship memory
```

Current delivery state:

```text
0.1.x reliability/security baseline                    COMPLETE
MCA selective synchronization S1-S8                    COMPLETE
Operator Lore S9-S10c                                  COMPLETE
M11 Phases A-E                                         COMPLETE AT AUTOMATION LAYER
acceptance catalog                                     28 AUTOMATED / 6 MANUAL / 0 PLANNED
release/recovery automation                            COMPLETE / VERSION-AWARE

0.2 Memory 2.0 foundation                              SUBSTANTIALLY IMPLEMENTED
Memory 2.0 persistent-dialogue clean cutover            COMPLETE / RELEASED
legacy memory.json migration                           CANCELLED BY DESIGN
0.2.0 clean-world installed acceptance                 7 PASS / 0 FAIL
controlled BELIEF admission contract                   COMPLETE / PR #123
bounded automatic claim extraction                     NOT IMPLEMENTED
causal relationship reasons                            NOT IMPLEMENTED
```

Installed boundaries that remain explicit:

```text
VAI-M2-INST-005  NOT TESTED / AUTOMATED EVIDENCE ONLY
VAI-CONCUR-004   NOT TESTED / DEFERRED
```

Neither is represented as PASS.

---

# Product and architecture

Target product:

```text
NPC Identity
+ Personality
+ Memory
+ Relationships
+ Knowledge
+ Voice
+ Goals
+ Server-authoritative Actions
+ Settlements
+ Factions
+ Emergent History
= Persistent Living Society
```

## Architecture laws

1. **The LLM is never authority.** Minecraft/server-owned state is truth.
2. Mutable game state used asynchronously is captured into immutable bounded context first.
3. Provider/model changes must not redefine persistent NPC identity, memory, relationships or voice.
4. Provider and auxiliary-persistence failures fail soft whenever safe.
5. Retry/replay paths must not duplicate dialogue, memory, relationship or gameplay effects.
6. **FACT requires `SYSTEM_OBSERVED` server-owned evidence.**
7. **BELIEF remains non-authoritative** and may use only `PLAYER_TOLD`, `NPC_TOLD` or `INFERRED` provenance.
8. Confidence never upgrades BELIEF into FACT.
9. Operator Lore is explicit background context, not an observed current-world fact.
10. Current observed world facts override conflicting lore or recalled beliefs.
11. Clients never own permissions, target identity, file access, revisions or persistence mutations.
12. Compatibility work requires a supported-data reason; experimental pre-1.0 data is not automatically entitled to migration code.
13. Exact release identity must match tag, filename, embedded metadata and manifest.
14. Published artifacts must be byte-identical to the exact artifact accepted by the release gate.
15. Automated logical-client evidence never silently becomes installed multi-client evidence.
16. Unknown, unsafe, protected and persistence-store CI changes fail closed to the complete mandatory matrix.
17. Release recovery may repair metadata/assets only from an existing immutable release tag commit and never moves that tag.
18. Release recovery validates the persistence contract defined by the immutable target release itself.

Canonical AI/state flow:

```text
Minecraft/server state
→ immutable bounded snapshot
→ observed facts + operator lore + episodic/semantic memory
→ provider/LLM proposal
→ server validation/revalidation
→ server-owned mutation
→ persistent authoritative evidence
```

Semantic knowledge flow:

```text
SYSTEM_OBSERVED evidence
→ controlled FACT ingestion
→ FACT

explicit persisted source evidence
→ bounded claim candidate
→ BELIEF admission policy
→ BELIEF
```

A future extractor may propose claim candidates, but it does not decide authoritative truth.

---

# Identity and compatibility

```text
public name:      VillAIgence
short name:       VAI
Minecraft:        1.21.1
Java:             21
internal mod id:  mca
package root:     net.conczin.mca
config:           config/livingworld.json
world data root:  <world>/livingworld/
```

Compatibility-sensitive `mca`, `LivingWorld` and `livingworld` identifiers remain unchanged.

Current world-local stores include:

```text
<world>/livingworld/memory2.json
<world>/livingworld/semantic-memory.json
<world>/livingworld/events.json
<world>/livingworld/relationships.json
<world>/livingworld/voices.json
<world>/livingworld/operator-lore.json
```

Current auxiliary corruption/recovery matrix:

```text
memory2.json
semantic-memory.json
relationships.json
voices.json
operator-lore.json
```

`events.json` is authoritative factual event history and has its own validation path.

The experimental pre-0.2 `<world>/livingworld/memory.json` conversation store is no longer part of current runtime or recovery. No importer, dual reader, checkpoint ledger or destructive migration is planned for the accepted pre-1.0 clean-state rollout boundary.

Historical immutable releases retain their own validation contracts through version-aware release recovery.

---

# Implemented systems

## Provider, parsing and security

Implemented and retained:

- OpenAI-compatible Chat, STT and TTS;
- OpenRouter-compatible Chat;
- endpoint normalization and credential-family binding;
- authenticated redirect blocking;
- remote plaintext rejection except explicit literal-loopback development mode;
- bounded response/error/verification bodies;
- controlled null, empty, malformed and provider-error handling;
- retry without duplicate persistent/gameplay effects;
- bounded deadlines, queueing and PCM limits;
- verified dependencies and pinned Actions;
- deterministic repository security policy;
- diagnostics without secrets, prompts, transcripts or hidden reasoning.

Security findings `SEC-001` through `SEC-009` remain closed.

Only release-critical publication/recovery jobs receive `contents: write`; normal workflows default to read-only repository permissions.

## Voice orchestration and transport

Automated evidence covers:

- one monotonic deadline across queue handoff, STT, Chat retries and optional TTS;
- exactly-once dialogue/relationship effects;
- deterministic mock-provider STT/Chat/TTS;
- real Simple Voice Chat Opus encode/decode;
- loss concealment, duplicate/order rejection and bounded PCM;
- encoder/decoder closure and cancellation;
- constrained-heap repeated transport and production restart soak.

Physical microphone permission, real client UDP routing, audible spatial playback and subjective audio quality remain installed/manual evidence categories.

## Memory 2.0

Implemented foundation:

- immutable NPC-owned episodic events;
- DIALOGUE, OBSERVATION, ACTION and RELATIONSHIP_CHANGE;
- deterministic UUID idempotency and ordering;
- bounded per-NPC persistence and retrieval;
- text/voice DIALOGUE parity;
- bounded Working Memory;
- typed semantic FACT/BELIEF entries;
- controlled server-observed FACT ingestion;
- deterministic semantic consolidation and source union;
- deterministic pressure-based forgetting;
- NPC/player isolation and restart safety.

Truth boundary:

```text
FACT     → SYSTEM_OBSERVED only
BELIEF   → PLAYER_TOLD / NPC_TOLD / INFERRED only
DIALOGUE → episodic by default
```

### Persistent-dialogue clean cutover

Released in `0.2.0+1.21.1`.

```text
usable text/voice AI result
→ ChatAI post-success boundary
→ Memory2DialogueLifecycle
→ structured DIALOGUE MemoryEvent
→ memory2.json
```

Each new DIALOGUE event carries:

- bounded human-readable episodic `summary`;
- structured `DialogueExchange(playerMessage, npcReply)`.

Prompt-history reconstruction uses the structured payload, never parses the summary, filters exact NPC/player DIALOGUE events before limiting, restores chronological user/assistant order and then applies Working Memory bounds.

`ConversationMemoryStore` and `MemoryMessage` are removed. `PersistentChatMemory` remains only as a no-storage compatibility façade for the inherited AI call surface.

### Controlled BELIEF admission — PR #123

Implemented after the 0.2.0 release boundary.

New fail-closed admission rules:

```text
PLAYER_TOLD candidate
→ requires PLAYER_TOLD DIALOGUE source
→ BELIEF

NPC_TOLD candidate
→ requires NPC_TOLD DIALOGUE source
→ BELIEF

INFERRED candidate
→ retains explicit persisted source event
→ BELIEF

SYSTEM_OBSERVED through BELIEF API
→ REJECT
```

The admission API derives owner/time/source event identity from the persisted source event, so callers cannot inject an arbitrary semantic source-event list through this path.

Exact replay remains idempotent. Equivalent corroborating entries use the existing deterministic consolidation/source-union pipeline.

Not yet implemented:

- automatic free-form dialogue-to-belief extraction;
- provider-based truth classification;
- automatic `NPC_TOLD` conversation producer;
- causal relationship explanation model;
- rumors.

This separation is intentional: **candidate extraction is not admission, and admission is not authority**.

## Operator Lore

Implemented:

- schema-versioned world-local store;
- WORLD, PLAYER, VILLAGER and VILLAGE scopes;
- immutable prompt-context capture;
- permission-level-2 server-authoritative API;
- server-resolved identity and scope;
- SHA-256 optimistic revisions;
- bounded validation;
- explicit success/conflict/error statuses;
- multiline editor and close confirmations;
- stale-generation rejection;
- authenticated two-session conflict/retry semantics.

Server-side two-session semantics are automated. Real installed two-graphical-client presentation remains deferred as `VAI-CONCUR-004`.

## Selective MCA corrections

Implemented and retained:

- tombstone item/entity-data integrity;
- pre-serialization inventory ownership transfer;
- UUID-preserving conversion and resurrection replay guard;
- occupied HOME-bed rejection;
- water, ladder, obstacle and door navigation;
- progress watchdog and staggered pathfinding;
- graveyard mourning lifecycle;
- gift semantics;
- fishing/AquaCulture compatibility;
- stable mounted archer control and NPC-owned projectile evidence;
- exactly one filled portable grave;
- exact loose-drop fallback when no tombstone captures the NPC.

---

# Automated acceptance and CI

Canonical acceptance catalog:

```text
34 total scenarios
28 AUTOMATED
6 MANUAL_CANARY
0 PLANNED
```

Permanent GitHub Actions surface after PR #121 and PR #122 is deliberately reduced and fail-closed to eight canonical workflows. The redundant PR Gradle workflow was removed; wrapper validation remains owned by supply-chain verification.

Runtime/product changes are expected to exercise the relevant combination of:

- common/unit/provider tests;
- risk selector and Fabric server GameTests;
- Fabric + NeoForge builds;
- production acceptance contracts;
- exact production startup/save/restart;
- selected/current persistence recovery;
- package smoke;
- repository security policy;
- bounded production soak;
- release dry-run where selected.

PR #123 final exact-head evidence:

```text
head:                                  b2803d6bee3c48128816e604e333b8efdba468b2
Repository security policy #1473:      SUCCESS
VillAIgence Production Soak #56:       SUCCESS
VillAIgence CI #1838:                  SUCCESS
VillAIgence GitHub Release dry-run #390: SUCCESS
publication job:                       SKIPPED
```

TDD RED evidence for the same feature:

```text
tests-only head:        1b8818e34208211c0631a3d852b5fd2e9409743d
Production Soak #52:    expected RED at :common:compileTestJava
reason:                 missing SemanticBeliefAdmissionPolicy and ControlledSemanticBeliefProducer
```

---

# Current official release boundary — 0.2.0+1.21.1

```text
tag:                    0.2.0+1.21.1
release commit:         e426f588efefa6aa48a6e536c4a998421bbda241
installed candidate SHA:56293f86634b50b2def044429aac6f2cf0d197eb16ac1e60224708f7b3333aee
```

Installed clean-world Memory 2.0 result:

```text
VAI-M2-INST-001  PASS
VAI-M2-INST-002  PASS
VAI-M2-INST-003  PASS
VAI-M2-INST-004  PASS
VAI-M2-INST-006  PASS
VAI-M2-INST-007  PASS
VAI-M2-INST-008  PASS

Required total: 7 PASS / 0 FAIL
VAI-M2-INST-005: NOT TESTED / AUTOMATED EVIDENCE ONLY
VAI-CONCUR-004:  NOT TESTED / DEFERRED
```

The physical voice seed `silver-fox-482` was accepted by STT as `SilverFox482`; the accepted transcript persisted and survived restart. This is a non-blocking STT normalization observation, not a Memory 2.0 persistence failure.

Canonical installed evidence:

```text
docs/livingworld/VALIDATION_0.2.0_CLEAN_WORLD_INSTALLED.md
```

Historical `0.1.26+1.21.1` remains immutable and is documented in root `CHANGELOG.md` plus its version-specific validation records.

---

# Changelog governance

Root `CHANGELOG.md` is the canonical product/release changelog.

Its policy requires `[Unreleased]` to be updated in the same PR for notable changes to:

```text
runtime behavior
persistent data
public configuration
release semantics
security guarantees
permanent CI guarantees
```

`docs/CHANGELOG.md` remains the older detailed engineering-history ledger and is retained for historical evidence. New product/release history belongs in root `CHANGELOG.md`.

Release sections must distinguish automated, candidate, exact-release and installed/manual evidence. Deferred or failed acceptance must remain explicit.

---

# Known gaps and technical debt

1. `VAI-CONCUR-004` real two-graphical-client Operator Lore conflict presentation remains deferred.
2. `VAI-M2-INST-005` real second-player installed isolation remains untested; automated exact player isolation exists.
3. Automatic bounded claim extraction feeding BELIEF admission is not implemented.
4. Trustworthy causal relationship-change reasons are not implemented.
5. NPC-to-NPC knowledge transfer and rumor propagation remain future work.
6. Long-horizon/multi-day and larger-server simulation soak remain future scaling evidence.
7. `PersistentChatMemory` is a no-storage compatibility façade and may be cleaned up when the inherited AI call surface is refactored.
8. Historical config fields for the removed raw conversation store remain deserializable compatibility baggage.
9. Historical Javadoc/deprecation warnings remain non-blocking.

---

# Next optimal delivery step

The next product slice is **bounded, inspectable BELIEF candidate extraction**, built on top of the admission contract from PR #123.

Recommended progression:

```text
candidate-extraction contract and schema
→ RED tests for successful, empty, malformed and retry cases
→ minimal deterministic/provider-independent admission adapter
→ bounded optional extractor integration
→ fail-soft malformed/provider behavior
→ exactly-once semantic persistence under retry/replay
→ retrieval precedence tests: current observed FACT > conflicting BELIEF
→ trustworthy causal relationship-change reason model
→ long-horizon recall scenarios
→ NPC-to-NPC knowledge transfer
→ provenance-aware rumors with uncertainty/distortion
```

Constraints:

- extraction must never directly create FACT;
- confidence must never promote BELIEF to FACT;
- provider failure must not create semantic entries;
- failed/empty dialogue must not create semantic entries;
- semantic retry/replay must not duplicate entries;
- current observed server facts remain authoritative over conflicting beliefs;
- do not restore legacy `memory.json` migration unless a new supported-user requirement justifies a separate compatibility project.

---

# Session handoff protocol

For a new development session:

1. read `docs/PROJECT_STATE.md`;
2. read `docs/ROADMAP.md`;
3. read root `CHANGELOG.md`;
4. inspect current `1.21.1` HEAD;
5. inspect open/recent PRs, tags/releases and current CI;
6. reconcile live GitHub state against these documents before changing code;
7. use TDD for runtime behavior: specification → RED → minimal GREEN → focused regression → complete selected gates;
8. update root `CHANGELOG.md` and canonical state docs whenever the delivery boundary changes.

Do not infer a PASS from stale documentation. GitHub state and exact evidence must be checked each session.
