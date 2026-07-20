# LivingWorld persistent memory

Configured LivingWorld servers persist a bounded conversation history per `NPC × player` pair.

## Location

`<world>/livingworld/memory.json`

Back up this file together with the Minecraft world. Writes use a temporary file and atomic replacement when the filesystem supports it.

## Defaults

- enabled: `persistentMemoryEnabled=true`
- history: `persistentMemoryMaxMessages=16`
- maximum stored length per message: `persistentMemoryMaxCharsPerMessage=1200`

These defaults require no extra setup beyond the normal LivingWorld API key.

## Privacy

Raw microphone audio is not persisted by this feature. Voice input is transcribed first; only the text sent into the NPC conversation and the NPC text reply can enter persistent memory.

## Scope

This is durable rolling dialogue context, not yet semantic human-like memory. Later milestones can add fact extraction, importance, forgetting, shared village knowledge, rumors, and SQLite without changing the public ChatAI flow.

## Failure behavior

Memory failures are logged and treated as non-fatal. NPC dialogue continues without persistent history rather than failing the conversation.
