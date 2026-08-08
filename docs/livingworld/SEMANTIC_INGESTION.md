# Controlled Semantic Memory Ingestion

## Status

Semantic Memory ingestion is an active Memory 2.0 subsystem.

Implemented layers now include:

- controlled server-observed FACT production;
- explicit provenance-safe BELIEF sources;
- deterministic consolidation/source union;
- deterministic pressure-based forgetting;
- controlled BELIEF admission from explicit persisted source evidence.

Automatic free-form LLM claim extraction is still not implemented.

## Purpose

Semantic Memory must allow NPCs to remember knowledge without confusing conversation, inference, or model output with authoritative Minecraft truth.

Canonical truth boundary:

```text
FACT   -> SYSTEM_OBSERVED only
BELIEF -> PLAYER_TOLD | NPC_TOLD | INFERRED only
```

Confidence never upgrades BELIEF into FACT.

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

## Persistence producer

`ControlledSemanticBeliefProducer.recordIfEnabled(...)` performs:

```text
explicit persisted MemoryEvent
+ bounded claim candidate
+ explicit BELIEF provenance
-> SemanticBeliefAdmissionPolicy
-> SemanticBeliefSource
-> ControlledSemanticMemoryIngestor
-> SemanticMemoryStore
-> semantic-memory.json
```

The producer is disabled/no-op when Memory 2.0 semantic ingestion is disabled or no world root is available.

Exact replay remains idempotent through deterministic semantic identity and store replay handling.

Equivalent corroborating claims from distinct source events pass through the existing deterministic consolidation pipeline and union their source UUIDs rather than multiplying identical entries.

---

## Dialogue boundary

Ordinary dialogue still does **not** automatically become Semantic Memory merely because a DIALOGUE event exists.

```text
DIALOGUE MemoryEvent
-> episodic memory
-> no semantic entry unless a controlled producer supplies an explicit claim candidate
```

The mod still does not ask an LLM to decide that arbitrary dialogue text is true.

A future extraction layer must remain separate from admission:

```text
conversation/evidence
-> bounded inspectable candidate extraction
-> admission policy
-> BELIEF persistence
```

Extraction failure, malformed output, provider failure, or an empty candidate must create no semantic entry.

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

The Semantic Memory store then applies the existing deterministic consolidation policy. Equivalent entries with compatible owner/kind/provenance/statement/related-entity boundaries union source evidence. Confidence is not artificially promoted by consolidation.

---

## Bounds

- statements are normalized for whitespace/control characters;
- persisted semantic statements are capped at 240 Unicode code points;
- importance and confidence remain bounded 0..100;
- semantic retention remains bounded per NPC;
- source evidence is explicit;
- `semantic-memory.json` remains the current world-local semantic store;
- no embeddings or vector database are required by this contract.

---

## Security and authority properties

The controlled BELIEF path does not:

- create FACT from dialogue;
- accept `SYSTEM_OBSERVED` as a BELIEF provenance;
- change Minecraft state;
- authorize server actions;
- infer truth from confidence;
- expose provider reasoning;
- call an external provider itself.

Current observed world facts remain authoritative when they conflict with recalled beliefs.

---

## TDD evidence for controlled BELIEF admission

PR #123 established a tests-first RED boundary before production classes existed.

RED production head predecessor:

```text
1b8818e34208211c0631a3d852b5fd2e9409743d
```

Production Soak #52 reached `:common:compileTestJava` and failed on the intentionally missing:

```text
SemanticBeliefAdmissionPolicy
ControlledSemanticBeliefProducer
```

The repository/soak contract checks before compilation passed, confirming the failure was the intended missing-feature RED rather than an infrastructure failure.

Initial GREEN production head:

```text
3da22729bcf2b8f981a1935dea69bff27b81bb22
```

On that exact head, `Run common and deterministic mock-provider tests` passed in VillAIgence CI #1834 before documentation commits were added.

Final exact-head validation must be recorded in PR #123 after all documentation and review changes are complete.

---

## Remaining 0.2 work

The next semantic-memory steps remain:

1. a bounded inspectable claim-extraction contract that can feed the admission API without granting authority to the provider;
2. trustworthy causal relationship-change reasons tied to validated source events;
3. long-horizon recall scenarios;
4. NPC-to-NPC knowledge transfer using the `NPC_TOLD` contract;
5. provenance-aware rumor propagation with uncertainty and bounded distortion.

Legacy `memory.json` migration remains cancelled by design for the accepted pre-1.0 clean-state rollout boundary.
