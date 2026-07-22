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

## Persistent NPC voice profiles

When voice output is enabled, LivingWorld assigns each NPC a stable voice identity and stores it in:

```text
<world>/livingworld/voices.json
```

The profile is keyed by NPC UUID and records the normalized gender, age bucket and selected voice id. It is independent from the chat/LLM model: changing OpenRouter/OpenAI chat models does not change an existing NPC voice.

Age mapping follows actual MCA 1.21.1 states:

- `BABY`, `TODDLER`, `CHILD` → child voice bucket;
- `TEEN` → teen voice bucket;
- `ADULT`, `UNASSIGNED` → adult voice bucket.

MCA currently has no separate elder age state, so LivingWorld does not invent one.

Voice pools are configurable:

```json
{
  "maleChildVoices": ["ash", "echo"],
  "femaleChildVoices": ["shimmer", "coral"],
  "neutralChildVoices": ["alloy", "verse"],

  "maleTeenVoices": ["ash", "echo", "cedar"],
  "femaleTeenVoices": ["coral", "nova", "shimmer"],
  "neutralTeenVoices": ["alloy", "verse"],

  "maleAdultVoices": ["cedar", "onyx", "echo", "ash"],
  "femaleAdultVoices": ["marin", "coral", "nova", "shimmer", "sage"],
  "neutralAdultVoices": ["alloy", "verse", "fable", "ballad"],

  "globalVoiceFallbacks": ["marin", "cedar", "alloy"],
  "ttsVoice": "marin"
}
```

These gender/age groupings are **LivingWorld defaults**, not provider-supplied gender labels. Server owners may replace the pools with any voice IDs supported by their chosen TTS provider.

Selection is deterministic from the NPC UUID inside the first compatible non-empty pool. The resolved voice is persisted. On a real age-stage transition, or when the stored voice is no longer eligible under the configured pools, a compatible profile is resolved again.

Fallback order is:

1. exact gender + age pool;
2. same-gender adult pool for child/teen NPCs;
3. neutral pool for the same age;
4. global fallback pool;
5. legacy `ttsVoice`.

## Mood-aware delivery

Mood never replaces the persistent base voice. LivingWorld derives delivery state from authoritative server-owned data:

- active panic or high fear → afraid;
- critical health → sad;
- strongly negative trust → angry;
- high trust + affinity → happy;
- reduced health → tired;
- otherwise neutral.

The resulting provider-neutral style contains bounded speaking speed and optional natural-language delivery instructions. The TTS adapter uses only capabilities supported by the configured TTS model. Unsupported style features fail gracefully without changing the assigned voice.

For OpenAI-compatible speech, `tts-1` and `tts-1-hd` keep the stable voice and speed but omit unsupported `instructions`; instruction-capable TTS models receive the richer delivery hint.

## TTS credentials are independent from chat

Chat, STT and TTS may use different providers/accounts.

For the default OpenAI speech endpoint, TTS credential priority is:

1. `OPENAI_API_KEY`;
2. `ttsApiKey`;
3. the resolved main provider key **only when the main provider is also `openai`**.

If chat uses OpenRouter and neither `OPENAI_API_KEY` nor `ttsApiKey` is configured, OpenAI TTS is treated as not configured. The OpenRouter key is never sent to the OpenAI speech endpoint.

For a custom/non-OpenAI TTS endpoint:

1. `ttsApiKey`;
2. the resolved main provider key.

This allows, for example:

```text
OpenRouter → NPC LLM
Groq/OpenRouter → STT
OpenAI → TTS
```

without sending the OpenRouter chat key to the OpenAI speech endpoint.

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

TTS key resolution is documented separately above because it intentionally follows the TTS endpoint rather than the chat provider.

## Existing config migration

Config version 1 is migrated automatically to version 2:

- `voiceEnabled=true` becomes `voiceInputEnabled=true` and `voiceOutputEnabled=true`;
- `voiceEnabled=false` becomes both new switches `false`.

The migrated file is rewritten on server startup. Existing full-voice installations therefore preserve their previous behavior, while new installations default to microphone input with text-only answers.

Persistent voice profiles require no manual migration. `voices.json` is created lazily when TTS is first used for an NPC.

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
- `voices.json`

Back these files up with the Minecraft world.

## Voice requirements

Simple Voice Chat 2.6.20 or newer is required on the server and clients whenever `voiceInputEnabled=true`, even if NPC speech output is disabled. LivingWorld uses its microphone packet and Opus decoding APIs.

See `docs/livingworld/VOICE.md` for the full interaction flow and troubleshooting.

## Backward compatibility

If LivingWorld is disabled, uses an unsupported provider, or lacks the required main chat credential, the fork falls back to MCA's legacy ChatAI configuration. LivingWorld memory, factual events, structured relationships and new voice routing apply to the configured direct LivingWorld provider path.
