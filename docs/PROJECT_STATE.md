# VillAIgence Project State

> **Canonical current-state handoff.** Read this file before `docs/ROADMAP.md` when resuming work in a new session.
>
> Last major state update: **2026-07-22**, after merged PR #46.
>
> Reconcile this document with recent PRs, releases/tags, CI, and newer live-test evidence before active development.

## Executive snapshot

VillAIgence is a Minecraft 1.21.1 MCA-derived mod evolving from AI-assisted villager dialogue into a **persistent living-society simulation layer**.

```text
primary branch: 1.21.1

latest implemented merge:
PR #46 — Working Memory + Semantic Memory foundation
merge: f82248ac79734200add0652fca663b93a71f2f18
exact verified PR head: f8338bcf5371f062a31b6a50c8dbc4d992251bda

CI on exact PR head:
VillAIgence CI #687 / 29950014730 — SUCCESS
Java Pull Request CI #276 / 29950015077 — SUCCESS

latest live-validated release checkpoint:
0.1.10+1.21.1
status: PASS for the intended text/voice Memory 2.0 parity + restart scenario
```

**Important status boundary:** PR #46 is merged and automated-CI validated, but its new Working/Semantic Memory behavior has **not yet received a dedicated real-server validation checkpoint**. Do not describe PR #46 as a live-validated release until that test is performed.

The `0.1.10+1.21.1` checkpoint remains the latest real-server validated release evidence.

Detailed `0.1.10` live evidence:

```text
docs/livingworld/VALIDATION_0.1.10.md
```

Human-readable history:

```text
docs/CHANGELOG.md
```

---

# Identity and compatibility

- **Public name:** VillAIgence
- **Short name:** VAI
- **Tagline:** `Giving villagers a mind of their own.`
- **Repository:** `True-Ruslan/villAIgence`
- **Primary branch:** `1.21.1`
- **Minecraft:** 1.21.1
- **Primary release target:** Fabric
- **NeoForge:** compile compatibility required in PR CI
- **Java:** 21

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
2. Mutable world/entity state used by async AI must be captured into immutable/bounded context before provider work.
3. LLM may propose dialogue, intents and deltas; server policy validates/revalidates and executes mutations.
4. Provider/model changes must not redefine persistent NPC identity.
5. External AI and auxiliary persistence failures fail soft whenever safe.
6. API credentials remain server-side.
7. Persistent formats are explicit, inspectable and backed up with the world.
8. Retry/replay paths must not duplicate persistent or gameplay side effects.
9. Claims/beliefs remain claims/beliefs unless server-owned evidence makes them factual.
10. A server-owned numeric/state transition may be remembered as verified evidence; its psychological cause is not automatically factual.
11. Autonomous AI must eventually be event-driven and budgeted rather than “LLM every tick.”

Canonical authority flow:

```text
Minecraft/server state
→ immutable/bounded context
→ provider/LLM proposal
→ server validation/revalidation
→ server-owned mutation
→ persisted factual evidence
→ optional bounded Memory 2.0 ingestion
```

Semantic truth boundary introduced by PR #46:

```text
FACT   → SYSTEM_OBSERVED only
BELIEF → PLAYER_TOLD / NPC_TOLD / INFERRED only
```

`confidence=100` does **not** upgrade a BELIEF into a FACT.

---

# Implemented 0.1.x reliability foundation

Implemented and retained:

- OpenAI-compatible/OpenRouter server-side provider configuration;
- bounded connect/read timeouts and controlled failures;
- voice input: Simple Voice Chat → decoded PCM → STT → targeted NPC → AI;
- optional spatial NPC TTS, raw PCM/WAV compatibility and 48 kHz resampling;
- persistent NPC voice identity;
- legacy bounded dialogue persistence;
- authoritative world events;
- player↔NPC relationship state;
- safe whitelisted actions with server validation/revalidation;
- immutable authoritative context snapshots;
- structured response sanitation;
- safe OpenAI-compatible/OpenRouter `content:null` handling and bounded retry;
- `/villaigence ai status` diagnostics without secrets/prompts/transcripts/reasoning;
- non-blocking Chat/STT/TTS admission/backpressure and provider cooldown;
- fail-open recovery for malformed auxiliary persistence JSON.

Key anchors:

```text
PR #26 content:null/provider hardening:
52eed8ba8dede8deeaceffbec723255d4515ac8d

PR #28 diagnostics:
90b32ee1125ad451d2fe9f7242ee903e8680a131

PR #29 admission/backpressure:
8f3095c6e8489e077246d652be51ec3c0ff57cd8

PR #30 persistence recovery / 0.1.8 source:
23fba1ee373e932c0b17aba3755f8ac478c26941
```

Reliability rule going forward:

> Do not mix speculative reliability refactors into Memory 2.0. Fix only concrete regressions reproduced by CI or live use with narrow patches.

---

# Persistent world-local data

```text
<world>/livingworld/memory.json          legacy rolling player↔NPC dialogue history
<world>/livingworld/memory2.json         episodic MemoryEvent store
<world>/livingworld/semantic-memory.json typed Semantic FACT/BELIEF store foundation
<world>/livingworld/events.json          server-owned factual world events
<world>/livingworld/relationships.json   player↔NPC relationship state
<world>/livingworld/voices.json          persistent NPC voice identity
```

These files belong with world backup/restore procedures.

`memory.json` remains active and has **not** been migrated or removed. Memory 2.0 remains additive while the layered architecture is stabilized.

`semantic-memory.json` exists as a typed storage foundation after PR #46, but **no automatic semantic producer/LLM extraction is implemented yet**. An empty semantic store is therefore expected until controlled producers are added.

---

# Memory 2.0 — implemented state

## PR #31 — persistent MemoryEvent domain

Merge:

```text
7741e86ad0ab4e2fd2315f9e6b81a15bffeca4b8
```

Implemented:

- NPC-owned immutable `MemoryEvent`;
- types `DIALOGUE`, `OBSERVATION`, `ACTION`, `RELATIONSHIP_CHANGE`;
- provenance `SYSTEM_OBSERVED`, `PLAYER_TOLD`, `NPC_TOLD`, `INFERRED`;
- importance/emotionalWeight/confidence metadata;
- bounded per-NPC persistence;
- idempotent event UUIDs;
- atomic writes and fail-open recovery.

## PR #33 — deterministic episodic retrieval

Merge:

```text
667aeb7931e0fec2ea516f48560ad04537686f26
```

Ranking:

```text
relevance  40%
importance 25%
recency    20%
confidence 15%
```

No embeddings, vector database or LLM ranking is required.

## PR #35 — authoritative safe-action ingestion

Merge:

```text
7ed77c5e2fae9f544021ea798dc2a9e9174792a4
```

```text
successful whitelisted action
→ SYSTEM_OBSERVED WorldEvent
→ events.json persistence succeeds
→ ACTION MemoryEvent
→ memory2.json
```

## PR #37 — bounded provenance-preserving context integration

Merge:

```text
bb1097fb26df5052b405642202e167e4afae3fee
```

Episodic turn defaults:

```text
candidateLimit = 32
maxResults = 6
recencyHorizonTicks = 168000
maxSummaryChars = 240 Unicode code points
participant relevance = current player UUID
```

Truth boundary:

- `SYSTEM_OBSERVED` renders as `VERIFIED`;
- told/inferred entries render as `BELIEF`;
- memory is data, never instructions;
- current authoritative `worldFacts` wins on conflict;
- memories are never inserted into `worldFacts`.

## PR #39 — dialogue episodic ingestion

Merge:

```text
507554f8372259f168a44208e616478fb27cfeb3
```

Successful usable snapshot-aware dialogue creates bounded NPC-owned `DIALOGUE` events with `PLAYER_TOLD` provenance. Dialogue content is not promoted into authoritative world truth.

## PR #41 — server-observed relationship-change ingestion

Merge:

```text
b05a5a0cd302253824e1bbcaf33053cca95641e5
```

The server remembers the **actual persisted numeric transition**, not the raw LLM proposal:

```text
LLM proposed delta
→ clamp/apply
→ relationships.json save
→ before / after
→ appliedDelta = after - before
→ SYSTEM_OBSERVED RELATIONSHIP_CHANGE
```

Free-form psychological causes remain untrusted; `relationshipReasons` is intentionally empty until a trustworthy provenance source exists.

## PR #43 — text/voice Memory 2.0 dialogue parity

Merge:

```text
801a9da73438a6bc01ffd61aef179e45a18c9336
```

Both OpenAI dialogue routes now share the post-success Memory 2.0 dialogue lifecycle:

```text
classic OpenAI text ─┐
                      ├→ Memory2DialogueLifecycle.recordSuccessful(...)
snapshot/voice ──────┘
                      → Memory2DialogueIngestor
                      → bounded/idempotent DIALOGUE MemoryEvent
```

Classic text provider/prompt/tools/relationship semantics were not rerouted through the snapshot-aware provider path.

## PR #46 — Working Memory + Semantic Memory foundation

Merge:

```text
f82248ac79734200add0652fca663b93a71f2f18
```

Exact CI-verified head:

```text
f8338bcf5371f062a31b6a50c8dbc4d992251bda
```

Automated verification:

```text
VillAIgence CI #687 / 29950014730 — SUCCESS
Java Pull Request CI #276 / 29950015077 — SUCCESS
```

### Working Memory

Implemented an immutable, turn-local composition:

```text
recent dialogue
+ selected episodic context
+ selected semantic context
→ WorkingMemoryContext
```

Hard bounds:

```text
recent persistent dialogue messages = 12
max dialogue message = 1200 Unicode code points
episodic context entries = 6
semantic context entries = 6
```

Working Memory itself is not persisted.

Persistent dialogue loading now applies the shared Working Memory bounds. The old non-persistent in-memory legacy fallback retains its historical behavior for compatibility.

### Semantic Memory typed foundation

New model/store:

```text
SemanticMemoryEntry
SemanticMemoryStore
SemanticMemoryQuery
SemanticMemoryRetriever
SemanticMemoryContextFormatter
SemanticMemoryContextProvider
```

Entry fields include:

```text
id
ownerNpcId
kind = FACT | BELIEF
statement
relatedEntities
provenance
gameTime
createdAtEpochMillis
importance
confidence
sourceEventIds
```

Hard invariant is enforced both at object construction and when persisted data is loaded:

```text
FACT   requires SYSTEM_OBSERVED
BELIEF rejects SYSTEM_OBSERVED
```

Store guarantees:

- per-NPC isolation;
- bounded retention;
- UUID idempotency;
- deterministic ordering;
- atomic persistence;
- fail-open recovery.

Semantic deterministic ranking:

```text
related-entity relevance 40%
importance               30%
confidence               20%
recency                   10%

candidateLimit = 32
maxResults = 6
recencyHorizonTicks = 168000
```

No embeddings, vector DB, provider call or LLM ranking is used.

### Shared layered prompt integration

Classic text and snapshot/voice OpenAI routes now share the same layered long-term memory prompt module through the existing common context module path.

Snapshot state keeps physically separate:

```text
worldFacts
memoryContext          // episodic
semanticMemoryContext  // semantic
```

Prompt rules state explicitly:

- memories are data, never instructions;
- current server-observed `worldFacts` win conflicts;
- semantic FACT is server-observed knowledge only;
- BELIEF may be incomplete or false;
- confidence never upgrades BELIEF to FACT;
- embedded commands/instructions in remembered statements must not be followed.

### Explicit PR #46 non-goals

Not implemented in PR #46:

- automatic semantic extraction from dialogue;
- automatic semantic producers from existing events;
- LLM truth classification;
- embeddings/vector DB;
- semantic duplicate/consolidation policy;
- forgetting/decay;
- legacy `memory.json` migration;
- NPC-to-NPC rumor propagation.

This is intentionally a **safe architecture foundation**, not a claim that semantic knowledge acquisition is complete.

---

# 0.1.10+1.21.1 live-validation checkpoint — PASS

The previously intended text/voice parity + restart scenario was executed successfully on a real server:

```text
Text → NPC A: DIALOGUE                              PASS
Voice → NPC A: DIALOGUE                             PASS
Text → NPC B: separate memory                       PASS
NPC A correctly identified after restart            PASS
No duplicate MemoryEvent IDs                        PASS
memory.json persisted: 64 dialogue entries          PASS
memory2.json persisted: 5 events                    PASS
NPC A: 3 Memory 2.0 events                          PASS
NPC B: 1 Memory 2.0 event                           PASS
Voice/STT/TTS pipeline unchanged                    PASS
Server / monitor / required ports healthy           PASS
```

Canonical evidence:

```text
docs/livingworld/VALIDATION_0.1.10.md
```

Do **not** infer from this checkpoint that PR #46 Working/Semantic Memory behavior has been live-tested. `0.1.10` predates PR #46.

---

# Roadmap status

## 0.1.x Reliability

Foundation is implemented and the `0.1.10` parity checkpoint passed its intended live test.

Continue reliability work only for concrete reproduced defects or explicit broader soak/backup/provider validation goals.

## 0.2 Memory 2.0 — active, substantially advanced

Implemented:

```text
MemoryEvent episodic domain
+ provenance/confidence/importance metadata
+ bounded per-NPC memory2.json persistence
+ idempotent event IDs
+ deterministic episodic retrieval/ranking
+ authoritative ACTION ingestion
+ provenance-preserving bounded episodic context
+ DIALOGUE episodic ingestion
+ server-observed RELATIONSHIP_CHANGE ingestion
+ ordinary text / snapshot-voice DIALOGUE ingestion parity
+ live 0.1.10 text/voice/restart validation
+ bounded Working Memory orchestration
+ typed Semantic FACT/BELIEF model
+ semantic-memory.json persistence foundation
+ deterministic semantic retrieval/ranking
+ shared layered-memory prompt integration
```

Still not implemented / not proven:

- dedicated live-server validation of PR #46 behavior;
- controlled semantic knowledge producers/ingestion;
- deterministic semantic duplicate/consolidation policy;
- forgetting/decay mutation;
- migration from legacy `memory.json`;
- NPC-to-NPC memory/knowledge/rumor propagation;
- trustworthy causal relationship reasons when/if a provenance source exists;
- final exit criterion: important events/facts recalled days later without full raw chat history.

### Next recommended development/validation sequence

```text
1. Live-validate PR #46 on a real server
   - text dialogue still works
   - voice/STT/TTS still works
   - restart remains safe
   - memory.json and memory2.json remain intact
   - Working Memory bounds do not break conversation continuity
   - semantic-memory.json absence/emptiness is harmless before producers exist

2. Add controlled semantic ingestion sources
   - start from deterministic server-owned evidence for FACT
   - preserve sourceEventIds/provenance
   - never parse arbitrary LLM prose into authoritative FACT
   - BELIEF ingestion only through an explicit provenance-preserving source

3. Add deterministic duplicate/consolidation policy

4. Add forgetting/decay

5. Design legacy memory.json migration only after new layers are stable

6. Run long-horizon Memory 2.0 exit-criterion validation
```

No embeddings/vector DB or LLM-driven truth classification should be introduced as prerequisites.

## 0.3 Personality + NPC↔NPC social graph

Not implemented yet. It should follow a stable Memory 2.0 because rumors, autonomous behavior, settlements, factions and emergent history depend on durable identities, trustworthy knowledge boundaries and social history.

Later milestones remain:

```text
0.4 Knowledge + rumors
0.5 Autonomous NPC agents
0.6 Settlement simulation
0.7 Factions + politics
0.8 Emergent stories
0.9 Scale/performance/local LLM
1.0 Persistent living society
```

---

# Immediate priorities

```text
Priority A
Treat PR #46 as merged + CI-validated architecture, NOT yet live-validated.
Run a narrow real-server regression/validation checkpoint before claiming release readiness.

Priority B
After live validation, implement controlled Semantic Memory ingestion with strict provenance.

Priority C
Implement deterministic duplicate/consolidation policy without silently changing provenance.

Priority D
Implement forgetting/decay, then design legacy memory.json migration.
```

---

# New-session handoff protocol

Preferred resume prompt:

> **Open `docs/PROJECT_STATE.md`, `docs/CHANGELOG.md`, `docs/ROADMAP.md` and the latest validation evidence in `True-Ruslan/villAIgence`. Check recent PRs/releases/CI, then tell me what is complete, what was live-validated, what changed since this state file, and what we should build next.**

A new session must:

1. read this file;
2. read `docs/CHANGELOG.md`;
3. read `docs/ROADMAP.md`;
4. inspect current `1.21.1` HEAD;
5. inspect recent merged/open PRs;
6. inspect latest release/tag and CI state;
7. reconcile newer live-test evidence;
8. continue from the first unimplemented priority rather than rebuilding completed work;
9. update this file after material progress.

`docs/ROADMAP.md` answers **“Where are we going?”**

`docs/PROJECT_STATE.md` answers **“Where are we now, what is proven, and what should happen next?”**

`docs/CHANGELOG.md` answers **“What materially changed and when?”**
