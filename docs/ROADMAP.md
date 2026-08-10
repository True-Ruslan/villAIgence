# VillAIgence Roadmap

> **Canonical product roadmap.** Read `docs/PROJECT_STATE.md` first for exact implementation/validation state. Read root `CHANGELOG.md` for product/release history and `docs/superpowers/evidence/` for staged TDD evidence.
>
> Last reconciled: **2026-08-10**, after PR #145 merged bounded contradiction candidate/producer policy.

## Product vision

VillAIgence is evolving from an MCA-derived AI conversation mod into a **persistent living-society simulation layer for Minecraft**.

The target world contains NPCs that:

- retain stable identity, memory, personality, voice and relationships;
- know only what they observed, learned or were explicitly told;
- distinguish authoritative facts from fallible beliefs, rumors, disagreement and uncertainty;
- communicate naturally by text and voice;
- act only through server-authoritative policy;
- form families, settlements, factions and social histories;
- exchange information with bounded provenance, fallibility and inspectable transformation;
- generate durable emergent stories rooted in simulation state rather than isolated AI tricks.

> **VillAIgence — Giving villagers a mind of their own.**

Compatibility-sensitive internal naming remains `mca`, `LivingWorld` and `livingworld` until an explicit migration is justified and designed.

---

# Architecture principles

1. **LLM is not authority.** Server state is truth; the model may propose bounded dialogue, claims or intent only through explicit contracts.
2. **Identity outlives providers.** Changing model/provider must not regenerate NPC identity, memory, relationships or voice.
3. **Fail soft without corruption.** Provider, voice, packet and auxiliary-store failures become controlled states.
4. **Persistence is explicit and world-local.** Important state lives under `<world>/livingworld/`.
5. **Provenance layers stay separate.** Observation, Operator Lore, episodic memory, FACT, BELIEF, rumor, disagreement, fallibility and transformation evidence are not interchangeable.
6. **Confidence is not authority.** BELIEF never becomes FACT because of model confidence, repetition, corroboration count, rumor depth, source distance or transformation count.
7. **Candidate extraction is not admission, and admission is not authority.** Model output cannot choose source identity or truth class.
8. **Current observations outrank recollection.** Current server-observed facts override conflicting lore, beliefs, transformed claims, disagreement and fallibility for current-world truth.
9. **Client convenience never becomes authority.** Permissions, identities, targets, revisions and mutations remain server-owned.
10. **Simulation before spectacle.** Prefer durable causal systems over one-off generated text.
11. **Evidence layers remain explicit.** Unit, integration, GameTest, production candidate, exact release and installed evidence are separate claims.
12. **Unknown CI changes fail closed.** Protected, unsafe and unclassified changes select the complete required matrix.
13. **Compatibility work requires a supported-data reason.** Experimental pre-1.0 state is not automatically entitled to migration code.
14. **Release identity is immutable.** Recovery may restore assets/metadata only from an existing verified tag commit and never moves the tag.
15. **Changelog is part of delivery.** Notable runtime/persistence/config/release/security/permanent-CI changes update root `CHANGELOG.md` in the same PR.
16. **Runtime behavior follows TDD.** Observe intended RED before production implementation, then implement smallest GREEN and re-run selected complete gates.
17. **Causal history is not retrospective model narration.** Server-proven process linkage does not make dialogue prose true.
18. **Player-scoped memory is filtered before ranking/allocation.** Foreign-player data consumes zero bounded slots.
19. **Prompt authority is structurally ordered.** Current observations precede Operator Lore, Semantic Memory, live disagreement context and episodic/social history.
20. **Long-horizon recall remains hard-bounded.** Recent/durable selection is deterministic and no memory becomes immortal.
21. **NPC-to-NPC transfer is evidence-backed, never implicit omniscience.** Listener knowledge requires exact speaker-owned persisted source evidence and remains `BELIEF/NPC_TOLD` downstream.
22. **Rumor ancestry is bounded process evidence, not truth authority.** Multi-hop retelling carries immutable server-backed v2 ancestry capped at eight hops.
23. **Canonical ancestry selection is listener-independent.** Cycle/limit rejection does not trigger a lower-branch fallback chosen for the destination.
24. **Contradiction is disagreement metadata, not a verdict.** Recording two conflicting retained claims never selects a winner, promotes FACT, mutates confidence or deletes either claim.
25. **Historical contradiction evidence cannot resurrect forgotten claim text.** Resolved disagreement exists only while both logical claims remain live and player-eligible.
26. **Disagreement prompt context is a bounded data layer.** At most four live relations are rendered using the same Semantic text safety rules.
27. **Fallibility models process history, not truth likelihood.** Source distance and transformation count are derived process metadata and cannot rank or promote a claim.
28. **Missing provenance is explicit.** A retained rumor whose direct provenance is gone becomes `UNRESOLVED`; ancestry/transformation history is not reconstructed from prose.
29. **Transformation is a separate authority boundary.** Current wording distortion is server-deterministic, hard-bounded and preserves exact source provenance; every transformed downstream claim remains BELIEF.
30. **Automatic contradiction production is bounded before classification.** Candidate eligibility and duplicate suppression precede a hard comparison budget; classification is truth-neutral and cannot choose a winner.
31. **Settlement information flow is transfer, not shared omniscience.** Autonomous dissemination must select bounded eligible speaker/listener opportunities and reuse exact source-backed transfer; no settlement-global memory may silently make every NPC know a claim.

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
controlled BELIEF admission                            COMPLETE / PR #123
bounded PLAYER_TOLD extraction                         COMPLETE / PR #125
causal relationship memory                             COMPLETE / PR #127
FACT > BELIEF prompt precedence                        COMPLETE / PR #129
long-horizon recall                                    COMPLETE / PR #131
NPC-to-NPC knowledge transfer                          COMPLETE / PR #133
bounded multi-hop rumor provenance                     COMPLETE / PR #135
Semantic contradiction representation                  COMPLETE / PR #137
contradiction-aware prompt context                     COMPLETE / PR #139
deterministic rumor fallibility                        COMPLETE / PR #141
bounded transformed-claim representation               COMPLETE / PR #143
bounded contradiction candidate/producer policy        COMPLETE / PR #145
settlement-scale information flow without omniscience  NEXT
```

Immediate sequence:

```text
settlement-scale information flow without omniscience
→ relationship/trust effects on belief confidence/fallibility as separate social epistemology
→ Personality + NPC↔NPC Social Graph / 0.3 convergence
```

`VAI-CONCUR-004` remains `NOT TESTED / DEFERRED` until two real graphical clients are available. It does not block current product development because server-side concurrency semantics are automated.

---

# Current official release — 0.2.0+1.21.1

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

The release intentionally removed the experimental raw `memory.json` conversation store from current runtime/recovery. The accepted pre-1.0 rollout boundary is clean-state; no legacy conversation importer or dual reader is planned.

PRs #127, #129, #131, #133, #135, #137, #139, #141, #143 and #145 are merged after this release and remain `[Unreleased]` source capabilities. Their automated evidence must not be represented as installed `0.2.0` acceptance.

---

# 0.2 — Memory 2.0

## Goal

Move from raw conversation history to bounded, layered, provenance-aware and fallibility-aware memory that can support social simulation without making the LLM omniscient or authoritative.

```text
Working Memory          recent bounded prompt context
Episodic Memory         meaningful events/dialogue
Semantic Memory         sourced FACT/BELIEF knowledge
Relationship Memory     causal social history
Rumor Provenance        bounded server-backed ancestry
Disagreement Context    live contradiction relation without verdict
Rumor Fallibility       exact source distance / transform count / unresolved state
Bounded Distortion      one inspectable deterministic omission primitive
Contradiction Producer  bounded automatic candidate production without truth arbitration
Settlement Flow         NEXT: bounded social propagation without shared omniscience
```

## Implemented foundation

- immutable episodic MemoryEvents;
- structured text/voice DIALOGUE payloads;
- exact relationship-transition and causal source linkage;
- exact NPC/player isolation;
- current-player/NPC-global/shared eligibility before bounded allocation;
- bounded recent/durable long-horizon selection;
- deterministic pressure retention using authoritative Minecraft game time;
- typed FACT/BELIEF Semantic Memory;
- controlled `SYSTEM_OBSERVED` FACT ingestion;
- source-backed BELIEF admission;
- deterministic consolidation/source union and forgetting;
- exact source-backed NPC-to-NPC transfer;
- immutable acyclic v2 rumor ancestry capped at eight hops;
- listener-independent deterministic ancestry selection;
- deterministic `SEMANTIC_CONTRADICTION` process evidence without duplicate claim prose;
- live resolved disagreement that disappears when either claim is forgotten;
- privacy eligibility before contradiction result limiting;
- dedicated contradiction-aware prompt context hard-bounded to four relations;
- shared safe Semantic claim rendering;
- immutable server-thread snapshot disagreement capture;
- deterministic five-layer prompt order preserving current observed truth authority;
- deterministic rumor fallibility derived from retained canonical provenance;
- explicit `RESOLVED` one-to-eight-hop distance and `UNRESOLVED` missing-direct-provenance state;
- one server-deterministic `OMIT_TRAILING_SENTENCE` transform with lineage budget 1;
- additive nullable transformation evidence in `memory2.json` v1;
- immutable exact source origin plus transformed current statement;
- resolved `transformationsUsed=0|1`, unresolved `transformationsUsed=UNKNOWN`;
- exact replay/same-ID conflict safety and restart-persistent transformation budget;
- automatic bounded contradiction candidate production after controlled Semantic admission;
- candidate cap 16 and comparison cap 8 per admission;
- same-owner/exact-scope/equivalence filtering before bounded allocation;
- existing-relation suppression before classifier budget;
- deterministic standalone `not` / `не` opposition primitive;
- fresh-root/replay/restart/pressure/privacy/prompt-injection regressions.

## Completed — persistent-dialogue clean cutover

Released in `0.2.0+1.21.1`.

```text
successful text/voice turn
→ structured DIALOGUE MemoryEvent
→ memory2.json
→ exact NPC/player retrieval
→ bounded Working Memory
→ prompt
```

No legacy `memory.json` importer, migration checkpoint ledger or dual persistent read exists.

## Completed — controlled BELIEF admission / extraction

Merged through PRs #123 and #125.

```text
SYSTEM_OBSERVED → FACT path only
PLAYER_TOLD     → BELIEF only
NPC_TOLD        → BELIEF only
INFERRED        → BELIEF only
```

Provider candidate text is not authority. The server binds owner/player/source/provenance/kind, persists DIALOGUE first, bounds/normalizes candidates and rejects unsupported source relationships.

## Completed — causal relationship memory

Merged through PR #127. Server-applied relationship transitions and deterministic source-event causes remain separate from generated psychological narration.

## Completed — FACT > BELIEF prompt precedence

Merged through PR #129. Current observations render before lore/memory and foreign-player memory is excluded before bounded allocation.

## Completed — long-horizon recall

Merged through PR #131.

```text
eligible memory
→ 24 newest
+ 8 strongest durable
→ deterministic de-duplication
→ existing domain ranker
→ at most 6 prompt entries
```

No memory class is immortal.

## Completed — NPC-to-NPC transfer

Merged through PR #133. Exact speaker-owned persisted Semantic FACT/BELIEF can transfer only through server-owned evidence; listener always receives `BELIEF/NPC_TOLD`.

## Completed — bounded multi-hop rumors

Merged through PR #135. Each v2 transfer carries immutable origin + ordered ancestry. Chain is acyclic, listener-independent in branch selection and capped at eight hops.

## Completed — Semantic contradiction representation

Merged through PR #137 / `afcd4f52187e1e419326abf9ae1ae7ac587f2064`.

```text
exact retained claim A
+ exact retained claim B
→ canonical structured SEMANTIC_CONTRADICTION evidence
→ no winner / no truth promotion
→ live relation only while both logical claims remain retained
```

## Completed — contradiction-aware prompt context

Merged through PR #139 / `05dac0eaff408c13bf02ddd25d98acefd4f9cf13`.

```text
retained contradiction evidence
+ both live claims
+ current-player eligibility
→ max 4 resolved relations
→ shared safe claim renderer
→ immutable snapshot disagreement layer
→ prompt after Semantic Memory, before episodic/social history
```

Disagreement remains remembered data rather than a verdict. Current observed facts remain authoritative.

## Completed — deterministic rumor fallibility

Merged through PR #141 / `e0951067227913b8cadb3e73ee34355b0b3302ff`.

This established canonical retained source distance as process metadata without changing claim wording, confidence, ranking or authority. PR #143 subsequently extended current fallibility rendering to include validated transformation history and changed missing-direct-evidence transformation count from the historical structural zero to explicit `UNKNOWN`.

## Completed — bounded transformed-claim representation

Merged through PR #143 / `4a34585cd8df7cbfac34d17be86c5fa36b41b213`.

```text
eligible sourced claim
+ canonical v2 provenance
+ explicit server-owned transform request
→ deterministic OMIT_TRAILING_SENTENCE at most once per lineage
→ exact transformation process evidence
→ downstream BELIEF / NPC_TOLD
→ unchanged later propagation with same snapshot
```

Properties:

- only `OMIT_TRAILING_SENTENCE` exists; no open-ended rewrite or random corruption;
- hard transformation budget is 1 across the selected retained canonical lineage;
- existing max-eight-hop provenance and `npc-knowledge-transfer-v2` identity are unchanged;
- original origin statement is never rewritten;
- transformed current statement and exact transformation hop remain inspectable while direct evidence survives;
- exact retry is idempotent;
- transformed/plain same-ID conflict rejects;
- second transform returns `TRANSFORMATION_LIMIT_REACHED`;
- non-applicable single-sentence input returns `TRANSFORMATION_NOT_APPLICABLE`;
- ordinary propagation after a transform carries the same immutable snapshot without budget reset;
- direct-evidence loss becomes `UNRESOLVED / transformationsUsed=UNKNOWN` and downstream transfer fails closed rather than reconstructing provenance from prose;
- transformed knowledge remains BELIEF/NPC_TOLD with unchanged transfer confidence;
- transformation/fallibility annotation consumes no extra Semantic prompt slot;
- existing `32 / 24+8 / 6` Semantic bounds and max-four disagreement bound are unchanged;
- no provider schema/call, config, new world file, semantic schema, migration or release publication was added.

Exact-head evidence and staged RED/GREEN history are recorded in:

```text
docs/superpowers/evidence/2026-08-09-bounded-transformed-claim-tdd.md
```

### Exit criterion — met

A retained sourced rumor can undergo one explicitly bounded, deterministic and auditable wording transformation while exact original source ancestry remains inspectable, replay/restart is deterministic, privacy/pressure are safe and the transformed result remains non-authoritative BELIEF.

## Completed — bounded contradiction candidate/producer policy

Merged through PR #145 / `ebda7ecd2290ce8eab0955c2be0d8ebed3065e1c`.

```text
controlled retained Semantic FACT/BELIEF
→ same-owner + exact-scope eligibility
→ max 16 candidates
→ retained relation suppression
→ max 8 comparisons
→ deterministic standalone not/не opposition classification
→ existing SemanticContradictionLifecycle
→ truth-neutral disagreement evidence
```

Properties:

- eligibility, scope, logical-identity and normalized-equivalence filtering happen before bounded allocation;
- at most 16 candidate claims are materialized per admission;
- retained identical contradiction relations are removed before the max-eight classifier budget;
- exact replay does not create another live relation;
- the initial classifier recognizes exactly one standalone English `not` or Russian `не` insertion/removal;
- antonyms, numbers, arbitrary paraphrase, reordering, double negation and trailing-sentence omission are deliberately not inferred as contradictions in this slice;
- controlled Semantic admission persists/consolidates and rereads the retained logical claim before producer invocation;
- direct low-level Semantic store append remains storage-only;
- the existing exact-ID contradiction lifecycle remains the only persistence authority;
- current observed FACT authority, FACT/BELIEF kind, confidence, importance, provenance and source IDs remain unchanged;
- no provider call/schema, public config, new world file, persistence version/field, migration, backfill or release publication was added.

Frozen source head and delivery evidence:

```text
head:                                      b43ccaa0d0e6fdcf480ac16dc3f80e74d1182584
Repository security policy #2075:         SUCCESS / 31375662931
VillAIgence CI #2440:                     SUCCESS / 31375662912
VillAIgence Production Soak #319:         SUCCESS / 31375662909
VillAIgence GitHub Release #652:          SUCCESS / 31375662908
github-release publication:               SKIPPED
review P0/P1/P2:                          0 / 0 / 0
unresolved review threads:                0
PR discussion comments:                   0
```

Staged RED/GREEN and preservation evidence:

```text
docs/superpowers/evidence/2026-08-10-bounded-contradiction-producer-tdd.md
```

### Exit criterion — met

Ordinary controlled Semantic admission now feeds a strictly bounded automatic contradiction producer with privacy/scope-before-allocation, deterministic duplicate-safe replay and no truth arbitration.

---

# NEXT — settlement-scale information flow without omniscience

The project now has safe individual transfer and the knowledge-quality machinery needed after a claim arrives. The next step is to let information move through groups of NPCs without introducing a settlement-global shared brain.

## Goal

Create a server-owned bounded dissemination policy that selects eligible **speaker/listener transfer opportunities** and delegates every actual knowledge mutation to the existing source-backed NPC-to-NPC transfer lifecycle.

Target flow:

```text
retained source-backed knowledge
+ server-owned settlement/locality context
+ eligible speaker/listener population
→ deterministic bounded opportunity selection
→ strict work/fan-out budget
→ existing NpcKnowledgeTransferLifecycle
→ existing v2 provenance / transformation state
→ listener BELIEF/NPC_TOLD
→ existing bounded contradiction producer after admission
→ no global broadcast / no omniscient settlement memory
```

## Required design decisions

1. **Settlement/locality scope before allocation**
   - define the server-owned notion used to establish that two NPCs may exchange information;
   - speaker/listener eligibility must be resolved before any bounded opportunity slot is consumed;
   - unrelated settlements or out-of-scope players consume zero work slots.

2. **No broadcast knowledge**
   - one dissemination opportunity produces one explicit source-backed transfer;
   - there is no operation that copies a claim into all village members at once;
   - NPC knowledge remains locally persisted and inspectable.

3. **Hard work/fan-out budgets**
   - define deterministic small caps for opportunities processed in one event/cycle;
   - prevent O(N²) speaker/listener scans and cascade explosions;
   - propagation scheduling must be event-driven or coarse-grained rather than per-NPC-per-tick.

4. **Reuse existing authority path**
   - selected transfer must call `NpcKnowledgeTransferLifecycle` rather than directly append listener Semantic state;
   - existing exact source reread, v2 ancestry, eight-hop limit, transformation budget and BELIEF-only result remain authoritative.

5. **Replay/restart safety**
   - repeated evaluation of the same dissemination opportunity must not silently duplicate transfer effects;
   - restart must not reset fan-out state in a way that floods the settlement;
   - any durable scheduling identity/state must be explicitly justified before adding persistence.

6. **Privacy / subject scope**
   - player-private knowledge cannot become public merely because NPCs share a settlement;
   - existing Semantic subject scope must remain intact through propagation;
   - settlement membership itself is not permission to bypass player-scoped eligibility.

7. **No social epistemology yet**
   - relationship/trust must not change claim truth or confidence in this slice;
   - recipient selection may eventually use social topology, but trust-weighted belief confidence is the following independent design.

8. **No provider dependency for routing**
   - recipient/opportunity selection should be deterministic and server-owned;
   - do not add an LLM request merely to decide whether one eligible NPC may tell another a retained claim.

## Required TDD progression

```text
settlement/locality authority specification
→ RED: speaker/listener eligibility before allocation
→ RED: bounded deterministic opportunity selector
→ RED: hard work/fan-out budget
→ RED: cross-settlement / private-scope exclusion
→ RED: exact existing transfer lifecycle delegation
→ RED: replay/restart duplicate safety
→ RED: provenance/transform/contradiction preservation
→ deterministic multi-settlement pressure simulation
→ full exact-head delivery gates
```

### Exit criterion

Information can propagate across an eligible settlement population through explicit bounded source-backed transfers while every NPC retains local non-authoritative knowledge, existing provenance remains inspectable, privacy remains intact, work is non-quadratic and no shared omniscient settlement state exists.

---

# Later 0.2 / transition to 0.3

After settlement-scale information flow is stable:

```text
relationship/trust effects on belief confidence/fallibility as separate social epistemology
→ Personality + NPC↔NPC Social Graph
```

Relationship/trust weighting must remain separate because social affinity is not truth authority. Any confidence effect needs explicit provenance-aware rules and cannot turn repetition or trust into FACT.

---

# 0.2 exit criterion

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
- able to preserve exact bounded multi-hop rumor ancestry;
- able to represent and safely prompt live contradictions without truth arbitration;
- able to represent process fallibility without turning it into FACT;
- able to perform bounded inspectable social-information transformation;
- able to automatically discover a bounded conservative subset of contradictory claim pairs without truth arbitration;
- able to move information through an eligible settlement population without global omniscient distribution;
- independent of the removed raw conversation store.

The bounded transformed-claim criterion is met through PR #143 and the automatic bounded contradiction-producer criterion is met through PR #145. Settlement-scale bounded information flow is the current quality/completeness step before social epistemology and 0.3 convergence.

---

# 0.3 — Personality and NPC↔NPC social graph

Persistent bounded personality plus pairwise social state that affects dialogue and behavior. Personality is persistent game state, not a fresh LLM-generated profile on every conversation.

### Exit criterion

Two NPCs retain durable relationship/personality history that affects dialogue, decisions and information exchange after restart.

---

# 0.4 — Knowledge ecosystem and rumors

Expand 0.2 transfer/provenance/contradiction/fallibility/transformation and settlement-flow primitives into a richer knowledge ecosystem without omniscient distribution.

### Exit criterion

Information moves through a settlement, conflicting/fallible claims remain inspectable, and source history remains bounded.

---

# 0.5 — Autonomous NPC agents

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

The LLM may propose intent but never arbitrary Minecraft commands. Required controls include event-driven scheduling, per-NPC/global budgets, action allowlists, server target revalidation and exactly-once effects.

---

# 0.6 — Settlement simulation

Persistent population, households, professions, resources, safety, morale, shared projects and public memory that affect individual NPC behavior.

---

# 0.7 — Factions and politics

Persistent alliances, disputes, leadership, rules and inter-settlement relations with server-owned consequences.

---

# 0.8 — Emergent stories

Multi-session narratives grounded in persistent simulation history. Story is the human-readable consequence of state; the system must not generate a story first and retrofit state afterward.

---

# 0.9 — Performance, large servers and local models

Scale the living society without turning AI into a per-NPC-per-tick cost center. Work includes event-driven scheduling, model budgets, backpressure/cancellation, persistence/retrieval profiling, large-population soak and optional local models without identity migration.

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

1.0 means these systems form one coherent persistent simulation rather than a collection of AI features.

---

# Delivery and TDD governance

Runtime behavior follows:

```text
specification
→ tests-only RED
→ observe intended RED
→ minimal GREEN
→ focused regression
→ complete common/provider suite
→ Fabric GameTests + NeoForge compatibility where selected
→ production startup/restart + persistence recovery
→ security policy
→ constrained soak
→ release dry-run
→ independent base→head review
→ exact candidate / installed acceptance when required
→ root CHANGELOG.md in runtime PR
→ PROJECT_STATE / ROADMAP reconciliation after merge
```

Rules:

1. Do not write production behavior before intended RED has been observed.
2. Do not weaken assertions merely to make CI green.
3. Test fixture/policy mistakes are recorded honestly and are not misrepresented as runtime RED evidence.
4. Exact-release and installed evidence remain separate from unit/automation evidence.
5. Deferred manual evidence remains explicitly deferred.
6. Significant product/runtime/persistence/config/release/security/permanent-CI changes update root `[Unreleased]` in the same PR.
7. Release PRs move shipped `[Unreleased]` items into the exact version section rather than duplicating them.
8. Before starting new work, reconcile these documents against live GitHub state.
