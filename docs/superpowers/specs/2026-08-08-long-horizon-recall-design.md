# Long-Horizon Recall Design

Date: 2026-08-08
Status: approved design; implementation gated by written-spec review and strict TDD
Base branch: `1.21.1`
Base commit: `b09924d7297775baabf577ca50dbcb65c22f0516`
Product track: Memory 2.0

## 1. Goal

Make Memory 2.0 useful across realistic Minecraft game-time distance, multiple sessions, restart and bounded capacity pressure without weakening truth precedence, provenance, player isolation, determinism or hard storage/prompt bounds.

The slice must prove two separate properties:

1. **Retention durability** — important old memory can remain persisted while weaker memory is forgotten deterministically.
2. **Recall durability** — an important memory that remains persisted can still enter the bounded prompt candidate set after enough newer eligible records exist to fill the current newest-first candidate window.

Long-horizon recall covers both Semantic Memory (`semantic-memory.json`) and episodic/social Memory 2.0 (`memory2.json`).

## 2. Existing contracts that remain authoritative

- current observations are authoritative for the current turn;
- snapshot order remains current observations → Operator Lore → Semantic Memory → episodic/social history;
- current relationship state precedes stale `RELATIONSHIP_CHANGE` / `RELATIONSHIP_CAUSE` history;
- `FACT` is only `SYSTEM_OBSERVED`;
- `PLAYER_TOLD`, `NPC_TOLD` and `INFERRED` remain BELIEF provenance classes;
- confidence, ranking, repetition and survival never promote BELIEF to FACT;
- player-scoped visibility is an eligibility boundary, not a ranking preference;
- foreign-player memory is excluded before bounded candidate selection;
- NPC-global memory remains eligible;
- shared memory remains eligible when the current player is among the related/participating entities;
- persistence, replay and retrieval remain deterministic;
- server state, never provider/model output, owns truth class, visibility, retention and gameplay authority.

Official installed-release evidence remains `0.2.0+1.21.1`. Long-horizon work is unreleased development until a later release candidate is explicitly accepted.

## 3. Non-goals

This slice does **not** authorize:

- increasing `memory2MaxEventsPerNpc` as the solution;
- increasing prompt candidate/result bounds without a separate observed RED;
- a new persistence/archive file;
- persistence format/version changes;
- config fields or config-version changes;
- legacy `memory.json` migration or dual reads;
- embeddings or vector databases;
- background/model summarization;
- extra provider calls for memory management;
- provider/model ownership of importance, provenance, retention or visibility;
- NPC-to-NPC knowledge transfer;
- rumors;
- generated psychological causes;
- relationship-authority changes;
- unrelated provider/transport/retry changes.

## 4. Core design: bounded dual-tier recall

Newest-only retrieval is insufficient: a record may survive persistence pressure yet remain unreachable once newer eligible records fill the candidate window.

Required flow:

```text
NPC-owned persisted memory
→ exact current-player/NPC-global eligibility
→ recent pool + durable pool
→ exact UUID de-duplication
→ hard candidate limit
→ existing ranker
→ existing hard prompt-result limit
```

For current context providers:

- `candidateLimit = 32` remains unchanged;
- `maxResults = 6` remains unchanged;
- recent quota = `24`;
- durable quota = `8`.

Generic selector rule:

- `candidateLimit <= 0` → empty;
- `candidateLimit == 1` → single newest eligible record;
- `candidateLimit >= 2` → `durableQuota = max(1, floor(candidateLimit / 4))`, `recentQuota = candidateLimit - durableQuota`.

Thus the normal budget is approximately 75% recent / 25% durable without increasing prompt size.

### 4.1 Recent ordering

Recent candidates are the newest eligible records ordered exactly by:

1. `gameTime` descending;
2. `createdAtEpochMillis` descending;
3. UUID string descending.

Foreign-player records are filtered before this ordering and consume zero recent slots.

### 4.2 Durable ordering

Durable candidates are selected only from eligible records not already in the recent pool.

Primary order:

1. effective retention/durability score descending;
2. domain-specific deterministic tie-breakers;
3. `gameTime` descending;
4. `createdAtEpochMillis` descending;
5. UUID string ascending for exact final ties.

For Semantic Memory, domain tie-breakers preserve the existing retention policy order where applicable:

- importance descending;
- confidence descending;
- source-event count descending.

For episodic/social memory, domain tie-breakers are:

- importance descending;
- confidence descending;
- absolute emotional weight descending;
- type durability tier descending;
- provenance durability tier descending.

The exact numeric coefficients used inside an episodic durability score are implementation details, but all ordering and monotonic contracts in section 7 must be locked by tests.

### 4.3 Combination

Recent and durable pools are combined by exact record UUID. The union must never exceed `candidateLimit` and must be deterministic for identical persisted data and identical current `gameTime`, regardless of input iteration order.

The existing `MemoryRetriever` and `SemanticMemoryRetriever` remain final rankers. Their ranking weights remain unchanged unless a dedicated RED proves the newly admitted durable candidate still fails solely because of ranking rather than candidate starvation.

## 5. Eligibility before both pools

`PlayerScopedMemoryEligibility` remains the visibility authority.

Semantic:

- owner NPC must match;
- empty `relatedEntities` → NPC-global, eligible;
- non-empty scope containing current player → eligible;
- foreign-only non-empty scope → ineligible;
- current player plus another entity → eligible.

Episodic/social:

- owner NPC must match;
- no external participant beyond owner NPC → NPC-global, eligible;
- external participants containing current player → eligible;
- foreign-only external participants → ineligible;
- current player plus another entity → eligible.

Required order is immutable:

```text
bounded store
→ eligibility filter
→ recent/durable selector
→ ranker
→ formatter
```

Selecting before filtering is forbidden because foreign-player memory could consume either recent or durable capacity.

## 6. Semantic persistence

`SemanticMemoryRetentionPolicy` remains the persistence authority.

Its existing behavior already provides hard per-NPC bounds, deterministic pressure selection, Minecraft `gameTime` decay, importance/confidence/provenance/source-evidence contributions and stable persistence order.

Do not redesign or reweight Semantic persistence unless an observed RED demonstrates a concrete contract violation.

Expected Semantic production work is limited to recall candidate selection if tests reproduce retained-but-starved memory.

## 7. Episodic/social persistence under pressure

`MemoryEventStore` currently performs chronological FIFO eviction. That cannot satisfy the approved contract when an important old event is removed only because newer weak dialogue exists.

After the required tests expose this failure, add a pure deterministic `MemoryEventRetentionPolicy` and use it from `MemoryEventStore.append(...)`.

### 7.1 Allowed durability inputs

Only persisted server-owned `MemoryEvent` fields may influence episodic durability:

- `importance`;
- `confidence`;
- absolute `emotionalWeight`;
- `type`;
- `provenance`;
- authoritative Minecraft `gameTime`.

Wall-clock time is forbidden.

### 7.2 Monotonic and type contracts

With all other inputs equal:

- increasing importance must never reduce durability;
- increasing confidence must never reduce durability;
- increasing absolute emotional weight must never reduce durability;
- `SYSTEM_OBSERVED` must not be less durable than otherwise-equal told/inferred provenance;
- type durability order is `RELATIONSHIP_CAUSE` > `RELATIONSHIP_CHANGE` > (`OBSERVATION` = `ACTION`) > `DIALOGUE`;
- advancing Minecraft `gameTime` must reduce effective retention for an old event deterministically;
- sufficiently old weak memory must remain evictable;
- no type is immortal;
- exact ties must resolve deterministically and independently of input order.

`RELATIONSHIP_CAUSE` durability protects only the server-observed causal-process linkage and embedded transition snapshot. It never promotes source dialogue prose to FACT.

### 7.3 Store behavior

Append flow after GREEN implementation:

```text
exact UUID duplicate check
→ add candidate
→ nowGameTime = max persisted gameTime
→ select bounded retained set
→ stable chronological persistence order
→ atomic save only if retained state changed
```

A weak append that is immediately rejected under pressure should not rewrite the file when the retained serialized state is unchanged.

No persistence migration is required.

## 8. Multi-session and restart semantics

Session boundaries have no independent truth/retention meaning. Behavior derives from persisted data plus authoritative `gameTime`.

Required properties:

- direct store reload preserves expected survivor IDs;
- the same query after reload selects identical candidate IDs;
- final ranking order is identical after reload;
- advancing only `gameTime` may alter recency/retention scores deterministically;
- wall-clock delay may not alter memory behavior;
- replay/reload does not duplicate records or mutate provenance;
- final production evidence includes real server startup/restart, not only reconstructed unit stores.

## 9. Truth precedence after long horizons

Long-horizon work changes only which eligible persisted memories can enter the bounded candidate set. Snapshot authority order does not change.

Required regressions:

1. stale PLAYER_TOLD BELIEF conflicts with current observed FACT → current observation remains authoritative;
2. stale relationship history conflicts with current relationship state → current relationship state remains authoritative;
3. Operator Lore conflicts with current observation → current observation remains authoritative;
4. repeated/corroborated BELIEF remains BELIEF;
5. surviving `RELATIONSHIP_CAUSE` never makes source DIALOGUE content FACT.

## 10. Strict TDD contract

Production changes for a slice are forbidden until a **tests-only** commit produces the expected RED for the intended reason.

Every RED record must capture:

- exact head SHA;
- failing test names;
- observed failure reason;
- confirmation that production code for that slice was unchanged.

Every GREEN transition must capture the exact implementation head and corresponding successful gates.

### RED 1 — Semantic retained-but-starved recall

Fixture:

- old high-durability Semantic memory;
- current-player or NPC-global visibility;
- more than 32 newer eligible records;
- old memory still physically persisted;
- newest-only context retrieval fails to expose the old memory;
- reload reproduces the failure.

Only then may Semantic long-horizon candidate selection change.

### RED 2 — Episodic pressure retention

Fixture:

- old important `RELATIONSHIP_CAUSE`, `RELATIONSHIP_CHANGE` or authoritative observation;
- newer weak ordinary dialogue;
- hard capacity pressure;
- current FIFO evicts the important old event.

Only then may episodic persistence retention change.

### RED 3 — Episodic retained-but-starved recall

After RED 2 is GREEN:

- important old episodic/social event is known to remain persisted;
- more than one newest-only candidate window of newer eligible history exists;
- current context retrieval fails to expose the important old event.

Only then may episodic candidate selection change.

### RED/Regression 4 — multi-session/restart determinism

Use multi-day Minecraft `gameTime`, repeated reloads and exact survivor/candidate/ranking IDs. Any failure is fixed minimally without wall-clock state.

### RED/Regression 5 — privacy under pressure

Use at least two NPCs and two players with:

- current-player-private memory;
- foreign-player-private memory;
- NPC-global memory;
- current-player-plus-other-entity shared memory;
- enough records to exercise both recent and durable pools.

Assert foreign-player data consumes zero candidate slots and pressure remains isolated per NPC.

### Regression 6 — authority precedence

Exercise stale long-horizon BELIEF/social history against newer current observations/current relationship state. If current code already passes, add/retain tests and make no production change.

### Regression 7 — deterministic long-running simulation

Generate hundreds of bounded records across multiple Minecraft days, without sleeps. Run the same fixture repeatedly, reopen stores and assert exact survivor/candidate/ranking IDs.

## 11. Expected component boundaries

Subject to observed RED evidence:

- keep `SemanticMemoryRetentionPolicy` for Semantic persistence;
- add pure `MemoryEventRetentionPolicy` for episodic/social pressure selection;
- add a small pure long-horizon candidate selector shared between domains only if the generic form remains clear and type-safe;
- otherwise prefer small explicit domain selectors over an opaque abstraction;
- add store access that exposes bounded NPC-owned records through an eligibility predicate before candidate selection;
- keep `SemanticMemoryContextProvider` and `Memory2ContextProvider` as prompt-facing retrieval boundaries;
- keep existing rankers as final rankers;
- keep `PlayerScopedMemoryEligibility` as visibility authority;
- keep `SnapshotContextPromptPolicy` as truth-layer authority.

Candidate selection itself is pure and must have no persistence side effect.

## 12. Failure and recovery behavior

- null/invalid queries continue to fail closed with empty results;
- malformed persistence continues through existing recovery policy;
- duplicate UUID replay remains idempotent;
- invalid persisted entries continue to be sanitized;
- provider failure cannot mutate retention state;
- rejected weak pressure candidates cannot corrupt/reorder another NPC's state;
- recovery, reload and restart cannot promote or rewrite provenance.

## 13. Final delivery evidence

Completion requires more than unit tests:

- pure retention/selector tests;
- store persistence/reload tests;
- context-provider long-horizon/privacy tests;
- prompt precedence regressions;
- common/mock-provider suite;
- relevant risk-selected server GameTests;
- Fabric build;
- NeoForge compatibility build;
- production startup/restart acceptance;
- selected persistence-recovery acceptance;
- repository security policy;
- constrained production soak/restart cycle;
- release dry-run with publication skipped;
- exact-head independent review;
- explicit distinction between candidate automation evidence and installed-release evidence.

## 14. Acceptance criteria

The slice is complete only when:

1. important old Semantic memory can survive storage pressure and remain recall-eligible after more than one candidate window of newer eligible memory;
2. important old episodic/social memory survives bounded pressure against weaker newer dialogue;
3. important persisted episodic/social memory remains recall-eligible after more than one candidate window of newer eligible history;
4. weak memory decays/evicts predictably;
5. no memory type is immortal;
6. multi-session/restart behavior preserves exact expected survivors and deterministic selection/ranking;
7. current-player/NPC-global/shared visibility remains exact;
8. foreign-player data consumes zero recent and durable slots;
9. current observed truth/current relationship state still outrank stale recall;
10. FACT/BELIEF provenance is unchanged;
11. persistence and prompt bounds remain hard;
12. no new persistence format, config version, external memory service or LLM memory-management call is introduced;
13. staged RED→GREEN evidence and final CI/production/release-dry-run evidence are documented before merge.

## 15. Follow-on work

Only after this slice is merged and canonical state/roadmap are reconciled:

1. NPC-to-NPC knowledge transfer via the existing `NPC_TOLD` BELIEF contract;
2. provenance-aware rumors.
