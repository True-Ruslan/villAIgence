# Memory 2.0 Deterministic Retrieval Design

## Context

PR #31 established the provider-independent `MemoryEvent` domain and bounded per-NPC `MemoryEventStore` in `<world>/livingworld/memory2.json`.

The next roadmap requirement is bounded retrieval by relevance, recency and importance while preserving provenance/confidence and avoiding an early dependency on embeddings or an LLM.

## Goal

Add a deterministic pure-Java retrieval/ranking layer above `MemoryEventStore` that selects a hard-bounded candidate set and ranks it by explicit inspectable signals.

## Components

### MemoryQuery

Immutable query fields:

```text
npcId
participants
preferredTypes
nowGameTime
recencyHorizonTicks
candidateLimit
maxResults
```

Rules:

- `npcId` is required;
- participant/type sets are normalized, de-duplicated and immutable;
- `nowGameTime >= 0`;
- `recencyHorizonTicks >= 1`;
- `candidateLimit` is clamped to `1..512`;
- `maxResults` is clamped to `1..candidateLimit`.

`candidateLimit` is a hard cost/scale bound. Ranking must never silently expand beyond it.

### MemoryRetriever

Retrieval flow:

```text
MemoryQuery
→ MemoryEventStore.getRecent(npcId, candidateLimit)
→ deterministic component scores
→ deterministic sort/tie-break
→ maxResults
```

No provider, Minecraft entity or mutable world access is allowed in this layer.

### Ranking signals

All component scores use integer `0..100`.

#### Relevance

Query relevance uses only explicit structured signals in this slice:

- participant overlap;
- preferred event type.

For each specified relevance dimension:

```text
match → 100
no match → 0
```

If both participant and type dimensions are specified, relevance is their integer average.

If neither is specified, relevance is `100` so a broad query does not arbitrarily penalize every candidate.

#### Recency

Linear game-time decay over `recencyHorizonTicks`:

```text
age <= 0        → 100
age >= horizon  → 0
otherwise        → linear 100..0
```

This is deterministic and Minecraft-time based; real wall-clock time is metadata only in this slice.

#### Importance and confidence

Use bounded values already stored in `MemoryEvent` directly.

### Total score

Fixed first-slice weights:

```text
relevance  40%
importance 25%
recency    20%
confidence 15%
```

Integer total:

```text
(relevance*40 + importance*25 + recency*20 + confidence*15) / 100
```

Weights are intentionally not configurable yet. Premature configuration would make retrieval behavior harder to reason about before the policy is validated.

### RankedMemory

Return an immutable diagnostic-friendly value containing:

```text
event
totalScore
relevanceScore
recencyScore
importanceScore
confidenceScore
```

This makes ranking inspectable and testable without exposing private prompt/reasoning content.

## Tie-breaking

Sort by:

1. total score descending;
2. relevance descending;
3. importance descending;
4. recency descending;
5. confidence descending;
6. event game time descending;
7. event real timestamp descending;
8. event UUID string ascending.

The final UUID tie-break ensures stable ordering across JVM runs.

## Security/truth boundary

Retrieval ranks memories; it does not validate truth or mutate world state.

A high-ranked `PLAYER_TOLD`, `NPC_TOLD` or `INFERRED` memory remains a claim/belief. Ranking never upgrades provenance to `SYSTEM_OBSERVED`.

## Non-goals

- embeddings/vector DB;
- LLM relevance scoring;
- lexical free-text similarity;
- prompt injection;
- dialogue extraction;
- event/relationship adapters;
- forgetting/decay mutation;
- consolidation/summarization;
- semantic fact merging.

## Testing

Tests must prove:

1. query normalization/clamping;
2. participant/type relevance calculation;
3. recency linear decay boundaries;
4. explicit weighted score ordering;
5. hard candidate/result bounds;
6. deterministic tie-breaking;
7. per-NPC isolation through store-backed retrieval;
8. retrieval does not mutate stored events.

## Success criteria

- retrieval is pure/deterministic apart from reading the bounded store;
- no Minecraft runtime or provider dependency is introduced;
- tests run under `:common:test`;
- Fabric package and Fabric/NeoForge CI remain green;
- no change to legacy `memory.json` or current AI prompt path;
- the next slice can safely build server-owned adapters on top of this retrieval contract.
