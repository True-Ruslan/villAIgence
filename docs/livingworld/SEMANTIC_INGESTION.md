# Controlled Semantic Memory Ingestion

## Status

Semantic Memory ingestion is an active Memory 2.0 subsystem.

Implemented layers include:

- controlled server-observed FACT production;
- explicit provenance-safe BELIEF sources;
- deterministic consolidation/source union;
- deterministic pressure-based forgetting;
- controlled BELIEF admission from explicit persisted source evidence;
- opt-in bounded `PLAYER_TOLD` claim extraction from the existing structured chat response.

The extraction path is advisory and non-authoritative. It does not create FACT and does not make a second provider request.

## Truth boundary

```text
FACT   -> SYSTEM_OBSERVED only
BELIEF -> PLAYER_TOLD | NPC_TOLD | INFERRED only
```

Confidence never upgrades BELIEF into FACT.

Current observed world facts remain authoritative when they conflict with recalled beliefs.

---

## Controlled FACT path

Eligible automatic FACT sources are Memory 2.0 events with `SYSTEM_OBSERVED` provenance:

```text
ACTION
OBSERVATION
RELATIONSHIP_CHANGE
```

Current production paths include:

```text
successful safe NPC action
-> server-owned WorldEvent
-> ACTION MemoryEvent
-> memory2.json
-> FACT SemanticMemoryEntry
-> semantic-memory.json
```

```text
persisted relationship transition
-> RELATIONSHIP_CHANGE MemoryEvent
-> memory2.json
-> FACT SemanticMemoryEntry
-> semantic-memory.json
```

The episodic event is persisted first. Semantic persistence cannot retroactively remove authoritative Minecraft state or the already-recorded source event.

---

## Controlled BELIEF admission

`SemanticBeliefAdmissionPolicy` is the fail-closed boundary between persisted Memory 2.0 evidence and a semantic BELIEF candidate.

An admitted BELIEF always carries the exact source MemoryEvent UUID. Callers cannot fabricate a source-event list through this API.

### PLAYER_TOLD

Admission requires:

```text
source type       DIALOGUE
source provenance PLAYER_TOLD
requested kind    BELIEF
requested provenance PLAYER_TOLD
```

A player-told claim cannot be admitted from a server-observed action, observation, or unrelated provenance.

### NPC_TOLD

Admission requires:

```text
source type       DIALOGUE
source provenance NPC_TOLD
requested kind    BELIEF
requested provenance NPC_TOLD
```

This is the contract required by future NPC-to-NPC knowledge transfer. It does not itself create an NPC-to-NPC dialogue producer.

### INFERRED

`INFERRED` remains explicitly non-authoritative. It may reference an explicit persisted Memory 2.0 event as evidence, but the resulting semantic entry remains BELIEF regardless of confidence.

### Rejected inputs

The admission path fails closed for:

- missing source event;
- blank statement;
- missing provenance;
- `SYSTEM_OBSERVED` requested through the BELIEF path;
- `PLAYER_TOLD` without matching `PLAYER_TOLD` DIALOGUE evidence;
- `NPC_TOLD` without matching `NPC_TOLD` DIALOGUE evidence.

Rejected admission writes nothing.

---

## Opt-in PLAYER_TOLD extraction

The next Memory 2.0 layer uses the existing structured OpenAI/OpenRouter Chat response. It does **not** issue a second model request.

When enabled, the structured response may contain:

```json
{
  "message": "natural NPC reply",
  "optionalCommand": "",
  "relationshipDelta": null,
  "beliefCandidates": [
    "short durable claim explicitly asserted by the latest player message"
  ]
}
```

The model controls only candidate statement text. It does not control semantic authority metadata.

Server-owned code fixes:

```text
owner NPC UUID
current player UUID
provenance = PLAYER_TOLD
source = the exact persisted DIALOGUE MemoryEvent
kind = BELIEF
```

Required order:

```text
provider returns one structured answer
-> visible message is sanitized
-> successful DIALOGUE is persisted to memory2.json
-> exact persisted DIALOGUE MemoryEvent is returned to the lifecycle
-> bounded candidate strings are admitted as PLAYER_TOLD BELIEF
-> semantic-memory.json
```

If DIALOGUE persistence fails or no successful visible answer exists, no semantic BELIEF candidate is persisted.

### Configuration

Extraction is deliberately disabled by default:

```json
{
  "semanticBeliefExtractionEnabled": false,
  "semanticBeliefMaxCandidatesPerTurn": 3
}
```

Hard limits:

```text
max candidates per turn: 8
max candidate statement: 240 Unicode code points
```

Existing config version remains `2`. Missing fields receive safe defaults; no config migration is required.

### Candidate parser

`SemanticBeliefCandidateParser`:

- accepts only a JSON array of strings;
- fails closed to an empty list for null, blank, malformed or non-array input;
- ignores non-string elements;
- Unicode-normalizes with NFKC;
- collapses whitespace/control characters;
- truncates by Unicode code points;
- deduplicates normalized candidates while preserving order;
- clamps candidate count to the hard maximum.

### Prompt contract

`SemanticBeliefExtractionPrompt` asks for only short durable claims explicitly asserted by the **latest player message**.

It explicitly requires `[]` for greetings, questions, commands, transient chatter or when no useful durable claim is present, and forbids provenance/source/truth/FACT metadata in model output.

The extraction prompt is added only when both Memory 2.0 and semantic belief extraction are enabled.

---

## Persistence producer

`PlayerToldBeliefLifecycle` feeds provider-supplied statement text into the already-existing controlled admission path:

```text
candidate statement
+ exact persisted PLAYER_TOLD DIALOGUE event
+ server-owned player UUID
-> ControlledSemanticBeliefProducer
-> SemanticBeliefAdmissionPolicy
-> SemanticBeliefSource
-> ControlledSemanticMemoryIngestor
-> SemanticMemoryStore
-> semantic-memory.json
```

Exact replay remains idempotent through deterministic semantic identity and store replay handling.

Equivalent corroborating claims from distinct source events pass through the deterministic consolidation pipeline and union their source UUIDs rather than multiplying identical entries.

---

## Dialogue boundary

Dialogue is still episodic by default.

```text
DIALOGUE MemoryEvent
-> episodic memory
-> no semantic entry while extraction is disabled
```

When extraction is enabled, only explicit bounded candidate metadata may feed the controlled BELIEF path. The raw NPC reply is never itself treated as truth, and the model cannot create a FACT.

Classic/Inworld paths remain outside this extraction slice.

---

## Deterministic identity and consolidation

Semantic FACT identity is based on owner NPC and source event UUID.

BELIEF identity is based on:

```text
semantic-belief-v1
+ owner NPC UUID
+ provenance
+ normalized statement
+ sorted source event UUIDs
```

The Semantic Memory store then applies deterministic consolidation. Equivalent entries with compatible owner/kind/provenance/statement/related-entity boundaries union source evidence. Confidence is not artificially promoted by consolidation.

---

## Security and authority properties

The controlled BELIEF/extraction path does not:

- create FACT from dialogue;
- accept `SYSTEM_OBSERVED` as BELIEF provenance;
- let the model choose owner/player/source UUIDs;
- let the model choose truth labels;
- change Minecraft state;
- authorize server actions;
- infer truth from confidence;
- persist provider hidden reasoning;
- make a second provider request for extraction.

Malformed extraction metadata fails soft and cannot invalidate an otherwise valid visible NPC reply.

---

## TDD evidence

### Controlled BELIEF admission — PR #123

Tests-only RED head:

```text
1b8818e34208211c0631a3d852b5fd2e9409743d
```

Production Soak #52 reached `:common:compileTestJava` and failed on intentionally missing `SemanticBeliefAdmissionPolicy` and `ControlledSemanticBeliefProducer` production APIs. Final PR #123 exact-head security, CI, production soak and release dry-run all passed before merge.

### PLAYER_TOLD extraction — PR #125

The feature is developed tests-first from parser/structured-metadata contracts before production wiring. Required final acceptance includes:

- parser bounds/normalization/deduplication;
- structured-response metadata isolation;
- safe disabled defaults and config normalization;
- persisted DIALOGUE source identity returned to the semantic lifecycle;
- one semantic write path only after successful DIALOGUE persistence;
- provider retry/replay idempotency;
- no AI-to-FACT path;
- no second provider request;
- retained voice deadline/commit ordering policy;
- final exact-head repository security, main CI, production soak and release dry-run.

Final exact-head run IDs are recorded in PR #125 after all changes are complete.

---

## Remaining 0.2 work

After bounded player-told extraction, the next semantic/social-memory steps are:

1. trustworthy causal relationship-change reasons tied to validated source events;
2. retrieval precedence regression scenarios for current FACT/context over conflicting BELIEF;
3. long-horizon recall scenarios;
4. NPC-to-NPC knowledge transfer using the `NPC_TOLD` contract;
5. provenance-aware rumor propagation with uncertainty and bounded distortion.

Legacy `memory.json` migration remains cancelled by design for the accepted pre-1.0 clean-state rollout boundary.
