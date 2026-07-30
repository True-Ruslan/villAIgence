# VillAIgence Project State

> **Canonical current-state handoff.** Read this file before `docs/ROADMAP.md` when resuming work.
>
> Last major state update: **2026-07-30**, after merged PR #53.
>
> Reconcile this state with newer PRs, tags/releases, CI and live-server evidence before starting development.

## Executive snapshot

VillAIgence is a Minecraft 1.21.1 MCA-derived mod evolving from AI-assisted villager dialogue into a persistent living-society simulation layer.

```text
primary branch: 1.21.1
Java: 21
primary package: Fabric
NeoForge: compile compatibility required

latest implementation:
PR #53 — deterministic Semantic Memory consolidation
merge: f85879d254f37d7f860380362b296e047bbbb781
exact verified feature head: 19c3d3e840431cc2b1b34e1841e2075f56e99f71

exact-head CI:
VillAIgence CI #746 / 30561015885 — SUCCESS
Java Pull Request CI #300 / 30561015985 — SUCCESS

latest live-validated release checkpoint:
0.1.12+1.21.1 — PASS
validation date: 2026-07-30
tested release commit: 746fa75ab4b5f4bee385efa0c8ae51009c1aec58
```

**Status boundary:** semantic consolidation is merged and automated-CI validated, but not yet validated on a real server. `0.1.12+1.21.1` remains the latest live-server checkpoint.

Canonical evidence:

```text
docs/livingworld/VALIDATION_0.1.12.md
docs/livingworld/SEMANTIC_CONSOLIDATION.md
docs/superpowers/specs/2026-07-30-memory2-semantic-consolidation-design.md
```

## Release metadata status

The old ambiguous-tag condition is resolved:

```text
0.1.11+1.21.1 → 60236524e37b60c639b93405f809ade883be253f
0.1.12+1.21.1 → 746fa75ab4b5f4bee385efa0c8ae51009c1aec58
```

`0.1.12` points at the exact implementation commit tested on the server. The branch advanced afterward through documentation and PR #53 development; that does not change the tested release payload.

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

Compatibility-sensitive identifiers remain:

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
11. Consolidation must preserve provenance and all independent source evidence.
12. Autonomous AI must eventually be event-driven and budgeted rather than “LLM every tick.”

Canonical authority flow:

```text
Minecraft/server state
→ immutable/bounded context
→ provider/LLM proposal
→ server validation/revalidation
→ server-owned mutation
→ persisted authoritative evidence
→ bounded Memory 2.0 ingestion
→ deterministic consolidation
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

`memory.json` remains active. Memory 2.0 is still additive; legacy migration has not started.

---

# Reliability foundation

Implemented and retained:

- OpenAI-compatible/OpenRouter server-side configuration;
- bounded timeouts and controlled provider failures;
- safe `content:null`, empty response and retry handling;
- Chat/STT/TTS admission limits and provider cooldown;
- Simple Voice Chat → PCM → STT → targeted NPC → AI;
- optional spatial TTS, PCM/WAV compatibility and resampling;
- persistent per-NPC voice identity;
- safe whitelisted actions with server validation/revalidation;
- authoritative world events and relationship persistence;
- immutable context snapshots;
- structured-response sanitation;
- fail-open malformed auxiliary JSON recovery;
- diagnostics without secrets, prompts, transcripts or hidden reasoning.

Reliability policy:

> Fix concrete regressions reproduced by CI or live use. Do not mix speculative provider or voice refactors into Memory 2.0 work.

---

# Memory 2.0 — implemented state

## Episodic Memory

Implemented through PRs #31, #33, #35, #37, #39, #41 and #43:

- immutable NPC-owned `MemoryEvent`;
- `DIALOGUE`, `OBSERVATION`, `ACTION`, `RELATIONSHIP_CHANGE`;
- explicit provenance;
- bounded per-NPC persistence;
- UUID idempotency and deterministic ordering;
- atomic writes and fail-open recovery;
- deterministic retrieval;
- authoritative ACTION ingestion;
- server-observed RELATIONSHIP_CHANGE ingestion;
- text and voice DIALOGUE parity.

Ranking:

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
recent dialogue messages = 12
max dialogue message = 1200 Unicode code points
episodic entries = 6
semantic entries = 6
```

Working Memory is not persisted.

`0.1.11` live evidence confirmed:

```text
NPC A durable rolling history = 16
NPC A prompt history = 12
continuity after prompt bound = PASS
```

## Semantic Memory foundation — PR #46

Implemented:

```text
SemanticMemoryEntry
SemanticMemoryStore
SemanticMemoryQuery
SemanticMemoryRetriever
SemanticMemoryContextFormatter
SemanticMemoryContextProvider
```

Store guarantees:

- NPC isolation;
- bounded retention;
- UUID idempotency;
- deterministic ordering;
- atomic persistence;
- fail-open malformed-file recovery.

Ranking:

```text
related-entity relevance 40%
importance               30%
confidence               20%
recency                   10%
```

Prompt layers remain physically separate:

```text
worldFacts               authoritative current state
memoryContext            episodic memory
semanticMemoryContext    semantic FACT/BELIEF memory
```

## Controlled semantic ingestion — PR #49

```text
merge: c6a7a17aa5bd7806667ff3b8b502b852640e606c
verified head: 7f9916e510a2ca70245d93c5b308ee31758fed0f
```

Implemented production flows:

```text
successful safe action
→ ACTION / SYSTEM_OBSERVED
→ semantic FACT
```

```text
persisted relationship transition
→ RELATIONSHIP_CHANGE / SYSTEM_OBSERVED
→ semantic FACT
```

Automatic FACT requires:

```text
provenance = SYSTEM_OBSERVED
type in {ACTION, OBSERVATION, RELATIONSHIP_CHANGE}
```

Critical exclusion:

```text
DIALOGUE
→ episodic only
→ no automatic semantic entry
```

An explicit sourced BELIEF API exists, but ordinary dialogue is not automatically converted to BELIEF.

PR #49 was live-validated by `0.1.12+1.21.1`.

## Deterministic semantic consolidation — PR #53

```text
merge: f85879d254f37d7f860380362b296e047bbbb781
verified head: 19c3d3e840431cc2b1b34e1841e2075f56e99f71

VillAIgence CI #746 / 30561015885 — SUCCESS
Java Pull Request CI #300 / 30561015985 — SUCCESS
```

Implemented:

```text
SemanticMemoryConsolidator
SemanticMemoryStore append integration
SemanticMemoryStore load-time in-memory consolidation
```

Consolidation key:

```text
ownerNpcId
kind
provenance
canonical statement
canonical relatedEntities set
```

Canonical statement:

```text
Unicode NFKC
+ control/whitespace collapse
+ trim
+ Locale.ROOT lowercase
```

Two entries consolidate only when both have source event IDs and the full key matches.

Consolidated result:

```text
sourceEventIds       = sorted union
relatedEntities      = sorted union
gameTime             = max
createdAtEpochMillis = max
importance           = max
confidence           = max
statement            = deterministic representative
id                   = deterministic consolidation UUID
```

Safety properties:

- exact UUID replay remains a no-op and does not rewrite the file;
- a single sourced entry keeps its original UUID;
- distinct corroborating evidence receives a stable consolidation UUID;
- consolidation runs before retention trimming;
- FACT never merges with BELIEF;
- different BELIEF provenance never merges;
- different related-entity sets never merge;
- unsourced entries never merge;
- confidence is not artificially increased;
- no fuzzy matching, LLM, embeddings or vector database;
- JSON format remains version 1 with no new fields.

Existing compatible entries are consolidated in memory during load. Loading alone does not rewrite the file; the compacted representation is saved on the next normal append.

---

# Live validation status

## 0.1.12+1.21.1 — PASS

Validated on a real server:

```text
successful NPC actions = 2
ACTION events = 2
relationship transition = trust +1, affinity +1
RELATIONSHIP_CHANGE events = 1
semantic FACT entries = 3
semantic UUID duplicates = 0
DIALOGUE interactions = 9
DIALOGUE-derived semantics = 0
```

Every FACT had correct kind, provenance, NPC owner, deterministic UUID and source-event linkage.

Byte-identical after restart:

```text
memory.json
memory2.json
semantic-memory.json
relationships.json
voices.json
```

Operations:

```text
Chat SUCCESS
STT SUCCESS / 1059 ms
TTS SUCCESS / 1663 ms
Voice Chat / Opus PASS
monitor PASS
UDP 24454 / 25565 PASS
startup persistence errors none
```

Canonical evidence:

```text
docs/livingworld/VALIDATION_0.1.12.md
```

## PR #53 validation boundary

Not yet live-proven:

- two distinct real source events consolidating into one semantic entry;
- both source UUIDs persisted exactly once;
- stable consolidation UUID after restart;
- separation for different related entities on a real server;
- load-time consolidation of a pre-existing file;
- byte-stable persistence after consolidation.

Do not call consolidation live-validated until the scenario below passes.

---

# Roadmap status

## 0.1.x Reliability

Stable and live-validated through `0.1.12+1.21.1`. Continue only for concrete defects or explicit soak, backup/restore or provider-failure goals.

## 0.2 Memory 2.0 — active and substantially advanced

Implemented:

```text
Episodic MemoryEvent model and persistence
+ deterministic episodic retrieval
+ ACTION / DIALOGUE / RELATIONSHIP_CHANGE ingestion
+ text / voice parity
+ Working Memory bounds
+ Semantic FACT/BELIEF model and persistence
+ deterministic semantic retrieval
+ shared layered prompt integration
+ controlled semantic FACT ingestion
+ deterministic semantic consolidation
```

Still not implemented or not proven:

- real-server validation of PR #53 consolidation;
- forgetting/decay;
- automatic controlled BELIEF producers;
- legacy `memory.json` migration;
- NPC-to-NPC knowledge and rumor propagation;
- trustworthy causal relationship reasons;
- long-horizon recall after days without full raw dialogue;
- large multiplayer and multi-day soak validation.

## Next sequence

```text
1. Live-validate PR #53 consolidation
2. Add explicit forgetting/decay and retention rules
3. Design legacy memory.json migration after semantic layers stabilize
4. Run long-horizon Memory 2.0 exit-criterion validation
5. Begin 0.3 Personality + NPC↔NPC social graph
```

No embeddings, vector DB or LLM truth classification should be prerequisites.

---

# Immediate live-test scenario

```text
1. Install a build containing PR #53.
2. Record semantic-memory.json hash, entry count and sourceEventIds.
3. Produce two distinct authoritative events with the same normalized statement and related-entity set.
4. Confirm memory2.json contains two distinct source event UUIDs.
5. Confirm semantic-memory.json contains one consolidated entry.
6. Confirm sourceEventIds contains both UUIDs exactly once.
7. Confirm the consolidated UUID is deterministic.
8. Replay one source and confirm no duplicate or file rewrite.
9. Produce identical text for a different related entity and confirm a separate semantic entry.
10. Confirm FACT and BELIEF / different BELIEF provenance remain separate where testable.
11. Restart the server.
12. Confirm semantic-memory.json remains byte-stable and retrieval still works.
13. Confirm Chat, STT, TTS, Voice Chat, Opus, monitor and ports remain healthy.
```

After success, create a new validation document and promote the tested build to the latest live-server checkpoint.

---

# New-session handoff protocol

Preferred resume prompt:

> **Open `docs/PROJECT_STATE.md`, `docs/CHANGELOG.md`, `docs/ROADMAP.md` and the latest validation evidence in `True-Ruslan/villAIgence`. Check recent PRs, tags/releases and CI, then tell me what is implemented, what is live-validated, what changed since the state file, and what should be built next.**

A new session must:

1. read this file;
2. read `docs/CHANGELOG.md`;
3. read `docs/ROADMAP.md`;
4. inspect current `1.21.1` HEAD;
5. inspect recent merged/open PRs;
6. inspect latest tag/release and CI state;
7. reconcile newer live-test evidence;
8. continue from the first unimplemented priority;
9. update canonical state after material progress.

```text
docs/ROADMAP.md       → where the project is going
docs/PROJECT_STATE.md → what exists, what is proven, and what comes next
docs/CHANGELOG.md     → material implementation and validation history
```
