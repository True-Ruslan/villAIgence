# Long-Horizon Recall Design

Date: 2026-08-08
Status: approved design; implementation gated by written-spec review and strict TDD
Base branch: `1.21.1`
Base commit: `b09924d7297775baabf577ca50dbcb65c22f0516`
Product track: Memory 2.0

## 1. Goal

Make Memory 2.0 useful across realistic game-time distance, multiple sessions, restart and bounded capacity pressure without weakening current truth precedence, provenance, player isolation or hard storage/prompt bounds.

The feature must prove two distinct properties:

1. **Retention durability** — important old memory can remain persisted while weaker memory is forgotten deterministically.
2. **Recall durability** — an important memory that remains persisted can still enter the bounded prompt candidate set even after enough newer eligible records exist to fill the current newest-first candidate window.

Long-horizon recall covers both:

- Semantic Memory (`semantic-memory.json`), where persistence already has deterministic durability/decay retention;
- episodic/social Memory 2.0 (`memory2.json`), where persistence currently evicts oldest-first and therefore needs a durability-aware pressure policy if RED tests demonstrate the expected failure.

This slice does not introduce cross-NPC knowledge transfer, rumor propagation, model-driven memory management or unbounded history.

## 2. Existing architecture that remains authoritative

The implementation starts from these already-merged contracts:

- current observations are authoritative for the current turn;
- prompt layer order is current observations → Operator Lore → Semantic Memory → episodic/social history;
- current relationship state precedes stale `RELATIONSHIP_CHANGE` / `RELATIONSHIP_CAUSE` history;
- `FACT` is only `SYSTEM_OBSERVED`;
- `PLAYER_TOLD`, `NPC_TOLD` and `INFERRED` remain `BELIEF` provenance classes;
- confidence, ranking, repetition and long-term survival never promote BELIEF to FACT;
- player-scoped prompt visibility is an eligibility boundary, not a ranking preference;
- foreign-player memory is excluded before bounded candidate selection;
- NPC-global memory remains eligible;
- shared memory remains eligible when the current player is among the related/participating entities;
- persistence, replay and retrieval are deterministic;
- server state, never the provider/model, owns memory truth class, visibility, retention and gameplay authority.

Official installed-release evidence remains `0.2.0+1.21.1`. This design is unreleased development work and must not be described as installed acceptance.

## 3. Non-goals

The following are explicitly outside this slice:

- increasing `memory2MaxEventsPerNpc` as the solution;
- adding a new persistence file or archive tier;
- changing `memory2.json` or `semantic-memory.json` format version;
- adding config fields or changing config version;
- migrating or re-reading legacy `memory.json`;
- embeddings, vector databases or semantic search services;
- background summarization;
- an extra LLM/provider call for memory ranking, retention, consolidation or summarization;
- model-authored importance, provenance, retention or visibility decisions;
- NPC-to-NPC knowledge transfer;
- rumor propagation;
- psychological causal explanation generation;
- changing relationship mutation authority;
- changing provider request/transport/retry semantics unless a test exposes an unrelated blocker, in which case that blocker is handled separately rather than folded into this feature.

## 4. Core design choice: bounded dual-tier recall

A newest-only candidate window is insufficient for long-horizon recall: an old important memory may survive persistence pressure but never reach ranking once newer eligible records fill the candidate limit.

The bounded candidate set therefore has two deterministic pools:

```text
NPC-owned persisted memory
→ exact current-player/NPC-global eligibility
→ recent pool
+ durable pool
→ deterministic de-duplication
→ hard candidate limit
→ existing ranker
→ existing hard prompt-result limit
```

The context-provider candidate limit remains `32` and final prompt-result limit remains `6` unless a separate RED test proves an existing bound itself is defective. This design does not pre-authorize increasing either bound.

### 4.1 Candidate budget

For the normal `candidateLimit = 32`:

- recent quota = `24`;
- durable quota = `8`.

Generic selector behavior for other positive limits:

- `candidateLimit <= 1`: use the single newest eligible item;
- `candidateLimit >= 2`: `durableQuota = max(1, floor(candidateLimit / 4))` and `recentQuota = candidateLimit - durableQuota`.

This keeps approximately 75% of candidate capacity focused on recent context while reserving approximately 25% for older durable memory.

### 4.2 Recent pool

The recent pool contains the newest eligible records using the existing stable persistence-time ordering:

1. `gameTime` descending;
2. `createdAtEpochMillis` descending;
3. UUID string descending only where the existing newest-first comparator implies it.

No foreign-player record may consume a recent-pool slot.

### 4.3 Durable pool

The durable pool is selected from eligible records not already selected into the recent pool.

It is ordered by the record's deterministic effective retention/durability score at the current authoritative Minecraft `gameTime`, then deterministic existing domain tie-breakers.

The durable pool is not a second truth system. It only decides which persisted records are allowed to compete in the existing ranker.

No foreign-player record may consume a durable-pool slot.

### 4.4 De-duplication and bound

The recent and durable pools are combined by exact record UUID. The union must never exceed `candidateLimit`.

Selection is deterministic for identical persisted data and identical current `gameTime` regardless of input-list iteration order.

The existing `MemoryRetriever` and `SemanticMemoryRetriever` remain responsible for final ranking and `maxResults` limiting. Ranking weights must remain unchanged unless a dedicated RED test proves that the newly eligible durable candidate still cannot satisfy the long-horizon contract for a reason caused by ranking rather than candidate starvation.

## 5. Eligibility must happen before both pools

`PlayerScopedMemoryEligibility` remains the server-side visibility authority.

For Semantic Memory:

- owner NPC must match;
- empty related-entity scope is NPC-global and eligible;
- a non-empty scope is eligible when it contains the current player;
- a non-empty foreign-only scope is ineligible;
- a shared scope containing current player plus other entities remains eligible.

For episodic/social memory:

- owner NPC must match;
- an event with no external participant beyond the owner NPC is NPC-global and eligible;
- an event with external participants is eligible when the current player participates;
- a foreign-only external participant set is ineligible;
- a shared participant set containing the current player plus another entity remains eligible.

Required ordering:

```text
persisted bounded store
→ player/NPC eligibility filter
→ recent/durable candidate selection
→ ranking
→ prompt formatting
```

The inverse order is forbidden because it would allow foreign-player data to consume either recent or durable capacity.

## 6. Semantic Memory persistence

The existing `SemanticMemoryRetentionPolicy` remains the persistence authority for Semantic Memory.

Its current behavior already includes:

- hard per-NPC bounds;
- deterministic input-order-independent selection;
- authoritative Minecraft `gameTime` decay;
- importance and confidence;
- provenance contribution;
- corroborating source-event contribution;
- stable persistence order.

This slice does not redesign or reweight Semantic persistence unless a new RED test demonstrates a concrete contract violation.

The expected Semantic production change is therefore limited to long-horizon candidate selection if tests reproduce retained-but-unrecallable starvation.

## 7. Episodic/social persistence under pressure

`MemoryEventStore` currently retains newest events by chronological FIFO pressure. That behavior cannot satisfy the approved long-horizon contract when an important old event is displaced only because newer low-value dialogue exists.

If the required tests expose this RED, introduce a pure deterministic `MemoryEventRetentionPolicy` and make `MemoryEventStore.append(...)` use it.

### 7.1 Episodic durability inputs

The policy may use only persisted server-owned `MemoryEvent` fields:

- `importance`;
- `confidence`;
- absolute `emotionalWeight`;
- `type`;
- `provenance`;
- authoritative Minecraft `gameTime`.

Wall-clock age must not affect retention.

### 7.2 Required monotonic behavior

The exact numeric coefficients are an internal implementation detail, but the following relations are product contracts and must be locked by tests:

- increasing `importance` while all other inputs are equal must never reduce durability;
- increasing `confidence` while all other inputs are equal must never reduce durability;
- increasing absolute emotional weight while all other inputs are equal must never reduce durability;
- authoritative `SYSTEM_OBSERVED` provenance must not be less durable than otherwise-equal told/inferred provenance;
- at otherwise-equal values and age, type durability order is:
  - `RELATIONSHIP_CAUSE` highest;
  - `RELATIONSHIP_CHANGE` next;
  - `OBSERVATION` and `ACTION` middle tier;
  - ordinary `DIALOGUE` lowest tier;
- older memory loses effective retention score deterministically as Minecraft `gameTime` advances;
- sufficiently old weak memory remains evictable;
- no memory type is immortal;
- exact score ties resolve deterministically without input-order dependence.

`RELATIONSHIP_CAUSE` durability protects the server-observed process linkage and its embedded transition snapshot. It does not make linked dialogue prose factual.

### 7.3 Store behavior

On append:

```text
exact UUID duplicate check
→ add candidate
→ evaluate bounded retained set at max persisted gameTime
→ stable chronological persistence order
→ atomic save only when retained state changed
```

A newly appended weak event that is immediately rejected under pressure should not rewrite the persistent file if the retained state is byte-for-byte unchanged after deterministic serialization.

No store-format migration is required.

## 8. Multi-session and restart semantics

A session boundary has no special truth or retention meaning. Long-horizon behavior is derived from persisted state plus authoritative `gameTime`.

Required properties:

- the same retained entries survive direct store reload;
- the same eligible candidate IDs are selected after reload;
- ranking order for the same query is identical after reload;
- advancing `gameTime` may change recency/retention scores deterministically but not through wall-clock time;
- repeated reloads do not duplicate records or mutate provenance;
- production restart acceptance must verify that world-local stores reopen cleanly and preserve the expected survivors.

Unit/integration tests may use direct store reconstruction from the same file. Final delivery evidence must also include the existing real server startup/restart acceptance path.

## 9. Current-truth precedence after long horizons

Long-horizon recall changes only which eligible persisted memories can enter the bounded candidate set. It does not change snapshot authority layering.

Required conflict regressions:

1. stale PLAYER_TOLD BELIEF conflicts with a current observed FACT → current observation remains first and authoritative;
2. stale `RELATIONSHIP_CHANGE` or `RELATIONSHIP_CAUSE` conflicts with current relationship state → current relationship state remains first and authoritative;
3. old Operator Lore conflicts with current observation → current observation remains authoritative;
4. repeated/corroborated BELIEF remains BELIEF;
5. long-lived `RELATIONSHIP_CAUSE` does not promote its source DIALOGUE content to FACT.

## 10. TDD delivery contract

Production changes are forbidden until the corresponding test-only commit has produced an observed failure for the intended reason.

### RED 1 — Semantic retained-but-starved recall

Fixture:

- one old high-durability Semantic record scoped to the current player or NPC-global;
- enough newer eligible records to exceed the 32-candidate newest-first window;
- the old record remains physically persisted;
- current implementation fails to include it in the context candidate/result path where the long-horizon contract expects it;
- reload reproduces the same failure.

Only after this exact RED may Semantic long-horizon candidate selection be implemented.

### RED 2 — Episodic pressure retention

Fixture:

- an old important `RELATIONSHIP_CAUSE`, `RELATIONSHIP_CHANGE` or authoritative observation;
- newer low-value ordinary dialogue;
- hard capacity pressure;
- current FIFO implementation evicts the important old event.

Only after this exact RED may episodic persistence retention change.

### RED 3 — Episodic retained-but-starved recall

After episodic retention is GREEN:

- the important old event is known to remain persisted;
- enough newer eligible records fill the newest-first candidate window;
- current candidate retrieval fails to expose it.

Only after this RED may episodic long-horizon candidate selection change.

### RED 4 — multi-session/restart determinism

Construct multi-day Minecraft `gameTime` fixtures and reload the same persistent stores multiple times. Assert exact retained IDs, exact selected candidate IDs and exact ranking order.

Any failure must be fixed minimally without adding wall-clock state.

### RED 5 — privacy under pressure

Use at least:

- two NPCs;
- two players;
- current-player private memory;
- foreign-player private memory;
- NPC-global memory;
- shared current-player-plus-other-entity memory;
- enough pressure to exercise both recent and durable pools.

Assert that pressure and recall remain independent per NPC/player scope and that foreign-player data consumes zero candidate slots.

### RED 6 — authority precedence

Exercise stale long-horizon BELIEF/social history against newer current observations/current relationship state and prove current truth remains structurally first.

If existing code already passes, preserve those tests as regression evidence and make no production change.

### RED 7 — deterministic long-running simulation

Generate hundreds of bounded events over multiple Minecraft days without sleeps or wall-clock dependencies.

Run the same logical fixture more than once and assert exact survivor/candidate/ranking IDs. Reopen persistent files and assert the same result.

## 11. Test levels and final delivery evidence

The final feature cannot be called complete from unit tests alone.

Required evidence:

- pure retention/candidate-selector unit tests;
- store persistence/reload tests;
- context-provider privacy and long-horizon tests;
- prompt authority-order regressions;
- existing common/mock-provider suite;
- relevant server GameTests selected by the repository risk selector;
- Fabric build;
- NeoForge compile/build compatibility;
- production startup/restart acceptance;
- selected persistence-recovery acceptance;
- repository security policy;
- constrained production soak/restart cycle;
- release dry-run with publication skipped;
- exact-head independent review;
- explicit distinction between automated candidate evidence and installed-release evidence.

Every RED commit must be recorded with:

- exact head SHA;
- failing test names;
- failure reason;
- confirmation that production code for that slice had not yet changed.

Every GREEN transition must record the exact implementation head and corresponding successful gate.

## 12. Expected component boundaries

The implementation should preserve small, testable units. Expected shape, subject to RED evidence:

- existing `SemanticMemoryRetentionPolicy`: unchanged Semantic persistence scoring;
- new pure `MemoryEventRetentionPolicy`: episodic/social persistence scoring and bounded survivor selection;
- a pure bounded long-horizon candidate-selection unit shared by Semantic and episodic domains where practical, parameterized by stable recent order and domain durability order;
- store APIs that can expose bounded persisted records for an NPC with an eligibility predicate before candidate selection;
- `SemanticMemoryContextProvider` and `Memory2ContextProvider` remain the prompt-facing composition boundaries;
- existing rankers remain final candidate rankers;
- `PlayerScopedMemoryEligibility` remains visibility authority;
- `SnapshotContextPromptPolicy` remains truth-layer authority.

Do not create a generic abstraction if it makes the domain rules harder to inspect. Small duplicated selectors are preferable to an opaque framework if a shared generic helper is not clear and type-safe.

## 13. Failure handling

- invalid/null query inputs continue to fail closed with empty results;
- malformed persistence continues through existing recovery policy;
- duplicate UUID replay remains idempotent;
- unknown/unsupported persisted entries continue to be sanitized by the stores;
- no model/provider failure may change retention state;
- candidate selection is pure and has no persistence side effect;
- rejected weak pressure candidates must not corrupt or reorder unrelated NPC state.

## 14. Acceptance criteria

Long-horizon recall is complete only when all of the following are true:

1. an important old Semantic memory can survive storage pressure and still be recall-eligible after more than one candidate window of newer eligible memory;
2. an important old episodic/social event can survive bounded pressure against weaker newer dialogue;
3. important persisted episodic/social memory can still enter the bounded recall candidate set after more than one candidate window of newer eligible history;
4. weak memory predictably decays/evicts under pressure;
5. no memory type becomes immortal;
6. multiple sessions and restart preserve exact expected survivors and deterministic selection/ranking;
7. current-player/NPC-global/shared visibility stays exact and foreign-player data cannot consume either recent or durable slots;
8. current observed truth and current relationship state continue to outrank stale recollection;
9. FACT/BELIEF provenance remains unchanged;
10. persistence and prompt bounds remain hard;
11. no new persistence format, config version, external memory service or LLM memory-management call is introduced;
12. all staged RED→GREEN evidence and final CI/production/release-dry-run evidence are documented before merge.

## 15. Follow-on work

Only after this slice is merged and canonical state/roadmap are reconciled should development advance to:

1. NPC-to-NPC knowledge transfer through the existing `NPC_TOLD` BELIEF contract;
2. provenance-aware rumor propagation.
