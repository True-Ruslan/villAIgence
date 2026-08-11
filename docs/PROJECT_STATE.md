# VillAIgence Project State

> **Canonical current-state handoff.** Read this file before `docs/ROADMAP.md`. Read root `CHANGELOG.md` for product/release history and `docs/superpowers/evidence/` for staged TDD evidence.
>
> Last reconciled: **2026-08-11**, after PR #153 merged the server-owned causal NPC↔NPC social mutation lifecycle.
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

latest product merge:               PR #153
latest product merge commit:        2a75e950e4e7f43f1321fc572c260b00f6d2bdf4
latest official release:            0.2.0+1.21.1
latest release commit:              e426f588efefa6aa48a6e536c4a998421bbda241
installed 0.2.0 candidate JAR SHA:   56293f86634b50b2def044429aac6f2cf0d197eb16ac1e60224708f7b3333aee

next product slice:                 bounded read-only MCA Personality + direct NPC-pair social snapshot / 0.3
then:                               dialogue/behavior integration + 0.3 convergence
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

0.3 Personality + NPC↔NPC Social Graph                 IN PROGRESS
NPC↔NPC social-graph persistence foundation            COMPLETE / PR #151
server-owned causal NPC↔NPC social mutation lifecycle COMPLETE / PR #153
bounded Personality + direct-pair social snapshot      NEXT
```

Installed boundaries remain explicit:

```text
VAI-M2-INST-005  NOT TESTED / AUTOMATED EVIDENCE ONLY
VAI-CONCUR-004   NOT TESTED / DEFERRED
```

Neither is represented as PASS.

PRs #127, #129, #131, #133, #135, #137, #139, #141, #143, #145, #147, #149, #151 and #153 are merged automated source capabilities **after** the installed `0.2.0` release. Their CI/candidate evidence is not installed `0.2.0` acceptance.

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

Canonical AI/state flow:

```text
Minecraft/server state
→ immutable bounded snapshot
→ current observations + lore + layered memory/disagreement/social context
→ deterministic authority-layer composition
→ provider/LLM proposal
→ server validation/revalidation
→ server-owned mutation
→ persistent authoritative/process evidence
```

Current prompt order remains:

```text
current server-observed facts
→ Operator Lore
→ Semantic Memory (including bounded fallibility/transformation/social-epistemic metadata)
→ live Semantic disagreement context
→ episodic / social history
→ structured provider/tool instructions
→ provider
```

The upcoming 0.3 pair snapshot must be inserted deliberately and bounded; it must not expose the whole NPC social graph or create a new truth layer.

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

Config version remains `2`. No 0.3 social-graph public configuration was added by PRs #151 or #153.

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

PR #153 already updated root `[Unreleased]`; this docs reconciliation does not duplicate or rewrite that product history.

---

# Known gaps and technical debt

1. `VAI-CONCUR-004` real two-graphical-client Operator Lore conflict presentation remains deferred.
2. `VAI-M2-INST-005` real second-player installed isolation remains untested; automated current-player/NPC-global/shared isolation exists.
3. Automatic contradiction production intentionally recognizes only a narrow standalone English `not` / Russian `не` polarity pattern; antonym/numeric/temporal/free-form opposition needs a separately justified bounded extension.
4. The current wording transform is intentionally narrow and deterministic; provider-authored open-ended paraphrase/generalization is not supported.
5. Settlement dissemination uses home-village membership and bounded deterministic routing only; physical proximity, travel gossip, alliances and social-topology routing remain future work.
6. The NPC↔NPC graph now has persistent causal mutation authority, but it is not yet exposed through a bounded dialogue/behavior snapshot and does not autonomously evolve.
7. New-edge capacity admission still scans the flat graph map; acceptable at current event-driven frequency, but index before high-frequency autonomous mutation.
8. NPC×player social epistemology currently uses trust only and only as a derived post-ranking annotation for player-origin BELIEF; NPC↔NPC state does not yet modify truth, ranking or source credibility.
9. Causal social frontier stores only the latest ordered cause per source NPC by design; retained Memory 2.0 audit is bounded process history, not an unbounded mutation ledger.
10. `PersistentChatMemory` remains a no-storage compatibility façade pending inherited AI call-surface refactoring.
11. Historical config fields for the removed raw conversation store remain deserializable compatibility baggage.
12. Historical Javadoc/deprecation warnings remain non-blocking.

---

# Next optimal delivery step

The next product slice is **bounded read-only MCA Personality + direct NPC-pair social snapshot / 0.3**.

The persistence and causal mutation boundaries are now proven. The next safe step is to make the current server-owned social/personality state observable to dialogue/behavior **without** giving the provider the whole graph and without creating another personality authority.

Target boundary:

```text
current MCA NPC entity
+ canonical MCA Personality tracked state
+ optional exact interaction counterpart NPC
+ direct source→target NpcSocialState only
→ server-owned immutable bounded snapshot
→ deterministic safe renderer / structured context
→ dialogue/behavior consumers read the snapshot
→ no persistence mutation from snapshot construction
```

Design requirements before production code:

- use existing MCA `Personality` as the only persistent personality authority;
- direct-pair lookup only; never enumerate/render the whole social graph;
- snapshot must be immutable, bounded, server-owned and captured before asynchronous provider work;
- absence of a pair edge renders neutral/empty state without creating a stored edge;
- A→B and B→A remain distinct;
- no provider-authored personality rewrite or social-score mutation;
- no FACT/BELIEF confidence/provenance/ranking mutation;
- no NPC×player `relationships.json` reinterpretation;
- no new persistence file/version/config unless a concrete need is proven;
- sanitize/render all text labels through existing prompt safety conventions;
- define the exact prompt/behavior placement explicitly so social/personality context cannot override current observed world facts;
- preserve all existing prompt result budgets; the snapshot should be a fixed-size server context layer, not an unbounded memory allocation path.

Recommended TDD order:

```text
snapshot design + exact data contract
→ RED: MCA Personality extraction from live server NPC
→ RED: direct pair lookup only / A→B != B→A
→ RED: neutral/missing pair creates no persistence write
→ RED: immutable bounded snapshot captured before async work
→ RED: safe deterministic rendering / no reserved-template injection
→ RED: prompt authority order and current FACT precedence
→ RED: no Semantic / relationships.json / graph mutation during snapshot construction
→ GameTest with two MCA NPCs + production startup/restart compatibility
→ exact-head CI / soak / release dry-run / review
```

After this read-only boundary is proven, the following slice can deliberately connect personality/social context to dialogue tone and selected behavior policies, then converge the 0.3 track.

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
