# VillAIgence Roadmap

> **Canonical product roadmap.** Read `docs/PROJECT_STATE.md` first for exact implementation and validation state. Read root `CHANGELOG.md` for release/product history.
>
> Last reconciled: **2026-08-09**, after PR #133 merged source-backed server-owned NPC-to-NPC knowledge transfer.

## Product vision

VillAIgence is evolving from an MCA-derived AI conversation mod into a **persistent living-society simulation layer for Minecraft**.

The target world contains NPCs that:

- retain stable identity, memory, personality, voice and relationships;
- know only what they observed, learned or were explicitly told;
- distinguish authoritative facts from fallible beliefs and rumors;
- communicate naturally by text and voice;
- act only through server-authoritative policy;
- form families, settlements, factions and social histories;
- exchange information with provenance and uncertainty;
- generate durable emergent stories rather than isolated AI tricks.

> **VillAIgence — Giving villagers a mind of their own.**

Compatibility-sensitive internal naming remains `mca`, `LivingWorld` and `livingworld` until an explicit migration is justified and designed.

---

# Architecture principles

1. **LLM is not authority.** Server state is truth; the model proposes bounded dialogue, claims or intent.
2. **Identity outlives providers.** Changing model/provider must not regenerate NPC identity, memory, relationships or voice.
3. **Fail soft without corruption.** Provider, voice, packet and auxiliary-store failures become controlled states.
4. **Persistence is explicit and world-local.** Important state lives under `<world>/livingworld/`.
5. **Provenance layers stay separate.** Observation, operator lore, episodic memory, FACT, BELIEF and rumor are not interchangeable.
6. **Confidence is not authority.** BELIEF never becomes FACT because a model is confident.
7. **Candidate extraction is not admission, and admission is not authority.** Model output cannot choose source identity or truth class.
8. **Current observations outrank recollection.** Current server-observed facts override conflicting lore or beliefs.
9. **Client convenience never becomes authority.** Permissions, identities, targets, revisions and mutations remain server-owned.
10. **Simulation before spectacle.** Prefer durable causal systems over one-off generated text.
11. **Evidence layers remain explicit.** Unit, integration, GameTest, production candidate, exact release and installed evidence are separate claims.
12. **Unknown CI changes fail closed.** Protected, unsafe and unclassified changes select the complete required matrix.
13. **Compatibility work requires a supported-data reason.** Experimental pre-1.0 state is not automatically entitled to migration code.
14. **Release identity is immutable.** Recovery may restore assets/metadata only from an existing verified tag commit and never moves the tag.
15. **Changelog is part of delivery.** Notable runtime, persistence, config, release, security and permanent-CI changes update root `CHANGELOG.md` in the same PR.
16. **Runtime behavior follows TDD.** Observe the intended RED before production implementation, then implement the smallest GREEN and re-run the complete selected gates.
17. **Causal history is not retrospective model narration.** A relationship cause records only source-backed process evidence the server can prove; dialogue text does not become FACT merely because a relationship changed during that turn.
18. **Player-scoped prompt memory is filtered before ranking.** Only current-player or NPC-global records may consume bounded prompt candidate slots; shared records remain eligible when the current player participates.
19. **Prompt authority is structurally ordered.** Current observations render before Operator Lore, Semantic Memory and episodic/social history; provider output does not decide precedence.
20. **Long-horizon recall remains hard-bounded.** Eligible memory uses a deterministic recent/durable split before the existing final ranker; durability never grants immortality and foreign memory consumes no prompt slots.
21. **NPC-to-NPC transfer is evidence-backed, never implicit omniscience.** A listener can learn only through an exact server-owned transfer event from a speaker that already owns the selected persisted Semantic knowledge; transferred truth remains BELIEF/NPC_TOLD.

---

# Current execution track

```text
0.1.x reliability/security/compatibility               COMPLETE
MCA selective synchronization S1-S8                    COMPLETE
Operator Lore S9-S10c                                  COMPLETE
M11 Phases A-E                                         COMPLETE AT AUTOMATION LAYER
release/recovery automation                            COMPLETE / VERSION-AWARE

0.2.0 Memory 2.0 persistent-dialogue clean cutover      RELEASED / INSTALLED ACCEPTED
legacy memory.json migration                           CANCELLED BY DESIGN
controlled BELIEF admission contract                   COMPLETE / PR #123
bounded PLAYER_TOLD claim extraction                   COMPLETE / PR #125
trustworthy causal relationship memory                 COMPLETE / PR #127
FACT > BELIEF retrieval regression package             COMPLETE / PR #129
long-horizon recall                                    COMPLETE / PR #131
NPC-to-NPC knowledge transfer                          COMPLETE / PR #133
provenance-aware rumors                                NEXT
```

Immediate sequence:

```text
bounded provenance-chain representation
→ exact multi-hop NPC_TOLD source-chain admission
→ cycle/replay/restart/pressure bounds
→ contradiction representation without truth promotion
→ current observation > rumor preservation
→ uncertainty / bounded distortion
→ settlement-scale information flow without omniscience
```

`VAI-CONCUR-004` remains deferred until two real graphical clients are available. It stays `NOT TESTED`, but does not block current product development because server-side concurrency semantics are already automated.

---

# Completed platform — 0.1.x reliability and M11 automation

The 0.1 line established the reliability/security platform on which later simulation work depends.

Implemented and verified across the line:

- provider parsing and transport hardening;
- bounded retries/deadlines/backpressure and exactly-once effects;
- endpoint/credential/redirect policy;
- deterministic text, voice and Operator Lore acceptance;
- world-local persistence recovery;
- selective MCA gameplay/navigation corrections;
- exact production startup/restart and package identity;
- risk-based Fabric GameTests;
- Fabric + NeoForge build compatibility;
- constrained-heap soak;
- immutable release artifact verification;
- version-aware recovery of incomplete GitHub Release publication.

M11 Phase E completed the deterministic automation program:

```text
34 catalog scenarios
28 AUTOMATED
6 MANUAL_CANARY
0 PLANNED
```

The remaining manual scenarios require installed graphical clients, a physical microphone/UDP path or subjective audible/spatial judgment rather than missing deterministic unit coverage.

Historical exact details remain in root `CHANGELOG.md`, `docs/CHANGELOG.md`, and version-specific validation documents.

---

# Current official release — 0.2.0+1.21.1

`0.2.0` begins the Memory 2.0 release line.

```text
tag:                     0.2.0+1.21.1
release commit:          e426f588efefa6aa48a6e536c4a998421bbda241
installed candidate SHA: 56293f86634b50b2def044429aac6f2cf0d197eb16ac1e60224708f7b3333aee
```

Installed clean-state result:

```text
required:          7 PASS / 0 FAIL
VAI-M2-INST-005:   NOT TESTED / AUTOMATED EVIDENCE ONLY
VAI-CONCUR-004:    NOT TESTED / DEFERRED
```

The release intentionally removed the experimental raw `memory.json` conversation store from current runtime/recovery. The accepted pre-1.0 rollout boundary is a clean LivingWorld state; no legacy conversation importer or dual-reader is planned.

PRs #127, #129, #131 and #133 are merged after this release and remain `[Unreleased]`; their automated acceptance must not be represented as installed `0.2.0` evidence.

---

# 0.2 — Memory 2.0

## Goal

Move from raw conversation history to bounded, layered, provenance-aware memory that can support social simulation without making the LLM omniscient or authoritative.

```text
Working Memory        recent bounded prompt context
Episodic Memory       meaningful events and dialogue
Semantic Memory       sourced FACT/BELIEF knowledge
Relationship Memory   causal social history
```

## Implemented foundation

- immutable episodic MemoryEvents;
- DIALOGUE / OBSERVATION / ACTION / RELATIONSHIP_CHANGE / RELATIONSHIP_CAUSE;
- structured text/voice DIALOGUE payloads;
- structured exact before/after relationship-transition payloads;
- source-linked deterministic dialogue-turn causal relationship events;
- exact NPC/player isolation;
- exact current-player/NPC-global/shared prompt eligibility before candidate allocation;
- deterministic dual-tier long-horizon selection at the existing hard candidate bound;
- durability-aware bounded episodic/social pressure retention using authoritative Minecraft game time;
- NPC-global memories remain fully relevant after eligibility;
- deterministic retrieval and idempotency;
- bounded Working Memory;
- typed FACT/BELIEF semantic entries;
- controlled `SYSTEM_OBSERVED` FACT ingestion;
- deterministic consolidation/source union;
- deterministic pressure-based forgetting;
- restart-safe world-local persistence;
- exact read-only owner+UUID authority lookup for source-backed lifecycle validation;
- source-backed listener-owned `NPC_TOLD` BELIEF transfer between NPCs;
- explicit event-vs-semantic partial-retention outcomes for transfer;
- current observed facts structurally outrank Operator Lore, Semantic Memory and stale episodic/social history in snapshot prompt framing;
- multi-session, multi-day, restart, pressure and mixed-scope regression evidence.

## Completed — persistent-dialogue clean cutover

Released in `0.2.0+1.21.1`.

```text
successful text/voice turn
→ one post-success Memory2DialogueLifecycle write
→ structured DIALOGUE MemoryEvent
→ memory2.json

next turn
→ exact NPC/player DIALOGUE retrieval
→ filter before limit
→ chronological user/assistant reconstruction
→ Working Memory bounds
→ prompt
```

Explicitly not part of current architecture:

```text
NO legacy memory.json importer
NO migration checkpoint ledger
NO dual persistent reads
NO summary parsing to recover dialogue roles
```

## Completed — controlled BELIEF admission

Merged through PR #123 / `fd7e9a1099cd73876acce8aaf99705b3763a28c6`.

```text
SYSTEM_OBSERVED → FACT path only
PLAYER_TOLD     → BELIEF only
NPC_TOLD        → BELIEF only
INFERRED        → BELIEF only
```

Implemented boundary:

- `PLAYER_TOLD` requires matching `PLAYER_TOLD` DIALOGUE evidence;
- `NPC_TOLD` requires matching `NPC_TOLD` DIALOGUE evidence;
- `INFERRED` remains non-authoritative with explicit persisted source evidence;
- `SYSTEM_OBSERVED` is rejected through the BELIEF API;
- source-event identity comes from persisted evidence;
- missing/blank/unsupported inputs fail closed;
- replay is idempotent;
- equivalent corroborating claims use deterministic source-union consolidation.

## Completed — bounded PLAYER_TOLD claim extraction

Merged through PR #125 / `b60bcf3c296340946afb443da5cfb4c0d3a793a6`.

The implementation deliberately reuses the existing structured OpenAI/OpenRouter reply rather than making a second extraction call.

```text
provider structured response
→ sanitized visible message
→ exact PLAYER_TOLD DIALOGUE persists
→ bounded beliefCandidates strings
→ verify current player belongs to source DIALOGUE
→ SemanticBeliefAdmissionPolicy
→ BELIEF persistence
```

Properties:

- opt-in: `semanticBeliefExtractionEnabled=false` by default;
- default limit `3`, hard limit `8` candidates per turn;
- candidate statement hard bound `240` Unicode code points;
- NFKC/whitespace/control normalization and deterministic deduplication;
- provider supplies statement strings only;
- server fixes owner, player, source event, BELIEF kind and `PLAYER_TOLD` provenance;
- DIALOGUE must persist before semantic admission;
- malformed/empty metadata fails soft without invalidating the visible NPC reply;
- failed/empty provider response creates no BELIEF;
- exact replay does not duplicate;
- corroborating equivalent claims union source event IDs;
- no AI→FACT path;
- classic/Inworld paths remain outside this slice.

TDD/review evidence is recorded in PR #125 and `docs/PROJECT_STATE.md`.

### Exit criterion — met

VillAIgence can learn a bounded non-authoritative claim explicitly attributed to the latest player dialogue, preserve where it came from, survive retries/restart without duplication, and never confuse that claim with server truth.

## Completed — trustworthy causal relationship memory

Merged through PR #127 / `e020f54258d468fd37b0fa5ada5bbc8b6c7c2f77`.

### Implemented model

Relationship transition and causal linkage remain separate:

```text
provider relationshipDelta proposal
→ server validates/clamps/persists exact relationship state
→ RELATIONSHIP_CHANGE
  - SYSTEM_OBSERVED
  - exact before/after RelationshipTransition

successful visible reply
→ exact DIALOGUE persisted

exact same NPC/player/gameTime source pair
→ RELATIONSHIP_CAUSE(DIALOGUE_TURN)
  - relationshipChangeEventId
  - evidenceEventId
  - transitionSnapshot
```

Authority properties:

- free-form model reason text is not part of the provider schema and cannot become an authoritative cause;
- cause kind, source UUIDs, owner/player, transition state, provenance and confidence are server-owned;
- both exact source events must already be present in world-local `memory2.json`;
- source owner/player and `gameTime` must match exactly;
- causal summary is deterministic/generic and does not copy dialogue prose;
- `RELATIONSHIP_CAUSE` states only that the transition occurred during that dialogue turn;
- dialogue content does not become FACT through causal linkage;
- `RELATIONSHIP_CAUSE` is not automatically projected to Semantic Memory.

Persistence/retrieval properties:

- deterministic cause IDs make replay idempotent;
- result-bearing relationship ingestion rereads and returns the exact event actually retained in the store, including duplicate replay;
- cause ordering deterministically follows its source events under bounded capacity;
- source UUIDs plus transition snapshot survive restart inside the cause payload;
- bounded retention may evict referenced source events, but query results expose them as unavailable rather than inventing replacements;
- `RelationshipCausalHistory` filters exact NPC/player eligibility before limiting and resolves only exact source UUID/type/owner/participants.

TDD/review hardening caught and fixed before merge:

- cause/source retention timestamp tie;
- cross-turn relationship/dialogue linking;
- replay API returning a reconstructed duplicate instead of the exact persisted event.

Final exact-head evidence is recorded in `docs/PROJECT_STATE.md` and PR #127.

### Exit criterion — met for this slice

VillAIgence can persist and query an exact relationship transition together with a deterministic server-authored link to the exact persisted dialogue turn during which the transition occurred, preserve source IDs/restart/replay behavior, and refuse to treat generated retrospective explanation as authoritative cause.

More expressive psychological, told or inferred causal narratives remain a separate future provenance-aware design rather than an implicit extension of this authoritative path.

## Completed — FACT > BELIEF retrieval precedence

Merged through PR #129 / `0f904315f890f588e33adce1a27620ed06a94457`.

### Implemented retrieval boundary

```text
NPC-owned semantic / episodic memory
→ exact current-player-or-NPC-global eligibility
→ bounded candidate window
→ deterministic ranking
→ immutable snapshot prompt
```

Player-scope behavior:

- non-empty Semantic Memory scopes that omit the current player are excluded before the 32-candidate bound;
- episodic/social events with external participants that omit the current player are excluded before the 32-candidate bound;
- foreign-player relationship-change and causal-history entries cannot consume candidate slots or enter another player's prompt;
- NPC-global records remain eligible;
- shared records remain eligible when the current player participates alongside another entity;
- ranker weights and deterministic tie-breakers stay server-owned and bounded.

### Implemented prompt authority

Snapshot memory is no longer duplicated through `PlayerModule → MemoryModule` during immutable context capture. The snapshot renders dedicated layers exactly once:

```text
stable NPC/player descriptive context
→ CURRENT OBSERVED WORLD FACTS
→ Operator Lore
→ Semantic Memory
→ episodic / relationship social history
→ structured-response / tool instructions
```

Properties:

- current observations are authoritative for the turn;
- current relationship state precedes stale `RELATIONSHIP_CHANGE` / `RELATIONSHIP_CAUSE` history;
- Operator Lore remains background context;
- conflicting BELIEFs remain BELIEF and are not silently resolved or promoted;
- causal history does not upgrade dialogue prose into FACT;
- the classic path retains `MemoryModule`, but its ContextProviders share the same player-eligibility boundary;
- the obsolete `MixinOpenAIChatAI` prompt insertion was removed;
- provider schema, retry/transport, action authority, relationship mutation, persistence format and config remain unchanged.

TDD boundaries were observed separately for semantic isolation, episodic/causal isolation, snapshot memory de-duplication, four-layer prompt composition, and direct OpenAI wiring. Independent review added preservation regressions for shared current-player scopes without requiring a production change.

Final exact-head evidence is recorded in `docs/PROJECT_STATE.md` and PR #129.

### Exit criterion — met

Current server-observed truth deterministically controls snapshot prompt framing; foreign-player Memory 2.0 data is excluded before candidate allocation, eligible current-player/NPC-global/shared records stay bounded and deterministic, and no model/provider output decides visibility or truth precedence.

## Completed — long-horizon recall

Merged through PR #131 / `9827a3b511421036c7ae6733fd4fabe4efc8e0c1`.

### Implemented retrieval model

At the existing hard prompt candidate bound:

```text
eligible memory
→ 24 newest eligible records
+ 8 strongest durable eligible records
→ deterministic UUID de-duplication
→ existing domain ranker
→ at most 6 prompt records
```

Properties:

- eligibility precedes both recent and durable pools;
- foreign-player and other-NPC memory consumes zero prompt slots;
- current-player, NPC-global and shared current-player scopes remain eligible;
- NPC-global records are fully relevant after eligibility;
- Semantic Memory keeps its existing deterministic persistence retention;
- ranker/result bounds stay finite and server-owned.

### Implemented episodic/social pressure retention

`MemoryEventStore` no longer evicts solely by oldest-first FIFO. Under bounded pressure it uses deterministic durability and authoritative Minecraft game-time decay.

Durability inputs are persisted/server-owned only:

```text
importance
confidence
absolute emotional weight
provenance
event type
gameTime age
```

Ordering intentionally makes ordinary DIALOGUE the weakest event tier while giving source-backed relationship history stronger bounded retention. No type is immortal; enough authoritative game time eventually overcomes durability.

A candidate that is immediately rejected under pressure does not rewrite `memory2.json` when the retained state did not change.

### Verification package

Observed RED→GREEN gates separately proved:

- retained-but-starved Semantic recall;
- the pure bounded recent/durable selector;
- FIFO episodic pressure loss;
- the pure episodic retention policy;
- rejected weak-append no-op persistence;
- retained-but-starved episodic recall;
- NPC-global relevance after eligibility.

Preservation simulations additionally exercise:

- multi-day authoritative game time;
- multiple fresh-world persistence reloads;
- exact survivor and prompt-context equality across sessions;
- hundreds of Semantic + episodic records;
- two NPCs, current/foreign players, NPC-global and shared scopes;
- restart/pressure isolation;
- no wall-clock-dependent assertions.

Final exact-head evidence is recorded in `docs/PROJECT_STATE.md`, PR #131 and the canonical TDD ledger.

### Exit criterion — met

An NPC can retain and retrieve important Semantic and episodic/social memory across multi-session, multi-day game time, bounded pressure and restart while weak memory decays predictably, privacy remains exact, and current server-observed truth still outranks stale recollection.

## Completed — NPC-to-NPC knowledge transfer

Merged through PR #133 / `aacfe19cccbc8fc03c7959956873d1bd777e6ee2`.

The accepted first slice is deliberately a strict server-owned transfer primitive rather than autonomous visible NPC conversation.

### Implemented transfer model

```text
speaker NPC A owns exact persisted Semantic FACT/BELIEF
→ server selects source entry by exact owner + UUID
→ authoritative exact reread validates immutable source snapshot
→ canonical listener-owned DIALOGUE / NPC_TOLD evidence persists
→ exact persisted evidence reread validates every canonical field
→ existing BELIEF admission path
→ listener NPC B receives BELIEF / NPC_TOLD
→ retained post-consolidation entry must contain exact transfer-evidence UUID
```

Canonical evidence properties:

```text
ownerNpcId:             listener
participants:           [listener, speaker]
type:                   DIALOGUE
provenance:             NPC_TOLD
gameTime:               authoritative server gameTime
createdAtEpochMillis:   0
importance:             50
emotionalWeight:        0
confidence:             50
dialogue payload:       null
summary:                NPC told: <bounded normalized statement>
```

Authority/visibility properties:

- caller does not provide arbitrary claim text, provenance, Semantic kind, scope, source-event IDs, importance or confidence;
- source must already exist and be owned by the claimed speaker;
- speaker FACT or BELIEF always becomes listener BELIEF/NPC_TOLD;
- source FACT authority and original upstream provenance/source chain are not copied to listener authority;
- listener semantic subject scope is the canonical source UUID set; speaker is not inserted merely because it was the provenance actor;
- one transfer creates knowledge only for the listener, not unrelated NPCs;
- raw transfer evidence does not masquerade as player Working Memory because it has no player-oriented `DialogueExchange`;
- existing player-private/NPC-global/shared eligibility remains unchanged.

Persistence/replay properties:

- exact transfer evidence UUID is versioned and deterministic from listener, speaker, source Semantic entry and authoritative game time;
- exact retry is byte-idempotent in both `memory2.json` and `semantic-memory.json`;
- later transfer at another game time creates distinct evidence;
- equivalent listener claims consolidate deterministically and union exact evidence UUIDs;
- event pressure rejection returns `SOURCE_NOT_RETAINED` and creates no BELIEF;
- semantic pressure rejection returns `BELIEF_NOT_RETAINED` while retaining the legitimate transfer event;
- no distributed rollback fabricates/deletes a transfer event that actually occurred;
- transfer participates in existing bounded retention/long-horizon retrieval and remains evictable.

### Verification package

Strict TDD observed:

- compile RED for exact authority lookup APIs;
- compile RED for canonical transfer adapter/policy contracts;
- compile RED for lifecycle/result API;
- behavioral RED after the lifecycle shell compiled: 557 common tests / exactly 2 expected transfer failures / 555 PASS;
- minimal lifecycle GREEN;
- preservation-only GREEN gates for invalid authority, replay/consolidation, pressure outcomes, fresh-root reload, privacy/Working Memory isolation and deterministic long-horizon multi-NPC simulation.

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

### Exit criterion — met

NPC A can explicitly transmit one bounded sourced claim to NPC B through a deterministic server-owned transfer event; B stores it as inspectable `NPC_TOLD` BELIEF, unrelated NPCs do not learn it automatically, retries/restart/pressure remain deterministic, and current server truth still outranks the transferred recollection.

## NEXT — provenance-aware rumors

Build on the accepted direct transfer primitive with bounded multi-hop provenance and explicit uncertainty, without weakening the FACT/BELIEF boundary.

### Goal

```text
origin Semantic knowledge
→ direct source-backed NPC→NPC transfer
→ listener NPC_TOLD BELIEF
→ later bounded retelling
→ exact origin + speaker-hop provenance
→ non-authoritative rumor state
→ bounded retrieval / contradiction representation
```

### Required design decisions before implementation

- minimal persistent representation for origin and transfer-hop chain;
- hard maximum hop depth and serialized provenance size;
- whether rumor is a distinct typed semantic class or remains BELIEF plus explicit bounded rumor metadata;
- deterministic behavior when two chains corroborate the same claim;
- cycle detection/canonicalization for `A→B→A`, repeated retells and replay;
- representation of contradictions without provider-selected truth resolution;
- how uncertainty may decay/change across hops without becoming authority.

### Required TDD scenarios

- A→B direct transfer keeps the exact PR #133 contract unchanged;
- B can retell to C only knowledge B actually retains;
- C retains exact origin and exact server-proven transfer hops;
- fabricated/missing hop evidence fails closed;
- cycles/replay cannot create unbounded chain growth or duplicate logical hops;
- repeated/corroborated rumor remains BELIEF/non-authoritative and never becomes FACT;
- contradictory rumors can coexist without generated winner selection;
- one chain does not leak across unrelated NPC/player scopes;
- restart and long-horizon pressure preserve bounded provenance deterministically;
- current observed FACT still outranks every rumor/BELIEF;
- no provider/model call controls source IDs, chain depth, truth class, visibility or retention.

### Delivery order

1. **Rumor provenance design/spec gate**
   - define exact semantics, hard bounds and storage impact before code.
2. **Representation RED**
   - source chain absent from current direct-transfer Semantic model;
   - prove the exact minimal new state required.
3. **Multi-hop authority RED**
   - source ownership + exact persisted hop evidence required at every retell.
4. **Cycle/replay/corroboration RED**
   - deterministic bounded chains and source union.
5. **Contradiction/privacy/authority preservation**
   - BELIEF/rumor remains below current server truth;
   - player/NPC isolation remains pre-limit.
6. **Pressure/restart simulation**
   - multiple NPCs, multiple chains, cycles, contradictory claims, hundreds of unrelated records, no wall-clock assertions.
7. **Full delivery gate**
   - common/provider tests and selected GameTests;
   - Fabric + NeoForge;
   - production startup/restart + persistence recovery;
   - security + constrained soak + release dry-run;
   - independent exact-head review.

### Invariants

- direct PR #133 transfer remains the trusted primitive;
- rumor remains non-authoritative even after repetition/corroboration;
- exact origin/hop identities are server-owned and correspond to persisted evidence;
- provenance chain is hard-bounded and deterministic;
- no implicit settlement-wide/global distribution;
- current observed FACT outranks rumor/BELIEF;
- existing candidate/result/long-horizon privacy bounds remain intact;
- no legacy `memory.json` importer/dual reader returns.

### Exit criterion

A sourced claim can move through more than one NPC with a bounded inspectable server-backed provenance chain, survive retry/restart/pressure deterministically, represent disagreement without invented truth, and remain explicitly non-authoritative.

## Later 0.2 — uncertainty / contradiction / bounded distortion

Once exact multi-hop provenance exists, model how fallible social information changes without granting the model authority.

Possible semantics:

```text
origin source
speaker chain
confidence / uncertainty
contradiction state
distortion count
bounded transformation budget
```

A rumor remains non-authoritative even when repeated by many NPCs.

## 0.2 exit criterion

Memory 2.0 is complete when persistent NPC memory is:

- layered;
- bounded;
- provenance-aware;
- restart-safe;
- deterministic under replay;
- able to learn controlled non-authoritative claims;
- able to retain source-backed causal relationship history;
- able to retain important memories across realistic temporal/pressure horizons;
- able to transfer knowledge between NPCs without omniscience;
- able to represent bounded multi-hop provenance, rumors and contradictions without turning them into FACT;
- independent of the removed raw conversation store.

---

# 0.3 — Personality and NPC↔NPC social graph

## Goal

Persistent bounded personality plus pairwise social state that changes dialogue and behavior.

Potential personality dimensions:

```text
temperament
values
goals
fears
speech style
morality
ambition
curiosity
sociability
aggression
loyalty
```

Pair state may include friendship, trust, respect, fear, family, rivalry, romance and grudges.

Personality is persistent game state, not a fresh LLM-generated profile on every conversation.

### Exit criterion

Two NPCs retain durable relationship/personality history that affects dialogue, decisions and information exchange after restart.

---

# 0.4 — Knowledge ecosystem and rumors

## Goal

Expand the 0.2 transfer primitives into settlement-scale provenance-aware information flow.

Target knowledge classes may include:

```text
OBSERVED
TOLD_BY_PLAYER
TOLD_BY_NPC
OFFICIAL
INFERRED
RUMOR
UNKNOWN
```

### Exit criterion

Information moves through a settlement without omniscient distribution, conflicts remain representable, and source history remains inspectable.

---

# 0.5 — Autonomous NPC agents

## Goal

Budgeted server-authoritative behavior based on needs, goals, social context and remembered information.

```text
perceive
→ evaluate needs/goals/social context
→ choose bounded intent
→ server policy validation
→ act
→ observe result
→ remember
```

The LLM may propose intent but never arbitrary Minecraft commands.

Required controls:

- event-driven scheduling rather than per-tick LLM calls;
- per-NPC/global budgets;
- action whitelist/policy;
- server-side target revalidation;
- bounded retry/backpressure;
- exactly-once effects.

### Exit criterion

NPCs can pursue simple persistent goals autonomously without compromising server authority or performance.

---

# 0.6 — Settlement simulation

## Goal

Villages become persistent social/economic systems with population, households, professions, resources, safety, morale, shared projects and public memory.

### Exit criterion

Settlement state changes over time and meaningfully affects individual NPC goals and behavior.

---

# 0.7 — Factions and politics

## Goal

Persistent alliances, disputes, leadership, rules and inter-settlement relations with server-owned consequences.

### Exit criterion

Faction/political state survives restart, is causally grounded in simulation events, and changes NPC/settlement behavior.

---

# 0.8 — Emergent stories

## Goal

Multi-session narratives grounded in persistent events, memories, relationships, settlements and factions.

Story is the human-readable consequence of simulation history; the system must not generate a story first and retrofit state afterward.

### Exit criterion

Players can return after multiple sessions and encounter explainable ongoing social narratives rooted in recorded world history.

---

# 0.9 — Performance, large servers and local models

## Goal

Scale the living society without turning AI into a per-NPC-per-tick cost center.

Work includes:

- event-driven scheduling;
- global/per-NPC model budgets;
- backpressure and cancellation;
- cache/retrieval profiling;
- large-population simulation soak;
- multi-day stability evidence;
- optional local models;
- provider replacement without identity migration.

### Exit criterion

Large populations remain bounded in CPU, memory, provider calls and persistence growth under realistic server workloads.

---

# 1.0 — Persistent Living Society

```text
NPC Identity
+ Personality
+ Memory
+ Relationships
+ Knowledge
+ Voice
+ Goals
+ Autonomous Actions
+ Settlements
+ Factions
+ Emergent History
= VillAIgence
```

1.0 means the systems above form one coherent persistent simulation, not merely a collection of AI features.

---

# Delivery and TDD governance

A milestone is not complete because code compiles or one CI job is green.

Runtime behavior follows:

```text
specification
→ RED regression/contract test
→ observe intended RED
→ minimal GREEN implementation
→ focused tests
→ relevant regression suite
→ Fabric + NeoForge where applicable
→ production/server acceptance
→ security policy
→ soak/release dry-run when selected
→ independent diff review
→ exact candidate / installed acceptance when required
→ root CHANGELOG.md update
→ PROJECT_STATE / ROADMAP reconciliation when delivery boundary changes
```

Rules:

1. Do not write production behavior before the intended RED has been observed.
2. Do not weaken assertions merely to make CI green.
3. Exact-release and installed evidence are separate from unit/automation evidence.
4. Deferred manual evidence remains explicitly deferred.
5. Significant product/runtime/persistence/config/release/security/permanent-CI changes update root `[Unreleased]` in the same PR.
6. Release PRs move shipped `[Unreleased]` items into the exact version section rather than duplicating them.
7. Before starting new work, reconcile these documents against live GitHub state.