# Semantic Memory Forgetting and Decay

## Purpose

Semantic Memory has a bounded per-NPC capacity. When that capacity is exceeded, VillAIgence must decide which knowledge to retain without relying on an LLM and without treating newest as automatically most valuable.

The implemented policy is **pressure-based forgetting**:

```text
under capacity
→ keep every valid semantic entry

above capacity
→ calculate deterministic retention ranks
→ keep the strongest bounded set
→ forget the lowest-ranked entries
```

Time alone never deletes an entry. There is no timer, background thread, scheduled cleanup, wall-clock TTL, or confidence mutation.

## Why pressure-based forgetting

Absolute expiration would be destructive and difficult to calibrate across servers with different uptime patterns. Lowering confidence over time would also be semantically incorrect: confidence represents evidential certainty, while forgetting is a storage-retention decision.

Pressure-based forgetting preserves all knowledge while space remains and becomes active only when new knowledge competes for a bounded slot.

## Retention durability

Each entry receives a static durability score from fields already stored in `semantic-memory.json`:

```text
importance contribution = importance × 4                 // 0..400
confidence contribution = confidence × 5 / 2             // 0..250

provenance contribution:
SYSTEM_OBSERVED = 200
PLAYER_TOLD     = 100
NPC_TOLD        = 75
INFERRED        = 25

source contribution = min(sourceEventIds count, 6) × 25   // 0..150
```

Maximum durability is 1000.

Independent source evidence increases retention durability but does not change confidence. Seven or more source IDs do not exceed the six-source retention cap; all source IDs remain persisted by consolidation.

## Decay

```text
DECAY_STEP_TICKS = 36,000
```

At 20 TPS this is approximately 30 minutes of active server runtime.

```text
ageTicks = max(0, nowGameTime - entry.gameTime)

effectiveRetentionScore =
    durability × DECAY_STEP_TICKS
    - ageTicks
```

One durability point offsets one decay step.

The score is transient. It is not persisted, not shown to the model, and not used as truth confidence.

## Time source

Decay uses authoritative Minecraft world game time already present on semantic entries.

Consequences:

- restart preserves the same age relationship;
- server-offline wall-clock time does not cause forgetting;
- provider latency and operating-system clock changes do not affect retention;
- a lower current game time cannot create negative age;
- no world-tick hook is required.

During append, the reference time is the greatest game time in the consolidated candidate set. This keeps out-of-order or replayed entries deterministic.

## Deterministic selection

Entries are ranked by:

```text
1. effective retention score descending
2. importance descending
3. confidence descending
4. sourceEventIds count descending
5. gameTime descending
6. createdAtEpochMillis descending
7. UUID ascending
```

After winner selection, retained entries are written in the existing persistence order:

```text
gameTime ascending
createdAtEpochMillis ascending
UUID ascending
```

The same logical candidate set therefore produces the same retained UUID set and stable JSON ordering regardless of input order.

## Consolidation interaction

The append pipeline is:

```text
exact UUID replay check
→ append candidate
→ semantic consolidation
→ retention selection
→ atomic save when state changed
```

Consolidation happens first. Therefore corroborating source events:

- remain one semantic entry;
- consume one retention slot;
- preserve every source UUID;
- gain bounded source-evidence durability;
- do not receive an artificial confidence increase.

## Rejected candidates

A weak new entry can lose against all entries already occupying the configured capacity.

When the retained list is identical to the pre-append list:

```text
candidate rejected
→ in-memory list restored to retained state
→ semantic-memory.json not rewritten
```

Exact UUID replay remains an earlier no-op and also does not rewrite the file.

## NPC isolation

Retention runs only inside the owner NPC's list.

```text
NPC A retention pressure
≠ NPC B retention pressure
```

Knowledge from one NPC cannot evict, reinforce, or merge with another NPC's semantic memory.

## Truth and provenance boundaries

Forgetting changes only which entries occupy bounded storage. It never changes entry meaning.

The policy does not mutate:

- `kind`;
- `provenance`;
- `statement`;
- `relatedEntities`;
- `importance`;
- `confidence`;
- `sourceEventIds`.

Existing invariants remain:

```text
FACT   → SYSTEM_OBSERVED only
BELIEF → PLAYER_TOLD / NPC_TOLD / INFERRED only
```

BELIEF is never promoted to FACT.

## Persistence and configuration

No persistent-format or configuration migration is required:

```text
semantic-memory.json version = 1
new JSON fields               = none
new config fields             = none
capacity control              = existing maxEntriesPerNpc
```

## Automated coverage

Tests cover:

- exact durability contributions;
- game-time decay;
- future timestamp age clamp;
- importance retention;
- confidence retention;
- provenance retention;
- source-evidence retention and six-source cap;
- input-order independence;
- UUID tie-breaking;
- under-capacity preservation;
- policy-based store eviction;
- consolidation before forgetting;
- rejected-append byte stability;
- persistence reload;
- NPC isolation;
- exact UUID replay no-op.

## Live-server validation scenario

A release containing this feature should be validated as follows:

1. Back up the five persistent world files.
2. Use a small semantic capacity for a controlled test NPC or generate enough entries to reach the existing limit.
3. Create an older high-importance, high-confidence authoritative FACT.
4. Create newer low-importance, low-confidence knowledge until retention pressure occurs.
5. Confirm the older strong FACT remains while the predicted weak entry is rejected or evicted.
6. Add independent corroborating source evidence for one consolidation key.
7. Confirm the consolidated entry contains every source UUID exactly once and receives the predicted source durability.
8. Repeat a rejected weak append and verify `semantic-memory.json` remains byte-identical.
9. Apply equivalent pressure to another NPC and confirm complete owner isolation.
10. Restart the server and compare hashes for:

```text
memory.json
memory2.json
semantic-memory.json
relationships.json
voices.json
```

11. Confirm retained semantic UUIDs and source IDs are unchanged after restart.
12. Confirm Chat, DIALOGUE ingestion, STT, TTS, Voice Chat, Opus, LinuxGSM monitor, UDP `24454`, and `25565` remain healthy.
13. Confirm there are no VillAIgence persistence errors or OutOfMemory failures.

## Non-goals

This feature does not implement:

- automatic expiration while under capacity;
- wall-clock or offline-time forgetting;
- confidence decay;
- contradiction resolution;
- automatic BELIEF producers;
- legacy `memory.json` migration;
- NPC-to-NPC knowledge propagation;
- configurable retention weights before live calibration.
