# VillAIgence Roadmap

> **Canonical product roadmap.** Read `docs/PROJECT_STATE.md` first for exact implementation/validation state. Read root `CHANGELOG.md` for product/release history and `docs/superpowers/evidence/` for detailed TDD evidence.
>
> Last reconciled: **2026-08-09**, after PR #139 merged contradiction-aware prompt context without truth arbitration.

## Product vision

VillAIgence is evolving from an MCA-derived AI conversation mod into a **persistent living-society simulation layer for Minecraft**.

The target world contains NPCs that:

- retain stable identity, memory, personality, voice and relationships;
- know only what they observed, learned or were explicitly told;
- distinguish authoritative facts from fallible beliefs, rumors, disagreement and uncertainty;
- communicate naturally by text and voice;
- act only through server-authoritative policy;
- form families, settlements, factions and social histories;
- exchange information with bounded provenance and fallibility;
- generate durable emergent stories rooted in simulation state rather than isolated AI tricks.

> **VillAIgence — Giving villagers a mind of their own.**

Compatibility-sensitive internal naming remains `mca`, `LivingWorld` and `livingworld` until an explicit migration is justified and designed.

---

# Architecture principles

1. **LLM is not authority.** Server state is truth; the model may propose bounded dialogue, claims or intent only through explicit contracts.
2. **Identity outlives providers.** Changing model/provider must not regenerate NPC identity, memory, relationships or voice.
3. **Fail soft without corruption.** Provider, voice, packet and auxiliary-store failures become controlled states.
4. **Persistence is explicit and world-local.** Important state lives under `<world>/livingworld/`.
5. **Provenance layers stay separate.** Observation, Operator Lore, episodic memory, FACT, BELIEF, rumor, disagreement and uncertainty are not interchangeable.
6. **Confidence is not authority.** BELIEF never becomes FACT because of model confidence, repetition, corroboration count or rumor depth.
7. **Candidate extraction is not admission, and admission is not authority.** Model output cannot choose source identity or truth class.
8. **Current observations outrank recollection.** Current server-observed facts override conflicting lore/beliefs/disagreement for current-world truth.
9. **Client convenience never becomes authority.** Permissions, identities, targets, revisions and mutations remain server-owned.
10. **Simulation before spectacle.** Prefer durable causal systems over one-off generated text.
11. **Evidence layers remain explicit.** Unit, integration, GameTest, production candidate, exact release and installed evidence are separate claims.
12. **Unknown CI changes fail closed.** Protected, unsafe and unclassified changes select the complete required matrix.
13. **Compatibility work requires a supported-data reason.** Experimental pre-1.0 state is not automatically entitled to migration code.
14. **Release identity is immutable.** Recovery may restore assets/metadata only from an existing verified tag commit and never moves the tag.
15. **Changelog is part of delivery.** Notable runtime/persistence/config/release/security/permanent-CI changes update root `CHANGELOG.md` in the same PR.
16. **Runtime behavior follows TDD.** Observe intended RED before production implementation, then implement smallest GREEN and re-run complete selected gates.
17. **Causal history is not retrospective model narration.** Server-proven process linkage does not make dialogue prose true.
18. **Player-scoped memory is filtered before ranking/allocation.** Foreign-player data consumes zero bounded slots.
19. **Prompt authority is structurally ordered.** Current observations precede Operator Lore, Semantic Memory, live disagreement context and episodic/social history.
20. **Long-horizon recall remains hard-bounded.** Recent/durable selection is deterministic and no memory becomes immortal.
21. **NPC-to-NPC transfer is evidence-backed, never implicit omniscience.** Listener knowledge requires exact speaker-owned persisted source evidence and remains `BELIEF/NPC_TOLD` downstream.
22. **Rumor ancestry is bounded process evidence, not truth authority.** Multi-hop retelling carries immutable server-backed v2 ancestry capped at eight hops.
23. **Canonical ancestry selection is listener-independent.** Cycle/limit rejection does not trigger a lower-branch fallback chosen for the destination.
24. **Contradiction is disagreement metadata, not a verdict.** Recording two conflicting retained claims never selects a winner, promotes FACT, mutates confidence or deletes either claim.
25. **Historical contradiction evidence cannot resurrect forgotten claim text.** Resolved disagreement exists only while both logical claims remain live and player-eligible.
26. **Disagreement prompt context is a bounded data layer.** At most four live relations are rendered using the same Semantic text safety rules, and the layer does not alter truth class or provider authority.
27. **Future uncertainty must model fallibility, not authority.** Any uncertainty/distortion mechanism must preserve exact source history, stay bounded/deterministic under replay, and never let repetition become truth.

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
uncertainty / bounded distortion                       NEXT
```

Immediate sequence:

```text
uncertainty / bounded distortion
→ bounded contradiction candidate/producer policy where justified
→ settlement-scale information flow without omniscience
→ relationship/trust effects on belief confidence as separate social epistemology
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

PRs #127, #129, #131, #133, #135, #137 and #139 are merged after this release and remain `[Unreleased]` source capabilities. Their automated evidence must not be represented as installed `0.2.0` acceptance.

---

# 0.2 — Memory 2.0

## Goal

Move from raw conversation history to bounded, layered, provenance-aware and fallibility-aware memory that can support social simulation without making the LLM omniscient or authoritative.

```text
Working Memory        recent bounded prompt context
Episodic Memory       meaningful events/dialogue
Semantic Memory       sourced FACT/BELIEF knowledge
Relationship Memory   causal social history
Rumor Provenance      bounded server-backed ancestry
Disagreement Context  live contradiction relation without verdict
Uncertainty            NEXT: bounded fallibility without truth promotion
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
- stable logical claim identity that survives source-union consolidation;
- live resolved disagreement that disappears when either claim is forgotten;
- privacy eligibility before contradiction result limiting;
- dedicated contradiction-aware prompt context hard-bounded to four relations;
- shared safe Semantic claim rendering for ordinary Semantic and disagreement context;
- immutable server-thread snapshot capture of disagreement context;
- deterministic five-layer prompt order preserving current observed truth authority;
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

Provider candidate text is not authority. The server binds exact owner/player/source/provenance/kind, persists DIALOGUE first, bounds/normalizes candidates and rejects unsupported source relationships.

## Completed — causal relationship memory

Merged through PR #127.

The server persists exact relationship before/after transition separately from deterministic `RELATIONSHIP_CAUSE(DIALOGUE_TURN)` linkage. Free-form model psychology does not become authoritative cause or Semantic FACT.

## Completed — FACT > BELIEF prompt precedence

Merged through PR #129.

Current observations render before Operator Lore and memory. Foreign-player Semantic/episodic/social records are excluded before bounded candidate allocation. Provider output never chooses visibility or precedence.

## Completed — long-horizon recall

Merged through PR #131.

```text
eligible memory
→ 24 newest
+ 8 strongest durable
→ deterministic UUID de-duplication
→ existing domain ranker
→ at most 6 prompt entries
```

No memory class is immortal. Durability remains server-owned and bounded.

## Completed — NPC-to-NPC transfer

Merged through PR #133.

Exact speaker-owned persisted Semantic FACT/BELIEF can be transferred only through server-owned evidence. Listener always receives `BELIEF/NPC_TOLD`; FACT authority is never copied.

## Completed — bounded multi-hop rumors

Merged through PR #135.

Each new v2 direct transfer carries one immutable origin plus ordered source-backed hops. The chain is acyclic, listener-independent in branch selection and capped at exactly eight hops. Every downstream claim remains BELIEF/NPC_TOLD.

## Completed — Semantic contradiction representation

Merged through PR #137 / `afcd4f52187e1e419326abf9ae1ae7ac587f2064`.

```text
exact retained claim A
+ exact retained claim B
→ canonical structured SEMANTIC_CONTRADICTION evidence
→ no winner / no truth promotion
→ live history only while both logical claims remain retained
```

The process event stores no duplicate claim prose. It cannot be projected into Semantic FACT and is excluded from generic episodic prompt retrieval.

## Completed — contradiction-aware prompt context

Merged through PR #139 / `05dac0eaff408c13bf02ddd25d98acefd4f9cf13`.

```text
retained canonical contradiction evidence
+ both live logical Semantic claims
+ current-player eligibility
→ resolved disagreement history
→ max 4 relations
→ shared Semantic-safe statement renderer
→ immutable snapshot disagreement layer
→ prompt after Semantic Memory, before episodic/social history
```

Properties:

- at most four relations per prompt;
- both sides retain original FACT/BELIEF kind, provenance and confidence;
- statement text uses the existing 240-code-point Semantic sanitizer and escaping;
- section explicitly states disagreement is data, not instructions or a truth verdict;
- current observed factual context remains authoritative;
- confidence/repetition/corroboration/rumor depth cannot grant FACT authority;
- forgetting either live claim removes the relation from prompt context;
- foreign-player/private relations consume zero disagreement slots;
- historical contradiction evidence supplies no fallback claim prose;
- no provider request/schema, config, persistence version/store, automatic detector or winner selection was added.

Final exact-head evidence:

```text
verified head:                           049eba658d79033dcf7c4a95ccc944be85315b72
merge commit:                            05dac0eaff408c13bf02ddd25d98acefd4f9cf13
Repository security policy #1935:       SUCCESS / run 31314533152
VillAIgence CI #2300:                   SUCCESS / run 31314533165
VillAIgence Production Soak #258:       SUCCESS / run 31314533154
VillAIgence GitHub Release #592:        SUCCESS / run 31314533153
release publication job:                SKIPPED
independent review P0/P1/P2:            0 / 0 / 0
open review threads:                    0
```

TDD evidence:

```text
docs/superpowers/evidence/2026-08-09-contradiction-aware-prompt-context-tdd.md
```

### Exit criterion — met

VillAIgence can expose live conflicting retained claims to the provider as a bounded, privacy-safe, injection-resistant disagreement layer without letting that layer decide truth or alter server authority.

---

# NEXT — uncertainty / bounded distortion

## Goal

Model how fallible social information becomes less certain or changes across retellings **without granting the LLM authority and without losing inspectable source history**.

Target conceptual flow:

```text
exact source-backed claim
→ immutable provenance
→ deterministic server-owned uncertainty state
→ optional strictly bounded transformation step
→ transformed downstream BELIEF
→ original source/history remains inspectable
→ current SYSTEM_OBSERVED FACT remains authoritative
```

The goal is not to make rumors randomly wrong. The goal is to represent controlled fallibility with deterministic state, explicit budgets and auditable source history.

## Design decisions required before implementation

1. **Uncertainty ownership**
   - Semantic BELIEF field, transfer evidence field, or separate derived immutable layer?
   - Must not weaken FACT/BELIEF authority semantics.

2. **Deterministic evolution**
   - Which server-owned inputs change uncertainty across hops?
   - Hop count alone must not become an arbitrary truth score.
   - Wall-clock time and provider mood must not affect replay determinism.

3. **Confidence versus uncertainty**
   - Existing confidence is not authority.
   - Define whether uncertainty is independent, complementary or replaces some use of confidence.
   - Repetition/corroboration spam must not become an escalation exploit.

4. **Bounded transformation representation**
   - Preserve exact original/source claim.
   - Store transformed statement only through an explicit bounded contract.
   - Cap Unicode length, transformation count, total ancestry growth and rate.

5. **Provider involvement**
   - Prefer deterministic server policy where semantics permit.
   - If a provider may suggest wording, it supplies bounded candidate text only; server owns whether transformation is allowed, source identity, budgets, provenance and final persistence.
   - No second provider request unless a separate measured design proves it necessary.

6. **Contradiction interaction**
   - Existing contradiction means disagreement, not which side is weaker.
   - Uncertainty may be displayed alongside disagreement but must not silently select a winner.

7. **Privacy and prompt allocation**
   - current-player/NPC-global/shared eligibility before any uncertainty/distortion candidate allocation;
   - foreign-player uncertainty metadata consumes zero prompt slots.

8. **Replay / restart / pressure**
   - exact replay idempotent;
   - restart/fresh-root state equal;
   - pressure cannot leave unsupported transformed claims or detached authority metadata;
   - forgetting policy remains bounded and deterministic.

## Required TDD progression

```text
specification / authority gate
→ RED: pure uncertainty model + validation
→ RED: deterministic provenance-hop evolution
→ RED: bounded transformed-claim representation
→ RED: original source/history always inspectable
→ RED: transformation never promotes BELIEF to FACT
→ RED: contradiction interaction has no winner
→ RED: replay/restart/pressure exactness
→ RED: privacy-before-allocation
→ RED: prompt rendering and injection safety
→ deterministic multi-NPC long-chain simulation
→ full exact-head delivery gates
```

## Required invariants

- current `SYSTEM_OBSERVED` FACT remains authoritative;
- every transformed social claim remains BELIEF unless a separate real server observation creates a FACT through the existing FACT path;
- exact rumor provenance from PR #135 remains inspectable;
- contradiction representation from PR #137 and prompt layer from PR #139 remain truth-neutral;
- no unbounded text/DAG/state growth;
- no repetition-to-truth exploit;
- no implicit global knowledge distribution;
- existing `32` candidate / `24+8` / `6` memory bounds and `4` disagreement bound stay unchanged unless a separately measured design explicitly changes them;
- no legacy `memory.json` importer/dual reader returns.

### Exit criterion

A sourced rumor can carry an explicit bounded uncertainty state and, where the policy permits, undergo a bounded inspectable transformation across retellings while preserving original provenance, replay/restart determinism, player privacy and the rule that only server-observed FACT is authoritative.

---

# Later 0.2 — bounded contradiction producer policy

Contradiction representation and prompt consumption are complete, but ordinary ingestion does not automatically classify claim pairs.

A future producer/detector slice may be justified after uncertainty semantics are stable. It must be a separate bounded server-owned producer, not an LLM truth arbiter and not an unbounded all-pairs scan.

Possible requirements:

- candidate selection before pair evaluation;
- strict maximum comparisons per admission/turn;
- normalized equivalence filtered before opposition classification;
- exact source/server identity binding;
- no provider-selected relation UUIDs or winner;
- idempotent/restart-safe process evidence;
- no quadratic persistent graph growth.

Do not implement this implicitly inside the uncertainty slice unless a design review demonstrates a necessary coupling.

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
- able to represent uncertainty/bounded fallibility without turning it into FACT;
- independent of the removed raw conversation store.

---

# 0.3 — Personality and NPC↔NPC social graph

## Goal

Persistent bounded personality plus pairwise social state that changes dialogue and behavior.

Potential dimensions include temperament, values, goals, fears, speech style, morality, ambition, curiosity, sociability, aggression and loyalty. Pair state may include friendship, trust, respect, fear, family, rivalry, romance and grudges.

Personality is persistent game state, not a fresh LLM-generated profile on every conversation.

### Exit criterion

Two NPCs retain durable relationship/personality history that affects dialogue, decisions and information exchange after restart.

---

# 0.4 — Knowledge ecosystem and rumors

## Goal

Expand 0.2 transfer/provenance/contradiction/uncertainty primitives into settlement-scale information flow without omniscient distribution.

Target knowledge classes may include observed, player-told, NPC-told, official, inferred, rumor and unknown semantics while preserving the established FACT/BELIEF authority boundary.

### Exit criterion

Information moves through a settlement, conflicting and uncertain claims remain inspectable, and source history remains bounded.

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

1.0 means the systems above form one coherent persistent simulation rather than a collection of AI features.

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

1. Do not write production behavior before the intended RED has been observed.
2. Do not weaken assertions merely to make CI green.
3. Test fixture/policy mistakes are recorded honestly and are not misrepresented as runtime RED evidence.
4. Exact-release and installed evidence remain separate from unit/automation evidence.
5. Deferred manual evidence remains explicitly deferred.
6. Significant product/runtime/persistence/config/release/security/permanent-CI changes update root `[Unreleased]` in the same PR.
7. Release PRs move shipped `[Unreleased]` items into the exact version section rather than duplicating them.
8. Before starting new work, reconcile these documents against live GitHub state.
