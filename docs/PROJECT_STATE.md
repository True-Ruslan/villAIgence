# VillAIgence Project State

> **Canonical current-state handoff.** Read this file before `docs/ROADMAP.md` when resuming work. Read root `CHANGELOG.md` for product/release history.
>
> Last reconciled: **2026-08-09**, after source-backed NPC-to-NPC knowledge transfer merged through PR #133.
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

latest product merge:               PR #133
latest product merge commit:        aacfe19cccbc8fc03c7959956873d1bd777e6ee2
latest official release:            0.2.0+1.21.1
latest release commit:              e426f588efefa6aa48a6e536c4a998421bbda241
installed 0.2.0 candidate JAR SHA:   56293f86634b50b2def044429aac6f2cf0d197eb16ac1e60224708f7b3333aee

next product slice:                 provenance-aware rumors
then:                               uncertainty / contradiction / bounded distortion
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
NPC-to-NPC knowledge transfer                          COMPLETE / PR #133
provenance-aware rumors                                NEXT
```

Installed boundaries that remain explicit:

```text
VAI-M2-INST-005  NOT TESTED / AUTOMATED EVIDENCE ONLY
VAI-CONCUR-004   NOT TESTED / DEFERRED
```

Neither is represented as PASS.

PRs #127, #129, #131 and #133 are merged and fully automated on their exact source heads, but they are **not** part of the already-installed `0.2.0` release evidence. Do not describe causal relationship memory, FACT-over-BELIEF retrieval precedence, long-horizon recall or NPC-to-NPC knowledge transfer as installed-release acceptance until a later release candidate is explicitly built and accepted.

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
24. NPC-to-NPC knowledge transfer is source-backed and server-owned: the speaker must own an exact persisted Semantic entry, the listener receives `NPC_TOLD` BELIEF only after exact transfer evidence persists and rereads successfully, and transfer never copies FACT authority or creates implicit global knowledge.

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

speaker-owned persisted Semantic FACT/BELIEF
→ exact server-owned source lookup + authoritative reread
→ exact listener-owned DIALOGUE / NPC_TOLD transfer evidence
→ exact persisted evidence reread + validation
→ listener-owned BELIEF / NPC_TOLD
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

The provider may suggest non-authoritative claim text and bounded numeric relationship delta. It never chooses memory visibility, truth class, source identity, causal-event identity, transfer speaker/listener identity, causal prose, precedence, retention score, candidate quota or gameplay authority.

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

PRs #127, #129, #131 and #133 introduce no new world file or public configuration. Causal structured payloads remain inside `memory2.json`; FACT-over-BELIEF precedence and long-horizon recall change retrieval/retention behavior without changing persistence format/version; NPC-to-NPC transfer reuses the existing `memory2.json` + `semantic-memory.json` contracts and adds exact read-only authority lookup APIs. No backfill or persistence-format migration is performed.

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
- exact owner+UUID/predicate authority lookup for source-backed lifecycle validation;
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
- source-backed server-owned NPC-to-NPC transfer into listener `NPC_TOLD` BELIEF;
- explicit `SOURCE_NOT_RETAINED` / `BELIEF_NOT_RETAINED` partial-retention outcomes;
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

### NPC-to-NPC knowledge transfer — PR #133

Merged through PR #133 / `aacfe19cccbc8fc03c7959956873d1bd777e6ee2`.

Implemented authoritative flow:

```text
exact persisted speaker Semantic entry
→ exact owner+UUID authority lookup
→ authoritative exact reread of immutable source snapshot
→ canonical listener-owned DIALOGUE / NPC_TOLD evidence
→ MemoryEventStore append
→ exact listener evidence reread + full canonical validation
→ existing SemanticBeliefAdmissionPolicy
→ listener-owned BELIEF / NPC_TOLD
→ retained post-consolidation verification containing exact transfer evidence UUID
```

Canonical transfer evidence is deterministic and server-owned:

```text
owner:                    listener NPC
participants:             [listener, speaker]
type:                     DIALOGUE
provenance:               NPC_TOLD
gameTime:                 authoritative Minecraft gameTime
createdAtEpochMillis:     0
importance/confidence:    50 / 50
emotionalWeight:          0
dialogue payload:         absent
summary:                  NPC told: <bounded normalized statement>
```

Guarantees:

- the caller selects only server-owned world/source/speaker/listener IDs and capacities; arbitrary claim text, provenance, truth class, scope, source IDs, importance and confidence are not public transfer inputs;
- the source must be an exact persisted Semantic entry owned by the claimed speaker;
- source FACT and source BELIEF are both transferable, but the listener always receives `BELIEF / NPC_TOLD`;
- FACT authority and the speaker's upstream provenance/source-event chain are not copied to the listener;
- subject scope is preserved canonically and the speaker is not injected into `relatedEntities`;
- exact transfer retry is byte-idempotent for both memory stores;
- a later transfer at a new authoritative `gameTime` creates distinct evidence and can consolidate exact source UUIDs into one logical listener BELIEF;
- `SOURCE_NOT_RETAINED` means event pressure rejected the transfer evidence and no BELIEF is admitted;
- `BELIEF_NOT_RETAINED` means the real transfer evidence remains but Semantic pressure did not retain the listener BELIEF; the evidence is not rolled back;
- raw NPC→NPC transfer evidence has no player-oriented `DialogueExchange`, so it is not reconstructed as player Working Memory;
- global/private/shared semantic scopes, foreign-player isolation and independent NPC-pair isolation remain exact;
- transferred BELIEF participates in existing long-horizon retrieval and remains evictable under stronger deterministic pressure;
- current observed truth still outranks transferred BELIEF;
- no new persistence schema/file, public config, provider call, scheduler, autonomous visible NPC conversation, trust weighting, rumor propagation or migration was added.

Observed strict TDD evidence included compile REDs for exact authority lookup and pure transfer contracts, then a behavioral lifecycle RED with **557 common tests / exactly 2 expected failures / 555 PASS** before the successful transfer implementation existed. Preservation-only GREEN stages then covered fail-closed authority, byte-idempotent replay, consolidation, partial-retention statuses, fresh-root reload, player Working Memory isolation, scope/privacy, independent NPC pairs, long-horizon recall and deterministic multi-NPC pressure.

Final exact-head evidence:

```text
verified head:                           864b7f7e263a0a9078c710416ceb680fa5affd88
merge commit:                            aacfe19cccbc8fc03c7959956873d1bd777e6ee2
Repository security policy #1758:       SUCCESS / run 31283523663
VillAIgence CI #2123:                   SUCCESS / run 31283523664
VillAIgence Production Soak #178:       SUCCESS / run 31283523656
VillAIgence GitHub Release #512:        SUCCESS / run 31283523657
release publication job:                SKIPPED
independent runtime review P0/P1/P2:    0 / 0 / 0
open review threads:                    0
```

Main CI selected and passed common/mock-provider tests, Fabric + NeoForge GameTests/builds, exact production startup/restart, persistence recovery and package verification. Soak passed constrained-heap concurrency and five restart cycles. Release dry-run passed exact acceptance/recovery, loader/GameTest coverage and accepted-JAR/package identity while publication remained skipped. This is unreleased source/candidate evidence, not installed `0.2.0` acceptance.

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

PR #133 final exact-head evidence:

```text
verified head:                           864b7f7e263a0a9078c710416ceb680fa5affd88
merge commit:                            aacfe19cccbc8fc03c7959956873d1bd777e6ee2
Repository security policy #1758:       SUCCESS / run 31283523663
VillAIgence CI #2123:                   SUCCESS / run 31283523664
VillAIgence Production Soak #178:       SUCCESS / run 31283523656
VillAIgence GitHub Release #512:        SUCCESS / run 31283523657
release publication job:                SKIPPED
independent runtime review P0/P1/P2:    0 / 0 / 0
open review threads:                    0
```

PR #133 additionally carries explicit staged RED→GREEN evidence for exact authority lookup, canonical transfer evidence/policy APIs and behavioral source-backed lifecycle admission. Preservation coverage proves byte-idempotent retry, deterministic corroborating source union, explicit partial-retention outcomes, fresh-root reload, player/NPC isolation and long-horizon deterministic pressure without wall-clock assertions.

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

PRs #127, #129, #131 and #133 do not alter this installed-release claim. They are merged unreleased capability until a later release boundary is prepared and accepted.

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

PR #133 already updated root `[Unreleased]` in the runtime PR. This documentation handoff therefore changes only canonical state/roadmap documents and must not duplicate the product changelog entry.

---

# Known gaps and technical debt

1. `VAI-CONCUR-004` real two-graphical-client Operator Lore conflict presentation remains deferred.
2. `VAI-M2-INST-005` real second-player installed isolation remains untested; automated current-player/NPC-global/shared-scope isolation exists before both recent/durable prompt allocation.
3. Provenance-aware rumor propagation, explicit origin/speaker chains, uncertainty, contradiction and bounded distortion are the next Memory 2.0 product gap.
4. The accepted NPC-to-NPC transfer primitive is intentionally server-invoked only; autonomous initiation, visible NPC↔NPC conversation presentation and voice are separate future product slices rather than implicit behavior in PR #133.
5. Causal relationship history currently records deterministic `DIALOGUE_TURN` process linkage; richer psychological/told/inferred causal explanations intentionally remain outside the authority model and need a separate provenance-aware design if later desired.
6. `RelationshipCausalHistory` provides resolved source-aware queries, while prompt context currently consumes bounded `RELATIONSHIP_CAUSE` through normal episodic/social retrieval rather than a dedicated resolved-causal prompt surface.
7. `PersistentChatMemory` is a no-storage compatibility façade and may be removed when the inherited AI call surface is refactored.
8. Historical config fields for the removed raw conversation store remain deserializable compatibility baggage.
9. Historical Javadoc/deprecation warnings remain non-blocking.

---

# Next optimal delivery step

The next product slice is **provenance-aware rumors built on the accepted source-backed NPC-to-NPC transfer primitive**.

Memory 2.0 now has source-bound FACT/BELIEF admission, causal relationship history, player-isolated FACT-over-BELIEF prompt framing, bounded long-horizon retention/retrieval and a deterministic exact-evidence NPC→NPC transfer primitive. The next missing capability is to preserve provenance and uncertainty across a bounded multi-hop social chain without letting repetition become authority or creating an omniscient knowledge bus.

Required product boundary:

```text
origin knowledge / direct source
→ explicit server-owned NPC→NPC transfer evidence
→ listener NPC_TOLD BELIEF
→ bounded later retelling
→ explicit origin + speaker-chain provenance
→ uncertainty / contradiction state retained as non-authoritative data
→ later retrieval remains BELIEF/RUMOR, never FACT
```

Recommended design/TDD progression:

```text
rumor/provenance design spec
→ decide minimal bounded provenance-chain representation and hard depth/size limits
→ RED: direct transfer remains valid while multi-hop source chain is absent
→ RED: A→B→C preserves exact origin + each server-proven transfer hop
→ RED: repetition/corroboration never upgrades rumor/BELIEF to FACT
→ RED: cycles/replay cannot grow source chains without bound or duplicate hops
→ RED: contradictory rumors remain representable without provider-selected truth resolution
→ RED: privacy eligibility, current-truth precedence, restart and long-horizon bounds remain unchanged
→ minimal provenance-aware multi-hop producer/persistence extension only where observed tests fail
→ deterministic multi-NPC/cycle/pressure/restart simulation
→ full selected CI / production / soak / release dry-run
```

Required invariants:

- direct `NPC_TOLD` transfer from PR #133 remains the trustworthy primitive and is not weakened;
- rumor/multi-hop data remains non-authoritative even when repeated or corroborated;
- origin and each transfer hop must correspond to exact server-backed evidence rather than provider-supplied UUIDs;
- provider/model output cannot choose truth class, source identity, speaker/listener identity, chain depth, visibility or retention;
- provenance chains are bounded in depth and serialized size and are deterministic under replay/restart;
- one NPC learning or repeating a rumor does not globally distribute it;
- current observed FACT still outranks all rumor/BELIEF recollection;
- existing `32` candidate / `24+8` long-horizon / `6` result bounds and player privacy eligibility stay intact;
- no legacy `memory.json` migration/dual reader returns.

After provenance-aware rumors:

```text
uncertainty / contradiction / bounded distortion
→ settlement-scale information flow without omniscience
→ relationship/trust effects on belief confidence as a separate social-epistemology slice
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