# VillAIgence Project State

> **Canonical current-state handoff.** Read this file before `docs/ROADMAP.md` when resuming work.
>
> Last reconciled: **2026-08-07**, during the Memory 2.0 clean-dialogue cutover in PR #119. `0.1.26+1.21.1` remains the latest official immutable release. The cutover is deliberately pre-1.0 and does **not** migrate the experimental legacy `memory.json` conversation store; installed acceptance must use a clean LivingWorld test state.
>
> Always distinguish unit/source-policy evidence, common integration, server GameTests, production-candidate evidence, exact-release evidence and installed operator-server/client evidence.

## Executive snapshot

VillAIgence is a Minecraft 1.21.1 MCA-derived mod evolving from AI-assisted villager dialogue into a persistent living-society simulation layer.

```text
repository:                         True-Ruslan/villAIgence
primary branch:                     1.21.1
Memory 2.0 clean cutover:           PR #119
Phase E merge:                      PR #114 / c51201d7a37b9d09c9a8cb490d1c56f3f6921c1f
0.1.26 release merge:               PR #115 / 40ce7cb77e9b9178fd96fd91025cee22ba686dc0
release-recovery merge:             PR #116 / ae551b81d221ce88ceebfce96b1038afa718da50
Java:                               21
primary distribution:               Fabric
NeoForge:                           compile compatibility required
latest official release:            0.1.26+1.21.1
latest release commit:              40ce7cb77e9b9178fd96fd91025cee22ba686dc0
latest release JAR SHA-256:          5728f0f1a57b4c268df9b73603539f09ca30945a2ba251e72a5169ab45ae0a53
next Memory 2.0 package:             controlled BELIEF producers + causal relationship memory
```

Current delivery state:

```text
0.1.x reliability/security baseline                    COMPLETE
Memory 2.0 foundation                                  SUBSTANTIALLY IMPLEMENTED
Memory 2.0 persistent-dialogue clean cutover            COMPLETE AT AUTOMATION LAYER
legacy memory.json migration                            CANCELLED BY DESIGN
MCA selective synchronization S1-S8                    COMPLETE
Operator Lore S9-S10c                                  COMPLETE
M11 Phases A-E                                          COMPLETE AT AUTOMATION LAYER
independent Phase E review                              COMPLETE — NO OPEN P0/P1/P2/P3
acceptance catalog                                      28 AUTOMATED / 6 MANUAL / 0 PLANNED
0.1.26 exact release gates                              COMPLETE
0.1.26 installed canaries                              5 PASS / 0 FAIL / 1 NOT TESTED
0.1.26 publication                                      COMPLETE
release-recovery automation                             COMPLETE / VERSION-AWARE
clean-world installed cutover acceptance                PENDING EXACT CANDIDATE
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
11. Persistence cutovers must have an explicit rollout/data-compatibility contract; compatibility work is not built automatically when no supported data population requires it.
12. Exact release identity must match tag, filename, embedded metadata and manifest.
13. Published artifacts must be byte-identical to the exact artifact accepted by the release gate.
14. Automated logical-client evidence never silently becomes installed multi-client evidence.
15. Unknown, unsafe, protected and persistence-store CI changes fail closed to the complete mandatory matrix.
16. Release recovery may repair missing metadata/assets only from an already-existing immutable tag commit; it must never create, delete or move that release tag.
17. Release recovery validates the persistence matrix defined by the immutable target release itself; it must not impose the current branch's store count on historical tags.

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

Compatibility-sensitive `mca`, `LivingWorld` and `livingworld` identifiers remain unchanged.

Current active world-local stores include:

```text
<world>/livingworld/memory2.json
<world>/livingworld/semantic-memory.json
<world>/livingworld/events.json
<world>/livingworld/relationships.json
<world>/livingworld/voices.json
<world>/livingworld/operator-lore.json
```

The current production corruption/recovery matrix covers these five auxiliary persistent stores:

```text
memory2.json
semantic-memory.json
relationships.json
voices.json
operator-lore.json
```

`events.json` is authoritative factual event history with its own validation path.

The experimental pre-0.2 `<world>/livingworld/memory.json` conversation store has been removed from the current runtime and recovery matrix. No importer, dual reader or checkpointed migration is planned. A clean LivingWorld test state is the accepted rollout boundary for this pre-1.0 development cutover.

Historical release `0.1.26` remains immutable and still describes its own older six-store validation boundary. Current release-recovery control is intentionally version-aware so historical release recovery continues to execute the contracts present at the target tag commit.

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

Security findings `SEC-001` through `SEC-009` remain closed.

The repository security policy permits `contents: write` only in the two release-critical jobs that require it:

```text
livingworld-release.yml           → github-release
livingworld-release-recovery.yml  → restore-github-release
```

Both workflows default to `contents: read`.

## Voice orchestration and transport

Automated evidence covers:

- one monotonic deadline across queue handoff, STT, Chat retries and optional TTS;
- exactly-once dialogue and relationship persistence;
- deterministic mock-provider STT/Chat/TTS;
- real Simple Voice Chat Opus encode/decode;
- loss concealment, duplicate/order rejection and bounded PCM;
- encoder/decoder closure and cancellation;
- repeated voice transport across constrained-heap production restarts.

Installed `VAI-AI-006` passed for `0.1.26` after Chat was switched to `google/gemini-2.5-flash-lite`. Physical microphone permissions, real client UDP routing, audible spatial playback and subjective quality remain manual evidence categories.

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

### Persistent-dialogue clean cutover

The current 0.2 development package removes the legacy persistent conversation store and makes Memory 2.0 the sole persistent dialogue source.

Successful dialogue path:

```text
usable text/voice AI result
→ ChatAI post-success boundary
→ Memory2DialogueLifecycle
→ structured DIALOGUE MemoryEvent
→ memory2.json
```

Each new DIALOGUE event carries both:

- a bounded human-readable episodic `summary`;
- a structured `DialogueExchange(playerMessage, npcReply)` payload.

Prompt reconstruction never parses the summary. `Memory2DialogueHistory` filters **before limiting** by:

```text
exact owner NPC
+ DIALOGUE type
+ exact NPC/player participants
+ structured dialogue payload
```

Eligible exchanges are selected newest-first, restored to chronological order, rendered as alternating `user`/`assistant` messages, and passed through the existing Working Memory hard bounds. Newer ACTION/OBSERVATION/RELATIONSHIP_CHANGE events therefore cannot starve recent dialogue merely by occupying the generic event limit.

Old summary-only DIALOGUE events without a structured payload are ignored by prompt-history reconstruction rather than guessed or parsed.

The old `ConversationMemoryStore`, `MemoryMessage`, and their dedicated tests are removed. `PersistentChatMemory` remains only as a temporary **no-storage compatibility adapter** for the inherited `OpenAIChatAI` call surface: it reads Memory 2.0 only and its append methods do not perform a second persistent write. A source-policy test prevents reintroduction of the old store/path or a second writer.

Current automated cutover evidence includes:

- structured round-trip with delimiter-like text;
- exact NPC/player isolation;
- chronological user/assistant reconstruction;
- filter-before-limit behavior under newer non-dialogue events;
- Working Memory bounds;
- old summary-only dialogue exclusion;
- Memory 2.0 startup/restart stability;
- five-store destructive corruption/recovery;
- Fabric and NeoForge builds;
- full production/JVM soak and existing gameplay acceptance.

The remaining installed boundary is deliberately separate: deploy an exact candidate to a **clean test-world/LivingWorld state**, then confirm text/voice dialogue recall and restart on the operator server before calling the cutover installed-accepted.

Remaining Memory 2.0 product work:

- controlled BELIEF producers;
- trustworthy causal relationship-change reasons;
- improved long-horizon and multi-day recall evidence;
- NPC-to-NPC knowledge transfer;
- rumor propagation with uncertainty, provenance and distortion.

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

Canonical catalog remains:

```text
common/src/test/resources/acceptance/scenarios.tsv
34 total scenarios
28 AUTOMATED
6 MANUAL_CANARY
0 PLANNED
```

M11 Phases A-E remain complete at the automation layer. Phase E merge:

```text
PR:            #114
merge commit:  c51201d7a37b9d09c9a8cb490d1c56f3f6921c1f
```

The Memory 2.0 cutover preserves rather than weakens the existing release-risk automation: exact production startup/restart, lifecycle, persistence recovery, Fabric GameTests, Fabric/NeoForge builds, package smoke, security policy and bounded production soak remain mandatory when selected.

---

# Completed official release boundary — 0.1.26+1.21.1

Official release remains:

```text
tag:                         0.1.26+1.21.1
release commit:              40ce7cb77e9b9178fd96fd91025cee22ba686dc0
release PR:                  #115
JAR SHA-256:                 5728f0f1a57b4c268df9b73603539f09ca30945a2ba251e72a5169ab45ae0a53
dependency manifest SHA-256: b16a7b842776d44ed21cad1b56cee63aadc782ada457c108c5107c483aab5816
```

Installed canaries on the exact 0.1.26 candidate bytes:

```text
VAI-BOOT-002    PASS
VAI-NAV-001     PASS
VAI-GAME-001    PASS
VAI-GAME-003    PASS
VAI-AI-006      PASS
VAI-CONCUR-004  NOT TESTED / DEFERRED

Total: 5 PASS / 0 FAIL / 1 NOT TESTED
```

Canonical installed evidence:

```text
docs/livingworld/VALIDATION_0.1.26_INSTALLED_CANARIES.md
```

Canonical final publication evidence:

```text
docs/livingworld/VALIDATION_0.1.26_RELEASE_COMPLETE.md
```

The publication outage and immutable-release recovery remain closed through PR #116 / Recovery #4. Current recovery control must remain capable of rebuilding historical 0.1.26 from its own immutable source contracts rather than silently applying later persistence assumptions.

---

# Known gaps and technical debt

1. `VAI-CONCUR-004` real two-graphical-client conflict presentation remains deferred and must stay labeled NOT TESTED until actually exercised.
2. Physical microphone/spatial-audio evidence remains inherently installed/manual even though `VAI-AI-006` passed for 0.1.26.
3. Memory 2.0 clean-world installed acceptance has not yet been claimed; automated exact-JAR evidence is not a substitute for operator deployment.
4. `PersistentChatMemory` is now a no-storage compatibility façade and can be renamed/removed when the inherited `OpenAIChatAI` call surface is cleaned up; this is API cleanup, not data migration.
5. Historical `persistentMemoryMaxMessages` / `persistentMemoryMaxCharsPerMessage` config fields remain deserializable but no longer size a separate persistent conversation store.
6. Controlled BELIEF producers and authoritative causal relationship reasons remain incomplete.
7. NPC-to-NPC knowledge and rumor propagation remain future work.
8. Multi-day and large-server simulation soak remains future scaling work; the existing bounded soak is not a multi-day claim.
9. Historical Javadoc/deprecation warnings remain but do not block verified gates.

---

# Next optimal delivery step

After the Memory 2.0 clean cutover is merged and an exact clean-world installed candidate is accepted, the next product package should **not** return to legacy migration. The next useful 0.2 work is controlled beliefs and causal social memory.

Recommended progression:

```text
controlled BELIEF producer contract
→ explicit provenance/admission policy for PLAYER_TOLD and NPC_TOLD
→ deterministic consolidation/retrieval tests
→ trustworthy relationship-change reason model
→ bind validated reasons to RELATIONSHIP_CHANGE memory
→ long-horizon recall scenarios
→ NPC-to-NPC knowledge transfer
→ rumor propagation with provenance/uncertainty/distortion
```

Do not:

- restore or import `memory.json` unless a new supported-user requirement justifies a dedicated compatibility project;
- parse DIALOGUE summaries to recover prompt roles;
- convert dialogue into FACT without server-observed evidence;
- regenerate NPC identity or ownership from provider output;
- weaken authority, revision, credential, deadline, release-recovery or fail-closed CI policies;
- claim automated exact-JAR evidence as installed clean-world acceptance;
- claim logical-client automation as graphical `VAI-CONCUR-004` evidence.

---

# New-session handoff protocol

Preferred resume prompt after this package:

> Open `docs/PROJECT_STATE.md`, `docs/ROADMAP.md`, `docs/livingworld/MEMORY_2.md`, the latest Memory 2.0 clean-cutover validation record, and the current acceptance catalog. Verify the latest repository/CI state. Treat `0.1.26+1.21.1` as the latest official release unless newer evidence exists. Do not rebuild a legacy `memory.json` migration unless a supported-data requirement has changed. Continue Memory 2.0 with controlled BELIEF producers and causal relationship memory, while keeping `VAI-CONCUR-004` explicitly deferred until a real two-graphical-client test is available.

A new session must:

1. read this file and `docs/ROADMAP.md`;
2. inspect current `1.21.1` head and open PRs;
3. distinguish latest official release from unreleased development work;
4. distinguish automated, exact-release and installed evidence;
5. keep `VAI-CONCUR-004` as NOT TESTED until actually performed;
6. preserve the Memory 2.0 truth boundary and exact NPC/player isolation;
7. keep release recovery compatible with the immutable target release's own contracts;
8. update canonical documents after material progress.
