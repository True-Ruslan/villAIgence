# Trustworthy Causal Relationship Memory — Design

Date: 2026-08-08  
Status: approved design, implementation not started  
Base: `1.21.1` at `3c8ac6536b6849be199adf6b1c9cb407da2550fd`

## Goal

Make relationship history explainable without allowing an LLM to invent authoritative retrospective reasons.

The first causal-memory slice records only a server-verifiable fact:

> a specific validated relationship transition occurred during a specific successfully persisted dialogue turn.

It does **not** claim why the player or NPC psychologically deserved that transition. Free-form model text is not an authoritative cause.

## Existing boundary

Today the snapshot OpenAI/OpenRouter path:

1. parses a bounded model-proposed `relationshipDelta`;
2. applies/clamps/persists it through the server-owned relationship store;
3. converts a successful transition into a `SYSTEM_OBSERVED` `RELATIONSHIP_CHANGE` Memory 2.0 event;
4. returns the visible answer;
5. `ChatAI` then persists the exact NPC/player DIALOGUE event.

`RelationshipChangeMemoryAdapter` currently keeps the exact applied delta in its deterministic summary, but the structured before/after state is not retained as a typed Memory 2.0 payload. `relationshipReasons` remains an unstructured legacy field and is not a trustworthy causal model.

The design preserves the existing numeric relationship application boundary. It does not reapply or delay a relationship delta merely to create causal history.

## Architecture decision

Introduce a separate structured `RELATIONSHIP_CAUSE` Memory 2.0 event.

The numeric transition and its causal link remain distinct:

```text
provider proposal
→ server validates/applies relationship delta
→ RELATIONSHIP_CHANGE

successful visible dialogue
→ DIALOGUE

persisted RELATIONSHIP_CHANGE + persisted DIALOGUE
→ server validates exact same NPC/player turn
→ RELATIONSHIP_CAUSE(DIALOGUE_TURN)
```

This is preferred over:

- filling `relationshipReasons` with free-form text, which would mix fact and explanation;
- storing causal history only in Semantic Memory, which would lose the explicit relationship-transition boundary;
- asking a second model/extractor request, which would add cost and another unreliable authority surface.

## Data model

### 1. Structured relationship transition payload

New `RELATIONSHIP_CHANGE` events carry a typed immutable payload containing the exact server state before and after the accepted mutation:

```text
RelationshipTransition
  beforeTrust
  beforeRespect
  beforeFear
  beforeAffinity
  afterTrust
  afterRespect
  afterFear
  afterAffinity
```

The payload is derived only from `LivingWorldRelationshipChange` after server validation/clamping/persistence.

It contains no model prose and no inferred reason.

Existing historical `RELATIONSHIP_CHANGE` events without the new optional payload remain readable. No backfill or migration is required for this pre-1.0 line. Only newly created transitions are eligible for the new causal-link producer.

### 2. New `RELATIONSHIP_CAUSE` event

Add `MemoryEvent.Type.RELATIONSHIP_CAUSE` with a required structured payload:

```text
RelationshipCause
  kind = DIALOGUE_TURN
  relationshipChangeEventId
  evidenceEventId
  transitionSnapshot
```

`transitionSnapshot` is the same bounded before/after numeric state captured from the relationship-change event. The small duplication is intentional: the causal record remains inspectable even if bounded Memory 2.0 pressure later evicts the referenced transition event.

The cause event itself uses:

```text
ownerNpcId   = exact NPC owner shared by both source events
participants = exact NPC + player pair
provenance   = SYSTEM_OBSERVED
confidence   = 100
```

`SYSTEM_OBSERVED` here means only that the server observed and recorded the causal association between the accepted transition and the exact dialogue turn. It does **not** promote any statement inside that dialogue to FACT.

The event summary is deterministic and generic, for example:

```text
Relationship change occurred during dialogue with player.
```

No player message, NPC reply, model reasoning, or generated psychological explanation is copied into the cause summary.

### 3. Cause kind

The only cause kind in this slice is:

```text
DIALOGUE_TURN
```

Do not predefine gifts, actions, combat, rumors, or inferred reasons. Those require their own validated producers later.

## Deterministic identity and replay

`RELATIONSHIP_CAUSE` uses a deterministic UUID derived from:

```text
namespace version
ownerNpcId
relationshipChangeEventId
evidenceEventId
cause kind
```

It does not depend on wall-clock time.

Consequences:

- exact replay creates the same cause ID;
- `MemoryEventStore` idempotency prevents duplicate persistence;
- the same relationship transition cannot acquire multiple identical dialogue-turn causes under retry;
- a different source dialogue produces a different cause event and must independently pass validation.

## Admission / validation boundary

A dedicated provider-independent lifecycle, tentatively `RelationshipCauseLifecycle`, creates the event only when all conditions hold.

Required checks:

1. Memory 2.0 is enabled.
2. The relationship source event exists and is `RELATIONSHIP_CHANGE`.
3. It is `SYSTEM_OBSERVED`.
4. It has the new structured transition payload.
5. The evidence event exists and is `DIALOGUE`.
6. Both events have the same `ownerNpcId`.
7. Both events contain the same server-owned player UUID.
8. Both events contain the owner NPC UUID.
9. The supplied player UUID is a participant of both source events.
10. The relationship event and dialogue event are present in the current world-local `MemoryEventStore` at the moment of admission.
11. The event IDs are distinct and non-null.
12. The cause kind is the server-selected `DIALOGUE_TURN` constant.

No caller may provide arbitrary owner, provenance, confidence, source-event list, free-form reason, before/after values, or event timestamps for the cause record.

## Orchestration

### Returning the exact relationship event

`Memory2RelationshipChangeIngestor` becomes result-bearing: after a successful eligible append it returns the exact deterministic `RELATIONSHIP_CHANGE` `MemoryEvent`. Existing call sites may ignore the return value.

`OpenAIChatAI.applySnapshotRelationshipDelta(...)` returns that server-created event when all of the following succeed:

```text
relationship feature enabled
proposed delta accepted
numeric relationship state persisted
Memory 2.0 relationship event persisted
```

The result is carried in the internal snapshot answer object. This metadata is server-created; the provider never supplies it.

### Creating the cause after dialogue persistence

`ChatAI` remains the post-success orchestration boundary:

```text
OpenAIChatAI returns
  visible reply
  bounded BELIEF candidates
  optional server-created RELATIONSHIP_CHANGE event

ChatAI persists exact DIALOGUE

if DIALOGUE and RELATIONSHIP_CHANGE both exist
  → RelationshipCauseLifecycle.recordDialogueTurn(...)

then existing controlled BELIEF candidate admission proceeds
```

This does not reapply the relationship delta.

The numeric relationship mutation remains valid even if later auxiliary Memory 2.0 writes fail. Missing causal evidence is preferable to inventing or reconstructing a cause after a partial failure.

## Failure semantics

Fail soft and never fabricate history.

### Relationship state persisted, relationship MemoryEvent failed

- numeric relationship state remains authoritative;
- no `RELATIONSHIP_CAUSE` is written;
- log bounded operational metadata only.

### Relationship MemoryEvent persisted, DIALOGUE persistence failed

- retain the valid `RELATIONSHIP_CHANGE` event;
- do not create a causal event;
- do not infer the missing dialogue source from provider text.

### DIALOGUE persisted, relationship did not change

- no relationship change event;
- no causal event.

### Mismatched NPC/player/source IDs

- reject cause creation;
- write nothing;
- do not repair or reinterpret the supplied source pair.

### Retry / replay

- numeric relationship application retains its existing exact-once/replay behavior;
- deterministic MemoryEvent IDs prevent duplicate relationship/cause events;
- duplicate cause admission is a no-op.

### Capacity pressure / missing historical source

The current store remains bounded. This slice does not pin source events indefinitely.

`RELATIONSHIP_CAUSE` retains its source UUIDs plus the small structured transition snapshot. A future reader may report a referenced source as unavailable if it has aged out. It must never replace missing source evidence with generated prose.

Long-horizon linked-retention policy belongs to the later 0.2 recall/scaling milestone.

## Query surface

Add a provider-independent read API for recent causal relationship history, scoped by exact NPC and player.

A resolved record exposes:

```text
cause event
cause kind
exact before/after transition snapshot
relationshipChangeEventId
relationshipChangeEvent when still available
evidenceEventId
evidence DIALOGUE when still available
```

Rules:

- exact NPC/player filtering happens before result limiting;
- newest eligible causes are selected, then returned in deterministic order suitable for callers;
- dangling/malformed source references never become invented reasons;
- the query API does not automatically inject causal history into prompts in this slice.

This creates an inspectable foundation for later personality/dialogue use without silently changing current NPC behavior.

## Persistence compatibility

No new world-local file is introduced. Everything remains in:

```text
<world>/livingworld/memory2.json
```

The Memory 2.0 file stays bounded and world-local.

The design adds optional structured payload fields and one new event type. Existing 0.2 entries remain readable. No legacy `memory.json` migration, dual reader, or backfill is introduced.

Current clean pre-1.0 compatibility policy remains unchanged.

## Semantic-memory boundary

`RELATIONSHIP_CHANGE` keeps the existing controlled server-observed FACT ingestion behavior where applicable.

`RELATIONSHIP_CAUSE` is social-history evidence and is **not** automatically projected into Semantic Memory in this slice.

A dialogue being the trigger for a relationship transition does not make the dialogue content true.

## Security and authority properties

The provider may still propose only the bounded numeric relationship delta already permitted by the current structured response contract.

The provider cannot choose:

```text
cause kind
cause text
cause event UUID
relationship source UUID
dialogue source UUID
NPC owner UUID
player UUID
before/after state
provenance
confidence
FACT authority
```

The client controls none of these fields.

No hidden reasoning, chain-of-thought, raw provider payload, or arbitrary model explanation is persisted.

## TDD plan / acceptance contract

Implementation must follow an observed RED before production changes.

### RED 1 — structured transition

Tests require:

- new relationship events expose exact before/after typed state;
- clamped/applied state, not raw model proposal, is stored;
- old constructor/read paths remain compatible;
- no free-form cause/reason is created.

### RED 2 — cause admission

Tests require:

- valid relationship-change + DIALOGUE pair creates one `RELATIONSHIP_CAUSE`;
- deterministic replay produces one event;
- wrong NPC is rejected;
- wrong player is rejected;
- non-dialogue evidence is rejected;
- non-relationship source is rejected;
- missing structured transition is rejected;
- fabricated/unpersisted source event is rejected;
- no API accepts model-supplied reason text.

### RED 3 — orchestration

Tests require:

- exact server-created relationship MemoryEvent is returned from relationship ingestion;
- ChatAI persists DIALOGUE before attempting causal-link admission;
- cause creation uses the exact DIALOGUE event returned by `Memory2DialogueLifecycle`;
- no relationship change means no cause;
- DIALOGUE persistence failure means no cause;
- classic/Inworld paths remain unchanged unless separately designed.

### RED 4 — query and persistence

Tests require:

- exact NPC/player isolation before limiting;
- deterministic recent-history ordering;
- restart persistence;
- missing referenced source is surfaced as unavailable, never fabricated;
- bounded store behavior remains intact.

### Regression gates

The final exact PR head must pass the repository-selected complete matrix appropriate for runtime + Memory 2.0 persistence changes, including at minimum:

- common/unit tests;
- repository security policy;
- risk selector / required server GameTests;
- Fabric + NeoForge builds;
- production startup/restart acceptance;
- current five-store persistence recovery;
- package smoke;
- production soak;
- GitHub Release dry-run with publication skipped;
- independent diff review with no unresolved P0/P1/P2 findings.

Root `CHANGELOG.md` `[Unreleased]` must be updated in the runtime PR.

## Explicit non-goals

This slice does **not** implement:

- psychological/natural-language explanations for why trust/respect/fear/affinity changed;
- model-generated authoritative relationship reasons;
- `NPC_TOLD` or `INFERRED` causal producers;
- gift/combat/action cause kinds;
- relationship-history prompt injection;
- personality behavior changes;
- rumor propagation;
- embeddings/vector search;
- unbounded retention or source pinning;
- legacy `memory.json` migration.

## Exit criterion

The slice is complete when VillAIgence can persist and later query an exact relationship transition together with a deterministic server-authored link to the exact dialogue turn during which that transition occurred, while:

- preserving numeric before/after server state;
- preserving source event UUIDs;
- surviving retry/replay/restart without duplicate causal history;
- rejecting cross-NPC/player or fabricated sources;
- never treating free-form LLM explanation as authoritative cause;
- never turning dialogue content into FACT merely because it triggered a relationship change.
