# VillAIgence Project State

> **Canonical current-state handoff.** Read this file before `docs/ROADMAP.md`. Read root `CHANGELOG.md` for product/release history and `docs/superpowers/evidence/` for detailed TDD evidence.
>
> Last reconciled: **2026-08-09**, after contradiction-aware prompt context merged through PR #139.
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

latest product merge:               PR #139
latest product merge commit:        05dac0eaff408c13bf02ddd25d98acefd4f9cf13
latest official release:            0.2.0+1.21.1
latest release commit:              e426f588efefa6aa48a6e536c4a998421bbda241
installed 0.2.0 candidate JAR SHA:   56293f86634b50b2def044429aac6f2cf0d197eb16ac1e60224708f7b3333aee

next product slice:                 uncertainty / bounded distortion
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
uncertainty / bounded distortion                       NEXT
```

Installed boundaries that remain explicit:

```text
VAI-M2-INST-005  NOT TESTED / AUTOMATED EVIDENCE ONLY
VAI-CONCUR-004   NOT TESTED / DEFERRED
```

Neither is represented as PASS.

PRs #127, #129, #131, #133, #135, #137 and #139 are merged, automated source capabilities after the already-installed `0.2.0` release. Their CI/candidate evidence must **not** be described as installed `0.2.0` acceptance until a later exact candidate is built and explicitly accepted.

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
8. Confidence, repetition, corroboration count and rumor depth never upgrade BELIEF into FACT.
9. Candidate extraction is not admission, and admission is not authority.
10. Operator Lore is explicit background context, not an observed current-world fact.
11. Current observed world facts override conflicting lore, beliefs, rumors and disagreement context for current-world truth.
12. Clients never own permissions, target identity, source identity, truth class, revisions or persistent mutations.
13. Compatibility work requires a supported-data reason; experimental pre-1.0 data is not automatically entitled to migration code.
14. Exact release identity must match tag, filename, embedded metadata and manifest.
15. Published artifacts must be byte-identical to the exact artifact accepted by the release gate.
16. Automated logical-client evidence never silently becomes installed multi-client evidence.
17. Unknown, unsafe, protected and persistence-store CI changes fail closed to the complete mandatory matrix.
18. Release recovery may repair metadata/assets only from an existing immutable release tag commit and never moves the tag.
19. Relationship transitions and explanations of cause are separate evidence; dialogue prose does not become FACT because it accompanied a transition.
20. Player-scoped retrieval is an eligibility boundary, not a ranking preference: foreign-player data is excluded before bounded candidate/result allocation.
21. Long-horizon recall is bounded and deterministic; no memory class becomes immortal.
22. NPC-to-NPC transfer is exact-source-backed and always produces listener `BELIEF/NPC_TOLD`, never copied FACT authority.
23. Multi-hop rumor provenance is immutable, source-backed, acyclic and capped at eight exact hops.
24. Contradiction evidence records disagreement only. It does not select a winner, promote a claim, change confidence or delete either claim.
25. Historical contradiction evidence cannot resurrect forgotten claim prose: resolved disagreement exists only while both logical Semantic claims remain live and player-eligible.
26. Contradiction prompt context is a dedicated data layer, not generic `SYSTEM_OBSERVED` prose. It is bounded to four live relations and never changes the underlying Semantic truth classes.

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

The disagreement layer is deliberately after Semantic Memory because it annotates relationships between currently retained claims, and deliberately before generic episodic/social history because it has its own server-authored semantics. It never outranks current observed facts.

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

Current Semantic BELIEF extraction config remains:

```json
{
  "semanticBeliefExtractionEnabled": false,
  "semanticBeliefMaxCandidatesPerTurn": 3
}
```

Extraction remains opt-in. Hard candidate count is `8`; statements are bounded to `240` Unicode code points. Existing config version remains `2` and missing fields receive safe defaults.

PR #139 adds **no** config field, provider schema/call, persistence store or persistence-version change.

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
- bounded long-horizon retrieval: 24 recent + 8 durable candidates at the normal 32-candidate bound, then at most 6 prompt entries per memory domain;
- exact current-player/NPC-global/shared eligibility before bounded candidate allocation;
- source-backed NPC-to-NPC transfer into listener `BELIEF/NPC_TOLD`;
- immutable bounded v2 rumor ancestry with max 8 hops, deterministic branch selection, cycle/limit rejection and restart/replay safety;
- stable `SemanticMemoryIdentity` shared with consolidation;
- structured `SEMANTIC_CONTRADICTION / SYSTEM_OBSERVED` process evidence that stores no duplicate claim prose;
- exact contradiction lifecycle and deterministic identity;
- live contradiction resolution through logical claim identity so consolidation is tolerated but forgetting removes the resolved relation;
- global/private/shared contradiction eligibility before result limiting;
- dedicated contradiction-aware prompt provider hard-bounded to four live relations;
- shared Semantic statement sanitization for ordinary Semantic and disagreement rendering;
- immutable server-thread snapshot capture of disagreement context;
- five-layer deterministic prompt authority order;
- explicit prompt wording that disagreement is remembered data, never a truth verdict or instruction;
- current observed FACT authority preserved through conflicting BELIEF/rumor/disagreement cases;
- fresh-root, pressure, privacy, replay/restart and prompt-injection regression coverage.

Truth boundary:

```text
FACT                    → SYSTEM_OBSERVED only
BELIEF                  → PLAYER_TOLD / NPC_TOLD / INFERRED only
RELATIONSHIP_CAUSE      → server-observed process linkage only
rumor retelling         → BELIEF / NPC_TOLD at every downstream hop
SEMANTIC_CONTRADICTION  → server-observed disagreement linkage only
prompt disagreement     → live derived context; never a truth verdict
```

## PR #139 — contradiction-aware prompt context

Merged source capability:

```text
exact retained contradiction evidence
+ both logical Semantic claims still live
+ current-player eligibility
→ SemanticContradictionHistory
→ at most 4 resolved relations
→ shared safe Semantic claim renderer
→ immutable snapshot contradictionContext
→ dedicated prompt section
```

Final source-head evidence before squash merge:

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

Main CI passed common/mock-provider, GameTests/loaders, production acceptance/startup, persistence recovery and package verification. Production Soak passed constrained concurrency, exact staging and five restart cycles. Release dry-run passed exact production acceptance/recovery, GameTests/loaders, package smoke and accepted-JAR/package identity without publication.

Canonical TDD evidence:

```text
docs/superpowers/evidence/2026-08-09-contradiction-aware-prompt-context-tdd.md
```

---

# Automated acceptance and release boundary

Canonical acceptance catalog remains:

```text
34 total scenarios
28 AUTOMATED
6 MANUAL_CANARY
0 PLANNED
```

The remaining manual scenarios require installed graphical clients, physical microphone/UDP routing or subjective audible/spatial judgment rather than missing deterministic unit coverage.

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

Root `CHANGELOG.md` is canonical product/release history. Significant runtime/persistence/config/release/security/permanent-CI changes update `[Unreleased]` in the same PR.

`docs/CHANGELOG.md` remains historical engineering detail. Exact staged TDD evidence for recent slices is under `docs/superpowers/evidence/`.

PR #139 already updated root `[Unreleased]`; this docs reconciliation must not duplicate a product changelog entry.

---

# Known gaps and technical debt

1. `VAI-CONCUR-004` real two-graphical-client Operator Lore conflict presentation remains deferred.
2. `VAI-M2-INST-005` real second-player installed isolation remains untested; automated current-player/NPC-global/shared isolation exists.
3. Memory 2.0 now represents and safely prompts live disagreement, but **uncertainty and bounded information distortion do not yet exist**.
4. Contradiction relations are still created only through the explicit server-owned lifecycle; no automatic bounded detector/producer is wired into ordinary claim ingestion yet.
5. Multi-hop knowledge transfer remains explicitly server-invoked. Autonomous initiation, visible NPC↔NPC conversation and voice are future product slices.
6. Relationship/trust values do not yet alter belief confidence or rumor uncertainty; that requires a separate social-epistemology design so social affinity cannot become truth authority.
7. `PersistentChatMemory` is a no-storage compatibility façade and may be removed when the inherited AI call surface is refactored.
8. Historical config fields for the removed raw conversation store remain deserializable compatibility baggage.
9. Historical Javadoc/deprecation warnings remain non-blocking.

---

# Next optimal delivery step

The next product slice is **uncertainty / bounded distortion**.

VillAIgence can now retain sourced beliefs, move them through an exact bounded social provenance chain, represent a disagreement without selecting a winner, and expose live disagreement safely to the LLM while preserving current observed truth. The next missing capability is to model fallibility **without turning model confidence or repeated retelling into authority**.

Required boundary:

```text
source-backed BELIEF / rumor
+ immutable provenance chain
+ optional live contradiction relation
→ deterministic server-owned uncertainty state
→ strictly bounded transformation/distortion budget
→ original source/history remains inspectable
→ transformed claim remains BELIEF
→ current SYSTEM_OBSERVED FACT remains authoritative
```

Design questions to settle before production code:

- whether uncertainty is stored on Semantic BELIEF, transfer evidence, or a derived immutable layer;
- exact deterministic inputs to uncertainty evolution across hops;
- how current confidence relates to uncertainty without becoming authority;
- how to represent a transformed statement while retaining the exact original/source chain;
- hard bounds on transformation count, text size, hop depth and rate;
- whether any provider-suggested transformation is allowed and, if so, how it is constrained/revalidated;
- how contradiction affects uncertainty without choosing truth;
- how replay/restart/pressure preserves deterministic state;
- how to prevent repetition/corroboration spam from escalating confidence;
- how privacy eligibility is enforced before any uncertainty/distortion prompt allocation.

Do **not** combine this slice with automatic contradiction detection. A bounded detector/producer should remain a separate later slice unless uncertainty design proves it must be coupled.

Recommended order:

```text
uncertainty/distortion semantics spec
→ pure deterministic uncertainty policy RED
→ provenance-hop evolution RED
→ bounded transformed-claim representation RED
→ original-source preservation + no truth promotion RED
→ contradiction interaction RED
→ privacy/replay/restart/pressure RED
→ prompt presentation RED
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
