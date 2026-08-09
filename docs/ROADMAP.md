# VillAIgence Roadmap

> **Canonical product roadmap.** Read `docs/PROJECT_STATE.md` first for exact implementation and validation state. Read root `CHANGELOG.md` for release/product history.
>
> Last reconciled: **2026-08-09**, after PR #135 merged provenance-aware bounded multi-hop rumors.

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
22. **Rumor ancestry is bounded process evidence, not truth authority.** Multi-hop retelling carries one immutable source-backed v2 ancestry path capped at eight hops; canonical ancestry is selected without listener influence and every downstream claim remains BELIEF/NPC_TOLD.

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
provenance-aware bounded multi-hop rumors              COMPLETE / PR #135
contradiction representation without truth promotion   NEXT
```

Immediate sequence:

```text
contradiction representation without truth promotion
→ current observation > contradictory-rumor preservation
→ uncertainty / bounded distortion
→ settlement-scale information flow without omniscience
→ relationship/trust effects on belief confidence as a separate social-epistemology slice
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

PRs #127, #129, #131, #133 and #135 are merged after this release and remain `[Unreleased]`; their automated acceptance must not be represented as installed `0.2.0` evidence.

---

# 0.2 — Memory 2.0

## Goal

Move from raw conversation history to bounded, layered, provenance-aware memory that can support social simulation without making the LLM omniscient or authoritative.

```text
Working Memory        recent bounded prompt context
Episodic Memory       meaningful events and dialogue
Semantic Memory       sourced FACT/BELIEF knowledge
Relationship Memory   causal social history
Rumor Provenance      bounded server-backed social ancestry
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
- bounded immutable v2 multi-hop provenance on direct transfer evidence;
- deterministic listener-independent ancestry selection;
- explicit event/Semantic/provenance pressure and rejection outcomes;
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

### Exit criterion — met

Text and voice dialogue survive restart as structured Memory 2.0 data and reconstruct bounded Working Memory without the removed raw conversation store.

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

### Exit criterion — met

A non-authoritative Semantic BELIEF can be admitted only from an exact server-owned source contract and cannot cross the FACT authority boundary.

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
- exact replay does not duplicate;
- corroborating equivalent claims union source event IDs;
- no AI→FACT path.

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

- free-form model reason text cannot become an authoritative cause;
- cause kind, source UUIDs, owner/player, transition state and provenance are server-owned;
- both exact source events must already exist;
- source owner/player and `gameTime` must match exactly;
- `RELATIONSHIP_CAUSE` states only that the transition occurred during that dialogue turn;
- dialogue content does not become FACT through causal linkage;
- `RELATIONSHIP_CAUSE` is not automatically projected to Semantic Memory.

### Exit criterion — met

VillAIgence can persist and query an exact relationship transition together with a deterministic server-authored link to the exact persisted dialogue turn during which the transition occurred without inventing psychological truth.

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

- foreign-player Semantic Memory entries are excluded before the 32-candidate bound;
- foreign-player episodic/social events are excluded before the 32-candidate bound;
- foreign relationship-change and causal-history entries cannot enter another player's prompt;
- NPC-global records remain eligible;
- shared records remain eligible when the current player participates.

Snapshot authority:

```text
stable NPC/player descriptive context
→ CURRENT OBSERVED WORLD FACTS
→ Operator Lore
→ Semantic Memory
→ episodic / relationship social history
→ structured-response / tool instructions
```

### Exit criterion — met

Current server-observed truth deterministically controls snapshot prompt framing; foreign-player Memory 2.0 data is excluded before candidate allocation and no model/provider output decides visibility or truth precedence.

## Completed — long-horizon recall

Merged through PR #131 / `9827a3b511421036c7ae6733fd4fabe4efc8e0c1`.

### Implemented retrieval model

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

Episodic/social pressure retention uses server-owned importance, confidence, absolute emotional weight, provenance, event type and authoritative Minecraft `gameTime`. No memory class becomes immortal.

### Exit criterion — met

An NPC can retain and retrieve important Semantic and episodic/social memory across multi-session, multi-day game time, bounded pressure and restart while weak memory decays predictably and current server-observed truth still outranks stale recollection.

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
```

Authority/visibility properties:

- caller does not provide arbitrary claim text, provenance, Semantic kind, scope, source-event IDs, importance or confidence;
- source must already exist and be owned by the claimed speaker;
- speaker FACT or BELIEF always becomes listener BELIEF/NPC_TOLD;
- source FACT authority is not copied to listener authority;
- one transfer creates knowledge only for the listener, not unrelated NPCs;
- raw transfer evidence does not masquerade as player Working Memory;
- existing player-private/NPC-global/shared eligibility remains unchanged.

Persistence/replay properties:

- transfer evidence identity is deterministic;
- exact retry is byte-idempotent;
- later transfer at another game time creates distinct evidence;
- equivalent listener claims consolidate deterministically;
- event pressure rejection returns `SOURCE_NOT_RETAINED`;
- semantic pressure rejection returns `BELIEF_NOT_RETAINED` while retaining real transfer evidence;
- transfer remains bounded and evictable.

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

NPC A can explicitly transmit one bounded sourced claim to NPC B through deterministic server-owned evidence; B stores it as inspectable `NPC_TOLD` BELIEF, unrelated NPCs do not learn it automatically, and current server truth still outranks the transferred recollection.

## Completed — provenance-aware bounded multi-hop rumors

Merged through PR #135 / `f1fdee1fa1cd0b3a04a2f33357d50d7ae4c1a6d7`.

This slice deliberately adds exact ancestry, **not** uncertainty, contradiction resolution, free-form distortion or autonomous rumor spread.

### Implemented model

Every new v2 direct NPC-to-NPC transfer evidence may carry one immutable bounded ancestry:

```text
Origin
  exact origin NPC
  exact origin Semantic entry
  origin FACT/BELIEF kind
  origin provenance
  exact normalized statement
  exact canonical semantic subject scope

Hop[]
  speaker NPC
  listener NPC
  speaker Semantic entry
  exact transfer evidence UUID
  authoritative gameTime
```

Lifecycle:

```text
exact persisted speaker Semantic source
→ authoritative exact reread
→ first-hop origin OR canonical retained direct ancestry
→ cycle check
→ hop-limit check
→ deterministic npc-knowledge-transfer-v2 evidence
→ exact evidence persist + reread + validation
→ listener BELIEF / NPC_TOLD
```

### Authority and boundedness

- first-hop origin may be `FACT/SYSTEM_OBSERVED`, `BELIEF/PLAYER_TOLD`, or `BELIEF/INFERRED`;
- an `NPC_TOLD` speaker BELIEF cannot reset origin and may continue only through valid retained structured v2 direct evidence;
- downstream knowledge remains `BELIEF/NPC_TOLD` at every hop;
- max lineage depth is exactly eight hops;
- repeated NPCs are rejected as `PROVENANCE_CYCLE`;
- ninth non-cyclic hop is rejected as `PROVENANCE_LIMIT_REACHED`;
- cycle takes precedence over limit on the selected lineage;
- canonical branch selection is `gameTime DESC`, then evidence UUID ascending;
- resolver receives no proposed listener;
- listener-dependent fallback is forbidden;
- provider/client cannot choose source IDs, ancestry, truth class, visibility, chain depth or retention;
- no summary parsing creates authority.

### Direct evidence versus ancestry

Semantic consolidation remains intentionally simple:

```text
Semantic BELIEF
→ direct sourceEventIds only

Direct v2 transfer MemoryEvent
→ one complete immutable ancestry snapshot
```

Corroboration may produce multiple direct Semantic source IDs, but ancestry UUIDs do not get copied into an unbounded Semantic DAG.

### Restart, pressure and privacy

Verified behavior includes:

- exact two-hop lineage survives fresh-root persistence reload;
- exact replay is byte-idempotent for tested `memory2.json` and `semantic-memory.json` state;
- global/private/shared scopes survive multiple hops exactly;
- speaker/provenance actors do not pollute semantic subject scope;
- rumor evidence remains outside player dialogue Working Memory;
- older physical ancestry may be evicted while a later direct event preserves its immutable ancestry snapshot;
- loss of the current direct evidence blocks further propagation with `PROVENANCE_UNAVAILABLE`;
- rumors/BELIEFs remain evictable under existing retention policies;
- current observed FACT remains structurally authoritative.

### Deterministic simulation

Coverage includes:

```text
10 NPCs
8-hop valid chain
rejected ninth hop
cycle attempts
independent lineages
corroborating direct evidence
>200 unrelated Semantic records
>200 unrelated episodic/social records
forward/reverse pressure insertion
2 fresh-root reloads
private/shared/global scopes
current FACT conflict preservation
```

Final exact-head evidence:

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

Evidence ledger:

```text
docs/superpowers/evidence/2026-08-09-provenance-aware-rumors-tdd.md
```

The final handoff explicitly records that the connected GitHub surface could not reconstruct the ordered intermediate RED commit/run mapping; missing historical pairs are marked as not reconstructed rather than fabricated. Final exact-head delivery proof is complete.

### Exit criterion — met

A sourced claim can move through more than one NPC with a bounded inspectable server-backed provenance chain, survive retry/restart/pressure deterministically, reject cycles/unbounded growth, remain player-scope safe, and stay explicitly non-authoritative even when its origin was FACT.

## NEXT — contradiction representation without truth promotion

Exact provenance exists. The next task is to represent **disagreement**, not decide truth.

### Goal

```text
sourced BELIEF / rumor A
+ sourced BELIEF / rumor B
→ deterministic bounded contradiction metadata/relation
→ both claims remain independently sourced and inspectable
→ no model-selected winner
→ current SYSTEM_OBSERVED FACT, if present, remains authoritative
```

### Required design decisions before implementation

- what exact normalized relationship makes two Semantic BELIEFs contradictory rather than merely different;
- whether contradiction is stored as a separate record, additive metadata, or deterministic relation derived from retained entries;
- exact identity and source binding of contradiction state;
- what happens when one side is forgotten under bounded pressure;
- whether equivalent/corroborating claims collapse before contradiction detection;
- how current FACT conflict is represented without rewriting the BELIEF itself;
- what prompt surface exposes contradiction without allowing memory text to become instructions;
- hard limits so contradiction state cannot become an all-pairs unbounded graph.

### Required TDD scenarios

1. equivalent normalized claims are not contradictory;
2. two explicitly opposing sourced BELIEFs coexist and produce one deterministic bounded contradiction relation;
3. provider/model cannot supply contradiction UUIDs, source IDs, truth winner or authority;
4. repeated/corroborated BELIEF remains BELIEF and does not win by vote count;
5. current `SYSTEM_OBSERVED` FACT still overrides conflicting rumor content for current-world truth;
6. contradiction metadata does not promote, delete or mutate either BELIEF by itself;
7. private/shared/global player visibility remains an eligibility boundary before contradiction prompt allocation;
8. replay/restart does not duplicate contradiction state;
9. forgetting/eviction of one side cannot leave fabricated unsupported authoritative conflict state;
10. many unrelated BELIEFs do not create unbounded all-pairs work or persistence growth;
11. deterministic multi-NPC conflicting-rumor simulations produce equal state across insertion order and fresh reloads;
12. existing long-horizon bounds and provenance-chain rules stay intact.

### Delivery order

1. **Contradiction semantics/spec gate**
   - define exact contradiction semantics and hard complexity bounds.
2. **Pure contract RED**
   - encode normalized equivalence/opposition behavior without persistence side effects.
3. **Source-binding RED**
   - contradiction must reference exact retained server-owned BELIEFs/evidence.
4. **Authority RED**
   - no winner selection, no FACT promotion, current observed FACT still wins.
5. **Lifecycle/replay/restart RED**
   - deterministic idempotent relation state and disappearance behavior.
6. **Privacy/pressure/complexity RED**
   - bounded candidate set; no foreign-player leakage; no unbounded all-pairs graph.
7. **Simulation + full delivery gate**
   - common/provider tests;
   - selected Fabric GameTests + NeoForge build compatibility;
   - production startup/restart + persistence recovery;
   - repository security;
   - constrained soak;
   - release dry-run;
   - independent exact-head review.

### Invariants

- contradiction is metadata/process state, not a truth verdict;
- PR #135 exact provenance remains unchanged and inspectable;
- current server-observed FACT remains authoritative;
- repetition/corroboration never promotes BELIEF to FACT;
- provider/model cannot choose contradiction identity, winner, source IDs, visibility, retention or confidence;
- contradiction representation is bounded and deterministic;
- one NPC's private contradiction does not become global knowledge;
- existing `32` candidate / `24+8` long-horizon / `6` result bounds remain the default boundary unless a separate measured design proves a change necessary;
- no legacy `memory.json` importer/dual reader returns.

### Exit criterion

VillAIgence can retain and expose two conflicting sourced beliefs as bounded inspectable disagreement, survive replay/restart/pressure deterministically, preserve privacy and provenance, and continue to treat only current server-observed FACT as authoritative truth.

## Later 0.2 — uncertainty / bounded distortion

After deterministic contradiction representation, model how fallible social information changes without granting the model authority.

Possible semantics:

```text
origin source
speaker chain
confidence / uncertainty
contradiction state
distortion count
bounded transformation budget
```

Questions to settle explicitly:

- how uncertainty evolves across exact hops;
- which changes are deterministic server policy versus bounded provider suggestions;
- how to preserve original and transformed statements for inspection;
- how distortion is capped in hops/size/rate;
- how trust/relationship effects are separated from truth authority;
- how to prevent repetition from becoming a confidence escalation exploit.

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
- able to preserve bounded exact multi-hop rumor ancestry;
- able to represent contradictions and uncertainty without turning them into FACT;
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

Expand the 0.2 transfer/provenance/contradiction primitives into settlement-scale provenance-aware information flow.

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

This milestone is about **distribution and social knowledge topology**, not retroactively weakening the 0.2 FACT/BELIEF authority model.

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