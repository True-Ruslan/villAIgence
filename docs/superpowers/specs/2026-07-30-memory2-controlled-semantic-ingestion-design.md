# Memory 2.0 Controlled Semantic Ingestion Design

## Status

Approved autonomous implementation direction after the live-validated `0.1.11+1.21.1` Working Memory checkpoint.

## Goal

Activate the existing typed Semantic Memory layer through explicit, provenance-preserving producers without allowing arbitrary dialogue or LLM output to become authoritative knowledge.

```text
server-observed MemoryEvent
→ controlled eligibility gate
→ FACT / SYSTEM_OBSERVED
→ semantic-memory.json

explicit sourced claim
→ controlled BELIEF API
→ PLAYER_TOLD | NPC_TOLD | INFERRED
→ semantic-memory.json
```

## Non-goals

This slice does not:

- parse arbitrary player/NPC dialogue into semantic statements;
- ask an LLM to extract, summarize, classify, or validate truth;
- promote `DIALOGUE` events into semantic memory automatically;
- merge logically similar entries from different source events;
- implement forgetting/decay;
- migrate or remove `memory.json`;
- propagate knowledge between NPCs;
- add embeddings or a vector database.

## Selected approach

Three approaches were considered:

1. **LLM extraction from dialogue** — rejected because it weakens the authority boundary, adds provider cost, and can convert generated prose into false facts.
2. **Automatic conversion of every MemoryEvent** — rejected because `DIALOGUE` is episodic and `PLAYER_TOLD` content is not inherently a durable semantic claim.
3. **Typed deterministic adapters with an explicit eligibility gate** — selected because it is inspectable, testable, idempotent, provider-independent, and preserves provenance.

## Architecture

### 1. Pure semantic ingestion adapter

Add a pure `SemanticMemoryIngestionAdapter` responsible only for converting eligible controlled inputs into `SemanticMemoryEntry`.

Automatic FACT eligibility:

```text
source.provenance == SYSTEM_OBSERVED
AND source.type in {ACTION, OBSERVATION, RELATIONSHIP_CHANGE}
```

Explicitly rejected:

```text
DIALOGUE
PLAYER_TOLD
NPC_TOLD
INFERRED
null/blank/unsourced input
```

FACT mapping:

```text
ownerNpcId       = source.ownerNpcId
kind             = FACT
statement        = normalized and bounded source.summary
relatedEntities  = source.participants
provenance       = SYSTEM_OBSERVED
gameTime         = source.gameTime
createdAt        = source.createdAtEpochMillis
importance       = source.importance
confidence       = source.confidence
sourceEventIds   = [source.id]
```

The persisted statement is bounded to 240 Unicode code points, matching the existing semantic prompt bound. Whitespace and control characters are normalized before deterministic ID generation.

### 2. Deterministic semantic IDs

FACT ID namespace:

```text
semantic-fact-v1
+ ownerNpcId
+ sourceEventId
```

BELIEF ID namespace:

```text
semantic-belief-v1
+ ownerNpcId
+ provenance
+ normalized statement
+ sorted sourceEventIds
```

Replaying the same source therefore produces the same semantic UUID. Store-level UUID idempotency prevents duplicate writes. Entries derived from distinct authoritative events remain distinct; logical consolidation is a later milestone.

### 3. Explicit BELIEF source object

Add immutable `SemanticBeliefSource` for future controlled producers. It requires:

```text
ownerNpcId
statement
relatedEntities
provenance = PLAYER_TOLD | NPC_TOLD | INFERRED
gameTime
createdAtEpochMillis
importance
confidence
sourceEventIds (non-empty)
```

`SYSTEM_OBSERVED` is rejected because it must be represented as FACT. Unsourced BELIEF creation is rejected. This API is intentionally not wired to ordinary dialogue in this slice.

### 4. Persistence facade

Add `ControlledSemanticMemoryIngestor`:

```text
recordFactIfEnabled(...)
recordFact(...)
recordBeliefIfEnabled(...)
recordBelief(...)
```

It delegates conversion to the pure adapter and persistence to `SemanticMemoryStore`. It does not call a provider or inspect free-form LLM JSON.

### 5. Automatic authoritative FACT producers

Extend the existing successful episodic ingestion paths:

```text
WorldEventMemoryAdapter
→ ACTION MemoryEvent
→ memory2.json
→ controlled FACT ingestion
→ semantic-memory.json
```

```text
RelationshipChangeMemoryAdapter
→ RELATIONSHIP_CHANGE MemoryEvent
→ memory2.json
→ controlled FACT ingestion
→ semantic-memory.json
```

The episodic append occurs first. A semantic persistence failure must not remove or roll back the already persisted episodic event or authoritative Minecraft state.

### 6. Configuration

Add additive version-2 fields:

```text
semanticMemoryEnabled = true
semanticMemoryMaxEntriesPerNpc = 128
```

Normalization:

```text
1 <= semanticMemoryMaxEntriesPerNpc <= 512
```

No config version bump is required because the fields are additive and receive defaults when absent.

### 7. Failure and authority behavior

- FACT can only be produced from an eligible `SYSTEM_OBSERVED` MemoryEvent.
- BELIEF can only be produced by the explicit sourced BELIEF API.
- `DIALOGUE` remains episodic only.
- Semantic persistence is auxiliary; authoritative action/relationship state remains committed even if semantic persistence fails.
- Existing `SemanticMemoryEntry` constructor invariants remain the final defensive boundary.
- Current `worldFacts` remain authoritative over recalled semantic entries.

## Compatibility

- Java 21 unchanged.
- Minecraft 1.21.1 unchanged.
- Fabric primary target unchanged.
- NeoForge compile compatibility remains required.
- `memory.json`, `memory2.json`, `events.json`, `relationships.json`, and `voices.json` formats remain unchanged.
- `semantic-memory.json` format remains version 1.
- Existing ingestion method signatures remain available through overloads; production call sites opt into semantic parameters.

## Test strategy

TDD coverage must prove:

1. eligible server-observed ACTION/OBSERVATION/RELATIONSHIP_CHANGE converts to FACT;
2. DIALOGUE and non-server provenance are rejected by automatic FACT conversion;
3. statements are normalized and bounded before persistence and ID generation;
4. FACT replay generates the same deterministic UUID;
5. explicit BELIEF preserves allowed provenance and source event IDs;
6. `SYSTEM_OBSERVED` and unsourced BELIEF inputs are rejected;
7. controlled ingestion persists entries and remains idempotent;
8. action and relationship episodic producers also write semantic FACT entries when enabled;
9. disabling semantic ingestion leaves episodic Memory 2.0 behavior unchanged;
10. configuration defaults and bounds are correct;
11. semantic write failure cannot roll back the episodic write;
12. full unit, Fabric, package, and NeoForge CI remains green.

## Acceptance criteria

The slice is complete when:

- real authoritative ACTION and RELATIONSHIP_CHANGE events create persistent semantic FACT entries;
- every automatic FACT has `SYSTEM_OBSERVED` provenance and a non-empty `sourceEventIds` list;
- ordinary dialogue creates no automatic semantic entry;
- an explicit provenance-safe BELIEF API exists without an automatic dialogue caller;
- duplicate source replay is idempotent;
- semantic retention is separately configurable and bounded;
- episodic persistence remains successful even if semantic persistence fails;
- both repository CI workflows pass on the exact PR head.
