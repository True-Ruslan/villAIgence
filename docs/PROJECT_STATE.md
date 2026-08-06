# VillAIgence Project State

> **Canonical current-state handoff.** Read this file before `docs/ROADMAP.md` when resuming work.
>
> Last reconciled: **2026-08-06**, during final documentation closure for M11 Phase E in draft PR #114.
>
> Always distinguish unit/source-policy evidence, common integration, server GameTests, production-candidate evidence, exact-release dry-run evidence and installed operator-server/client evidence.

## Executive snapshot

VillAIgence is a Minecraft 1.21.1 MCA-derived mod evolving from AI-assisted villager dialogue into a persistent living-society simulation layer.

```text
repository:                         True-Ruslan/villAIgence
primary branch:                     1.21.1
active implementation PR:           #114 — draft, unmerged
runtime implementation head:        20fb7fd741916ffcb3f7f4630fd6d0bb046efba7
Java:                               21
primary distribution:               Fabric
NeoForge:                           compile compatibility required
latest official release:            0.1.25+1.21.1
latest release commit:              588cc676d356271c4cf74eb21131f6d071476e48
next release:                       not requested
```

Current delivery state:

```text
0.1.x reliability/security baseline                    COMPLETE
Memory 2.0 foundation                                  SUBSTANTIALLY IMPLEMENTED
MCA selective synchronization S1-S8                    COMPLETE
Operator Lore S9-S10c                                  COMPLETE
M11 Phases A-D                                          COMPLETE
M11 Phase E automation completion E0-E9                 COMPLETE AT AUTOMATION LAYER
acceptance catalog                                      28 AUTOMATED / 6 MANUAL / 0 PLANNED
installed graphical/physical canaries                   PENDING
next post-0.1.25 exact release                          NOT REQUESTED
legacy memory.json migration                            AFTER RELEASE BOUNDARY
```

PR #114 remains draft. No Phase E tag or release has been created.

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

1. The LLM is never authoritative; Minecraft/server-owned state is truth.
2. Mutable game state used by asynchronous AI is captured into immutable bounded context first.
3. Provider/model changes must not redefine persistent NPC identity, memory, relationships or voice.
4. External-provider and auxiliary-persistence failures fail soft whenever safe.
5. Retry and replay paths must not duplicate dialogue, memory, relationship or gameplay effects.
6. FACT requires server-owned evidence; dialogue is episodic by default.
7. Confidence never upgrades BELIEF into FACT.
8. Operator Lore is explicit background context, not an observed current-world fact.
9. Current observed world facts override conflicting lore or recalled memory.
10. Clients never own permissions, target identity, file access, revisions or persistence mutations.
11. Migration remains additive, deterministic, idempotent and reversible until cutover evidence exists.
12. Exact release identity must match tag, filename, embedded metadata and manifest.
13. Published artifacts must be byte-identical to the exact artifact accepted by the release gate.
14. Automated logical-client evidence never silently becomes a real installed multi-client claim.
15. Unknown CI changes fail closed to the complete mandatory acceptance matrix.

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

Canonical runtime stores:

```text
<world>/livingworld/memory.json
<world>/livingworld/memory2.json
<world>/livingworld/semantic-memory.json
<world>/livingworld/relationships.json
<world>/livingworld/voices.json
<world>/livingworld/operator-lore.json
```

Additional event/fixture evidence may exist under the same world-local root during runtime or acceptance.

`memory.json` remains active. Memory 2.0 is additive and legacy migration has not started. Unrelated work must not delete or destructively reinterpret it.

Implemented persistence guarantees include deterministic JSON, bounded stores, temporary-file writes, atomic replacement where supported, corrupt-file preservation, fail-open auxiliary recovery, explicit provenance and restart-safe restoration.

---

# Implemented systems

## AI provider, parsing and security

Implemented and retained:

- OpenAI-compatible Chat, STT and TTS endpoints;
- OpenRouter-compatible Chat operation;
- endpoint normalization and credential-family binding;
- authenticated redirect blocking;
- remote plaintext rejection except explicit literal-loopback development mode;
- bounded Chat, STT, TTS, error and verification bodies;
- controlled null, empty, malformed and provider-error handling;
- retry without duplicate persistent/gameplay effects;
- voice-duration and aggregate PCM limits;
- verified dependencies, pinned Actions and repository security policy;
- diagnostics without secrets, prompts, transcripts or hidden reasoning.

Security findings `SEC-001` through `SEC-009` are closed.

## Voice orchestration and transport

Implemented automated evidence covers:

- one monotonic total deadline across capture, queue handoff, STT, Chat retries and optional TTS;
- exactly-once dialogue and relationship persistence;
- deterministic mock-provider STT/Chat/TTS integration;
- real Simple Voice Chat Opus encode/decode;
- packet loss concealment, duplicate/order rejection and bounded PCM;
- encoder/decoder closure and cancellation;
- repeated voice transport validation across five constrained-heap production restarts.

Physical microphone permissions, real client UDP routing, audible spatial playback and subjective audio quality remain manual.

## Memory 2.0

Implemented:

- immutable NPC-owned episodic events;
- DIALOGUE, OBSERVATION, ACTION and RELATIONSHIP_CHANGE;
- deterministic UUID idempotency and ordering;
- bounded per-NPC persistence and retrieval;
- text/voice DIALOGUE parity;
- bounded Working Memory;
- typed semantic FACT/BELIEF entries;
- controlled server-observed FACT ingestion;
- consolidation and provenance/source union;
- deterministic pressure-based forgetting;
- NPC isolation and restart safety.

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
- long-horizon and multi-day simulation evidence;
- NPC-to-NPC knowledge transfer;
- rumor propagation with uncertainty and provenance.

## Operator Lore

Implemented:

- world-local schema-versioned `operator-lore.json`;
- WORLD, PLAYER, VILLAGER and VILLAGE scopes;
- immutable prompt-context capture with explicit provenance;
- permission-level-2 server-authoritative read/write API;
- server-resolved identity and target scope;
- SHA-256 optimistic revisions;
- bounded code-point and UTF-8 validation;
- explicit success/conflict/error statuses;
- multiline editor, counters and close confirmations;
- request-generation correlation and stale-response rejection;
- conflict review without blind overwrite;
- authenticated two-session logical network harness with owner-bound responses.

The client contains no arbitrary target identity path and has no direct persistence access.

## Selective MCA synchronization and runtime corrections

Implemented and retained:

- tombstone item/entity-data integrity;
- pre-serialization inventory ownership transfer;
- UUID-preserving villager/zombie conversion;
- repeated resurrection/replay identity guard;
- occupied HOME-bed rejection;
- water-aware, climbable, ladder, obstacle and door navigation;
- progress watchdog and staggered pathfinding;
- graveyard mourning lifecycle;
- relationship-gift interaction semantics;
- fishing/AquaCulture compatibility;
- stable mounted archer control and NPC-owned projectile evidence;
- exactly one filled Silk Touch portable grave;
- exact loose-drop fallback when no tombstone captures the NPC.

---

# M11 automated acceptance

## Phase A — risk catalog and server GameTests

Complete. The catalog is the canonical risk-to-proof map at:

```text
common/src/test/resources/acceptance/scenarios.tsv
```

Current catalog state:

```text
34 total scenarios
28 AUTOMATED
6 MANUAL_CANARY
0 PLANNED
```

## Phase B — exact production-JAR startup/restart

Complete:

- exact remapped Fabric candidate staged outside Loom/dev runtime;
- pinned installer/runtime dependencies;
- deterministic manifest and hashes;
- real JVM startup, controlled stop/save and restart;
- forbidden startup-signature scan;
- six canonical stores discovered exactly once;
- stable paths and hashes;
- fixture classes excluded from the public JAR.

## Phase C — provider and voice orchestration

Complete:

- production Chat/STT/TTS clients against literal-loopback providers;
- no external provider or real credential in CI;
- shared retry and full-turn deadlines;
- bounded streaming reads;
- explicit deadline exhaustion;
- exactly-once dialogue and relationship effects.

## Phase D — concurrency and client-state acceptance

Complete:

- authenticated text session ownership;
- two logical Operator Lore sessions;
- stale conflict with canonical value/revision;
- retained-draft retry exactly once;
- unauthorized requests disclose and mutate nothing;
- response ownership and stale-generation rejection.

Real installed graphical conflict rendering remains manual.

## Phase E — automation completion

Complete at the automation layer through draft PR #114.

### E0 — production staging hardening

- execution-time Gradle Project access removed;
- provider-backed/configuration-cache-safe staging;
- subprocess handle leaks removed;
- source-policy regression coverage added.

### E1/E2 — identity lifecycle

- duplicate resurrection/replay prevention;
- real death → tombstone → portable data → resurrection lifecycle;
- identity, name and complete inventory preserved;
- second JVM verifies one authoritative entity after restart.

### E3 — corrupt persistence recovery

- six destructive cases;
- exact corrupt-byte backup;
- valid canonical regeneration;
- unaffected sibling hashes unchanged;
- idempotent second startup;
- nightly and release evidence.

### E4/E5 — authenticated transport and concurrency

- authenticated text turn cannot spoof player or NPC ownership;
- provider, persistence and response effects occur exactly once;
- two Operator Lore sessions cannot receive each other's responses;
- stale write returns explicit conflict and retry succeeds once.

### E6 — production voice transport

- real Opus codec and transport lifecycle;
- loss, duplicate and ordering behavior;
- bounded PCM and resource closure;
- hardware-independent production evidence.

### E7/E8 — gameplay and navigation matrix

Sixteen required Fabric GameTests include:

- gifts;
- deterministic fishing and rod durability;
- mounted archer controller/projectile ownership;
- water escape and dry-land continuation;
- obstacle reroute;
- ladder ascent/descent;
- closed-door passage;
- tombstone lifecycle, inventory and replay controls.

### E9 — fail-closed selection and production soak

Implemented:

- deterministic path-to-risk selector;
- five explicit suites: fast, server, production, recovery and package;
- release always selects all suites;
- protected, empty, unsafe and unknown changes fail closed to all;
- main CI executes only explicitly selected groups;
- weekly/relevant-PR/manual production soak;
- three clean authenticated concurrency repetitions at 512 MiB;
- five exact-candidate JVM cycles at 512 MiB;
- lifecycle, voice, identity and six-store hash validation after every cycle;
- machine-readable evidence artifact.

Canonical evidence:

```text
docs/livingworld/VALIDATION_M11_PHASE_E_E7.md
docs/livingworld/VALIDATION_M11_PHASE_E_E8.md
docs/livingworld/VALIDATION_M11_PHASE_E_E9.md
```

---

# Exact Phase E code-head evidence

Validated runtime implementation head:

```text
20fb7fd741916ffcb3f7f4630fd6d0bb046efba7
```

Mandatory results:

```text
VillAIgence CI:                1715 / 31081408589 — PASS
Java Pull Request CI:          1101 / 31081408606 — PASS
Repository security policy:   1335 / 31081408597 — PASS
Supply-chain verification:    161  / 31081408667 — PASS
Production Soak:              8    / 31081408703 — PASS
GitHub Release dry-run:        327  / 31081408638 — PASS
release publication:                              SKIPPED
```

Soak artifact:

```text
production-soak-8
artifact id: 8959713111
digest: sha256:a623cd6e662a1b4e6759ad4ba15a1fe1c646b87d374442435f2cc8bc1ef78c9f
```

Inspected report:

```text
5 clean JVM exits
512 MiB production heap
1 live lifecycle NPC in every cycle
voice PASS in every cycle
peak PCM 7680 bytes in every cycle
six persistent-store SHA-256 values identical across all cycles
```

Release dry-run artifacts:

```text
production-server-acceptance-327  artifact 8959844583
persistence-recovery-327          artifact 8959920387
villaigence-fabric-package        artifact 8959947498
```

The exact production-tested and packaged JARs were byte-identical.

---

# Latest official release and code ahead

Latest official release:

```text
tag:     0.1.25+1.21.1
commit:  588cc676d356271c4cf74eb21131f6d071476e48
```

PR #114 contains post-`0.1.25` automation and acceptance work. It is not part of the published release and must not be described as installed release behavior.

No `0.1.26+1.21.1` request, tag or release has been created by Phase E.

---

# Remaining manual canaries

Routine deterministic manual regression has been removed. The six remaining catalog canaries cover only installed/physical/visual boundaries:

1. `VAI-BOOT-002` — exact released JAR starts on the operator server.
2. `VAI-NAV-001` — two ordinary MCA NPC brains visibly escape water in an installed world.
3. `VAI-GAME-001` — an installed client visibly addresses the selected NPC and renders one response.
4. `VAI-GAME-003` — a real player performs the Silk Touch grave pickup/placement interaction without loss or duplication.
5. `VAI-AI-006` — physical microphone, client UDP routing and audible spatial playback.
6. `VAI-CONCUR-004` — two graphical clients visibly render conflict, reload/keep-draft and reviewed retry.

These canaries must remain small. They must not repeat automated persistence, codec, retry, recovery, identity or concurrency internals.

---

# Known gaps and technical debt

1. Exact installed post-Phase-E canaries are pending.
2. Physical microphone and subjective spatial audio remain inherently manual.
3. `memory.json` migration has not started.
4. Controlled BELIEF producers and causal relationship reasons remain incomplete.
5. NPC-to-NPC knowledge and rumor propagation remain future work.
6. Multi-day and large-server simulation soak remains future scaling work; the new five-cycle bounded production soak is not a multi-day claim.
7. Historical Javadoc/deprecation warnings remain but do not currently block the verified build gates.

---

# Next optimal delivery step

Complete final documentation-head CI and change review for PR #114. Then prepare the focused installed canary package; do not publish until it passes.

Recommended sequence:

```text
final exact-head CI + soak + release dry-run
→ final change review
→ keep PR #114 draft until review evidence is complete
→ resolve next free version from repository/tags
→ build exact versioned dry-run candidate
→ install exact candidate on operator server/client
→ run the six minimal installed canaries
→ merge/release only on PASS
→ verify published JAR identity and one restart
→ begin additive legacy memory.json migration
```

The delivery boundary must not:

- use a snapshot JAR as release evidence;
- claim logical-client automation as graphical multi-client evidence;
- overwrite or migrate `memory.json`;
- weaken revision, authority, provider, credential or fail-closed CI policies;
- combine unrelated personality/social-graph implementation into the release.

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

> Open `docs/PROJECT_STATE.md`, `docs/ROADMAP.md`, `common/src/test/resources/acceptance/scenarios.tsv`, and `docs/livingworld/VALIDATION_M11_PHASE_E_E9.md`. Check current PR #114 head, all exact-head workflows, releases and tags. Complete the final review and installed canary/release boundary without weakening fail-closed suite selection or server authority.

A new session must:

1. read this file and `docs/ROADMAP.md`;
2. inspect current PR #114 head and mergeability;
3. inspect exact-head CI, release dry-run and production soak;
4. confirm the next free release version from current tags;
5. distinguish automated, candidate, exact-release and installed evidence;
6. complete installed canaries before publication;
7. begin legacy migration only after the release boundary;
8. update canonical documents after material progress.
