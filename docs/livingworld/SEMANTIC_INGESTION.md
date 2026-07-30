# Controlled Semantic Memory Ingestion

## Status

Implemented after the live-validated `0.1.11+1.21.1` checkpoint. Automated CI validation is required before merge; a new real-server checkpoint is still required before calling this behavior live-validated.

## Purpose

The Semantic Memory foundation now has controlled producers without allowing arbitrary dialogue or model output to become authoritative knowledge.

```text
server-observed event
→ episodic MemoryEvent
→ controlled eligibility gate
→ semantic FACT
```

## Automatic FACT sources

The following Memory 2.0 event types are eligible only with `SYSTEM_OBSERVED` provenance:

```text
ACTION
OBSERVATION
RELATIONSHIP_CHANGE
```

Current automatic production paths are:

```text
successful safe NPC action
→ server-owned WorldEvent
→ ACTION MemoryEvent
→ memory2.json
→ FACT SemanticMemoryEntry
→ semantic-memory.json
```

```text
persisted relationship transition
→ RELATIONSHIP_CHANGE MemoryEvent
→ memory2.json
→ FACT SemanticMemoryEntry
→ semantic-memory.json
```

The episodic event is appended first. Semantic persistence cannot retroactively remove the episodic event or authoritative Minecraft state.

## Explicit BELIEF API

`SemanticBeliefSource` and `ControlledSemanticMemoryIngestor.recordBelief(...)` provide a controlled API for future provenance-aware producers.

A BELIEF requires:

- `PLAYER_TOLD`, `NPC_TOLD`, or `INFERRED` provenance;
- a non-empty statement;
- at least one source event UUID;
- one owning NPC UUID.

`SYSTEM_OBSERVED` is rejected for BELIEF because server-observed evidence must be represented as FACT.

The BELIEF API is not automatically called from ordinary dialogue in this slice.

## Dialogue boundary

```text
DIALOGUE MemoryEvent
→ episodic memory only
→ no automatic SemanticMemoryEntry
```

The mod does not use an LLM to extract semantic claims, classify truth, or promote player/NPC prose into facts.

## Deterministic identity

Semantic FACT IDs are derived from:

```text
semantic-fact-v1
+ owner NPC UUID
+ source event UUID
```

Semantic BELIEF IDs are derived from:

```text
semantic-belief-v1
+ owner NPC UUID
+ provenance
+ normalized statement
+ sorted source event UUIDs
```

Replaying the same source is idempotent. Separate source events remain separate until a future deterministic consolidation policy is implemented.

## Bounds and normalization

- statements are normalized for whitespace/control characters;
- persisted statements are limited to 240 Unicode code points;
- semantic retention uses the existing `memory2MaxEventsPerNpc` bound;
- no new configuration keys are introduced;
- `semantic-memory.json` remains format version 1.

## Truth boundary

```text
FACT   → SYSTEM_OBSERVED only
BELIEF → PLAYER_TOLD | NPC_TOLD | INFERRED only
```

Confidence never converts BELIEF into FACT. Current server-observed `worldFacts` remain authoritative when recalled semantic memory conflicts with current state.

## Not implemented

This slice does not add:

- automatic semantic extraction from dialogue;
- LLM truth classification;
- logical duplicate consolidation across different sources;
- forgetting/decay;
- migration from `memory.json`;
- NPC-to-NPC knowledge or rumor propagation;
- embeddings or vector storage.

## Live validation target

A real-server test should verify:

1. perform a successful safe action;
2. produce a persisted relationship change;
3. confirm corresponding episodic events exist in `memory2.json`;
4. confirm corresponding FACT entries exist in `semantic-memory.json`;
5. confirm every FACT has `SYSTEM_OBSERVED` and a source event UUID;
6. replay/retry does not create duplicate semantic UUIDs;
7. ordinary text and voice dialogue do not create semantic entries by themselves;
8. restart preserves semantic and episodic files byte-for-byte;
9. text, voice, STT, TTS, monitor and server health remain unchanged.
