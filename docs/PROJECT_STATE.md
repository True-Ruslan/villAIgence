# VillAIgence Project State

> **Canonical current-state handoff.** Read this file before `docs/ROADMAP.md` when resuming work.
>
> Last reconciled: **2026-08-06**, after implementation, independent review and exact-head validation of M11 Phase E in draft PR #114.
>
> Always distinguish unit/source-policy evidence, common integration, server GameTests, production-candidate evidence, exact-release dry-run evidence and installed operator-server/client evidence.

## Executive snapshot

VillAIgence is a Minecraft 1.21.1 MCA-derived mod evolving from AI-assisted villager dialogue into a persistent living-society simulation layer.

```text
repository:                         True-Ruslan/villAIgence
primary branch:                     1.21.1
active implementation PR:           #114 — draft, unmerged
validated runtime implementation:   78d7961632501b038d233dd662c62384d81a7c3b
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
independent PR review                                   COMPLETE — NO OPEN P0/P1/P2/P3
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
15. Unknown, unsafe, protected and persistence-store CI changes fail closed to the complete mandatory matrix.

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

Automated evidence covers:

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

Canonical catalog:

```text
common/src/test/resources/acceptance/scenarios.tsv
34 total scenarios
28 AUTOMATED
6 MANUAL_CANARY
0 PLANNED
```

## Phase A — risk catalog and GameTests

Complete. Risk-to-proof mappings are explicit and source-controlled.

## Phase B — exact production-JAR startup/restart

Complete:

- exact remapped Fabric candidate staged outside Loom/dev runtime;
- pinned installer/runtime dependencies;
- deterministic manifest and hashes;
- real JVM startup, controlled stop/save and restart;
- forbidden startup-signature scan;
- six canonical stores discovered exactly once;
- fixture classes excluded from the public JAR.

## Phase C — provider and voice orchestration

Complete:

- production Chat/STT/TTS clients against literal-loopback providers;
- no external provider or real credential in CI;
- shared retry and full-turn deadlines;
- bounded streaming reads;
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
- nightly, selected-main-CI and release evidence.

### E4/E5 — authenticated transport and concurrency

- authenticated text cannot spoof player or NPC ownership;
- provider, persistence and response effects occur exactly once;
- two Operator Lore sessions cannot receive each other's responses;
- stale write returns conflict and reviewed retry succeeds once.

### E6 — production voice transport

- real Opus codec and transport lifecycle;
- loss, duplicate and ordering behavior;
- bounded PCM and resource closure;
- hardware-independent production evidence.

### E7/E8 — gameplay and navigation matrix

Sixteen required Fabric GameTests include gifts, deterministic fishing, mounted archer ownership, water escape, obstacle reroute, ladder ascent/descent, closed-door passage and tombstone lifecycle/replay controls.

### E9 — fail-closed selection and production soak

Implemented:

- deterministic path-to-risk selector;
- five explicit suites: fast, server, production, recovery and package;
- release always selects all suites;
- protected, empty, unsafe and unknown changes fail closed to all;
- all production LivingWorld `*Store.java` changes select recovery;
- main CI executes only explicitly selected groups;
- weekly/relevant-PR/manual production soak;
- three authenticated concurrency repetitions at 512 MiB;
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

# Independent review and exact code-head evidence

Independent review covered the complete PR diff, CI/release/soak, persistence, server authority, lifecycle and packaging boundaries.

One P2 finding was found and fixed: canonical store-class changes did not directly request the main-CI recovery suite. A RED regression produced seven failures, the selector was corrected for current and future LivingWorld `*Store.java` implementations, and the full matrix was rerun.

Open review findings after the fix:

```text
P0: 0
P1: 0
P2: 0
P3: 0
```

Validated implementation head:

```text
78d7961632501b038d233dd662c62384d81a7c3b
```

Fresh mandatory results:

```text
VillAIgence CI:                1721 / 31083451312 — PASS
Java Pull Request CI:          1107 / 31083451053 — PASS
Repository security policy:   1347 / 31083451124 — PASS
Supply-chain verification:    167  / 31083451252 — PASS
Production Soak:              14   / 31083451193 — PASS
GitHub Release dry-run:        333  / 31083451015 — PASS
release publication:                              SKIPPED
```

Inspected soak artifact:

```text
production-soak-14
artifact id: 8960538615
digest: sha256:6567b4a8cad895945a204a8a64b64e7a9078ab989b6d6e7db3c242a56fbd1c83
```

Report evidence:

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
production-server-acceptance-333  artifact 8960504586
persistence-recovery-333          artifact 8960600598
villaigence-fabric-package        artifact 8960636786
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

1. `VAI-BOOT-002` — exact candidate/released JAR starts on the operator server.
2. `VAI-NAV-001` — two ordinary MCA NPC brains visibly escape water in an installed world.
3. `VAI-GAME-001` — an installed client visibly addresses the selected NPC and renders one response.
4. `VAI-GAME-003` — a real player performs Silk Touch grave pickup/placement/restart without loss or duplication.
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
6. Multi-day and large-server simulation soak remains future scaling work; the five-cycle bounded soak is not a multi-day claim.
7. Historical Javadoc/deprecation warnings remain but do not block verified build gates.

---

# Next optimal delivery step

Complete final documentation-head workflows for PR #114. Then prepare the focused installed canary package; do not publish until it passes.

Recommended sequence:

```text
final documentation-head CI + soak + release dry-run
→ keep PR #114 draft until all checks are green
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

> Open `docs/PROJECT_STATE.md`, `docs/ROADMAP.md`, `common/src/test/resources/acceptance/scenarios.tsv`, and `docs/livingworld/VALIDATION_M11_PHASE_E_E9.md`. Check current PR #114 head, exact-head workflows, releases and tags. Prepare the six installed canaries and exact release boundary without weakening fail-closed suite selection or server authority.

A new session must:

1. read this file and `docs/ROADMAP.md`;
2. inspect current PR #114 head and mergeability;
3. inspect exact-head CI, release dry-run and production soak;
4. confirm the next free release version from current tags;
5. distinguish automated, candidate, exact-release and installed evidence;
6. complete installed canaries before publication;
7. begin legacy migration only after the release boundary;
8. update canonical documents after material progress.
