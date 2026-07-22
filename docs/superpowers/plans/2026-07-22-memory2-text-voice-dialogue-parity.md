# Memory 2.0 Text/Voice Dialogue Parity Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make successful ordinary OpenAI text chat and successful snapshot/voice chat persist identical bounded `DIALOGUE` Memory 2.0 events through one shared post-success lifecycle.

**Architecture:** Add a Minecraft-independent `Memory2DialogueLifecycle` that validates successful answers and delegates to existing `Memory2DialogueIngestor`. Refactor `ChatAI` so both OpenAI classic text and snapshot-aware paths call this one lifecycle with immutable worldRoot/NPC/player/gameTime coordinates while preserving their existing provider behavior.

**Tech Stack:** Java 21, JUnit 5, existing `ChatAI`, `Memory2DialogueIngestor`, `MemoryEventStore`, Gradle/GitHub Actions.

## Global Constraints

- No `MemoryEvent` schema change.
- No config version change.
- No `OpenAIChatAI` provider/parser/retry/prompt changes.
- No STT/TTS pipeline changes.
- No relationship/action behavior changes.
- Legacy `memory.json` behavior remains unchanged.
- Inworld/non-OpenAI classic path remains unchanged.
- Successful text and snapshot/voice turns must each produce at most one Memory 2.0 `DIALOGUE` event.
- Memory 2.0 persistence failure must not replace/remove the already-produced answer.

---

### Task 1: Shared post-success dialogue-memory lifecycle

**Files:**
- Create: `common/src/main/java/net/conczin/mca/livingworld/memory2/Memory2DialogueLifecycle.java`
- Create: `common/src/test/java/net/conczin/mca/livingworld/memory2/Memory2DialogueLifecycleTest.java`

**Interfaces:**

```java
public static void recordSuccessful(
    boolean enabled,
    Path worldRoot,
    UUID npcId,
    UUID playerId,
    long gameTime,
    String playerMessage,
    Optional<String> answer,
    int maxEventsPerNpc,
    long createdAtEpochMillis
)
```

- [ ] Add RED tests proving a present nonblank answer persists exactly one `DIALOGUE` event with existing `PLAYER_TOLD` semantics.
- [ ] Add RED tests proving `Optional.empty()`, blank answer, null answer wrapper, and `enabled=false` persist nothing.
- [ ] Add RED replay test proving identical NPC/player/gameTime/player message remains one event even when reply/wall clock differs, preserving current first-write/idempotent behavior.
- [ ] Run `VillAIgence CI` on tests-only head; expected RED is `:common:compileTestJava` because `Memory2DialogueLifecycle` does not exist.
- [ ] Implement minimal helper: validate success, then call `Memory2DialogueIngestor.recordIfEnabled(...)` with `answer.get()`.
- [ ] Run exact code-head CI to GREEN.

---

### Task 2: Route classic text and snapshot/voice through the same lifecycle

**Files:**
- Modify: `common/src/main/java/net/conczin/mca/entity/ai/chatAI/ChatAI.java`
- Modify: `docs/livingworld/MEMORY_2.md`

**Classic text target flow:**

```text
strategy = computeStrategyIfAbsent(...)
openConversation(...)
if OpenAI strategy:
  capture worldRoot + villagerId + playerId + gameTime
answer = existing strategy.answer(player, villager, msg)
if OpenAI strategy:
  shared Memory2DialogueLifecycle.recordSuccessful(...)
return original answer unchanged
```

`worldRoot` uses `player.serverLevel().getServer().getWorldPath(LevelResource.ROOT)` and `gameTime` uses the originating villager level game time captured before result ingestion.

**Snapshot target flow:**

```text
OpenAIChatAI.answer(...snapshot)
→ shared Memory2DialogueLifecycle.recordSuccessful(
    snapshot.worldRoot(), snapshot.villagerId(), snapshot.playerId(), snapshot.gameTime(), ...)
→ return original answer unchanged
```

- [ ] Replace the current private direct `Memory2DialogueIngestor` call with `Memory2DialogueLifecycle`.
- [ ] Add the same shared post-success call to classic text **only when strategy is `OpenAIChatAI`**.
- [ ] Preserve classic Inworld path exactly: no new Memory 2.0 event.
- [ ] Preserve `openConversation(...)`, original strategy invocation, return value, and fail-soft semantics.
- [ ] Keep one `try/catch` orchestration boundary around auxiliary Memory 2.0 persistence so a persistence failure only logs.
- [ ] Review the `ChatAI` diff: no provider/parser/retry code; no voice STT/TTS code; no duplicate direct `Memory2DialogueIngestor` append remains.
- [ ] Update `MEMORY_2.md` with text/voice parity and explicitly state this patch does **not** reroute classic text through snapshot prompt/provider semantics.
- [ ] Run fresh exact-final-head `VillAIgence CI`: `:common:test`, Fabric build, distributable package verification must all succeed.
- [ ] Run fresh exact-final-head Java PR CI: NeoForge and Fabric builds must succeed.
- [ ] Confirm no unresolved review threads/comments and merge with pinned expected head SHA.
- [ ] Immediately sync `docs/PROJECT_STATE.md` in a separate docs-only PR using the actual merge SHA.

## Manual acceptance after patch build

```text
text → NPC A
voice → NPC A
text → NPC B
restart
```

Expected:

- `memory.json` continues recording existing dialogue history;
- `memory2.json` contains unique `DIALOGUE` events from both text and voice;
- NPC A/B memories remain isolated;
- replay does not multiply identical event IDs;
- restart preserves `memory.json` and `memory2.json`;
- voice STT/Chat/TTS behavior remains unchanged.