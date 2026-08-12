# VillAIgence Project State

> **Canonical current-state handoff.** Read this file before `docs/ROADMAP.md`. Read root `CHANGELOG.md` for product/release history and `docs/superpowers/evidence/` for staged TDD evidence.
>
> Last reconciled: **2026-08-12**, after PR #160 merged the 0.3 release-convergence contract and verified release-candidate boundary.
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

latest product merge:               PR #158
latest product merge commit:        b3938678e9424a88f271131ac75a57b73ffec5bf
latest convergence merge:           PR #160
latest convergence merge commit:    03ccb2d5d047ca551a5ac6be6b927de4404f09cf
latest official release:            0.2.0+1.21.1
latest release commit:              e426f588efefa6aa48a6e536c4a998421bbda241
installed 0.2.0 candidate JAR SHA:   56293f86634b50b2def044429aac6f2cf0d197eb16ac1e60224708f7b3333aee

next product slice:                 exact 0.3.0+1.21.1 release request / candidate creation
then:                               installed clean-state acceptance + post-release reconciliation
```

Current delivery state:

```text
0.1.x reliability/security baseline                    COMPLETE
MCA selective synchronization S1-S8                    COMPLETE
Operator Lore S9-S10c                                  COMPLETE
M11 Phases A-E                                         COMPLETE AT AUTOMATION LAYER
acceptance catalog                                     28 AUTOMATED / 6 MANUAL / 0 PLANNED
release/recovery automation                            COMPLETE / VERSION-AWARE

0.2 Memory 2.0 source-capability track                 COMPLETE AT CURRENT PLANNED BOUNDARY / UNRELEASED
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
settlement-scale information flow without omniscience  COMPLETE / PR #147
relationship/trust social epistemology                 COMPLETE / PR #149

0.3 Personality + NPC↔NPC Social Graph                 CONVERGENCE COMPLETE / RELEASE REQUEST NEXT
NPC↔NPC social-graph persistence foundation            COMPLETE / PR #151
server-owned causal NPC↔NPC social mutation lifecycle COMPLETE / PR #153
bounded Personality + direct-pair social snapshot      COMPLETE / PR #155
deliberate dialogue/behavior integration               COMPLETE / PR #158
0.3 convergence / release-candidate planning           COMPLETE / PR #160
exact 0.3.0+1.21.1 release request / candidate         NEXT
```

Installed boundaries remain explicit:

```text
VAI-M2-INST-005  NOT TESTED / AUTOMATED EVIDENCE ONLY
VAI-CONCUR-004   NOT TESTED / DEFERRED
```

Neither is represented as PASS.

PRs #127, #129, #131, #133, #135, #137, #139, #141, #143, #145, #147, #149, #151, #153, #155 and #158 are merged automated source capabilities **after** the installed `0.2.0` release. Their CI/candidate evidence is not installed `0.2.0` acceptance.

PR #160 is release-convergence infrastructure and evidence, not a gameplay capability and not installed acceptance. It deliberately leaves `docs/releases/NEXT_RELEASE.txt` at `0.2.0+1.21.1`; publication remains a separate explicit shipping step.

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
4. Provider and auxiliary-persistence failures fail soft whenever safe; corruption boundaries fail closed where replay authority could be lost.
5. Retry/replay paths must not duplicate dialogue, memory, relationship, social-graph or gameplay effects.
6. **FACT requires `SYSTEM_OBSERVED` server-owned evidence.**
7. **BELIEF remains non-authoritative** and may use only `PLAYER_TOLD`, `NPC_TOLD` or `INFERRED` provenance.
8. Confidence, repetition, corroboration count, rumor depth, fallibility, transformations and social state never upgrade BELIEF into FACT.
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
19. Relationship/social transitions and explanations of cause are separate evidence; dialogue prose does not become FACT because it accompanied a transition.
20. Player-scoped retrieval is an eligibility boundary: foreign-player data is excluded before bounded allocation.
21. Long-horizon recall is bounded and deterministic; no memory class becomes immortal.
22. NPC-to-NPC knowledge transfer is exact-source-backed and always produces listener `BELIEF/NPC_TOLD`, never copied FACT authority.
23. Multi-hop rumor provenance is immutable, source-backed, acyclic and capped at eight hops.
24. Canonical rumor ancestry selection is listener-independent and does not fall back after cycle/limit rejection.
25. Contradiction evidence records disagreement only; it does not choose a winner, promote a claim, change confidence or delete either claim.
26. Historical contradiction evidence cannot resurrect forgotten claim prose.
27. Contradiction prompt context is a dedicated bounded data layer, not generic verified prose.
28. **Rumor fallibility is process metadata, not truth likelihood.**
29. Loss of direct rumor provenance degrades to explicit `UNRESOLVED`; ancestry/transformation history is never reconstructed from stale prose.
30. **Wording transformation is explicit bounded process evidence.** The current primitive may omit one trailing sentence at most once per retained lineage.
31. A transformed claim never rewrites its original v2 origin snapshot.
32. **Automatic contradiction production is bounded before classification.** Eligibility and duplicate suppression precede comparison.
33. **Settlement information flow is transfer, not shared omniscience.** There is no settlement-global memory or broadcast-copy authority.
34. **Social trust is a current personal BELIEF view, not truth authority.** NPC×player trust may annotate an already-selected BELIEF but cannot change ranking or persistence.
35. **NPC×player and NPC↔NPC social state are separate authority domains.** `relationships.json` and `npc-social-graph.json` are not interchangeable.
36. **MCA Personality remains the canonical persistent personality source.** 0.3 derives snapshots from existing tracked/NBT entity state rather than creating another personality store.
37. **NPC↔NPC social mutation requires server-owned cause evidence.** The low-level graph store is persistence only; the lifecycle validates real NPC identities and exact source evidence.
38. **Exactly-once NPC social mutation authority lives in the graph frontier.** Bounded Memory 2.0 audit is process evidence and may be forgotten without reopening a historical mutation.
39. **Terminal causal outcomes consume cause order.** `APPLIED`, `NO_CHANGE` and `CAPACITY_REACHED` all advance the source frontier; old causes cannot become effective later.
40. **Malformed attributable causal frontier state fails closed per source.** One bad cursor must not erase otherwise-valid graph state or silently regain mutation authority after unrelated writes/restart.
41. **Personality/social snapshot construction is read-only derived context.** It may expose only canonical MCA Personality plus one exact directed NPC pair, cannot enumerate the graph, and cannot mutate persistence or truth authority.
42. **Social/personality influence is preference, not authority.** Dialogue tone and optional behavior gating may consume closed server-owned influence, but current observations, gameplay validation, truth rules and causal mutation authority remain stronger.
43. **Behavior authorization does not use fail-open recovery.** Authority-sensitive social/relationship reads are strict, read-only and fail closed on malformed, unsafe or non-canonical persisted state rather than repairing it into neutral/allowed state.
44. **Release convergence is not publication.** A planned candidate may be fully validated while `NEXT_RELEASE.txt` remains unchanged; exact release request, immutable tag/publication and installed acceptance are separate evidence stages.

Canonical AI/state flow:

```text
Minecraft/server state
→ immutable bounded snapshot
→ current observations + personality/direct-social context + bounded dialogue guidance + lore + layered memory/disagreement context
→ deterministic authority-layer composition
→ provider/LLM proposal
→ server validation/revalidation
→ server-owned mutation
→ persistent authoritative/process evidence
```

Current prompt order is now:

```text
current server-observed facts
→ bounded MCA Personality / direct NPC-pair social context
→ bounded server-authored dialogue guidance
→ Operator Lore
→ Semantic Memory (including bounded fallibility/transformation/social-epistemic metadata)
→ live Semantic disagreement context
→ episodic / social history
→ structured provider/tool instructions
→ provider
```

PR #155 established the fixed-size read-only snapshot layer. PR #158 added closed influence/guidance and narrow server-owned behavior gating without granting the provider graph-write or truth authority. Normal player↔NPC dialogue remains personality-only unless an explicit counterpart exists.

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
<world>/livingworld/npc-social-graph.json
```

Current auxiliary corruption/recovery matrix:

```text
memory2.json
semantic-memory.json
relationships.json
voices.json
operator-lore.json
npc-social-graph.json
```

`events.json` is authoritative factual event history with its own validation path.

The experimental pre-0.2 `<world>/livingworld/memory.json` conversation store is no longer runtime/recovery state. No importer, dual reader or destructive migration is planned for the accepted pre-1.0 clean-state boundary.

Current Semantic BELIEF extraction config remains:

```json
{
  "semanticBeliefExtractionEnabled": false,
  "semanticBeliefMaxCandidatesPerTurn": 3
}
```

Config version remains `2`. PR #158 added no public configuration, persistence file/version or migration.

## NPC social graph persistence and causal frontier

`npc-social-graph.json` remains format v1. PR #151 established the directed graph foundation; PR #153 added an additive causal frontier without a format bump or migration.

Directed edge contract:

```text
state dimensions:                trust / respect / fear / affinity
state bounds:                    [-100,+100]
max outgoing non-neutral edges:  64 per source NPC
overflow:                        CAPACITY_REACHED / reject-new / no eviction
self/null pair:                  INVALID_PAIR
neutral edge:                    compacted away
```

Causal mutation contract:

```text
cause authority:          exact retained source-owned SYSTEM_OBSERVED OBSERVATION|ACTION
required target evidence: target NPC UUID included in the source event participants
identity authority:       server validates source + target as MCA NPCs
mutation identity:        deterministic from sourceNpcId + causeEventId
ordering:                 gameTime, then cause-event UUID
replay:                   REPLAYED / no second delta
conflicting same cause:   CONFLICTING_CAUSE
older cause:              STALE_CAUSE
corrupt source frontier:  FRONTIER_CORRUPT
terminal frontier states: APPLIED / NO_CHANGE / CAPACITY_REACHED
```

The frontier is persisted atomically with graph state. `NPC_SOCIAL_CHANGE` Memory 2.0 evidence is an audit view only; it is not the exactly-once ledger and is excluded from generic prompt context.

Malformed persisted frontier payloads are decoded through a persistence-only tolerant boundary and then rejected by source-local sanitation. Runtime `NpcSocialMutationCursor` invariants remain strict.

For behavior/authorization decisions, PR #158 added strict read-only authority views that intentionally bypass normal recovery. Missing persistence may be neutral where the contract allows it; malformed, symlinked, non-regular, hostile, out-of-range or non-canonical persisted authority fails closed without repair/mutation.

---

# Memory 2.0 current capability

Implemented and retained:

- immutable NPC-owned episodic events;
- structured DIALOGUE, OBSERVATION, ACTION, RELATIONSHIP_CHANGE, RELATIONSHIP_CAUSE and NPC_SOCIAL_CHANGE process evidence;
- bounded Working Memory;
- typed Semantic FACT/BELIEF;
- controlled server-observed FACT ingestion and source-backed BELIEF admission;
- deterministic Semantic consolidation/source union and pressure forgetting;
- deterministic episodic/social pressure retention using authoritative game time;
- bounded long-horizon retrieval: 24 recent + 8 durable candidates at the normal 32-candidate bound, then at most 6 prompt entries;
- current-player/NPC-global/shared eligibility before bounded allocation;
- exact source-backed NPC-to-NPC transfer into listener `BELIEF/NPC_TOLD`;
- immutable v2 rumor ancestry capped at eight hops;
- deterministic contradiction representation, live resolution and max-four disagreement prompt layer;
- conservative automatic contradiction producer: max 16 candidates / max 8 comparisons per admission;
- bounded home-village settlement dissemination: max 16 residents / 4 speakers / 2 sources per speaker / 4 opportunities per cycle;
- exact player-origin social source resolution and bounded NPC×player trust annotation after ranking;
- one deterministic rumor wording transform (`OMIT_TRAILING_SENTENCE`) with lineage budget 1;
- server-owned causal NPC↔NPC social mutation audit evidence without Semantic truth effects;
- exact-pair social suppression of settlement knowledge transfer after deterministic pair selection;
- replay/restart/pressure/privacy/prompt-injection and corruption-regression coverage.

Truth/process boundary:

```text
FACT                    → SYSTEM_OBSERVED only
BELIEF                  → PLAYER_TOLD / NPC_TOLD / INFERRED only
RELATIONSHIP_CAUSE      → server-observed process linkage only
rumor retelling         → BELIEF / NPC_TOLD at every downstream hop
SEMANTIC_CONTRADICTION  → server-observed disagreement linkage only
NPC_SOCIAL_CHANGE       → server-observed pairwise social process audit only
prompt disagreement     → live derived context; never a truth verdict
rumor fallibility       → live derived process metadata; never a truth score
claim transformation    → server-owned bounded process evidence; never truth promotion
settlement flow         → bounded transfer routing; never shared truth
social epistemics       → bounded personal player-trust view; never persisted truth
NPC social graph        → persistent directed social state; never FACT/Semantic authority
personality/social view → immutable bounded derived context; never mutation authority
dialogue guidance       → server-authored preference layer; never truth/gameplay authority
behavior social gate    → server-owned allow/suppress policy; never graph mutation authority
```

---

# PR #153 — server-owned causal NPC↔NPC social mutation lifecycle

Merged source capability:

```text
exact retained SYSTEM_OBSERVED cause
+ validated live source/target MCA NPC identities
+ bounded directed social delta
→ deterministic mutation identity
→ source-local ordered causal frontier
→ atomic graph state + frontier persistence
→ optional bounded structured NPC_SOCIAL_CHANGE audit
→ exact replay without duplicate effect
```

Key guarantees:

- exact source event lookup uses `MemoryEventStore.findById`;
- one source cause cannot be silently reused for another target/delta;
- `APPLIED`, `NO_CHANGE` and `CAPACITY_REACHED` all consume causal order;
- crash after graph commit but before audit append cannot cause a duplicate graph delta and replay does not fabricate missing history;
- source/audit forgetting never rolls back graph state or reopens an old cause;
- `relationships.json` remains byte-independent from NPC↔NPC state;
- no Semantic state is created by the lifecycle;
- old graph v1 files without a causal frontier remain compatible;
- malformed/duplicate/inconsistent frontier state fails closed by attributable source;
- malformed required cursor fields cannot escalate one record into whole-file graph loss;
- live MCA identity authority is covered by Fabric GameTest;
- exact production startup/restart exercises causal graph + audit replay;
- no provider request/schema, public config, autonomous social evolution or official release identity was changed.

Final exact-head evidence before squash merge:

```text
verified head:                           ad75a7e51dfe13a43631d4de29848c8f7656d330
merge commit:                            2a75e950e4e7f43f1321fc572c260b00f6d2bdf4
Repository security policy #2303:       SUCCESS / run 31469172227
VillAIgence CI #2668:                   SUCCESS / run 31469172371
VillAIgence Production Soak #419:       SUCCESS / run 31469172211
VillAIgence GitHub Release #752:        SUCCESS / run 31469172244
release publication job:                SKIPPED
review P0/P1/P2:                        0 / 0 / 0
unresolved review threads:              0
PR discussion comments:                 0
```

CI passed the 775-test common suite, deterministic mock-provider tests, server GameTests, Fabric/NeoForge builds, production acceptance, two-start production restart coverage, persistence recovery and package verification. Soak passed constrained concurrency/staging/restarts. Release dry-run repeated the complete acceptance/recovery/GameTest/package matrix and verified accepted-JAR/package identity while publication remained skipped.

Review hardening found and fixed two real persistence defects tests-first:

1. malformed map key with a valid cursor source did not initially block the attributable source across unrelated save/reload;
2. a missing required cursor field could initially throw during Gson record construction and trigger whole-file recovery.

Both are covered by permanent regression tests. Full staged evidence:

```text
docs/superpowers/evidence/2026-08-11-causal-npc-social-mutation-tdd.md
```

---

# PR #155 — bounded read-only MCA Personality + direct NPC-pair social snapshot

Merged source capability:

```text
live MCA VillagerBrain Personality
+ optional exact counterpart NPC
+ exact direct source→target NpcSocialState
→ immutable bounded PersonalitySocialSnapshot
→ deterministic fixed-size rendering
→ centralized prompt authority placement
→ zero snapshot-construction persistence mutation
```

Key guarantees:

- MCA `VillagerBrain.getPersonality()` remains the sole persistent personality authority;
- common transport carries a closed canonical lowercase token rather than a second personality model;
- with a counterpart, exactly one directed graph pair is read; A→B and B→A remain independent;
- without a counterpart, capture is personality-only and does not open/create the NPC social graph;
- snapshot owner must match `LivingWorldContextSnapshot.villagerId`;
- legacy `PersonalityModule.apply(...)` remains source-compatible while snapshot capture de-duplicates personality text;
- prompt order is current world facts → personality/social → Operator Lore → Semantic → disagreement → episodic/social history;
- rendering is fixed-size and does not expose UUID/name/free-form graph content;
- repeated capture/render preserves existing `npc-social-graph.json`, `relationships.json`, `memory2.json` and `semantic-memory.json` bytes;
- tracked MCA Personality is unchanged by capture;
- Fabric GameTests cover live FRIENDLY/CRABBY extraction, asymmetric A→B/B→A state, personality-only capture and every current `Personality.values()` transport token;
- no provider call/schema, public config, persistence file/version, migration, autonomous social delta or release identity was added.

Final exact-head evidence before squash merge:

```text
verified head:                           7ed568bbb608c03f96f3d23113881b6cf99ca912
merge commit:                            a04e76dcf3ca6a07126e4e4b46f4d417a857a10f
Repository security policy #2388:       SUCCESS
VillAIgence CI #2753:                   SUCCESS
VillAIgence Production Soak #459:       SUCCESS
VillAIgence GitHub Release #792:        SUCCESS
release publication job:                SKIPPED
review P0/P1/P2/P3:                     0 / 0 / 0 / 0
unresolved review threads:              0
```

CI passed common/deterministic provider tests, Fabric GameTests, Fabric/NeoForge builds, production acceptance, persistence recovery and distributable package verification. The release dry-run repeated exact production acceptance, exact recovery, GameTests/builds and accepted-JAR/package identity while publication remained skipped.

Full staged evidence:

```text
docs/superpowers/evidence/2026-08-11-personality-social-snapshot-tdd.md
```

---

# PR #158 — deliberate Personality + social dialogue/behavior integration

Merged source capability:

```text
immutable PersonalitySocialSnapshot
→ closed server-owned PersonalitySocialInfluence
→ fixed-size dialogue guidance
→ centralized prompt composition

existing deterministic settlement pair selection
→ strict exact selected speaker→listener social read
→ conservative allow/suppress gate
→ existing NpcKnowledgeTransferLifecycle

capture-time optionalCommand allowlist
→ server-thread execution boundary
→ strict fresh NPC×player relationship authorization
→ existing command lookup/call
```

Key guarantees:

- MCA Personality influences dialogue through closed deterministic style categories only;
- exact directed NPC social state may add one bounded pair-disposition guidance line; no graph enumeration/ranking or raw numeric graph prose is exposed;
- prompt order is current facts → descriptive personality/social → bounded guidance → lore → Semantic → disagreement → episodic/social history;
- current observations, safety rules, permissions and structured action validation outrank style/stance preference;
- settlement knowledge-flow selector/fan-out remain unchanged; strong fear/distrust/antipathy suppresses only the already-selected directed transfer with no fallback listener;
- malformed/unsafe social authority fails closed instead of recovering to neutral/allow;
- `follow-player` is revalidated immediately before server execution against fresh NPC×player relationship authority;
- malformed/unsafe/hostile relationship persistence fails closed without recovery mutation; unrelated safe commands remain relationship-store independent;
- provider output still cannot author raw NPC social deltas or bypass `NpcSocialMutationLifecycle`;
- no provider request/schema, config, persistence version/file, migration/backfill, autonomous per-tick agent or release publication was added;
- live Fabric GameTests cover real MCA `FRIENDLY`, `CRABBY`, `ANXIOUS` and asymmetric A→B/B→A influence.

Independent review found four real authority-integrity defects before delivery and drove permanent RED→GREEN coverage:

1. corrupt `npc-social-graph.json` could recover to neutral/allow during settlement gating;
2. capture-time `relationships.json` recovery could erase a later deny;
3. missing/out-of-range relationship fields could default/clamp into allowed authority;
4. non-canonical UUID pair keys accepted by `UUID.fromString(...)` could normalize malformed persisted authority.

Final exact-head evidence before squash merge:

```text
verified head:                           6522b69fc885635d9be79df574fb29b15a97eddf
merge commit:                            b3938678e9424a88f271131ac75a57b73ffec5bf
Repository security policy #2456:       SUCCESS / run 31548923212
VillAIgence CI #2821:                   SUCCESS / run 31548923255
VillAIgence Production Soak #490:       SUCCESS / run 31548923258
VillAIgence GitHub Release #823:        SUCCESS / run 31548923215
release publication job:                SKIPPED
review P0/P1/P2:                        0 / 0 / 0
unresolved review threads:              0
PR discussion comments:                 0
```

CI passed common/deterministic provider tests, live Fabric GameTests, supported loader builds, production acceptance, selected persistence recovery and package verification. Soak passed constrained concurrency, exact runtime staging and five restart cycles. Release dry-run repeated complete release acceptance/recovery/GameTest/package verification and verified the production-accepted JAR was the packaged JAR while publication remained skipped.

Full staged and review-hardening evidence:

```text
docs/superpowers/evidence/2026-08-11-personality-social-dialogue-behavior-tdd.md
```

---

# PR #160 — 0.3 release convergence contract

Merged release-convergence capability:

```text
immutable release baseline 0.2.0+1.21.1
+ actual post-release feat: history
+ root CHANGELOG [Unreleased]
+ seven world-store / six recovery-store contracts
+ explicit manual/deferred acceptance boundary
→ machine-readable 0.3.0+1.21.1 convergence contract
→ fail-closed CI/release-dry-run validation
→ exact candidate plan
→ publication trigger remains separate
```

Key guarantees:

- planned exact candidate is `0.3.0+1.21.1`;
- immutable previous release is `0.2.0+1.21.1` at `e426f588efefa6aa48a6e536c4a998421bbda241`;
- actual post-release product capability inventory is #123, #125, #127, #129, #131, #133, #135, #137, #139, #141, #143, #145, #147, #149, #151, #153, #155 and #158;
- release-infrastructure PRs #121/#122 are tracked separately from gameplay/product capability;
- the convergence validator checks exact version identity, post-release feature history, root `[Unreleased]` traceability, world/recovery-store boundaries, clean-state/no-migration policy and exact installed manual/deferred boundaries;
- pull-request release validation uses canonical base history rather than the synthetic merge head; exact release contexts validate `HEAD`;
- `docs/releases/NEXT_RELEASE.txt` remains `0.2.0+1.21.1`, so PR #160 cannot arm or publish 0.3;
- no runtime behavior, provider protocol, persistence format, public config, migration/backfill or GitHub Actions workflow count changed.

Final exact-head evidence before squash merge:

```text
verified head:                           41f76c518bae98a8c373522c76eb7066c280a3e9
merge commit:                            03ccb2d5d047ca551a5ac6be6b927de4404f09cf
Repository security policy #2498:       SUCCESS / run 31580558127
VillAIgence CI #2862:                   SUCCESS / run 31580558133
VillAIgence Production Soak #509:       SUCCESS / run 31580558351
VillAIgence GitHub Release #842:        SUCCESS / run 31580558274
release publication job:                SKIPPED
review P0/P1/P2:                        0 / 0 / 0
unresolved review threads:              0
```

The full common suite, deterministic provider coverage, live Fabric GameTests, Fabric/NeoForge builds, production startup/restart acceptance, exact persistence recovery matrix, package verification, constrained soak and release dry-run all passed on the frozen head. Release dry-run verified accepted-JAR/package identity while `PUBLISH_RELEASE=false` kept publication skipped.

Staged convergence evidence and exact candidate plan:

```text
docs/superpowers/evidence/2026-08-12-0.3-release-convergence-tdd.md
docs/releases/0.3.0+1.21.1-PLAN.md
docs/releases/0.3.0-convergence.json
```

### 0.3 convergence exit criterion — met

The repository has one explicit 0.3 release scope, exact candidate identity, complete automated coverage map, honest manual/deferred boundary, clean-state migration policy and verified rollback/recovery path. The next step is the **separate explicit release request/candidate creation**, not further feature expansion.

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

Root `CHANGELOG.md` remains canonical product/release history. Significant runtime/persistence/config/release/security/permanent-CI changes update `[Unreleased]` in the same runtime PR.

`docs/CHANGELOG.md` remains historical engineering detail. Staged TDD evidence for recent slices is under `docs/superpowers/evidence/`.

PR #160 already updated root `[Unreleased]` for convergence infrastructure; this docs reconciliation does not duplicate or rewrite that product history.

---

# Known gaps and technical debt

1. `VAI-CONCUR-004` real two-graphical-client Operator Lore conflict presentation remains deferred.
2. `VAI-M2-INST-005` real second-player installed isolation remains untested; automated current-player/NPC-global/shared isolation exists.
3. Automatic contradiction production intentionally recognizes only a narrow standalone English `not` / Russian `не` polarity pattern; antonym/numeric/temporal/free-form opposition needs a separately justified bounded extension.
4. The current wording transform is intentionally narrow and deterministic; provider-authored open-ended paraphrase/generalization is not supported.
5. Settlement dissemination uses home-village membership and bounded deterministic routing only; physical proximity, travel gossip, alliances and social-topology routing remain future work.
6. NPC↔NPC social state now deliberately influences bounded dialogue guidance and exact-pair settlement knowledge suppression, but there is still no high-frequency autonomous social evolution or graph-neighborhood behavior policy.
7. New-edge capacity admission still scans the flat graph map; acceptable at current event-driven frequency, but index before high-frequency autonomous mutation.
8. NPC×player social epistemology currently uses trust only and only as a derived post-ranking annotation for player-origin BELIEF; NPC↔NPC state does not modify truth, ranking or source credibility.
9. Causal social frontier stores only the latest ordered cause per source NPC by design; retained Memory 2.0 audit is bounded process history, not an unbounded mutation ledger.
10. `PersistentChatMemory` remains a no-storage compatibility façade pending inherited AI call-surface refactoring.
11. Historical config fields for the removed raw conversation store remain deserializable compatibility baggage.
12. Historical Javadoc/deprecation warnings remain non-blocking.
13. Strict authority readers intentionally parse canonical files synchronously at bounded interaction/settlement boundaries; reassess indexing/caching before any high-frequency autonomous loop.

---

# Next optimal delivery step

The next delivery slice is **exact `0.3.0+1.21.1` release request / candidate creation**.

Convergence is complete. Do not add more 0.3 gameplay scope before creating the exact candidate. The release request must remain a separate shipping change so that publication is impossible until the exact request head has passed release validation.

Target shipping boundary:

```text
green reconciled 1.21.1 main
+ docs/releases/0.3.0+1.21.1-PLAN.md
+ docs/releases/0.3.0-convergence.json
+ exact release request NEXT_RELEASE.txt = 0.3.0+1.21.1
→ release-request validation on exact PR head
→ squash merge with expected-head protection
→ main push creates immutable 0.3.0+1.21.1 tag/release through existing workflow
→ verify tag / asset / embedded metadata / manifest / accepted-JAR identity
→ install exact release asset on a clean private test-server boundary
→ run required installed acceptance
→ record PASS / FAIL / NOT TESTED honestly
→ reconcile PROJECT_STATE / ROADMAP / CHANGELOG release state
```

Required release-request work:

- start from current reconciled `1.21.1` main and change `docs/releases/NEXT_RELEASE.txt` to exactly `0.3.0+1.21.1` in a dedicated release PR;
- move shipped root `[Unreleased]` entries into the exact `0.3.0+1.21.1` release section according to changelog governance, without duplicating history;
- keep the machine-readable convergence contract unchanged unless a real release-critical mismatch is found;
- require repository security, full CI, production soak and GitHub Release dry-run on the exact release-request head before merge;
- independently review base→head and require P0/P1/P2 = 0 plus zero unresolved threads;
- merge only with expected-head protection; do not move or recreate any existing tag;
- after publication, verify the GitHub Release asset is byte-identical to the production-accepted/package-verified JAR;
- only then perform installed clean-state acceptance; `VAI-M2-INST-005` and `VAI-CONCUR-004` remain explicit NOT TESTED/deferred unless real evidence becomes available.

Do not start 0.4/0.5 feature expansion until the exact 0.3 release and installed acceptance boundary are reconciled.

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
8. update root `CHANGELOG.md` in runtime PRs and reconcile `PROJECT_STATE.md` / `ROADMAP.md` after merge.

Do not infer PASS from stale documentation. GitHub state and exact evidence must be checked each session.