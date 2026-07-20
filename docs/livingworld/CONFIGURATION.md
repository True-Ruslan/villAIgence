# LivingWorld configuration

LivingWorld is designed so the normal MVP needs only one server-side secret.

## First run

1. Start the Minecraft server once with the LivingWorld-enabled MCA build.
2. Stop the server.
3. Open `config/livingworld.json`.
4. Put your OpenAI API key into `apiKey`.
5. Start the server again.

You may instead set `OPENAI_API_KEY` in the server environment. The environment variable takes precedence over the JSON value and is the recommended production setup.

The generated config already contains working defaults for:

- LLM: `gpt-4.1-mini`
- safe whitelisted NPC actions: enabled
- persistent per-NPC/per-player conversation memory: enabled, 16 messages, 1200 characters per stored message
- factual local event memory: enabled, 512 stored events, 3 Minecraft days max age, 32-block context radius, 8 events per context snapshot
- structured LivingWorld relationship state: enabled, max per-turn proposed delta `2` per axis
- STT: `gpt-4o-mini-transcribe`
- TTS: `tts-1`
- voice: `marin`
- OpenAI chat, transcription, and speech endpoints
- voice segmentation, duration limits, spatial range, and network timeouts

For the normal MVP setup, do not change those values.

## Secret handling

Never commit a real API key to Git or include it in a modpack. API credentials are only needed by the dedicated server; players do not configure an AI provider.

## Safe actions

`safeActionsEnabled=true` exposes only MCA's hard-coded AI action whitelist. It does not grant arbitrary command execution. See `docs/livingworld/ACTIONS.md`.

## Persistent conversation memory

LivingWorld stores bounded rolling dialogue memory in `<world>/livingworld/memory.json`. Back it up together with the Minecraft world. See `docs/livingworld/MEMORY.md` for details.

## Factual event memory

LivingWorld stores bounded server-generated factual events in `<world>/livingworld/events.json`. Only recent nearby events are injected into an NPC's immutable context snapshot. Player/LLM claims are not automatically treated as facts. See `docs/livingworld/EVENTS.md`.

## Structured relationship state

LivingWorld stores bounded `trust`, `respect`, `fear`, and `affinity` in `<world>/livingworld/relationships.json`. It is separate from MCA hearts/family/marriage data. The snapshot-aware direct LivingWorld path may propose only small per-turn deltas; the server clamps and persists them. See `docs/livingworld/RELATIONSHIPS.md`.

## Voice requirements

For voice conversations, install Simple Voice Chat with API `2.6.20` or newer on both the server and players' clients. The Fabric build declares `voicechat_api >= 2.6.20` so an incompatible older voice-chat API is rejected at load time instead of crashing later.

See `docs/livingworld/VOICE.md` for the interaction flow and troubleshooting.

## Backward compatibility

If LivingWorld is disabled, uses an unsupported provider, or has no API key, the fork falls back to MCA's existing `mca.json` ChatAI configuration. Persistent conversation memory, factual event recording, structured relationship state, and the LivingWorld safe-action switch are used only for the configured direct LivingWorld provider path.
