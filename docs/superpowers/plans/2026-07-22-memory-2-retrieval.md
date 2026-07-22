# Memory 2.0 Deterministic Retrieval Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add hard-bounded deterministic Memory 2.0 retrieval using explicit relevance, recency, importance and confidence signals.

**Architecture:** Keep persistence unchanged. `MemoryQuery` normalizes bounded inputs, `MemoryRetriever` reads at most `candidateLimit` events from `MemoryEventStore`, computes inspectable integer scores and returns at most `maxResults` `RankedMemory` values with deterministic tie-breaking.

**Tech Stack:** Java 21, JUnit 5, existing `MemoryEvent`/`MemoryEventStore`, Gradle/GitHub Actions.

## Global Constraints

- No LLM/provider calls.
- No embeddings/vector database.
- No Minecraft entity/world access.
- No prompt/context integration in this slice.
- Do not modify legacy `<world>/livingworld/memory.json`.
- Candidate limit hard maximum: `512`.
- Score components and total remain integer `0..100`.

---

### Task 1: MemoryQuery contract

**Files:**
- Create: `common/src/main/java/net/conczin/mca/livingworld/memory2/MemoryQuery.java`
- Create: `common/src/test/java/net/conczin/mca/livingworld/memory2/MemoryQueryTest.java`

- [ ] Write RED tests for required NPC, immutable/de-duplicated sets and limit/time clamping.
- [ ] Confirm tests fail because `MemoryQuery` does not exist.
- [ ] Implement immutable normalized record.
- [ ] Confirm focused GREEN.

### Task 2: RankedMemory and deterministic scoring

**Files:**
- Create: `common/src/main/java/net/conczin/mca/livingworld/memory2/RankedMemory.java`
- Create: `common/src/main/java/net/conczin/mca/livingworld/memory2/MemoryRetriever.java`
- Create: `common/src/test/java/net/conczin/mca/livingworld/memory2/MemoryRetrieverTest.java`

- [ ] Write RED tests for relevance, recency, weighted ranking, hard bounds, tie-breaking and per-NPC isolation.
- [ ] Confirm tests fail before implementation.
- [ ] Implement relevance:

```text
specified participant dimension: overlap ? 100 : 0
specified type dimension: match ? 100 : 0
both specified: integer average
neither specified: 100
```

- [ ] Implement recency:

```text
age <= 0 -> 100
age >= horizon -> 0
otherwise -> 100 - (age*100/horizon)
```

- [ ] Implement total:

```text
(relevance*40 + importance*25 + recency*20 + confidence*15) / 100
```

- [ ] Implement deterministic tie-breaking from design.
- [ ] Retrieve only `candidateLimit` from store and return only `maxResults`.
- [ ] Confirm focused GREEN.

### Task 3: Documentation/state

**Files:**
- Modify: `docs/livingworld/MEMORY_2.md`
- Modify: `docs/PROJECT_STATE.md`

- [ ] Document retrieval algorithm, weights, bounds and truth-boundary semantics.
- [ ] Mark deterministic retrieval/ranking implemented after merge.
- [ ] Set next slice to controlled server-owned adapters from authoritative `WorldEvent` and explicit relationship reasons into `MemoryEvent`.

### Task 4: Verification

- [ ] Require RED evidence from tests-only head.
- [ ] Require fresh `VillAIgence CI` success on exact final head.
- [ ] Require fresh official Fabric/NeoForge Gradle CI success.
- [ ] Review final diff for accidental provider/gameplay/prompt/persistence changes.
- [ ] Require no unresolved review threads/comments before merge.
