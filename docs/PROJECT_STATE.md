# VillAIgence Project State

> **Canonical current-state handoff.** Read this file before `docs/ROADMAP.md` when resuming work in a new session.
>
> Last major state update: **2026-07-22**.
>
> This file records implemented repository state, release anchors, architecture/truth boundaries, and the first unimplemented development priority. Always reconcile it with recent PRs, releases/tags, and CI before active development.

## Executive snapshot

VillAIgence is a Minecraft 1.21.1 MCA-derived mod evolving from AI-assisted villager dialogue into a **persistent living-society simulation layer**.

Current anchors:

```text
base branch: 1.21.1
current implemented HEAD after PR #39:
507554f8372259f168a44208e616478fb27cfeb3

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
PR #37  bounded provenance-preserving NPC context integration
PR #39  controlled successful dialogue → episodic-memory ingestion
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
2. Mutable world/entity state is captured on the Minecraft server thread before asynchronous AI work.
3. LLM may propose dialogue/intents/structured deltas; server policy validates and executes mutations.
4. Provider/model changes must not redefine persistent NPC identity.
5. External AI and auxiliary persistence failures fail soft whenever safe.
6. API credentials remain server-side.
7. Persistent formats are explicit, inspectable, and backed up with the world.
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

Identity derives from NPC UUID + MCA gender + MCA age bucket and is independent from provider/model.

Mood-aware delivery changes style, not persistent base identity.

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

The snapshot also carries a physically separate bounded `memoryContext` field for Memory 2.0 while retaining separate authoritative `worldFacts`.

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

Diagnostics are read-only and exclude credentials, prompts, transcripts, answers, TTS input, reasoning, and raw provider payloads.

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

This remains the proven `0.1.x` rolling dialogue-history path.

Memory 2.0 now additionally records bounded successful dialogue episodes, but legacy `memory.json` has **not** been migrated or removed.

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

# Memory 2.0 — active implementation

Primary storage:

```text
<world>/livingworld/memory2.json
```

## PR #31 — persistent MemoryEvent domain

Merge commit:

```text
7741e86ad0ab4e2fd2315f9e6b81a15bffeca4b8
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

`MemoryEventStore` provides per-NPC isolation, bounded retention, idempotent event UUIDs, atomic persistence, deterministic newest-first access, and fail-open recovery.

## PR #33 — deterministic bounded retrieval

Merge commit:

```text
667aeb7931e0fec2ea516f48560ad04537686f26
```

Implemented:

- immutable `MemoryQuery`;
- hard `candidateLimit` `1..512`;
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

No embeddings/vector DB, provider/LLM ranking, or mutable Minecraft access.

## PR #35 — authoritative safe-action ingestion

Merge commit:

```text
7ed77c5e2fae9f544021ea798dc2a9e9174792a4
```

First authoritative production source feeding Memory 2.0:

```text
whitelisted NPC action succeeds
→ SYSTEM_OBSERVED WorldEvent
→ events.json persistence succeeds
→ same source event maps to actor-owned ACTION MemoryEvent
→ memory2.json append
```

Guarantees:

- ingestion never precedes authoritative source persistence;
- secondary memory failure cannot roll back gameplay/factual event success;
- source `WorldEvent.id` is reused as `MemoryEvent.id` for idempotency;
- only the acting NPC owns this memory in the current slice;
- no LLM decides event truth or importance.

Configuration:

```text
memory2Enabled = true
memory2MaxEventsPerNpc = 256
normalized max range = 1..512
config version remains 2
```

Existing version-2 configs require no migration.

## PR #37 — bounded provenance-preserving context integration

Merge commit:

```text
bb1097fb26df5052b405642202e167e4afae3fee
```

Memory 2.0 contributes selected memories to real snapshot-aware NPC turns.

Turn retrieval policy:

```text
candidateLimit = 32
maxResults = 6
recencyHorizonTicks = 168000   // 7 Minecraft days
maxSummaryChars = 240 Unicode code points
participant relevance = current player UUID
```

Flow:

```text
server-thread LivingWorldContextCapture
→ worldRoot/villagerId/playerId/gameTime
→ Memory2ContextProvider
→ MemoryEventStore
→ MemoryQuery / MemoryRetriever
→ MemoryContextFormatter
→ immutable snapshot.memoryContext
→ bounded labeled memory section in existing non-authoritative context channel
```

Truth/prompt boundary:

- `SYSTEM_OBSERVED` renders as `VERIFIED`;
- `PLAYER_TOLD`, `NPC_TOLD`, `INFERRED` render as `BELIEF`;
- memory entries are data, never instructions;
- BELIEF entries may be false/incomplete;
- current authoritative `worldFacts` wins on conflict;
- Memory 2.0 entries are never inserted into `worldFacts`.

Prompt-safety formatting:

- collapse newline/control whitespace;
- escape quotes/backslashes;
- cap summary to 240 Unicode code points;
- do not dump raw MemoryEvent JSON/ranking internals;
- neutralize reserved historical prompt template markers only in the prompt copy:

```text
$player   → ＄player
$villager → ＄villager
```

Persisted `MemoryEvent.summary` is never mutated.

Memory context loading/retrieval/formatting is fail-soft; failure leaves existing personality, world facts, actions, relationships, and legacy dialogue history intact.

## PR #39 — controlled successful dialogue episodic ingestion

Merge commit:

```text
507554f8372259f168a44208e616478fb27cfeb3
```

Successful usable snapshot-aware OpenAI turns now create bounded NPC-owned `DIALOGUE` MemoryEvents.

Mapping:

```text
type = DIALOGUE
provenance = PLAYER_TOLD
participants = [villagerId, playerId]
importance = 40
emotionalWeight = 0
confidence = 60
relationshipReasons = []
```

`PLAYER_TOLD` is deliberately conservative: the server can verify that a conversation occurred, but the semantic content of the player's statement and the generated NPC reply remains belief/dialogue data rather than authoritative Minecraft truth.

Stored summary:

```text
Player said: <bounded player utterance> | NPC replied: <bounded NPC utterance>
```

Each utterance is whitespace/control normalized and independently capped to 240 Unicode code points. No LLM summarization or semantic fact extraction is used.

Deterministic event identity is derived from:

```text
memory2-dialogue-v1
villager UUID
player UUID
snapshot game time
full normalized player message
```

NPC reply and wall-clock time are excluded from identity, so replay/redelivery of the same turn maps to the same UUID even if provider wording differs.

Final lifecycle hook is intentionally outside the provider implementation:

```text
snapshot-aware ChatAI.answer(...)
→ OpenAIChatAI.answer(...snapshot) completes
→ original Optional<String> returned to ChatAI
→ present + nonblank result
→ fail-soft Memory2DialogueIngestor.recordIfEnabled(...)
→ original Optional returned unchanged
```

Important boundaries:

- `OpenAIChatAI` provider/parser/retry code was not changed;
- legacy `memory.json` behavior was not changed;
- command and relationship-delta behavior was not changed;
- no event is created for absent/blank results, provider failures, exhausted `content:null`/empty responses, disabled Memory 2.0, classic/non-snapshot paths, or Inworld fallback;
- Memory 2.0 persistence failure is logged and cannot remove/replace an already-produced visible reply.

### Relationship reasons deliberately not ingested yet

Current relationship flow has bounded numeric LLM-proposed deltas but no separately validated provenance-bearing reason.

VillAIgence therefore does not invent a relationship reason and does not promote an LLM explanation to authoritative memory.

A dedicated relationship-reason provenance contract is the next development slice.

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

Memory 2.0 PRs #31/#33/#35/#37/#39 are post-release development and are not yet in a published artifact.

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

## PR #37 context integration evidence

```text
valid main RED head:
bd1da9aa8bcd70a27f41388504a7ece1cb4518fb
VillAIgence CI 29923707297 → expected FAILURE for missing formatter/provider/snapshot contract

review-found template-safety RED head:
e74715f634b22cb9901587842241e42626a57c49
VillAIgence CI 29924889537 → expected FAILURE on reserved template-marker regression test

final head:
787e32abaa53024b4aac19f8f9871daa80213e65
VillAIgence CI 29925211476 → SUCCESS
Java Pull Request CI 29925211473 → SUCCESS
```

## PR #39 dialogue ingestion evidence

Initial tests-only run `29926837430` failed because Modrinth returned external HTTP 502 during Fabric dependency resolution; this was **not** counted as TDD RED evidence.

Valid RED:

```text
head:
4aea2db256ee14d0761526efdcbd5b1ae101dd25
VillAIgence CI 29927032710
→ expected FAILURE at :common:compileTestJava
→ missing DialogueMemoryAdapter / Memory2DialogueIngestor
```

Lifecycle-guard RED:

```text
head:
edb745314c97909a201113885eb3722125fed252
VillAIgence CI 29927844441
→ expected FAILURE at :common:compileTestJava
→ missing recordIfEnabled(...)
```

Exact final head before merge:

```text
0a0fd0f515f77bb7fab99c26e9414ec1cd42b125
VillAIgence CI 29929152567 → SUCCESS
Java Pull Request CI 29929152537 → SUCCESS
```

Fresh final checks covered unit tests, Fabric build, distributable package verification, NeoForge build, and Fabric compatibility build.

Final review confirmed:

- `OpenAIChatAI` absent from PR #39 diff;
- provider/parser/retry code untouched;
- legacy `memory.json` format/path code untouched;
- no ingestion on absent/blank result paths;
- no unresolved review threads/comments.

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

Any concrete release-blocking defect should be a narrow reliability patch rather than mixed into Memory 2.0.

## 0.2 Memory 2.0 — active, partial

Implemented:

```text
MemoryEvent domain
+ explicit provenance/confidence/importance/emotional metadata
+ bounded per-NPC memory2.json persistence
+ idempotent event IDs
+ deterministic bounded retrieval/ranking
+ authoritative ACTION ingestion from successful safe actions
+ bounded provenance-preserving context integration into real NPC turns
+ controlled successful dialogue → DIALOGUE episodic-memory ingestion
```

Still not implemented:

- validated relationship-reason provenance/ingestion;
- dedicated working-memory orchestration beyond existing bounded dialogue history/context;
- semantic facts/beliefs layer;
- deterministic consolidation policy;
- forgetting/decay;
- semantic duplicate merging;
- migration from existing `memory.json`;
- NPC-to-NPC memory/rumor propagation;
- exit criterion: important events recalled days later without full raw chat history.

### Next recommended development slice

Build a **validated relationship-reason provenance contract** before production `RELATIONSHIP_CHANGE` memories are created.

The current relationship mutation path has numeric proposed deltas:

```text
trust
respect
fear
affinity
```

But it does not yet have a safely typed, provenance-bearing reason that can be persisted without pretending the LLM explanation is authoritative.

Recommended contract goals:

```text
successful relationship delta application
→ deterministic RelationshipChange event identity
→ bounded reason/evidence object with explicit provenance
→ no invented authoritative reason
→ fail-soft MemoryEvent.Type.RELATIONSHIP_CHANGE ingestion
```

Requirements:

- reason provenance must be explicit;
- numeric relationship state remains server-controlled;
- an LLM-provided explanation must never silently become `SYSTEM_OBSERVED`;
- duplicate/replay must not multiply relationship memories;
- no relationship memory should be written if the underlying delta was not actually applied;
- relationship-reason persistence failure must not roll back already-valid relationship state;
- avoid LLM summarization/embeddings in the first slice.

After this contract is stable, proceed to working-memory/semantic-belief design, deterministic consolidation/duplicate policy, forgetting/decay, then migration.

## 0.3 Personality + NPC↔NPC social graph

Not implemented yet.

This should follow Memory 2.0 closely because later rumors, autonomous behavior, and settlements depend on durable identities and social history.

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
Continue 0.2 with validated relationship-reason provenance and RELATIONSHIP_CHANGE memory ingestion.

Priority C
Then design working-memory orchestration + semantic facts/beliefs boundaries.

Priority D
Later add deterministic consolidation/duplicate policy, forgetting/decay, and migration.
```

Do **not** jump directly to embeddings or LLM summarization before deterministic provenance and memory lifecycle behavior are proven.

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
