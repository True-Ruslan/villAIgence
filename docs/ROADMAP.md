# VillAIgence Roadmap

> **Canonical product roadmap.** Read `docs/PROJECT_STATE.md` first for exact implementation/validation state. Read root `CHANGELOG.md` for product/release history and `docs/superpowers/evidence/` for detailed TDD evidence.
>
> Last reconciled: **2026-08-09**, after PR #137 merged deterministic Semantic contradiction representation without truth promotion.

## Product vision

VillAIgence is evolving from an MCA-derived AI conversation mod into a **persistent living-society simulation layer for Minecraft**.

The target world contains NPCs that:

- retain stable identity, memory, personality, voice and relationships;
- know only what they observed, learned or were explicitly told;
- distinguish authoritative facts from fallible beliefs, rumors and disagreement;
- communicate naturally by text and voice;
- act only through server-authoritative policy;
- form families, settlements, factions and social histories;
- exchange information with bounded provenance and uncertainty;
- generate durable emergent stories rooted in simulation state rather than isolated AI tricks.

> **VillAIgence — Giving villagers a mind of their own.**

Compatibility-sensitive internal naming remains `mca`, `LivingWorld` and `livingworld` until an explicit migration is justified and designed.

---

# Architecture principles

1. **LLM is not authority.** Server state is truth; the model may propose bounded dialogue/claims/intent only through explicit contracts.
2. **Identity outlives providers.** Changing model/provider must not regenerate NPC identity, memory, relationships or voice.
3. **Fail soft without corruption.** Provider, voice, packet and auxiliary-store failures become controlled states.
4. **Persistence is explicit and world-local.** Important state lives under `<world>/livingworld/`.
5. **Provenance layers stay separate.** Observation, Operator Lore, episodic memory, FACT, BELIEF, rumor and contradiction metadata are not interchangeable.
6. **Confidence is not authority.** BELIEF never becomes FACT because of model confidence, repetition or corroboration count.
7. **Candidate extraction is not admission, and admission is not authority.** Model output cannot choose source identity or truth class.
8. **Current observations outrank recollection.** Current server-observed facts override conflicting lore/beliefs for current-world truth.
9. **Client convenience never becomes authority.** Permissions, identities, targets, revisions and mutations remain server-owned.
10. **Simulation before spectacle.** Prefer durable causal systems over one-off generated text.
11. **Evidence layers remain explicit.** Unit, integration, GameTest, production candidate, exact release and installed evidence are separate claims.
12. **Unknown CI changes fail closed.** Protected, unsafe and unclassified changes select the complete required matrix.
13. **Compatibility work requires a supported-data reason.** Experimental pre-1.0 state is not automatically entitled to migration code.
14. **Release identity is immutable.** Recovery may restore assets/metadata only from an existing verified tag commit and never moves the tag.
15. **Changelog is part of delivery.** Notable runtime/persistence/config/release/security/permanent-CI changes update root `CHANGELOG.md` in the same PR.
16. **Runtime behavior follows TDD.** Observe intended RED before production implementation, then implement smallest GREEN and re-run complete selected gates.
17. **Causal history is not retrospective model narration.** Server-proven process linkage does not make dialogue prose true.
18. **Player-scoped prompt memory is filtered before ranking/allocation.** Foreign-player memory consumes zero bounded slots.
19. **Prompt authority is structurally ordered.** Current observations precede Operator Lore, Semantic Memory and episodic/social history.
20. **Long-horizon recall remains hard-bounded.** Recent/durable selection is deterministic and no memory becomes immortal.
21. **NPC-to-NPC transfer is evidence-backed, never implicit omniscience.** Listener knowledge requires exact speaker-owned persisted source evidence and remains `BELIEF/NPC_TOLD` downstream.
22. **Rumor ancestry is bounded process evidence, not truth authority.** Multi-hop retelling carries immutable server-backed v2 ancestry capped at eight hops.
23. **Canonical ancestry selection is listener-independent.** Cycle/limit rejection does not trigger a lower-branch fallback chosen for the destination.
24. **Contradiction is disagreement metadata, not a verdict.** Recording two conflicting retained claims never selects a winner, promotes FACT, mutates confidence or deletes either claim.
25. **Historical contradiction evidence cannot resurrect forgotten claim text.** Resolved disagreement exists only while both logical claims remain live and player-eligible.

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
contradiction-aware prompt context                     NEXT
```

Immediate sequence:

```text
contradiction-aware prompt context without truth arbitration
→ uncertainty / bounded distortion
→ bounded contradiction candidate/producer policy where justified
→ settlement-scale information flow without omniscience
→ relationship/trust effects on belief confidence as separate social epistemology
```

`VAI-CONCUR-004` remains `NOT TESTED / DEFERRED` until two real graphical clients are available. It does not block current product development because server-side concurrency semantics are automated.

---

# Completed platform — 0.1.x and M11

The 0.1 line established the reliability/security platform used by later simulation work:

- provider parsing/transport hardening;
- bounded retries/deadlines/backpressure and exactly-once effects;
- endpoint/credential/redirect policy;
- deterministic text/voice/Operator Lore acceptance;
- world-local persistence recovery;
- selective MCA gameplay/navigation corrections;
- exact production startup/restart and package identity;
- risk-based Fabric GameTests;
- Fabric + NeoForge compatibility;
- constrained-heap soak;
- immutable release artifact verification;
- version-aware recovery of incomplete GitHub Release publication.

Acceptance catalog:

```text
34 total scenarios
28 AUTOMATED
6 MANUAL_CANARY
0 PLANNED
```

The remaining manual scenarios require installed graphical clients, physical microphone/UDP paths or subjective audible/spatial judgment rather than missing deterministic unit coverage.

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

The release intentionally removed the experimental raw `memory.json` conversation store. The accepted pre-1.0 rollout boundary is clean LivingWorld state; no legacy conversation importer/dual reader is planned.

PRs #127, #129, #131, #133, #135 and #137 are merged after this release and remain `[Unreleased]`; their automated acceptance must not be represented as installed `0.2.0` evidence.

---

# 0.2 — Memory 2.0

## Goal

Move from raw conversation history to bounded, layered, provenance-aware memory that supports social simulation without making the LLM omniscient or authoritative.

```text
Working Memory          recent bounded dialogue context
Episodic Memory         meaningful events and dialogue
Semantic Memory         sourced FACT/BELIEF knowledge
Relationship Memory     source-backed causal social history
Rumor Provenance        bounded server-backed social ancestry
Contradiction Evidence  bounded disagreement metadata between live claims
```

## Foundation now implemented

- immutable episodic MemoryEvents;
- structured DIALOGUE / relationship transition / causal payloads;
- exact NPC/player isolation;
- bounded Working Memory;
- typed FACT/BELIEF Semantic Memory;
- controlled server-observed FACT ingestion;
- deterministic Semantic consolidation/source union;
- bounded deterministic forgetting;
- long-horizon recent/durable retrieval;
- current-player/NPC-global/shared eligibility before candidate allocation;
- exact relationship-cause source linkage;
- exact NPC-to-NPC source-backed transfer;
- bounded immutable 8-hop rumor ancestry;
- listener-independent canonical rumor branch selection;
- stable logical Semantic claim identity across source-union consolidation;
- structured deterministic Semantic contradiction process evidence;
- live contradiction resolution without forgotten-text resurrection;
- current observed FACT authority preserved across all memory layers.

---

## Completed — persistent-dialogue clean cutover

Released in `0.2.0+1.21.1`.

```text
successful text/voice turn
→ structured DIALOGUE MemoryEvent
→ memory2.json
→ exact NPC/player retrieval before limit
→ chronological bounded Working Memory
```

No legacy `memory.json` importer, migration checkpoint, dual reader or summary parsing is part of current architecture.

### Exit criterion — met

Text and voice dialogue survive restart as structured Memory 2.0 data and reconstruct bounded Working Memory without the removed raw conversation store.

---

## Completed — controlled BELIEF admission — PR #123

```text
SYSTEM_OBSERVED → FACT path only
PLAYER_TOLD     → BELIEF only
NPC_TOLD        → BELIEF only
INFERRED        → BELIEF only
```

Source identity comes from persisted evidence. Replay is idempotent; equivalent sourced claims use deterministic consolidation.

### Exit criterion — met

Non-authoritative claims can be admitted only through exact source contracts and cannot cross the FACT authority boundary.

---

## Completed — bounded PLAYER_TOLD extraction — PR #125

The existing structured OpenAI/OpenRouter reply may carry bounded statement candidates. The server binds NPC/player/source/provenance/kind, persists DIALOGUE first, normalizes/deduplicates/bounds claims and never creates FACT through this path.

### Exit criterion — met

A bounded player-told claim can be learned with exact source attribution and retry/restart safety without a second provider call.

---

## Completed — causal relationship memory — PR #127

`RELATIONSHIP_CHANGE` stores exact server-applied before/after state. `RELATIONSHIP_CAUSE(DIALOGUE_TURN)` links the transition to exact persisted same-turn DIALOGUE evidence. Generated retrospective explanation is never authoritative cause.

### Exit criterion — met

Relationship history can retain source-backed process causality without turning conversation prose into server truth.

---

## Completed — FACT > BELIEF prompt precedence — PR #129

Prompt authority:

```text
current observed world facts
→ Operator Lore
→ Semantic Memory
→ episodic/social history
→ structured-response/tool instructions
```

Foreign-player memory is excluded before bounded allocation. Shared current-player/NPC-global records remain eligible.

### Exit criterion — met

Current server-observed truth structurally controls prompt framing and provider output does not decide memory visibility/authority.

---

## Completed — long-horizon recall — PR #131

Normal candidate allocation:

```text
24 newest eligible
+ 8 strongest durable eligible
→ deterministic de-duplication
→ existing ranker
→ at most 6 prompt records
```

Retention uses server-owned persisted attributes plus authoritative Minecraft game time. No class is immortal.

### Exit criterion — met

Important memory can survive realistic multi-session pressure/restart while weak memory decays predictably and privacy stays exact.

---

## Completed — NPC-to-NPC knowledge transfer — PR #133

```text
exact speaker-owned Semantic source
→ authoritative reread
→ listener-owned NPC_TOLD evidence
→ exact reread/validation
→ listener BELIEF/NPC_TOLD
```

The caller cannot inject claim text/truth class/provenance/scope/source IDs. FACT authority is never copied to the listener. Transfer is deterministic, bounded and retry-safe.

### Exit criterion — met

One NPC can explicitly transmit sourced knowledge to another without global distribution or truth promotion.

---

## Completed — bounded multi-hop rumor provenance — PR #135

Every new v2 direct transfer may carry immutable ancestry:

```text
Origin
  origin NPC / Semantic entry / kind / provenance / statement / scope

Hop[]
  speaker / listener / speaker Semantic entry / evidence UUID / gameTime
```

Properties:

- first-hop origins are restricted to accepted FACT/BELIEF provenance combinations;
- `NPC_TOLD` cannot reset origin;
- downstream claims remain BELIEF/NPC_TOLD;
- lineage max = 8 hops;
- cycles rejected;
- canonical branch = `gameTime DESC → evidence UUID ASC`;
- resolver has no listener input;
- no listener-dependent fallback;
- Semantic source IDs remain direct-only;
- older physical hop eviction does not mutate later immutable ancestry snapshot;
- loss of current direct evidence blocks further propagation;
- privacy/restart/pressure remain deterministic.

Exact-head product evidence:

```text
verified head:                           d2d487d980c7ffe9819e3250489519005fd6767c
merge commit:                            f1fdee1fa1cd0b3a04a2f33357d50d7ae4c1a6d7
security / CI / soak / release dry-run:  SUCCESS / SUCCESS / SUCCESS / SUCCESS
release publication:                     SKIPPED
independent review P0/P1/P2:             0 / 0 / 0
```

### Exit criterion — met

A claim can traverse multiple NPCs with bounded inspectable server-backed ancestry and remain explicitly non-authoritative.

---

## Completed — deterministic Semantic contradiction representation — PR #137

Merged through PR #137 / `afcd4f52187e1e419326abf9ae1ae7ac587f2064`.

This slice implements disagreement **representation/lifecycle/query**, not automatic natural-language detection and not prompt truth arbitration.

### Stable logical claim identity

`SemanticMemoryIdentity` reuses existing consolidation semantics:

```text
owner
+ kind
+ provenance
+ canonical NFKC/lowercase/whitespace statement
+ canonical sorted unique semantic subject scope
```

Source-event IDs do not affect the logical identity, so a claim survives deterministic source-union consolidation. Existing consolidated Semantic entry IDs remain byte-compatible.

### Structured process evidence

A new `SEMANTIC_CONTRADICTION / SYSTEM_OBSERVED` MemoryEvent may contain two canonical `ClaimSnapshot`s:

```text
logicalClaimId
exact detectedSemanticEntryId
kind
provenance
canonical relatedEntities
```

The contradiction event intentionally stores **no copy of source claim prose**.

Deterministic `semantic-contradiction-v1` identity binds owner + both full canonical ordered snapshots + authoritative game time. Reversing A/B therefore preserves identity, while snapshot mutation fails integrity validation.

`SYSTEM_OBSERVED` applies only to the fact that the server recorded a relation between two retained claims. It does **not** make either claim true.

### Exact lifecycle

```text
exact A/B Semantic IDs
→ owner-scoped exact reads + authoritative rereads
→ distinct logical claims / same canonical scope
→ canonical contradiction event
→ append + exact reread + integrity validation
→ RECORDED / explicit controlled status
```

Statuses include:

```text
RECORDED
REJECTED
SOURCE_NOT_RETAINED
SCOPE_MISMATCH
SAME_CLAIM
EVENT_NOT_RETAINED
```

Exact replay is byte-idempotent. Later detection time produces distinct bounded evidence. Event pressure never mutates either Semantic source claim.

### Live history and forgetting

`SemanticContradictionHistory`:

- resolves current claims by stable logical identity;
- survives source-union consolidation replacing a concrete entry ID;
- requires kind/provenance/scope consistency;
- filters current-player/NPC-global/shared eligibility before limiting;
- ignores malformed contradiction evidence fail-closed;
- stops resolving if either logical claim is forgotten;
- never parses summary or restores source text from historical contradiction evidence.

### Prompt isolation

`SEMANTIC_CONTRADICTION` is deliberately excluded from generic episodic prompt retrieval. Otherwise its `SYSTEM_OBSERVED` process provenance could be mislabeled as a generic verified factual memory.

This is why **dedicated contradiction-aware prompt context is the next slice** rather than simply allowing the new event into existing Memory 2.0 formatting.

### TDD/review evidence

Observed separate tests-only RED gates for:

```text
stable Semantic logical identity
structured contradiction event/model
canonical adapter/integrity policy
exact-ID lifecycle/result statuses
live resolved contradiction history
```

Preservation coverage then exercised fresh-root restart, source-union consolidation, forgetting, malformed persisted evidence, privacy-before-limit, exact replay, bounded event rejection, no duplicate claim prose, 240 Semantic + 240 episodic pressure records, forward/reverse deterministic snapshots, unchanged FACT/BELIEF authority and existing eight-hop rumor regressions.

A suspected corruption NPE investigated during independent review was disproved by a corrected real production persistence-path test; no production fix was needed.

Final exact-head evidence:

```text
verified head:                           c20354e2cfa34b01cbcb8ea9da0b7edd68cadc1f
merge commit:                            afcd4f52187e1e419326abf9ae1ae7ac587f2064
Repository security policy #1893:       SUCCESS / run 31311225992
VillAIgence CI #2258:                   SUCCESS / run 31311225966
VillAIgence Production Soak #240:       SUCCESS / run 31311225982
VillAIgence GitHub Release #574:        SUCCESS / run 31311225980
release publication job:                SKIPPED
independent review P0/P1/P2:            0 / 0 / 0
open review threads:                    0
```

### Exit criterion — met

VillAIgence can persist and deterministically query bounded server-owned disagreement between two exact retained Semantic claims, survive replay/restart/consolidation/pressure/privacy boundaries, stop resolving disagreement when either live claim is forgotten, and prove that disagreement never promotes, rewrites, ranks or resolves either claim.

---

# NEXT — contradiction-aware prompt context without truth arbitration

PR #137 intentionally keeps contradiction evidence out of generic episodic formatting. The next slice should expose **live resolved disagreement** to the provider through a dedicated, bounded, non-authoritative context layer.

## Goal

```text
live resolved contradiction relation
→ exact current-player/NPC-global/shared eligibility
→ deterministic bounded selection
→ dedicated contradiction prompt section
→ both current retained claims visible as disagreement
→ current SYSTEM_OBSERVED facts rendered earlier and declared authoritative
→ no winner / promotion / confidence mutation
```

The provider should understand that the NPC has conflicting recollections while never being asked to decide Minecraft/server truth.

## Required design decisions

- exact prompt schema/wording for disagreement;
- hard candidate/result bounds for contradiction context;
- whether duplicate relations sharing one logical claim are deduplicated/clustered before formatting;
- deterministic ordering relative to Semantic Memory and episodic/social history;
- how to avoid re-rendering the same claim prose excessively;
- how the layer states that current observed facts remain authoritative;
- how malformed/forgotten relations contribute zero prompt context;
- how prompt text is kept inert data rather than instructions/tool authority.

## Required TDD scenarios

1. generic `Memory2ContextProvider` continues excluding `SEMANTIC_CONTRADICTION`;
2. dedicated contradiction provider returns both **live retained** source claims, not event summary text;
3. current server FACT appears structurally before contradiction context and remains authoritative;
4. contradiction context never changes source kind/provenance/confidence;
5. foreign-player contradiction consumes zero candidate/result slots;
6. private/shared/global visibility remains exact;
7. forgotten or malformed relations produce zero context;
8. source-union consolidation still resolves current logical claims;
9. repeated relation evidence cannot create unbounded duplicate prompt lines;
10. selection has explicit hard bounds and deterministic tie-breakers;
11. user-controlled remembered claim text is safely framed as data, not provider instructions;
12. restart/pressure/multi-NPC simulations remain deterministic;
13. existing Semantic `32` / `24+8` / `6` bounds and eight-hop rumor ancestry remain unchanged unless a separate measured design proves change necessary.

## Recommended implementation order

```text
prompt semantics/spec gate
→ pure bounded contradiction-context selector RED
→ live resolver-to-context adapter RED
→ privacy/filter-before-limit RED
→ snapshot layer ordering/authority RED
→ prompt injection/inert-data hardening RED
→ restart/pressure/dedup simulation
→ exact-head security / CI / soak / release dry-run
```

## Invariants

- contradiction context is recollection/disagreement, not server truth;
- no model-selected winner;
- no FACT promotion or BELIEF mutation;
- current SYSTEM_OBSERVED state remains structurally authoritative;
- source claim text comes from live retained Semantic entries only;
- historical contradiction event stores no prose and cannot resurrect forgotten text;
- provider/model cannot choose contradiction IDs, source IDs, visibility, retention or truth class;
- hard bounds prevent all-pairs prompt growth;
- no new store, public config, provider request, migration or legacy reader.

### Exit criterion

The provider can receive bounded, privacy-safe, inspectable live disagreement context that improves conversational awareness while current server-observed truth remains structurally authoritative and no contradiction is silently resolved.

---

# Later 0.2 — uncertainty / bounded distortion

After contradiction-aware prompt context, model how fallible social information may change across exact provenance chains without granting the model authority.

Potential semantics:

```text
origin source
speaker chain
confidence / uncertainty
contradiction state
distortion count
bounded transformation budget
```

Questions requiring explicit design:

- deterministic/server-owned uncertainty evolution versus bounded provider suggestions;
- how transformed statements retain inspectable origin history;
- hard hop/size/rate budgets for distortion;
- how relationship/trust influences confidence without becoming truth authority;
- how to prevent repetition/corroboration from becoming a confidence-escalation exploit;
- where an automatic contradiction candidate producer belongs without letting model output become authority.

A rumor remains non-authoritative even when repeated by many NPCs.

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
- able to preserve bounded exact multi-hop rumor ancestry;
- able to represent and surface contradiction without turning disagreement into FACT;
- able to model bounded uncertainty/distortion without provider truth authority;
- independent of the removed raw conversation store.

---

# 0.3 — Personality and NPC↔NPC social graph

## Goal

Persistent bounded personality plus pairwise social state that changes dialogue and behavior.

Potential dimensions:

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

### Exit criterion

Two NPCs retain durable relationship/personality history that affects dialogue, decisions and information exchange after restart.

---

# 0.4 — Knowledge ecosystem and rumors

## Goal

Expand the 0.2 transfer/provenance/contradiction primitives into settlement-scale information flow without omniscient distribution.

Potential knowledge classes:

```text
OBSERVED
TOLD_BY_PLAYER
TOLD_BY_NPC
OFFICIAL
INFERRED
RUMOR
UNKNOWN
```

This milestone is about distribution/social topology, not weakening the 0.2 FACT/BELIEF authority model.

### Exit criterion

Information moves through a settlement without omniscient distribution, conflicts remain representable and source history remains inspectable.

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

Required controls include event-driven scheduling, per-NPC/global budgets, action whitelist/policy, server-side target revalidation, bounded retry/backpressure and exactly-once effects.

### Exit criterion

NPCs pursue simple persistent goals autonomously without compromising server authority or performance.

---

# 0.6 — Settlement simulation

Villages become persistent social/economic systems with population, households, professions, resources, safety, morale, shared projects and public memory.

### Exit criterion

Settlement state changes over time and meaningfully affects individual NPC goals and behavior.

---

# 0.7 — Factions and politics

Persistent alliances, disputes, leadership, rules and inter-settlement relations with server-owned consequences.

### Exit criterion

Faction/political state survives restart, is grounded in simulation evidence and changes NPC/settlement behavior.

---

# 0.8 — Emergent stories

Multi-session narratives grounded in persistent events, memories, relationships, settlements and factions. Story remains the human-readable consequence of simulation history; the system must not generate a story first and retrofit state afterward.

### Exit criterion

Players can return after multiple sessions and encounter explainable ongoing social narratives rooted in recorded world history.

---

# 0.9 — Performance, large servers and local models

Scale the living society without making AI a per-NPC-per-tick cost center:

- event-driven scheduling;
- global/per-NPC model budgets;
- backpressure/cancellation;
- cache/retrieval profiling;
- large-population simulation soak;
- multi-day stability;
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

1.0 means these systems form one coherent persistent simulation, not a collection of unrelated AI features.

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
→ repository security
→ constrained soak / release dry-run when selected
→ independent base→head review
→ exact candidate / installed acceptance when required
→ root CHANGELOG update
→ PROJECT_STATE / ROADMAP reconciliation
```

Rules:

1. Do not write production behavior before intended RED has been observed.
2. Do not weaken assertions merely to make CI green.
3. Exact-release and installed evidence are separate from unit/automation evidence.
4. Deferred manual evidence remains explicitly deferred.
5. Significant product/runtime/persistence/config/release/security/permanent-CI changes update root `[Unreleased]` in the same PR.
6. Release PRs move shipped `[Unreleased]` items into exact version sections rather than duplicating them.
7. Before starting new work, reconcile these documents against live GitHub state.