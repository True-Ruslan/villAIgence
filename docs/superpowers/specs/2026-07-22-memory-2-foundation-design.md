# Memory 2.0 Foundation Design

## Context

VillAIgence `0.1.x` has stable bounded dialogue history in `<world>/livingworld/memory.json`, factual world events in `events.json`, relationship state in `relationships.json`, and persistent voice identity in `voices.json`.

Roadmap `0.2 Memory 2.0` must move beyond raw chat history into layered memory with episodic events, provenance, confidence, importance, emotional weight, relationship reasons, later consolidation/forgetting, and bounded retrieval.

The first slice must not replace the proven dialogue path or allow LLM output to become authoritative world truth.

## Goal

Introduce a provider-independent persistent `MemoryEvent` domain and bounded per-NPC event store that can become the durable episodic/relationship-memory substrate for later Memory 2.0 slices.

## Architecture

### New storage

```text
<world>/livingworld/memory2.json
```

This file is separate from legacy/current `memory.json` so the existing conversation path remains stable while Memory 2.0 evolves.

Format version starts at `1`.

### MemoryEvent

`MemoryEvent` is immutable and contains:

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

Types in the first schema:

```text
DIALOGUE
OBSERVATION
ACTION
RELATIONSHIP_CHANGE
```

Provenance values:

```text
SYSTEM_OBSERVED
PLAYER_TOLD
NPC_TOLD
INFERRED
```

`SYSTEM_OBSERVED` means server-verified evidence. The other provenance values remain non-authoritative claims/beliefs and must never be promoted to authoritative world facts merely by storage.

Scores use bounded integer ranges:

```text
importance:       0..100
emotionalWeight: -100..100
confidence:       0..100
```

The record constructor normalizes summary/reasons, removes null/duplicate participants and reasons, and clamps bounded scores.

### MemoryEventStore

Responsibilities:

- world-local persistent storage;
- per-NPC isolation by `ownerNpcId`;
- bounded retention per NPC;
- deterministic newest-first retrieval;
- idempotent append by event UUID;
- fail-open load on malformed/unreadable auxiliary JSON;
- atomic temp-file + replace writes.

The store does not rank semantic relevance, call an LLM, mutate relationship state, or inject memories into prompts.

### Idempotency

Appending the same `MemoryEvent.id` twice for the same NPC must not create duplicate persistence. This preserves the project rule that retries/replays cannot multiply persistent side effects.

### Retrieval

First-slice API provides only bounded newest-first retrieval for one NPC.

Relevance scoring, recency/importance blending, consolidation and decay belong to later Memory 2.0 slices and will be layered above the store rather than embedded prematurely in persistence.

## Compatibility

Must remain unchanged:

```text
Minecraft 1.21.1
Java 21
mod id mca
package root net.conczin.mca
config/livingworld.json
existing <world>/livingworld/memory.json behavior
```

No migration of existing dialogue history occurs in this slice.

## Testing

Regression/unit coverage must prove:

1. event normalization and score clamping;
2. per-NPC persistence across store recreation;
3. newest-first bounded retention;
4. duplicate event IDs are idempotent;
5. malformed `memory2.json` fails open and is repaired by the next successful append;
6. one NPC cannot read another NPC's events.

## Non-goals

- automatic dialogue-to-memory extraction;
- LLM summarization;
- legacy `memory.json` migration;
- semantic facts store;
- forgetting/decay;
- relevance/vector search;
- prompt/context injection;
- NPC-to-NPC rumor propagation;
- relationship mutations.

## Success criteria

- `MemoryEvent` and `MemoryEventStore` compile without Minecraft runtime dependencies;
- focused tests cover all contracts above;
- `:common:test`, Fabric build/package smoke-check, and Fabric/NeoForge compatibility CI pass;
- existing `memory.json` code path is untouched;
- canonical project state documents Memory 2.0 foundation as partial implementation, not completion of milestone `0.2`.
