# VillAIgence Project State

> **Canonical current-state handoff.** Read this file before `docs/ROADMAP.md` when resuming work in a new session.
>
> Last major state update: **2026-07-22**.
>
> This file describes implemented repository state, not aspirational roadmap items. Always reconcile it with recent PRs, releases/tags and CI before active development.

## Executive snapshot

VillAIgence is a Minecraft 1.21.1 MCA-derived mod that is evolving from AI-assisted villager dialogue into a **persistent living-society simulation layer**.

Current anchors:

```text
base branch: 1.21.1
current implemented HEAD after PR #31:
7741e86ad0ab4e2fd2315f9e6b81a15bffeca4b8

latest published release:
0.1.8+1.21.1

release commit:
23fba1ee373e932c0b17aba3755f8ac478c26941

release workflow run:
29918008438 — SUCCESS
```

`0.1.8+1.21.1` contains the completed `0.1.x` reliability foundation through PR #30.

PR #31 (`Memory 2.0 persistent event foundation`) was merged **after** that release, so the new `memory2.json` foundation is implemented on `1.21.1` but is not part of the already-published `0.1.8` artifact.

---

# Project identity and compatibility

- **Public name:** VillAIgence
- **Short name:** VAI
- **Tagline:** `Giving villagers a mind of their own.`
- **Repository:** `True-Ruslan/villAIgence`
- **Primary branch:** `1.21.1`
- **Minecraft:** 1.21.1
- **Primary loader/release target:** Fabric
- **Compatibility build:** NeoForge remains compiling in PR CI where applicable
- **Java:** 21

Compatibility-sensitive identifiers intentionally remain:

```text
mod id: mca
Java package root: net.conczin.mca
config: config/livingworld.json
world data root: <world>/livingworld/
internal engine/data naming: LivingWorld / livingworld
```

Do not casually rename these without a dedicated migration design.

---

# Product direction

VillAIgence is not treated as only “MCA Reborn with AI chat.”

Target system:

```text
NPC identity
+ personality
+ layered memory
+ relationships
+ knowledge and beliefs
+ voice
+ goals
+ autonomous actions
+ settlements
+ factions
+ emergent history
= persistent living society
```

Canonical milestone order:

```text
0.1.x  Reliability / provider hardening
        ↓
0.2    Memory 2.0
        ↓
0.3    Personality + NPC↔NPC social graph
        ↓
0.4    Knowledge + rumors
        ↓
0.5    Autonomous NPC agents
        ↓
0.6    Settlement simulation
        ↓
0.7    Factions + politics
        ↓
0.8    Emergent stories
        ↓
0.9    Performance + local LLM + large servers
        ↓
1.0    Persistent living society
```

See `docs/ROADMAP.md` for full milestone scope and exit criteria.

---

# Architecture laws

These are project-wide constraints, not optional implementation preferences.

1. **LLM is never authoritative.** Minecraft/server-owned state is truth.
2. Mutable entity/world state is captured on the Minecraft server thread before asynchronous AI work.
3. LLM may propose dialogue/intents/structured deltas; server policy validates and executes mutations.
4. Provider/model changes must not redefine persistent NPC identity.
5. External AI failures must fail soft rather than crash gameplay or corrupt state.
6. API credentials remain server-side.
7. Persistent formats are explicit, inspectable and backed up with the world.
8. Retry/replay paths must not duplicate persistent or gameplay side effects.
9. Simulation systems come before spectacle; autonomous LLM usage must eventually be event-driven and budgeted rather than “LLM every tick.”
10. Claims/beliefs are not automatically authoritative facts merely because an LLM or player said them.

---

# Implemented systems

## 1. Server-side OpenAI-compatible/OpenRouter AI foundation

Implemented:

- server-owned provider configuration;
- server-side API credentials;
- OpenAI-compatible/OpenRouter chat routing;
- bounded timeouts and controlled provider failures;
- LLM output separated from authoritative Minecraft mutation.

## 2. Voice input pipeline

```text
player microphone
→ Simple Voice Chat
→ decoded PCM
→ STT
→ targeted NPC
→ AI
→ text reply
```

Implemented:

- optional microphone-driven conversation;
- independent `voiceInputEnabled` and `voiceOutputEnabled`;
- OpenAI-compatible/OpenRouter STT;
- player-to-player voice remains separate;
- raw microphone audio is not intentionally persisted.

## 3. TTS and spatial NPC voice

Implemented:

- optional NPC speech through Simple Voice Chat;
- `ttsResponseFormat=auto|wav|pcm`;
- OpenRouter raw PCM support;
- PCM16 LE mono decode;
- WAV compatibility path;
- sample-rate handling and 48 kHz resampling;
- text published independently of TTS success/failure/backpressure.

A TTS problem must never remove a valid text answer.

## 4. Persistent per-NPC voice identity

Storage:

```text
<world>/livingworld/voices.json
```

Identity inputs:

```text
NPC UUID + MCA gender + MCA age bucket
→ deterministic compatible voice
→ persisted profile
```

Voice identity is independent from the selected chat model/provider.

Mood-aware delivery foundation supports:

```text
NEUTRAL
HAPPY
SAD
ANGRY
AFRAID
TIRED
```

Mood changes delivery, not persistent base identity.

## 5. Existing persistent player↔NPC dialogue memory

Storage:

```text
<world>/livingworld/memory.json
```

This remains the proven `0.1.x` bounded dialogue-history path.

It is **not** Memory 2.0 and remains intentionally untouched by the first Memory 2.0 foundation PR.

## 6. Factual world-event knowledge foundation

Storage:

```text
<world>/livingworld/events.json
```

Implemented:

- server-owned factual events;
- provenance/time/location/dimension metadata;
- bounded local queries;
- injection into immutable authoritative context snapshots;
- LLM/player claims cannot directly become authoritative world events.

This is a factual-event substrate, not yet a full belief/rumor network.

## 7. Player↔NPC relationship state

Storage:

```text
<world>/livingworld/relationships.json
```

Dimensions:

```text
trust
respect
fear
affinity
```

Implemented:

- bounded state;
- structured per-turn deltas;
- server-side clamp/policy;
- relationship context influences behavior/action eligibility;
- malformed metadata cannot leak raw structured JSON into dialogue.

General persistent `NPC ↔ NPC` social graph is **not** implemented yet; that is roadmap `0.3`.

## 8. Safe actions and server authority

Action boundary:

```text
LLM proposes
→ whitelist/policy validates
→ server revalidates current entity/player/world state
→ server executes allowed mutation
```

Arbitrary command/console authority is not granted to the LLM.

Related server-authority hardening also exists for MCA blueprint/village mutation paths.

## 9. Immutable authoritative context snapshots

Before asynchronous AI work, VillAIgence captures immutable server-thread context for relevant:

- player/NPC identity;
- world facts;
- location/environment;
- available safe actions;
- relationship state;
- selected relevant memory/event context.

Async provider code should not freely read mutable Minecraft entity/world state.

## 10. Structured-response and provider-envelope hardening

Implemented across PRs #22, #24 and #26:

- visible NPC message isolated from structured metadata;
- malformed relationship/action metadata cannot expose raw JSON;
- safe recovery of valid top-level `message` only;
- OpenRouter/OpenAI-compatible `content:null` handled safely;
- `finish_reason`, provider error type and generation metadata captured safely;
- reasoning content is never substituted as NPC-visible/spoken text;
- bounded empty-completion retry: initial request + at most one retry;
- terminal errors/reasons are not blindly retried;
- retry remains before persistent/game-side effects.

PR #26 reliability merge anchor:

```text
52eed8ba8dede8deeaceffbec723255d4515ac8d
```

## 11. Operator diagnostics

PR #28 added:

```text
/villaigence ai status
```

Merge anchor:

```text
90b32ee1125ad451d2fe9f7242ee903e8680a131
```

Status is read-only and exposes safe process-local metadata for Chat/STT/TTS without provider probes/token spend.

It excludes credentials, Authorization headers, prompts, transcripts, NPC answers, TTS input, reasoning content and raw provider bodies.

Documentation: `docs/livingworld/DIAGNOSTICS.md`.

## 12. Non-blocking AI admission/backpressure

PR #29 implemented bounded independent admission for Chat/STT/TTS.

Release-anchor merge commit:

```text
8f3095c6e8489e077246d652be51ec3c0ff57cd8
```

Defaults:

```text
aiChatMaxConcurrentRequests = 4
aiSttMaxConcurrentRequests = 2
aiTtsMaxConcurrentRequests = 2
aiPerPlayerCooldownMillis = 750
aiProviderRateLimitCooldownMillis = 5000
```

Controlled local rejections:

```text
admission_saturated
admission_player_cooldown
admission_provider_cooldown
```

No blocking server-thread wait or unbounded provider queue is introduced.

## 13. Persistent auxiliary JSON corruption recovery

PR #30 merge commit / `0.1.8` release commit:

```text
23fba1ee373e932c0b17aba3755f8ac478c26941
```

Covered stores:

```text
memory.json
events.json
relationships.json
voices.json
```

Malformed/unreadable auxiliary JSON fails open to an empty/neutral/re-resolved state; the next normal mutation can rewrite valid current-format JSON through existing atomic temp + replace behavior.

This is availability recovery, not data reconstruction. Backups remain authoritative for recovering old data.

Operational reliability checklist:

```text
docs/livingworld/PLAYTEST_CHECKLIST.md
```

## 14. Memory 2.0 persistent event foundation — implemented

PR #31 merge commit:

```text
7741e86ad0ab4e2fd2315f9e6b81a15bffeca4b8
```

New storage:

```text
<world>/livingworld/memory2.json
```

New immutable provider-independent `MemoryEvent` fields:

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

Initial types:

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

Important truth boundary:

- `SYSTEM_OBSERVED` is server-verified evidence;
- told/inferred entries remain claims/beliefs;
- persistence does not automatically make a claim authoritative world truth.

`MemoryEventStore` currently provides:

- per-NPC isolation;
- bounded retention;
- deterministic newest-first retrieval;
- idempotent append by event UUID;
- atomic temp + replace persistence;
- fail-open malformed-file recovery.

The existing `memory.json` dialogue path is unchanged.

Documentation:

```text
docs/livingworld/MEMORY_2.md
```

This means roadmap `0.2 Memory 2.0` has **started**, but the milestone is far from complete.

---

# Release state

Latest published release:

```text
0.1.8+1.21.1
```

Exact release source:

```text
commit: 23fba1ee373e932c0b17aba3755f8ac478c26941
workflow run: 29918008438
workflow conclusion: SUCCESS
artifact branch/tag: 0.1.8+1.21.1
```

Release workflow verified:

- tests and Fabric build;
- distributable JAR package/smoke-check;
- artifact upload;
- digest verification;
- GitHub Release publication.

`0.1.8` includes persistence hardening through PR #30.

PR #31 / Memory 2.0 foundation is newer than `0.1.8` and is currently implemented only on `1.21.1` until a later release is intentionally cut.

Never move an already-published release tag.

---

# CI and quality gates

Required checks before feature/bugfix merge:

```text
VillAIgence CI
→ :common:test
→ Fabric build
→ distributable Fabric package smoke-check

Java Pull Request CI with Gradle
→ Fabric compatibility build
→ NeoForge compatibility build
```

Before claiming completion:

1. verify exact final PR head;
2. require fresh green CI on that head;
3. review final diff/lifecycle boundaries;
4. confirm no unresolved critical/important review issue;
5. merge only after evidence exists.

Bugfixes should use RED → GREEN TDD where practical.

---

# Remaining roadmap work

## 0.1.x live validation

Core reliability architecture is implemented and released in `0.1.8`.

Remaining work is field evidence, not another large reliability subsystem:

- repeated voice-dialogue soak;
- multiplayer concurrency/admission behavior;
- restart/reconnect persistence;
- world backup/restore;
- provider 429/cooldown behavior where safely reproducible;
- real operator diagnostics feedback.

Use:

```text
docs/livingworld/PLAYTEST_CHECKLIST.md
```

Any release-blocking defect found here should be fixed as a narrow reliability patch, not mixed into Memory 2.0 architecture.

## 0.2 Memory 2.0 — started, partial

Implemented so far:

- immutable `MemoryEvent` domain;
- explicit provenance/confidence/importance/emotional metadata;
- separate versioned `memory2.json` store;
- bounded per-NPC persistence;
- idempotent event IDs;
- fail-open recovery.

Still not implemented:

- automatic controlled conversion of dialogue/events/relationship reasons into Memory 2.0;
- bounded relevance ranking beyond newest-first persistence access;
- recency/importance/confidence retrieval scoring;
- working-memory orchestration;
- semantic memory/facts;
- consolidation/summarization;
- forgetting/decay;
- duplicate semantic memory merging;
- migration from existing `memory.json`;
- prompt/context integration of retrieved Memory 2.0;
- exit criterion: reliable recall of important events days later without full raw chat history.

### Next recommended development slice

Build a deterministic **bounded retrieval/ranking layer** above `MemoryEventStore`.

Recommended sequence:

```text
1. MemoryQuery / MemoryRetriever
   → hard per-NPC bounds
   → relevance signal
   → recency signal
   → importance signal
   → confidence signal
   → deterministic score/tie-breaking

2. Controlled server-owned adapters
   → authoritative WorldEvent → SYSTEM_OBSERVED MemoryEvent
   → explicit relationship reason → RELATIONSHIP_CHANGE MemoryEvent

3. Only then
   → prompt/context injection of retrieved bounded memories

4. Later
   → dialogue extraction
   → consolidation/summarization
   → forgetting/decay
   → migration
```

Do **not** start with LLM summarization or embeddings before deterministic bounded retrieval and truth/provenance adapters are tested.

## 0.3 Personality + NPC↔NPC social graph

Not implemented yet.

Planned after Memory 2.0 foundation is sufficiently mature:

- stable values/goals/fears/likes/dislikes;
- persistent NPC↔NPC friendship/trust/respect/fear;
- rivalry/grudges/history;
- family/romance integration into a unified social graph.

## Later milestones

Not yet implemented as full systems:

- `0.4` knowledge/rumor propagation;
- `0.5` autonomous agent loop;
- `0.6` settlement simulation;
- `0.7` factions/politics;
- `0.8` emergent story system;
- `0.9` scale/performance/local LLM architecture;
- `1.0` persistent living society.

---

# Immediate priorities

```text
Priority A
Continue live validation of 0.1.8 reliability using PLAYTEST_CHECKLIST.md.
Fix only concrete blocking regressions found in real use.

Priority B
Continue 0.2 Memory 2.0 with deterministic bounded retrieval/ranking.
Do not couple persistence directly to an LLM.

Priority C
Add controlled adapters from authoritative world events and explicit relationship reasons into Memory 2.0.

Priority D
After bounded retrieval is stable, inject only selected memories into NPC context and begin consolidation/migration work.
```

---

# New-session handoff protocol

Preferred resume prompt:

> **Open `docs/PROJECT_STATE.md` and `docs/ROADMAP.md` in `True-Ruslan/villAIgence`. Check recent PRs/releases/CI, then tell me how VillAIgence development is going, what is complete, what changed since the state file, and what we should build next.**

A new session must:

1. read this file;
2. read `docs/ROADMAP.md`;
3. inspect current `1.21.1` HEAD;
4. inspect recent merged/open PRs;
5. inspect latest tags/releases and CI;
6. reconcile any discrepancy;
7. continue from the first unimplemented priority rather than rebuilding completed work;
8. update this file after material progress.

---

# Maintenance rule

Any PR/release materially changing one of these must update this file in the same PR or immediately after merge:

- current release version/state;
- completed roadmap milestone/subsystem;
- persistent storage/schema;
- architecture/truth boundary;
- provider behavior;
- compatibility requirement;
- next immediate priority.

`docs/ROADMAP.md` answers **“Where are we going?”**

`docs/PROJECT_STATE.md` answers **“Where are we now, and what should happen next?”**
