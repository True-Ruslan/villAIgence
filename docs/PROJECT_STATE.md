# VillAIgence Project State

> **Canonical current-state handoff.** Read this file before `docs/ROADMAP.md` when resuming work in a new session.
>
> Last major state update: **2026-07-22**.
>
> This file records implemented repository state, release anchors, architecture/truth boundaries, compatibility constraints, and the first unimplemented development priority. Always reconcile it with recent PRs, releases/tags, and CI before active development.

## Executive snapshot

VillAIgence is a Minecraft 1.21.1 MCA-derived mod evolving from AI-assisted villager dialogue into a **persistent living-society simulation layer**.

Current anchors:

```text
base branch: 1.21.1
current implemented HEAD after PR #41:
b05a5a0cd302253824e1bbcaf33053cca95641e5

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
PR #41  server-observed relationship-change ingestion
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
10. A persisted server-owned state transition may be remembered as factual evidence, but its psychological/causal explanation is not automatically factual.
11. Autonomous AI must eventually be event-driven and budgeted rather than “LLM every tick.”

Canonical flow:

```text
Mutable Minecraft state
→ server-thread immutable snapshot
→ provider/LLM
→ proposed response / intents / deltas
→ server validation / revalidation
→ server-owned mutations
→ persisted factual evidence
→ optional bounded Memory 2.0 ingestion
```

---

# Implemented 0.1.x reliability and AI foundation

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

# Persistent world-local data

```text
<world>/livingworld/memory.json         legacy bounded player↔NPC dialogue history
<world>/livingworld/memory2.json        Memory 2.0 event store
<world>/livingworld/events.json         server-owned factual world events
<world>/livingworld/relationships.json  player↔NPC relationship state
<world>/livingworld/voices.json         persistent NPC voice identity
```

These files belong with world backup/restore procedures.

## Legacy dialogue history

`memory.json` remains the proven `0.1.x` rolling dialogue-history path.

Memory 2.0 now additionally records bounded successful dialogue episodes, but legacy `memory.json` has **not** been migrated or removed.

## Authoritative factual world events

`events.json` stores server-owned factual events with provenance/time/location/dimension metadata and bounded context queries.

Player/LLM claims cannot directly become authoritative `WorldEvent`s.

## Player↔NPC relationship state

`relationships.json` stores bounded server-controlled dimensions:

```text
trust
respect
fear
affinity
```

The LLM may propose small numeric per-turn deltas, but the server clamps/applies/persists the actual resulting state.

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

Configuration introduced/used by Memory 2.0:

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

Successful usable snapshot-aware OpenAI turns create bounded NPC-owned `DIALOGUE` MemoryEvents.

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

`PLAYER_TOLD` is deliberately conservative: the server can verify that a conversation occurred, but the semantic content of the player's statement and generated NPC reply remains belief/dialogue data rather than authoritative Minecraft truth.

Stored summary:

```text
Player said: <bounded player utterance> | NPC replied: <bounded NPC utterance>
```

Each utterance is whitespace/control normalized and independently capped to 240 Unicode code points. No LLM summarization or semantic fact extraction is used.

Deterministic event identity:

```text
memory2-dialogue-v1
villager UUID
player UUID
snapshot game time
full normalized player message
```

NPC reply and wall-clock time are excluded from identity, so replay/redelivery of the same turn maps to the same UUID even if provider wording differs.

Lifecycle:

```text
snapshot-aware ChatAI.answer(...)
→ OpenAIChatAI.answer(...snapshot) completes
→ original Optional<String> returned to ChatAI
→ present + nonblank result
→ fail-soft Memory2DialogueIngestor.recordIfEnabled(...)
→ original Optional returned unchanged
```

Important boundaries:

- legacy `memory.json` behavior is unchanged;
- no event is created for absent/blank results, provider failures, exhausted `content:null`/empty responses, disabled Memory 2.0, classic/non-snapshot paths, or Inworld fallback;
- Memory 2.0 persistence failure cannot remove/replace an already-produced visible reply.

## PR #41 — server-observed relationship-change ingestion

Merge commit:

```text
b05a5a0cd302253824e1bbcaf33053cca95641e5
```

VillAIgence now records a deterministic `RELATIONSHIP_CHANGE` MemoryEvent only for a real relationship transition that was successfully persisted by the server-owned relationship store.

### Exact applied transition

`LivingWorldRelationshipStore` now exposes:

```text
applyDeltaWithResult(...)
→ before
→ after
→ appliedDelta = after - before
→ changed
```

The existing public `applyDelta(...) -> LivingWorldRelationshipState` remains source-compatible and delegates to the richer result method.

Memory uses the **actual applied delta**, not the raw model proposal.

Example:

```text
before trust = 99
LLM proposes trust +5
server-bound final trust = 100
actual applied delta = +1
Memory 2.0 records +1
```

`relationships.json` schema/version did not change.

### Memory mapping

```text
type = RELATIONSHIP_CHANGE
provenance = SYSTEM_OBSERVED
participants = [villagerId, playerId]
importance = 55
emotionalWeight = 0
confidence = 100
relationshipReasons = []
```

Deterministic summary contains only server-observed numeric evidence:

```text
Relationship with player changed: trust +2, respect -1, fear -1, affinity +1; now trust=12, respect=3, fear=0, affinity=8.
```

Deterministic identity:

```text
memory2-relationship-change-v1
villager UUID
player UUID
snapshot game time
before trust,respect,fear,affinity
after trust,respect,fear,affinity
```

Wall-clock time is excluded; exact replay/redelivery remains idempotent through `MemoryEventStore`.

Lifecycle ordering:

```text
LLM proposes bounded numeric relationship delta
→ server applies/clamps
→ relationships.json persistence succeeds when changed
→ exact LivingWorldRelationshipChange returned
→ unchanged? stop
→ Memory2RelationshipChangeIngestor.recordIfEnabled(...)
→ memory2.json append
```

Failure boundaries:

- relationship persistence failure → log + return, no Memory 2.0 write;
- secondary Memory 2.0 failure → log only; already-valid relationship state and visible reply remain intact.

Truth boundary:

- factual: the relationship state numerically changed from before to after;
- factual: the actual applied delta was `after - before`;
- **not automatically factual:** why the NPC felt that way or what psychological cause produced the change.

Therefore `relationshipReasons=[]` intentionally remains empty. Free-form causal reasons require a future explicit `PLAYER_TOLD`, `NPC_TOLD`, `INFERRED`, or genuinely server-owned provenance source.

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

Memory 2.0 PRs #31/#33/#35/#37/#39/#41 are post-release development and are not yet in a published artifact.

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

## PR #39 dialogue ingestion evidence

Valid main RED:

```text
head: 4aea2db256ee14d0761526efdcbd5b1ae101dd25
VillAIgence CI 29927032710
→ expected FAILURE at :common:compileTestJava
→ missing DialogueMemoryAdapter / Memory2DialogueIngestor
```

Lifecycle-guard RED:

```text
head: edb745314c97909a201113885eb3722125fed252
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

## PR #41 relationship-change ingestion evidence

Exact mutation result RED:

```text
head: f4c6eaf59c79cb08621c8dcdd31e7317976c3ebd
VillAIgence CI 29931503379
→ expected FAILURE at :common:compileTestJava
→ missing LivingWorldRelationshipChange / applyDeltaWithResult(...)
```

Adapter RED:

```text
head: f415fde9e52e45bd200f1b73fbde362a3b171e2a
VillAIgence CI 29932123694
→ expected FAILURE at :common:compileTestJava
→ missing RelationshipChangeMemoryAdapter
```

Ingestor RED:

```text
head: 38b614b8508d20f9085dc4290ab9cf95f9ff5ff6
VillAIgence CI 29932750358
→ expected FAILURE at :common:compileTestJava
→ missing Memory2RelationshipChangeIngestor
```

Exact final head before merge:

```text
59829be135c9c31d1c7076c0d2de7d71c117b891
VillAIgence CI 29934220443 → SUCCESS
Java Pull Request CI 29934223826 → SUCCESS
```

Final PR #41 checks covered:

- unit tests;
- Fabric build;
- distributable Fabric package verification;
- NeoForge build;
- Fabric compatibility build;
- per-file diff review confirming `OpenAIChatAI` changed only two imports + `applySnapshotRelationshipDelta(...)`;
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
+ server-observed persisted relationship transition → RELATIONSHIP_CHANGE ingestion
```

Still not implemented:

- dedicated working-memory orchestration beyond existing bounded dialogue history/context;
- explicit semantic facts/beliefs layer;
- deterministic duplicate handling / consolidation policy;
- forgetting/decay mutation;
- migration from existing `memory.json`;
- NPC-to-NPC memory/rumor propagation;
- trustworthy causal relationship reasons when/if a provenance source exists;
- exit criterion: important event recall days later without full raw chat history.

### Next recommended development slice

Build **working-memory orchestration + explicit semantic facts/beliefs boundaries** before adding LLM-driven consolidation or embeddings.

Recommended architecture split:

```text
Working memory
→ bounded immediate conversational/task context
→ short-lived and intentionally small

Episodic memory
→ existing MemoryEvent experiences
→ DIALOGUE / ACTION / RELATIONSHIP_CHANGE / OBSERVATION

Semantic facts/beliefs
→ explicit subject/predicate/value-style knowledge or equivalent typed model
→ provenance + confidence mandatory
→ SYSTEM_OBSERVED facts separated from PLAYER_TOLD/NPC_TOLD/INFERRED beliefs
```

First-slice goals:

- define typed immutable semantic fact/belief model without LLM authority;
- define deterministic extraction inputs only from already typed server-owned or provenance-bearing sources;
- define bounded working-memory selection separately from persistent MemoryEvent retrieval;
- avoid duplicating full raw dialogue history;
- do not introduce embeddings/vector DB as a prerequisite;
- do not let summarization silently upgrade beliefs to facts;
- keep migration from `memory.json` deferred until the new layers are stable.

After that:

```text
deterministic duplicate handling / consolidation policy
→ forgetting / decay
→ migration from legacy memory.json
→ Memory 2.0 exit-criterion validation
```

Causal relationship reasons remain deferred until VillAIgence has a trustworthy provenance source. Do not invent them merely to populate `relationshipReasons`.

## 0.3 Personality + NPC↔NPC social graph

Not implemented yet.

This should follow Memory 2.0 closely because later rumors, autonomous behavior, settlements, factions, and emergent history depend on durable identities and social history.

Planned traits include temperament, values, goals, fears, likes/dislikes, speech style, morality, ambition, curiosity, sociability, aggression, and loyalty.

Planned general NPC↔NPC relationship dimensions include friendship, trust, respect, fear, family, rivalry, romance, and grudge.

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
Continue 0.2 with working-memory orchestration + explicit semantic facts/beliefs design.

Priority C
Then implement deterministic duplicate/consolidation policy without silently changing provenance.

Priority D
Then implement forgetting/decay and only afterward design migration from legacy memory.json.
```

Do **not** jump directly to embeddings or LLM summarization before typed provenance, memory-layer boundaries, and lifecycle behavior are proven.

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
