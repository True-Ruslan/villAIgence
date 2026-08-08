# VillAIgence Project State

> **Canonical current-state handoff.** Read this file before `docs/ROADMAP.md` when resuming work. Read root `CHANGELOG.md` for product/release history.
>
> Last reconciled: **2026-08-08**, after FACT-over-BELIEF retrieval precedence merged through PR #129.
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

latest product merge:               PR #129
latest product merge commit:        0f904315f890f588e33adce1a27620ed06a94457
latest official release:            0.2.0+1.21.1
latest release commit:              e426f588efefa6aa48a6e536c4a998421bbda241
installed 0.2.0 candidate JAR SHA:   56293f86634b50b2def044429aac6f2cf0d197eb16ac1e60224708f7b3333aee

next product slice:                 long-horizon recall
then:                               NPC-to-NPC knowledge transfer
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
long-horizon recall                                    NEXT
NPC-to-NPC knowledge / rumors                          NOT IMPLEMENTED
```

Installed boundaries that remain explicit:

```text
VAI-M2-INST-005  NOT TESTED / AUTOMATED EVIDENCE ONLY
VAI-CONCUR-004   NOT TESTED / DEFERRED
```

Neither is represented as PASS.

PRs #127 and #129 are merged and fully automated on their exact source heads, but they are **not** part of the already-installed `0.2.0` release evidence. Do not describe causal relationship memory or FACT-over-BELIEF retrieval precedence as installed-release acceptance until a later release candidate is explicitly built and accepted.

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
20. A relationship transition and an explanation of its cause are distinct evidence. A causal link is authoritative only to the extent of what the server actually observed; dialogue content does not become FACT merely because it accompanied a transition.
21. Player-scoped prompt retrieval is an eligibility boundary, not a ranking preference: foreign-player Memory 2.0 data is excluded before candidate limiting.
22. Immutable snapshot context renders exactly once in deterministic authority order: current observations, Operator Lore, Semantic Memory, then episodic/social history. Lower layers may disagree but cannot override current observed truth.

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
→ exact current-player-or-NPC-global eligibility
→ bounded candidate window
→ unchanged deterministic ranking
→ immutable snapshot layers
   current observed facts
   → Operator Lore
   → Semantic Memory
   → episodic / social history
→ structured response/tool instructions
→ provider
```

The provider may suggest non-authoritative claim text and bounded numeric relationship delta. It never chooses memory visibility, truth class, source identity, causal-event identity, causal prose, precedence, or gameplay authority.

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

PRs #127 and #129 introduce no new world file or public configuration. Causal structured payloads remain inside `memory2.json`; FACT-over-BELIEF precedence changes only retrieval eligibility and prompt composition. No backfill or persistence-format migration is performed.

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
- exact relationship before/after transition snapshots;
- exact persisted source UUIDs for dialogue-trigger causal relationship history;
- exact current-player-or-NPC-global eligibility before bounded episodic/Semantic prompt retrieval;
- deterministic one-pass snapshot layering for facts, lore, Semantic Memory and episodic/social history;
- current relationship state structurally preceding stale relationship/cause history;
- NPC/player isolation and restart safety.

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
- existing relevance/importance/confidence/recency weights and deterministic tie-breaking are unchanged for eligible records;
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

PRs #127 and #129 do not alter this installed-release claim. They are merged unreleased capability until a later release boundary is prepared and accepted.

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

PR #129 already updated root `[Unreleased]` in the runtime PR. This documentation handoff therefore changes only canonical state/roadmap documents and must not duplicate the product changelog entry.

---

# Known gaps and technical debt

1. `VAI-CONCUR-004` real two-graphical-client Operator Lore conflict presentation remains deferred.
2. `VAI-M2-INST-005` real second-player installed isolation remains untested; automated current-player/NPC-global/shared-scope isolation now exists before bounded prompt retrieval.
3. Long-horizon/multi-session/multi-day recall and larger-server memory-pressure evidence remain the next Memory 2.0 validation gap.
4. Causal relationship history currently records deterministic `DIALOGUE_TURN` process linkage; richer psychological/told/inferred causal explanations intentionally remain outside the authority model and need a separate provenance-aware design if later desired.
5. Automatic `NPC_TOLD` production, NPC-to-NPC knowledge transfer and rumor propagation remain future work.
6. `RelationshipCausalHistory` provides resolved source-aware queries, while prompt context currently consumes bounded `RELATIONSHIP_CAUSE` through normal episodic/social retrieval rather than a dedicated resolved-causal prompt surface.
7. `PersistentChatMemory` is a no-storage compatibility façade and may be removed when the inherited AI call surface is refactored.
8. Historical config fields for the removed raw conversation store remain deserializable compatibility baggage.
9. Historical Javadoc/deprecation warnings remain non-blocking.

---

# Next optimal delivery step

The next product slice is **long-horizon recall**.

The short-horizon Memory 2.0 stack now has provenance-safe semantic admission, causal social history, player-isolated bounded retrieval, and executable current-truth precedence. The next risk is durability under realistic game-time distance, capacity pressure, multiple sessions and restart.

Recommended TDD progression:

```text
long-horizon contract/spec
→ RED: important semantic/episodic memory survives multi-session + multi-day gameTime + restart
→ RED: weak memory loses under bounded pressure without evicting stronger relevant evidence incorrectly
→ RED: current-player/NPC-global/shared-scope isolation still holds after pressure and restart
→ RED: current observed facts still outrank stale recalled state after long time horizons
→ minimal deterministic retention/retrieval changes only where existing behavior fails
→ multi-session / restart / pressure regression package
→ constrained long-horizon simulation evidence
→ full selected CI / production / soak / release dry-run
```

Required invariants:

- authoritative game time, not wall clock, controls temporal memory reasoning;
- memory remains hard-bounded; no unlimited history or background LLM summarization is introduced;
- current observations remain authoritative over stale recollection;
- current-player/NPC-global eligibility remains before candidate limiting;
- FACT/BELIEF provenance and causal-history authority boundaries remain unchanged;
- replay/restart behavior stays deterministic;
- no legacy `memory.json` migration or dual reader is restored.

After long-horizon recall:

```text
NPC-to-NPC knowledge transfer through NPC_TOLD
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