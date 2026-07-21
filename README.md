# LivingWorld — AI-powered Minecraft Comes Alive fork

LivingWorld is an experimental fork of **Minecraft Comes Alive Reborn (MCA)** for **Minecraft 1.21.1 / Fabric**. It keeps MCA's villagers, relationships and family systems, and adds server-driven AI conversations, voice interaction, persistent memory, factual world context, bounded social state and safe NPC actions.

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

Recommended: set the environment variable before starting Minecraft:

```bash
export OPENAI_API_KEY="your-api-key"
```

Or edit:

```text
config/livingworld.json
```

and set:

```json
{
  "apiKey": "your-api-key"
}
```

7. Start the server again.

The default configuration already contains working defaults for chat, speech-to-text, text-to-speech, memory, events, relationships and safe NPC actions. For a first test, normally only the API key is required.

## Client setup

Each player installs into the client `mods/` folder:

- the **same LivingWorld JAR** as the server;
- Fabric API;
- Simple Voice Chat 2.6.20+.

Remove the original MCA Reborn JAR from the client as well.

**Clients do not need an OpenAI/API key and do not need any separate AI application.**

After joining the server, verify that Simple Voice Chat is connected and configure the microphone / push-to-talk key normally.

## Talking to an NPC by voice

1. Interact normally with an MCA villager to select that NPC as the current conversation target.
2. Look toward the villager.
3. Hold the normal Simple Voice Chat push-to-talk key and speak.
4. Stop speaking briefly; LivingWorld sends the utterance through STT → NPC AI → TTS and plays the answer spatially from the NPC.

Normal player-to-player voice chat continues to work normally. Ambient voice is not intentionally sent to the AI unless an NPC has been selected and the server validates the conversation target.

## What is stored on the server

LivingWorld keeps its persistent world data under the Minecraft world directory:

```text
<world>/livingworld/
├── memory.json
├── events.json
└── relationships.json
```

Back these files up together with the world.

The API key stays server-side. Raw microphone audio is processed in memory and is not intentionally stored by LivingWorld.

## Common problems

### Server does not start

Check that:

- Minecraft is exactly 1.21.1;
- the server runs Java 21;
- Fabric API is installed;
- Simple Voice Chat 2.6.20+ is installed;
- the original MCA Reborn JAR is not installed alongside LivingWorld.

### NPC AI does not answer

Check `config/livingworld.json` or the `OPENAI_API_KEY` environment variable, then inspect the dedicated server log for the provider error.

### Text works, but voice does not

Check that Simple Voice Chat is installed and connected on both server and client. Interact with the NPC again, look toward it and use the normal push-to-talk key.

### Server and client report mod mismatch

Use the same LivingWorld release JAR on both sides and remove any second/original MCA JAR.

## Advanced configuration

Most users should keep the generated defaults. Additional documentation:

- [LivingWorld configuration](docs/livingworld/CONFIGURATION.md)
- [Voice interaction](docs/livingworld/VOICE.md)
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

For upstream MCA development information, see the original project's documentation and repository.

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
