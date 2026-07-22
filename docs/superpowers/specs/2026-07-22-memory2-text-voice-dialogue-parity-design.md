# Memory 2.0 Text/Voice Dialogue Parity Design

## Goal

Make successful ordinary text chat and successful voice/snapshot chat create the same bounded `DIALOGUE` Memory 2.0 event semantics without changing provider, prompt, parser, retry, command, relationship, TTS, or legacy `memory.json` behavior.

## Observed boundary

Current `ChatAI` has two dialogue paths:

```text
classic text ChatAI.answer(player, villager, msg)
→ strategy.answer(player, villager, msg)
→ legacy memory.json only

snapshot-aware ChatAI.answer(server, player, villager, msg, snapshot)
→ OpenAIChatAI.answer(...snapshot)
→ shared answer returned
→ Memory2DialogueIngestor
→ memory2.json
```

This creates transport-dependent long-term memory.

## Considered approaches

### A. Route classic text chat through the full snapshot-aware provider path

Pros: one end-to-end lifecycle.

Cons: changes far more than memory parity. Text chat would suddenly gain snapshot prompt/world facts, Memory 2.0 retrieval context, snapshot tools/action filtering and relationship semantics. This is too broad for a patch release.

### B. Add a second direct `Memory2DialogueIngestor` append in the classic method

Pros: smallest edit.

Cons: duplicates post-success rules and makes text/voice behavior drift again later.

### C. Shared post-success dialogue-memory lifecycle — chosen

Create one small Minecraft-independent helper that owns successful dialogue Memory 2.0 admission rules and delegates to the existing `Memory2DialogueIngestor`. Both `ChatAI` overloads call this same helper with immutable identifiers/root/game time.

This fixes parity while preserving existing provider behavior on both routes.

## Architecture

Introduce a small immutable input:

```text
DialogueMemoryContext
├── worldRoot
├── villagerId
├── playerId
└── gameTime
```

and a shared lifecycle helper:

```text
Memory2DialogueLifecycle.recordSuccessful(...)
```

Responsibilities:

- require `memory2Enabled=true`;
- require non-null context;
- require a present, nonblank NPC reply;
- keep player message/reply mapping delegated to existing `Memory2DialogueIngestor`;
- use the same `memory2MaxEventsPerNpc` bound;
- catch no Minecraft/provider concerns;
- remain deterministic/idempotent through the existing dialogue event identity.

`ChatAI` remains the orchestration boundary that catches persistence failures and never changes the already-produced answer.

## Classic text path

For `OpenAIChatAI` strategies only:

```text
openConversation(...)
→ capture minimal immutable dialogue-memory coordinates
   (worldRoot, villagerId, playerId, gameTime)
→ existing strategy.answer(player, villager, msg)
→ existing provider/parser/retry/legacy memory behavior unchanged
→ shared Memory2DialogueLifecycle.recordSuccessful(...)
→ return original Optional unchanged
```

Inworld/non-OpenAI classic strategies remain unchanged and do not gain Memory 2.0 ingestion in this patch.

The minimal context is captured before the provider result is processed so event identity uses the originating Minecraft game time rather than a later wall-clock-dependent state.

## Snapshot/voice path

Keep the existing snapshot-aware provider path unchanged. Replace only the current private `rememberMemory2Dialogue(...)` implementation with the same shared lifecycle helper using fields already present in `LivingWorldContextSnapshot`.

No second append is allowed. One successful turn must yield at most one `DIALOGUE` event.

## Event semantics

No schema or mapping change:

```text
type = DIALOGUE
provenance = PLAYER_TOLD
participants = [villagerId, playerId]
importance = 40
emotionalWeight = 0
confidence = 60
relationshipReasons = []
```

The server records that the conversation occurred. Dialogue content remains belief/episodic data and is not upgraded to authoritative world truth.

## Failure boundaries

- provider failure / empty result / blank result → no Memory 2.0 event;
- `memory2Enabled=false` → no Memory 2.0 event;
- Memory 2.0 persistence failure → log only, original text/voice answer unchanged;
- legacy `memory.json` behavior remains unchanged;
- no migration is performed;
- no duplicate event may be created by the shared hook.

## Compatibility

No config version change.
No persistent schema change.
No release-tag movement.
No change to `OpenAIChatAI` provider/parser/retry contracts.
No change to voice STT/TTS pipeline.
No change to relationship/action behavior.

## Tests / acceptance

Automated tests must prove:

1. shared lifecycle persists one successful text-like turn;
2. empty/blank/disabled cases persist nothing;
3. exact replay remains idempotent;
4. retention remains bounded through existing store behavior;
5. both `ChatAI` overloads use the shared lifecycle rather than separate Memory 2.0 append logic;
6. exact-head unit/Fabric/package and NeoForge/Fabric CI are green.

Manual acceptance for the next patch build:

```text
text → NPC A
voice → NPC A
text → NPC B
restart
```

Expected: both transports appear as bounded `DIALOGUE` Memory 2.0 events, NPC isolation is preserved, no duplicates appear, and `memory.json`/`memory2.json` survive restart.