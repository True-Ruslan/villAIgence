# VillAIgence Project State

> **Canonical current-state handoff.** Read this file before `docs/ROADMAP.md` when resuming work.
>
> Last reconciled: **2026-08-05**, after M11 Phase D was merged through PR #112.
>
> Always distinguish repository/unit evidence, production-candidate evidence, exact-release evidence and installed operator-server/client evidence.

## Executive snapshot

VillAIgence is a Minecraft 1.21.1 MCA-derived mod evolving from AI-assisted villager dialogue into a persistent living-society simulation layer.

```text
repository:                         True-Ruslan/villAIgence
primary branch:                     1.21.1
latest runtime implementation head: 67e0644b355708c06747e3ec4659a337bc4189b3
Java:                               21
primary distribution:               Fabric
NeoForge:                           compile compatibility required
latest official release:            0.1.25+1.21.1
release commit:                     588cc676d356271c4cf74eb21131f6d071476e48
```

Current delivery state:

```text
0.1.x reliability and security baseline             complete
Memory 2.0 foundation                               substantially implemented
MCA selective synchronization S1-S8                 implemented and validated
Operator Lore S9-S10c                               implemented
M11 Phase A risk catalog and GameTests              complete
M11 Phase B exact production-JAR restart harness    complete
M11 Phase C provider and voice orchestration        complete
M11 Phase D concurrency and client acceptance       automated and merged
real installed two-client Operator Lore canary      pending
0.1.25 inventory/grave/resurrection canary          pending
next release containing post-0.1.25 work            not yet requested
```

PR #112 exact-head evidence before merge:

```text
reviewed head:                da0ddd0a463f29cdf211a14a749c4872e66d0e30
VillAIgence CI:               run 31017438938 / #1527 — SUCCESS
Java Pull Request CI:         run 31017438567 / #915  — SUCCESS
Repository security policy:  run 31017438672 / #961  — SUCCESS
review verdict:               no unresolved P0/P1/P2 finding
```

The squash merge is `67e0644b355708c06747e3ec4659a337bc4189b3`. Post-merge and documentation-head workflows must still be checked when resuming.

---

# Product and architecture

VillAIgence is not merely MCA with an LLM call. The target product is:

```text
NPC Identity
+ Personality
+ Memory
+ Relationships
+ Knowledge
+ Voice
+ Goals
+ Server-authoritative Actions
+ Settlements
+ Factions
+ Emergent History
= Persistent Living Society
```

## Architecture laws

1. **The LLM is never authoritative.** Minecraft/server-owned state is truth.
2. Mutable game state used by asynchronous AI is captured into immutable bounded context first.
3. The LLM may propose dialogue, actions and relationship deltas; server policy validates and executes mutations.
4. Provider/model changes must not redefine persistent NPC identity, memory, relationships or voice.
5. External-provider and auxiliary persistence failures fail soft whenever safe.
6. Credentials remain server-side and endpoint-bound.
7. Important persistent formats remain explicit, bounded, inspectable and world-local.
8. Retry and replay paths must not duplicate dialogue, memory, relationship or gameplay effects.
9. FACT requires server-owned evidence; dialogue is episodic by default.
10. Confidence never upgrades BELIEF into FACT.
11. Consolidation preserves provenance and independent source-event IDs.
12. Forgetting is deterministic storage policy, not an LLM decision.
13. Operator Lore is explicit background context, not an observed current-world fact.
14. Current observed world facts override conflicting lore or recalled memory.
15. Clients never own permissions, target identity, file access, revisions or persistence mutations.
16. Migration remains additive, deterministic, idempotent and reversible until cutover evidence exists.
17. Exact release identity must match the tag, filename, `fabric.mod.json` and manifest.
18. Published artifacts must be byte-identical to the exact artifact accepted by the release gate.
19. Automated logical-client evidence never silently becomes a real installed multi-client claim.

Canonical AI flow:

```text
Minecraft/server state
→ immutable bounded snapshot
→ observed facts + operator lore + episodic/semantic memory
→ provider/LLM proposal
→ server validation/revalidation
→ server-owned mutation
→ persistent authoritative evidence
```

---

# Identity and compatibility

```text
public name:      VillAIgence
short name:       VAI
Minecraft:        1.21.1
Java:             21
internal mod id:  mca
package root:     net.conczin.mca
config:           config/livingworld.json
world data root:  <world>/livingworld/
```

Compatibility-sensitive `mca`, `LivingWorld` and `livingworld` identifiers must not be renamed without a dedicated migration design.

---

# Persistent world-local data

```text
<world>/livingworld/memory.json
<world>/livingworld/memory2.json
<world>/livingworld/semantic-memory.json
<world>/livingworld/events.json
<world>/livingworld/relationships.json
<world>/livingworld/voices.json
<world>/livingworld/operator-lore.json
```

`memory.json` remains active. Memory 2.0 is additive and the legacy migration has not started. Unrelated release, editor or synchronization work must not delete or destructively rewrite it.

Implemented persistence guarantees include deterministic JSON, bounded stores, temporary-file writes, atomic replacement where supported, fail-open auxiliary recovery, corrupt-file preservation, explicit provenance and restart-safe restoration.

---

# Implemented systems

## AI provider, parsing and security

Implemented and retained:

- OpenAI-compatible Chat, STT and TTS endpoints;
- OpenRouter-compatible Chat operation;
- endpoint normalization and credential-family binding;
- remote plaintext rejection except explicit literal-loopback development mode;
- authenticated redirect blocking;
- bounded Chat, STT, TTS, error and verification bodies;
- controlled `content:null`, empty, malformed and provider-error handling;
- retry without duplicate persistent/gameplay effects;
- voice-duration and aggregate PCM limits;
- verified dependencies, pinned Actions and repository security policy;
- diagnostics without secrets, prompts, transcripts or hidden reasoning.

Step 1 security findings `SEC-001` through `SEC-009` are closed.

## Voice orchestration

M11 Phase C proves one monotonic total deadline across one captured voice turn:

```text
capture accepted
→ queue handoff
→ STT
→ Chat including retries
→ authoritative text/relationship commit
→ optional TTS
```

`VoiceConversationService` creates one `AiRequestDeadline` before the first queue handoff and passes the same object through STT, Chat and TTS. Streaming response reads are bounded by both byte limits and the remaining deadline.

Configuration:

```text
voiceConversationTimeoutSeconds default: 120
normalized minimum:                     10
normalized maximum:                     300
```

Deterministic production-client integration proves:

- one STT request;
- one empty Chat completion followed by one successful retry;
- one TTS request;
- exactly one `DIALOGUE` event;
- exactly one `RELATIONSHIP_CHANGE` event;
- relationship delta applied once;
- deterministic returned PCM;
- blocked TTS cannot obtain a fresh timeout budget;
- already committed text and relationship effects are not duplicated.

Physical microphone capture, Simple Voice Chat UDP/Opus playback, audible spatial output and subjective response quality remain installed-client canaries.

## Memory 2.0

Implemented:

- immutable NPC-owned episodic `MemoryEvent`;
- `DIALOGUE`, `OBSERVATION`, `ACTION`, `RELATIONSHIP_CHANGE`;
- deterministic UUID idempotency and ordering;
- bounded per-NPC persistence;
- deterministic retrieval;
- text/voice DIALOGUE parity;
- authoritative ACTION and relationship-transition ingestion;
- bounded Working Memory;
- typed semantic FACT/BELIEF entries;
- controlled server-observed FACT ingestion;
- consolidation and source union;
- deterministic pressure-based forgetting/decay;
- source durability and NPC isolation;
- restart-safe persistence.

Truth boundary:

```text
FACT     → SYSTEM_OBSERVED only
BELIEF   → PLAYER_TOLD / NPC_TOLD / INFERRED only
DIALOGUE → episodic only by default
```

Remaining Memory 2.0 work:

- additive legacy `memory.json` migration;
- controlled BELIEF producers;
- trustworthy relationship-change reasons;
- long-horizon and multi-day soak;
- NPC-to-NPC knowledge transfer;
- rumor propagation with uncertainty and provenance.

## Operator Lore S9-S10c

Implemented:

- world-local `operator-lore.json` schema version 1;
- WORLD, PLAYER, VILLAGER and VILLAGE scopes;
- immutable prompt-context capture with explicit provenance;
- permission-level-2 server-authoritative read/write API;
- server-resolved player, villager, dimension and village identity;
- SHA-256 optimistic revisions;
- bounded code-point and UTF-8 validation;
- explicit `OK`, `UNCHANGED`, `CONFLICT`, `INVALID`, `FORBIDDEN`, `NOT_FOUND`, `ERROR` statuses;
- dedicated responsive client editor;
- multiline editing and budget counters;
- Save, Clear, Reload and close confirmations;
- request-generation correlation and stale-response rejection;
- conflict review without blind overwrite.

The client contains no arbitrary UUID/dimension/village ID path and has no direct persistence access.

## Selective MCA synchronization and runtime corrections

Implemented and retained:

- tombstone item/entity-data integrity;
- UUID-preserving villager/zombie conversion;
- occupied HOME-bed rejection;
- water-aware navigation without replacing vanilla path-position semantics;
- climbable and ladder navigation;
- staggered pathfinding and progress watchdog;
- graveyard mourning lifecycle;
- relationship-gift `InteractionResult` contract;
- fishing and AquaCulture compatibility;
- stable mounted archer movement control;
- filled Silk Touch grave fallback with exactly one data-bearing item;
- direct source-level tombstone wiring instead of fragile project-owned mixins;
- inventory ownership transfer before tombstone serialization;
- exact fallback loose drops when no tombstone captures the NPC.

The installed `0.1.23` acceptance exposed inventory loss before tombstone serialization. PR #105 fixed the real MCA death path and added captured/fallback GameTests.

---

# M11 automated acceptance

## Phase A — risk catalog and server GameTests

Complete:

- validated risk catalog across seven domains;
- exact entity/registry boot;
- production navigation wiring;
- NPC → tombstone item → NPC identity/inventory lifecycle;
- real Silk Touch `getDrops` round trip;
- empty-grave negative control;
- two isolated water lanes;
- post-water dry-land route;
- direct water-tag surface hook;
- production-JAR rejection of test metadata/classes.

## Phase B — exact production-JAR startup/restart

Complete:

- exact remapped Fabric candidate staged outside Loom/dev runtime;
- pinned Fabric installer, Fabric API and Simple Voice Chat inputs;
- deterministic artifact manifest and SHA-256 recording;
- real Fabric server startup in JVM 1;
- controlled `stop`, full save and exit code 0;
- same-world restart in JVM 2;
- forbidden Mixin/refmap/mod-resolution/JVM signature scan;
- six canonical JSON stores discovered exactly once;
- stable paths and SHA-256 values across restart;
- fixture/test code excluded from the public JAR.

## Phase C — deterministic provider and voice integration

Complete through PRs #108 and #110:

- production Chat/STT/TTS HTTP clients against literal-loopback providers;
- no external provider or real credential in CI;
- shared Chat retry deadline;
- one complete STT → Chat retries → TTS orchestration deadline;
- bounded response reads;
- explicit deadline exhaustion classification;
- exactly-once dialogue and relationship persistence.

`VAI-AI-003` and `VAI-AI-004` are automated VillAIgence CI scenarios.

## Phase D — concurrency and client acceptance

Complete through PR #112.

Production structure:

```text
trusted Minecraft authority
→ permission and target resolution
→ package-private resolved authority seam
→ SHA-256 revision decision
→ world-local Operator Lore store
→ canonical result
```

Automated logical-client scenario:

```text
client A reads V0/R0
client B reads V0/R0
client A writes V1/R0 → OK/R1
client B writes V2/R0 → CONFLICT with V1/R1
store remains V1/R1
client B keeps its draft and adopts R1
client B writes V2/R1 → OK/R2
final store is V2/R2 exactly once
```

Additional automated guarantees:

- exact replay returns `UNCHANGED` and does not rewrite the file;
- unauthorized logical read/write returns `FORBIDDEN`, discloses no canonical value/revision and performs no mutation;
- Clear uses the same revision-protected conflict/retry path;
- stale request-generation responses cannot modify the active editor state;
- conflict preserves the user draft and exposes explicit review choices;
- modal response forwarding remains owned by the active editor;
- C2S contains no arbitrary player/villager UUID, dimension ID or village ID;
- client code has no Operator Lore file/store access or global response mailbox;
- Phase D fixtures do not enter the distributable JAR.

Acceptance catalog:

```text
VAI-CONCUR-003 → automated logical-client/common-integration evidence
VAI-CONCUR-004 → real installed two-client UI/network canary
```

Canonical implementation evidence:

```text
docs/livingworld/VALIDATION_M11_PHASE_D_IMPLEMENTATION.md
```

Phase D does **not** claim two physical Minecraft clients, real network timing or visual/OS-specific UI behavior.

---

# Releases and validation boundaries

## Latest official release

```text
tag:          0.1.25+1.21.1
commit:       588cc676d356271c4cf74eb21131f6d071476e48
release run:  30989597246 / #138 — SUCCESS
assets:       Fabric JAR, SHA-256 file, dependency manifest
```

`0.1.25` is the first release published through the complete exact-production release gate. The published JAR is byte-identical to the production-accepted JAR.

The release contains the tombstone inventory-ownership correction. Its focused installed inventory/grave/resurrection canary is still required before the release is called fully operator-accepted.

## Code ahead of the release

Current `1.21.1` contains post-`0.1.25` work:

- PR #108 deterministic provider acceptance and shared Chat retry deadline;
- PR #109 configuration-cache-safe refmap verification and release-request scoping;
- PR #110 complete voice-turn orchestration deadline and exactly-once persistence proof;
- PR #111 canonical state reconciliation and Phase D plan;
- PR #112 deterministic logical-client concurrency/client acceptance.

These changes are not part of `0.1.25`. The next official release must use the next free sequential version and the established exact-production gate.

## Evidence layers

Never collapse these into one claim:

```text
UNIT / SOURCE POLICY
COMMON INTEGRATION
SERVER GAMETEST
PRODUCTION CANDIDATE STARTUP/RESTART
EXACT RELEASE WORKFLOW
INSTALLED OPERATOR SERVER/CLIENT
REAL MULTI-CLIENT CANARY
```

Known installed evidence includes broad `0.1.20` partial acceptance, focused corrected water/grave checks on later candidates and the `0.1.23` inventory defect reproduction. Exact `0.1.25` installed inventory acceptance remains pending. The Phase D real multi-client canary also remains pending on a candidate containing PR #112.

---

# Known gaps and technical debt

1. A true simultaneous installed two-client Operator Lore conflict remains unverified.
2. Exact official `0.1.25` inventory/grave/resurrection canary remains pending.
3. Physical microphone, UDP/Opus playback, spatial audible TTS and subjective LLM quality remain manual canaries.
4. `memory.json` migration has not started.
5. Controlled BELIEF producers and causal relationship reasons remain incomplete.
6. NPC-to-NPC knowledge and rumor propagation remain future work.
7. Long-horizon, multiplayer and multi-day soak coverage remains incomplete.
8. Broader automated gameplay matrix for ladders, doors, gifts, fishing and mounted combat remains planned.

---

# Next optimal delivery step

Prepare the next sequential exact candidate containing PRs #108–#112, but do not publish it until the focused installed acceptance boundary is satisfied.

Recommended sequence:

```text
confirm post-merge and documentation-head CI
→ choose the next free version (expected 0.1.26+1.21.1 unless repository evidence differs)
→ open a release-request PR and build the exact versioned dry-run candidate
→ install that exact candidate on the operator server/client
→ run inventory/grave/resurrection canary
→ run real two-client Operator Lore stale-revision canary
→ run short physical voice smoke
→ merge/publish only on PASS
→ verify published JAR identity and one restart
→ begin additive legacy memory.json migration
```

The release/canary package must not:

- use a snapshot JAR as release evidence;
- claim logical-client automation as installed multi-client evidence;
- overwrite or migrate `memory.json`;
- weaken revision, authority, provider or credential policies;
- combine unrelated personality/social-graph implementation into the release boundary.

After the release boundary:

```text
additive memory.json migration
→ remaining Memory 2.0 exit criteria
→ personality and NPC↔NPC social graph
→ knowledge propagation and rumors
```

---

# New-session handoff protocol

Preferred resume prompt:

> Open `docs/PROJECT_STATE.md`, `docs/ROADMAP.md`, `common/src/test/resources/acceptance/scenarios.tsv`, and the M11 Phase D implementation document. Check current `1.21.1` HEAD, open PRs, releases and CI. Continue from the exact next-release candidate and installed canary boundary without weakening Operator Lore authority or revision contracts.

A new session must:

1. read this file;
2. read `docs/ROADMAP.md`;
3. inspect current `1.21.1` HEAD;
4. inspect recent/open PRs and exact-head CI;
5. inspect latest tag/release and confirm the next free version;
6. distinguish automated, production-candidate, exact-release and installed evidence;
7. complete the candidate/canary/release boundary before beginning `memory.json` migration;
8. update canonical documents after material progress.
