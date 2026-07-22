# VillAIgence Project State

> **Canonical current-state handoff.** Read this file before `docs/ROADMAP.md` when resuming work in a new session.
>
> Last major state update: **2026-07-22**.
>
> Reconcile this document with recent PRs, releases/tags, CI, and new live-test evidence before active development.

## Executive snapshot

VillAIgence is a Minecraft 1.21.1 MCA-derived mod evolving from AI-assisted villager dialogue into a **persistent living-society simulation layer**.

```text
primary branch: 1.21.1
current implemented code HEAD after PR #43:
801a9da73438a6bc01ffd61aef179e45a18c9336

last previously confirmed published release:
0.1.8+1.21.1
release commit:
23fba1ee373e932c0b17aba3755f8ac478c26941
release workflow:
29918008438 — SUCCESS
```

A `0.1.9`-era build was live-tested by the user after the first Memory 2.0 slices. That test exposed text/voice Memory 2.0 asymmetry, now fixed by PR #43. Before publishing the next artifact, reconcile current release/tag state and package the `1.21.1` HEAD containing PR #43.

Post-`0.1.8` Memory 2.0 development on `1.21.1`:

```text
PR #31  persistent MemoryEvent foundation
PR #33  deterministic bounded retrieval/ranking
PR #35  authoritative safe-action WorldEvent ingestion
PR #37  bounded provenance-preserving NPC context integration
PR #39  controlled successful dialogue → episodic-memory ingestion
PR #41  server-observed relationship-change ingestion
PR #43  text/voice Memory 2.0 dialogue-ingestion parity
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
2. Mutable world/entity state used by async AI must be captured into immutable context before provider work.
3. LLM may propose dialogue/intents/deltas; server policy validates and executes mutations.
4. Provider/model changes must not redefine persistent NPC identity.
5. External AI and auxiliary persistence failures fail soft whenever safe.
6. API credentials remain server-side.
7. Persistent formats are explicit, inspectable, and backed up with the world.
8. Retry/replay paths must not duplicate persistent or gameplay side effects.
9. Claims/beliefs remain claims/beliefs unless server-owned evidence makes them factual.
10. A server-owned numeric/state transition may be remembered as verified evidence; its psychological cause is not automatically factual.
11. Later autonomous AI must be event-driven and budgeted rather than “LLM every tick.”

Canonical authority flow:

```text
Minecraft/server state
→ immutable/bounded context
→ provider/LLM proposal
→ server validation/revalidation
→ server-owned mutation
→ persistent factual evidence
→ optional bounded Memory 2.0 ingestion
```

---

# Implemented 0.1.x reliability foundation

Implemented and retained:

- OpenAI-compatible/OpenRouter server-side provider configuration;
- bounded connect/read timeouts and controlled failures;
- voice input: Simple Voice Chat → decoded PCM → STT → targeted NPC → AI;
- optional spatial NPC TTS, raw PCM/WAV compatibility and 48 kHz resampling;
- persistent NPC voice identity in `<world>/livingworld/voices.json`;
- legacy bounded dialogue history in `<world>/livingworld/memory.json`;
- authoritative world events in `<world>/livingworld/events.json`;
- player↔NPC relationship state in `<world>/livingworld/relationships.json`;
- safe whitelisted actions with server validation/revalidation;
- immutable authoritative context snapshots;
- structured response sanitation;
- safe `content:null` handling and bounded retry;
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

Live reliability checklist:

```text
docs/livingworld/PLAYTEST_CHECKLIST.md
```

---

# Persistent world-local data

```text
<world>/livingworld/memory.json         legacy rolling player↔NPC dialogue history
<world>/livingworld/memory2.json        Memory 2.0 event store
<world>/livingworld/events.json         server-owned factual world events
<world>/livingworld/relationships.json  player↔NPC relationship state
<world>/livingworld/voices.json         persistent NPC voice identity
```

These belong with world backup/restore procedures.

---

# Memory 2.0 — implemented slices

Primary storage:

```text
<world>/livingworld/memory2.json
```

## PR #31 — persistent MemoryEvent domain

Merge:

```text
7741e86ad0ab4e2fd2315f9e6b81a15bffeca4b8
```

Core event fields:

```text
id
ownerNpcId
type
summary
participants
provenance
gameTime
createdAtEpochMillis
importance
emotionalWeight
confidence
relationshipReasons
```

Types:

```text
DIALOGUE
OBSERVATION
ACTION
RELATIONSHIP_CHANGE
```

Provenance:

```text
SYSTEM_OBSERVED
PLAYER_TOLD
NPC_TOLD
INFERRED
```

`MemoryEventStore` provides per-NPC isolation, bounded retention, idempotent UUIDs, atomic persistence, deterministic newest-first access, and fail-open recovery.

## PR #33 — deterministic bounded retrieval

Merge:

```text
667aeb7931e0fec2ea516f48560ad04537686f26
```

Implemented deterministic ranking without embeddings/LLM ranking:

```text
relevance  40%
importance 25%
recency    20%
confidence 15%
```

Hard-bounded candidates/results and deterministic tie-breaking are enforced.

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

The source `WorldEvent.id` is reused for MemoryEvent idempotency. Memory failure cannot roll back the action/factual event.

Configuration:

```text
memory2Enabled = true
memory2MaxEventsPerNpc = 256
normalized max = 1..512
config version = 2
```

## PR #37 — bounded provenance-preserving context integration

Merge:

```text
bb1097fb26df5052b405642202e167e4afae3fee
```

Memory 2.0 contributes a bounded set to real snapshot-aware NPC turns:

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
- Memory 2.0 entries are never inserted into `worldFacts`.

## PR #39 — successful dialogue episodic ingestion

Merge:

```text
507554f8372259f168a44208e616478fb27cfeb3
```

Successful usable dialogue creates bounded NPC-owned `DIALOGUE` events:

```text
type = DIALOGUE
provenance = PLAYER_TOLD
importance = 40
emotionalWeight = 0
confidence = 60
relationshipReasons = []
```

Stored summary:

```text
Player said: <bounded utterance> | NPC replied: <bounded utterance>
```

Deterministic identity uses NPC/player IDs, originating game time and normalized player message. NPC reply and wall-clock time are excluded.

## PR #41 — server-observed relationship-change ingestion

Merge:

```text
b05a5a0cd302253824e1bbcaf33053cca95641e5
```

The server now remembers the **actual persisted relationship transition**, not the raw LLM proposal:

```text
LLM proposed delta
→ clamp/apply
→ relationships.json save
→ before / after
→ appliedDelta = after - before
→ SYSTEM_OBSERVED RELATIONSHIP_CHANGE
```

Mapping:

```text
importance = 55
emotionalWeight = 0
confidence = 100
relationshipReasons = []
```

`relationshipReasons` deliberately remains empty. Numeric transition is factual; free-form psychological cause is not automatically factual.

## PR #43 — text/voice Memory 2.0 dialogue parity

Merge:

```text
801a9da73438a6bc01ffd61aef179e45a18c9336
```

Live testing of the preceding build found:

```text
voice/snapshot turn → legacy memory.json + Memory 2.0 DIALOGUE
ordinary text turn  → legacy memory.json only
```

PR #43 fixes that asymmetry through one shared post-success lifecycle:

```text
classic OpenAI text ─┐
                     ├→ Memory2DialogueLifecycle.recordSuccessful(...)
snapshot/voice ──────┘
                     → Memory2DialogueIngestor
                     → bounded/idempotent DIALOGUE event
```

Important non-goal: classic text was **not** rerouted through snapshot prompt/provider semantics. Existing text provider/prompt/tools/relationship behavior remains unchanged; only Memory 2.0 post-success ingestion is shared.

Inworld/non-OpenAI classic behavior remains unchanged.

TDD / CI evidence:

```text
valid RED head:
1873cb65c736be155b81b5807ffcff33e22768b7
VillAIgence CI 29937886800 → expected FAILURE at :common:compileTestJava

shared lifecycle GREEN:
7980970b8ff593b36655d99dd2ef3c635c38b57e
VillAIgence CI 29938243453 → SUCCESS
Java PR CI 29938243395 → SUCCESS

wiring GREEN:
c40a93dc3d9cfdd89a6d1def9c9bd01b79eba97e
VillAIgence CI 29938570358 → SUCCESS
Java PR CI 29938570394 → SUCCESS

exact final head:
cee822de059d344e27b5b3456fc5eb4c187fff78
VillAIgence CI 29938941710 → SUCCESS
Java PR CI 29938941839 → SUCCESS
```

Final exact-head gates covered unit tests, Fabric build, distributable Fabric package verification, NeoForge build and Fabric compatibility build.

---

# Latest live-test evidence supplied by user

The user tested the pre-parity `0.1.9`-era build and restarted the server.

Observed successful voice cycle:

```text
STT  2.296 s
Chat 8.292 s
TTS  10.299 s
```

Also observed:

- text response persisted;
- legacy memory grew from 52 to 56 dialogue entries;
- fifth persistent voice profile appeared;
- Memory 2.0 created one unique `DIALOGUE` event for the snapshot/voice turn;
- after restart, hashes of `memory.json`, `memory2.json`, and `voices.json` were unchanged;
- server, cron, monitor and required ports remained healthy;
- no errors were reported.

The same test proved the now-fixed boundary: ordinary text chat did not create Memory 2.0 events before PR #43.

## Required next live validation for PR #43 build

```text
1. text → NPC A
2. voice → NPC A
3. text → NPC B
4. inspect memory2.json for unique DIALOGUE events from both transports
5. restart server
6. verify memory.json / memory2.json / voices.json persistence
7. verify no duplicate event IDs and no NPC A/B memory mixing
```

A successful result closes the text/voice Memory 2.0 parity regression.

---

# Release state / next patch

Last previously confirmed published anchor in this document:

```text
0.1.8+1.21.1
commit 23fba1ee373e932c0b17aba3755f8ac478c26941
workflow 29918008438 — SUCCESS
```

The user has live-tested a later `0.1.9`-era artifact. Before the next publication, inspect current GitHub releases/tags and package current `1.21.1` HEAD containing PR #43.

Recommended next patch-release purpose:

```text
text + voice Memory 2.0 dialogue parity
```

Do not move an already-published tag.

---

# Roadmap status

## 0.1.x Reliability

Implementation foundation complete; continue live validation and fix only concrete blocking regressions with narrow patches.

## 0.2 Memory 2.0 — active, partial

Implemented:

```text
MemoryEvent domain
+ provenance/confidence/importance metadata
+ bounded per-NPC memory2.json persistence
+ idempotent event IDs
+ deterministic bounded retrieval/ranking
+ authoritative ACTION ingestion
+ provenance-preserving bounded context integration
+ DIALOGUE episodic ingestion
+ server-observed RELATIONSHIP_CHANGE ingestion
+ ordinary text / snapshot-voice DIALOGUE ingestion parity
```

Still not implemented:

- dedicated working-memory orchestration beyond existing bounded dialogue/context;
- explicit semantic facts/beliefs layer;
- deterministic duplicate handling / consolidation policy;
- forgetting/decay mutation;
- migration from legacy `memory.json`;
- NPC-to-NPC memory/rumor propagation;
- trustworthy causal relationship reasons when/if a provenance source exists;
- final exit criterion: important events recalled days later without full raw chat history.

### Next recommended development slice

After PR #43 live parity validation, continue with **Working Memory orchestration + explicit Semantic Facts/Beliefs boundaries**.

Recommended split:

```text
Working Memory
→ intentionally small immediate conversational/task context
→ short-lived / bounded

Episodic Memory
→ existing MemoryEvent experiences
→ DIALOGUE / ACTION / RELATIONSHIP_CHANGE / OBSERVATION

Semantic Facts / Beliefs
→ typed knowledge model
→ explicit provenance + confidence
→ SYSTEM_OBSERVED facts physically/logically distinct from
  PLAYER_TOLD / NPC_TOLD / INFERRED beliefs
```

Do not introduce embeddings/vector DB or LLM-driven consolidation as prerequisites.

After that:

```text
deterministic duplicate/consolidation policy
→ forgetting/decay
→ legacy memory migration
→ Memory 2.0 exit-criterion validation
```

## 0.3 Personality + NPC↔NPC social graph

Not implemented yet. It should follow Memory 2.0 closely because rumors, autonomous behavior, settlements, factions and emergent history depend on durable identities and social history.

Later milestones:

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
Build/package current post-PR #43 HEAD and run the short text/voice parity live test.
Fix only concrete blocking regressions found by that test.

Priority B
After parity validation, continue 0.2 with Working Memory orchestration + Semantic Facts/Beliefs design.

Priority C
Implement deterministic duplicate/consolidation policy without changing provenance.

Priority D
Implement forgetting/decay, then design migration from legacy memory.json.
```

---

# New-session handoff protocol

Preferred resume prompt:

> **Open `docs/PROJECT_STATE.md` and `docs/ROADMAP.md` in `True-Ruslan/villAIgence`. Check recent PRs/releases/CI, then tell me what is complete, what changed since this state file, what live tests remain, and what we should build next.**

A new session must:

1. read this file;
2. read `docs/ROADMAP.md`;
3. inspect current `1.21.1` HEAD;
4. inspect recent merged/open PRs;
5. inspect latest releases/tags and CI;
6. reconcile discrepancies;
7. continue from the first unimplemented priority rather than rebuilding completed work;
8. update this file after material progress.

`docs/ROADMAP.md` answers **“Where are we going?”**

`docs/PROJECT_STATE.md` answers **“Where are we now, what was verified, and what should happen next?”**
