# Memory 2.0 Controlled Dialogue Extraction Design

## Context

Memory 2.0 currently provides:

- persistent per-NPC `MemoryEvent` storage;
- deterministic bounded retrieval/ranking;
- authoritative safe-action ingestion;
- bounded provenance-preserving context integration into real snapshot-aware NPC turns.

The next milestone is to let successful conversations become durable episodic memories without treating dialogue content as authoritative world truth and without introducing LLM summarization.

## Goal

Persist one bounded, deterministic `MemoryEvent.Type.DIALOGUE` for each successful usable snapshot-aware player↔NPC AI turn.

The event must be conservative about truth, idempotent under replay, bounded in stored text, fail-soft, and independent from the existing legacy `memory.json` dialogue-history path.

## Scope boundary

This first slice applies only to the direct snapshot-aware LivingWorld/OpenAI path where the turn already has an immutable `LivingWorldContextSnapshot`.

It does not migrate or replace legacy dialogue storage.

A Memory 2.0 dialogue event is created only when:

```text
provider request completed successfully
→ structured response produced a usable nonblank visible NPC message
→ existing post-success dialogue flow is reached
```

No event is created for:

- provider/network error;
- `content:null` / empty response exhaustion;
- sanitized response with no usable visible message;
- thrown processing exception before a usable response;
- rejected/admission-blocked request.

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

Event UUID must identify the dialogue turn, not the provider's exact wording.

Canonical ID input:

```text
memory2-dialogue-v1
npcId
playerId
gameTime
normalized full player message
```

Use `UUID.nameUUIDFromBytes(... UTF-8 ...)`.

Do **not** include `createdAtEpochMillis` or NPC reply in the ID.

Reasoning:

- replay/redelivery of the same successful turn remains idempotent even if wall-clock time differs;
- provider retry/replay cannot multiply persistent effects;
- if a replay somehow produced a different assistant wording for the same turn identity, the first successfully stored event remains authoritative for that episodic record;
- a later separate turn naturally has a different game time and therefore a different ID.

## Memory2DialogueIngestor

A small persistence bridge accepts:

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

It converts through `DialogueMemoryAdapter` and appends through existing `MemoryEventStore`.

No Minecraft entity access, provider access, prompt logic or relationship mutation belongs in this class.

## OpenAIChatAI lifecycle integration

The snapshot-aware direct path currently reaches post-success logic only after a provider response is parsed.

Integration rule:

```text
response.error == null
AND response.answer != null
AND response.answer.message != null/nonblank
→ existing dialogue history persistence
→ Memory 2.0 dialogue ingestion (fail-soft)
→ existing command / relationship handling
```

The existing legacy `rememberDialogue(...)` behavior is preserved exactly.

Memory 2.0 ingestion is separate and fail-soft:

- only when `memory2Enabled`;
- use `snapshot.worldRoot()`, `snapshot.villagerId()`, `snapshot.playerId()`, `snapshot.gameTime()`;
- max bound from `memory2MaxEventsPerNpc`;
- `System.currentTimeMillis()` only for metadata, never identity;
- catch/log Memory 2.0 failure without changing the visible reply, command execution, relationship delta or legacy memory result.

## Security/truth boundary

- Player text is not a fact merely because it was spoken.
- NPC-generated text is not a fact merely because the model said it.
- The whole dialogue episode remains `BELIEF`/`PLAYER_TOLD` for prompt purposes.
- No instructions embedded in stored dialogue gain authority; later context formatting already treats memories as data, never instructions.
- No raw provider reasoning or structured metadata enters the event summary.

## Non-goals

- LLM summarization;
- semantic extraction of facts from dialogue;
- promotion of claims to `SYSTEM_OBSERVED`;
- relationship-reason generation;
- working-memory orchestration;
- forgetting/decay;
- semantic duplicate merging;
- migration/removal of legacy `memory.json`;
- classic/non-snapshot MCA ChatAI integration.

## Testing

Tests must prove:

1. exact `DIALOGUE` / `PLAYER_TOLD` mapping and fixed scores;
2. speaker-labeled whitespace-normalized summary;
3. 240-code-point per-speaker storage bounds including Unicode safety;
4. missing/blank messages are rejected;
5. same turn identity produces the same event UUID across different reply text and wall-clock timestamps;
6. different game time or player message produces a different event UUID;
7. duplicate ingestion is idempotent in `MemoryEventStore`;
8. distinct successful turns remain distinct and retention remains bounded;
9. stored event participants are NPC + player;
10. integration diff proves no Memory 2.0 call exists on provider-error/empty-message paths.

## Success criteria

- successful usable snapshot-aware dialogue turns create bounded actor-owned `DIALOGUE` MemoryEvents;
- failed/empty turns create none;
- dialogue content remains belief data, never authoritative facts;
- duplicate/replay cannot multiply the same turn memory;
- legacy `memory.json` behavior remains unchanged;
- no LLM summarization or new dependency is introduced;
- exact-final-head unit tests, Fabric package verification, and Fabric/NeoForge CI pass.
