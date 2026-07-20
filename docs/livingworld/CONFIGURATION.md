# LivingWorld configuration

LivingWorld is designed so the MVP normally needs only one server-side secret.

## First run

1. Start the Minecraft server once with the LivingWorld-enabled MCA build.
2. Stop the server.
3. Open `config/livingworld.json`.
4. Put your OpenAI API key into `apiKey`.
5. Start the server again.

Example:

```json
{
  "version": 1,
  "enabled": true,
  "apiKey": "YOUR_OPENAI_API_KEY",
  "provider": "openai",
  "endpoint": "https://api.openai.com/v1/chat/completions",
  "model": "gpt-4.1-mini",
  "connectTimeoutSeconds": 10,
  "readTimeoutSeconds": 60
}
```

For the normal MVP setup, do not change the other values.

## Recommended secret setup

For a managed or hosted server, set the environment variable `OPENAI_API_KEY` instead of writing the key into JSON. The environment variable takes precedence over `apiKey`.

Never commit a real API key to Git or include it in a modpack.

## Client requirements

Players do not need an API key and do not configure an AI provider. AI requests are initiated by the server.

## Backward compatibility

If LivingWorld is disabled, uses an unsupported provider, or has no API key, the fork falls back to MCA's existing `mca.json` ChatAI configuration. Existing MCA servers therefore keep their previous behavior unless LivingWorld is explicitly configured.

## Current MVP scope

This foundation covers server-side LLM chat configuration and reuses MCA's existing NPC context/conversation pipeline. Voice input, STT, TTS spatial playback, persistent world knowledge, and factions are separate milestones.
