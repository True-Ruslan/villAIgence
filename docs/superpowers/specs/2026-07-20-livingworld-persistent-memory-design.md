# LivingWorld Persistent Memory Design

## Goal

Give direct LivingWorld/OpenAI conversations durable per-player NPC memory that survives server restarts without adding another service or database dependency.

## Architecture

- Store rolling conversation memory under `world/livingworld/memory.json`.
- Key each conversation by `villager UUID + player UUID` so NPCs remember players independently.
- Use atomic temp-file replacement for writes and synchronized in-process access.
- Keep bounded history to control prompt size and disk growth.
- Existing MCA legacy/conczin ChatAI behavior remains unchanged when LivingWorld is not configured.

## Defaults

- Persistent memory enabled automatically for configured LivingWorld.
- Keep 16 messages (8 user/assistant exchanges) per NPC/player pair.
- Trim each stored message to 1200 characters.
- Raw microphone audio is never persisted; only transcribed user text and NPC text replies are stored.

## Failure behavior

Memory is fail-open at the ChatAI boundary: read/write failures are logged but do not prevent an NPC from replying.

## Future migration

Storage is hidden behind a dedicated memory API so SQLite, semantic summaries, embeddings, shared village knowledge, and rumors can be added later without replacing the ChatAI flow.
