# VillAIgence configuration

VillAIgence keeps AI, STT and TTS credentials on the dedicated server. Clients never need API keys.

The file remains named `config/livingworld.json` because **LivingWorld is the internal engine namespace**. This compatibility-sensitive path is intentionally not renamed by the VillAIgence rebrand.

## First run

1. Start the server once with VillAIgence installed.
2. Stop the server.
3. Edit `config/livingworld.json`.
4. Configure provider/model/credentials.
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

Recommended text-only mode:

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

```json
{
  "ttsResponseFormat": "auto",
  "ttsPcmSampleRate": 24000
}
```

`ttsResponseFormat` accepts:

- `auto` — raw PCM for `openrouter.ai`, WAV for other OpenAI-compatible endpoints;
- `pcm` — force headerless PCM16 little-endian decoding;
- `wav` — force WAV decoding.

Unknown values normalize to `auto`.

`ttsPcmSampleRate` is used when a raw PCM response does not provide a valid `rate` parameter in `Content-Type`. Invalid/non-positive values normalize to `24000`.

For PCM responses VillAIgence:

1. requires base `Content-Type` `audio/pcm`;
2. uses MIME `rate` when present, otherwise `ttsPcmSampleRate`;
3. accepts missing `channels` as mono;
4. rejects explicit channel count other than `1`;
5. decodes signed PCM16 little-endian directly;
6. resamples to Simple Voice Chat's 48 kHz playback rate.

No fake WAV header is added. WAV mode preserves the existing WAV decoder path.

## OpenRouter TTS example

Example using a provider/model whose supported voice IDs are `eve`, `ara`, `rex`, `sal`, `leo`:

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

Model and voice IDs are configuration examples only; runtime code does not hardcode them.

Voice IDs are provider/model-specific. The repeated pools above intentionally avoid claiming provider-defined gender or age classifications. Server owners can classify validated voices into narrower VillAIgence pools.

When switching TTS providers, update voice pools as well as `ttsVoice`. A stored NPC profile whose voice is no longer eligible is automatically resolved to a compatible configured replacement.

## Persistent NPC voices

Profiles are stored at:

```text
<world>/livingworld/voices.json
```

The `livingworld` path remains unchanged for backward compatibility.

Each profile is keyed by NPC UUID and stores normalized gender, age bucket and selected voice ID.

MCA 1.21.1 age mapping:

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

Built-in groupings are VillAIgence defaults, not provider-supplied gender labels.

## Mood-aware delivery

Mood changes delivery, not persistent voice identity.

Current server-owned signals include panic, health, trust, fear and affinity. They resolve to neutral/happy/sad/angry/afraid/tired delivery styles.

Supported controls are applied best-effort. `speed` is bounded. For OpenAI speech models routed through OpenRouter, OpenAI-specific `instructions` are placed under `provider.options.openai`.

## TTS credentials

Credential selection follows the **TTS endpoint**.

For OpenRouter speech:

1. `OPENROUTER_API_KEY`;
2. `ttsApiKey`;
3. main provider key only when the main provider is `openrouter`.

For OpenAI speech:

1. `OPENAI_API_KEY`;
2. `ttsApiKey`;
3. main provider key only when the main provider is `openai`.

For another custom TTS endpoint:

1. `ttsApiKey`;
2. resolved main provider key.

This prevents accidental cross-provider credential leakage.

## Structured AI response safety

VillAIgence treats the visible NPC message independently from optional command and relationship metadata.

- malformed `optionalCommand` is ignored without discarding a valid message;
- malformed relationship fields are ignored;
- relationship values must be integers before acceptance;
- server-side relationship application performs configured per-turn clamping;
- syntactically malformed JSON may recover only a safe top-level JSON string `message`;
- JSON metadata/tails are never used as visible fallback text;
- unrecoverable JSON-looking responses produce no unsafe answer.

The same sanitized message is used for text and TTS.

## Text survives TTS failure

The conversation path is text-first:

```text
AI answer
→ sanitize structured response
→ publish MCA text reply
→ attempt TTS
→ spatial playback
```

A TTS HTTP/format/decode failure does not remove the text reply, disconnect the player or automatically retry the same utterance.

## Existing config migration

Config version remains `2`. Existing version-2 files do not require migration: missing newer fields receive safe defaults.

Legacy version-1 `voiceEnabled` migration remains unchanged.

The VillAIgence rebrand does **not** rename `config/livingworld.json` or serialized field names.

## Persistent server data

```text
<world>/livingworld/
├── memory.json
├── events.json
├── relationships.json
└── voices.json
```

Back these files up with the Minecraft world.

Raw microphone and synthesized audio are processed in memory and are not intentionally persisted.

## Voice requirements

Simple Voice Chat 2.6.20+ is required on server and clients when microphone input is used. The same VillAIgence release JAR must be installed on server and clients.

See [VOICE.md](VOICE.md) for interaction flow and troubleshooting.
