# Memory 2.0 Controlled Dialogue Extraction Design

## Context

Memory 2.0 currently provides:

- persistent per-NPC `MemoryEvent` storage;
- deterministic bounded retrieval/ranking;
- authoritative safe-action ingestion;
- bounded provenance-preserving context integration into real snapshot-aware NPC turns.

The next milestone is to let successful conversations become durable episodic memories without treating dialogue content as authoritative world truth and without introducing LLM summarization.

## Goal

Persist one bounded, deterministic `MemoryEvent.Type.DIALOGUE` for each successful usable snapshot-aware player↔NPC OpenAI turn.

The event must be conservative about truth, idempotent under replay, bounded in stored text, fail-soft, and independent from the existing legacy `memory.json` dialogue-history path.

## Scope boundary

This first slice applies only to the snapshot-aware LivingWorld/OpenAI path where the turn already has an immutable `LivingWorldContextSnapshot`.

It does not migrate or replace legacy dialogue storage and does not add dialogue extraction to classic/non-snapshot MCA ChatAI or Inworld fallback paths.

A Memory 2.0 dialogue event is created only when:

```text
snapshot-aware OpenAI answer path completes
→ caller receives Optional<String> result
→ result is present and nonblank
→ Memory 2.0 is enabled
→ bounded dialogue event append
```

No event is created for:

- provider/network error;
- `content:null` / empty response exhaustion;
- sanitized response with no usable visible message;
- thrown processing exception that returns no usable answer;
- rejected/admission-blocked request;
- blank answer;
- disabled Memory 2.0.

## DialogueMemoryAdapter

Pure provider/Minecraft-independent conversion inputs:

```text
npcId
playerId
gameTime
playerMessage
npcReply
createdAtEpochMillis
```

Returns no event when IDs are missing or either utterance is null/blank after normalization.

### Provenance

The event uses:

```text
MemoryEvent.Type.DIALOGUE
MemoryEvent.Provenance.PLAYER_TOLD
```

This is deliberately conservative.

The server can verify that a dialogue turn occurred, but the semantic content of the player's statement and the NPC's generated response is not authoritative Minecraft truth. Marking the whole episodic record as `PLAYER_TOLD` ensures context formatting treats it as `BELIEF`, not `VERIFIED`.

The summary explicitly labels speakers so the model can distinguish statement from reply.

### Bounded deterministic summary

Normalize each utterance by:

- collapse whitespace/control/newlines to single spaces;
- trim;
- cap each speaker utterance to 240 Unicode code points.

Stored summary format:

```text
Player said: <bounded player text> | NPC replied: <bounded NPC text>
```

This avoids duplicating unlimited raw transcripts into `memory2.json` while preserving enough conversational meaning for later deterministic retrieval.

Fixed first-policy metadata:

```text
importance = 40
emotionalWeight = 0
confidence = 60
relationshipReasons = []
participants = [npcId, playerId]
```

Importance/confidence are intentionally fixed in this first slice. No LLM decides them.

### Deterministic event identity

Event UUID identifies the dialogue turn, not the provider's exact wording.

Canonical ID input:

```text
memory2-dialogue-v1
npcId
playerId
gameTime
normalized full player message
```

Implementation uses `UUID.nameUUIDFromBytes(... UTF-8 ...)`.

`createdAtEpochMillis` and NPC reply are intentionally excluded from identity.

Consequences:

- replay/redelivery of the same turn remains idempotent even if wall-clock time differs;
- a replay with different NPC wording still maps to the same event UUID;
- the first successfully persisted event wins because `MemoryEventStore` rejects duplicate UUIDs;
- a later turn naturally has a different game time and therefore a different ID.

## Memory2DialogueIngestor

The persistence bridge accepts:

```text
worldRoot
npcId
playerId
gameTime
playerMessage
npcReply
maxEventsPerNpc
createdAtEpochMillis
```

`record(...)` converts through `DialogueMemoryAdapter` and appends through existing `MemoryEventStore`.

`recordIfEnabled(...)` provides the explicit configuration guard used by lifecycle integration.

No Minecraft entity access, provider access, prompt logic or relationship mutation belongs in this class.

## Final lifecycle integration: ChatAI orchestration hook

The final implementation deliberately leaves `OpenAIChatAI` unchanged.

Snapshot-aware orchestration already exists in:

```text
ChatAI.answer(server, player, villager, msg, snapshot)
```

Flow:

```text
ChatAI
→ OpenAIChatAI.answer(...snapshot)
   → provider/retry/parser
   → legacy dialogue memory behavior
   → existing command handling
   → existing relationship-delta handling
   → Optional<String> visible result
→ ChatAI inspects returned Optional
→ present + nonblank → fail-soft Memory 2.0 dialogue ingestion
→ return the original Optional unchanged
```

This is safer than editing the provider/request implementation because:

- provider/retry/parser semantics are untouched;
- legacy `memory.json` behavior is untouched;
- command and relationship code is untouched;
- provider/empty/error paths already surface as no usable `Optional<String>` and therefore cannot create a dialogue event;
- the returned answer is never replaced or modified by Memory 2.0.

The hook applies only when the selected strategy is `OpenAIChatAI` (including its subclasses) on the snapshot-aware path. Non-snapshot and Inworld fallback behavior remains unchanged.

### Fail-soft behavior

The orchestration helper:

- rejects empty/blank results;
- resolves `memory2Enabled` and `memory2MaxEventsPerNpc` from `LivingWorldConfig`;
- uses immutable snapshot `worldRoot`, villager/player UUIDs and game time;
- uses `System.currentTimeMillis()` only as metadata, never identity;
- catches `RuntimeException` from Memory 2.0 persistence;
- logs the failure without changing the already-produced answer.

## Security/truth boundary

- Player text is not a fact merely because it was spoken.
- NPC-generated text is not a fact merely because the model said it.
- The whole dialogue episode remains `BELIEF` / `PLAYER_TOLD` for prompt purposes.
- No instructions embedded in stored dialogue gain authority; context formatting treats memories as data, never instructions.
- No raw provider reasoning or structured metadata enters the event summary.
- Retrieval never upgrades the event to authoritative `worldFacts`.

## Non-goals

- LLM summarization;
- semantic extraction of facts from dialogue;
- promotion of claims to `SYSTEM_OBSERVED`;
- relationship-reason generation;
- working-memory orchestration;
- forgetting/decay;
- semantic duplicate merging;
- migration/removal of legacy `memory.json`;
- classic/non-snapshot MCA ChatAI integration;
- Inworld dialogue ingestion.

## Testing

Tests prove:

1. exact `DIALOGUE` / `PLAYER_TOLD` mapping and fixed scores;
2. speaker-labeled whitespace-normalized summary;
3. 240-code-point per-speaker storage bounds including Unicode safety;
4. missing/blank messages are rejected;
5. same turn identity produces the same event UUID across different reply text and wall-clock timestamps;
6. different game time or player message produces a different event UUID;
7. duplicate ingestion is idempotent in `MemoryEventStore`;
8. distinct successful turns remain distinct and retention remains bounded;
9. stored event participants are NPC + player;
10. `memory2Enabled=false` produces no persistent dialogue event;
11. final integration diff leaves `OpenAIChatAI` unchanged and records only from present/nonblank snapshot-aware OpenAI results.

## Success criteria

- successful usable snapshot-aware OpenAI dialogue turns create bounded actor-owned `DIALOGUE` MemoryEvents;
- failed/empty/blank turns create none;
- dialogue content remains belief data, never authoritative facts;
- duplicate/replay cannot multiply the same turn memory;
- legacy `memory.json` behavior remains unchanged;
- provider/parser/retry lifecycle remains unchanged;
- no LLM summarization or new dependency is introduced;
- exact-final-head unit tests, Fabric package verification, and Fabric/NeoForge CI pass.
