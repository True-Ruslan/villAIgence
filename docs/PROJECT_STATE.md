# VillAIgence Project State

> **Canonical current-state handoff.** Read this file before `docs/ROADMAP.md`. Read root `CHANGELOG.md` for product/release history and `docs/superpowers/evidence/` for staged TDD evidence.
>
> Last reconciled: **2026-08-09**, after deterministic rumor fallibility merged through PR #141.
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

latest product merge:               PR #141
latest product merge commit:        e0951067227913b8cadb3e73ee34355b0b3302ff
latest official release:            0.2.0+1.21.1
latest release commit:              e426f588efefa6aa48a6e536c4a998421bbda241
installed 0.2.0 candidate JAR SHA:   56293f86634b50b2def044429aac6f2cf0d197eb16ac1e60224708f7b3333aee

next product slice:                 bounded transformed-claim representation
then:                               bounded contradiction producer policy where justified
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
bounded transformed-claim representation               NEXT
```

Installed boundaries remain explicit:

```text
VAI-M2-INST-005  NOT TESTED / AUTOMATED EVIDENCE ONLY
VAI-CONCUR-004   NOT TESTED / DEFERRED
```

Neither is represented as PASS.

PRs #127, #129, #131, #133, #135, #137, #139 and #141 are merged automated source capabilities **after** the installed `0.2.0` release. Their CI/candidate evidence is not installed `0.2.0` acceptance.

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
8. Confidence, repetition, corroboration count, rumor depth and fallibility metadata never upgrade BELIEF into FACT.
9. Candidate extraction is not admission, and admission is not authority.
10. Operator Lore is explicit background context, not an observed current-world fact.
11. Current observed world facts override conflicting lore, beliefs, rumors, disagreement and fallibility context.
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
28. **Rumor fallibility is process metadata, not truth likelihood.** Source distance describes the retained canonical provenance path only.
29. Loss of direct rumor provenance degrades to explicit `UNRESOLVED`; ancestry is never reconstructed from stale prose.
30. Any future wording transformation must preserve original source/provenance, remain hard-bounded and keep the downstream claim `BELIEF`.

Canonical AI/state flow:

```text
Minecraft/server state
→ immutable bounded snapshot
→ current observations + lore + layered memory/disagreement context
→ deterministic authority-layer composition
→ provider/LLM proposal
→ server validation/revalidation
→ server-owned mutation
→ persistent authoritative evidence
```

Current snapshot prompt order:

```text
current server-observed facts
→ Operator Lore
→ Semantic Memory
→ live Semantic disagreement context
→ episodic / social history
→ structured provider/tool instructions
→ provider
```

Semantic rumor fallibility is inline metadata on already-selected Semantic lines; it does not add a new authority layer or prompt slot.

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

PR #141 adds **no** config field, provider schema/call, persistence store, JSON field, persistence-version change, migration/backfill or evidence-ID namespace.

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
- shared safe Semantic text rendering for ordinary memory and disagreements;
- deterministic rumor fallibility derived only after existing Semantic eligibility/selection/ranking;
- `RESOLVED` source distance of exactly 1..8 from retained canonical v2 provenance;
- explicit `UNRESOLVED` state when a retained `NPC_TOLD` BELIEF loses resolvable direct provenance;
- `transformationsUsed=0` as a structural no-distortion boundary for PR #141;
- ordinary FACT/PLAYER_TOLD/INFERRED prompt rendering unchanged by fallibility;
- forged fallibility markers inside claim prose cannot enable server-authored fallibility guidance;
- current observed FACT authority preserved under rumors, disagreement and fallibility metadata;
- restart/replay, pressure, privacy and prompt-injection regression coverage.

Truth boundary:

```text
FACT                    → SYSTEM_OBSERVED only
BELIEF                  → PLAYER_TOLD / NPC_TOLD / INFERRED only
RELATIONSHIP_CAUSE      → server-observed process linkage only
rumor retelling         → BELIEF / NPC_TOLD at every downstream hop
SEMANTIC_CONTRADICTION  → server-observed disagreement linkage only
prompt disagreement     → live derived context; never a truth verdict
rumor fallibility       → live derived process metadata; never a truth score
```

## PR #141 — deterministic rumor fallibility

Merged source capability:

```text
selected eligible BELIEF / NPC_TOLD
+ retained canonical direct transfer evidence
→ existing KnowledgeTransferProvenanceResolver
→ valid canonical v2 provenance
→ RESOLVED sourceDistanceHops=1..8
→ inline Semantic fallibility metadata
```

If the retained rumor's direct evidence cannot be resolved:

```text
BELIEF / NPC_TOLD remains retained
→ sourcePath=UNRESOLVED
→ no fabricated distance
→ no reconstructed ancestry
```

Final source-head evidence before squash merge:

```text
verified head:                           a8726d17b1f71ed7594d8728cb920b97fea31493
merge commit:                            e0951067227913b8cadb3e73ee34355b0b3302ff
Repository security policy #1975:       SUCCESS / run 31321543868
VillAIgence CI #2340:                   SUCCESS / run 31321543834
VillAIgence Production Soak #275:       SUCCESS / run 31321543872
VillAIgence GitHub Release #609:        SUCCESS / run 31321543807
release publication job:                SKIPPED
independent review P0/P1/P2 after fix:  0 / 0 / 0
open review threads:                    0
```

Main CI passed common/mock-provider, GameTests/loaders, production acceptance/startup, persistence recovery and package verification. Production Soak passed constrained concurrency, exact staging and five restart cycles. Release dry-run passed exact production acceptance/recovery, GameTests/loaders, package smoke and accepted-JAR/package identity without publication.

Canonical TDD evidence:

```text
docs/superpowers/evidence/2026-08-09-deterministic-rumor-fallibility-tdd.md
```

The review hardening gate found and fixed one pre-freeze metadata/prose separation issue: ordinary statement text containing a fallibility-looking literal could enable explanatory guidance. The final detector accepts the marker as metadata only before the canonical statement field.

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

PR #141 already updated root `[Unreleased]`; this docs reconciliation must not duplicate or rewrite that product changelog entry.

---

# Known gaps and technical debt

1. `VAI-CONCUR-004` real two-graphical-client Operator Lore conflict presentation remains deferred.
2. `VAI-M2-INST-005` real second-player installed isolation remains untested; automated current-player/NPC-global/shared isolation exists.
3. Fallibility now exposes exact retained source distance, but **actual bounded wording transformation does not yet exist**; `transformationsUsed` is deliberately fixed to zero.
4. Contradiction relations are still created only through the explicit server-owned lifecycle; no automatic bounded detector/producer is wired into ordinary claim ingestion.
5. Multi-hop knowledge transfer remains explicitly server-invoked. Autonomous initiation, visible NPC↔NPC conversation and voice are future product slices.
6. Relationship/trust values do not alter belief confidence/fallibility; that requires a separate social-epistemology design so social affinity cannot become truth authority.
7. `PersistentChatMemory` remains a no-storage compatibility façade pending inherited AI call-surface refactoring.
8. Historical config fields for the removed raw conversation store remain deserializable compatibility baggage.
9. Historical Javadoc/deprecation warnings remain non-blocking.

---

# Next optimal delivery step

The next product slice is **bounded transformed-claim representation**, the second sub-slice of uncertainty / bounded distortion.

The system can now say how far a retained rumor is from its canonical source without pretending that distance is a truth probability. The next missing capability is a strictly bounded wording transformation whose process evidence remains auditable.

Required boundary:

```text
retained BELIEF / NPC_TOLD
+ canonical v2 provenance
+ deterministic fallibility state
→ explicit bounded transformation candidate
→ server-owned validation/admission
→ transformed downstream BELIEF
→ exact original source/history remains inspectable
→ transformation count remains hard-bounded
→ current SYSTEM_OBSERVED FACT remains authoritative
```

Design questions that must be settled before production code:

- where transformed wording and transformation evidence live without corrupting the existing provenance identity contract;
- whether transformation is purely server-deterministic or provider-suggested through a bounded schema;
- the exact hard transformation count/budget and whether it is lower than the eight-hop provenance limit;
- whether one transfer may transform at most once and how replay determines the same outcome;
- how original and transformed statements remain linked without treating either as FACT;
- what semantic transformations are allowed versus rejected as meaning-changing fabrication;
- how transformed-claim identity remains deterministic across restart/replay;
- how pressure/forgetting behaves when transformation evidence is lost;
- how contradiction interacts with transformed claims without selecting truth;
- how privacy eligibility remains enforced before any transformed prompt allocation.

Do **not** combine this slice with automatic contradiction detection, trust-weighted belief or autonomous rumor propagation.

Recommended TDD order:

```text
transformation semantics + evidence design
→ pure bounded transformation-state RED
→ deterministic identity/replay RED
→ exact original-source preservation RED
→ bounded candidate/admission RED
→ no FACT/confidence promotion RED
→ pressure/forgetting/restart RED
→ contradiction/privacy interaction RED
→ prompt rendering/injection safety RED
→ deterministic multi-NPC simulation
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
