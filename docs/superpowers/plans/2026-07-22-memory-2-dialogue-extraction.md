# Memory 2.0 Controlled Dialogue Extraction Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Persist one bounded conservative `DIALOGUE` MemoryEvent for each successful usable snapshot-aware player↔NPC OpenAI turn without LLM summarization or truth promotion.

**Final architecture:** Add a pure deterministic `DialogueMemoryAdapter`, an idempotent `Memory2DialogueIngestor`, then invoke it from the compact snapshot-aware `ChatAI` orchestration layer only after `OpenAIChatAI.answer(...)` returns a present nonblank result. Keep `OpenAIChatAI`, provider/retry/parser behavior, legacy `memory.json`, command handling, and relationship handling unchanged.

**Tech Stack:** Java 21, JUnit 5, existing MemoryEvent/MemoryEventStore/LivingWorldContextSnapshot, Gradle/GitHub Actions.

## Global Constraints

- Snapshot-aware OpenAI path only.
- `MemoryEvent.Type.DIALOGUE`.
- `MemoryEvent.Provenance.PLAYER_TOLD`.
- Player/NPC utterance max: 240 Unicode code points each in stored summary.
- `importance=40`, `emotionalWeight=0`, `confidence=60`.
- Deterministic UUID excludes wall-clock timestamp and NPC reply.
- No event for provider error, empty/null/sanitized unusable response, blank result, or disabled Memory 2.0.
- No LLM summarization or fact extraction.
- Do not change legacy `<world>/livingworld/memory.json` behavior.
- Do not modify `OpenAIChatAI` in the final implementation.

---

### Task 1: Pure DialogueMemoryAdapter

**Files:**
- Create: `common/src/main/java/net/conczin/mca/livingworld/memory2/DialogueMemoryAdapter.java`
- Create: `common/src/test/java/net/conczin/mca/livingworld/memory2/DialogueMemoryAdapterTest.java`

- [x] Write RED tests for mapping, provenance/scores, normalization, Unicode code-point bounds, invalid input and deterministic identity.
- [x] Confirm valid RED because adapter does not exist.
- [x] Implement deterministic normalization and summary formatting.
- [x] ID canonical input:

```text
memory2-dialogue-v1\n<npcId>\n<playerId>\n<gameTime>\n<full normalized player message>
```

- [x] Use `UUID.nameUUIDFromBytes(... UTF_8 ...)`.
- [x] Confirm same turn/player text gives same ID despite different NPC reply/createdAt.
- [x] Confirm focused GREEN.

### Task 2: Idempotent dialogue persistence bridge

**Files:**
- Create: `common/src/main/java/net/conczin/mca/livingworld/memory2/Memory2DialogueIngestor.java`
- Create: `common/src/test/java/net/conczin/mca/livingworld/memory2/Memory2DialogueIngestorTest.java`

- [x] Write tests for duplicate replay idempotency, distinct turn retention and max-per-NPC bound.
- [x] Implement adapter → `MemoryEventStore.forWorld(worldRoot).append(...)`.
- [x] Add explicit `recordIfEnabled(...)` lifecycle guard.
- [x] Confirm RED for missing guard before implementation.
- [x] Confirm GREEN after guard implementation.

### Task 3: Snapshot-aware post-success integration

**Files:**
- Modify: `common/src/main/java/net/conczin/mca/entity/ai/chatAI/ChatAI.java`
- Verify unchanged: `common/src/main/java/net/conczin/mca/entity/ai/chatAI/OpenAIChatAI.java`

- [x] Keep provider/retry/parser lifecycle unchanged.
- [x] Keep existing `OpenAIChatAI` post-success behavior unchanged.
- [x] Capture the returned `Optional<String>` in snapshot-aware `ChatAI.answer(...)`.
- [x] Record only when the result is present and nonblank.
- [x] Use `memory2Enabled`, snapshot worldRoot/villagerId/playerId/gameTime, configured max retention, and wall clock only as metadata.
- [x] Wrap Memory 2.0 ingestion in a dedicated fail-soft helper/catch.
- [x] Return the original Optional unchanged.
- [x] Leave non-snapshot and Inworld fallback paths unchanged.

### Task 4: Documentation

**Files:**
- Modify: `docs/livingworld/MEMORY_2.md`
- Update this design/plan to match final `ChatAI` orchestration hook.
- Update `docs/PROJECT_STATE.md` in an immediate post-merge sync with actual merge SHA.

- [ ] Document conservative `PLAYER_TOLD/BELIEF` dialogue provenance.
- [ ] Document deterministic bounded summary and event ID semantics.
- [ ] Document only usable successful snapshot-aware OpenAI turns are ingested.
- [ ] Document legacy memory/provider lifecycle remains separate.
- [ ] Set next slice to validated relationship-reason provenance or working-memory/duplicate/consolidation design.

### Task 5: Verification

- [x] Preserve valid RED evidence from tests-only head after one external Modrinth 502 rerun.
- [x] Preserve lifecycle-guard RED evidence.
- [x] Confirm code-head `VillAIgence CI` success after lifecycle wiring.
- [x] Confirm code-head official NeoForge/Fabric Gradle CI success after lifecycle wiring.
- [ ] Require exact-final-head `VillAIgence CI` success after documentation.
- [ ] Require exact-final-head official NeoForge/Fabric Gradle CI success after documentation.
- [ ] Final diff review: no event creation on absent/blank result paths, no `OpenAIChatAI` diff, no parser/retry changes, no legacy-memory format changes.
- [ ] Confirm no unresolved review threads/comments before merge.
