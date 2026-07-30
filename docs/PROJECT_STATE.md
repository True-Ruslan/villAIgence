# VillAIgence Project State

> **Canonical current-state handoff.** Read this file before `docs/ROADMAP.md` when resuming work.
>
> Last major state update: **2026-07-30**, after merged PR #49.
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

exact-head CI:
VillAIgence CI #721 / 30548196801 — SUCCESS
Java Pull Request CI #289 / 30548198746 — SUCCESS

latest live-validated release checkpoint:
0.1.11+1.21.1 — PASS
validation date: 2026-07-30
```

**Status boundary:** PR #49 is merged and automated-CI validated, but its new semantic producer behavior has not yet received a real-server validation checkpoint. `0.1.11+1.21.1` remains the latest live-validated release.

Canonical live evidence:

```text
docs/livingworld/VALIDATION_0.1.11.md
```

Controlled semantic-ingestion architecture:

```text
docs/livingworld/SEMANTIC_INGESTION.md
docs/superpowers/specs/2026-07-30-memory2-controlled-semantic-ingestion-design.md
docs/superpowers/plans/2026-07-30-memory2-controlled-semantic-ingestion.md
```

## Release metadata caveat

The attempted `0.1.12+1.21.1` workflow run `30540119567` failed before Gradle because `0.1.11+1.21.1` and `0.1.12+1.21.1` pointed at the same commit.

Do not describe `0.1.12+1.21.1` as built, published or validated. Correct the ambiguous tag before attempting that release again.

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

The `0.1.11` live test confirmed:

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

Merge:

```text
c6a7a17aa5bd7806667ff3b8b502b852640e606c
```

Exact verified feature head:

```text
7f9916e510a2ca70245d93c5b308ee31758fed0f
```

TDD evidence:

```text
RED:
VillAIgence CI #701 / 30547268997 — expected FAILURE
Reason: controlled-ingestion classes and overloads did not yet exist

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

Production flows now include:

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
- normalized statement;
- maximum 240 Unicode code points;
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

## 0.1.11+1.21.1 — PASS

Validated on a real server:

- NPC A/B UUID and memory isolation;
- Working Memory 16 durable / 12 prompt distinction;
- full episodic history preservation;
- no UUID or logical duplicates;
- three independent voice turns;
- Voice Chat, Opus, STT and TTS;
- one successful OpenRouter retry without fallback;
- byte-identical persistence files across restart;
- server, monitor and required ports healthy.

At that checkpoint `semantic-memory.json` was absent, which was correct because PR #49 had not yet been implemented.

## PR #49 validation boundary

Not yet live-proven:

- automatic ACTION → FACT persistence;
- automatic RELATIONSHIP_CHANGE → FACT persistence;
- source-event linkage in `semantic-memory.json`;
- semantic UUID idempotency on replay/retry;
- dialogue exclusion on a real server;
- semantic persistence across restart.

Do not call PR #49 live-validated until this scenario is completed.

---

# Roadmap status

## 0.1.x Reliability

Foundation is stable and live-validated through `0.1.11+1.21.1`. Continue only for concrete defects or explicit soak/backup/provider-failure goals.

## 0.2 Memory 2.0 — active and substantially advanced

Implemented:

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
+ explicit sourced BELIEF API
```

Still not implemented or not proven:

- real-server validation of PR #49 semantic producers;
- deterministic logical duplicate/consolidation policy across distinct sources;
- forgetting/decay;
- migration from legacy `memory.json`;
- NPC-to-NPC knowledge and rumor propagation;
- trustworthy causal relationship reasons;
- long-horizon recall after days without full raw dialogue;
- large multiplayer and multi-day soak validation.

## Next sequence

```text
1. Live-validate PR #49 semantic ingestion
2. Add deterministic semantic duplicate/consolidation policy
3. Add forgetting/decay with explicit retention rules
4. Design legacy memory.json migration after semantic layers stabilize
5. Run long-horizon Memory 2.0 exit-criterion validation
6. Begin 0.3 Personality + NPC↔NPC social graph
```

No embeddings, vector DB or LLM-driven truth classification should be prerequisites.

---

# Immediate live-test scenario

```text
1. Install a build containing PR #49.
2. Record hashes/counts for memory2.json and semantic-memory.json.
3. Perform a successful safe NPC action.
4. Produce a persisted relationship-state change.
5. Confirm ACTION and RELATIONSHIP_CHANGE exist in memory2.json.
6. Confirm matching FACT entries exist in semantic-memory.json.
7. Verify each FACT:
   - kind = FACT
   - provenance = SYSTEM_OBSERVED
   - ownerNpcId is correct
   - sourceEventIds contains the episodic event UUID
8. Replay/retry equivalent operations and confirm no duplicate semantic UUIDs.
9. Perform ordinary text and voice dialogue only; confirm no semantic entry is created solely from DIALOGUE.
10. Restart server.
11. Confirm files persist byte-for-byte and NPC ownership remains isolated.
12. Confirm text, voice, STT, TTS, monitor and ports remain healthy.
```

After a successful test, create a new validation document and promote the tested version to the latest live-server checkpoint.

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
docs/ROADMAP.md      → where the project is going
docs/PROJECT_STATE.md → what exists, what is proven, and what comes next
docs/CHANGELOG.md     → material implementation and validation history
```
