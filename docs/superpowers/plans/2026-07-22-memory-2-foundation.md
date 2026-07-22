# Memory 2.0 Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the first provider-independent Memory 2.0 persistence substrate: immutable `MemoryEvent` plus bounded, idempotent, fail-open per-NPC storage.

**Architecture:** Keep current `memory.json` dialogue behavior untouched. Add `memory2.json` as a separate versioned auxiliary store, with no prompt integration or migration yet. Model provenance and confidence explicitly so later retrieval/consolidation can distinguish server-observed facts from told/inferred beliefs.

**Tech Stack:** Java 21, JUnit 5, Gson, Gradle multi-module build, GitHub Actions, Fabric 1.21.1 with NeoForge compile compatibility.

## Global Constraints

- Minecraft target remains `1.21.1`.
- Java remains `21`.
- Preserve mod id `mca`.
- Preserve package root `net.conczin.mca`.
- Preserve `config/livingworld.json` and existing `<world>/livingworld/memory.json` behavior.
- New storage path is exactly `<world>/livingworld/memory2.json`.
- No LLM/provider calls, prompt injection, migration or relationship mutation in this slice.
- Writes use atomic temp-file + replace semantics.
- Corrupt auxiliary JSON fails open.

---

### Task 1: Immutable MemoryEvent domain

**Files:**
- Create: `common/src/main/java/net/conczin/mca/livingworld/memory2/MemoryEvent.java`
- Create: `common/src/test/java/net/conczin/mca/livingworld/memory2/MemoryEventTest.java`

**Interfaces:**
- Produces `MemoryEvent` record with nested `Type` and `Provenance` enums.

- [ ] Write RED tests covering score clamping, participant/reason normalization and required fields.
- [ ] Confirm focused test fails because `MemoryEvent` does not exist.
- [ ] Implement the immutable record with:
  - required `id`, `ownerNpcId`, `type`, nonblank `summary`, `provenance`;
  - defensive immutable normalized lists;
  - `importance` clamp `0..100`;
  - `emotionalWeight` clamp `-100..100`;
  - `confidence` clamp `0..100`.
- [ ] Re-run focused tests and confirm GREEN.

### Task 2: Persistent bounded MemoryEventStore

**Files:**
- Create: `common/src/main/java/net/conczin/mca/livingworld/memory2/MemoryEventStore.java`
- Create: `common/src/test/java/net/conczin/mca/livingworld/memory2/MemoryEventStoreTest.java`

**Interfaces:**
- `public static MemoryEventStore forWorld(Path worldRoot)`
- package-private `MemoryEventStore(Path file)` for tests
- `public synchronized void append(MemoryEvent event, int maxEventsPerNpc)`
- `public synchronized List<MemoryEvent> getRecent(UUID npcId, int maxResults)`

- [ ] Write RED tests for persistence/recreation, per-NPC isolation, bounded newest retention, idempotent event IDs and corrupt-file recovery.
- [ ] Confirm focused test fails because store does not exist.
- [ ] Implement version-1 Gson persistence at `<world>/livingworld/memory2.json`.
- [ ] Store events by owner NPC UUID.
- [ ] `append` ignores duplicate event IDs for the same NPC and enforces `maxEventsPerNpc >= 1`.
- [ ] `getRecent` returns immutable newest-first data and never exposes another NPC's events.
- [ ] Load malformed/unreadable JSON as an empty store.
- [ ] Save through sibling `.tmp`, `ATOMIC_MOVE` where supported, fallback `REPLACE_EXISTING`.
- [ ] Re-run focused tests and confirm GREEN.

### Task 3: Documentation and canonical state

**Files:**
- Create: `docs/livingworld/MEMORY_2.md`
- Modify: `docs/PROJECT_STATE.md`

- [ ] Document the separation between legacy dialogue `memory.json` and new `memory2.json`.
- [ ] Document provenance semantics and the rule that stored claims are not automatically authoritative facts.
- [ ] Record published `0.1.8+1.21.1` release:

```text
release commit: 23fba1ee373e932c0b17aba3755f8ac478c26941
release workflow run: 29918008438
workflow conclusion: success
```

- [ ] Mark `0.2 Memory 2.0` as **started / foundation implemented** only after this PR merges.
- [ ] Set next slice to bounded retrieval/ranking and controlled conversion from authoritative events/relationship reasons before any LLM-driven consolidation.

### Task 4: Full verification and PR

- [ ] Run/require `:common:test`.
- [ ] Run/require Fabric build and distributable package smoke-check.
- [ ] Require official Gradle PR CI for Fabric + NeoForge.
- [ ] Review exact final diff for accidental changes to current `memory.json`, provider paths, compatibility identifiers or gameplay behavior.
- [ ] Require no unresolved review threads before merge.
