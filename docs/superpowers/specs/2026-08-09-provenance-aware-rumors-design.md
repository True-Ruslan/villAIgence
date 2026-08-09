# Provenance-Aware Rumors — Exact Multi-Hop Lineage Design

Date: 2026-08-09
Status: approved interactive design; implementation gated by written-spec review and strict TDD
Base branch: `1.21.1`
Base commit: `e543a7f6220de0bc6f7d4bc4d6cd2b31cfbf36ad`
Product track: Memory 2.0

## 1. Goal

Extend the source-backed NPC-to-NPC knowledge-transfer primitive with exact, bounded, inspectable multi-hop provenance.

The first rumor slice deliberately preserves claim text exactly. It proves where a transferred BELIEF came from across multiple NPC hops without yet adding paraphrase, distortion, uncertainty, contradiction resolution, social trust weighting or autonomous conversation scheduling.

Required capability:

```text
NPC A owns persisted Semantic knowledge
→ A tells B
→ B receives BELIEF / NPC_TOLD
→ B tells C using an exact retained provenance-bearing source
→ C receives BELIEF / NPC_TOLD
→ C's direct evidence contains a bounded immutable ancestry snapshot
```

For a chain:

```text
A → B → C → D
```

D still owns only a non-authoritative `BELIEF / NPC_TOLD`. The provenance payload answers where that BELIEF travelled; it never upgrades the BELIEF to FACT.

## 2. Existing authority laws remain unchanged

This slice preserves the current VillAIgence architecture laws:

- Minecraft/server state is truth; the provider/LLM is never authority;
- `FACT` requires `SYSTEM_OBSERVED` evidence;
- `BELIEF` may use `PLAYER_TOLD`, `NPC_TOLD` or `INFERRED` provenance only;
- confidence, repetition, corroboration, hop count, retention and ranking never promote BELIEF to FACT;
- current server-observed facts outrank Operator Lore, Semantic Memory and episodic/social recollection;
- player-scoped prompt visibility is an eligibility boundary before candidate allocation;
- NPC-global and valid shared scopes remain eligible under existing rules;
- no provider or client chooses memory owner, source UUID, truth class, provenance, visibility, retention score or gameplay authority;
- persistence, replay and selection are deterministic;
- long-horizon candidate/result bounds stay finite and unchanged;
- no memory class becomes immortal;
- source/candidate automation evidence remains distinct from installed-release evidence.

Official installed-release evidence remains `0.2.0+1.21.1`. This slice is unreleased development until a later immutable release candidate is explicitly accepted.

## 3. Clean-cutover policy

VillAIgence is pre-1.0 and currently used only on the private test server. This design therefore does not add migration/backfill/dual-read machinery for experimental NPC-transfer history.

The clean-cutover rules are:

- existing ordinary Memory 2.0 events remain readable where the additive field shape permits it for free;
- existing Semantic BELIEFs are not rewritten or migrated;
- old `NPC_TOLD` transfer evidence without structured lineage is not reconstructed from summary text;
- old transfer evidence without lineage cannot be used to continue a multi-hop rumor;
- no importer, checkpoint ledger, background backfill or compatibility bridge is added;
- rebuilding/clearing experimental test-world LivingWorld data is the supported operational path when a clean state is desired.

`memory2.json` remains format version `1`. The new payload is additive and nullable; bumping the file format solely to force a clean cutover would incorrectly route otherwise valid old files through corruption recovery.

## 4. Non-goals

This slice does not add:

- claim paraphrase or LLM rewriting;
- semantic distortion;
- uncertainty decay;
- contradiction resolution;
- social trust/reputation weighting;
- relationship-dependent transfer confidence;
- a `RUMOR` Semantic kind;
- a `RUMOR` MemoryEvent provenance enum;
- autonomous NPC conversation scheduling;
- visible NPC-to-NPC chat bubbles, client UI or voice;
- a second provider/LLM call;
- settlement-wide broadcast or omniscient knowledge distribution;
- a provenance DAG stored inside Semantic Memory;
- a separate `rumors.json` store;
- new public configuration;
- changed long-horizon quotas/ranker weights/retention coefficients;
- unrelated provider, transport, relationship or gameplay refactoring.

## 5. Chosen architecture: evidence-centric immutable lineage snapshot

Provenance belongs to the exact server-owned NPC-to-NPC transfer evidence, not to `SemanticMemoryEntry` itself.

`SemanticMemoryEntry.sourceEventIds` continues to mean direct evidence sources for the current NPC's BELIEF. It does not become a recursive ancestry list and never stores Semantic-entry IDs.

The new structured payload is:

```text
KnowledgeTransferProvenance
  origin
  hops[]
```

Every successful new NPC-to-NPC transfer event contains exactly one provenance lineage.

A listener Semantic BELIEF continues to point only at its direct transfer evidence:

```text
BELIEF / NPC_TOLD
sourceEventIds = [direct transfer evidence UUID, ...direct corroborating evidence UUIDs]
```

Each direct evidence event independently carries one exact bounded ancestry path.

This keeps Semantic Memory simple while making ancestry inspectable and restart-safe even if older bounded events are later forgotten.

## 6. Persisted payload model

`MemoryEvent` gains one nullable structured field:

```java
KnowledgeTransferProvenance knowledgeTransferProvenance
```

The immutable logical model is:

```text
KnowledgeTransferProvenance(
    Origin origin,
    List<Hop> hops
)

Origin(
    UUID originNpcId,
    UUID originSemanticEntryId,
    SemanticMemoryEntry.Kind originKind,
    MemoryEvent.Provenance originProvenance,
    String statement,
    List<UUID> relatedEntities
)

Hop(
    UUID speakerNpcId,
    UUID listenerNpcId,
    UUID speakerSemanticEntryId,
    UUID evidenceEventId,
    long gameTime
)
```

### 6.1 Why `speakerSemanticEntryId` is stored in every hop

The deterministic transfer-evidence identity depends on:

```text
speakerNpcId
listenerNpcId
speakerSemanticEntryId
authoritativeGameTime
```

A hop without the exact speaker Semantic-entry UUID would be inspectable but not independently verifiable against the deterministic evidence-ID contract.

### 6.2 Canonical origin snapshot

The origin is a snapshot of the exact Semantic knowledge held by the first NPC before NPC-to-NPC propagation began.

It contains:

- origin NPC UUID;
- exact origin Semantic-entry UUID;
- origin Semantic kind;
- origin provenance;
- canonical bounded statement;
- canonical Semantic subject scope.

The origin snapshot records provenance history. It does not grant downstream truth authority.

## 7. Semantic classification remains BELIEF / NPC_TOLD

This slice does not introduce a new truth class.

At any hop depth:

```text
kind       = BELIEF
provenance = NPC_TOLD
```

Examples:

```text
A owns FACT / SYSTEM_OBSERVED
A → B → C
C still owns BELIEF / NPC_TOLD

A owns BELIEF / PLAYER_TOLD
A → B → C
C still owns BELIEF / NPC_TOLD
```

Origin metadata may state that A originally held a FACT or a PLAYER_TOLD/INFERRED BELIEF, but downstream listeners do not inherit that authority class.

## 8. Public lifecycle input remains authority-minimal

The existing server-owned transfer lifecycle input remains:

```text
worldRoot
speakerNpcId
listenerNpcId
speakerSemanticEntryId
authoritativeGameTime
memory2CapacityPerNpc
semanticCapacityPerNpc
```

The caller does not provide:

- claim text;
- origin metadata;
- lineage/hops;
- direct source-event choice;
- truth kind/provenance;
- related-entity scope;
- confidence/importance;
- evidence UUID;
- listener Semantic-entry UUID.

The lifecycle must derive all provenance from exact persisted server-owned state.

## 9. First-hop derivation

A new provenance lineage begins only when the speaker Semantic entry is not itself an `NPC_TOLD` BELIEF.

Allowed first-hop sources are exactly:

```text
FACT / SYSTEM_OBSERVED
BELIEF / PLAYER_TOLD
BELIEF / INFERRED
```

For first transfer `A → B`:

```text
origin = exact canonical snapshot of A's selected Semantic entry
hops   = [canonical A→B hop]
```

B still receives only `BELIEF / NPC_TOLD` with fixed server-owned transfer policy values.

An `NPC_TOLD` BELIEF never becomes a fake new origin. It must inherit a valid existing lineage or fail closed.

## 10. Multi-hop derivation for NPC_TOLD speaker BELIEF

When speaker B owns an `NPC_TOLD` BELIEF and attempts `B → C`, the lifecycle must derive ancestry from one exact retained direct evidence source already referenced by B's Semantic entry.

Required flow:

```text
exact B Semantic BELIEF
→ enumerate exact sourceEventIds
→ resolve retained events owned by B
→ keep only structurally valid NPC_TOLD transfer evidence
→ require valid KnowledgeTransferProvenance
→ deterministic branch selection
→ inherit origin + hops
→ append canonical B→C hop
```

If B has no retained valid direct provenance-bearing source, the result is:

```text
PROVENANCE_UNAVAILABLE
```

The lifecycle must not parse summaries, infer ancestry from UUID names, fabricate a new origin, or follow unrelated events.

## 11. Canonical branch selection after Semantic consolidation

A consolidated listener BELIEF may reference several direct evidence sources:

```text
sourceEventIds = [A→B, X→B, Y→B]
```

One transfer uses exactly one lineage. This slice does not merge provenance branches into a DAG.

Eligible branch candidates must:

- have event UUID contained in the exact speaker Semantic entry's `sourceEventIds`;
- be retained in `MemoryEventStore` under the speaker owner;
- be canonical `DIALOGUE / NPC_TOLD` transfer evidence;
- carry a valid structured lineage;
- end at the current speaker;
- match the exact current Semantic statement and canonical subject scope.

Selection order is deterministic:

```text
event.gameTime DESC
→ event.id ASC
```

Insertion order must not affect selection.

If the newest referenced source was evicted or structurally invalid but another retained valid source exists, the next valid source may be selected.

Branch selection is completed before evaluating the proposed listener. Once the highest-priority valid direct branch is selected, cycle or hop-limit rejection does **not** fall through to a lower-priority branch merely because that alternative would allow the requested listener. This prevents listener identity from steering ancestry selection.

## 12. New deterministic evidence identity — v2 clean cutover

Structured provenance changes the authority meaning of transfer evidence. New rumor-capable transfer events therefore use a new deterministic namespace:

```text
npc-knowledge-transfer-v2
```

The UUID input remains:

```text
namespace
listenerNpcId
speakerNpcId
speakerSemanticEntryId
authoritativeGameTime
```

The provenance payload is deliberately not part of the UUID input. The exact source tuple and game time determine event identity; canonical lineage is then validated as part of event equality/authority.

Consequences:

- exact retry on the new contract produces the same v2 event UUID;
- old v1 events are not silently upgraded or treated as equivalent v2 evidence;
- old v1 evidence without structured lineage is historical direct evidence only and cannot continue a multi-hop rumor;
- no migration is provided.

## 13. Canonical new transfer evidence

The existing listener-owned transfer event shape remains:

```text
ownerNpcId              = listenerNpcId
type                    = DIALOGUE
participants            = [listenerNpcId, speakerNpcId]
provenance              = NPC_TOLD
gameTime                = authoritativeGameTime
createdAtEpochMillis    = 0
importance              = 50
emotionalWeight         = 0
confidence              = 50
relationshipReasons     = []
dialogue                = null
relationshipTransition  = null
relationshipCause       = null
summary                 = "NPC told: " + normalizedStatement
knowledgeTransferProvenance = canonical non-null payload
```

`dialogue == null` remains intentional because player-oriented `DialogueExchange` must not be reused for NPC↔NPC transfer evidence.

Summary is display/evidence text only. Authority never depends on parsing it.

## 14. Exact statement preservation

This rumor slice does not transform claim text.

All lineage continuation requires:

```text
current speaker Semantic normalized statement
== provenance.origin.statement
```

The statement uses the current Semantic normalization and hard bound of 240 Unicode code points.

No LLM paraphrase, synonym replacement, grammatical rewriting or hidden canonical-content transformation occurs.

If a candidate lineage describes a different statement, it is not a valid source for continuation.

## 15. Exact Semantic scope preservation

The origin snapshot stores canonical `relatedEntities` as a sorted unique UUID set.

Every continuation requires:

```text
canonical(current source.relatedEntities)
== provenance.origin.relatedEntities
```

The new listener BELIEF preserves the same scope.

The speaker NPC is not injected into Semantic `relatedEntities` merely because it appears in provenance.

This prevents rumor propagation from silently expanding player-private or shared knowledge into NPC-global knowledge.

## 16. Fixed transfer confidence and importance

Each hop continues to use fixed server-owned values:

```text
importance = 50
confidence = 50
```

The values do not depend on:

- source confidence/importance;
- hop count;
- number of repetitions;
- social relationship/trust;
- speaker personality;
- provider confidence;
- origin truth class.

Hop depth therefore does not implement uncertainty decay. That is a later design.

## 17. Bounded lineage

Hard limit:

```text
MAX_PROVENANCE_HOPS = 8
```

Eight hops are permitted. An attempted ninth hop returns:

```text
PROVENANCE_LIMIT_REACHED
```

and writes no new transfer evidence or listener BELIEF.

The payload is intentionally a simple path, not recursive nested history or an unbounded graph.

## 18. Cycle prevention

Every persisted lineage must represent a simple acyclic NPC path.

For a valid lineage, the ordered NPC path is:

```text
hop[0].speaker
hop[0].listener == hop[1].speaker
...
lastHop.listener
```

No NPC UUID may appear twice in that path.

Before appending a new hop, if the proposed listener already appears anywhere in the current path, the lifecycle returns:

```text
PROVENANCE_CYCLE
```

Examples rejected:

```text
A → A
A → B → A
A → B → C → A
A → B → C → B
```

Independent corroborating paths remain allowed, for example:

```text
A → B → D
X → C → D
```

D may consolidate those as multiple direct source events, while each direct evidence event carries its own acyclic lineage.

When a selected valid lineage simultaneously cannot append because of both cycle and hop count, cycle detection is evaluated first and returns `PROVENANCE_CYCLE`; otherwise the hop-limit check follows. The lifecycle does not retry branch selection with a lower-priority lineage after either rejection.

## 19. Full pure lineage integrity validation

A pure `KnowledgeTransferProvenancePolicy` validates persisted provenance without repairing it.

### 19.1 Origin validation

The origin must satisfy:

- all required IDs are non-null;
- kind/provenance are non-null;
- statement is non-blank and exactly canonical under the current Semantic normalization/bound;
- related-entity scope is canonical sorted unique UUIDs;
- `FACT` requires exactly `SYSTEM_OBSERVED`;
- `BELIEF` origin requires exactly `PLAYER_TOLD` or `INFERRED`;
- `BELIEF / NPC_TOLD` is invalid as a new origin because an NPC_TOLD source must inherit a prior structured lineage rather than reset ancestry.

### 19.2 Hop validation

Each hop must satisfy:

- all UUIDs are non-null;
- speaker and listener differ;
- game time is non-negative;
- evidence UUID exactly matches deterministic v2 identity for speaker/listener/speakerSemanticEntryId/gameTime.

Path requirements:

- `1 <= hops.size <= 8`;
- first hop speaker equals `origin.originNpcId`;
- first hop speaker Semantic-entry UUID equals `origin.originSemanticEntryId`;
- each previous listener equals the next speaker;
- no NPC UUID repeats;
- the last hop identifies the exact transfer evidence being validated.

### 19.3 Direct-source requirement

For continuation, the last hop must additionally satisfy:

```text
lastHop.listenerNpcId == current speakerNpcId
lastHop.evidenceEventId == exact retained direct evidence.id
```

Older ancestry events do not need to remain physically retained. Their immutable structural snapshot remains inside the current direct evidence.

The direct source evidence itself must exist now.

## 20. Corruption and malformed optional payload behavior

Authority validation does not live solely in record constructors and does not silently normalize malformed persisted ancestry into a valid chain.

If an otherwise base-valid historical `MemoryEvent` has null/malformed/inconsistent provenance payload:

- it is not reconstructed from summary text;
- it is not repaired into authority;
- it cannot be selected as a source for the next rumor hop;
- candidate selection skips it;
- if no other valid direct provenance source exists for an `NPC_TOLD` BELIEF, the lifecycle returns `PROVENANCE_UNAVAILABLE`.

A mismatch discovered in a newly constructed/persisted current transfer after write is an authority conflict and returns `REJECTED`; no derived BELIEF is created.

## 21. Canonical evidence equality boundary

`NpcKnowledgeTransferPolicy.validEvidence(...)` remains fail-closed by comparing the exact persisted event to the server's canonical expected event.

For new v2 transfer evidence, canonical equality includes the entire exact provenance payload.

Mutation of any of the following invalidates the event as authority:

- event UUID;
- owner;
- participants/order;
- type/provenance;
- game time;
- fixed importance/emotion/confidence;
- summary;
- origin fields;
- origin statement/scope;
- hop IDs/order;
- hop speaker/listener;
- hop speaker Semantic-entry ID;
- hop evidence ID;
- hop game time.

Summary parsing is never used to recover authority.

## 22. Semantic consolidation remains unchanged

`SemanticMemoryConsolidator` continues to use the existing logical key:

```text
owner
kind
provenance
canonical statement
canonical relatedEntities
```

It continues to union direct `sourceEventIds`.

It does not merge `KnowledgeTransferProvenance` payloads and does not build a provenance DAG inside Semantic Memory.

After listener BELIEF admission, lifecycle success still means a retained compatible listener BELIEF exists whose direct `sourceEventIds` contains the exact new transfer evidence UUID.

## 23. Retention and long-horizon behavior remain bounded

No provenance record is pinned or immortal.

- transfer evidence remains subject to existing `MemoryEventRetentionPolicy`;
- listener BELIEF remains subject to existing `SemanticMemoryRetentionPolicy`;
- long-horizon recent/durable quotas remain unchanged;
- ranker weights remain unchanged.

Important distinction:

```text
older ancestry event evicted
→ current direct evidence may still retain a self-contained lineage snapshot

current direct source evidence evicted
→ that exact branch cannot be propagated further

speaker BELIEF still retained
→ speaker may still recall it personally, but cannot prove that missing branch for further provenance-aware transfer
```

This is deliberate fail-closed epistemic behavior.

## 24. Cross-store lifecycle ordering

The authoritative write order remains evidence-before-derived-knowledge:

```text
1. exact speaker Semantic lookup
2. authoritative exact source reread/snapshot validation
3. derive new origin OR select/inherit one valid retained lineage
4. validate statement/scope/path
5. for inherited lineage: evaluate proposed-listener cycle, then hop limit
6. compute deterministic v2 evidence UUID
7. append canonical new hop into provenance payload
8. construct canonical listener-owned NPC_TOLD evidence
9. append MemoryEventStore
10. exact evidence reread by listener owner + UUID
11. validate full canonical evidence and provenance payload
12. run generic SemanticBeliefAdmissionPolicy
13. append listener BELIEF through ControlledSemanticMemoryIngestor
14. exact compatible retained listener BELIEF reread containing new evidence UUID
15. return explicit result
```

For a first hop, the existing request-level `speaker != listener` validation is the cycle boundary; the inherited-lineage cycle check applies once a prior path exists.

No distributed transaction is introduced between `memory2.json` and `semantic-memory.json`.

If evidence is retained but Semantic BELIEF is not retained under pressure, evidence is not rolled back because the historical transfer occurred.

## 25. Result surface

Existing statuses remain:

```text
ADMITTED
REJECTED
SOURCE_NOT_RETAINED
BELIEF_NOT_RETAINED
```

New exact provenance outcomes are:

```text
PROVENANCE_UNAVAILABLE
PROVENANCE_LIMIT_REACHED
PROVENANCE_CYCLE
```

Semantics:

- `PROVENANCE_UNAVAILABLE`: the speaker source is `BELIEF / NPC_TOLD`, but no retained direct evidence referenced by that Semantic entry provides a valid compatible lineage;
- `PROVENANCE_LIMIT_REACHED`: a selected valid lineage exists but the new hop would exceed 8;
- `PROVENANCE_CYCLE`: the proposed listener already appears in the selected valid path;
- `REJECTED`: generic request/authority/current-write mismatch, including invalid source ownership or newly persisted canonical-evidence mismatch;
- `SOURCE_NOT_RETAINED`: newly appended evidence did not survive current bounded event pressure;
- `BELIEF_NOT_RETAINED`: evidence survived but the derived listener Semantic BELIEF did not survive Semantic pressure.

Expected fail-closed outcomes do not escape as uncaught exceptions and do not create partial BELIEF writes.

## 26. Deterministic replay

Exact retry of the same v2 transfer tuple:

```text
speaker
listener
speakerSemanticEntryId
gameTime
```

produces the same evidence UUID and, given the same valid retained source state, the same provenance payload.

If an `NPC_TOLD` BELIEF has multiple branches, canonical branch selection by `gameTime DESC → evidence UUID ASC` must produce the same selected lineage after restart and independent of source insertion order.

The new payload contains no wall-clock-dependent field. Existing synthetic transfer evidence continues to use `createdAtEpochMillis = 0`.

## 27. Prompt authority and privacy preservation

Provenance history does not alter prompt truth precedence or visibility.

Required preservation:

- current observed FACT remains structurally authoritative over conflicting transferred BELIEF;
- repeated or 8-hop NPC_TOLD rumor remains BELIEF;
- origin FACT metadata does not make downstream BELIEF authoritative;
- foreign-player Semantic scope remains excluded before candidate allocation;
- player-private/shared/global `relatedEntities` scope remains exactly preserved;
- provenance speaker/listener UUIDs do not expand Semantic subject scope;
- raw NPC↔NPC evidence with `dialogue == null` remains outside player `user/assistant` Working Memory reconstruction.

## 28. TDD acceptance sequence

Runtime implementation must follow strict observed RED → minimal GREEN.

### 28.1 Structured payload RED

Tests-only compile RED requires:

- `KnowledgeTransferProvenance`;
- `Origin` and `Hop`;
- canonical origin normalization;
- max-eight bound;
- deterministic v2 hop evidence identity;
- path continuity;
- cycle rejection.

No production payload implementation exists before this RED is observed.

### 28.2 First-hop behavioral RED

Separate cases:

```text
FACT / SYSTEM_OBSERVED A → B
BELIEF / PLAYER_TOLD A → B
BELIEF / INFERRED A → B
```

Prove:

- exact origin snapshot;
- one canonical hop;
- statement/scope preservation;
- listener remains `BELIEF / NPC_TOLD`;
- direct listener Semantic source is the new evidence UUID only.

### 28.3 Multi-hop behavioral RED

For `A → B → C`, prove before GREEN that continuation is missing.

GREEN must establish:

- B's `NPC_TOLD` BELIEF does not become a fake new origin;
- A→B ancestry is inherited;
- B→C is appended as hop 2;
- C remains BELIEF/NPC_TOLD;
- C Semantic source references direct B→C evidence.

### 28.4 Consolidation branch-selection regressions

Create a speaker BELIEF with several direct valid branches.

Prove:

- only IDs actually referenced by the speaker Semantic entry are considered;
- only retained structurally valid provenance evidence is eligible;
- newest `gameTime`, then UUID ascending, selects the branch;
- insertion-order permutations choose the same branch;
- restart chooses the same branch;
- eviction of the highest-priority branch falls through deterministically to the next valid retained branch;
- after a branch is selected, listener-specific cycle/hop-limit rejection does not cause fallback to a lower-priority branch.

### 28.5 Provenance-unavailable matrix

Cover:

- NPC_TOLD BELIEF with no retained source evidence;
- historical v1 evidence without structured provenance;
- malformed provenance payload;
- origin `BELIEF / NPC_TOLD` reset attempt;
- wrong event owner;
- direct evidence not referenced by current Semantic source;
- last hop not ending at current speaker;
- statement mismatch;
- Semantic subject-scope mismatch.

No new evidence or listener BELIEF may appear.

### 28.6 Cycle matrix

Reject with `PROVENANCE_CYCLE`:

```text
A → A
A → B → A
A → B → C → A
A → B → C → B
```

Independent corroborating acyclic branches remain legal.

### 28.7 Hop-limit matrix

Eight hops are accepted. Attempted ninth hop returns `PROVENANCE_LIMIT_REACHED` with no partial writes.

### 28.8 Integrity mutation matrix

Mutate one persisted field at a time:

- origin NPC;
- origin Semantic-entry UUID;
- origin kind;
- origin provenance;
- origin statement;
- origin scope;
- hop speaker;
- hop listener;
- hop speaker Semantic-entry UUID;
- hop evidence UUID;
- hop game time;
- hop order;
- missing hop;
- extra hop.

Every mutation fails closed.

### 28.9 Exact-text and scope preservation

Across an eight-hop path prove:

- normalized statement remains exact;
- canonical `relatedEntities` set remains exact;
- player-private knowledge never becomes global;
- speaker UUIDs never enter Semantic subject scope merely due to provenance.

### 28.10 Authority regression

Required conflict scenario:

```text
rumor chain says: "bridge intact"
current server observation says: "bridge destroyed"
```

Prompt framing must preserve current `SYSTEM_OBSERVED` truth authority. Hop count/repetition never upgrades rumor to FACT.

### 28.11 Pressure and restart

Prove:

- older ancestry source events may be evicted while a later direct evidence retains self-contained ancestry snapshot;
- fresh-root reload preserves exact provenance payload;
- exact replay after reload remains idempotent;
- current direct evidence eviction blocks further propagation of that branch;
- already-retained speaker BELIEF may still participate in personal recall;
- transfer evidence and BELIEF remain evictable under stronger pressure.

### 28.12 Deterministic multi-NPC simulation

Use fixed UUIDs/game times and no sleep/wall-clock assertions.

Minimum scenario characteristics:

- at least 10 NPCs;
- multiple independent lineages;
- at least one corroborated Semantic BELIEF;
- cycle attempts;
- an eight-hop valid chain and rejected ninth hop;
- more than 200 unrelated Semantic/episodic records;
- forward and reversed insertion order;
- multiple fresh-root reloads.

Compare exact:

```text
retained persisted IDs
provenance payloads
selected branch identities
Semantic direct source IDs
prompt contexts
```

## 29. Expected minimal production surface

Expected runtime changes are limited to focused Memory 2.0 components:

```text
MemoryEvent
KnowledgeTransferProvenance
KnowledgeTransferProvenancePolicy
NpcToldDialogueAdapter
NpcKnowledgeTransferPolicy
NpcKnowledgeTransferLifecycle
NpcKnowledgeTransferResult
```

Small pure/store helper changes are allowed only if an observed RED proves they are required.

The slice must not broaden into provider, UI, scheduler, relationship, voice or settlement behavior.

## 30. Delivery gates

After all focused RED→GREEN stages, the exact final source head must pass the complete selected delivery matrix:

```text
common + deterministic mock-provider tests
→ risk-selected server GameTests
→ Fabric + NeoForge builds
→ production startup/restart acceptance
→ selected persistence recovery
→ repository security policy
→ constrained production soak
→ release dry-run
→ accepted/package JAR identity where selected
→ independent base→head review
```

Unknown CI classification remains fail-closed.

The runtime PR must update root `CHANGELOG.md` under `[Unreleased]`. After merge, `docs/PROJECT_STATE.md` and `docs/ROADMAP.md` are reconciled in a separate documentation-only delivery boundary unless the implementation plan explicitly chooses an equivalent already-established docs workflow.

## 31. Exit criterion

This slice is complete when VillAIgence can propagate an exact non-authoritative statement across a bounded acyclic NPC chain while every new listener:

- remains `BELIEF / NPC_TOLD`;
- has only direct evidence in Semantic `sourceEventIds`;
- has direct evidence containing one immutable exact ancestry path;
- preserves origin statement and Semantic subject scope;
- cannot continue through missing or invalid direct provenance;
- cannot create cycles or exceed eight hops;
- behaves deterministically across replay, consolidation, pressure and restart;
- remains subordinate to current server-observed truth;
- remains bounded and non-omniscient.

Uncertainty, contradictions, trust weighting, bounded distortion and settlement-scale autonomous rumor spread are explicitly later slices.