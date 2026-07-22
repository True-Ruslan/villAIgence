# Memory 2.0 Bounded Context Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Inject a small deterministic provenance-preserving Memory 2.0 set into the snapshot-aware NPC prompt without mixing beliefs into authoritative world facts.

**Architecture:** Add a pure formatter/provider layer, extend `LivingWorldContextSnapshot` with separate `memoryContext`, load it synchronously during server-thread snapshot capture, then add one truth-labeled memory section to the existing non-authoritative `contextLines` channel. Do not modify `OpenAIChatAI`; its existing prompt builder already emits `contextLines` before the later authoritative `worldFacts` section.

**Tech Stack:** Java 21, JUnit 5, existing Memory 2.0 classes, existing immutable context snapshot, Gradle/GitHub Actions.

## Global Constraints

- Do not modify legacy `<world>/livingworld/memory.json` behavior.
- Do not add LLM/provider calls for retrieval/formatting.
- Do not put Memory 2.0 entries into `worldFacts`.
- Do not modify `OpenAIChatAI` in this slice.
- Hard candidates: `32`.
- Hard results: `6`.
- Recency horizon: `168000` ticks.
- Formatted summary max: `240` Unicode code points.
- Current observed factual context wins over memory on conflict.

---

### Task 1: Provenance-preserving formatter

**Files:**
- Create: `common/src/main/java/net/conczin/mca/livingworld/memory2/MemoryContextFormatter.java`
- Create: `common/src/test/java/net/conczin/mca/livingworld/memory2/MemoryContextFormatterTest.java`

- [ ] Write RED tests for VERIFIED/BELIEF labels, sanitization, quote/backslash escaping, length cap, empty input and prompt truth instructions.
- [ ] Confirm tests fail before implementation.
- [ ] Implement deterministic line formatting and prompt-section generation.
- [ ] Confirm focused GREEN.

### Task 2: Bounded context provider

**Files:**
- Create: `common/src/main/java/net/conczin/mca/livingworld/memory2/Memory2ContextProvider.java`
- Create: `common/src/test/java/net/conczin/mca/livingworld/memory2/Memory2ContextProviderTest.java`

- [ ] Write RED tests proving max 6 results, current-player participant relevance, per-NPC isolation and empty path.
- [ ] Confirm RED.
- [ ] Implement:

```text
candidateLimit=32
maxResults=6
recencyHorizonTicks=168000
participants={current player}
preferredTypes={}
```

- [ ] Retrieve through existing `MemoryRetriever` and format through `MemoryContextFormatter`.
- [ ] Confirm GREEN.

### Task 3: Immutable snapshot separation and compatibility

**Files:**
- Modify: `common/src/main/java/net/conczin/mca/livingworld/context/LivingWorldContextSnapshot.java`
- Create: `common/src/test/java/net/conczin/mca/livingworld/context/LivingWorldContextSnapshotMemoryTest.java`

- [ ] Add `List<String> memoryContext` as a separate field from `worldFacts`.
- [ ] Defensively copy it in the canonical record constructor.
- [ ] Preserve the previous constructor signature as a delegating compatibility constructor with `memoryContext=List.of()`.
- [ ] Prove memory/world facts remain physically separate and immutable.
- [ ] Confirm compilation/tests GREEN.

### Task 4: Server-thread capture and prompt-channel integration

**Files:**
- Modify: `common/src/main/java/net/conczin/mca/livingworld/context/LivingWorldContextCapture.java`

- [ ] If `memory2Enabled=false`, use empty memory context.
- [ ] Otherwise load via `Memory2ContextProvider` using already captured `worldRoot`, villager/player UUIDs and game time.
- [ ] Catch runtime failure and log bounded warning; return empty memory context.
- [ ] Preserve raw formatted entries separately in `snapshot.memoryContext`.
- [ ] Build `MemoryContextFormatter.promptSection(memoryContext)` during capture and append it as one entry to existing `contextLines` only when non-empty.
- [ ] Do not add any Memory 2.0 entry to `worldFacts`.
- [ ] Verify existing `OpenAIChatAI.buildSnapshotSystem(...)` ordering remains unchanged: `contextLines` first, authoritative `worldFacts` later.

### Task 5: No provider/request-builder changes

**Files:**
- Verify unchanged: `common/src/main/java/net/conczin/mca/entity/ai/chatAI/OpenAIChatAI.java`

- [ ] Confirm no diff in `OpenAIChatAI`.
- [ ] Confirm existing legacy `pastDialogue` flow remains unchanged.
- [ ] Confirm current authoritative worldFacts wording still appears after the memory block and explicitly wins on conflict.

### Task 6: Documentation/state

**Files:**
- Modify: `docs/livingworld/MEMORY_2.md`
- Modify this spec/plan to match final architecture.
- Update `docs/PROJECT_STATE.md` in immediate post-merge sync with actual merge SHA.

- [ ] Document bounded context policy and prompt truth boundary.
- [ ] Document that provider/request builder remains unchanged; integration uses existing snapshot `contextLines`.
- [ ] Mark context integration implemented only after merge.
- [ ] Set next slice to controlled dialogue extraction or validated relationship-reason provenance, not embeddings/LLM consolidation.

### Task 7: Verification

- [ ] Preserve valid RED evidence from tests-only head after correcting any test-harness syntax defect.
- [ ] Require exact-final-head `VillAIgence CI` success.
- [ ] Require exact-final-head official NeoForge/Fabric Gradle CI success.
- [ ] Final diff review for truth hierarchy, prompt-injection hardening, source compatibility and no legacy-memory/provider changes.
- [ ] Confirm no unresolved review threads/comments before merge.
