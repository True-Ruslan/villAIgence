# FACT > BELIEF Retrieval Precedence — Design

Status: approved design, implementation not started.

Date: 2026-08-08

Base branch: `1.21.1`

Base commit: `853739f3e580ebb85538e2fb7febd4fa4b5ddfcf`

## Goal

Make the existing VillAIgence authority rule executable and regression-protected:

```text
current server-observed truth
> remembered BELIEF
> Operator Lore / stale recollection
```

The implementation must guarantee this precedence before provider invocation rather than relying only on natural-language prompt instructions.

This slice also closes the current player-isolation weakness in episodic and semantic retrieval: memory associated only with another player must be ineligible for the current player's prompt, not merely receive a lower relevance score.

## Current problem

The architecture already keeps current world facts, Operator Lore, episodic Memory 2.0 and Semantic Memory conceptually separate, but two implementation details weaken the contract.

### 1. Player-related retrieval currently ranks mismatches instead of rejecting them

`SemanticMemoryRetriever` currently loads a bounded recent candidate set for the NPC and gives a related-entity mismatch relevance `0`. The mismatched entry still participates in ranking and may remain in the result set if its importance/confidence/recency is high enough.

`MemoryRetriever` behaves similarly for participant mismatches: a mismatched event loses participant relevance but remains eligible.

This is insufficient for privacy and exact player isolation.

### 2. Snapshot memory is duplicated into generic context lines

`LivingWorldContextSnapshot` already has separate immutable fields for:

- `worldFacts`;
- `operatorAuthoredContext`;
- `memoryContext`;
- `semanticMemoryContext`.

However `LivingWorldContextCapture.capture(...)` invokes `PlayerModule.apply(...)`, and `PlayerModule` currently invokes `MemoryModule.apply(...)`. This inserts episodic and semantic prompt sections into generic `contextLines` before the same snapshot separately stores the dedicated memory fields.

The result is a hidden ordering dependency and a duplicated context path. The authoritative precedence is therefore not represented by one deterministic composition boundary.

## Design principles

1. **Server state is authority.** Current `worldFacts` remain the highest-authority factual context for the turn.
2. **BELIEF is never promoted.** Confidence, score, repetition or recency never upgrades BELIEF to FACT.
3. **Isolation is eligibility, not ranking.** Memory scoped only to another player must be excluded before candidate limiting and ranking.
4. **NPC-global memory remains usable.** Entries/events with no player-specific association remain eligible for the NPC.
5. **Historical evidence is retained.** Conflicting or stale memories are not deleted or rewritten solely because current state disagrees with them.
6. **Prompt composition is deterministic.** All snapshot context layers are rendered exactly once in a fixed authority order before provider invocation.
7. **The provider does not decide precedence.** The LLM receives an already-structured context; it does not choose which source is authoritative.
8. **No semantic contradiction detector in this slice.** No LLM, embeddings, vector search, fuzzy matching or generated conflict-resolution prose is introduced.
9. **No persistence migration.** Existing Memory 2.0 / Semantic Memory stores and format versions remain unchanged.
10. **Classic compatibility is preserved where practical.** The legacy/classic `PlayerModule` path may continue using `MemoryModule`; snapshot capture must stop using that side-effect path.

## Architecture

Target snapshot flow:

```text
Minecraft/server state
→ capture immutable stable NPC/player context
→ capture CURRENT OBSERVED WORLD FACTS
→ capture Operator Lore
→ load exact-player-or-NPC-global Semantic Memory
→ load exact-player-or-NPC-global episodic Memory 2.0
→ compose fixed layered snapshot prompt
→ append structured-response / command instructions
→ provider
```

Required prompt authority order:

```text
stable personality / NPC context
→ CURRENT OBSERVED WORLD FACTS
→ Operator Lore
→ Semantic Memory
→ Episodic / relationship causal history
→ structured-response / command instructions
```

The order expresses authority, not deletion. Lower-authority layers may contain conflicting historical statements, but the prompt must label them as remembered/background data and current observations remain authoritative for the turn.

## Player-scope eligibility

### Semantic Memory

For `SemanticMemoryContextProvider.load(worldRoot, npcId, playerId, gameTime)`:

An entry is eligible when all of the following hold:

1. `entry.ownerNpcId == npcId` through the existing store boundary; and
2. either:
   - `entry.relatedEntities` is empty; or
   - `playerId != null` and `entry.relatedEntities` contains `playerId`.

An entry whose `relatedEntities` is non-empty and contains only other players/entities is ineligible for this current-player prompt.

Eligibility filtering occurs **before** candidate limiting and before ranking.

This slice does not attempt semantic interpretation of arbitrary related entity UUIDs. The current player UUID is the only player-scope key used by this retrieval boundary.

### Episodic Memory 2.0

For `Memory2ContextProvider.load(worldRoot, npcId, playerId, gameTime)`:

An event is eligible when all of the following hold:

1. `event.ownerNpcId == npcId` through the existing store boundary; and
2. either:
   - the event has no participant other than its NPC owner / no external participant scope; or
   - `playerId != null` and `event.participants` contains `playerId`.

Events associated with another player but not the current player are ineligible.

Eligibility filtering occurs **before** candidate limiting and before ranking.

For events whose participant list contains the NPC owner plus the current player, current-player eligibility is satisfied. Events with no player-specific participant remain NPC-global and may be retrieved.

### Exact filtering before limits

A high-scoring foreign-player record must never consume one of the 32 candidate slots and must never displace an eligible current-player or NPC-global record.

The invariant is:

```text
owner isolation
→ player/global eligibility
→ candidate limit
→ ranking
→ result limit
```

not:

```text
owner isolation
→ candidate limit
→ rank player mismatch lower
→ result limit
```

## Retrieval API shape

The implementation should preserve the current public provider APIs where possible:

```text
Memory2ContextProvider.load(...)
SemanticMemoryContextProvider.load(...)
```

A focused store/query helper may be introduced if necessary to support predicate filtering before limiting. The preferred design is a reusable exact scoped retrieval seam rather than loading an unbounded store into the provider.

The read path must remain bounded and deterministic.

No store schema change is required.

## Snapshot capture and prompt composition

### Remove snapshot-side memory duplication

`LivingWorldContextCapture.capture(...)` must no longer cause `PlayerModule` to inject `MemoryModule` output into generic `contextLines`.

Preferred source-compatible shape:

- keep existing `PlayerModule.apply(...)` behavior for legacy/classic callers;
- add a narrow player-context method or overload used by snapshot capture that applies player advancement/context information **without** `MemoryModule`;
- snapshot capture continues loading `memoryContext` and `semanticMemoryContext` explicitly into their dedicated immutable fields.

No memory load should occur twice for one snapshot turn.

### Single layered prompt policy

Extend or replace the current `SnapshotContextPromptPolicy` so that one deterministic policy composes the snapshot's authority-bearing layers.

It must render:

1. current observed facts;
2. Operator Lore;
3. Semantic Memory;
4. episodic/relationship memory.

The existing stable `contextLines` remain before these authority layers because they represent personality, traits, village/environment/player descriptive context rather than the separate Memory 2.0 authority classes.

Structured-response instructions remain after all context layers.

### Current observed relationship state

Current relationship state is already appended to `worldFacts` during snapshot capture. Therefore it naturally outranks stale `RELATIONSHIP_CHANGE` or `RELATIONSHIP_CAUSE` history.

The implementation must not delete stale relationship history. It must only make the authority order explicit.

### Causal relationship history

`RELATIONSHIP_CAUSE` remains historical process evidence. This slice does not introduce a new causal-history prompt provider if the current episodic path does not already surface it.

If it is retrieved through the current Memory 2.0 path, it remains below current relationship state and must not promote linked dialogue prose to FACT.

No psychological interpretation is added.

## Conflicts

### Current FACT vs BELIEF

Example:

```text
current world fact: Observed weather: rain.
remembered PLAYER_TOLD BELIEF: "It is sunny."
```

Both may remain present, but the prompt structure must make the current observation authoritative and the BELIEF explicitly non-authoritative.

No BELIEF mutation or deletion occurs.

### Current relationship state vs stale relationship history

Example:

```text
current relationship: trust=5
historical relationship memory: trust previously increased to 40
```

Historical memory remains inspectable as history. Current relationship state is authoritative for the turn.

### Operator Lore vs current observation

Operator Lore remains background server-authored context. It does not override a current observed fact.

### BELIEF vs BELIEF

Conflicting BELIEFs remain representable simultaneously. The system does not choose one as truth in this slice.

High confidence or multiple corroborating BELIEF sources do not create FACT authority.

## Failure semantics

- If Memory 2.0 retrieval fails, snapshot prompt remains usable without episodic memory.
- If Semantic Memory retrieval fails, snapshot prompt remains usable without semantic memory.
- If Operator Lore loading fails, current observations and other layers remain usable.
- A retrieval error must not weaken current world facts or action validation.
- Filtering must fail closed with respect to foreign-player memory: uncertainty about scope must not broaden visibility.
- No provider failure changes persistent truth classes or source provenance.

## TDD acceptance plan

Production behavior must not change before the intended RED is observed.

### RED 1 — semantic exact-player eligibility

Tests must prove the current implementation fails these contracts:

- a high-importance/high-confidence foreign-player semantic entry is excluded completely;
- NPC-global semantic entries remain eligible;
- exact current-player entries remain eligible;
- foreign entries are filtered before candidate limit, so 32 foreign records cannot starve one eligible record;
- result ordering among eligible records remains deterministic;
- no FACT/BELIEF authority mutation occurs.

### RED 2 — episodic exact-player eligibility

Tests must prove:

- a high-score foreign-player event is excluded completely;
- NPC-global event remains eligible;
- current-player event remains eligible;
- filtering occurs before candidate limit;
- `RELATIONSHIP_CHANGE` / `RELATIONSHIP_CAUSE` for another player cannot leak into current-player context;
- existing NPC-owner isolation remains intact.

### RED 3 — snapshot duplication / authority composition

Tests must prove the current snapshot path violates the intended single-source composition boundary, then require:

- snapshot capture player-context path does not invoke `MemoryModule`;
- `memoryContext` and `semanticMemoryContext` are present only through dedicated snapshot fields;
- prompt authority order is exactly:
  - observed facts;
  - Operator Lore;
  - Semantic Memory;
  - episodic memory;
  - structured response instructions;
- each durable memory section appears exactly once;
- current relationship factual summary appears above stale relationship history.

### RED 4 — conflict regression package

End-to-end/policy tests must cover:

- current world FACT conflicts with `PLAYER_TOLD` BELIEF → both visible, current FACT structurally authoritative;
- current relationship state conflicts with stale relationship history → current state above history;
- Operator Lore conflicts with current observation → current observation above lore;
- two conflicting BELIEFs remain BELIEF and neither becomes FACT;
- causal relationship history does not make dialogue prose authoritative;
- mixed NPC/player data cannot leak another player's private memory;
- provider request body contains the fixed layered order.

### Regression gates

On the final exact PR head run all selected mandatory gates:

- common/unit/mock-provider tests;
- repository security policy;
- risk selector and required server GameTests;
- Fabric + NeoForge builds;
- production startup/restart acceptance;
- selected/current persistence recovery;
- package smoke;
- production soak;
- GitHub Release dry-run with publication skipped;
- independent changed-file review with no unresolved P0/P1/P2 findings.

Root `CHANGELOG.md` `[Unreleased]` must be updated in the runtime PR because retrieval/privacy/prompt behavior changes are product/runtime guarantees.

## Compatibility and persistence

No new file is introduced.

No format/version changes are planned for:

- `memory2.json`;
- `semantic-memory.json`;
- `relationships.json`;
- `operator-lore.json`.

No backfill, dual reader, `memory.json` migration, cleanup job or destructive rewrite is introduced.

Existing stored memories remain readable. This feature changes only which records are eligible for a current-player prompt and how existing snapshot layers are composed.

## Security and privacy boundary

This feature strengthens confidentiality between player-scoped NPC memories.

Provider input must never include a Memory 2.0 / Semantic Memory record that is scoped only to a different player.

The client does not select memory owner, player scope, rank, precedence or truth class.

The provider cannot override retrieval eligibility, source provenance or current-world authority.

## Non-goals

- semantic contradiction detection;
- deleting or rewriting stale memories;
- automatic BELIEF correction;
- confidence-based FACT promotion;
- embeddings or vector DB;
- LLM ranking or LLM conflict resolution;
- second provider/extractor call;
- new causal psychological explanations;
- NPC-to-NPC knowledge transfer;
- rumor propagation;
- new persistence schema;
- pre-0.2 legacy migration;
- changing action authority or relationship mutation policy.

## Exit criterion

The slice is complete when VillAIgence can prove, with deterministic tests and exact-head CI evidence, that:

1. current observed server state is rendered above and remains authoritative over remembered BELIEF, Operator Lore and stale relationship history;
2. foreign-player episodic and semantic memories are excluded before candidate limiting and ranking;
3. eligible current-player and NPC-global memories remain bounded and deterministic;
4. snapshot memory is loaded and rendered exactly once through dedicated immutable layers;
5. conflicting BELIEFs remain non-authoritative rather than silently promoted, rewritten or deleted;
6. no new persistence format, provider authority or migration path is introduced.
