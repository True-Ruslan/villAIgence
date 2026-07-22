# VillAIgence Memory 2.0

Memory 2.0 is roadmap `0.2`: the transition from bounded raw dialogue history to layered, persistent and selectively retrievable NPC memory.

## Storage boundary

Memory 2.0 uses a separate versioned auxiliary store:

```text
<world>/livingworld/memory2.json
```

The existing proven dialogue-history path remains unchanged:

```text
<world>/livingworld/memory.json
```

Keeping the files separate prevents the new architecture from silently changing `0.1.x` conversation behavior while Memory 2.0 evolves.

## MemoryEvent

Each immutable event belongs to one NPC and contains:

```text
id
ownerNpcId
type
summary
participants
provenance
gameTime
createdAtEpochMillis
importance
emotionalWeight
confidence
relationshipReasons
```

Initial types:

```text
DIALOGUE
OBSERVATION
ACTION
RELATIONSHIP_CHANGE
```

Bounded scores:

```text
importance:       0..100
emotionalWeight: -100..100
confidence:       0..100
```

## Provenance and truth boundary

Explicit provenance:

```text
SYSTEM_OBSERVED
PLAYER_TOLD
NPC_TOLD
INFERRED
```

`SYSTEM_OBSERVED` is reserved for server-verified evidence.

`PLAYER_TOLD`, `NPC_TOLD`, and `INFERRED` represent claims or beliefs. Persisting or highly ranking them does **not** make them authoritative Minecraft facts.

Retrieval never upgrades provenance. The LLM is never allowed to promote a remembered claim to authoritative world state merely because it appears in Memory 2.0 context.

## Persistence guarantees

`MemoryEventStore` provides:

- per-NPC isolation;
- bounded retention;
- deterministic newest-first storage access;
- idempotent append by event UUID;
- atomic temp-file + replace writes;
- fail-open recovery from malformed/unreadable auxiliary JSON.

Idempotent event IDs preserve the project rule that retries/replays must not multiply persistent side effects.

## Deterministic retrieval/ranking

The second Memory 2.0 slice adds `MemoryQuery`, `MemoryRetriever`, and inspectable `RankedMemory` results above persistence.

Retrieval flow:

```text
MemoryQuery
→ hard-bounded MemoryEventStore candidates for one NPC
→ deterministic component scores
→ deterministic ranking/tie-break
→ hard-bounded results
```

### Query bounds

`MemoryQuery` carries:

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

- NPC identity is required;
- participant/type filters are immutable and de-duplicated;
- game-time values are normalized to safe non-negative/minimum bounds;
- `candidateLimit` is clamped to `1..512`;
- `maxResults` is clamped to `1..candidateLimit`.

`candidateLimit` is a hard scale/cost boundary: ranking never silently expands the candidate set.

### Relevance

This slice intentionally uses only explicit structured signals.

For each specified dimension:

```text
participant overlap: match = 100, no match = 0
preferred event type: match = 100, no match = 0
```

When both dimensions are specified, relevance is their integer average.

When neither is specified, relevance is `100`, allowing a broad query to rank by the remaining signals without arbitrarily penalizing every memory.

No embeddings, vector search, free-text semantic similarity, or LLM relevance scoring are used.

### Recency

Recency is deterministic Minecraft game-time decay over the query horizon:

```text
age <= 0        → 100
age >= horizon  → 0
otherwise        → linear 100..0
```

Wall-clock creation time remains metadata and is used only as a late deterministic tie-break in this slice.

### Ranking weights

Fixed first-policy weights:

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

The weights are intentionally not configurable yet. Keeping one explicit policy makes behavior reproducible and reviewable before exposing tuning knobs.

### Inspectable results

Each `RankedMemory` contains:

```text
event
totalScore
relevanceScore
recencyScore
importanceScore
confidenceScore
```

This allows tests and future diagnostics to explain why a memory ranked highly without relying on hidden model reasoning.

### Stable tie-breaking

Equal memories are ordered by:

```text
1. total score descending
2. relevance descending
3. importance descending
4. recency descending
5. confidence descending
6. game time descending
7. real timestamp descending
8. event UUID string ascending
```

The final UUID tie-break makes ordering stable across JVM runs.

## Not integrated yet

Memory 2.0 still does not automatically:

- convert dialogue into durable events;
- convert authoritative world events into per-NPC memories;
- persist explicit relationship-change reasons from gameplay;
- inject ranked memories into AI prompts;
- migrate legacy `memory.json` history;
- perform LLM summarization/consolidation;
- implement forgetting/decay mutation;
- perform semantic/vector retrieval;
- propagate rumors between NPCs;
- mutate relationship state from retrieval.

## Next recommended slice

Add controlled server-owned adapters:

```text
authoritative WorldEvent
→ SYSTEM_OBSERVED MemoryEvent

explicit server-approved relationship reason
→ RELATIONSHIP_CHANGE MemoryEvent
```

Those adapters must preserve provenance, deterministic IDs/idempotency, and per-NPC ownership.

Only after authoritative inputs can enter Memory 2.0 safely should bounded ranked memories be injected into NPC context. Dialogue extraction, consolidation/summarization, forgetting/decay and migration remain later slices.
