# VillAIgence Project State

> **Canonical current-state handoff.** Read this file before `docs/ROADMAP.md` when resuming work.
>
> Last reconciled: **2026-08-06**, after M11 Phase E was merged through PR #114 and the `0.1.26+1.21.1` release-request branch was prepared.
>
> Always distinguish unit/source-policy evidence, common integration, server GameTests, production-candidate evidence, exact-release evidence and installed operator-server/client evidence.

## Executive snapshot

VillAIgence is a Minecraft 1.21.1 MCA-derived mod evolving from AI-assisted villager dialogue into a persistent living-society simulation layer.

```text
repository:                         True-Ruslan/villAIgence
primary branch:                     1.21.1
current primary head:               c51201d7a37b9d09c9a8cb490d1c56f3f6921c1f
Phase E merge:                      PR #114 / c51201d7a37b9d09c9a8cb490d1c56f3f6921c1f
release candidate branch:           release/0.1.26+1.21.1
Java:                               21
primary distribution:               Fabric
NeoForge:                           compile compatibility required
latest official release:            0.1.25+1.21.1
latest release commit:              588cc676d356271c4cf74eb21131f6d071476e48
next requested candidate:            0.1.26+1.21.1
```

Current delivery state:

```text
0.1.x reliability/security baseline                    COMPLETE
Memory 2.0 foundation                                  SUBSTANTIALLY IMPLEMENTED
MCA selective synchronization S1-S8                    COMPLETE
Operator Lore S9-S10c                                  COMPLETE
M11 Phases A-E                                          COMPLETE AT AUTOMATION LAYER
independent Phase E review                              COMPLETE — NO OPEN P0/P1/P2/P3
acceptance catalog                                      28 AUTOMATED / 6 MANUAL / 0 PLANNED
0.1.26 exact release-request dry run                    PENDING
0.1.26 installed graphical/physical canaries            PENDING
0.1.26 publication                                      BLOCKED UNTIL INSTALLED PASS
legacy memory.json migration                            AFTER RELEASE BOUNDARY
```

PR #114 is merged. No `0.1.26+1.21.1` tag or GitHub Release exists yet.

---

# Product and architecture

Target product:

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
2. Mutable game state used asynchronously is captured into immutable bounded context first.
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
14. Automated logical-client evidence never silently becomes installed multi-client evidence.
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

---

# Implemented systems

## Provider, parsing and security

Implemented and retained:

- OpenAI-compatible Chat, STT and TTS;
- OpenRouter-compatible Chat;
- endpoint normalization and credential-family binding;
- authenticated redirect blocking;
- remote plaintext rejection except explicit literal-loopback development mode;
- bounded response/error/verification bodies;
- controlled null, empty, malformed and provider-error handling;
- retry without duplicate persistent/gameplay effects;
- voice-duration and aggregate PCM limits;
- verified dependencies, pinned Actions and repository security policy;
- diagnostics without secrets, prompts, transcripts or hidden reasoning.

Security findings `SEC-001` through `SEC-009` are closed.

## Voice orchestration and transport

Automated evidence covers:

- one monotonic deadline across queue handoff, STT, Chat retries and optional TTS;
- exactly-once dialogue and relationship persistence;
- deterministic mock-provider STT/Chat/TTS;
- real Simple Voice Chat Opus encode/decode;
- loss concealment, duplicate/order rejection and bounded PCM;
- encoder/decoder closure and cancellation;
- repeated voice transport across constrained-heap production restarts.

Physical microphone permissions, real client UDP routing, audible spatial playback and subjective quality remain manual.

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
- consolidation and source union;
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

- schema-versioned world-local store;
- WORLD, PLAYER, VILLAGER and VILLAGE scopes;
- immutable prompt-context capture;
- permission-level-2 server-authoritative API;
- server-resolved identity and scope;
- SHA-256 optimistic revisions;
- bounded validation;
- explicit success/conflict/error statuses;
- multiline editor and close confirmations;
- stale-generation rejection;
- authenticated two-session owner-bound transport;
- explicit stale conflict and reviewed retry.

## Selective MCA corrections

Implemented and retained:

- tombstone item/entity-data integrity;
- pre-serialization inventory ownership transfer;
- UUID-preserving conversion and resurrection replay guard;
- occupied HOME-bed rejection;
- water, ladder, obstacle and door navigation;
- progress watchdog and staggered pathfinding;
- graveyard mourning lifecycle;
- gift interaction semantics;
- fishing/AquaCulture compatibility;
- stable mounted archer control and NPC-owned projectile evidence;
- exactly one filled portable grave;
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

## Phases A-E

Complete at the automation layer:

- **A:** risk catalog and server GameTests;
- **B:** exact production-JAR startup, controlled stop/save and restart;
- **C:** provider and complete voice-turn orchestration;
- **D:** authenticated text and Operator Lore concurrency/client-state acceptance;
- **E0:** configuration-cache-safe production staging;
- **E1/E2:** duplicate-identity prevention and real death/grave/resurrection across two JVMs;
- **E3:** six-case corrupt persistence recovery;
- **E4/E5:** authenticated text and owner-bound Operator Lore sessions;
- **E6:** production Simple Voice Chat transport;
- **E7/E8:** gifts, fishing, mounted combat, water, obstacles, ladders and doors;
- **E9:** fail-closed suite selection and bounded production soak.

Phase E merge:

```text
PR:            #114
merge commit:  c51201d7a37b9d09c9a8cb490d1c56f3f6921c1f
```

Independent review found one P2 selector gap for canonical `*Store.java` changes. Seven focused RED cases reproduced it; the selector was corrected and all mandatory workflows passed afterward.

Final exact PR-head evidence before merge:

```text
head:                         a8f1ce741904adc67db3b804a1c568d30b91c217
VillAIgence CI:              1725 / 31084661119 — PASS
Java Pull Request CI:        1111 / 31084661085 — PASS
Repository security policy: 1355 / 31084661096 — PASS
Supply-chain verification:  171  / 31084661090 — PASS
Production Soak:            18   / 31084661091 — PASS
GitHub Release dry-run:      337  / 31084661176 — PASS
release publication:                              SKIPPED
```

Inspected soak evidence:

```text
artifact: production-soak-18
artifact id: 8961024036
five clean JVM exits
512 MiB production heap
one live lifecycle NPC every cycle
voice PASS every cycle
peak PCM 7680 bytes every cycle
six persistent-store hashes unchanged across all cycles
```

---

# Release boundary

Latest official release:

```text
tag:     0.1.25+1.21.1
commit:  588cc676d356271c4cf74eb21131f6d071476e48
```

The next exact candidate is requested on:

```text
branch:  release/0.1.26+1.21.1
file:    docs/releases/NEXT_RELEASE.txt
value:   0.1.26+1.21.1
notes:   docs/releases/0.1.26+1.21.1.md
```

A release-request PR is a non-publishing exact dry run. It must remain unmerged until all six installed canaries pass. Merging that PR causes the exact merge commit to run the complete release gate and publish only after PASS.

## Remaining manual canaries

1. `VAI-BOOT-002` — exact candidate starts on the operator server and a client connects.
2. `VAI-NAV-001` — two ordinary MCA NPC brains visibly escape reachable water.
3. `VAI-GAME-001` — an installed client addresses the selected NPC and renders one response.
4. `VAI-GAME-003` — real Silk Touch pickup/placement/restart/resurrection preserves one UUID, name and inventory.
5. `VAI-AI-006` — physical microphone, client UDP routing and audible spatial playback.
6. `VAI-CONCUR-004` — two graphical clients visibly expose and resolve an Operator Lore conflict without losing drafts.

These checks must remain small and must not repeat deterministic persistence, codec, retry, recovery or logical-session internals already automated.

---

# Known gaps and technical debt

1. Exact `0.1.26` dry-run and installed canaries are pending.
2. Physical microphone and subjective spatial audio remain inherently manual.
3. `memory.json` migration has not started.
4. Controlled BELIEF producers and causal relationship reasons remain incomplete.
5. NPC-to-NPC knowledge and rumor propagation remain future work.
6. Multi-day and large-server simulation soak remains future scaling work; the five-cycle soak is not a multi-day claim.
7. Historical Javadoc/deprecation warnings remain but do not block verified gates.

---

# Next optimal delivery step

```text
open 0.1.26 release-request PR
→ complete exact non-publishing dry run
→ download and record exact candidate JAR + SHA-256
→ install the same JAR on operator server/client
→ run six minimal installed canaries
→ merge release request only on PASS
→ verify official assets and byte identity
→ perform one post-release restart
→ update canonical evidence
→ begin additive legacy memory.json migration
```

Do not:

- use a snapshot or local JAR as release evidence;
- claim logical-client automation as graphical multi-client evidence;
- publish before installed canaries pass;
- overwrite or migrate `memory.json` before the release boundary;
- weaken authority, revision, credential, deadline or fail-closed CI policies.

---

# New-session handoff protocol

Preferred resume prompt:

> Open `docs/PROJECT_STATE.md`, `docs/ROADMAP.md`, `docs/RELEASING.md`, `common/src/test/resources/acceptance/scenarios.tsv`, and `docs/livingworld/VALIDATION_M11_PHASE_E_E9.md`. Check branch `release/0.1.26+1.21.1`, its PR and exact dry-run artifacts. Continue from the six installed canaries without weakening server authority or fail-closed acceptance.

A new session must:

1. read this file and `docs/ROADMAP.md`;
2. inspect current `1.21.1` head and open PRs;
3. inspect exact candidate CI, artifacts and tag availability;
4. distinguish automated, candidate, exact-release and installed evidence;
5. complete installed canaries before publication;
6. begin legacy migration only after release verification;
7. update canonical documents after material progress.
