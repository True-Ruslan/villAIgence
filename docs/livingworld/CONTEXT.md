# LivingWorld Context Snapshots

LivingWorld voice conversations capture an immutable context snapshot on the Minecraft server thread before the asynchronous LLM request starts.

## Why

Minecraft entities, brains, relationships, inventories, advancements, levels, and navigation state are mutable game state. Reading them from an AI/network worker can race the server tick and makes later context expansion unsafe.

The snapshot creates a hard boundary:

`Minecraft server thread -> immutable snapshot -> async LLM/memory -> Minecraft server thread apply`

## Captured context

The snapshot reuses MCA's existing context modules for:

- personality and mood;
- traits;
- relationships/family/hearts;
- village context;
- environment;
- player advancements/context.

It additionally records bounded factual observations:

- dimension and villager block position;
- biome;
- day/night and weather;
- player main-hand item and armor;
- bounded villager inventory summary;
- currently available whitelisted AI actions.

The LLM prompt marks these as observed facts for the current turn. Information not present in the snapshot is treated as unknown, not automatically false.

## Thread boundary

For the direct LivingWorld/OpenAI voice path, snapshot capture happens during the post-STT server-thread target revalidation. Prompt construction, persistent-memory file I/O, and HTTP/LLM processing then use copied immutable values.

Client-visible errors and AI actions return through `MinecraftServer.execute(...)`. Actions are revalidated against current state before execution.

## Scope

This milestone hardens the direct LivingWorld/OpenAI voice path. Legacy MCA/Inworld callers remain backward compatible and require a separate caller-by-caller migration before claiming that every historical ChatAI path uses the same thread boundary.

## Privacy and size

The snapshot is in-memory and ephemeral. It does not persist raw audio. Item/inventory facts are bounded and the system does not dump arbitrary chunks or full entity lists into the LLM context.
