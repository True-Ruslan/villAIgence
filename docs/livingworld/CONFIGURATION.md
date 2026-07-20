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
- STT: `gpt-4o-mini-transcribe`
- TTS: `gpt-4o-mini-tts`
- voice: `marin`
- OpenAI chat, transcription, and speech endpoints
- voice segmentation, duration limits, spatial range, and network timeouts

For the normal MVP setup, do not change those values.

## Secret handling

Never commit a real API key to Git or include it in a modpack. API credentials are only needed by the dedicated server; players do not configure an AI provider.

## Voice requirements

For voice conversations, install a compatible Simple Voice Chat version on both the server and players' clients. The LivingWorld integration targets Simple Voice Chat API `2.6.20`.

See `docs/livingworld/VOICE.md` for the interaction flow and troubleshooting.

## Backward compatibility

If LivingWorld is disabled, uses an unsupported provider, or has no API key, the fork falls back to MCA's existing `mca.json` ChatAI configuration.

Simple Voice Chat integration is isolated to the Fabric module. The MCA/LivingWorld text conversation path remains independent of microphone capture.
