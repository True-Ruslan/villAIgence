# VillAIgence Project State

> **Canonical current-state handoff.** Read this file before `docs/ROADMAP.md` when resuming work. Read root `CHANGELOG.md` for product/release history.
>
> Last reconciled: **2026-08-09**, after provenance-aware bounded multi-hop rumors merged through PR #135.
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

latest product merge:               PR #135
latest product merge commit:        f1fdee1fa1cd0b3a04a2f33357d50d7ae4c1a6d7
latest official release:            0.2.0+1.21.1
latest release commit:              e426f588efefa6aa48a6e536c4a998421bbda241
installed 0.2.0 candidate JAR SHA:   56293f86634b50b2def044429aac6f2cf0d197eb16ac1e60224708f7b3333aee

next product slice:                 contradiction representation without truth promotion
then:                               uncertainty / bounded distortion
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
provenance-aware bounded multi-hop rumors              COMPLETE / PR #135
contradiction representation                           NEXT
```

Installed boundaries that remain explicit:

```text
VAI-M2-INST-005  NOT TESTED / AUTOMATED EVIDENCE ONLY
VAI-CONCUR-004   NOT TESTED / DEFERRED
```

Neither is represented as PASS.

PRs #127, #129, #131, #133 and #135 are merged and automated on their exact source heads, but they are **not** part of the already-installed `0.2.0` release evidence. Do not describe causal relationship memory, FACT-over-BELIEF retrieval precedence, long-horizon recall, NPC-to-NPC transfer or multi-hop provenance-aware rumors as installed-release acceptance until a later release candidate is explicitly built and accepted.

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
25. Multi-hop rumor provenance is bounded and source-backed: every new v2 direct `NPC_TOLD` transfer stores one immutable origin plus at most eight exact server-owned hops; canonical ancestry is selected independently of the proposed listener, downstream knowledge remains BELIEF/NPC_TOLD, and current observed truth remains authoritative.

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

retained listener NPC_TOLD BELIEF
+ retained canonical v2 direct transfer evidence
→ deterministic listener-independent ancestry resolution
→ cycle check
→ hop-limit check
→ next direct v2 transfer evidence with immutable ancestry snapshot
→ downstream BELIEF / NPC_TOLD
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

The provider may suggest non-authoritative claim text and bounded numeric relationship delta. It never chooses memory visibility, truth class, source identity, causal-event identity, transfer speaker/listener identity, rumor origin/ancestry, causal prose, precedence, retention score, candidate quota or gameplay authority.

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

PRs #127, #129, #131, #133 and #135 introduce no new world file or public configuration. Causal structured payloads and v2 transfer-provenance payloads remain inside `memory2.json`; FACT-over-BELIEF precedence and long-horizon recall change retrieval/retention behavior without changing persistence format/version; NPC-to-NPC transfer and multi-hop provenance reuse the existing `memory2.json` + `semantic-memory.json` contracts and exact read-only authority lookup APIs. `memory2.json` remains format version 1 for the provenance slice. No backfill or persistence-format migration is performed.

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
- bounded immutable multi-hop v2 transfer provenance with exact origin and ordered hops;
- listener-independent canonical direct-branch selection by `gameTime DESC → evidence UUID ASC`;
- explicit `SOURCE_NOT_RETAINED`, `BELIEF_NOT_RETAINED`, `PROVENANCE_UNAVAILABLE`, `PROVENANCE_CYCLE` and `PROVENANCE_LIMIT_REACHED` outcomes;
- exact current-player/NPC-global/shared eligibility before bounded episodic/Semantic prompt candidate selection;
- NPC-global semantic and episodic memory treated as fully relevant after eligibility;
- deterministic one-pass snapshot layering for facts, lore, Semantic Memory and episodic/social history;
- current relationship state structurally preceding stale relationship/cause history;
- NPC/player isolation and restart safety;
- multi-session, multi-day game-time, capacity-pressure and restart regression evidence;
- deterministic 10-NPC multi-hop rumor simulation with pressure-order and fresh-root equality checks.

Truth boundary:

```text
FACT               → SYSTEM_OBSERVED only
BELIEF             → PLAYER_TOLD / NPC_TOLD / INFERRED only
DIALOGUE           → episodic by default
RELATIONSHIP_CAUSE → server-observed process linkage, not truth of dialogue prose
rumor retelling    → BELIEF / NPC_TOLD at every downstream hop
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
- causal payload retains source UUIDs plus transition snapshot even if bounded pressure later evicts referenced sources;
- missing source evidence is exposed as unavailable and never reconstructed from generated prose;
- `RELATIONSHIP_CAUSE` is not automatically projected into Semantic Memory FACT.

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

- foreign-player Semantic/episodic/social records are excluded before the 32-candidate window;
- NPC-global memories remain eligible;
- shared memories remain eligible when the current player participates alongside another entity;
- current world facts render before lore, semantic memory and episodic/social history;
- current relationship state structurally precedes stale relationship and causal history;
- conflicting BELIEFs remain BELIEF and are never promoted by confidence, ranking or repetition;
- provider request schema, retry/transport, action authority, relationship mutation policy, persistence schemas and public config remain unchanged.

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

Durability is server-owned and provider-independent. It uses persisted importance, confidence, absolute emotional weight, event type, provenance and authoritative Minecraft `gameTime`. No event type is immortal. Semantic persistence keeps its existing deterministic retention policy.

Visibility and authority guarantees:

- current-player/NPC-global/shared eligibility happens before both recent and durable allocation;
- foreign-player and other-NPC memory consumes zero prompt candidate slots;
- candidate/result bounds remain `32` / `6`;
- current observed facts and current relationship state still structurally precede stale recalled memory;
- FACT/BELIEF/provenance classes are unchanged;
- no provider/model output controls retention, visibility, truth class or quota.

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

Guarantees:

- caller cannot inject arbitrary claim text, provenance, truth class, scope, source IDs, importance or confidence;
- source must be exact persisted Semantic knowledge owned by the claimed speaker;
- source FACT or BELIEF always becomes listener BELIEF/NPC_TOLD;
- FACT authority is not copied to the listener;
- subject scope is preserved without automatically adding the speaker;
- exact retry is byte-idempotent;
- transfer evidence remains distinct across later authoritative game times;
- event/Semantic pressure produces explicit partial-retention statuses;
- raw NPC→NPC evidence is not reconstructed as player Working Memory;
- transferred BELIEF remains bounded and evictable.

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

### Provenance-aware bounded multi-hop rumors — PR #135

Merged through PR #135 / `f1fdee1fa1cd0b3a04a2f33357d50d7ae4c1a6d7`.

Implemented v2 provenance model:

```text
exact persisted speaker Semantic source
→ authoritative exact reread
→ first-hop origin OR canonical retained direct ancestry
→ cycle check
→ hop-limit check
→ deterministic npc-knowledge-transfer-v2 evidence identity
→ listener-owned DIALOGUE / NPC_TOLD evidence with immutable ancestry
→ exact evidence reread + canonical validation
→ listener BELIEF / NPC_TOLD
```

Each direct v2 transfer evidence contains one immutable `KnowledgeTransferProvenance`:

```text
Origin
  originNpcId
  originSemanticEntryId
  originKind
  originProvenance
  exact normalized statement
  exact canonical relatedEntities

Hop[]
  speakerNpcId
  listenerNpcId
  speakerSemanticEntryId
  evidenceEventId
  authoritative gameTime
```

Guarantees:

- first-hop origins are restricted to `FACT/SYSTEM_OBSERVED`, `BELIEF/PLAYER_TOLD`, or `BELIEF/INFERRED`;
- `BELIEF/NPC_TOLD` may continue only from retained structured v2 direct evidence and cannot reset origin;
- every downstream listener remains `BELIEF/NPC_TOLD` regardless of an origin FACT;
- lineage is acyclic and hard-capped at eight hops;
- `PROVENANCE_CYCLE` is evaluated before `PROVENANCE_LIMIT_REACHED` for the selected lineage;
- canonical retained ancestry is selected by `gameTime DESC`, then evidence UUID ascending;
- the resolver accepts no proposed listener, so ancestry selection is listener-independent;
- cycle/limit rejection never falls back to a lower-priority lineage;
- Semantic BELIEF keeps direct `sourceEventIds` only; ancestry remains in each direct evidence event instead of expanding into an unbounded Semantic DAG;
- exact statement and semantic subject scope are preserved across hops;
- provenance actors do not pollute `relatedEntities`;
- malformed, missing, provenance-less historical-v1, wrong-owner, unreferenced, statement-mismatched or scope-mismatched evidence fails closed;
- loss of the current direct evidence prevents further propagation but does not erase an already-retained speaker BELIEF;
- later direct evidence keeps its immutable ancestry snapshot even if an older physical hop is evicted;
- exact replay after fresh-root reload is byte-idempotent for tested `memory2.json` and `semantic-memory.json` state;
- rumor evidence and BELIEFs remain evictable under the existing bounded policies;
- private/shared/global visibility and player Working Memory isolation remain exact;
- current observed FACT still outranks all downstream rumors/BELIEFs.

Deterministic simulation evidence covers:

```text
10 NPCs
8 admitted hops
rejected ninth hop
cycle attempts
multiple independent origins
corroborating direct evidence
>200 Semantic noise records
>200 episodic/social noise records
forward/reverse pressure order
2 fresh-root reloads
private player isolation
conflicting current FACT preservation
```

Final verified runtime/evidence head before squash merge:

```text
verified head:                           d2d487d980c7ffe9819e3250489519005fd6767c
merge commit:                            f1fdee1fa1cd0b3a04a2f33357d50d7ae4c1a6d7
Repository security policy #1825:       SUCCESS / run 31307460948
VillAIgence CI #2190:                   SUCCESS / run 31307460913
VillAIgence Production Soak #209:       SUCCESS / run 31307461008
VillAIgence GitHub Release #543:        SUCCESS / run 31307460937
release publication job:                SKIPPED
independent runtime review P0/P1/P2:    0 / 0 / 0
open review threads:                    0
```

Main CI passed common/mock-provider tests, risk-selected server GameTests, Fabric + NeoForge, production acceptance contracts, staged production acceptance, persistence recovery and package verification. Production Soak passed constrained-heap authenticated concurrency, exact production staging and five restart cycles. Release dry-run passed exact acceptance/recovery, GameTests/loaders, package smoke and accepted/package JAR identity without publication.

Canonical implementation evidence:

```text
docs/superpowers/evidence/2026-08-09-provenance-aware-rumors-tdd.md
```

The connected GitHub surface used for the final handoff did not expose the ordered list of all intermediate PR commits, so the evidence ledger explicitly marks historical per-stage RED SHA/run pairs that could not be reconstructed rather than fabricating them. Final exact-head delivery evidence is complete and directly observed.

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

Recent exact-head evidence:

```text
PR #127
  merge:                                 e020f54258d468fd37b0fa5ada5bbc8b6c7c2f77
  security / CI / soak / release dry:    SUCCESS / SUCCESS / SUCCESS / SUCCESS

PR #129
  merge:                                 0f904315f890f588e33adce1a27620ed06a94457
  security / CI / soak / release dry:    SUCCESS / SUCCESS / SUCCESS / SUCCESS

PR #131
  merge:                                 9827a3b511421036c7ae6733fd4fabe4efc8e0c1
  security / CI / soak / release dry:    SUCCESS / SUCCESS / SUCCESS / SUCCESS

PR #133
  merge:                                 aacfe19cccbc8fc03c7959956873d1bd777e6ee2
  security / CI / soak / release dry:    SUCCESS / SUCCESS / SUCCESS / SUCCESS

PR #135
  verified head:                         d2d487d980c7ffe9819e3250489519005fd6767c
  merge:                                 f1fdee1fa1cd0b3a04a2f33357d50d7ae4c1a6d7
  Repository security policy #1825:     SUCCESS / run 31307460948
  VillAIgence CI #2190:                 SUCCESS / run 31307460913
  VillAIgence Production Soak #209:     SUCCESS / run 31307461008
  VillAIgence GitHub Release #543:      SUCCESS / run 31307460937
  release publication job:              SKIPPED
  independent review P0/P1/P2:          0 / 0 / 0
  open review threads:                  0
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

PRs #127, #129, #131, #133 and #135 do not alter this installed-release claim. They are merged unreleased capability until a later release boundary is prepared and explicitly accepted.

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

PR #135 already updated root `[Unreleased]` in the runtime PR. This documentation handoff changes only canonical state/roadmap documents and does not duplicate the product changelog entry.

---

# Known gaps and technical debt

1. `VAI-CONCUR-004` real two-graphical-client Operator Lore conflict presentation remains deferred.
2. `VAI-M2-INST-005` real second-player installed isolation remains untested; automated current-player/NPC-global/shared-scope isolation exists before both recent/durable prompt allocation.
3. Contradictory BELIEFs/rumors do not yet have a first-class server-owned representation beyond coexisting sourced entries; conflict state, uncertainty and bounded distortion are the next Memory 2.0 product gap.
4. Multi-hop knowledge transfer remains explicitly server-invoked. Autonomous initiation, visible NPC↔NPC conversation presentation and voice are future product slices rather than implicit behavior in PR #135.
5. Causal relationship history currently records deterministic `DIALOGUE_TURN` process linkage; richer psychological/told/inferred causal explanations intentionally remain outside the authority model and require a separate provenance-aware design if later desired.
6. `RelationshipCausalHistory` provides resolved source-aware queries, while prompt context currently consumes bounded `RELATIONSHIP_CAUSE` through normal episodic/social retrieval rather than a dedicated resolved-causal prompt surface.
7. `PersistentChatMemory` is a no-storage compatibility façade and may be removed when the inherited AI call surface is refactored.
8. Historical config fields for the removed raw conversation store remain deserializable compatibility baggage.
9. Historical Javadoc/deprecation warnings remain non-blocking.

---

# Next optimal delivery step

The next product slice is **contradiction representation without truth promotion**, built on the accepted exact multi-hop provenance chain.

Memory 2.0 can now preserve exact source-backed social ancestry through up to eight NPC-to-NPC retellings without turning repetition into FACT. The next missing capability is to represent when sourced BELIEFs conflict, preserve both sides and their provenance, and expose uncertainty without letting the provider choose which claim is true.

Required product boundary:

```text
retained sourced BELIEF / rumor A
+ retained sourced BELIEF / rumor B
→ deterministic server-owned contradiction relation/state
→ both claims remain independently inspectable
→ provenance and visibility remain unchanged
→ current SYSTEM_OBSERVED FACT, when present, remains authoritative
→ contradiction metadata never promotes or deletes a BELIEF by itself
```

Recommended design/TDD progression:

```text
contradiction semantics/specification
→ define exact server-owned contradiction identity and bounded representation
→ RED: compatible equivalent claims do not become contradictions
→ RED: opposing sourced claims can coexist and receive deterministic conflict metadata
→ RED: contradiction metadata never promotes either BELIEF to FACT
→ RED: current observed FACT still wins while conflicting rumors remain preserved as non-authoritative history
→ RED: player/private/shared eligibility is evaluated before any contradiction context allocation
→ RED: replay/restart/corroboration does not duplicate conflict state
→ RED: pressure/forgetting of one side cannot fabricate a surviving unsupported contradiction
→ deterministic multi-NPC conflicting-rumor simulation
→ full selected CI / production / soak / release dry-run
```

Required invariants:

- PR #135 provenance chain remains unchanged and authoritative only as process evidence;
- contradiction is not truth resolution;
- no provider/model output chooses which claim wins, source IDs, truth class, visibility, retention or confidence;
- current `SYSTEM_OBSERVED` FACT remains structurally authoritative;
- repeated/corroborated BELIEF remains BELIEF;
- contradiction identity and replay behavior are deterministic and bounded;
- no implicit global knowledge distribution;
- existing `32` candidate / `24+8` long-horizon / `6` result bounds and player privacy eligibility stay intact unless a separate measured design explicitly changes them;
- no legacy `memory.json` migration/dual reader returns.

After contradiction representation:

```text
uncertainty / bounded distortion
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