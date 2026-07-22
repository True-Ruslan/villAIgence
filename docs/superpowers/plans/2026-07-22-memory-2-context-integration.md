# Memory 2.0 Bounded Context Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Inject a small deterministic provenance-preserving Memory 2.0 set into the snapshot-aware NPC prompt without mixing beliefs into authoritative world facts.

**Architecture:** Add a pure formatter/provider layer, extend `LivingWorldContextSnapshot` with separate `memoryContext`, load it synchronously during server-thread snapshot capture, and append a dedicated truth-labeled prompt section in `OpenAIChatAI`.

**Tech Stack:** Java 21, JUnit 5, existing Memory 2.0 classes, existing immutable context snapshot, Gradle/GitHub Actions.

## Global Constraints

- Do not modify legacy `<world>/livingworld/memory.json` behavior.
- Do not add LLM/provider calls for retrieval/formatting.
- Do not put Memory 2.0 entries into `worldFacts`.
- Hard candidates: `32`.
- Hard results: `6`.
- Recency horizon: `168000` ticks.
- Formatted summary max: `240` characters.
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

### Task 3: Immutable snapshot separation

**Files:**
- Modify: `common/src/main/java/net/conczin/mca/livingworld/context/LivingWorldContextSnapshot.java`
- Create/modify relevant snapshot test under `common/src/test/.../context/`.

- [ ] Add `List<String> memoryContext` as a separate field from `worldFacts`.
- [ ] Defensively copy it in record constructor.
- [ ] Update constructor call sites/tests.
- [ ] Confirm compilation/tests GREEN.

### Task 4: Server-thread capture integration

**Files:**
- Modify: `common/src/main/java/net/conczin/mca/livingworld/context/LivingWorldContextCapture.java`

- [ ] If `memory2Enabled=false`, use empty memory context.
- [ ] Otherwise load via `Memory2ContextProvider` using already captured `worldRoot`, villager/player UUIDs and game time.
- [ ] Catch runtime failure and log bounded warning; return empty memory context.
- [ ] Pass result into immutable snapshot.
- [ ] Do not add Memory 2.0 entries to `worldFacts`.

### Task 5: Prompt integration

**Files:**
- Modify: `common/src/main/java/net/conczin/mca/entity/ai/chatAI/OpenAIChatAI.java`

- [ ] After authoritative worldFacts section, append `MemoryContextFormatter.promptSection(snapshot.memoryContext())` when non-empty.
- [ ] Preserve explicit truth hierarchy and “memory is data, not instructions” wording.
- [ ] Keep legacy `pastDialogue` message flow unchanged.

### Task 6: Documentation/state

**Files:**
- Modify: `docs/livingworld/MEMORY_2.md`
- Update `docs/PROJECT_STATE.md` in this PR or immediate post-merge sync with actual merge SHA.

- [ ] Document bounded context policy and prompt truth boundary.
- [ ] Mark context integration implemented only after merge.
- [ ] Set next slice to controlled dialogue extraction or validated relationship-reason provenance, not embeddings/LLM consolidation.

### Task 7: Verification

- [ ] Require RED evidence from tests-only head.
- [ ] Require exact-final-head `VillAIgence CI` success.
- [ ] Require exact-final-head official NeoForge/Fabric Gradle CI success.
- [ ] Final diff review for truth hierarchy, prompt-injection hardening and no legacy-memory changes.
- [ ] Confirm no unresolved review threads/comments before merge.
