# VillAIgence Project State

> **Canonical current-state handoff.** Read this file before `docs/ROADMAP.md` when resuming work in a new session.
>
> Last major state update: **2026-07-22**.
>
> This file records implemented repository state. Always reconcile it with recent PRs, releases/tags and CI before active development.

## Executive snapshot

VillAIgence is a Minecraft 1.21.1 MCA-derived mod evolving from AI-assisted villager dialogue into a **persistent living-society simulation layer**.

Current anchors:

```text
base branch: 1.21.1
current implemented HEAD after PR #33:
667aeb7931e0fec2ea516f48560ad04537686f26

latest published release:
0.1.8+1.21.1

release commit:
23fba1ee373e932c0b17aba3755f8ac478c26941

release workflow:
29918008438 — SUCCESS
```

`0.1.8+1.21.1` contains the completed `0.1.x` reliability foundation through PR #30.

PR #31 and PR #33 are newer than that release. Therefore the Memory 2.0 persistence and deterministic retrieval foundations are implemented on `1.21.1` but are **not** part of the already-published `0.1.8` artifact.

---

# Identity and compatibility

- **Public name:** VillAIgence
- **Short name:** VAI
- **Tagline:** `Giving villagers a mind of their own.`
- **Repository:** `True-Ruslan/villAIgence`
- **Primary branch:** `1.21.1`
- **Minecraft:** 1.21.1
- **Primary release target:** Fabric
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

Do not rename these without a dedicated migration design.

---

# Architecture laws

1. **LLM is never authoritative.** Minecraft/server-owned state is truth.
2. Mutable entity/world state is captured on the Minecraft server thread before asynchronous AI work.
3. LLM may propose dialogue/intents/structured deltas; server policy validates and executes mutations.
4. Provider/model changes must not redefine persistent NPC identity.
5. External AI failures fail soft rather than crash gameplay or corrupt state.
6. API credentials remain server-side.
7. Persistent formats are explicit, inspectable and backed up with the world.
8. Retry/replay paths must not duplicate persistent or gameplay side effects.
9. Claims/beliefs are not automatically facts merely because an LLM, player or NPC said them.
10. Autonomous AI must eventually be event-driven and budgeted rather than “LLM every tick.”

---

# Implemented systems

## 1. Server-side AI/provider foundation

Implemented:

- server-owned OpenAI-compatible/OpenRouter configuration;
- server-side credentials;
- bounded timeouts and controlled provider failures;
- LLM output separated from authoritative Minecraft mutation.

## 2. Voice input

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

- independent `voiceInputEnabled` / `voiceOutputEnabled`;
- OpenAI-compatible/OpenRouter STT;
- player-to-player voice remains separate;
- raw microphone audio is not intentionally persisted.

## 3. TTS and spatial NPC voice

Implemented:

- optional NPC speech through Simple Voice Chat;
- `ttsResponseFormat=auto|wav|pcm`;
- OpenRouter raw PCM and WAV compatibility paths;
- PCM16 LE mono decode and 48 kHz resampling;
- text publication independent of TTS success/failure/backpressure.

## 4. Persistent NPC voice identity

Storage:

```text
<world>/livingworld/voices.json
```

Identity:

```text
NPC UUID + MCA gender + MCA age bucket
→ deterministic compatible voice
→ persisted profile
```

Voice identity is independent from the chat model/provider. Mood-aware delivery changes style, not persistent base identity.

## 5. Existing bounded dialogue memory

Storage:

```text
<world>/livingworld/memory.json
```

This is the proven `0.1.x` player↔NPC dialogue-history path. It remains intentionally separate from Memory 2.0.

## 6. Factual world events

Storage:

```text
<world>/livingworld/events.json
```

Implemented:

- server-owned factual events;
- provenance/time/location/dimension metadata;
- bounded local queries;
- injection into authoritative immutable context snapshots;
- LLM/player claims cannot directly become authoritative events.

## 7. Player↔NPC relationships

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

Implemented bounded deltas, server-side clamp/policy and behavioral consequences. General persistent `NPC ↔ NPC` social graph remains roadmap `0.3`.

## 8. Safe actions and server authority

```text
LLM proposes
→ whitelist/policy validates
→ server revalidates current state
→ server executes allowed mutation
```

Arbitrary command/console authority is not granted to the LLM.

## 9. Immutable authoritative context snapshots

Before asynchronous AI work VillAIgence captures immutable server-thread context for relevant identity, world facts, environment, actions and relationships.

Async provider code should not freely read mutable Minecraft entities/world state.

## 10. Structured-response/provider-envelope hardening

Implemented across PRs #22, #24 and #26:

- visible reply isolated from structured metadata;
- malformed relationship/action JSON cannot leak into chat/TTS;
- OpenRouter/OpenAI-compatible `content:null` handled safely;
- reasoning never substituted as visible/spoken text;
- bounded empty-completion retry;
- retries occur before persistent/gameplay side effects.

PR #26 merge anchor:

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

Read-only process-local diagnostics exclude credentials, prompts, transcripts, NPC answers, TTS input, reasoning and raw provider bodies.

## 12. Non-blocking AI admission/backpressure

PR #29 merge commit:

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

No blocking server-thread wait or unbounded provider queue.

## 13. Persistent JSON corruption recovery

PR #30 merge/release commit:

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

Malformed auxiliary JSON fails open; the next normal mutation can rewrite valid current-format JSON through existing atomic temp+replace behavior.

Operational checklist:

```text
docs/livingworld/PLAYTEST_CHECKLIST.md
```

## 14. Memory 2.0 persistent event foundation

PR #31 merge commit:

```text
7741e86ad0ab4e2fd2315f9e6b81a15bffeca4b8
```

Storage:

```text
<world>/livingworld/memory2.json
```

`MemoryEvent` fields:

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

Truth boundary:

- `SYSTEM_OBSERVED` means server-verified evidence;
- told/inferred entries remain claims/beliefs;
- persistence never upgrades a claim into authoritative world truth.

`MemoryEventStore` provides:

- per-NPC isolation;
- bounded retention;
- idempotent event UUIDs;
- atomic persistence;
- fail-open recovery.

## 15. Memory 2.0 deterministic bounded retrieval

PR #33 merge commit:

```text
667aeb7931e0fec2ea516f48560ad04537686f26
```

Implemented:

- immutable `MemoryQuery`;
- hard `candidateLimit` bound `1..512`;
- hard `maxResults <= candidateLimit`;
- structured participant/type relevance;
- deterministic Minecraft game-time recency;
- importance and confidence signals;
- inspectable `RankedMemory` score components;
- stable deterministic tie-breaking;
- per-NPC isolation inherited through `MemoryEventStore` candidate reads;
- no mutation during retrieval.

Ranking policy:

```text
relevance  40%
importance 25%
recency    20%
confidence 15%
```

Total:

```text
(relevance*40 + importance*25 + recency*20 + confidence*15) / 100
```

Important boundary:

- no embeddings/vector DB;
- no provider/LLM ranking;
- no mutable Minecraft world/entity access;
- no prompt integration yet;
- ranking never changes provenance or truth status.

Documentation:

```text
docs/livingworld/MEMORY_2.md
```

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
```

`0.1.8` includes reliability work through PR #30.

PR #31 and PR #33 are post-release Memory 2.0 development and are not yet in a published artifact.

Never move an already-published release tag.

---

# CI and quality gates

Required before feature/bugfix merge:

```text
VillAIgence CI
→ :common:test
→ Fabric build
→ distributable Fabric package smoke-check

Java Pull Request CI with Gradle
→ NeoForge compatibility build
→ Fabric compatibility build
```

PR #33 TDD evidence:

```text
RED tests-only head:
7d2f220d771f3d510717b4e560357bbfc4586fdf
VillAIgence CI 29920125705 → expected FAILURE at compileTestJava
(missing MemoryQuery / RankedMemory / MemoryRetriever)

final head:
e7ab64d8e4b06c759022eb7f426909f248d2ae29
VillAIgence CI 29920781581 → SUCCESS
Java Pull Request CI 29920781699 → SUCCESS
```

---

# Roadmap status

## 0.1.x Reliability — implementation complete, live validation ongoing

Released in `0.1.8`.

Remaining field evidence:

- repeated voice-dialogue soak;
- multiplayer concurrency/admission behavior;
- restart/reconnect persistence;
- backup/restore;
- provider 429/cooldown behavior where safely reproducible;
- operator diagnostics feedback.

Any concrete release-blocking defect should be a narrow reliability patch, not mixed into Memory 2.0 architecture.

## 0.2 Memory 2.0 — started, partial

Implemented:

```text
MemoryEvent domain
+ provenance/confidence/importance/emotional metadata
+ memory2.json persistent per-NPC store
+ idempotent bounded persistence
+ deterministic bounded retrieval/ranking
```

Still not implemented:

- controlled conversion of authoritative world events into per-NPC MemoryEvents;
- explicit persistent relationship-reason event adapters;
- dialogue-to-episodic-memory extraction;
- working-memory orchestration;
- semantic facts/beliefs layer;
- prompt/context integration of ranked Memory 2.0;
- consolidation/summarization;
- forgetting/decay;
- semantic duplicate merging;
- migration from existing `memory.json`;
- exit criterion: important event recall days later without full raw chat history.

### Next recommended slice

Build **controlled server-owned ingestion adapters**:

```text
authoritative WorldEvent
→ deterministic/idempotent SYSTEM_OBSERVED MemoryEvent
→ explicit target NPC ownership

server-approved relationship change + reason
→ deterministic/idempotent RELATIONSHIP_CHANGE MemoryEvent
→ explicit target NPC ownership
```

Requirements:

- no LLM authority;
- preserve provenance;
- deterministic event IDs so retries cannot duplicate memories;
- hard per-NPC persistence bounds;
- no automatic prompt injection yet.

After these adapters are stable, the next slice should inject only bounded ranked memories into NPC context.

## 0.3 Personality + NPC↔NPC social graph

Not implemented yet.

Planned after Memory 2.0 becomes sufficiently mature because later rumors, autonomous behavior and settlements depend on durable identity/social state.

## Later milestones

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
Continue real-server validation of released 0.1.8 using PLAYTEST_CHECKLIST.md.
Fix only concrete blocking regressions found in use.

Priority B
Continue 0.2 with controlled server-owned MemoryEvent ingestion adapters.

Priority C
After ingestion is trustworthy, inject only bounded ranked memories into immutable NPC context.

Priority D
Then add dialogue extraction, consolidation, forgetting/decay and migration in separate slices.
```

Do **not** jump directly to LLM summarization/embeddings before authoritative ingestion and bounded context integration are tested.

---

# New-session handoff protocol

Preferred resume prompt:

> **Open `docs/PROJECT_STATE.md` and `docs/ROADMAP.md` in `True-Ruslan/villAIgence`. Check recent PRs/releases/CI, then tell me how VillAIgence development is going, what is complete, what changed since the state file, and what we should build next.**

A new session must:

1. read this file;
2. read `docs/ROADMAP.md`;
3. inspect current `1.21.1` HEAD;
4. inspect recent merged/open PRs;
5. inspect latest releases/tags and CI;
6. reconcile discrepancies;
7. continue from the first unimplemented priority rather than rebuilding completed work;
8. update this file after material progress.

---

# Maintenance rule

Any PR/release materially changing one of these must update this file in the same PR or immediately after merge:

- release state;
- completed roadmap subsystem;
- persistent storage/schema;
- architecture/truth boundary;
- provider behavior;
- compatibility requirement;
- next immediate priority.

`docs/ROADMAP.md` answers **“Where are we going?”**

`docs/PROJECT_STATE.md` answers **“Where are we now, and what should happen next?”**
