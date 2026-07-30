# VillAIgence Project State

> **Canonical current-state handoff.** Read this file before `docs/ROADMAP.md` when resuming work.
>
> Last major state update: **2026-07-30**, after live validation of `0.1.12+1.21.1`.
>
> Reconcile this state with newer PRs, tags/releases, CI and live-server evidence before starting development.

## Executive snapshot

VillAIgence is a Minecraft 1.21.1 MCA-derived mod evolving from AI-assisted villager dialogue into a persistent living-society simulation layer.

```text
primary branch: 1.21.1
Java: 21
primary package: Fabric
NeoForge: compile compatibility required

latest implementation merge:
PR #49 — controlled Semantic Memory ingestion
merge: c6a7a17aa5bd7806667ff3b8b502b852640e606c
exact verified feature head: 7f9916e510a2ca70245d93c5b308ee31758fed0f

feature CI:
VillAIgence CI #721 / 30548196801 — SUCCESS
Java Pull Request CI #289 / 30548198746 — SUCCESS

latest canonical state merge before this validation update:
PR #50 — 746fa75ab4b5f4bee385efa0c8ae51009c1aec58

latest live-validated release checkpoint:
0.1.12+1.21.1 — PASS
validation date: 2026-07-30
release/tag commit: 746fa75ab4b5f4bee385efa0c8ae51009c1aec58
```

Canonical live evidence:

```text
docs/livingworld/VALIDATION_0.1.12.md
```

Previous live checkpoint:

```text
0.1.11+1.21.1
docs/livingworld/VALIDATION_0.1.11.md
```

`0.1.12+1.21.1` supersedes `0.1.11+1.21.1` as the latest confirmed live-server checkpoint.

## Release metadata status

The earlier `0.1.12+1.21.1` workflow attempt `30540119567` failed before Gradle because `0.1.11` and `0.1.12` temporarily pointed at the same commit.

That ambiguity is now resolved:

```text
0.1.11+1.21.1 → 60236524e37b60c639b93405f809ade883be253f
0.1.12+1.21.1 → 746fa75ab4b5f4bee385efa0c8ae51009c1aec58
1.21.1          → 746fa75ab4b5f4bee385efa0c8ae51009c1aec58
```

The `0.1.12` tag is distinct from `0.1.11`, points at the tested current branch head and is now a valid live-server checkpoint.

---

# Identity and compatibility

```text
public name: VillAIgence
short name: VAI
tagline: Giving villagers a mind of their own.
repository: True-Ruslan/villAIgence
Minecraft: 1.21.1
Java: 21
```

Compatibility-sensitive identifiers intentionally remain:

```text
mod id: mca
Java package root: net.conczin.mca
config: config/livingworld.json
world data root: <world>/livingworld/
internal engine/data naming: LivingWorld / livingworld
```

Do not rename these without a dedicated migration design.

---

# Architecture laws

1. **LLM is never authoritative.** Minecraft/server-owned state is truth.
2. Mutable state used by async AI must be captured into immutable, bounded context before provider work.
3. LLM may propose dialogue, actions and relationship deltas; server policy validates, revalidates and executes mutations.
4. Provider/model changes must not redefine persistent NPC identity.
5. External AI and auxiliary persistence failures fail soft whenever safe.
6. API credentials remain server-side.
7. Persistent formats remain explicit, inspectable and backed up with the world.
8. Retry/replay paths must not duplicate persistent or gameplay side effects.
9. Claims and beliefs remain non-authoritative unless server-owned evidence makes them factual.
10. Confidence never upgrades BELIEF into FACT.
11. Autonomous AI must eventually be event-driven and budgeted rather than “LLM every tick.”

Canonical authority flow:

```text
Minecraft/server state
→ immutable/bounded context
→ provider/LLM proposal
→ server validation/revalidation
→ server-owned mutation
→ persisted authoritative evidence
→ bounded Memory 2.0 ingestion
```

Semantic truth boundary:

```text
FACT   → SYSTEM_OBSERVED only
BELIEF → PLAYER_TOLD / NPC_TOLD / INFERRED only
```

Current server-observed `worldFacts` win when recalled memory conflicts with current state.

---

# Persistent world-local data

```text
<world>/livingworld/memory.json          bounded rolling dialogue history
<world>/livingworld/memory2.json         episodic MemoryEvent store
<world>/livingworld/semantic-memory.json typed Semantic FACT/BELIEF store
<world>/livingworld/events.json          server-owned world events
<world>/livingworld/relationships.json   player↔NPC relationship state
<world>/livingworld/voices.json          persistent NPC voice identity
```

All files belong with world backup/restore procedures.

`memory.json` remains active and has not been migrated or removed. Memory 2.0 remains additive while the layered architecture is stabilized.

---

# Implemented reliability foundation

Implemented and retained:

- OpenAI-compatible/OpenRouter server-side provider configuration;
- bounded connect/read timeouts and controlled failures;
- `content:null`, empty response and bounded retry handling;
- non-blocking Chat/STT/TTS admission and provider cooldown;
- Simple Voice Chat → PCM → STT → targeted NPC → AI;
- optional spatial TTS, PCM/WAV support and resampling;
- persistent per-NPC voice identity;
- safe whitelisted actions with server validation/revalidation;
- authoritative world events and relationship persistence;
- immutable context snapshots;
- structured-response sanitation;
- fail-open recovery for malformed auxiliary JSON;
- diagnostics without secrets, prompts, transcripts or hidden reasoning.

Reliability policy:

> Fix concrete regressions reproduced by CI or live use. Do not mix speculative reliability refactors into Memory 2.0 work.

---

# Memory 2.0 — implemented state

## Episodic Memory

Implemented through PRs #31, #33, #35, #37, #39, #41 and #43:

- immutable NPC-owned `MemoryEvent`;
- types `DIALOGUE`, `OBSERVATION`, `ACTION`, `RELATIONSHIP_CHANGE`;
- provenance `SYSTEM_OBSERVED`, `PLAYER_TOLD`, `NPC_TOLD`, `INFERRED`;
- importance, emotional weight and confidence metadata;
- bounded per-NPC `memory2.json` persistence;
- UUID idempotency and deterministic ordering;
- atomic writes and fail-open recovery;
- deterministic episodic ranking;
- authoritative ACTION ingestion;
- server-observed RELATIONSHIP_CHANGE ingestion;
- ordinary text and snapshot/voice DIALOGUE parity;
- shared post-success dialogue lifecycle.

Episodic ranking:

```text
relevance  40%
importance 25%
recency    20%
confidence 15%

candidateLimit = 32
maxResults = 6
recencyHorizonTicks = 168000
```

## Working Memory

PR #46 introduced turn-local bounded orchestration:

```text
recent dialogue
+ selected episodic context
+ selected semantic context
→ WorkingMemoryContext
```

Hard prompt bounds:

```text
recent persistent dialogue messages = 12
max dialogue message = 1200 Unicode code points
episodic context entries = 6
semantic context entries = 6
```

Working Memory itself is not persisted.

The `0.1.11` live test confirmed the intended durable/prompt distinction:

```text
memory.json total messages: 86
NPC A durable rolling history: 16
NPC A messages selected for prompt: 12
conversation continuity after bound: PASS
```

## Semantic Memory foundation

PR #46 introduced:

```text
SemanticMemoryEntry
SemanticMemoryStore
SemanticMemoryQuery
SemanticMemoryRetriever
SemanticMemoryContextFormatter
SemanticMemoryContextProvider
```

Store guarantees:

- per-NPC isolation;
- bounded retention;
- UUID idempotency;
- deterministic ordering;
- atomic persistence;
- fail-open malformed-file recovery.

Semantic ranking:

```text
related-entity relevance 40%
importance               30%
confidence               20%
recency                   10%

candidateLimit = 32
maxResults = 6
recencyHorizonTicks = 168000
```

Prompt layers remain physically separate:

```text
worldFacts               authoritative current state
memoryContext            episodic memory
semanticMemoryContext    semantic FACT/BELIEF memory
```

## PR #49 — controlled Semantic Memory ingestion

Merge and exact verified head:

```text
merge: c6a7a17aa5bd7806667ff3b8b502b852640e606c
head:  7f9916e510a2ca70245d93c5b308ee31758fed0f
```

TDD and CI evidence:

```text
RED:
VillAIgence CI #701 / 30547268997 — expected FAILURE
Reason: controlled-ingestion production API did not yet exist

GREEN exact final head:
VillAIgence CI #721 / 30548196801 — SUCCESS
Java Pull Request CI #289 / 30548198746 — SUCCESS
```

Implemented components:

```text
SemanticBeliefSource
SemanticMemoryIngestionAdapter
ControlledSemanticMemoryIngestor
```

Automatic FACT eligibility:

```text
source.provenance == SYSTEM_OBSERVED
AND source.type in {ACTION, OBSERVATION, RELATIONSHIP_CHANGE}
```

Production flows:

```text
successful safe NPC action
→ SYSTEM_OBSERVED WorldEvent
→ ACTION MemoryEvent
→ memory2.json
→ FACT SemanticMemoryEntry
→ semantic-memory.json
```

```text
persisted relationship transition
→ SYSTEM_OBSERVED RELATIONSHIP_CHANGE MemoryEvent
→ memory2.json
→ FACT SemanticMemoryEntry
→ semantic-memory.json
```

FACT properties:

- deterministic UUID derived from owner NPC and source event UUID;
- `SYSTEM_OBSERVED` provenance;
- non-empty `sourceEventIds`;
- normalized statement, maximum 240 Unicode code points;
- replay-safe store idempotency.

Explicit BELIEF API:

- accepts only `PLAYER_TOLD`, `NPC_TOLD` or `INFERRED`;
- requires non-empty source event IDs;
- preserves provenance;
- uses deterministic IDs;
- is not automatically called by ordinary dialogue.

Critical exclusion:

```text
DIALOGUE MemoryEvent
→ episodic memory only
→ no automatic SemanticMemoryEntry
```

No provider call, LLM truth classification, embeddings or vector database was introduced.

Semantic ingestion remains part of existing `memory2Enabled` and reuses `memory2MaxEventsPerNpc`; no new config fields or format version were added.

---

# Live validation status

## 0.1.12+1.21.1 — PASS

Controlled Semantic Memory ingestion was validated on a real server.

### Authoritative events and semantic production

```text
successful NPC actions: 2                                PASS
ACTION MemoryEvents: 2                                   PASS
relationship transition: trust +1, affinity +1           PASS
RELATIONSHIP_CHANGE MemoryEvents: 1                       PASS
semantic FACT entries: 3                                 PASS
```

Every FACT had:

- `kind = FACT`;
- `provenance = SYSTEM_OBSERVED`;
- correct `ownerNpcId`;
- the matching episodic UUID in `sourceEventIds`;
- the expected deterministic semantic UUID.

### Idempotency and dialogue boundary

```text
semantic UUID duplicates: 0                              PASS
retry-created semantic duplicates: 0                     PASS
ordinary DIALOGUE interactions: 9                        OBSERVED
DIALOGUE-derived semantic entries: 0                     PASS
```

### Restart persistence

All five tested files were byte-identical before and after restart:

```text
memory.json                                               PASS
memory2.json                                              PASS
semantic-memory.json                                      PASS
relationships.json                                        PASS
voices.json                                               PASS
```

### Chat, voice and operations

```text
Chat                                                      SUCCESS
STT                                                       SUCCESS / 1059 ms
TTS                                                       SUCCESS / 1663 ms
Simple Voice Chat / Opus                                  PASS
monitor                                                   PASS
UDP 24454 / 25565                                         PASS
VillAIgence, memory and persistence errors                none
```

Canonical evidence:

```text
docs/livingworld/VALIDATION_0.1.12.md
```

## 0.1.11+1.21.1 — previous PASS checkpoint

Validated Working Memory bounds, NPC isolation, text/voice parity, retry recovery and byte-stable persistence before automatic semantic producers existed.

Canonical evidence:

```text
docs/livingworld/VALIDATION_0.1.11.md
```

---

# Roadmap status

## 0.1.x Reliability

Foundation is stable and live-validated through `0.1.12+1.21.1`. Continue only for concrete defects or explicit soak, backup/restore or provider-failure goals.

## 0.2 Memory 2.0 — active and substantially advanced

Implemented and live-proven:

```text
Episodic MemoryEvent model and persistence
+ deterministic episodic retrieval
+ ACTION / DIALOGUE / RELATIONSHIP_CHANGE ingestion
+ text / voice dialogue parity
+ Working Memory bounds
+ Semantic FACT/BELIEF model and persistence
+ deterministic semantic retrieval
+ shared layered prompt integration
+ controlled ACTION / RELATIONSHIP_CHANGE → FACT ingestion
+ deterministic semantic UUID and retry idempotency
+ DIALOGUE exclusion from automatic semantics
+ explicit sourced BELIEF API foundation
```

Still not implemented or not proven:

- automatic controlled BELIEF producers;
- deterministic logical duplicate/consolidation policy across distinct source events;
- forgetting/decay;
- migration from legacy `memory.json`;
- NPC-to-NPC knowledge and rumor propagation;
- trustworthy causal relationship reasons;
- long-horizon recall after days without full raw dialogue;
- large multiplayer and multi-day soak validation.

## Next sequence

```text
1. Add deterministic semantic duplicate/consolidation policy
2. Add forgetting/decay with explicit retention rules
3. Design legacy memory.json migration after semantic layers stabilize
4. Run long-horizon Memory 2.0 exit-criterion validation
5. Begin 0.3 Personality + NPC↔NPC social graph
```

Consolidation constraints:

- preserve provenance and all source event IDs;
- never merge FACT and BELIEF into one authoritative entry;
- distinguish replay duplicates from separate corroborating evidence;
- remain deterministic and provider-independent;
- do not require embeddings, vector DB or LLM truth classification.

---

# New-session handoff protocol

Preferred resume prompt:

> **Open `docs/PROJECT_STATE.md`, `docs/CHANGELOG.md`, `docs/ROADMAP.md` and the latest validation evidence in `True-Ruslan/villAIgence`. Check recent PRs, releases/tags and CI, then tell me what is complete, what is live-validated, what changed since the state file, and what should be built next.**

A new session must:

1. read this file;
2. read `docs/CHANGELOG.md`;
3. read `docs/ROADMAP.md`;
4. inspect current `1.21.1` HEAD;
5. inspect recent merged/open PRs;
6. inspect latest release/tag and CI state;
7. reconcile newer live-test evidence;
8. continue from the first unimplemented priority;
9. update canonical state after material progress.

```text
docs/ROADMAP.md       → where the project is going
docs/PROJECT_STATE.md → what exists, what is proven, and what comes next
docs/CHANGELOG.md     → material implementation and validation history
```
