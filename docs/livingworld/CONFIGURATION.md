# LivingWorld configuration

LivingWorld keeps all AI credentials and provider settings on the dedicated server. Clients never need an API key.

## First run

1. Start the server once with LivingWorld installed.
2. Stop the server.
3. Open `config/livingworld.json`.
4. Configure the chat provider and API key.
5. Start the server again.

Environment variables are recommended over storing keys in JSON:

```bash
# OpenAI
export OPENAI_API_KEY="sk-..."

# OpenRouter
export OPENROUTER_API_KEY="sk-or-v1-..."
```

`provider` accepts:

- `openai`
- `openrouter`

Both use OpenAI-compatible chat requests; only endpoint, model slug and credential source differ.

## Recommended OpenRouter text-only voice mode

This is the recommended low-cost configuration:

```text
microphone → OpenRouter STT → NPC AI → text answer
```

```json
{
  "version": 2,
  "enabled": true,
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

`OPENROUTER_API_KEY` is used automatically for OpenRouter chat and STT. A separate `sttApiKey` may be configured when chat and STT use different accounts/providers.

OpenRouter expects JSON with a raw Base64 WAV value in `input_audio.data`. `sttRequestFormat="auto"` detects `openrouter.ai` and selects this format automatically.

OpenRouter speech-to-text is paid usage. HTTP `402 Payment Required` means the account needs credits/balance; it is not a malformed-audio error.

## Voice input and output switches

The old single `voiceEnabled` field was replaced by independent controls:

| Setting | Default for new configs | Behavior |
|---|---:|---|
| `voiceInputEnabled` | `true` | capture microphone audio and call STT |
| `voiceOutputEnabled` | `false` | call TTS and play NPC speech spatially |

Text-only answers:

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

No TTS request is made while `voiceOutputEnabled=false`.

## STT request formats

`sttRequestFormat` accepts:

- `auto` — JSON/Base64 for OpenRouter, multipart file upload otherwise;
- `json_base64` — force JSON with `input_audio.data` and `input_audio.format`;
- `multipart` — force OpenAI-style `multipart/form-data` file upload.

Invalid values normalize to `auto`.

## Credential resolution

Main chat key:

- `provider=openrouter`: `OPENROUTER_API_KEY`, then `apiKey`;
- `provider=openai`: `OPENAI_API_KEY`, then `apiKey`.

STT key:

1. `OPENROUTER_API_KEY` for an OpenRouter STT endpoint;
2. `sttApiKey`;
3. the resolved main provider key.

This allows OpenAI chat with OpenRouter STT, OpenRouter chat with OpenAI STT, or a single key for both.

## Existing config migration

Config version 1 is migrated automatically to version 2:

- `voiceEnabled=true` becomes `voiceInputEnabled=true` and `voiceOutputEnabled=true`;
- `voiceEnabled=false` becomes both new switches `false`.

The migrated file is rewritten on server startup. Existing full-voice installations therefore preserve their previous behavior, while new installations default to microphone input with text-only answers.

## Other defaults

The generated config also enables:

- safe whitelisted NPC actions;
- persistent NPC/player dialogue memory;
- bounded factual event memory;
- bounded `trust`, `respect`, `fear`, and `affinity` state;
- 800 ms speech segmentation silence;
- 250 ms minimum utterance;
- 20 second maximum utterance;
- 10 second connect timeout;
- 60 second read timeout.

## Secret handling

Never commit a real API key to Git or include it in a modpack. API credentials belong only on the dedicated server.

## Persistent data

LivingWorld stores server-owned data under `<world>/livingworld/`:

- `memory.json`
- `events.json`
- `relationships.json`

Back these files up with the Minecraft world.

## Voice requirements

Simple Voice Chat 2.6.20 or newer is required on the server and clients whenever `voiceInputEnabled=true`, even if NPC speech output is disabled. LivingWorld uses its microphone packet and Opus decoding APIs.

See `docs/livingworld/VOICE.md` for the full interaction flow and troubleshooting.

## Backward compatibility

If LivingWorld is disabled, uses an unsupported provider, or lacks the required main chat credential, the fork falls back to MCA's legacy ChatAI configuration. LivingWorld memory, factual events, structured relationships and new voice routing apply to the configured direct LivingWorld provider path.
