# VillAIgence Project State

> **Canonical current-state handoff.** Read this file before `docs/ROADMAP.md` when resuming work in a new session.
>
> Last major state update: **2026-07-22**.
>
> This file records implemented repository state, current release anchors, architectural boundaries and the first unimplemented development priority. Always reconcile it with recent PRs/releases/CI before active work.

## Executive snapshot

VillAIgence is a Minecraft 1.21.1 MCA-derived mod evolving from AI-assisted villager dialogue into a **persistent living-society simulation layer**.

Current anchors:

```text
base branch: 1.21.1
current implemented HEAD after PR #35:
7ed77c5e2fae9f544021ea798dc2a9e9174792a4

latest published release:
0.1.8+1.21.1

release commit:
23fba1ee373e932c0b17aba3755f8ac478c26941

release workflow:
29918008438 — SUCCESS
```

`0.1.8+1.21.1` contains the completed `0.1.x` reliability foundation through PR #30.

The following Memory 2.0 work is newer than that release and currently exists on `1.21.1` only:

```text
PR #31  persistent MemoryEvent foundation
PR #33  deterministic bounded retrieval/ranking
PR #35  authoritative safe-action WorldEvent ingestion
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
- **NeoForge:** compile compatibility remains required in PR CI
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
2. Mutable world/entity state is captured on the Minecraft server thread before asynchronous AI work.
3. LLM may propose dialogue/intents/structured deltas; server policy validates and executes mutations.
4. Provider/model changes must not redefine persistent NPC identity.
5. External AI and auxiliary persistence failures fail soft whenever safe.
6. API credentials remain server-side.
7. Persistent formats are explicit, inspectable and backed up with the world.
8. Retry/replay paths must not duplicate persistent or gameplay side effects.
9. Claims/beliefs remain claims/beliefs unless server-owned evidence makes them factual.
10. Autonomous AI must eventually be event-driven and budgeted rather than “LLM every tick.”

---

# Implemented reliability and AI foundation

## Server-side provider layer

Implemented:

- OpenAI-compatible/OpenRouter chat provider support;
- server-owned configuration and credentials;
- bounded connect/read timeouts;
- controlled provider failures;
- no direct LLM authority over Minecraft state.

## Voice input

```text
player microphone
→ Simple Voice Chat
→ decoded PCM
→ STT
→ targeted NPC
→ AI
→ text reply
```

Implemented independent `voiceInputEnabled` and `voiceOutputEnabled`, OpenAI-compatible/OpenRouter STT, and separation from normal player-to-player voice traffic.

Raw microphone audio is not intentionally persisted.

## TTS and spatial NPC voice

Implemented:

- optional NPC TTS through Simple Voice Chat;
- `ttsResponseFormat=auto|wav|pcm`;
- OpenRouter raw PCM support;
- WAV compatibility;
- PCM16 LE mono decode and 48 kHz resampling;
- text publication before/independent of TTS success or backpressure.

## Persistent NPC voice identity

Storage:

```text
<world>/livingworld/voices.json
```

Identity is derived from NPC UUID + MCA gender + MCA age bucket and is independent from chat provider/model.

Mood-aware delivery changes style, not persistent voice identity.

## Safe actions and server authority

```text
LLM proposes
→ whitelist/policy validates
→ server revalidates current state
→ server executes allowed mutation
```

Arbitrary console/OP authority is not granted to the LLM.

## Immutable authoritative context snapshot

Async AI uses immutable server-thread-captured context rather than freely reading mutable Minecraft state.

Current snapshot includes stable identity/time/world-root boundaries needed for future Memory 2.0 retrieval integration.

## Structured response/provider hardening

Implemented across PRs #22/#24/#26:

- visible reply isolated from optional structured metadata;
- malformed relationship/action metadata cannot leak raw JSON;
- OpenRouter/OpenAI-compatible `content:null` handled safely;
- reasoning is never substituted as visible/spoken NPC text;
- bounded empty-completion retry;
- retry occurs before persistent/gameplay side effects.

PR #26 merge anchor:

```text
52eed8ba8dede8deeaceffbec723255d4515ac8d
```

## Operator diagnostics

PR #28 added:

```text
/villaigence ai status
```

Merge anchor:

```text
90b32ee1125ad451d2fe9f7242ee903e8680a131
```

Diagnostics are read-only and intentionally exclude credentials, prompts, transcripts, answers, TTS input, reasoning and raw provider payloads.

## Non-blocking admission/backpressure

PR #29 merge anchor:

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

Controlled local rejection types:

```text
admission_saturated
admission_player_cooldown
admission_provider_cooldown
```

No blocking server-thread wait or unbounded provider queue.

## Persistent auxiliary JSON recovery

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

Malformed/unreadable auxiliary JSON fails open; a later successful normal mutation may rewrite valid current-format data.

This is availability recovery, not data reconstruction.

Live reliability checklist:

```text
docs/livingworld/PLAYTEST_CHECKLIST.md
```

---

# Existing pre-Memory-2 persistence

## Bounded player↔NPC dialogue history

```text
<world>/livingworld/memory.json
```

This remains the proven `0.1.x` dialogue-history path and has not yet been migrated into Memory 2.0.

## Authoritative factual world events

```text
<world>/livingworld/events.json
```

Implemented server-owned factual events with provenance/time/location/dimension metadata and bounded context queries.

Player/LLM claims cannot directly become authoritative `WorldEvent`s.

## Player↔NPC relationship state

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

Implemented bounded server-controlled state and small structured per-turn deltas.

General persistent `NPC ↔ NPC` social graph remains roadmap `0.3`.

---

# Memory 2.0 — implemented foundation

## PR #31 — persistent MemoryEvent domain

Merge commit:

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
- `PLAYER_TOLD`, `NPC_TOLD`, and `INFERRED` remain claims/beliefs;
- persistence never upgrades a claim into authoritative world truth.

`MemoryEventStore` provides:

- per-NPC isolation;
- bounded retention;
- idempotent event UUIDs;
- deterministic newest-first access;
- atomic temp+replace writes;
- fail-open malformed-file recovery.

## PR #33 — deterministic bounded retrieval

Merge commit:

```text
667aeb7931e0fec2ea516f48560ad04537686f26
```

Implemented:

- immutable `MemoryQuery`;
- hard `candidateLimit` bound `1..512`;
- hard `maxResults <= candidateLimit`;
- participant/type relevance;
- deterministic game-time recency;
- importance/confidence scoring;
- inspectable `RankedMemory` components;
- stable deterministic tie-breaking;
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

Important non-goals still preserved:

- no embeddings/vector DB;
- no provider/LLM ranking;
- no mutable Minecraft access;
- no prompt integration yet.

## PR #35 — authoritative safe-action ingestion

Merge commit:

```text
7ed77c5e2fae9f544021ea798dc2a9e9174792a4
```

This is the first production source feeding Memory 2.0.

Lifecycle:

```text
whitelisted NPC action succeeds
→ WorldEventRecorder creates SYSTEM_OBSERVED WorldEvent
→ events.json persistence succeeds
→ same source event maps to actor-owned ACTION MemoryEvent
→ memory2.json append
```

Guarantees:

- Memory 2.0 ingestion never precedes authoritative source persistence;
- secondary memory failure cannot roll back a successful gameplay action or factual event;
- source `WorldEvent.id` is reused as `MemoryEvent.id`, making redelivery idempotent;
- only the acting NPC owns this memory in the current slice;
- nearby NPC propagation is not implied;
- no LLM decides event truth or importance.

Mapping defaults for authoritative NPC actions:

```text
type = ACTION
provenance = SYSTEM_OBSERVED
importance = 60
emotionalWeight = 0
confidence = 100
```

Configuration added without version bump:

```text
memory2Enabled = true
memory2MaxEventsPerNpc = 256
normalized max range = 1..512
config version remains 2
```

Existing version-2 configs require no migration.

### Relationship reasons deliberately not ingested yet

Current relationship flow has numeric LLM-proposed deltas but **no separately server-validated reason**.

VillAIgence therefore does not invent a relationship reason or promote an LLM explanation to authoritative memory.

A later dedicated provenance contract is required before `RELATIONSHIP_CHANGE.relationshipReasons` becomes production data.

Documentation:

```text
docs/livingworld/MEMORY_2.md
docs/livingworld/CONFIGURATION.md
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

The release includes reliability work through PR #30.

Memory 2.0 PRs #31/#33/#35 are post-release development and are not yet in a published artifact.

Never move an already-published release tag.

---

# CI / quality evidence

Required merge gates:

```text
VillAIgence CI
→ :common:test
→ Fabric build
→ distributable Fabric package verification

Java Pull Request CI with Gradle
→ NeoForge build
→ Fabric build
```

PR #35 TDD evidence:

```text
RED tests-only head:
6f0769053cb50fb5c79207d4dd2110ce9c825044
VillAIgence CI 29921694337 → expected FAILURE at :common:compileTestJava

final head:
75a2c0efc45d50e62fd241dd04718258e8bbd633
VillAIgence CI 29922482649 → SUCCESS
Java Pull Request CI 29922482470 → SUCCESS
```

Final lifecycle review also caught and corrected an intermediate boundary regression before merge: `WorldEvent` capture/persistence was restored inside its original protected `try` before secondary Memory 2.0 ingestion.

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

Any concrete release-blocking defect should be fixed as a narrow reliability patch rather than mixed into Memory 2.0.

## 0.2 Memory 2.0 — active, partial

Implemented:

```text
MemoryEvent domain
+ explicit provenance/confidence/importance/emotional metadata
+ bounded per-NPC memory2.json persistence
+ idempotent event IDs
+ deterministic bounded retrieval/ranking
+ first authoritative production ingestion path from successful safe actions
```

Still not implemented:

- bounded ranked Memory 2.0 injection into NPC context/prompts;
- dialogue-to-episodic-memory extraction;
- working-memory orchestration;
- semantic facts/beliefs layer;
- validated relationship-reason provenance/ingestion;
- consolidation/summarization;
- forgetting/decay;
- semantic duplicate merging;
- migration from existing `memory.json`;
- NPC-to-NPC memory/rumor propagation;
- exit criterion: important event recall days later without full raw chat history.

### Next recommended development slice

Build **bounded Memory 2.0 context integration** through the existing immutable snapshot boundary.

Recommended contract:

```text
LivingWorldContextSnapshot
(villagerId, playerId, gameTime, worldRoot)
→ build bounded MemoryQuery
→ MemoryRetriever
→ small ranked result set
→ provenance-preserving deterministic formatter
→ append labeled memory/belief lines to AI context
```

Requirements:

- hard candidate/result bounds;
- no mutable entity/world reads off-thread;
- explicit provenance labels preserved in prompt/context;
- `SYSTEM_OBSERVED` facts distinguishable from told/inferred beliefs;
- no raw JSON/schema dump into prompt;
- Memory 2.0 I/O/retrieval failure must fail soft and not remove existing AI context;
- do not replace/migrate legacy `memory.json` in this slice;
- no LLM summarization required to retrieve/format memories.

After bounded context integration is stable, next slices can address dialogue extraction, relationship-reason provenance, consolidation, forgetting/decay and migration separately.

## 0.3 Personality + NPC↔NPC social graph

Not implemented yet.

This should follow Memory 2.0 closely because later rumors, autonomous behavior and settlements depend on durable identities and social history.

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
Fix only concrete blocking reliability regressions found in use.

Priority B
Continue 0.2 with bounded provenance-preserving Memory 2.0 context integration.

Priority C
Then define controlled dialogue extraction and/or validated relationship-reason provenance as separate slices.

Priority D
Later add consolidation, forgetting/decay and migration after retrieval/context boundaries are proven.
```

Do **not** jump directly to embeddings or LLM summarization before bounded context integration and provenance handling are tested.

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
