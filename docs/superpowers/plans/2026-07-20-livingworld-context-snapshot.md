# LivingWorld Context Snapshot Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make direct LivingWorld/OpenAI voice conversations use an immutable Minecraft context captured on the server thread before asynchronous LLM processing.

**Architecture:** Add a small immutable snapshot model plus a Minecraft capture adapter. Voice captures the snapshot during its existing server-thread revalidation step, then passes it through a snapshot-aware `ChatAI` overload to `OpenAIChatAI`. Legacy strategy behavior remains available unchanged.

**Tech Stack:** Java 21, Minecraft 1.21.1 Mojang/Parchment mappings, MCA ChatAI modules, JUnit 5.

## Global Constraints

- Never read mutable Minecraft entity/world/brain/relationship/inventory/advancement state from the LivingWorld AI worker after snapshot capture.
- STT/LLM/TTS and persistent-memory file I/O stay off the Minecraft server thread.
- Actions and client-visible error/application effects run on the Minecraft server thread and are revalidated.
- Preserve legacy MCA/Inworld behavior and configuration fallback.
- Keep factual context bounded; never dump arbitrary world/chunk state into the LLM.

---

### Task 1: Immutable snapshot model and factual formatting

**Files:**
- Create: `common/src/main/java/net/conczin/mca/livingworld/context/LivingWorldContextSnapshot.java`
- Create: `common/src/main/java/net/conczin/mca/livingworld/context/WorldFactFormatter.java`
- Test: `common/src/test/java/net/conczin/mca/livingworld/context/WorldFactFormatterTest.java`

**Produces:** immutable snapshot fields, immutable `ActionDescriptor`, bounded item fact formatting.

- [ ] Write failing pure unit tests proving copied immutable lists and bounded item summaries.
- [ ] Implement the record/model and pure formatter with explicit limits.
- [ ] Run the focused tests when Gradle is available; independently Java-compile pure components otherwise.

### Task 2: Minecraft server-thread capture adapter

**Files:**
- Create: `common/src/main/java/net/conczin/mca/livingworld/context/LivingWorldContextCapture.java`

**Consumes:** `LivingWorldContextSnapshot`, existing MCA context modules and `TriggerCommandInfos`.

**Produces:** `LivingWorldContextSnapshot capture(ServerPlayer player, VillagerEntityMCA villager)`.

- [ ] Capture names/UUIDs, context-module output, age/relative safety facts, world seed/game time/world root, dimension/position/biome/time/weather, bounded equipment/inventory facts, language, and currently available action descriptors.
- [ ] Copy all collections before returning so the AI worker receives no live Minecraft-backed collection.
- [ ] Keep optional fact failures local: missing biome/item details become `unknown`/empty facts rather than aborting the conversation.

### Task 3: Snapshot-aware ChatAI/OpenAI path

**Files:**
- Modify: `common/src/main/java/net/conczin/mca/entity/ai/chatAI/ChatAI.java`
- Modify: `common/src/main/java/net/conczin/mca/entity/ai/chatAI/OpenAIChatAI.java`
- Modify: `common/src/main/java/net/conczin/mca/livingworld/memory/PersistentChatMemory.java`

**Consumes:** immutable `LivingWorldContextSnapshot`.

**Produces:** snapshot-aware answer overload used only when a snapshot is supplied and the selected strategy is `OpenAIChatAI`.

- [ ] Add a `ChatAI.answer(..., LivingWorldContextSnapshot snapshot, MinecraftServer server)` overload while preserving the existing method.
- [ ] Add path/UUID overloads to persistent memory so snapshot-mode memory I/O never dereferences player/villager entities.
- [ ] Refactor `OpenAIChatAI` snapshot-mode prompt construction to use snapshot values/context lines/action descriptors only.
- [ ] Append explicit factual world context with wording that distinguishes observed facts from unknown world state.
- [ ] Schedule client-visible errors and action revalidation/application through `server.execute(...)` in snapshot mode.
- [ ] Keep existing legacy `answer(ServerPlayer, VillagerEntityMCA, String)` behavior as fallback.

### Task 4: Voice bridge integration

**Files:**
- Modify: `fabric/src/main/java/net/conczin/mca/fabric/livingworld/voice/VoiceConversationService.java`

- [ ] During the existing post-STT `server.execute(...)` target revalidation, capture `LivingWorldContextSnapshot` before returning to the AI executor.
- [ ] Pass the snapshot + `MinecraftServer` into the new `ChatAI.answer` overload.
- [ ] Ensure player/NPC disconnect/removal still releases busy gates and drops the request cleanly.

### Task 5: Verification and documentation

**Files:**
- Create: `docs/livingworld/CONTEXT.md`
- Update: `docs/UPSTREAM_ISSUE_AUDIT.md` if implementation changes #1314/#1243 status.

- [ ] Review the diff for any `player.*`, `villager.*`, `level.*`, brain/relationship/inventory/advancement reads remaining in the snapshot-mode AI worker path.
- [ ] Independently compile pure snapshot/formatter components on Java 21.
- [ ] Run `./gradlew :common:test :fabric:build --stacktrace --no-daemon` when a dependency-resolving runner is available; do not claim it passed unless it actually ran successfully.
- [ ] Open a focused PR, verify mergeability, perform final review, then merge autonomously if no critical issue remains.
