# VillAIgence Project State

> **Canonical current-state handoff.** Read this file before `docs/ROADMAP.md`. Read root `CHANGELOG.md` for product/release history and `docs/superpowers/evidence/` for staged TDD evidence.
>
> Last reconciled: **2026-08-10**, after bounded contradiction candidate/producer policy merged through PR #145.
>
> Always distinguish source/unit evidence, common integration, GameTests, production-candidate evidence, exact-release evidence and installed operator server/client evidence.

## Executive snapshot

VillAIgence is a Minecraft 1.21.1 MCA-derived mod evolving from AI-assisted villager dialogue into a persistent living-society simulation layer.

```text
repository:                         True-Ruslan/villAIgence
primary branch:                     1.21.1
Java:                               21
primary distribution:               Fabric
NeoForge:                           compile compatibility required

latest product merge:               PR #145
latest product merge commit:        ebda7ecd2290ce8eab0955c2be0d8ebed3065e1c
latest official release:            0.2.0+1.21.1
latest release commit:              e426f588efefa6aa48a6e536c4a998421bbda241
installed 0.2.0 candidate JAR SHA:   56293f86634b50b2def044429aac6f2cf0d197eb16ac1e60224708f7b3333aee

next product slice:                 settlement-scale information flow without omniscience
then:                               relationship/trust social epistemology
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
persistent-dialogue clean cutover                       COMPLETE / RELEASED
legacy memory.json migration                           CANCELLED BY DESIGN
0.2.0 clean-world installed acceptance                 7 PASS / 0 FAIL
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

Installed boundaries remain explicit:

```text
VAI-M2-INST-005  NOT TESTED / AUTOMATED EVIDENCE ONLY
VAI-CONCUR-004   NOT TESTED / DEFERRED
```

Neither is represented as PASS.

PRs #127, #129, #131, #133, #135, #137, #139, #141, #143 and #145 are merged automated source capabilities **after** the installed `0.2.0` release. Their CI/candidate evidence is not installed `0.2.0` acceptance.

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
8. Confidence, repetition, corroboration count, rumor depth, fallibility and transformation metadata never upgrade BELIEF into FACT.
9. Candidate extraction is not admission, and admission is not authority.
10. Operator Lore is explicit background context, not an observed current-world fact.
11. Current observed world facts override conflicting lore, beliefs, rumors, disagreement, fallibility and transformed claims.
12. Clients never own permissions, target/source identity, truth class, revisions or persistent mutations.
13. Compatibility work requires a supported-data reason; experimental pre-1.0 data is not automatically entitled to migration code.
14. Exact release identity must match tag, filename, embedded metadata and manifest.
15. Published artifacts must be byte-identical to the exact artifact accepted by the release gate.
16. Automated logical-client evidence never silently becomes installed multi-client evidence.
17. Unknown, unsafe, protected and persistence-store CI changes fail closed to the complete mandatory matrix.
18. Release recovery may repair metadata/assets only from an existing immutable release tag commit and never moves the tag.
19. Relationship transitions and explanations of cause are separate evidence; dialogue prose does not become FACT because it accompanied a transition.
20. Player-scoped retrieval is an eligibility boundary: foreign-player data is excluded before bounded candidate/result allocation.
21. Long-horizon recall is bounded and deterministic; no memory class becomes immortal.
22. NPC-to-NPC transfer is exact-source-backed and always produces listener `BELIEF/NPC_TOLD`, never copied FACT authority.
23. Multi-hop rumor provenance is immutable, source-backed, acyclic and capped at eight hops.
24. Canonical rumor ancestry selection is listener-independent and does not fall back to a more convenient branch after cycle/limit rejection.
25. Contradiction evidence records disagreement only; it does not select a winner, promote a claim, change confidence or delete either claim.
26. Historical contradiction evidence cannot resurrect forgotten claim prose; both logical Semantic claims must remain live and player-eligible.
27. Contradiction prompt context is a dedicated data layer, hard-bounded to four live relations, not generic verified prose.
28. **Rumor fallibility is process metadata, not truth likelihood.** Source distance describes the selected retained canonical provenance path only.
29. Loss of direct rumor provenance degrades to explicit `UNRESOLVED`; ancestry or transformation history is never reconstructed from stale prose.
30. **Wording transformation is explicit bounded process evidence.** The current primitive may omit one trailing sentence at most once per retained canonical lineage; transformed downstream knowledge remains BELIEF.
31. A transformed claim never rewrites its original v2 origin snapshot. Original source and current transformed wording remain separately auditable while direct evidence survives.
32. **Automatic contradiction production is bounded before classification.** Same-owner/exact-scope eligibility and duplicate suppression precede bounded comparison; classification records disagreement only and never becomes truth authority.
33. Settlement-scale propagation must reuse source-backed transfer rather than create shared omniscient state; eligibility, fan-out and work budgets must be explicit before autonomous dissemination is enabled.

Canonical AI/state flow:

```text
Minecraft/server state
→ immutable bounded snapshot
→ current observations + lore + layered memory/disagreement context
→ deterministic authority-layer composition
→ provider/LLM proposal
→ server validation/revalidation
→ server-owned mutation
→ persistent authoritative/process evidence
```

Current snapshot prompt order:

```text
current server-observed facts
→ Operator Lore
→ Semantic Memory (including inline rumor fallibility/transformation metadata)
→ live Semantic disagreement context
→ episodic / social history
→ structured provider/tool instructions
→ provider
```

Fallibility/transformation metadata uses the already-selected Semantic slot; it does not add a new authority layer or prompt capacity.

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

`events.json` is authoritative factual event history with its own validation path.

The experimental pre-0.2 `<world>/livingworld/memory.json` conversation store is no longer runtime/recovery state. No importer, dual reader, checkpoint ledger or destructive migration is planned for the accepted pre-1.0 clean-state boundary.

Current Semantic BELIEF extraction config remains:

```json
{
  "semanticBeliefExtractionEnabled": false,
  "semanticBeliefMaxCandidatesPerTurn": 3
}
```

Extraction remains opt-in. Hard candidate count is `8`; statements are bounded to `240` Unicode code points. Existing config version remains `2` and missing fields receive safe defaults.

PR #143 adds no public config, provider schema/call, new world file, migration/backfill, `semantic-memory.json` schema change or transfer evidence-ID namespace change. `memory2.json` remains format version 1; transfer DIALOGUE evidence may now carry an additive nullable `knowledgeTransferTransformation` snapshot, while historical records deserialize with no transformation.

PR #145 adds no provider schema/call, public config, world file, persistence version/field, migration/backfill or release identity change. It reuses existing `semantic-memory.json`, `memory2.json`, `semantic-contradiction-v1` evidence and the current bounded Memory 2.0 capacity.

---

# Memory 2.0 current capability

Implemented and retained:

- immutable NPC-owned episodic events;
- structured DIALOGUE, OBSERVATION, ACTION, RELATIONSHIP_CHANGE and RELATIONSHIP_CAUSE evidence;
- bounded Working Memory;
- typed Semantic FACT/BELIEF;
- controlled server-observed FACT ingestion;
- controlled source-backed BELIEF admission;
- deterministic Semantic consolidation/source union and pressure forgetting;
- deterministic episodic/social pressure retention using authoritative game time;
- bounded long-horizon retrieval: 24 recent + 8 durable candidates at the normal 32-candidate bound, then at most 6 prompt entries;
- exact current-player/NPC-global/shared eligibility before bounded candidate allocation;
- source-backed NPC-to-NPC transfer into listener `BELIEF/NPC_TOLD`;
- immutable bounded v2 rumor ancestry, max eight hops, deterministic branch selection and cycle/limit rejection;
- structured `SEMANTIC_CONTRADICTION / SYSTEM_OBSERVED` process evidence without duplicate claim prose;
- live contradiction resolution only while both logical claims remain retained and eligible;
- dedicated contradiction prompt context hard-bounded to four live relations;
- automatic contradiction production after controlled Semantic admission;
- contradiction candidate cap of 16 and comparison cap of 8 per admission;
- same-owner/exact-scope/equivalence filtering before candidate allocation;
- retained-relation duplicate suppression before comparison allocation;
- conservative deterministic opposition classification using exactly one standalone `not` / `не` insertion or removal;
- shared safe Semantic text rendering for ordinary memory and disagreements;
- deterministic rumor fallibility derived only after existing Semantic eligibility/selection/ranking;
- `RESOLVED` source distance of exactly 1..8 from retained canonical v2 provenance;
- explicit `UNRESOLVED` state when a retained `NPC_TOLD` BELIEF loses resolvable direct provenance;
- one deterministic wording transform primitive: `OMIT_TRAILING_SENTENCE`;
- hard one-transformation budget per selected retained canonical rumor lineage;
- immutable transformation snapshot carried forward unchanged by later ordinary transfers;
- exact v2 origin statement retained separately from transformed current statement;
- resolved fallibility reports `transformationsUsed=0|1` only from retained validated evidence;
- unresolved fallibility reports `transformationsUsed=UNKNOWN`, never a fabricated zero;
- transformed/plain same-ID transfer conflicts reject and exact transformed replay is idempotent;
- transformed knowledge remains `BELIEF/NPC_TOLD` with unchanged transfer confidence;
- ordinary FACT/PLAYER_TOLD/INFERRED prompt rendering remains unchanged;
- forged fallibility markers inside claim prose cannot enable server-authored guidance;
- current observed FACT authority preserved under rumors, disagreement, fallibility, transformed claims and automatically produced contradiction relations;
- restart/replay, pressure, privacy and prompt-injection regression coverage.

Truth/process boundary:

```text
FACT                    → SYSTEM_OBSERVED only
BELIEF                  → PLAYER_TOLD / NPC_TOLD / INFERRED only
RELATIONSHIP_CAUSE      → server-observed process linkage only
rumor retelling         → BELIEF / NPC_TOLD at every downstream hop
SEMANTIC_CONTRADICTION  → server-observed disagreement linkage only
prompt disagreement     → live derived context; never a truth verdict
rumor fallibility       → live derived process metadata; never a truth score
claim transformation    → server-owned bounded process evidence; never truth promotion
contradiction producer  → bounded pair classification; never truth arbitration
```

## PR #143 — bounded transformed-claim representation

Merged source capability:

```text
eligible sourced Semantic claim
+ server-owned transfer request
+ canonical v2 provenance
→ optional deterministic OMIT_TRAILING_SENTENCE
→ listener DIALOGUE / NPC_TOLD evidence
→ listener BELIEF / NPC_TOLD
→ immutable original origin + transformation snapshot
```

Current transformation contract:

```text
MAX_TRANSFORMATIONS = 1
max provenance hops = 8
allowed transform = OMIT_TRAILING_SENTENCE only
provider-supplied rewrite = none
second provider call = none
```

A transformed lineage may propagate unchanged. A second transformation request returns `TRANSFORMATION_LIMIT_REACHED`; a single-sentence/non-applicable source returns `TRANSFORMATION_NOT_APPLICABLE`. If direct evidence is lost, transformation history is not reconstructed from prose and downstream transfer remains fail-closed through `PROVENANCE_UNAVAILABLE`.

Current fallibility rendering:

```text
resolved ordinary rumor:    sourceDistanceHops=1..8, transformationsUsed=0
resolved transformed rumor: sourceDistanceHops=1..8, transformationsUsed=1
unresolved direct evidence:  sourcePath=UNRESOLVED, transformationsUsed=UNKNOWN
```

PR #143 final exact-head evidence before squash merge:

```text
verified head:                           29771cd6f1fdfb29f266c03b4b99928d3c048cc9
merge commit:                            4a34585cd8df7cbfac34d17be86c5fa36b41b213
Repository security policy #2038:       SUCCESS / run 31339892209
VillAIgence CI #2403:                   SUCCESS / run 31339892218
VillAIgence Production Soak #303:       SUCCESS / run 31339892208
VillAIgence GitHub Release #637:        SUCCESS / run 31339892210
release publication job:                SKIPPED
independent review P0/P1/P2:            0 / 0 / 0
open review threads:                    0
PR discussion comments:                 0
```

Canonical TDD evidence:

```text
docs/superpowers/evidence/2026-08-09-bounded-transformed-claim-tdd.md
```

## PR #145 — bounded contradiction candidate/producer policy

Merged source capability:

```text
controlled retained Semantic FACT/BELIEF
→ same-owner + exact-scope candidate selection
→ max 16 eligible candidates
→ retained-relation duplicate suppression
→ max 8 classifier comparisons
→ deterministic standalone not/не opposition rule
→ existing SemanticContradictionLifecycle
→ truth-neutral SEMANTIC_CONTRADICTION evidence
```

Current producer contract:

```text
MAX_CANDIDATES_PER_ADMISSION = 16
MAX_COMPARISONS_PER_ADMISSION = 8
provider classifier call = none
truth winner = none
confidence/provenance mutation = none
```

Controlled Semantic admission persists/consolidates first and rereads the retained logical claim before producer invocation. Direct low-level `SemanticMemoryStore.append(...)` remains storage-only. Existing retained contradiction pairs consume zero comparison slots. The initial classifier is intentionally narrow: antonyms, numeric differences, arbitrary paraphrases, double negation, reordering and trailing-sentence omission are not automatically classified.

Final exact-head evidence before squash merge:

```text
verified head:                           b43ccaa0d0e6fdcf480ac16dc3f80e74d1182584
merge commit:                            ebda7ecd2290ce8eab0955c2be0d8ebed3065e1c
Repository security policy #2075:       SUCCESS / run 31375662931
VillAIgence CI #2440:                   SUCCESS / run 31375662912
VillAIgence Production Soak #319:       SUCCESS / run 31375662909
VillAIgence GitHub Release #652:        SUCCESS / run 31375662908
release publication job:                SKIPPED
independent review P0/P1/P2:            0 / 0 / 0
open review threads:                    0
PR discussion comments:                 0
```

Main CI passed common/mock-provider tests, risk-selected GameTests and supported loader builds, production acceptance/startup, selected persistence recovery and distributable package verification. Production Soak passed constrained authenticated concurrency, exact production staging and five restart cycles. Release dry-run selected the complete release acceptance suite and passed exact identity/security, production acceptance, exact persistence recovery, GameTests/loaders, package smoke and accepted-JAR/package identity; publication remained skipped.

Canonical TDD evidence:

```text
docs/superpowers/evidence/2026-08-10-bounded-contradiction-producer-tdd.md
```

Review hardening made the duplicate-suppression event scan explicitly use the current bounded event capacity rather than an unbounded limit. Two intermediate test-fixture mistakes were recorded honestly and corrected without production changes: a wrong nested history result type and a semantic-polarity ordering assumption after exact fresh-root relation equality had already passed.

---

# Automated acceptance and release boundary

Canonical acceptance catalog remains:

```text
34 total scenarios
28 AUTOMATED
6 MANUAL_CANARY
0 PLANNED
```

Remaining manual scenarios require installed graphical clients, physical microphone/UDP routing or subjective audible/spatial judgment rather than missing deterministic unit coverage.

Current official release remains:

```text
tag:                     0.2.0+1.21.1
release commit:          e426f588efefa6aa48a6e536c4a998421bbda241
installed candidate SHA: 56293f86634b50b2def044429aac6f2cf0d197eb16ac1e60224708f7b3333aee
```

Installed clean-world result remains:

```text
required:          7 PASS / 0 FAIL
VAI-M2-INST-005:   NOT TESTED / AUTOMATED EVIDENCE ONLY
VAI-CONCUR-004:    NOT TESTED / DEFERRED
```

No source capability merged after `0.2.0` expands this installed-release claim without a later exact release candidate and explicit acceptance.

---

# Changelog governance

Root `CHANGELOG.md` remains canonical product/release history. Significant runtime/persistence/config/release/security/permanent-CI changes update `[Unreleased]` in the same PR.

`docs/CHANGELOG.md` remains historical engineering detail. Staged TDD evidence for recent slices is under `docs/superpowers/evidence/`.

PR #145 already updated root `[Unreleased]`; this docs reconciliation must not duplicate or rewrite that product changelog entry.

---

# Known gaps and technical debt

1. `VAI-CONCUR-004` real two-graphical-client Operator Lore conflict presentation remains deferred.
2. `VAI-M2-INST-005` real second-player installed isolation remains untested; automated current-player/NPC-global/shared isolation exists.
3. Automatic contradiction production is intentionally conservative: only one standalone English `not` or Russian `не` insertion/removal is classified. Antonyms, numeric/temporal disagreement and free-form semantic opposition require a separately justified bounded classifier extension.
4. The current wording transform is intentionally narrow and deterministic. Provider-authored paraphrase/generalization is not supported and should not be added without a separately justified authority-safe design.
5. Multi-hop knowledge transfer remains explicitly server-invoked. Autonomous initiation, visible NPC↔NPC conversation and voice are future product slices.
6. Relationship/trust values do not alter belief confidence/fallibility; that requires a separate social-epistemology design so social affinity cannot become truth authority.
7. `PersistentChatMemory` remains a no-storage compatibility façade pending inherited AI call-surface refactoring.
8. Historical config fields for the removed raw conversation store remain deserializable compatibility baggage.
9. Historical Javadoc/deprecation warnings remain non-blocking.

---

# Next optimal delivery step

The next product slice is **settlement-scale information flow without omniscience**.

The project now has source-backed NPC→NPC transfer, bounded eight-hop provenance, deterministic fallibility/transformation, live contradiction handling and automatic bounded contradiction production. The missing capability is not more truth machinery; it is a server-owned policy for deciding **when and between which eligible NPCs information may propagate** without broadcasting knowledge to an entire settlement.

Required boundary:

```text
retained source-backed knowledge
+ server-owned settlement / locality / participant context
+ eligible speaker/listener pair
→ bounded dissemination opportunity selection
→ strict per-source / per-cycle work and fan-out budgets
→ existing NpcKnowledgeTransferLifecycle
→ existing provenance / transformation / contradiction machinery
→ listener-local BELIEF only
→ no settlement-global omniscient memory
```

Design requirements before production code:

- settlement membership/locality and source/listener eligibility must be server-owned and evaluated before allocation;
- information must move only through explicit transfer evidence; no broadcast copy into every NPC memory;
- reuse the existing source-backed transfer lifecycle rather than creating a second knowledge-admission path;
- propagation opportunities must be event-driven or coarsely scheduled, never per-NPC-per-tick LLM work;
- define strict deterministic work/fan-out budgets before enabling autonomous dissemination;
- player-private scope must remain private unless an explicit existing sharing scope allows the target;
- existing eight-hop provenance, one-transform budget, contradiction producer and no-FACT-promotion rules remain unchanged;
- replay/restart must not duplicate transfer effects or silently fan information out again;
- no relationship/trust weighting is introduced in this slice; social epistemology remains the following separate step;
- no provider call should be required merely to select recipients or decide whether an eligible transfer opportunity exists.

Recommended TDD order:

```text
settlement/locality eligibility specification
→ RED: speaker/listener scope before allocation
→ RED: deterministic bounded opportunity selector
→ RED: strict work/fan-out budgets
→ RED: no broadcast / no foreign-player leakage
→ RED: exact existing transfer lifecycle delegation
→ RED: replay/restart idempotence
→ RED: provenance/transform/contradiction preservation
→ pressure with multiple settlements and many NPCs
→ exact-head CI / soak / release dry-run / independent review
```

---

# Session handoff protocol

For a new development session:

1. read `docs/PROJECT_STATE.md`;
2. read `docs/ROADMAP.md`;
3. read root `CHANGELOG.md`;
4. inspect current `1.21.1` HEAD;
5. inspect open/recent PRs, tags/releases and current CI;
6. reconcile live GitHub state against these documents before changing code;
7. use TDD for runtime behavior: specification → observed RED → minimal GREEN → focused regression → complete selected gates;
8. update root `CHANGELOG.md` and canonical state docs whenever the delivery boundary changes.

Do not infer PASS from stale documentation. GitHub state and exact evidence must be checked each session.
