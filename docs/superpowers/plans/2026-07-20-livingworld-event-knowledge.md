# LivingWorld Event / Knowledge Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a bounded world-local journal of server-verified events and inject only recent nearby events into immutable LivingWorld context snapshots.

**Architecture:** Keep factual world events separate from conversation memory and future beliefs/rumors. Persist immutable server-generated events in a bounded JSON store, query by dimension/radius/age on the Minecraft server thread, and emit the first events only from successful whitelisted NPC actions.

**Tech Stack:** Java 21, Gson, Minecraft 1.21.1, MCA ChatAI/ContextSnapshot, JUnit 5.

## Global Constraints

- No additional service/database.
- No new required user configuration.
- Player/LLM text is never promoted directly to factual events.
- Event I/O is fail-open for gameplay and AI conversation.
- Context queries are bounded by age, dimension, distance and count.
- Full Fabric/NeoForge CI must pass before merge.

---

### Task 1: Immutable event model and bounded store

**Files:**
- Create: `common/src/main/java/net/conczin/mca/livingworld/knowledge/WorldEvent.java`
- Create: `common/src/main/java/net/conczin/mca/livingworld/knowledge/WorldEventStore.java`
- Test: `common/src/test/java/net/conczin/mca/livingworld/knowledge/WorldEventStoreTest.java`

**Produces:** `WorldEventStore.forWorld(Path)`, `append(WorldEvent,int)`, `queryRecent(String,int,int,int,long,long,double,int)`.

- [ ] Define immutable event fields: id, type, description, provenance, dimension, x/y/z, gameTime, actorId, subjectId.
- [ ] Write persistence/reload and max-size tests.
- [ ] Write dimension/radius/age/count filtering tests with newest-first ordering.
- [ ] Implement synchronized Gson JSON store with temp + atomic replace.
- [ ] Run `:common:test` through CI after integration.

### Task 2: Zero-config defaults and snapshot integration

**Files:**
- Modify: `common/src/main/java/net/conczin/mca/livingworld/LivingWorldConfig.java`
- Modify: `common/src/main/java/net/conczin/mca/livingworld/context/LivingWorldContextCapture.java`
- Modify: `common/src/test/java/net/conczin/mca/livingworld/LivingWorldConfigTest.java`

**Consumes:** `WorldEventStore.queryRecent(...)`.

- [ ] Add defaults: enabled=true, maxEvents=512, maxAgeTicks=72000, contextRadius=32, contextMaxEvents=8.
- [ ] Assert defaults in config tests.
- [ ] Query only from `capture(...)` on server thread using villager dimension/position/game time/world root.
- [ ] Add deterministic `Observed recent local event: ...` facts to `worldFacts`.
- [ ] Catch store/runtime errors and continue snapshot creation without events.

### Task 3: Safe-action event emission

**Files:**
- Modify: `common/src/main/java/net/conczin/mca/entity/ai/chatAI/TriggerCommandInfo.java`
- Create: `common/src/main/java/net/conczin/mca/livingworld/knowledge/WorldEventRecorder.java`
- Test: pure formatter/descriptor tests where possible.

**Consumes:** server-thread action wrapper already present in `TriggerCommandInfo.call`.

- [ ] Emit only after action preconditions are revalidated and action callback completes successfully.
- [ ] Map known whitelist command names to deterministic descriptions; unknown commands do not create events.
- [ ] Record dimension, villager position, game time, player UUID and villager UUID on server thread.
- [ ] Event persistence failure logs a warning and never rolls back the completed action.

### Task 4: Documentation, audit status and verification

**Files:**
- Create: `docs/livingworld/EVENTS.md`
- Modify: `docs/UPSTREAM_ISSUE_AUDIT.md`

- [ ] Document truth/provenance boundary and world file location.
- [ ] Mark #1243 event-awareness foundation as partially implemented while rumors/NPC propagation remain future work.
- [ ] Open focused PR.
- [ ] Require `LivingWorld CI` success and official Fabric + NeoForge Gradle build success before merge.
