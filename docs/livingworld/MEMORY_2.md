# VillAIgence Memory 2.0

Memory 2.0 is the roadmap `0.2` transition from bounded raw dialogue history to layered, persistent NPC memory.

## Current foundation

The first Memory 2.0 slice introduces a provider-independent immutable `MemoryEvent` model and a per-NPC persistent event store:

```text
<world>/livingworld/memory2.json
```

The existing dialogue history remains unchanged at:

```text
<world>/livingworld/memory.json
```

`memory2.json` is deliberately separate so the new schema can evolve without silently changing the proven `0.1.x` conversation path.

## MemoryEvent

Each event belongs to one NPC and contains:

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

Initial event types:

```text
DIALOGUE
OBSERVATION
ACTION
RELATIONSHIP_CHANGE
```

Scores are bounded:

```text
importance:       0..100
emotionalWeight: -100..100
confidence:       0..100
```

## Provenance and truth boundary

Provenance is explicit:

```text
SYSTEM_OBSERVED
PLAYER_TOLD
NPC_TOLD
INFERRED
```

`SYSTEM_OBSERVED` is reserved for server-verified evidence.

`PLAYER_TOLD`, `NPC_TOLD`, and `INFERRED` represent claims or beliefs. Persisting them does **not** make them authoritative Minecraft facts.

The LLM is never allowed to promote a stored claim to authoritative world state merely because it exists in Memory 2.0.

## Persistence guarantees

The first store provides:

- per-NPC isolation;
- bounded retention;
- deterministic newest-first retrieval;
- idempotent append by event UUID;
- atomic temp-file + replace writes;
- fail-open recovery from malformed/unreadable auxiliary JSON.

Idempotency is important because retries/replays must not multiply persistent effects.

## Not integrated yet

This foundation does not yet:

- automatically convert chat into durable events;
- inject Memory 2.0 events into AI prompts;
- migrate legacy `memory.json` history;
- perform LLM summarization or consolidation;
- implement forgetting/decay;
- perform semantic/vector retrieval;
- propagate rumors between NPCs;
- mutate relationship state.

These remain later roadmap `0.2` slices.

## Planned next slice

The next step should add a bounded retrieval/ranking layer above persistence, combining explicit signals such as:

```text
relevance
recency
importance
confidence
```

After retrieval is deterministic and bounded, authoritative world events and explicit relationship reasons can be converted into Memory 2.0 events through controlled server-owned adapters. LLM-driven consolidation should come only after those boundaries are tested.
