# Memory 2.0 Authoritative World-Event Ingestion Design

## Context

PR #31 added the provider-independent `MemoryEvent` / `MemoryEventStore` foundation. PR #33 added deterministic bounded retrieval/ranking.

The next safe step is to let **already authoritative server-observed events** enter Memory 2.0 without using an LLM to decide truth.

Current `WorldEventRecorder` runs on the Minecraft server thread only after a whitelisted NPC action completed successfully. It then persists a `WorldEvent` with `SYSTEM_OBSERVED` provenance.

That makes successful `WorldEvent.Type.NPC_ACTION` the first trustworthy production ingestion source for Memory 2.0.

## Goal

Convert successfully persisted server-observed NPC action `WorldEvent`s into idempotent per-NPC `MemoryEvent.Type.ACTION` entries owned by the acting NPC.

## Truth and lifecycle boundary

Required ordering:

```text
safe action succeeds
→ WorldEventRecorder creates SYSTEM_OBSERVED WorldEvent
→ WorldEventStore persists factual event successfully
→ Memory 2.0 adapter converts the same event
→ MemoryEventStore appends per-NPC memory
```

Memory 2.0 ingestion must never happen before the authoritative source event is accepted/persisted.

A Memory 2.0 write failure must not roll back or invalidate the already successful gameplay action or factual `WorldEvent`.

## WorldEventMemoryAdapter

Pure provider/Minecraft-independent conversion:

```text
WorldEvent.Type.NPC_ACTION
+ WorldEvent.Provenance.SYSTEM_OBSERVED
+ actorId present
→ MemoryEvent.Type.ACTION
```

Mapping:

```text
MemoryEvent.id                = WorldEvent.id
MemoryEvent.ownerNpcId        = WorldEvent.actorId
MemoryEvent.type              = ACTION
MemoryEvent.summary           = WorldEvent.description
MemoryEvent.participants      = actorId + subjectId when present, de-duplicated
MemoryEvent.provenance        = SYSTEM_OBSERVED
MemoryEvent.gameTime          = WorldEvent.gameTime
MemoryEvent.createdAtEpochMillis = supplied server timestamp
MemoryEvent.importance        = 60
MemoryEvent.emotionalWeight   = 0
MemoryEvent.confidence        = 100
MemoryEvent.relationshipReasons = []
```

Reusing the authoritative `WorldEvent.id` as the `MemoryEvent.id` makes redelivery naturally idempotent through the existing `MemoryEventStore` duplicate-ID rule.

The adapter returns no event when:

- source event is null;
- provenance is not `SYSTEM_OBSERVED`;
- type is unsupported;
- `actorId` is absent.

## Memory2WorldEventIngestor

A small persistence bridge accepts:

```text
worldRoot
WorldEvent
maxEventsPerNpc
createdAtEpochMillis
```

It converts through `WorldEventMemoryAdapter` and appends to `MemoryEventStore` only when conversion succeeds.

It contains no Minecraft entity access, provider access or prompt logic.

## Configuration

Add version-2-compatible optional fields:

```text
memory2Enabled = true
memory2MaxEventsPerNpc = 256
```

Rules:

- existing version-2 configs require no migration;
- missing fields receive Java defaults;
- `memory2MaxEventsPerNpc` normalizes to `1..512`;
- `memory2Enabled=false` disables this Memory 2.0 ingestion without disabling existing `events.json` factual memory.

The existing `eventMemoryEnabled` still controls whether `WorldEventRecorder` creates the authoritative source event at all.

## WorldEventRecorder integration

After `WorldEventStore.append(...)` succeeds:

```text
if memory2Enabled:
    attempt Memory2WorldEventIngestor.record(...)
```

Use a separate catch/log boundary for Memory 2.0 ingestion. A failed Memory 2.0 write must not be logged as if factual event persistence failed.

## Relationship-memory decision

Do **not** add relationship-reason ingestion in this slice.

Current relationship flow stores only a numeric proposed delta (`trust/respect/fear/affinity`) and does not carry a separately validated reason. Inventing a reason or treating LLM-generated explanation as authoritative would violate the truth boundary.

A later dedicated slice must define reason provenance before relationship reasons enter Memory 2.0.

## Non-goals

- prompt/context injection;
- dialogue extraction;
- relationship-reason ingestion;
- semantic/consolidated memory;
- LLM importance scoring;
- embeddings/vector search;
- recording failed/rejected actions;
- making nearby NPCs automatically remember another NPC's action.

## Testing

Tests must prove:

1. authoritative NPC action maps exactly to expected MemoryEvent fields;
2. non-authoritative/invalid source events are rejected;
3. actor becomes the memory owner and participants are normalized;
4. source WorldEvent UUID is reused;
5. duplicate ingestion of the same WorldEvent is idempotent in storage;
6. different source event IDs produce distinct memories;
7. Memory 2.0 per-NPC retention remains bounded;
8. configuration defaults/clamping and version-2 compatibility remain safe.

## Success criteria

- successful safe actions can produce authoritative actor-owned Memory 2.0 entries;
- failed/rejected actions cannot enter through this path;
- no LLM/provider decides event truth or importance;
- retries/redelivery cannot duplicate the same source event memory;
- existing `events.json` behavior remains intact;
- `memory.json` and prompt paths remain untouched;
- all required CI is green on exact final head.
