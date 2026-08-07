# VillAIgence Memory 2.0

Memory 2.0 is the canonical world-local memory subsystem for VillAIgence. The clean-cutover development package deliberately drops the experimental pre-0.2 `memory.json` conversation store instead of building an importer for test-only data.

## Storage boundary

Canonical memory stores:

```text
<world>/livingworld/memory2.json
<world>/livingworld/semantic-memory.json
```

`memory2.json` owns episodic events, including persistent DIALOGUE history. There is no dual-read period and no runtime fallback to `memory.json`.

The cutover is intentionally pre-1.0 and assumes a clean LivingWorld test state. Historical 0.1.x release evidence remains valid for the versions it describes; it is not a compatibility promise for 0.2 development worlds.

## MemoryEvent

Each immutable event belongs to exactly one NPC and contains:

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
dialogue?                 // structured DIALOGUE payload only
```

Types:

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

```text
SYSTEM_OBSERVED
PLAYER_TOLD
NPC_TOLD
INFERRED
```

`SYSTEM_OBSERVED` is reserved for server-verified evidence. `PLAYER_TOLD`, `NPC_TOLD`, and `INFERRED` remain claims/beliefs regardless of confidence or retrieval rank.

Hard rule:

```text
FACT     → SYSTEM_OBSERVED only
BELIEF   → PLAYER_TOLD / NPC_TOLD / INFERRED only
DIALOGUE → episodic only by default
```

Current observed Minecraft state always overrides conflicting recalled memory or operator lore.

## Persistent dialogue model

A successful OpenAI text or snapshot/voice turn is admitted once through:

```text
ChatAI
→ successful usable Optional<String>
→ Memory2DialogueLifecycle
→ Memory2DialogueIngestor
→ DialogueMemoryAdapter
→ MemoryEventStore
→ memory2.json
```

Provider retries remain outside the persistence boundary, so repeated provider attempts do not multiply dialogue side effects.

No dialogue event is created for an absent/blank result, exhausted empty completion, failed provider turn, disabled Memory 2.0, or non-OpenAI fallback that does not enter the shared lifecycle.

### Structured DIALOGUE payload

DIALOGUE retains a human-readable episodic summary but prompt reconstruction never parses that summary.

```text
MemoryEvent.DialogueExchange
├── playerMessage
└── npcReply
```

Both utterances are whitespace/control-normalized and independently capped to 240 Unicode code points. The same bounded values are used for the structured payload and summary:

```text
Player said: <playerMessage> | NPC replied: <npcReply>
```

Delimiter-like text inside either utterance is therefore harmless; roles are structural, not inferred from strings.

Legacy/summary-only DIALOGUE events without `DialogueExchange` are ignored by the persistent dialogue-history reader. They are not migrated or guessed.

### Deterministic identity

DIALOGUE UUID is derived from:

```text
memory2-dialogue-v1
villager UUID
player UUID
originating game time
full normalized player message
```

NPC reply and wall-clock time are excluded. Replay/redelivery of the same turn therefore maps to the same ID; `MemoryEventStore` keeps the first successfully persisted event.

## Dialogue retrieval for prompts

`Memory2DialogueHistory` reads one exact NPC/player conversation from `MemoryEventStore`.

Eligibility is applied before the result limit:

```text
ownerNpcId == current NPC
AND type == DIALOGUE
AND exact current player participant
AND structured DialogueExchange exists
```

This prevents newer ACTION/OBSERVATION/RELATIONSHIP_CHANGE entries from consuming the dialogue result limit merely because they are newer.

Eligible exchanges are selected newest-first, reversed back into chronological order, rendered as alternating `user` / `assistant` messages, and finally passed through `WorkingMemoryOrchestrator`.

Current hard dialogue bound:

```text
MAX_RECENT_DIALOGUE_MESSAGES = 12
MAX_DIALOGUE_CODE_POINTS      = 1200 per message
```

The current implementation therefore reconstructs at most six complete recent exchanges for one exact NPC/player pair.

NPC and player isolation are both mandatory. Another player's conversation with the same NPC and the same player's conversation with another NPC are not eligible.

## Working Memory

`WorkingMemoryOrchestrator` composes hard-bounded turn context:

```text
recent dialogue     ≤ 12 messages
episodic entries    ≤ 6
semantic entries    ≤ 6
```

It is a pure bounded composition layer; it does not make provider calls or mutate persistent data.

## Event persistence guarantees

`MemoryEventStore` provides:

- per-NPC ownership;
- bounded retention;
- deterministic ordering;
- idempotent append by UUID;
- filtered-before-limited retrieval seams;
- atomic temp-file + replace writes;
- corrupt-file backup/recovery through the shared JSON persistence layer.

The production acceptance/recovery boundary treats these five LivingWorld stores as canonical:

```text
memory2.json
semantic-memory.json
relationships.json
voices.json
operator-lore.json
```

`memory.json` is no longer part of that matrix.

## Deterministic episodic retrieval

`MemoryQuery`, `MemoryRetriever`, and inspectable `RankedMemory` values remain the deterministic retrieval layer for episodic context.

```text
MemoryQuery
→ bounded candidates for one NPC
→ deterministic component scores
→ deterministic ranking/tie-break
→ bounded results
```

Ranking signals include participant/type relevance, importance, recency, and confidence. Retrieval never upgrades provenance.

## Semantic Memory

`semantic-memory.json` remains separate from episodic events and stores typed `FACT` / `BELIEF` entries with sources and deterministic consolidation/forgetting behavior.

Server-observed FACT ingestion is controlled. Dialogue is not automatically converted into semantic truth.

## Safe-action ingestion

A successful server-authoritative whitelisted NPC action follows:

```text
action succeeds
→ authoritative world event persists
→ actor-owned ACTION MemoryEvent
→ memory2.json
```

The source event ID is reused where appropriate for natural idempotency. Secondary Memory 2.0 failure cannot roll back a successful gameplay action or authoritative factual event.

## Relationship-change ingestion

A successfully persisted bounded relationship transition can become `RELATIONSHIP_CHANGE`:

```text
LLM proposes bounded delta
→ server validates/applies
→ relationships.json save succeeds
→ exact before/after transition known
→ RELATIONSHIP_CHANGE MemoryEvent
```

The memory records the actual applied transition, not the unconstrained proposal.

Causal relationship reasons remain deliberately separate: the server may know that trust changed without knowing an authoritative reason why. No generated explanation is promoted to FACT.

## Failure behavior

Memory persistence/retrieval is auxiliary and fail-soft where safe:

- a Memory 2.0 read failure produces empty recalled dialogue/context rather than switching to another persistent format;
- a secondary memory write failure does not remove an already-valid visible answer or successful authoritative action;
- corrupt canonical stores use the shared backup/regeneration policy;
- retries/replays do not duplicate deterministic events.

## Clean-cutover contract

This development package intentionally does **not** provide:

- `memory.json` importer;
- migration checkpoint/version ledger for legacy conversations;
- dual reads;
- automatic legacy-to-Memory-2.0 conversion;
- destructive rewrite of an old test world.

For installed acceptance, use a clean test-world/LivingWorld state. This is an explicit pre-1.0 development boundary, not silent data loss hidden behind a compatibility claim.

## Remaining Memory 2.0 work

After the clean cutover, the next useful slices are:

```text
controlled BELIEF producers
→ trustworthy causal relationship reasons
→ improved long-horizon episodic/semantic recall
→ NPC-to-NPC knowledge transfer
→ rumor propagation with provenance/uncertainty/distortion
```

Embeddings or vector search remain optional future implementation choices rather than prerequisites for the correctness of the deterministic memory model.
