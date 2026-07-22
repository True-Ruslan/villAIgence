# LivingWorld configuration

LivingWorld keeps AI, STT and TTS credentials on the dedicated server. Clients never need API keys.

## First run

1. Start the server once with LivingWorld installed.
2. Stop the server.
3. Edit `config/livingworld.json`.
4. Configure the provider/model/credentials you need.
5. Start the server again.

Environment variables are preferred:

```bash
export OPENAI_API_KEY="sk-..."
export OPENROUTER_API_KEY="sk-or-v1-..."
```

Never commit real keys or distribute them in a modpack.

## Main AI provider

`provider` accepts:

- `openai`
- `openrouter`

Main chat credential resolution:

- `provider=openrouter`: `OPENROUTER_API_KEY`, then `apiKey`;
- `provider=openai`: `OPENAI_API_KEY`, then `apiKey`.

## Voice switches

| Setting | Default | Behavior |
|---|---:|---|
| `voiceInputEnabled` | `true` | microphone → STT → NPC AI |
| `voiceOutputEnabled` | `false` | synthesize and spatially play NPC speech |

Text-only low-cost mode:

```json
{
  "voiceInputEnabled": true,
  "voiceOutputEnabled": false
}
```

Full voice mode:

```json
{
  "voiceInputEnabled": true,
  "voiceOutputEnabled": true
}
```

No TTS request is made while `voiceOutputEnabled=false`.

## OpenRouter STT

Example:

```json
{
  "provider": "openrouter",
  "endpoint": "https://openrouter.ai/api/v1/chat/completions",
  "model": "your-chat-model-slug",

  "voiceInputEnabled": true,
  "sttEndpoint": "https://openrouter.ai/api/v1/audio/transcriptions",
  "sttModel": "openai/gpt-4o-mini-transcribe",
  "sttRequestFormat": "json_base64",
  "sttLanguage": "ru"
}
```

`sttRequestFormat` accepts:

- `auto` — JSON/Base64 for OpenRouter, multipart WAV upload otherwise;
- `json_base64` — force JSON/Base64;
- `multipart` — force multipart file upload.

Unknown values normalize to `auto`.

STT credential resolution:

1. `OPENROUTER_API_KEY` for an OpenRouter STT endpoint;
2. `sttApiKey`;
3. resolved main provider key.

OpenRouter STT is billed API usage. HTTP `402 Payment Required` means the account has no usable credits for that request.

## TTS response format

New optional settings:

```json
{
  "ttsResponseFormat": "auto",
  "ttsPcmSampleRate": 24000
}
```

`ttsResponseFormat` accepts:

- `auto` — raw PCM for `openrouter.ai`, WAV for other OpenAI-compatible endpoints;
- `pcm` — force headerless PCM16 little-endian decoding;
- `wav` — force the existing WAV decoder.

Unknown values normalize to `auto`.

`ttsPcmSampleRate` is used only when a raw PCM response does not provide a valid `rate` parameter in `Content-Type`. Invalid/non-positive values normalize to `24000`.

For PCM responses LivingWorld:

1. requires the base `Content-Type` to be `audio/pcm`;
2. uses `rate` when present, otherwise `ttsPcmSampleRate`;
3. accepts missing `channels` as mono;
4. rejects an explicit channel count other than `1`;
5. decodes signed PCM16 little-endian directly, without adding a fake WAV header;
6. resamples the resulting audio to Simple Voice Chat's 48 kHz playback rate.

WAV mode preserves the existing `WavCodec.decodePcm16Mono()` path.

## OpenRouter TTS example

Example for an OpenRouter TTS model using raw PCM:

```json
{
  "provider": "openrouter",
  "voiceInputEnabled": true,
  "voiceOutputEnabled": true,

  "sttEndpoint": "https://openrouter.ai/api/v1/audio/transcriptions",
  "sttModel": "openai/gpt-4o-mini-transcribe",
  "sttRequestFormat": "json_base64",
  "sttLanguage": "ru",

  "ttsEndpoint": "https://openrouter.ai/api/v1/audio/speech",
  "ttsModel": "x-ai/grok-voice-tts-1.0",
  "ttsResponseFormat": "auto",
  "ttsPcmSampleRate": 24000,

  "maleChildVoices": ["eve", "ara", "rex", "sal", "leo"],
  "femaleChildVoices": ["eve", "ara", "rex", "sal", "leo"],
  "neutralChildVoices": ["eve", "ara", "rex", "sal", "leo"],
  "maleTeenVoices": ["eve", "ara", "rex", "sal", "leo"],
  "femaleTeenVoices": ["eve", "ara", "rex", "sal", "leo"],
  "neutralTeenVoices": ["eve", "ara", "rex", "sal", "leo"],
  "maleAdultVoices": ["eve", "ara", "rex", "sal", "leo"],
  "femaleAdultVoices": ["eve", "ara", "rex", "sal", "leo"],
  "neutralAdultVoices": ["eve", "ara", "rex", "sal", "leo"],
  "globalVoiceFallbacks": ["eve", "ara", "rex", "sal", "leo"],
  "ttsVoice": "rex",

  "voiceDistance": 32
}
```

The model and voice IDs above are configuration examples only; runtime code does not hardcode them.

Voice IDs are provider/model-specific. The repeated pools above intentionally avoid claiming provider-defined gender or age classifications. Server owners may classify supported voices into narrower LivingWorld pools after listening/testing them.

When switching TTS providers, update the voice pools as well as `ttsVoice`. If a stored NPC profile contains a voice that is no longer eligible under the new configured pools, LivingWorld resolves and persists a compatible replacement automatically.

OpenRouter TTS is billed API usage. Check the selected model/provider pricing before enabling `voiceOutputEnabled`.

## Persistent NPC voices

When TTS is enabled, profiles are stored at:

```text
<world>/livingworld/voices.json
```

The profile is keyed by NPC UUID and stores normalized gender, age bucket and selected voice ID.

Age mapping follows MCA 1.21.1:

- `BABY`, `TODDLER`, `CHILD` → child;
- `TEEN` → teen;
- `ADULT`, `UNASSIGNED` → adult.

MCA currently has no separate elder state.

Selection fallback order:

1. exact gender + age pool;
2. same-gender adult pool for child/teen NPCs;
3. neutral pool for the same age;
4. global fallback pool;
5. legacy `ttsVoice`.

The built-in groupings are LivingWorld configuration defaults, not provider-supplied gender labels.

## Mood-aware delivery

Mood changes delivery, not persistent voice identity.

Current server-owned signals include panic, health, trust, fear and affinity. They resolve to neutral/happy/sad/angry/afraid/tired delivery styles.

Supported TTS controls are applied best-effort. `speed` is sent as a bounded standard speech parameter. For OpenAI speech models routed through OpenRouter, OpenAI-specific `instructions` are placed under `provider.options.openai` instead of being sent as an unsupported top-level standard field. Model-specific unsupported controls do not change or randomize the assigned voice.

## TTS credentials

Credential selection follows the **TTS endpoint**, not only the chat provider.

For an OpenRouter speech endpoint:

1. `OPENROUTER_API_KEY`;
2. `ttsApiKey`;
3. resolved main provider key only when the main provider is also `openrouter`.

For an OpenAI speech endpoint:

1. `OPENAI_API_KEY`;
2. `ttsApiKey`;
3. resolved main provider key only when the main provider is also `openai`.

For another custom TTS endpoint:

1. `ttsApiKey`;
2. resolved main provider key.

This prevents cross-provider credential leakage: an OpenRouter key is not sent to the OpenAI speech endpoint, and an OpenAI main key is not sent to the OpenRouter speech endpoint unless explicitly supplied as a dedicated `ttsApiKey` by the server owner.

With `provider=openrouter` and an OpenRouter TTS endpoint, the same server-side `OPENROUTER_API_KEY` can be used for chat/STT/TTS.

## Structured AI response safety

LivingWorld treats the visible NPC message independently from optional command and relationship metadata.

- malformed `optionalCommand` is ignored without discarding a valid message;
- malformed relationship fields are ignored rather than exposed;
- relationship values must be integers before they are accepted;
- server-side relationship application performs the configured per-turn clamping;
- if the whole JSON object is syntactically malformed, LivingWorld attempts to recover only a valid top-level JSON string field named `message`;
- JSON metadata/tails are never used as fallback visible text;
- if a JSON-looking response contains no safely recoverable message, no NPC answer is published from that response.

The same sanitized message is used for text and TTS, so malformed structured metadata cannot become spoken audio.

## Text survives TTS failure

The conversation path is text-first:

```text
AI answer
→ sanitize structured response
→ publish MCA text reply
→ attempt TTS
→ spatial playback
```

A TTS HTTP/format/decode failure does not remove the text reply, does not disconnect the player, and does not automatically retry the same utterance. Busy player/NPC locks are released normally.

## Existing config migration

Config version remains `2`. Existing version-2 files do not require a migration step: missing `ttsResponseFormat` and `ttsPcmSampleRate` receive safe defaults (`auto`, `24000`).

Legacy version-1 `voiceEnabled` migration remains unchanged.

## Persistent server data

```text
<world>/livingworld/
├── memory.json
├── events.json
├── relationships.json
└── voices.json
```

Back these files up with the Minecraft world.

Raw microphone audio and synthesized audio are processed in memory and are not intentionally persisted by LivingWorld.

## Voice requirements

Simple Voice Chat 2.6.20 or newer is required on server and clients when microphone input is used. The same LivingWorld release JAR must be installed on server and clients.

See `docs/livingworld/VOICE.md` for interaction flow and troubleshooting.
