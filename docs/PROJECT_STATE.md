# VillAIgence Project State

> **Canonical current-state handoff.** Read this file before `docs/ROADMAP.md` when resuming work. Read root `CHANGELOG.md` for product/release history.
>
> Last reconciled: **2026-08-08**, after long-horizon Memory 2.0 recall merged through PR #131.
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

latest product merge:               PR #131
latest product merge commit:        9827a3b511421036c7ae6733fd4fabe4efc8e0c1
latest official release:            0.2.0+1.21.1
latest release commit:              e426f588efefa6aa48a6e536c4a998421bbda241
installed 0.2.0 candidate JAR SHA:   56293f86634b50b2def044429aac6f2cf0d197eb16ac1e60224708f7b3333aee

next product slice:                 NPC-to-NPC knowledge transfer
then:                               provenance-aware rumors
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
trustworthy causal relationship memory                 COMPLETE / PR #127
FACT > BELIEF retrieval regression package             COMPLETE / PR #129
long-horizon recall                                    COMPLETE / PR #131
NPC-to-NPC knowledge transfer                          NEXT
provenance-aware rumors                                NOT IMPLEMENTED
```

Installed boundaries that remain explicit:

```text
VAI-M2-INST-005  NOT TESTED / AUTOMATED EVIDENCE ONLY
VAI-CONCUR-004   NOT TESTED / DEFERRED
```

Neither is represented as PASS.

PRs #127, #129 and #131 are merged and fully automated on their exact source heads, but they are **not** part of the already-installed `0.2.0` release evidence. Do not describe causal relationship memory, FACT-over-BELIEF retrieval precedence or long-horizon recall as installed-release acceptance until a later release candidate is explicitly built and accepted.

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
18. Release recovery may repair metadata/assets only from an existing immutable release tag commit and never moves the tag.
19. Release recovery validates the persistence contract defined by the immutable target release itself.
20. A relationship transition and an explanation of its cause are distinct evidence. A causal link is authoritative only to the extent of what the server actually observed; dialogue content does not become FACT merely because it accompanied a transition.
21. Player-scoped prompt retrieval is an eligibility boundary, not a ranking preference: foreign-player Memory 2.0 data is excluded before candidate limiting.
22. Immutable snapshot context renders exactly once in deterministic authority order: current observations, Operator Lore, Semantic Memory, then episodic/social history. Lower layers may disagree but cannot override current observed truth.
23. Long-horizon recall is bounded and two-tiered: eligible memory is selected into recent and durable pools before the existing final ranker, while foreign-player and other-NPC data consume zero prompt-candidate slots and no memory class becomes immortal.

Canonical AI/state flow:

```text
Minecraft/server state
→ immutable bounded snapshot
→ current observed facts + operator lore + episodic/semantic memory
→ deterministic authority-layer composition
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

Relationship causal-history flow:

```text
provider proposes bounded numeric relationshipDelta
→ server validates/clamps/persists relationship state
→ exact RELATIONSHIP_CHANGE with before/after snapshot

successful visible reply
→ exact persisted DIALOGUE

persisted same-NPC/player/same-gameTime source pair
→ server-authored RELATIONSHIP_CAUSE(DIALOGUE_TURN)
→ source UUID linkage + transition snapshot
→ queryable causal history
```

Prompt retrieval/precedence flow:

```text
NPC-owned Memory 2.0 / Semantic Memory
→ exact current-player / NPC-global / shared-scope eligibility
→ bounded long-horizon candidate selection
   24 recent + 8 durable at the normal 32-candidate bound
→ existing deterministic final ranking
→ at most 6 prompt entries per memory domain
→ immutable snapshot layers
   current observed facts
   → Operator Lore
   → Semantic Memory
   → episodic / social history
→ structured response/tool instructions
→ provider
```

The provider may suggest non-authoritative claim text and bounded numeric relationship delta. It never chooses memory visibility, truth class, source identity, causal-event identity, causal prose, precedence, retention score, candidate quota or gameplay authority.

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

PRs #127, #129 and #131 introduce no new world file or public configuration. Causal structured payloads remain inside `memory2.json`; FACT-over-BELIEF precedence and long-horizon recall change retrieval/retention behavior without changing persistence format/version. No backfill or persistence-format migration is performed.

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
- DIALOGUE, OBSERVATION, ACTION, RELATIONSHIP_CHANGE and RELATIONSHIP_CAUSE;
- deterministic UUID idempotency and ordering;
- bounded per-NPC persistence and retrieval;
- text/voice DIALOGUE parity;
- bounded Working Memory;
- typed semantic FACT/BELIEF entries;
- controlled server-observed FACT ingestion;
- deterministic semantic consolidation and source union;
- deterministic pressure-based forgetting;
- deterministic durability-aware episodic/social retention using authoritative game time;
- bounded dual-tier long-horizon candidate selection with recent and durable quotas;
- exact relationship before/after transition snapshots;
- exact persisted source UUIDs for dialogue-trigger causal relationship history;
- exact current-player/NPC-global/shared eligibility before bounded episodic/Semantic prompt candidate selection;
- NPC-global semantic and episodic memory treated as fully relevant after eligibility;
- deterministic one-pass snapshot layering for facts, lore, Semantic Memory and episodic/social history;
- current relationship state structurally preceding stale relationship/cause history;
- NPC/player isolation and restart safety;
- multi-session, multi-day game-time, capacity-pressure and restart regression evidence.

Truth boundary:

```text
FACT               → SYSTEM_OBSERVED only
BELIEF             → PLAYER_TOLD / NPC_TOLD / INFERRED only
DIALOGUE           → episodic by default
RELATIONSHIP_CAUSE → server-observed process linkage, not truth of dialogue prose
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

### Trustworthy causal relationship memory — PR #127

Merged through PR #127 / `e020f54258d468fd37b0fa5ada5bbc8b6c7c2f77`.

Implemented flow:

```text
validated relationship mutation
→ RELATIONSHIP_CHANGE
  - SYSTEM_OBSERVED
  - exact before/after RelationshipTransition

successful persisted dialogue from the same NPC/player game-time turn
→ DIALOGUE

exact persisted source pair
→ RelationshipCauseLifecycle
→ RELATIONSHIP_CAUSE(DIALOGUE_TURN)
  - SYSTEM_OBSERVED process linkage
  - relationshipChangeEventId
  - evidenceEventId
  - transitionSnapshot
```

Guarantees:

- numeric relationship state remains the authoritative server-owned transition;
- the provider response schema contains no causal reason or source-event UUID fields;
- free-form provider/player/NPC prose cannot become an authoritative cause through this path;
- `RELATIONSHIP_CAUSE` means only that the accepted transition occurred during the exact linked dialogue turn;
- relationship and DIALOGUE source events must already be present in the current world-local `MemoryEventStore`;
- source owner NPC, player participant and exact `gameTime` must match;
- deterministic cause UUIDs make exact replay idempotent;
- result-bearing relationship ingestion returns the exact event retained by the store, including on duplicate replay;
- causal `createdAt` deterministically follows its source events, avoiding ambiguous bounded-retention tie ordering;
- cause payload retains source UUIDs plus transition snapshot even if bounded pressure later evicts referenced sources;
- `RelationshipCausalHistory` filters exact NPC/player eligibility before limiting and resolves only exact source UUID/type/owner/participants;
- missing source evidence is exposed as unavailable and never reconstructed from generated prose;
- `RELATIONSHIP_CAUSE` is not automatically projected into Semantic Memory FACT;
- classic/Inworld paths remain outside the causal producer.

This is intentionally process-level causal history, not generated psychological explanation. More expressive told/inferred causal narratives require a separate provenance-aware design if later needed.

### FACT > BELIEF retrieval precedence — PR #129

Merged through PR #129 / `0f904315f890f588e33adce1a27620ed06a94457`.

Implemented prompt retrieval boundary:

```text
NPC-owned semantic / episodic memory
→ exact current-player-or-NPC-global eligibility
→ candidate limit
→ existing deterministic ranking
→ immutable snapshot
→ current observed facts
→ Operator Lore
→ Semantic Memory
→ episodic / relationship social history
→ structured-response / command instructions
```

Guarantees:

- non-empty Semantic Memory scopes that do not contain the current player are excluded before the 32-candidate window;
- episodic/social events with external participants that do not include the current player are likewise excluded before the 32-candidate window;
- foreign-player `RELATIONSHIP_CHANGE` and `RELATIONSHIP_CAUSE` cannot enter another player's prompt;
- NPC-global memories remain eligible;
- shared memories remain eligible when the current player participates alongside another entity;
- existing relevance/importance/confidence/recency weights and deterministic tie-breaking are unchanged for eligible records at this delivery boundary;
- classic `PlayerModule.apply(...)` retains the existing `MemoryModule` behavior, while immutable snapshot capture uses a no-memory player-context seam and dedicated memory fields;
- snapshot memory is loaded/rendered once rather than through both generic `contextLines` and dedicated fields;
- current world facts render before lore, semantic memory and episodic/social history;
- current relationship state therefore structurally precedes stale relationship and causal history;
- conflicting BELIEFs remain BELIEF and are never promoted by confidence, ranking or repetition;
- the obsolete `MixinOpenAIChatAI` lore insertion was removed after direct layered composition became authoritative;
- provider request schema, retry/transport, action authority, relationship mutation policy, persistence schemas and public config remain unchanged.

Observed TDD RED boundaries included:

```text
semantic foreign-player pre-limit leakage/starvation
foreign episodic + RELATIONSHIP_CAUSE leakage/starvation
snapshot PlayerModule → MemoryModule duplication
missing four-layer SnapshotContextPromptPolicy API
direct OpenAI layered wiring + obsolete lore mixin
```

Final review also added preservation regressions proving shared current-player-plus-other-entity semantic/episodic memory remains eligible without changing production code.

### Long-horizon recall — PR #131

Merged through PR #131 / `9827a3b511421036c7ae6733fd4fabe4efc8e0c1`.

Implemented bounded retrieval:

```text
eligible NPC-owned memory
→ 24 newest records
+ 8 strongest durable records
→ de-duplicate within the existing hard candidate bound 32
→ existing domain ranker
→ at most 6 prompt records
```

Implemented episodic/social pressure retention:

```text
exact duplicate check
→ add candidate
→ authoritative max persisted gameTime
→ deterministic durability + game-time decay
→ retain hard-bounded per-NPC set
→ stable persistence order
→ save only if retained state actually changed
```

Durability is server-owned and provider-independent. It uses persisted importance, confidence, absolute emotional weight, event type, provenance and authoritative Minecraft `gameTime`. `RELATIONSHIP_CAUSE` and `RELATIONSHIP_CHANGE` receive stronger bounded retention than ordinary DIALOGUE, but no event type is immortal. Semantic persistence keeps its existing deterministic retention policy.

Long-horizon visibility and authority guarantees:

- current-player/NPC-global/shared eligibility happens before both recent and durable allocation;
- foreign-player and other-NPC memory consumes zero prompt candidate slots;
- NPC-global Semantic entries and episodic events remain fully relevant after eligibility;
- candidate/result bounds remain `32` / `6`;
- current observed facts and current relationship state still structurally precede stale recalled memory;
- FACT/BELIEF/provenance classes are unchanged;
- no provider/model output controls retention, visibility, truth class or quota;
- no persistence schema/version, public config, provider protocol, migration/backfill, vector database or background summarizer was added.

Observed TDD gates covered:

```text
retained-but-starved Semantic recall
pure recent/durable candidate selector
FIFO episodic pressure loss
pure episodic retention policy
rejected weak append must not rewrite persistence
retained-but-starved episodic recall
multi-session / multi-day / restart preservation
mixed two-NPC / two-player / shared-scope pressure
NPC-global relevance starvation
```

Final exact-head merge evidence:

```text
verified head:                           a6bbb45396a2831ac2ace7099c9087e0f5615e12
merge commit:                            9827a3b511421036c7ae6733fd4fabe4efc8e0c1
Repository security policy #1712:       SUCCESS / run 31261296326
VillAIgence CI #2077:                   SUCCESS / run 31261296333
VillAIgence Production Soak #157:       SUCCESS / run 31261296357
VillAIgence GitHub Release #491:        SUCCESS / run 31261296336
release publication job:                SKIPPED
independent runtime review P0/P1/P2:    0 / 0 / 0
open review threads:                    0
```

The final runtime head before evidence-only synchronization also passed the same four gate families. Main CI and release dry-run cover common tests, risk/GameTests, Fabric + NeoForge, production startup/restart, persistence recovery, package verification and accepted/package JAR identity. This is source/candidate automation evidence, not installed `0.2.0` acceptance.

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

PR #125 review hardening TDD evidence:

```text
RED head:               806b6bd9f5601fc4cefa4a14d64422b0b11b6f2e
VillAIgence CI #1897:   494 tests / 1 expected failure
failure:                rejectsPlayerThatIsNotAParticipantOfSourceDialogue
GREEN implementation:   1b7b5fb75e7eb591a4b105ea3163c0c177d6b780
```

PR #127 final exact-head evidence:

```text
verified head:                           a027de8a69d4eef19b330b9e0d014e7c9b0ef6c7
merge commit:                            e020f54258d468fd37b0fa5ada5bbc8b6c7c2f77
Repository security policy #1590:       SUCCESS / run 31251212282
VillAIgence CI #1955:                   SUCCESS / run 31251212279
VillAIgence Production Soak #107:       SUCCESS / run 31251212273
VillAIgence GitHub Release #441:        SUCCESS / run 31251212289
release publication job:                SKIPPED
common/mock-provider suite:             511 tests / 0 FAIL
independent review P0/P1/P2:            0 / 0 / 0
open review threads:                    0
```

PR #127 TDD/review hardening included distinct observed RED cycles for structured transition state, causal admission, result-bearing ingestion, DIALOGUE-before-cause wiring, query/restart/eviction behavior, retention ordering, cross-turn rejection, and exact persisted replay results.

PR #129 final exact-head evidence:

```text
verified head:                           f5b3027e819ffaa177225de4a91a9725e3dff79c
merge commit:                            0f904315f890f588e33adce1a27620ed06a94457
Repository security policy #1642:       SUCCESS / run 31254499798
VillAIgence CI #2007:                   SUCCESS / run 31254499774
VillAIgence Production Soak #129:       SUCCESS / run 31254499790
VillAIgence GitHub Release #463:        SUCCESS / run 31254499781
release publication job:                SKIPPED
independent review P0/P1/P2:            0 / 0 / 0
open review threads:                    0
```

PR #129 exact-head CI passed common/mock-provider regressions, required server GameTests, Fabric + NeoForge, production startup/restart acceptance, selected persistence recovery, package verification, constrained authenticated concurrency, five production restart cycles, complete release dry-run acceptance, and production/package JAR identity.

PR #131 final exact-head evidence:

```text
verified head:                           a6bbb45396a2831ac2ace7099c9087e0f5615e12
merge commit:                            9827a3b511421036c7ae6733fd4fabe4efc8e0c1
Repository security policy #1712:       SUCCESS / run 31261296326
VillAIgence CI #2077:                   SUCCESS / run 31261296333
VillAIgence Production Soak #157:       SUCCESS / run 31261296357
VillAIgence GitHub Release #491:        SUCCESS / run 31261296336
release publication job:                SKIPPED
independent runtime review P0/P1/P2:    0 / 0 / 0
open review threads:                    0
```

PR #131 additionally carries explicit observed RED→GREEN evidence for Semantic/episodic recall starvation, episodic pressure retention, no-op persistence writes and NPC-global relevance; its multi-session simulation verifies deterministic behavior across fresh persistence reloads and pressure without wall-clock assertions.

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

PRs #127, #129 and #131 do not alter this installed-release claim. They are merged unreleased capability until a later release boundary is prepared and accepted.

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

PR #131 already updated root `[Unreleased]` in the runtime PR. This documentation handoff therefore changes only canonical state/roadmap documents and must not duplicate the product changelog entry.

---

# Known gaps and technical debt

1. `VAI-CONCUR-004` real two-graphical-client Operator Lore conflict presentation remains deferred.
2. `VAI-M2-INST-005` real second-player installed isolation remains untested; automated current-player/NPC-global/shared-scope isolation exists before both recent/durable prompt allocation.
3. Automatic `NPC_TOLD` production and NPC-to-NPC knowledge transfer remain the next Memory 2.0 product gap.
4. Provenance-aware rumor propagation, uncertainty and bounded distortion remain future work after NPC-to-NPC transfer.
5. Causal relationship history currently records deterministic `DIALOGUE_TURN` process linkage; richer psychological/told/inferred causal explanations intentionally remain outside the authority model and need a separate provenance-aware design if later desired.
6. `RelationshipCausalHistory` provides resolved source-aware queries, while prompt context currently consumes bounded `RELATIONSHIP_CAUSE` through normal episodic/social retrieval rather than a dedicated resolved-causal prompt surface.
7. `PersistentChatMemory` is a no-storage compatibility façade and may be removed when the inherited AI call surface is refactored.
8. Historical config fields for the removed raw conversation store remain deserializable compatibility baggage.
9. Historical Javadoc/deprecation warnings remain non-blocking.

---

# Next optimal delivery step

The next product slice is **NPC-to-NPC knowledge transfer through `NPC_TOLD` BELIEF**.

The Memory 2.0 stack now has source-bound BELIEF admission, causal relationship history, player-isolated FACT-over-BELIEF prompt framing, bounded long-horizon retention/retrieval and deterministic restart/pressure behavior. The next missing capability is controlled information movement between NPCs without omniscient/global knowledge distribution.

Required product boundary:

```text
NPC A owns sourced knowledge
→ bounded server-authorized social exchange
→ explicit source event proving NPC A told NPC B
→ NPC B receives a bounded claim
→ NPC_TOLD BELIEF admitted with exact source/provenance chain
→ later retrieval remains BELIEF
```

Recommended TDD progression:

```text
NPC-to-NPC transfer contract/spec
→ RED: NPC_TOLD admission requires exact persisted NPC→NPC dialogue/source evidence
→ RED: wrong speaker/listener/source NPC/time fails closed
→ RED: transfer creates BELIEF for listener only; never FACT and never global knowledge
→ RED: replay/retry is idempotent and corroborating exact sources union deterministically
→ RED: transferred knowledge survives restart/pressure/long-horizon recall under existing bounds
→ RED: foreign-player/NPC visibility and current-truth precedence remain unchanged
→ minimal server-owned producer/wiring only where observed tests fail
→ deterministic multi-NPC propagation regression package
→ full selected CI / production / soak / release dry-run
```

Required invariants:

- `NPC_TOLD` remains BELIEF-only provenance;
- speaker/listener/source identities come from exact persisted server evidence, not provider metadata;
- no provider/model call chooses source UUID, truth class, persistence owner or visibility;
- one NPC learning a claim does not make every NPC know it;
- current observed FACT still outranks transferred BELIEF;
- repeated/corroborated telling may affect confidence/source history only through explicit deterministic policy and never upgrades to FACT;
- existing long-horizon bounds, retention, privacy and restart guarantees stay intact;
- no new persistence format/config/migration is introduced unless an observed contract failure proves it necessary.

After NPC-to-NPC knowledge transfer:

```text
provenance-aware rumors
→ uncertainty / contradiction / bounded distortion
→ settlement-scale information flow without omniscience
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