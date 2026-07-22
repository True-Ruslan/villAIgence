# LivingWorld microphone and NPC voice modes

## Required mods

Server and clients:

- Minecraft 1.21.1
- Fabric Loader / Fabric API
- the same LivingWorld release JAR
- Simple Voice Chat 2.6.20 or newer

Only the server needs AI/STT/TTS credentials.

## Voice modes

Recommended text-output mode:

```text
player microphone
→ Simple Voice Chat
→ STT
→ NPC AI
→ clean MCA text reply
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
→ Simple Voice Chat
→ STT
→ NPC AI
→ clean MCA text reply
→ TTS
→ spatial NPC audio
```

```json
{
  "voiceInputEnabled": true,
  "voiceOutputEnabled": true
}
```

`voiceOutputEnabled=false` remains the default.

## Stable NPC voice identity

TTS transport is separate from NPC voice identity.

```text
NPC UUID + MCA gender + MCA age state
→ compatible configured voice pool
→ deterministic selected voice
→ <world>/livingworld/voices.json
```

The profile survives restarts and LLM changes. Mood changes delivery style, not the persistent voice ID.

MCA age mapping:

- `BABY`, `TODDLER`, `CHILD` → child profile;
- `TEEN` → teen profile;
- `ADULT`, `UNASSIGNED` → adult profile.

Current MCA 1.21.1 has no separate elder state.

Voice IDs are provider/model-specific. When changing TTS provider, update all relevant voice pools. Stored profiles using IDs that are no longer eligible are automatically replaced with a compatible configured voice.

## How to talk to an NPC

1. Interact with an MCA villager to select it as the current target.
2. Close or leave the interaction UI as usual.
3. Look toward that villager.
4. Hold the normal Simple Voice Chat push-to-talk key and speak.
5. Stop speaking; after the configured silence segmentation, LivingWorld finalizes the utterance.
6. The server revalidates the selected target and sends audio to STT.
7. The AI answer is sanitized and shown through the MCA text conversation path.
8. If TTS is enabled, the same clean text is synthesized and played spatially from the NPC.

## OpenRouter STT

Example:

```json
{
  "sttEndpoint": "https://openrouter.ai/api/v1/audio/transcriptions",
  "sttModel": "openai/gpt-4o-mini-transcribe",
  "sttRequestFormat": "json_base64",
  "sttLanguage": "ru"
}
```

`sttRequestFormat="auto"` also chooses JSON/Base64 for `openrouter.ai`.

The Base64 field contains raw WAV bytes, not a `data:audio/...` URI.

## OpenRouter raw PCM TTS

LivingWorld supports two TTS response transports:

- `wav`
- raw `pcm`

Configuration:

```json
{
  "ttsResponseFormat": "auto",
  "ttsPcmSampleRate": 24000
}
```

Resolution:

```text
auto + openrouter.ai → pcm
auto + other endpoint → wav
explicit pcm/wav     → explicit value wins
```

For raw PCM, LivingWorld expects `Content-Type: audio/pcm`.

Optional MIME parameters are handled when present:

```text
Content-Type: audio/pcm;rate=24000;channels=1
```

Rules:

- `rate` present → use it after validation;
- `rate` absent → use `ttsPcmSampleRate` (default 24000 Hz);
- `channels` absent → assume mono;
- explicit `channels` must equal `1`;
- body must contain an even number of bytes;
- samples are decoded as signed PCM16 little-endian;
- no fake RIFF/WAV header is added;
- decoded audio is resampled through the existing path to 48 kHz before Simple Voice Chat playback.

WAV responses continue through the existing WAV decoder.

OpenRouter currently documents PCM as the default/real-time-friendly TTS response format and returns raw audio bytes rather than JSON on success. `X-Generation-Id`, when present, is retained for concise diagnostics.

## Example: OpenRouter Grok voice TTS

Example server configuration:

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
  "ttsVoice": "rex"
}
```

The model and voice IDs are examples from the selected provider/model, not hardcoded LivingWorld behavior.

The repeated voice lists intentionally avoid claiming that the provider defines male/female/child/teen categories. Classify voice IDs into narrower LivingWorld pools only after validating them for your server.

Using only `ttsVoice="rex"` while leaving old incompatible pools unchanged is not sufficient: persistent profile selection uses configured pools before the legacy fallback. Update the pools when switching providers.

## Mood-aware delivery

LivingWorld derives delivery mood from server-owned state:

- panic/high fear → afraid;
- critical health → sad;
- strongly negative trust → angry;
- high trust + affinity → happy;
- reduced health → tired;
- otherwise neutral.

The base voice does not change because of mood.

A bounded `speed` value is included in the provider-neutral speech request. Providers/models that do not support speed may ignore it. Additional style controls are best-effort and must not replace the persistent voice identity.

## Structured response sanitation

Before text or speech is published, LivingWorld sanitizes structured AI output.

Valid shape:

```json
{
  "message": "Да, я житель!",
  "optionalCommand": "",
  "relationshipDelta": {
    "trust": 0,
    "respect": 0,
    "fear": 0,
    "affinity": 0
  }
}
```

If optional metadata is malformed:

- valid `message` is preserved;
- invalid command is ignored;
- invalid relationship delta is ignored;
- integer relationship deltas are clamped later by server-authoritative relationship logic.

Even if the object itself contains malformed syntax such as:

```text
"fear": INVALID_VALUE
```

LivingWorld recovers only a syntactically valid top-level JSON string value for `message`. It never speaks or displays the remaining JSON tail.

If no safe message can be recovered from a JSON-looking response, no answer from that response is published.

## Text-first failure semantics

The order is intentional:

```text
AI response
→ sanitize message
→ publish MCA text
→ resolve persistent NPC voice/mood
→ call TTS
→ decode PCM/WAV
→ resample to 48 kHz
→ spatial playback
```

Therefore a TTS HTTP error, unsupported format, invalid PCM metadata, decode error or playback failure cannot remove the already-published text answer.

LivingWorld does not automatically retry TTS for the same utterance. Busy player/NPC locks are released normally.

## Important behavior

- Ambient player voice is not sent to STT unless a villager was intentionally selected and addressed.
- Normal player-to-player Simple Voice Chat traffic is not cancelled or modified.
- Additional microphone packets are ignored by LivingWorld while that player's AI request is already in flight; they remain normal voice-chat traffic.
- Voice input is capped by `voiceMaxSeconds`.
- Short audio below `voiceMinMillis` is ignored.
- NPC audio is entity-bound spatial audio with `voiceDistance` range.
- Mutable MCA gender/age/panic/health state is captured on the Minecraft server thread before async TTS work.
- Raw microphone and synthesized audio are processed in memory and are not intentionally persisted.
- API keys remain server-side.

## Data flow

```text
microphone
→ Simple Voice Chat Opus packets
→ PCM
→ WAV encoding for STT
→ STT
→ NPC AI
→ resilient structured-response parser
→ clean MCA text reply
→ persistent NPC voice + server-derived mood
→ TTS request
→ raw PCM or WAV response
→ mono PCM samples
→ 48 kHz resample
→ spatial Simple Voice Chat playback
```

## Troubleshooting

### No NPC reaction

Check:

- `voiceInputEnabled=true`;
- the player interacted with the NPC first;
- the player is looking toward the NPC;
- Simple Voice Chat is connected;
- STT endpoint/model/key are valid.

### OpenRouter HTTP 402

`402 Payment Required` means the OpenRouter account has no usable credits for the selected request/model. STT and TTS are billed API usage.

### Text works, but NPC is silent

Check:

- `voiceOutputEnabled=true`;
- TTS endpoint/model/key are valid;
- configured persistent voice pools contain IDs supported by the selected TTS model;
- `ttsResponseFormat` matches the endpoint behavior (`auto` is recommended for OpenRouter);
- for PCM, the server log does not report invalid `Content-Type`, `rate`, `channels` or odd PCM16 byte count.

Text working while speech fails is expected fail-open behavior: text is deliberately independent from TTS success.

### NPC got a different voice after switching TTS provider

Expected when the old stored voice ID is no longer eligible under the new provider-compatible pools. LivingWorld resolves a new compatible persistent voice.

### NPC got a different voice after growing up

Expected when crossing a real MCA age bucket such as child → teen or teen → adult.

### Wrong language recognition

Set `sttLanguage` to an ISO-639-1 code such as `ru`, or leave blank for automatic detection.

### API/TTS decode errors

Inspect the dedicated server log. Diagnostics may include operation/status, model, response format, content type and generation ID. LivingWorld does not intentionally log API keys, Authorization headers, raw audio or full prompts.
