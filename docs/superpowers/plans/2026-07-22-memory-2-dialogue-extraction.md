# Memory 2.0 Controlled Dialogue Extraction Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Persist one bounded conservative `DIALOGUE` MemoryEvent for each successful usable snapshot-aware player↔NPC AI turn without LLM summarization or truth promotion.

**Architecture:** Add a pure deterministic `DialogueMemoryAdapter`, an idempotent `Memory2DialogueIngestor`, then invoke it only from the existing successful snapshot-aware response path after a nonblank visible message exists. Keep legacy `memory.json` behavior unchanged and make Memory 2.0 ingestion independently fail-soft.

**Tech Stack:** Java 21, JUnit 5, existing MemoryEvent/MemoryEventStore/LivingWorldContextSnapshot, Gradle/GitHub Actions.

## Global Constraints

- Snapshot-aware direct path only.
- `MemoryEvent.Type.DIALOGUE`.
- `MemoryEvent.Provenance.PLAYER_TOLD`.
- Player/NPC utterance max: 240 Unicode code points each in stored summary.
- `importance=40`, `emotionalWeight=0`, `confidence=60`.
- Deterministic UUID excludes wall-clock timestamp and NPC reply.
- No event for provider error, empty/null/sanitized unusable response.
- No LLM summarization or fact extraction.
- Do not change legacy `<world>/livingworld/memory.json` behavior.

---

### Task 1: Pure DialogueMemoryAdapter

**Files:**
- Create: `common/src/main/java/net/conczin/mca/livingworld/memory2/DialogueMemoryAdapter.java`
- Create: `common/src/test/java/net/conczin/mca/livingworld/memory2/DialogueMemoryAdapterTest.java`

- [ ] Write RED tests for mapping, provenance/scores, normalization, Unicode code-point bounds, invalid input and deterministic identity.
- [ ] Confirm RED because adapter does not exist.
- [ ] Implement deterministic normalization and summary formatting.
- [ ] ID canonical input:

```text
memory2-dialogue-v1\n<npcId>\n<playerId>\n<gameTime>\n<full normalized player message>
```

- [ ] Use `UUID.nameUUIDFromBytes(... UTF_8 ...)`.
- [ ] Confirm same turn/player text gives same ID despite different NPC reply/createdAt.
- [ ] Confirm focused GREEN.

### Task 2: Idempotent dialogue persistence bridge

**Files:**
- Create: `common/src/main/java/net/conczin/mca/livingworld/memory2/Memory2DialogueIngestor.java`
- Create: `common/src/test/java/net/conczin/mca/livingworld/memory2/Memory2DialogueIngestorTest.java`

- [ ] Write RED tests for duplicate replay idempotency, distinct turn retention and max-per-NPC bound.
- [ ] Confirm RED.
- [ ] Implement adapter → `MemoryEventStore.forWorld(worldRoot).append(...)`.
- [ ] Confirm GREEN.

### Task 3: Snapshot-aware post-success integration

**Files:**
- Modify: `common/src/main/java/net/conczin/mca/entity/ai/chatAI/OpenAIChatAI.java`

- [ ] Keep provider/retry/parser lifecycle unchanged.
- [ ] In snapshot-aware success block, derive `visibleMessage = response.answer.message`.
- [ ] Keep existing `rememberDialogue(...)` semantics unchanged, including existing fallback behavior.
- [ ] Call Memory 2.0 dialogue ingestion only when visible message is nonnull/nonblank and `memory2Enabled=true`.
- [ ] Use snapshot worldRoot/villagerId/playerId/gameTime, configured `memory2MaxEventsPerNpc`, and wall clock only as metadata.
- [ ] Wrap Memory 2.0 ingestion in a dedicated fail-soft helper/catch.
- [ ] Do not call ingestion on provider-error/empty-response branches.
- [ ] Do not change command or relationship-delta ordering/semantics beyond adding the isolated memory side effect in the successful response block.

### Task 4: Documentation

**Files:**
- Modify: `docs/livingworld/MEMORY_2.md`
- Update `docs/PROJECT_STATE.md` in an immediate post-merge sync with actual merge SHA.

- [ ] Document conservative `PLAYER_TOLD/BELIEF` dialogue provenance.
- [ ] Document deterministic bounded summary and event ID semantics.
- [ ] Document only usable successful turns are ingested.
- [ ] Document legacy memory remains separate.
- [ ] Set next slice to validated relationship-reason provenance or working-memory/consolidation design.

### Task 5: Verification

- [ ] Require valid RED evidence from tests-only head.
- [ ] Require exact-final-head `VillAIgence CI` success.
- [ ] Require exact-final-head official NeoForge/Fabric Gradle CI success.
- [ ] Final diff review: no event creation on error/empty paths, no changes to parser/retry semantics, no legacy-memory format changes.
- [ ] Confirm no unresolved review threads/comments before merge.
