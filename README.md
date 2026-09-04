# VillAIgence

**Vill-AI-gence** — *Giving villagers a mind of their own.*

VillAIgence is an AI-driven Minecraft mod built on **Minecraft Comes Alive Reborn (MCA)** for **Minecraft 1.21.1**. It keeps MCA's human-like villagers, families and relationship systems, then adds persistent AI conversations, microphone input, layered memory, world context, social state, stable NPC voices and safe server-authoritative actions.

`LivingWorld` remains the name of the internal AI/living-world engine and its compatibility-sensitive paths. The public mod is **VillAIgence**.

**Author and engineering portfolio:** [trueruslan.ru](https://trueruslan.ru/)

> **Experimental software:** back up your world before installing or updating.

## Current release

The current official release line is **`0.2.0+1.21.1`**, which begins the Memory 2.0 clean-cutover line.

See:

- [changelog.md](changelog.md) — canonical product/release changelog;
- [Project state](docs/PROJECT_STATE.md) — exact current implementation and validation state;
- [Roadmap](docs/ROADMAP.md) — current development direction;
- [Releases](../../releases) — published artifacts.

## What VillAIgence adds

- AI conversations with individual NPCs;
- microphone → STT → NPC AI → text and optional spatial TTS;
- persistent Memory 2.0 dialogue history;
- semantic FACT/BELIEF memory with explicit provenance boundaries;
- factual world-event context;
- trust, respect, fear and affinity;
- stable per-NPC voices based on UUID, gender and age pools;
- mood-aware voice delivery;
- safe whitelisted NPC actions;
- server-only API credentials.

## Quick install

### Required on server and client

| Component | Requirement |
|---|---|
| Minecraft | **1.21.1** |
| Java | **21** |
| Fabric Loader | compatible with Minecraft 1.21.1 |
| Fabric API | compatible with Minecraft 1.21.1 |
| VillAIgence | the same release JAR on server and clients |
| Simple Voice Chat | **2.6.20 or newer** |

Download VillAIgence from this repository's **GitHub Releases** page.

**Do not install original MCA Reborn together with VillAIgence.** For compatibility, VillAIgence intentionally keeps MCA's internal mod id `mca`, so both JARs cannot be loaded at the same time.

## Server setup

1. Back up the Minecraft world.
2. Install Fabric for Minecraft 1.21.1 and run the server with Java 21.
3. Put into the server `mods/` directory:
   - VillAIgence release JAR;
   - Fabric API;
   - Simple Voice Chat 2.6.20+.
4. Remove the original MCA Reborn JAR if present.
5. Start the server once, then stop it. The internal LivingWorld engine creates `config/livingworld.json`.
6. Configure AI credentials on the **server only**.

OpenAI:

```bash
export OPENAI_API_KEY="your-api-key"
```

OpenRouter:

```bash
export OPENROUTER_API_KEY="sk-or-v1-..."
```

Keys may also be configured in `config/livingworld.json`, but environment variables are preferred. Never commit real credentials or distribute them in a client modpack.

7. Start the server again.

### Pre-0.2 data boundary

`0.2.0` intentionally does **not** migrate the experimental pre-0.2 `<world>/livingworld/memory.json` conversation store.

For this pre-1.0 development line, the supported Memory 2.0 rollout boundary is a clean LivingWorld state on a dedicated test world or offline world copy. Keep backups before changing versions. The old raw conversation store is not opened, imported or recreated by current runtime code.

## Client setup

Each player installs:

- the same VillAIgence JAR as the server;
- Fabric API;
- Simple Voice Chat 2.6.20+.

Remove original MCA Reborn from the client as well.

Clients do **not** need OpenAI/OpenRouter keys or a separate AI application.

## Voice modes

Recommended low-cost mode:

```text
player microphone
→ STT
→ VillAIgence NPC AI
→ clean text reply
```

```json
{
  "voiceInputEnabled": true,
  "voiceOutputEnabled": false
}
```

Full voice mode:

```text
player microphone
→ STT
→ NPC AI
→ clean text reply
→ persistent NPC voice + mood
→ TTS
→ spatial voice from the NPC
```

```json
{
  "voiceInputEnabled": true,
  "voiceOutputEnabled": true
}
```

`voiceOutputEnabled=false` remains the default.

## Talking to an NPC

1. Interact with an MCA villager to select it.
2. Look toward the selected NPC.
3. Hold the normal Simple Voice Chat push-to-talk key and speak.
4. Stop speaking; VillAIgence segments the utterance and sends it to STT.
5. The selected NPC receives the recognized text and generates a response.
6. The clean response appears in the MCA conversation UI.
7. When TTS is enabled, the same response is played spatially from that NPC.

Normal player-to-player Simple Voice Chat traffic remains unaffected.

## Memory 2.0

Current persistent dialogue flow:

```text
successful text/voice turn
→ structured DIALOGUE MemoryEvent
→ <world>/livingworld/memory2.json
→ exact NPC/player recall on later turns
```

Semantic knowledge distinguishes authority explicitly:

```text
FACT   → SYSTEM_OBSERVED server evidence only
BELIEF → PLAYER_TOLD / NPC_TOLD / INFERRED only
```

Confidence never upgrades BELIEF into FACT. Current observed server facts remain authoritative over conflicting recalled beliefs.

Bounded `PLAYER_TOLD` BELIEF extraction exists after PR #125 but is deliberately **disabled by default**:

```json
{
  "semanticBeliefExtractionEnabled": false,
  "semanticBeliefMaxCandidatesPerTurn": 3
}
```

When enabled, candidate statements reuse the existing structured chat response; VillAIgence does not make a second extraction request. The model supplies candidate text only. Server code owns the NPC, player, source DIALOGUE event, provenance and BELIEF kind, and persists the DIALOGUE before any semantic admission.

See [Memory 2.0](docs/livingworld/MEMORY_2.md) and [Semantic ingestion](docs/livingworld/SEMANTIC_INGESTION.md).

## Persistent NPC voices

VillAIgence assigns each NPC a stable voice identity based on its UUID, MCA gender and age bucket. The profile is stored in:

```text
<world>/livingworld/voices.json
```

Changing the chat/LLM model does not change an NPC's persisted voice. Mood affects delivery style, not voice identity.

## Server-side data

The internal LivingWorld engine stores persistent data under:

```text
<world>/livingworld/
├── memory2.json
├── semantic-memory.json
├── events.json
├── relationships.json
├── voices.json
└── operator-lore.json
```

Current auxiliary corruption/recovery coverage includes:

```text
memory2.json
semantic-memory.json
relationships.json
voices.json
operator-lore.json
```

`events.json` is authoritative factual event history and has its own validation path.

Compatibility-sensitive paths intentionally keep the `livingworld` name. The branding migration does not rename the config or current world-data root, but the pre-0.2 raw `memory.json` conversation format was intentionally retired as a separate clean-cutover decision.

Configuration remains:

```text
config/livingworld.json
```

API keys remain server-side. Raw microphone and synthesized audio are processed in memory and are not intentionally persisted.

## OpenRouter voice example

VillAIgence supports OpenRouter STT and raw PCM TTS. For TTS, `ttsResponseFormat="auto"` resolves OpenRouter endpoints to PCM and other OpenAI-compatible endpoints to WAV.

See:

- [Configuration](docs/livingworld/CONFIGURATION.md)
- [Voice, STT and TTS](docs/livingworld/VOICE.md)

## Compatibility identity

VillAIgence is a new public product built on MCA, but deliberately preserves these compatibility-sensitive internals:

```text
mod id:            mca
Java namespace:    net.conczin.mca
config:            config/livingworld.json
world data root:   <world>/livingworld/
internal engine:   LivingWorld
```

These identifiers are not renamed merely for branding. Persistent schema changes still have their own explicit compatibility/rollout rules, such as the pre-1.0 Memory 2.0 clean cutover.

## AI diagnostics

Server operators can inspect current AI configuration readiness and the latest runtime result for Chat, STT and TTS with:

```text
/villaigence ai status
```

The command is read-only. It does **not** call a provider, spend tokens/credits, retry a request or mutate NPC/world state. It reports only safe operational metadata such as configured provider/model/endpoint host, stage state, duration, controlled error type, chat `finish_reason`, generation ID and attempt count where available.

It also shows process-local non-blocking admission/backpressure metrics for Chat, STT and TTS: current `active/max`, total locally rejected requests and remaining provider cooldown after detected rate limiting. VillAIgence rejects overload immediately instead of blocking the Minecraft server thread or building an unbounded AI queue. A locally rejected request does not call the external provider, and TTS backpressure never removes an already-published valid text reply.

API keys, Authorization headers, prompts, transcripts, NPC answers, TTS input, reasoning content and raw provider payloads are never intentionally included in the status surface. `last: NEVER` means no completed operation for that stage has been observed since the current server process started.

See [AI diagnostics](docs/livingworld/DIAGNOSTICS.md) and [Configuration](docs/livingworld/CONFIGURATION.md) for the full interpretation and tuning guide.

## Common problems

### Server does not start

Check that:

- Minecraft is exactly 1.21.1;
- Java 21 is used;
- Fabric API is installed;
- Simple Voice Chat 2.6.20+ is installed;
- original MCA Reborn is not installed alongside VillAIgence.

### NPC AI does not answer

Run `/villaigence ai status`, then check `config/livingworld.json`, `OPENAI_API_KEY` / `OPENROUTER_API_KEY` and the dedicated server log.

### OpenRouter returns HTTP 402

The account does not have enough usable credits for the selected speech request/model. Free chat models do not imply free STT/TTS.

### Text works, but microphone input does not

Check:

- `voiceInputEnabled=true`;
- Simple Voice Chat is connected on server and client;
- the NPC was selected and is still the active target;
- STT endpoint/model/key are valid.

### Text works, but NPC is silent

Check:

- `voiceOutputEnabled=true`;
- TTS endpoint/model/key are valid;
- voice pools contain IDs supported by the selected TTS provider;
- `ttsResponseFormat` matches the endpoint (`auto` is recommended).

Text replies are intentionally published before TTS, so a TTS failure or local TTS admission rejection does not remove the NPC's text response.

## Documentation

- [Project state](docs/PROJECT_STATE.md)
- [Roadmap](docs/ROADMAP.md)
- [Changelog](changelog.md)
- [Configuration](docs/livingworld/CONFIGURATION.md)
- [AI diagnostics](docs/livingworld/DIAGNOSTICS.md)
- [Voice and STT/TTS](docs/livingworld/VOICE.md)
- [Memory 2.0](docs/livingworld/MEMORY_2.md)
- [Semantic ingestion](docs/livingworld/SEMANTIC_INGESTION.md)
- [Factual events](docs/livingworld/EVENTS.md)
- [Relationships](docs/livingworld/RELATIONSHIPS.md)
- [Release process](docs/RELEASING.md)

## About Minecraft Comes Alive Reborn

VillAIgence is built on **Minecraft Comes Alive Reborn**, whose villagers, families, relationships, rendering and simulation provide the foundation for this project.

Upstream project:

- [Luke100000/minecraft-comes-alive](https://github.com/Luke100000/minecraft-comes-alive)
- [MCA Reborn on Modrinth](https://modrinth.com/mod/minecraft-comes-alive-reborn)
- [MCA Reborn on CurseForge](https://www.curseforge.com/minecraft/mc-mods/minecraft-comes-alive-reborn)

The Modrinth and CurseForge links above refer to the **original MCA Reborn project**, not VillAIgence releases.

VillAIgence remains licensed under **GPL-3.0** and retains attribution to MCA Reborn and its contributors.

## Contributing

Keep changes scoped, use TDD for runtime behavior, test Fabric/NeoForge behavior where relevant, preserve server authority around AI actions/data, update root `changelog.md` for notable delivery changes, and do not change compatibility-sensitive `mca`/`livingworld` identities without an explicit migration design.

## Credits

VillAIgence builds on Minecraft Comes Alive Reborn and the work of its contributors, including Cleora, WildBamaBoy, SheWolfDeadly, ntzrmtthihu777, ko2fan, Akjosch, Innectic, Sollace, CDAGaming, Luke100000 and the many MCA contributors listed in [`contributors.json`](resources/assets/mca/api/supporters/contributors.json).