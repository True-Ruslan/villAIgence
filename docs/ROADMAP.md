# VillAIgence Roadmap

> **Canonical product roadmap.** Read `docs/PROJECT_STATE.md` first for exact implementation/validation state. Read root `CHANGELOG.md` for product/release history and `docs/superpowers/evidence/` for staged TDD evidence.
>
> Last reconciled: **2026-08-11**, after PR #153 merged the server-owned causal NPC↔NPC social mutation lifecycle.

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
3. **Fail soft without corruption.** Provider, voice, packet and auxiliary-store failures become controlled states; replay-authority corruption fails closed.
4. **Persistence is explicit and world-local.** Important state lives under `<world>/livingworld/`.
5. **Provenance layers stay separate.** Observation, Operator Lore, episodic memory, FACT, BELIEF, rumor, disagreement, fallibility, transformation and social-process evidence are not interchangeable.
6. **Confidence is not authority.** BELIEF never becomes FACT because of model confidence, repetition, corroboration, rumor depth, source distance, transformations or social values.
7. **Candidate extraction is not admission, and admission is not authority.** Model output cannot choose source identity or truth class.
8. **Current observations outrank recollection.** Current server-observed facts override conflicting lore, beliefs, transformed claims, disagreement and fallibility.
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
19. **Prompt authority is structurally ordered.** Current observations precede lore, memory, disagreement and lower-authority social/history context.
20. **Long-horizon recall remains hard-bounded.** Recent/durable selection is deterministic and no memory becomes immortal.
21. **NPC-to-NPC transfer is evidence-backed, never implicit omniscience.** Listener knowledge requires exact persisted speaker evidence and remains `BELIEF/NPC_TOLD`.
22. **Rumor ancestry is bounded process evidence, not truth authority.** Multi-hop retelling carries immutable server-backed ancestry capped at eight hops.
23. **Canonical ancestry selection is listener-independent.** Cycle/limit rejection does not trigger a convenient fallback branch.
24. **Contradiction is disagreement metadata, not a verdict.** It never promotes FACT, changes confidence or deletes a claim.
25. **Historical contradiction evidence cannot resurrect forgotten claim text.**
26. **Disagreement prompt context is a bounded data layer.** At most four live relations are rendered.
27. **Fallibility models process history, not truth likelihood.**
28. **Missing provenance is explicit.** Lost direct provenance becomes `UNRESOLVED`; history is not reconstructed from prose.
29. **Transformation is a separate authority boundary.** Current wording distortion is server-deterministic, hard-bounded and preserves source provenance.
30. **Automatic contradiction production is bounded before classification.** Eligibility and duplicate suppression precede comparison work.
31. **Settlement information flow is transfer, not shared omniscience.** No settlement-global knowledge state exists.
32. **Social trust is a personal derived BELIEF view, not truth authority.** NPC×player trust cannot change persisted confidence or ranking.
33. **NPC×player and NPC↔NPC relationships are separate social authority domains.**
34. **MCA Personality is already persistent server-owned personality state.** 0.3 derives snapshots from it rather than creating a competing profile store.
35. **NPC social changes require causal server evidence.** Runtime validates exact NPC identities and exact retained causes before changing a directed pair.
36. **The NPC social graph owns exactly-once mutation replay.** Memory audit is bounded process evidence, not the mutation ledger.
37. **Malformed attributable graph frontier state fails closed locally.** One bad cursor must not erase valid graph state or reopen a historical mutation.
38. **Read-only social/personality context precedes social behavior policy.** First expose a bounded direct-pair snapshot; only then let dialogue/behavior consume it.

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
settlement-scale information flow without omniscience  COMPLETE / PR #147
relationship/trust social epistemology                 COMPLETE / PR #149
0.2 Memory 2.0 source capability track                 COMPLETE AT CURRENT PLANNED BOUNDARY / UNRELEASED

0.3 Personality + NPC↔NPC Social Graph                 IN PROGRESS
NPC↔NPC social-graph persistence foundation            COMPLETE / PR #151
server-owned causal NPC↔NPC social mutation lifecycle COMPLETE / PR #153
bounded read-only MCA Personality + pair snapshot      NEXT
```

Immediate sequence:

```text
bounded read-only MCA Personality + direct NPC-pair social snapshot
→ deliberate dialogue/behavior integration
→ 0.3 convergence / release-candidate planning
→ richer knowledge ecosystem / 0.4
```

`VAI-CONCUR-004` remains `NOT TESTED / DEFERRED` until two real graphical clients are available. It does not block server-side product development because concurrency semantics are automated.

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

PRs #127 through #153 listed in `PROJECT_STATE.md` are post-release `[Unreleased]` source capabilities. Their automated evidence must not be represented as installed `0.2.0` acceptance.

---

# 0.2 — Memory 2.0

## Goal

Bounded, layered, provenance-aware and fallibility-aware memory that supports social simulation without making the LLM omniscient or authoritative.

Current completed source-capability set:

```text
Working Memory          recent bounded prompt context
Episodic Memory         meaningful events/dialogue
Semantic Memory         sourced FACT/BELIEF knowledge
Relationship Memory     causal NPC×player social history
Rumor Provenance        bounded server-backed ancestry
Disagreement Context    live contradiction relation without verdict
Rumor Fallibility       exact source distance / transform count / unresolved state
Bounded Distortion      one inspectable deterministic omission primitive
Contradiction Producer  bounded automatic candidate production without truth arbitration
Settlement Flow         bounded home-village propagation without shared omniscience
Social Epistemology     bounded NPC×player trust treatment without truth promotion
```

0.2 source-capability exit criteria are met through PR #149. This does **not** upgrade installed `0.2.0+1.21.1`; post-release source capabilities remain `[Unreleased]` until a later exact release candidate and explicit installed acceptance.

---

# 0.3 — Personality and NPC↔NPC social graph

## Goal

Persistent MCA-owned personality plus directed NPC↔NPC social state that can affect dialogue, decisions and later information exchange while remaining server-owned, bounded, restart-safe and separate from truth authority.

Current state:

```text
MCA Personality persistence authority            AVAILABLE / EXISTING MCA STATE
NPC↔NPC directed graph persistence               COMPLETE / PR #151
six-store corruption/recovery                    COMPLETE / PR #151
causal NPC↔NPC mutation lifecycle                COMPLETE / PR #153
bounded read-only personality/social snapshot    NEXT
dialogue/behavior effect                         PLANNED AFTER SNAPSHOT
high-frequency autonomous social evolution       NOT IMPLEMENTED
```

## Completed — NPC↔NPC social-graph persistence foundation

Merged through PR #151 / `093be3892a35ad07e074503be58e320356b080e2`.

```text
existing MCA Personality tracked state
+ exact directed source NPC UUID → target NPC UUID pair
→ bounded NpcSocialState(trust,respect,fear,affinity)
→ dedicated npc-social-graph.json format v1
→ six-store production/recovery automation
```

Core graph properties:

- MCA Personality remains the canonical persistent personality authority;
- A→B and B→A are independent;
- social dimensions clamp to `[-100,+100]`;
- null/self pairs fail closed;
- neutral states compact away;
- each source retains at most 64 non-neutral outgoing edges;
- overflow rejects new edges without eviction;
- duplicate/over-capacity/corrupt source state fails closed;
- no provider call/config/Semantic authority/dialogue integration was added.

Exact delivery evidence is preserved in `docs/PROJECT_STATE.md`, root `CHANGELOG.md` and `docs/superpowers/evidence/2026-08-10-npc-social-graph-foundation-tdd.md`.

## Completed — server-owned causal NPC↔NPC social mutation lifecycle

Merged through PR #153 / `2a75e950e4e7f43f1321fc572c260b00f6d2bdf4`.

Target boundary now implemented:

```text
exact retained source-owned SYSTEM_OBSERVED event
+ validated live source NPC
+ validated live target NPC
+ bounded directed NpcSocialDelta
→ deterministic source cause identity/order
→ atomic graph edge + latest source frontier persistence
→ optional bounded structured NPC_SOCIAL_CHANGE audit
→ exact replay without duplicate effect
```

Properties:

- exact retained cause must be `SYSTEM_OBSERVED` `OBSERVATION|ACTION` and include the target;
- live server identity authority proves both UUIDs are MCA villagers;
- mutation ID binds source NPC + cause event;
- same cause with conflicting target/delta fails closed;
- cause ordering is `gameTime` then event UUID;
- `APPLIED`, `NO_CHANGE` and `CAPACITY_REACHED` all consume frontier order;
- exact replay returns `REPLAYED` and never applies another delta;
- older cause returns `STALE_CAUSE`;
- graph state + frontier are atomic; bounded Memory audit is not the replay ledger;
- crash after graph commit but before audit append cannot duplicate state and replay does not backfill history;
- source/audit forgetting never rolls back graph state;
- `NPC_SOCIAL_CHANGE` is excluded from generic prompt context and never becomes Semantic authority;
- malformed attributable frontier state fails closed per source;
- malformed cursor decode cannot reset an otherwise-valid graph file;
- old v1 files without a frontier remain compatible;
- no provider call/schema, public config, new world store, format bump, migration or release publication was added.

Frozen source head and delivery evidence:

```text
head:                                      ad75a7e51dfe13a43631d4de29848c8f7656d330
merge:                                     2a75e950e4e7f43f1321fc572c260b00f6d2bdf4
Repository security policy #2303:         SUCCESS / 31469172227
VillAIgence CI #2668:                     SUCCESS / 31469172371
VillAIgence Production Soak #419:         SUCCESS / 31469172211
VillAIgence GitHub Release #752:          SUCCESS / 31469172244
github-release publication:               SKIPPED
review P0/P1/P2:                          0 / 0 / 0
unresolved review threads:                0
PR discussion comments:                   0
```

Staged RED/GREEN, preservation and persistence hardening evidence:

```text
docs/superpowers/evidence/2026-08-11-causal-npc-social-mutation-tdd.md
```

Review hardening found two real persistence defects tests-first: malformed-key source attribution and whole-file recovery caused by a malformed required cursor field. Both have permanent regression coverage and were reverified on the frozen exact head.

### Causal lifecycle exit criterion — met

A retained authoritative server event can cause exactly one bounded directed NPC social mutation; exact retry/restart cannot duplicate it, process evidence is auditable while retained, source evidence loss is handled honestly, and social state never becomes FACT/Semantic authority.

---

# NEXT — bounded read-only MCA Personality + direct NPC-pair social snapshot / 0.3

The next step is **observation before behavior**: expose only the already-authoritative state needed for one interaction, without allowing prompt construction to mutate personality or relationships.

## Goal

Build one immutable server-owned context object from the current MCA NPC entity plus, when relevant, exactly one directed NPC→NPC social edge. The snapshot can then be safely consumed by dialogue/behavior code in a later slice.

Target contract:

```text
current MCA NPC
+ existing MCA Personality tracked/NBT state
+ optional interaction counterpart NPC
+ exact direct source→target NpcSocialState
→ bounded immutable PersonalitySocialSnapshot
→ deterministic safe server-authored rendering
→ no persistence mutation
```

## Required design decisions

1. **Personality authority**
   - read existing MCA Personality only;
   - do not generate or persist a second personality profile;
   - provider text cannot rewrite personality through snapshot construction.

2. **Direct-pair social retrieval**
   - retrieve only source→current-counterpart state;
   - do not enumerate neighbors or expose the whole graph;
   - A→B and B→A remain distinct;
   - missing edge is neutral/absent context and must not create a stored edge.

3. **Snapshot bounds**
   - fixed-size fields only;
   - no unbounded list/map/string growth;
   - capture synchronously on the server thread before asynchronous provider use.

4. **Rendering and authority order**
   - server-authored labels only;
   - sanitize any personality display text with existing context-safety rules;
   - snapshot cannot override current observed world facts;
   - place it explicitly in prompt composition rather than appending ad-hoc text.

5. **Read-only guarantee**
   - snapshot construction must not mutate `npc-social-graph.json`, `relationships.json`, Semantic Memory, Memory 2.0, Personality NBT/tracked state or config;
   - no provider call is needed to construct it.

6. **Compatibility**
   - no new persistence file/version/config unless a concrete product need is proven;
   - loaders and production startup/restart remain unchanged when snapshot has no consumer.

## Required TDD progression

```text
snapshot specification + exact field contract
→ RED: extract canonical MCA Personality from live server NPC
→ RED: direct source→target social lookup only
→ RED: A→B differs from B→A
→ RED: missing pair produces neutral/empty snapshot and zero persistence writes
→ RED: immutable bounded snapshot survives async handoff
→ RED: deterministic safe rendering / reserved-template injection resistance
→ RED: prompt placement preserves current FACT precedence
→ RED: snapshot construction mutates no graph/relationships/Semantic/Memory/Personality state
→ GameTest with two live MCA NPCs
→ production startup/restart compatibility
→ exact-head CI / soak / release dry-run / review
```

### Slice exit criterion

For one server interaction, VillAIgence can capture and safely render the NPC's canonical existing personality plus only the directly relevant directed social edge, with strict bounds, no persistence side effects, no graph-wide disclosure and no truth-authority leakage.

After this slice, implement **deliberate dialogue/behavior integration**: use the snapshot to influence tone and selected server-owned social policies while keeping gameplay mutations revalidated and causal.

---

# 0.4 — Knowledge ecosystem and rumors

Expand 0.2 transfer/provenance/contradiction/fallibility/transformation, settlement-flow and social-epistemology primitives into a richer knowledge ecosystem without omniscient distribution.

### Exit criterion

Information moves through settlements, conflicting/fallible claims remain inspectable, source history remains bounded, and social context affects propagation without becoming truth authority.

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
