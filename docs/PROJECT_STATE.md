# VillAIgence Project State

> **Canonical current-state handoff.** Read this file before `docs/ROADMAP.md` when resuming work in a new session.
>
> Last major state update: **2026-07-22**.
>
> Reconcile this document with recent PRs, releases/tags, CI, and newer live-test evidence before active development.

## Executive snapshot

VillAIgence is a Minecraft 1.21.1 MCA-derived mod evolving from AI-assisted villager dialogue into a **persistent living-society simulation layer**.

```text
primary branch: 1.21.1

current implemented code anchor after PR #43:
801a9da73438a6bc01ffd61aef179e45a18c9336

canonical state-sync anchor after PR #44:
b7a53c1c24eb6dbedebec63f7dea06a6fb54ff69

latest live-validated release checkpoint:
0.1.10+1.21.1
status: PASS for the intended text/voice Memory 2.0 parity + restart scenario

last previously confirmed published release anchor in historical docs:
0.1.8+1.21.1
commit: 23fba1ee373e932c0b17aba3755f8ac478c26941
workflow: 29918008438 — SUCCESS
```

The user reports that `0.1.10+1.21.1` was released/tested successfully. This document treats it as the current **live-validated checkpoint**. Exact GitHub Release/tag publication metadata should still be reconciled directly from GitHub before making claims about release-page/tag chronology.

Detailed live evidence:

```text
docs/livingworld/VALIDATION_0.1.10.md
```

Human-readable history:

```text
docs/CHANGELOG.md
```

Post-`0.1.8` Memory 2.0 implementation now on `1.21.1`:

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
<world>/livingworld/memory.json         legacy rolling player↔NPC dialogue history
<world>/livingworld/memory2.json        Memory 2.0 event store
<world>/livingworld/events.json         server-owned factual world events
<world>/livingworld/relationships.json  player↔NPC relationship state
<world>/livingworld/voices.json         persistent NPC voice identity
```

These files belong with world backup/restore procedures.

`memory.json` remains active and has **not** been migrated or removed. Memory 2.0 is additive while its layered architecture is still under development.

---

# Memory 2.0 — implemented foundation

Primary storage:

```text
<world>/livingworld/memory2.json
```

## PR #31 — persistent MemoryEvent domain

Merge:

```text
7741e86ad0ab4e2fd2315f9e6b81a15bffeca4b8
```

Core fields:

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

`MemoryEventStore` provides per-NPC isolation, bounded retention, idempotent event UUIDs, atomic persistence, deterministic access and fail-open recovery.

## PR #33 — deterministic bounded retrieval

Merge:

```text
667aeb7931e0fec2ea516f48560ad04537686f26
```

Current deterministic ranking:

```text
relevance  40%
importance 25%
recency    20%
confidence 15%
```

Hard candidate/result bounds and deterministic tie-breaking are enforced. No embeddings, vector DB or provider/LLM ranking is required.

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

Source `WorldEvent.id` is reused for MemoryEvent idempotency. Memory 2.0 failure cannot roll back the already-valid action/factual event.

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

Memory 2.0 contributes a bounded set to snapshot-aware NPC turns:

```text
candidateLimit = 32
maxResults = 6
recencyHorizonTicks = 168000
maxSummaryChars = 240 Unicode code points
participant relevance = current player UUID
```

Truth/prompt boundary:

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

Successful usable dialogue produces bounded NPC-owned `DIALOGUE` events:

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

The server remembers the **actual persisted relationship transition**, not the raw LLM proposal:

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

A live `0.1.9`-era test exposed this asymmetry:

```text
voice/snapshot turn → legacy memory.json + Memory 2.0 DIALOGUE
ordinary text turn  → legacy memory.json only
```

PR #43 unified only the post-success ingestion boundary:

```text
classic OpenAI text ─┐
                      ├→ Memory2DialogueLifecycle.recordSuccessful(...)
snapshot/voice ──────┘
                      → Memory2DialogueIngestor
                      → bounded/idempotent DIALOGUE event
```

Classic text was **not** rerouted through snapshot prompt/provider semantics. Existing text provider/prompt/tools/relationship behavior remains unchanged; only Memory 2.0 dialogue ingestion is shared.

Exact PR #43 verification:

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

Final automated gates covered unit tests, Fabric build, distributable Fabric package verification, NeoForge build and Fabric compatibility build.

---

# 0.1.10+1.21.1 live-validation checkpoint — PASS

The intended parity/restart scenario has now been executed successfully on a real server.

Test sequence:

```text
Text → NPC A
Voice → NPC A
Text → NPC B
inspect persistent memory
restart server
return to NPC A
verify identity / persistence / duplicate safety / service health
```

Observed:

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

Interpretation:

- the text/voice Memory 2.0 ingestion parity regression is closed for the tested scenario;
- Memory 2.0 NPC ownership remained isolated across NPC A/NPC B in the test;
- no duplicate event IDs were observed;
- legacy `memory.json` and `memory2.json` survived restart;
- returning to NPC A after restart resolved the expected NPC correctly;
- voice/STT/TTS behavior showed no regression.

Canonical detailed evidence:

```text
docs/livingworld/VALIDATION_0.1.10.md
```

Do **not** infer from this single checkpoint that every long-duration multiplayer, provider-failure, backup/restore or long-horizon recall scenario has been proven. Those remain separate validation concerns.

---

# Release/checkpoint state

Current development checkpoint:

```text
0.1.10+1.21.1
live validation: PASS
purpose: text + voice Memory 2.0 parity checkpoint
```

The user reports this release successfully passed the stated scenario.

Historical confirmed release anchor retained from earlier state:

```text
0.1.8+1.21.1
commit 23fba1ee373e932c0b17aba3755f8ac478c26941
workflow 29918008438 — SUCCESS
```

Before stating exact GitHub Release/tag chronology in external release notes, reconcile the actual GitHub Releases/tags page directly. Do not move an already-published tag.

---

# Roadmap status

## 0.1.x Reliability

Foundation is implemented and the `0.1.10` parity checkpoint passed its intended live test.

Continue reliability work only for concrete reproduced defects or explicit broader soak/backup/provider validation goals.

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
+ live text/voice/restart parity validation checkpoint
```

Still not implemented:

- dedicated Working Memory orchestration beyond existing bounded dialogue/context;
- explicit Semantic Facts/Beliefs layer;
- deterministic duplicate handling / consolidation policy beyond exact UUID idempotency;
- forgetting/decay mutation;
- migration from legacy `memory.json`;
- NPC-to-NPC memory/rumor propagation;
- trustworthy causal relationship reasons when/if a provenance source exists;
- final exit criterion: important events recalled days later without full raw chat history.

### Next recommended development slice

Proceed with **Working Memory orchestration + explicit Semantic Facts/Beliefs boundaries**.

Target architecture split:

```text
Working Memory
→ intentionally small immediate conversational/task context
→ short-lived and bounded

Episodic Memory
→ existing MemoryEvent experiences
→ DIALOGUE / ACTION / RELATIONSHIP_CHANGE / OBSERVATION

Semantic Facts / Beliefs
→ typed knowledge model
→ explicit provenance + confidence
→ SYSTEM_OBSERVED facts physically/logically distinct from
  PLAYER_TOLD / NPC_TOLD / INFERRED beliefs
```

First-slice constraints:

- do not introduce embeddings/vector DB as a prerequisite;
- do not use LLM summarization to silently upgrade beliefs into facts;
- keep persistent truth/provenance explicit and inspectable;
- keep Working Memory bounded and separate from durable episodic/semantic stores;
- keep migration from legacy `memory.json` deferred until the new layers are stable.

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
Treat 0.1.10+1.21.1 as the validated Memory 2.0 ingestion checkpoint.
Fix only concrete blocking regressions if new evidence appears.

Priority B
Continue 0.2 with Working Memory orchestration + explicit Semantic Facts/Beliefs design.

Priority C
Implement deterministic duplicate/consolidation policy without silently changing provenance.

Priority D
Implement forgetting/decay, then design migration from legacy memory.json.
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
