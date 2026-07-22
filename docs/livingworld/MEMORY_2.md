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

`MemoryQuery`, `MemoryRetriever`, and inspectable `RankedMemory` results provide a deterministic bounded layer above persistence.

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

This layer intentionally uses only explicit structured signals.

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

Wall-clock creation time remains metadata and is used only as a late deterministic tie-break in this layer.

### Ranking weights

Fixed current-policy weights:

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

This makes ranking explainable without hidden model reasoning.

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

## Authoritative safe-action ingestion

The first production ingestion path uses only an already server-authoritative event source.

Lifecycle:

```text
whitelisted NPC action succeeds on the server
→ WorldEventRecorder creates SYSTEM_OBSERVED WorldEvent
→ events.json persistence succeeds
→ same WorldEvent is converted to actor-owned MemoryEvent
→ memory2.json append
```

Memory 2.0 ingestion never happens before the source factual event is accepted.

If Memory 2.0 persistence fails, the already successful gameplay action and factual `events.json` record remain valid. The secondary memory failure is logged separately and fails soft.

### Mapping

For `WorldEvent.Type.NPC_ACTION` with `SYSTEM_OBSERVED` provenance and a valid actor:

```text
MemoryEvent.id                   = WorldEvent.id
MemoryEvent.ownerNpcId           = WorldEvent.actorId
MemoryEvent.type                 = ACTION
MemoryEvent.summary              = WorldEvent.description
MemoryEvent.participants         = actorId + subjectId when present
MemoryEvent.provenance           = SYSTEM_OBSERVED
MemoryEvent.gameTime             = WorldEvent.gameTime
MemoryEvent.createdAtEpochMillis = ingestion timestamp
MemoryEvent.importance           = 60
MemoryEvent.emotionalWeight      = 0
MemoryEvent.confidence           = 100
MemoryEvent.relationshipReasons  = []
```

Reusing the source `WorldEvent.id` gives natural idempotency: replaying/redelivering the same source event cannot create another copy in the same NPC memory bucket.

Only the acting NPC owns this memory in the current slice. Nearby NPCs do not automatically remember or learn the event; NPC-to-NPC knowledge propagation belongs to later roadmap work.

### Configuration

```json
{
  "memory2Enabled": true,
  "memory2MaxEventsPerNpc": 256
}
```

- `memory2Enabled=false` disables secondary Memory 2.0 ingestion while leaving existing factual `events.json` behavior controlled separately by `eventMemoryEnabled`;
- `memory2MaxEventsPerNpc` is normalized to `1..512`;
- config version remains `2`; existing version-2 configs require no migration.

## Relationship reasons are deliberately deferred

The current relationship path applies bounded numeric LLM-proposed deltas for `trust`, `respect`, `fear`, and `affinity`.

It does **not** currently carry a separately server-validated reason explaining why that change occurred.

Therefore VillAIgence does not invent a relationship reason or promote an LLM explanation to authoritative memory. A dedicated future contract must define reason provenance before `RELATIONSHIP_CHANGE` memories can safely store reasons.

## Not integrated yet

Memory 2.0 still does not automatically:

- convert dialogue into durable episodic events;
- persist validated relationship-change reasons;
- inject ranked memories into AI prompts;
- migrate legacy `memory.json` history;
- perform LLM summarization/consolidation;
- implement forgetting/decay mutation;
- perform semantic/vector retrieval;
- propagate rumors between NPCs;
- mutate relationship state from retrieval.

## Next recommended slice

With persistence, deterministic retrieval, and the first authoritative ingestion path in place, the next high-value slice is **bounded context integration**:

```text
immutable NPC snapshot
+ MemoryQuery
→ MemoryRetriever
→ small bounded ranked memory set
→ explicitly labeled memory/belief context for the NPC turn
```

The context formatter must preserve provenance labels so claims remain distinguishable from `SYSTEM_OBSERVED` facts.

Dialogue extraction, relationship-reason provenance, consolidation/summarization, forgetting/decay and migration should remain separate later slices.
