# VillAIgence Project State

> **Canonical current-state handoff.** Read this file before `docs/ROADMAP.md` when resuming work. Read root `CHANGELOG.md` for product/release history.
>
> Last reconciled: **2026-08-08**, after bounded `PLAYER_TOLD` Semantic Memory extraction merged through PR #125.
>
> Always distinguish source/unit evidence, common integration, server GameTests, production-candidate evidence, exact-release evidence, and installed operator server/client evidence.

## Executive snapshot

VillAIgence is a Minecraft 1.21.1 MCA-derived mod evolving from AI-assisted villager dialogue into a persistent living-society simulation layer.

```text
repository:                         True-Ruslan/villAIgence
primary branch:                     1.21.1
Java:                               21
primary distribution:               Fabric
NeoForge:                           compile compatibility required

latest product merge:               PR #125
latest product merge commit:        b60bcf3c296340946afb443da5cfb4c0d3a793a6
latest official release:            0.2.0+1.21.1
latest release commit:              e426f588efefa6aa48a6e536c4a998421bbda241
installed 0.2.0 candidate JAR SHA:   56293f86634b50b2def044429aac6f2cf0d197eb16ac1e60224708f7b3333aee

next product slice:                 trustworthy causal relationship memory
then:                               FACT > BELIEF retrieval regression evidence
then:                               long-horizon recall / NPC-to-NPC knowledge / rumors
```

Current delivery state:

```text
0.1.x reliability/security baseline                    COMPLETE
MCA selective synchronization S1-S8                    COMPLETE
Operator Lore S9-S10c                                  COMPLETE
M11 Phases A-E                                         COMPLETE AT AUTOMATION LAYER
acceptance catalog                                     28 AUTOMATED / 6 MANUAL / 0 PLANNED
release/recovery automation                            COMPLETE / VERSION-AWARE

0.2 Memory 2.0 foundation                              SUBSTANTIALLY IMPLEMENTED
Memory 2.0 persistent-dialogue clean cutover            COMPLETE / RELEASED
legacy memory.json migration                           CANCELLED BY DESIGN
0.2.0 clean-world installed acceptance                 7 PASS / 0 FAIL
controlled BELIEF admission contract                   COMPLETE / PR #123
bounded PLAYER_TOLD claim extraction                   COMPLETE / PR #125
causal relationship reasons                            NOT IMPLEMENTED
NPC-to-NPC knowledge / rumors                          NOT IMPLEMENTED
```

Installed boundaries that remain explicit:

```text
VAI-M2-INST-005  NOT TESTED / AUTOMATED EVIDENCE ONLY
VAI-CONCUR-004   NOT TESTED / DEFERRED
```

Neither is represented as PASS.

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

1. **The LLM is never authority.** Minecraft/server-owned state is truth.
2. Mutable game state used asynchronously is captured into immutable bounded context first.
3. Provider/model changes must not redefine persistent NPC identity, memory, relationships or voice.
4. Provider and auxiliary-persistence failures fail soft whenever safe.
5. Retry/replay paths must not duplicate dialogue, memory, relationship or gameplay effects.
6. **FACT requires `SYSTEM_OBSERVED` server-owned evidence.**
7. **BELIEF remains non-authoritative** and may use only `PLAYER_TOLD`, `NPC_TOLD` or `INFERRED` provenance.
8. Confidence never upgrades BELIEF into FACT.
9. Candidate extraction is not admission, and admission is not authority.
10. Operator Lore is explicit background context, not an observed current-world fact.
11. Current observed world facts override conflicting lore or recalled beliefs.
12. Clients never own permissions, target identity, file access, revisions or persistence mutations.
13. Compatibility work requires a supported-data reason; experimental pre-1.0 data is not automatically entitled to migration code.
14. Exact release identity must match tag, filename, embedded metadata and manifest.
15. Published artifacts must be byte-identical to the exact artifact accepted by the release gate.
16. Automated logical-client evidence never silently becomes installed multi-client evidence.
17. Unknown, unsafe, protected and persistence-store CI changes fail closed to the complete mandatory matrix.
18. Release recovery may repair metadata/assets only from an existing immutable release tag commit and never moves that tag.
19. Release recovery validates the persistence contract defined by the immutable target release itself.

Canonical AI/state flow:

```text
Minecraft/server state
→ immutable bounded snapshot
→ observed facts + operator lore + episodic/semantic memory
→ provider/LLM proposal
→ server validation/revalidation
→ server-owned mutation
→ persistent authoritative evidence
```

Semantic knowledge flow:

```text
SYSTEM_OBSERVED evidence
→ controlled FACT ingestion
→ FACT

persisted DIALOGUE/source evidence
→ bounded candidate text
→ server-owned provenance/source binding
→ BELIEF admission policy
→ BELIEF
```

The provider may suggest non-authoritative claim text. It never chooses truth class, source identity or gameplay authority.

---

# Identity, configuration and persistence

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

Current world-local stores:

```text
<world>/livingworld/memory2.json
<world>/livingworld/semantic-memory.json
<world>/livingworld/events.json
<world>/livingworld/relationships.json
<world>/livingworld/voices.json
<world>/livingworld/operator-lore.json
```

Current auxiliary corruption/recovery matrix:

```text
memory2.json
semantic-memory.json
relationships.json
voices.json
operator-lore.json
```

`events.json` is authoritative factual event history and has its own validation path.

The experimental pre-0.2 `<world>/livingworld/memory.json` conversation store is no longer part of current runtime or recovery. No importer, dual reader, checkpoint ledger or destructive migration is planned for the accepted pre-1.0 clean-state rollout boundary.

Current Semantic BELIEF extraction config:

```json
{
  "semanticBeliefExtractionEnabled": false,
  "semanticBeliefMaxCandidatesPerTurn": 3
}
```

Extraction is opt-in. Hard candidate count is `8`; statements are bounded to `240` Unicode code points. Existing config version remains `2` and missing fields receive safe defaults.

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
- bounded deadlines, queueing and PCM limits;
- verified dependencies and pinned Actions;
- deterministic repository security policy;
- diagnostics without secrets, prompts, transcripts or hidden reasoning.

Security findings `SEC-001` through `SEC-009` remain closed.

Only release-critical publication/recovery jobs receive `contents: write`; normal workflows default to read-only repository permissions.

## Voice orchestration and transport

Automated evidence covers:

- one monotonic deadline across queue handoff, STT, Chat retries and optional TTS;
- exactly-once dialogue/relationship effects;
- deterministic mock-provider STT/Chat/TTS;
- real Simple Voice Chat Opus encode/decode;
- loss concealment, duplicate/order rejection and bounded PCM;
- encoder/decoder closure and cancellation;
- constrained-heap repeated transport and production restart soak.

Physical microphone permission, real client UDP routing, audible spatial playback and subjective audio quality remain installed/manual evidence categories.

## Memory 2.0

Implemented foundation:

- immutable NPC-owned episodic events;
- DIALOGUE, OBSERVATION, ACTION and RELATIONSHIP_CHANGE;
- deterministic UUID idempotency and ordering;
- bounded per-NPC persistence and retrieval;
- text/voice DIALOGUE parity;
- bounded Working Memory;
- typed semantic FACT/BELIEF entries;
- controlled server-observed FACT ingestion;
- deterministic semantic consolidation and source union;
- deterministic pressure-based forgetting;
- NPC/player isolation and restart safety.

Truth boundary:

```text
FACT     → SYSTEM_OBSERVED only
BELIEF   → PLAYER_TOLD / NPC_TOLD / INFERRED only
DIALOGUE → episodic by default
```

### Persistent-dialogue clean cutover — released in 0.2.0

```text
usable text/voice AI result
→ ChatAI post-success boundary
→ Memory2DialogueLifecycle
→ structured DIALOGUE MemoryEvent
→ memory2.json
```

New DIALOGUE events carry both a human-readable summary and structured `DialogueExchange(playerMessage, npcReply)`. Prompt reconstruction uses the structured payload only, filters exact NPC/player DIALOGUE events before limiting, restores chronological user/assistant order and applies hard Working Memory bounds.

`ConversationMemoryStore` and `MemoryMessage` are removed. `PersistentChatMemory` remains only as a no-storage compatibility façade for the inherited AI call surface.

### Controlled BELIEF admission — PR #123

Fail-closed admission contract:

```text
PLAYER_TOLD → requires matching PLAYER_TOLD DIALOGUE source → BELIEF
NPC_TOLD    → requires matching NPC_TOLD DIALOGUE source    → BELIEF
INFERRED    → retains explicit persisted source event       → BELIEF
SYSTEM_OBSERVED through BELIEF API                           → REJECT
```

Owner/time/source identity comes from persisted evidence; callers cannot inject an arbitrary source-event list. Replay is idempotent and corroborating equivalent entries reuse deterministic source-union consolidation.

### Bounded PLAYER_TOLD extraction — PR #125

Current opt-in path:

```text
one structured OpenAI/OpenRouter response
→ sanitized visible NPC message
→ exact PLAYER_TOLD DIALOGUE persisted
→ bounded beliefCandidates strings
→ verify current player participates in source DIALOGUE
→ server-owned PLAYER_TOLD admission
→ semantic-memory.json
```

Guarantees:

- no second provider request;
- model supplies statement strings only;
- server owns NPC/player/source/provenance/kind;
- DIALOGUE must persist before BELIEF;
- malformed candidate metadata cannot discard a valid visible reply;
- candidates are NFKC/whitespace/control normalized, Unicode bounded and deduplicated;
- failed/empty/unusable provider responses create no BELIEF;
- exact replay is idempotent;
- distinct corroborating source dialogues union evidence;
- extraction cannot create FACT;
- classic/Inworld paths are not wired to this producer.

Independent review found and TDD-fixed one cross-player source-binding gap before merge: `PlayerToldBeliefLifecycle` now rejects a player UUID that is not a participant in the exact source DIALOGUE.

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
- authenticated two-session conflict/retry semantics.

Server-side two-session semantics are automated. Real installed two-graphical-client presentation remains deferred as `VAI-CONCUR-004`.

## Selective MCA corrections

Implemented and retained:

- tombstone item/entity-data integrity;
- pre-serialization inventory ownership transfer;
- UUID-preserving conversion and resurrection replay guard;
- occupied HOME-bed rejection;
- water, ladder, obstacle and door navigation;
- progress watchdog and staggered pathfinding;
- graveyard mourning lifecycle;
- gift semantics;
- fishing/AquaCulture compatibility;
- stable mounted archer control and NPC-owned projectile evidence;
- exactly one filled portable grave;
- exact loose-drop fallback when no tombstone captures the NPC.

---

# Automated acceptance and CI

Canonical acceptance catalog:

```text
34 total scenarios
28 AUTOMATED
6 MANUAL_CANARY
0 PLANNED
```

Permanent GitHub Actions surface is fail-closed to eight canonical workflows. The redundant PR Gradle workflow was removed; wrapper validation remains owned by supply-chain verification.

Runtime/product changes exercise the selected combination of:

- common/unit/provider tests;
- risk selector and Fabric server GameTests;
- Fabric + NeoForge builds;
- production acceptance contracts;
- exact production startup/save/restart;
- selected/current persistence recovery;
- package smoke;
- repository security policy;
- bounded production soak;
- release dry-run where selected.

PR #125 final exact-head evidence:

```text
head:                                  3fb65fc429fd7c4b91814fbd2437c88b40181db3
Repository security policy #1536:      SUCCESS / run 31246889365
VillAIgence CI #1901:                  SUCCESS / run 31246889394
VillAIgence Production Soak #84:       SUCCESS / run 31246889353
VillAIgence GitHub Release #418:       SUCCESS / run 31246889395
publication job:                       SKIPPED
```

Review hardening TDD evidence:

```text
RED head:               806b6bd9f5601fc4cefa4a14d64422b0b11b6f2e
VillAIgence CI #1897:   494 tests / 1 expected failure
failure:                rejectsPlayerThatIsNotAParticipantOfSourceDialogue
GREEN implementation:   1b7b5fb75e7eb591a4b105ea3163c0c177d6b780
```

Final independent review after the fix:

```text
P0: 0
P1: 0
P2: 0
open review threads: 0
```

---

# Current official release boundary — 0.2.0+1.21.1

```text
tag:                     0.2.0+1.21.1
release commit:          e426f588efefa6aa48a6e536c4a998421bbda241
installed candidate SHA: 56293f86634b50b2def044429aac6f2cf0d197eb16ac1e60224708f7b3333aee
```

Installed clean-world Memory 2.0 result:

```text
VAI-M2-INST-001  PASS
VAI-M2-INST-002  PASS
VAI-M2-INST-003  PASS
VAI-M2-INST-004  PASS
VAI-M2-INST-006  PASS
VAI-M2-INST-007  PASS
VAI-M2-INST-008  PASS

Required total: 7 PASS / 0 FAIL
VAI-M2-INST-005: NOT TESTED / AUTOMATED EVIDENCE ONLY
VAI-CONCUR-004:  NOT TESTED / DEFERRED
```

The physical voice seed `silver-fox-482` was accepted by STT as `SilverFox482`; the accepted transcript persisted and survived restart. This is a non-blocking STT normalization observation, not a Memory 2.0 persistence failure.

Canonical installed evidence:

```text
docs/livingworld/VALIDATION_0.2.0_CLEAN_WORLD_INSTALLED.md
```

---

# Changelog governance

Root `CHANGELOG.md` is the canonical product/release changelog.

`[Unreleased]` must be updated in the same PR for notable changes to:

```text
runtime behavior
persistent data
public configuration
release semantics
security guarantees
permanent CI guarantees
```

`docs/CHANGELOG.md` remains the older detailed engineering-history ledger only. New product/release history belongs in root `CHANGELOG.md`.

Release sections must distinguish automated, candidate, exact-release and installed/manual evidence. Deferred or failed acceptance remains explicit.

---

# Known gaps and technical debt

1. `VAI-CONCUR-004` real two-graphical-client Operator Lore conflict presentation remains deferred.
2. `VAI-M2-INST-005` real second-player installed isolation remains untested; automated exact player isolation exists.
3. Trustworthy causal relationship-change reasons are not implemented.
4. Current-FACT-over-conflicting-BELIEF precedence needs an explicit end-to-end regression package as semantic usage expands.
5. Automatic `NPC_TOLD` production, NPC-to-NPC knowledge transfer and rumor propagation remain future work.
6. Long-horizon/multi-day and larger-server simulation soak remain future scaling evidence.
7. `PersistentChatMemory` is a no-storage compatibility façade and may be removed when the inherited AI call surface is refactored.
8. Historical config fields for the removed raw conversation store remain deserializable compatibility baggage.
9. Historical Javadoc/deprecation warnings remain non-blocking.

---

# Next optimal delivery step

The next product slice is **trustworthy causal relationship memory**.

Current relationship persistence already records server-applied numeric transitions exactly. What is intentionally missing is a trustworthy reason for *why* a relationship changed.

Recommended TDD progression:

```text
causal-reason domain contract
→ RED: reason absent unless tied to explicit validated source evidence
→ RED: LLM free-form reason cannot become authoritative cause
→ minimal source-linked reason representation
→ exact transition before/after + source event IDs
→ replay/idempotency and contradiction tests
→ persistence/restart tests
→ prompt/retrieval integration with current FACT precedence
→ full selected CI / production / soak / release dry-run
```

Required invariants:

- a relationship numeric transition and its causal explanation remain distinct data;
- a cause must point to validated server evidence or controlled told/inferred evidence with explicit provenance;
- model prose alone never becomes an authoritative cause;
- no cause is invented when the system cannot prove one;
- exact before/after relationship state remains preserved;
- retries/replays do not duplicate reasons or transitions;
- current observed FACT/context overrides conflicting BELIEF or recalled explanation.

After causal relationship memory:

```text
FACT > BELIEF retrieval regression package
→ long-horizon recall
→ NPC-to-NPC knowledge transfer through NPC_TOLD
→ provenance-aware rumors with uncertainty / bounded distortion
```

Do not restore legacy `memory.json` migration unless a new supported-user requirement justifies a separate compatibility project.

---

# Session handoff protocol

For a new development session:

1. read `docs/PROJECT_STATE.md`;
2. read `docs/ROADMAP.md`;
3. read root `CHANGELOG.md`;
4. inspect current `1.21.1` HEAD;
5. inspect open/recent PRs, tags/releases and current CI;
6. reconcile live GitHub state against these documents before changing code;
7. use TDD for runtime behavior: specification → RED → minimal GREEN → focused regression → complete selected gates;
8. update root `CHANGELOG.md` and canonical state docs whenever the delivery boundary changes.

Do not infer a PASS from stale documentation. GitHub state and exact evidence must be checked each session.