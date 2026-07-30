# VillAIgence Project State

> **Canonical current-state handoff.** Read this file before `docs/ROADMAP.md` when resuming work.
>
> Last major state update: **2026-07-30**, after live validation of `0.1.13+1.21.1`.
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

feature CI:
VillAIgence CI #746 / 30561015885 — SUCCESS
Java Pull Request CI #300 / 30561015985 — SUCCESS

latest canonical state merge before this validation update:
PR #54 — b553bf7e83674145bdf42927b9ace7287afa560c

latest live-validated release checkpoint:
0.1.13+1.21.1 — PASS
validation date: 2026-07-30
release/tag and tested commit:
b553bf7e83674145bdf42927b9ace7287afa560c
```

Canonical live evidence:

```text
docs/livingworld/VALIDATION_0.1.13.md
```

Previous live checkpoints:

```text
0.1.12+1.21.1 — controlled Semantic Memory ingestion
docs/livingworld/VALIDATION_0.1.12.md

0.1.11+1.21.1 — Working Memory foundation
docs/livingworld/VALIDATION_0.1.11.md

0.1.10+1.21.1 — text/voice Memory 2.0 parity
docs/livingworld/VALIDATION_0.1.10.md
```

`0.1.13+1.21.1` supersedes `0.1.12+1.21.1` as the latest confirmed live-server checkpoint.

## Release metadata status

```text
0.1.11+1.21.1 → 60236524e37b60c639b93405f809ade883be253f
0.1.12+1.21.1 → 746fa75ab4b5f4bee385efa0c8ae51009c1aec58
0.1.13+1.21.1 → b553bf7e83674145bdf42927b9ace7287afa560c
```

Before this validation documentation update, GitHub comparison confirmed that `0.1.13+1.21.1` and branch `1.21.1` were identical at `b553bf7e83674145bdf42927b9ace7287afa560c`.

Later documentation commits may move `1.21.1`; the tag remains the exact tested release payload.

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
2. Mutable state used by async AI must be captured into immutable bounded context before provider work.
3. LLM may propose dialogue, actions and relationship deltas; server policy validates and executes mutations.
4. Provider/model changes must not redefine persistent NPC identity.
5. External AI and auxiliary persistence failures fail soft whenever safe.
6. API credentials remain server-side.
7. Persistent formats remain explicit, inspectable and backed up with the world.
8. Retry/replay paths must not duplicate persistent or gameplay side effects.
9. Claims and beliefs remain non-authoritative unless server-owned evidence makes them factual.
10. Confidence never upgrades BELIEF into FACT.
11. Consolidation preserves provenance and all independent source evidence.
12. Autonomous AI must eventually be event-driven and budgeted rather than “LLM every tick.”

Canonical authority flow:

```text
Minecraft/server state
→ immutable bounded context
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

`memory.json` remains active. Memory 2.0 is additive; legacy migration has not started.

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

## Working Memory — PR #46

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

Working Memory is turn-local and not persisted.

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
- deterministic ordering and retrieval;
- atomic persistence;
- fail-open malformed-file recovery.

Semantic ranking:

```text
related-entity relevance 40%
importance               30%
confidence               20%
recency                  10%
```

Prompt layers remain separate:

```text
worldFacts               authoritative current state
memoryContext            episodic memory
semanticMemoryContext    semantic FACT/BELIEF memory
```

## Controlled semantic ingestion — PR #49

```text
successful safe action
→ ACTION / SYSTEM_OBSERVED
→ semantic FACT

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

Both entries must contain source event IDs and the full key must match.

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

- exact UUID replay is a no-op and does not rewrite the file;
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

`0.1.13+1.21.1` live-validates the primary append-time production path.

---

# Live validation status

## 0.1.13+1.21.1 — PASS

Primary semantic consolidation behavior was validated on a real server.

```text
two same-knowledge authoritative ACTION events             PASS
distinct ACTION UUIDs                                      PASS
one consolidated Semantic Memory entry                     PASS
both sourceEventIds present exactly once                   PASS
observed deterministic UUID                                093aabb0-e61b-3e62-a5fe-fbb9d15b8494
independently calculated UUID                              093aabb0-e61b-3e62-a5fe-fbb9d15b8494
retry created new ACTION                                   no
retry changed semantic file                               no
NPC A / NPC B owner isolation                              PASS
NPC A / NPC B relatedEntities isolation                    PASS
```

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
0.1.13 reloaded                                            PASS
Chat and DIALOGUE                                          PASS
Voice Chat / Opus                                          PASS
STT / TTS voice dialogue                                   PASS
UDP 24454 / 25565                                          PASS
LinuxGSM monitor                                           PASS
server STARTED                                             PASS
VillAIgence / persistence / OutOfMemory errors             none
```

Canonical evidence:

```text
docs/livingworld/VALIDATION_0.1.13.md
```

Live-test boundary:

- append-time consolidation is proven;
- restart loading of the already consolidated file is proven;
- manually seeded historical logical duplicates were not separately tested live;
- that prepared-file load-time compaction path remains unit-tested.

## 0.1.12+1.21.1 — previous PASS checkpoint

Validated controlled ACTION and RELATIONSHIP_CHANGE → FACT ingestion, deterministic source-based semantic UUIDs, retry idempotency, DIALOGUE exclusion and restart-safe semantic persistence.

Canonical evidence:

```text
docs/livingworld/VALIDATION_0.1.12.md
```

---

# Roadmap status

## 0.1.x Reliability

Stable and live-validated through `0.1.13+1.21.1`. Continue only for concrete defects or explicit soak, backup/restore or provider-failure goals.

## 0.2 Memory 2.0 — active and substantially advanced

Implemented and live-proven:

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
+ source-union and retry idempotency
+ NPC and related-entity isolation
```

Still not implemented or not proven:

- forgetting/decay;
- automatic controlled BELIEF producers;
- legacy `memory.json` migration;
- NPC-to-NPC knowledge and rumor propagation;
- trustworthy causal relationship reasons;
- long-horizon recall after days without full raw dialogue;
- large multiplayer and multi-day soak validation;
- live test of manually seeded pre-existing semantic duplicates.

## Next sequence

```text
1. Add explicit forgetting/decay and deterministic retention rules
2. Live-validate forgetting/decay
3. Design legacy memory.json migration after semantic layers stabilize
4. Run long-horizon Memory 2.0 exit-criterion validation
5. Begin 0.3 Personality + NPC↔NPC social graph
```

No embeddings, vector DB or LLM truth classification should be prerequisites.

---

# Immediate development target

Design forgetting/decay as deterministic retention policy, not probabilistic deletion.

Required properties:

- server-controlled and provider-independent;
- FACT/BELIEF-aware;
- importance-, confidence-, recency- and corroboration-aware;
- provenance-preserving;
- no deletion of current authoritative world truth merely because it is old;
- deterministic ordering and testable thresholds;
- safe migration from existing format version 1;
- no LLM decision about what is forgotten.

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
