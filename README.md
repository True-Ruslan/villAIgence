# LivingWorld — AI-powered Minecraft Comes Alive fork

LivingWorld is an experimental fork of **Minecraft Comes Alive Reborn (MCA)** for **Minecraft 1.21.1 / Fabric**. It keeps MCA's villagers, relationships and family systems, and adds server-driven AI conversations, microphone input, persistent memory, factual world context, bounded social state and safe NPC actions.

> **Alpha software:** back up your world before installing or updating.

## Quick install

### Required on both server and client

| Component | Requirement |
|---|---|
| Minecraft | **1.21.1** |
| Java | **21** |
| Fabric Loader | compatible with Minecraft 1.21.1 |
| Fabric API | compatible with Minecraft 1.21.1 |
| LivingWorld | the same release JAR on server and clients |
| Simple Voice Chat | **2.6.20 or newer** |

Download LivingWorld from this repository's **GitHub Releases** page.

**Do not install the original MCA Reborn JAR together with LivingWorld.** This fork keeps MCA's internal mod id `mca`, so the original mod and this fork cannot be loaded at the same time.

## Server setup

1. **Back up the Minecraft world.**
2. Install Fabric for Minecraft 1.21.1 and run the server with **Java 21**.
3. Put these files into the server `mods/` folder:
   - LivingWorld release JAR;
   - Fabric API;
   - Simple Voice Chat 2.6.20+.
4. Remove the original MCA Reborn JAR if it is present.
5. Start the server once, then stop it. LivingWorld creates `config/livingworld.json`.
6. Configure the AI API key on the **server only**.

For OpenAI:

```bash
export OPENAI_API_KEY="your-api-key"
```

For OpenRouter:

```bash
export OPENROUTER_API_KEY="sk-or-v1-..."
```

Secrets may also be placed in `config/livingworld.json`, but environment variables are recommended for production servers. Never commit a real key to Git or distribute it in a modpack.

7. Start the server again.

## Recommended: microphone input, text-only NPC answers

This mode avoids TTS requests and synthesis cost:

```text
player microphone → OpenRouter STT → LivingWorld NPC AI → text in Minecraft chat
```

Set the following values in `config/livingworld.json`:

```json
{
  "version": 2,
  "provider": "openrouter",
  "endpoint": "https://openrouter.ai/api/v1/chat/completions",
  "model": "your-chat-model-slug",
  "voiceInputEnabled": true,
  "voiceOutputEnabled": false,
  "sttEndpoint": "https://openrouter.ai/api/v1/audio/transcriptions",
  "sttModel": "openai/gpt-4o-mini-transcribe",
  "sttRequestFormat": "json_base64",
  "sttLanguage": "ru"
}
```

`sttRequestFormat="auto"` is also valid and automatically selects JSON/Base64 for an `openrouter.ai` endpoint.

OpenRouter STT is paid usage. HTTP `402 Payment Required` means the OpenRouter account has no usable credits; add balance and retry. No TTS request is made while `voiceOutputEnabled=false`.

## Client setup

Each player installs into the client `mods/` folder:

- the **same LivingWorld JAR** as the server;
- Fabric API;
- Simple Voice Chat 2.6.20+.

Remove the original MCA Reborn JAR from the client as well.

**Clients do not need an OpenAI/OpenRouter key and do not need any separate AI application.**

Simple Voice Chat remains required on clients even when NPC speech output is disabled because LivingWorld uses it to capture and transport microphone audio.

## Talking to an NPC by microphone

1. Interact normally with an MCA villager to select that NPC as the current conversation target.
2. Look toward the villager.
3. Hold the normal Simple Voice Chat push-to-talk key and speak.
4. Stop speaking briefly; LivingWorld sends the utterance through STT and NPC AI.
5. The NPC answer appears in the existing MCA conversation/chat UI.
6. When `voiceOutputEnabled=true`, the same answer is additionally synthesized and played spatially from the NPC.

Normal player-to-player voice chat continues to work normally. Ambient voice is not intentionally sent to the AI unless an NPC has been selected and the server validates the conversation target.

## Voice switches

| Setting | Effect |
|---|---|
| `voiceInputEnabled` | Captures microphone speech and runs STT |
| `voiceOutputEnabled` | Runs TTS and spatially plays NPC speech |

Recommended low-cost mode:

```json
{
  "voiceInputEnabled": true,
  "voiceOutputEnabled": false
}
```

Full voice dialogue:

```json
{
  "voiceInputEnabled": true,
  "voiceOutputEnabled": true
}
```

### Persistent NPC voices

When TTS is enabled, LivingWorld assigns each NPC a stable voice profile based on its UUID, MCA gender and age group. Children, teens and adults use separate configurable voice pools where possible. The selected voice is persisted in `<world>/livingworld/voices.json`, so changing the chat/LLM model does **not** change an NPC's voice.

Mood changes delivery style, not voice identity. LivingWorld derives mood from server-owned state such as panic, health and relationship state. TTS providers/models that support richer style instructions use them; older models safely fall back to supported controls such as speaking speed.

## What is stored on the server

LivingWorld keeps its persistent world data under the Minecraft world directory:

```text
<world>/livingworld/
├── memory.json
├── events.json
├── relationships.json
└── voices.json
```

Back these files up together with the world.

API keys stay server-side. Raw microphone audio is processed in memory and is not intentionally stored by LivingWorld.

## Common problems

### Server does not start

Check that:

- Minecraft is exactly 1.21.1;
- the server runs Java 21;
- Fabric API is installed;
- Simple Voice Chat 2.6.20+ is installed;
- the original MCA Reborn JAR is not installed alongside LivingWorld.

### NPC AI does not answer

Check `config/livingworld.json`, `OPENAI_API_KEY` or `OPENROUTER_API_KEY`, then inspect the dedicated server log for the provider error.

### OpenRouter STT returns HTTP 402

The request format is valid, but the OpenRouter account needs credits. Add balance in OpenRouter and retry. Free-tier chat availability does not guarantee free speech-to-text usage.

### Text works, but microphone input does not

Check that:

- `voiceInputEnabled=true`;
- Simple Voice Chat is installed and connected on both server and client;
- the player interacted with the NPC and is looking toward it;
- the STT endpoint/model/key are configured.

### Text appears, but the NPC is silent

This is expected when `voiceOutputEnabled=false`. Enable it only when TTS and its cost are desired. For an OpenAI TTS endpoint, configure `OPENAI_API_KEY` or a dedicated `ttsApiKey` even when chat uses OpenRouter.

### Server and client report mod mismatch

Use the same LivingWorld release JAR on both sides and remove any second/original MCA JAR.

## Advanced configuration

Additional documentation:

- [LivingWorld configuration](docs/livingworld/CONFIGURATION.md)
- [Voice and STT modes](docs/livingworld/VOICE.md)
- [Persistent memory](docs/livingworld/MEMORY.md)
- [Factual events](docs/livingworld/EVENTS.md)
- [Relationships](docs/livingworld/RELATIONSHIPS.md)
- [Release process](docs/RELEASING.md)

---

## About Minecraft Comes Alive Reborn

Minecraft Comes Alive replaces vanilla villagers with human-like NPCs. Villagers can be interacted with, build relationships, marry, have children, manage villages and participate in the broader MCA simulation.

LivingWorld is built on the work of the original **Minecraft Comes Alive Reborn** project and remains licensed under **GPL-3.0**.

Upstream project:

- [Luke100000/minecraft-comes-alive](https://github.com/Luke100000/minecraft-comes-alive)
- [MCA Reborn on Modrinth](https://modrinth.com/mod/minecraft-comes-alive-reborn)
- [MCA Reborn on CurseForge](https://www.curseforge.com/minecraft/mc-mods/minecraft-comes-alive-reborn)

The Modrinth and CurseForge links above refer to the **original MCA Reborn project**, not to LivingWorld releases from this fork.

## Compatibility

MCA is generally compatible with other mods, though item recognition, villager interaction hooks, rendering and pathfinding can vary between modpacks. LivingWorld-specific compatibility findings are tracked separately in this fork.

## Contributing

Contributions are welcome. Keep changes scoped, test Fabric 1.21.1 behavior and preserve server authority around AI actions and persistent data.

## Credits

LivingWorld builds on Minecraft Comes Alive Reborn and the work of its contributors, including:

- Cleora
- WildBamaBoy
- SheWolfDeadly
- ntzrmtthihu777
- ko2fan
- Akjosch
- Innectic
- Sollace
- CDAGaming
- Luke100000
- and the many MCA contributors listed in [`contributors.json`](resources/assets/mca/api/supporters/contributors.json).
