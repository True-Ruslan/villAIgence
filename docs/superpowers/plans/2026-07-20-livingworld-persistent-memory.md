# LivingWorld Persistent Memory Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Persist bounded direct-LivingWorld NPC/player conversation history across server restarts.

**Architecture:** Store world-local JSON behind `ConversationMemoryStore`; bridge it into existing `OpenAIChatAI` only when LivingWorld direct provider is configured. Preserve legacy MCA memory/backend behavior otherwise.

**Tech Stack:** Java 21, Gson, MCA ChatAI, JUnit 5.

## Global Constraints

- No additional service/database required.
- One-key AI setup remains unchanged.
- Raw audio is never persisted.
- Memory failure must not fail NPC dialogue.
- History is isolated by NPC UUID and player UUID.

---

### Task 1: File store

- [x] Define persistence/reload/isolation/bounds tests.
- [x] Implement `MemoryMessage` and synchronized `ConversationMemoryStore`.
- [x] Use temp-file + atomic replace where supported.

### Task 2: Configuration and bridge

- [x] Add zero-config memory defaults.
- [x] Add world-path bridge using Minecraft `LevelResource.ROOT`.
- [x] Add config regression assertions.

### Task 3: ChatAI integration

- [x] Load persistent NPC×player messages for configured direct LivingWorld.
- [x] Persist successful user/assistant exchanges.
- [x] Keep legacy MCA memory path unchanged when LivingWorld is not configured.
- [x] Fail open on memory read/write errors.

### Task 4: Documentation and verification

- [x] Document storage, backup, privacy, and limits.
- [ ] Run full Gradle tests/build when repository Actions/checkout is available.
- [ ] Final diff review and merge.
