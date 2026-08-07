# VillAIgence Project State

> **Canonical current-state handoff.** Read this file before `docs/ROADMAP.md` when resuming work.
>
> Last reconciled: **2026-08-07**, after `0.1.26+1.21.1` was published, its interrupted GitHub Actions publication was recovered through PR #116, and the final assets were verified byte-for-byte.
>
> Always distinguish unit/source-policy evidence, common integration, server GameTests, production-candidate evidence, exact-release evidence and installed operator-server/client evidence.

## Executive snapshot

VillAIgence is a Minecraft 1.21.1 MCA-derived mod evolving from AI-assisted villager dialogue into a persistent living-society simulation layer.

```text
repository:                         True-Ruslan/villAIgence
primary branch:                     1.21.1
Phase E merge:                      PR #114 / c51201d7a37b9d09c9a8cb490d1c56f3f6921c1f
0.1.26 release merge:               PR #115 / 40ce7cb77e9b9178fd96fd91025cee22ba686dc0
release-recovery merge:             PR #116 / ae551b81d221ce88ceebfce96b1038afa718da50
Java:                               21
primary distribution:               Fabric
NeoForge:                           compile compatibility required
latest official release:            0.1.26+1.21.1
latest release commit:              40ce7cb77e9b9178fd96fd91025cee22ba686dc0
latest release JAR SHA-256:          5728f0f1a57b4c268df9b73603539f09ca30945a2ba251e72a5169ab45ae0a53
next development package:           additive legacy memory.json migration
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
0.1.26 exact release gates                              COMPLETE
0.1.26 installed canaries                              5 PASS / 0 FAIL / 1 NOT TESTED
0.1.26 publication                                      COMPLETE
release-recovery automation                             COMPLETE
legacy memory.json migration                            NEXT
```

`VAI-CONCUR-004` remains explicitly **NOT TESTED / DEFERRED** because a second graphical client was unavailable. It is not represented as PASS.

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
16. Release recovery may repair missing metadata/assets only from an already-existing immutable tag commit; it must never create, delete or move that release tag.

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
- verified dependencies, pinned Actions and deterministic repository security policy;
- diagnostics without secrets, prompts, transcripts or hidden reasoning.

Security findings `SEC-001` through `SEC-009` are closed.

The repository security policy now permits `contents: write` only in two release-critical jobs:

```text
livingworld-release.yml           → github-release
livingworld-release-recovery.yml  → restore-github-release
```

Both workflows default to `contents: read`, and focused policy tests reject write access in any other workflow/job.

## Voice orchestration and transport

Automated evidence covers:

- one monotonic deadline across queue handoff, STT, Chat retries and optional TTS;
- exactly-once dialogue and relationship persistence;
- deterministic mock-provider STT/Chat/TTS;
- real Simple Voice Chat Opus encode/decode;
- loss concealment, duplicate/order rejection and bounded PCM;
- encoder/decoder closure and cancellation;
- repeated voice transport across constrained-heap production restarts.

Installed `VAI-AI-006` passed for `0.1.26` after Chat was switched to `google/gemini-2.5-flash-lite`. Physical microphone permissions, real client UDP routing, audible spatial playback and subjective quality remain manual evidence categories by nature.

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

Server-side two-session conflict/retry semantics are automated. Real installed two-graphical-client presentation remains the deferred `VAI-CONCUR-004` boundary.

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

The release/recovery boundary subsequently re-executed exact identity, production startup/restart, six-case persistence recovery, Fabric GameTests, Fabric/NeoForge builds, package smoke and byte-identity on the immutable `0.1.26` release commit.

---

# Completed release boundary — 0.1.26+1.21.1

Official release:

```text
tag:                         0.1.26+1.21.1
release commit:              40ce7cb77e9b9178fd96fd91025cee22ba686dc0
release PR:                  #115
JAR SHA-256:                 5728f0f1a57b4c268df9b73603539f09ca30945a2ba251e72a5169ab45ae0a53
dependency manifest SHA-256: b16a7b842776d44ed21cad1b56cee63aadc782ada457c108c5107c483aab5816
```

Installed canaries on the exact candidate bytes:

```text
VAI-BOOT-002    PASS
VAI-NAV-001     PASS
VAI-GAME-001    PASS
VAI-GAME-003    PASS
VAI-AI-006      PASS
VAI-CONCUR-004  NOT TESTED / DEFERRED

Total: 5 PASS / 0 FAIL / 1 NOT TESTED
```

The operator explicitly accepted deferring `VAI-CONCUR-004` because a second graphical client was unavailable. Automated authenticated two-session coverage remains green, but it is not substituted for graphical installed evidence.

Canonical installed evidence:

```text
docs/livingworld/VALIDATION_0.1.26_INSTALLED_CANARIES.md
```

Canonical final publication evidence:

```text
docs/livingworld/VALIDATION_0.1.26_RELEASE_COMPLETE.md
```

## Publication outage and recovery

The initial merge-triggered publication encountered a GitHub Actions service outage after the immutable tag/Release record existed but before assets were complete. Recovery PR #116 added a fail-closed immutable-release recovery workflow.

```text
recovery PR:             #116
recovery control commit: ae551b81d221ce88ceebfce96b1038afa718da50
recovery workflow:       VillAIgence Release Recovery #4
recovery run id:         31154864224
result:                  PASS
```

Recovery #4 passed the complete release gate on tag commit `40ce7cb77e9b9178fd96fd91025cee22ba686dc0`, restored the GitHub Release assets, downloaded those assets again and compared them byte-for-byte.

Final published assets:

```text
villaigence-fabric-0.1.26+1.21.1.jar
villaigence-fabric-0.1.26+1.21.1.jar.sha256
villaigence-dependencies-0.1.26+1.21.1.txt
```

The release tag remained immutable and still resolves to the exact release commit.

The published JAR is byte-identical to the installed candidate that passed startup/restart and grave/restart/resurrection acceptance. No separate temporal claim is made that an additional operator restart happened only after the GitHub assets became visible.

---

# Known gaps and technical debt

1. `VAI-CONCUR-004` real two-graphical-client conflict presentation remains deferred and must stay labeled NOT TESTED until actually exercised.
2. Physical microphone/spatial-audio evidence remains inherently installed/manual even though `VAI-AI-006` passed for 0.1.26.
3. Additive legacy `memory.json` migration has not started.
4. Controlled BELIEF producers and causal relationship reasons remain incomplete.
5. NPC-to-NPC knowledge and rumor propagation remain future work.
6. Multi-day and large-server simulation soak remains future scaling work; the existing bounded soak is not a multi-day claim.
7. Historical Javadoc/deprecation warnings remain but do not block verified gates.

---

# Next optimal delivery step

The release boundary is closed. The next package is the additive legacy `memory.json` migration.

Required progression:

```text
inventory current memory.json shapes and ownership
→ define migration schema/checkpoint/version contract
→ implement deterministic dry-run parser/report
→ add RED duplicate/rerun/partial-failure/ownership tests
→ create backup before any canonical mutation
→ perform bounded additive import with deterministic event IDs
→ preserve DIALOGUE as episodic; never auto-upgrade to FACT
→ verify atomic writes and rollback
→ verify idempotent rerun and same-world restart
→ retain legacy reads until cutover acceptance
→ consider cutover only after evidence
```

Do not:

- destructively rewrite or delete `memory.json` during migration development;
- convert dialogue into FACT without server-observed evidence;
- regenerate NPC identity or ownership during import;
- remove legacy reads before migration/cutover acceptance;
- claim logical-client automation as graphical `VAI-CONCUR-004` evidence;
- weaken authority, revision, credential, deadline, release-recovery or fail-closed CI policies.

---

# New-session handoff protocol

Preferred resume prompt:

> Open `docs/PROJECT_STATE.md`, `docs/ROADMAP.md`, `docs/livingworld/VALIDATION_0.1.26_RELEASE_COMPLETE.md`, `docs/RELEASING.md`, and `common/src/test/resources/acceptance/scenarios.tsv`. Verify the latest repository/CI state, then continue with the additive legacy `memory.json` migration. Preserve the immutable 0.1.26 release evidence and keep `VAI-CONCUR-004` explicitly deferred until a real two-graphical-client test is available.

A new session must:

1. read this file and `docs/ROADMAP.md`;
2. inspect current `1.21.1` head and open PRs;
3. treat `0.1.26+1.21.1` as the latest verified official release unless newer repository evidence exists;
4. distinguish automated, exact-release and installed evidence;
5. keep `VAI-CONCUR-004` as NOT TESTED until actually performed;
6. implement legacy migration only additively, deterministically and reversibly;
7. update canonical documents after material progress.
