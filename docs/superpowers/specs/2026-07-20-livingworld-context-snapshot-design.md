# LivingWorld Context Snapshot Design

## Goal

Make the direct LivingWorld/OpenAI voice conversation path thread-safe and more factual by capturing all Minecraft world/entity context on the Minecraft server thread before asynchronous LLM work begins.

## Scope

This milestone targets the confirmed unsafe path used by `VoiceConversationService -> ChatAI -> OpenAIChatAI` when LivingWorld's direct OpenAI provider is active.

It does not redesign the legacy Inworld integration or every historical MCA chat caller in one change. Those paths must remain backward compatible and can be migrated after a complete caller audit.

## Architecture

Introduce an immutable `LivingWorldContextSnapshot` in `common`.

The snapshot is captured while already running on the Minecraft server thread and contains only immutable/copied data needed by the asynchronous AI request:

- player/villager UUID and display name;
- existing MCA context lines produced by personality, traits, relationships, village, environment, and player modules;
- world seed/session identifiers required by the prompt;
- villager age safety flags and relative/family safety facts;
- dimension, block position, biome, time/weather;
- player held/equipped item facts;
- bounded villager inventory facts;
- immutable descriptors for currently available whitelisted AI actions;
- world-root path and game time needed by persistent/legacy conversation memory without reading entities later.

`VoiceConversationService` captures this snapshot before switching back to its AI executor.

`ChatAI` gets an overload that can pass the snapshot to `OpenAIChatAI`; other strategies preserve the existing interface/fallback behavior.

`OpenAIChatAI` uses the snapshot for prompt construction and memory identifiers. HTTP stays on the AI worker. Any client message or action application is scheduled back onto the Minecraft server thread and revalidated against the current player/NPC state.

## Data flow

`voice packet -> STT async -> server.execute(validate player/NPC + capture snapshot) -> AI executor(OpenAI request using snapshot) -> server.execute(apply message/action) -> TTS async -> server.execute(spatial playback)`

## Factuality

The snapshot adds explicit world facts to reduce hallucinations reported upstream (#1314): current dimension/location/biome/time/weather, held/equipped items, and a bounded NPC inventory summary.

The LLM is told these are observed facts. Absence from the bounded snapshot must not be interpreted as proof that an item/entity can never exist elsewhere.

## Thread safety

After snapshot capture, direct LivingWorld/OpenAI prompt construction must not dereference `ServerPlayer`, `VillagerEntityMCA`, `Level`, brain, relationships, inventory, advancements, or other mutable Minecraft state on the AI worker.

Persistent memory file I/O remains off the server thread and uses immutable world path + UUIDs from the snapshot.

Actions remain hard-whitelisted and are revalidated/executed on the server thread.

## Failure behavior

If snapshot capture cannot complete because player/NPC state disappeared, the voice request is dropped cleanly before any LLM request.

If optional factual fields cannot be resolved, use bounded `unknown`/empty facts rather than failing the conversation.

Provider/memory failures retain current fail-open/logging behavior and must not crash the server.

## Testing

Add pure tests for snapshot formatting/bounds and prompt facts without requiring a live Minecraft world where possible.

Add regression checks that the snapshot data is immutable/copied and inventory summaries are bounded.

Full Gradle verification remains required when a dependency-resolving runner is available; until then, independently compile/test pure snapshot components and inspect Minecraft API-sensitive call sites before merge.
