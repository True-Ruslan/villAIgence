# VillAIgence microphone and NPC voice modes

VillAIgence uses Simple Voice Chat for microphone capture and spatial NPC playback. `LivingWorld` remains the internal engine namespace used by compatibility-sensitive classes and paths.

## Required mods

Server and clients:

- Minecraft 1.21.1
- Fabric Loader / Fabric API
- the same VillAIgence release JAR
- Simple Voice Chat 2.6.20+

Only the server needs AI/STT/TTS credentials.

## Voice modes

Recommended text-output mode:

```text
player microphone
→ Simple Voice Chat
→ STT
→ VillAIgence NPC AI
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
→ persistent NPC voice + mood
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

The profile survives restarts and LLM changes. Mood changes delivery style, not persistent voice ID.

MCA age mapping:

- `BABY`, `TODDLER`, `CHILD` → child profile;
- `TEEN` → teen profile;
- `ADULT`, `UNASSIGNED` → adult profile.

Current MCA 1.21.1 has no separate elder state.

Voice IDs are provider/model-specific. When changing TTS provider, update all relevant voice pools. Stored profiles using no-longer-eligible IDs are replaced with compatible configured voices.

## How to talk to an NPC

1. Interact with an MCA villager to select it as the current target.
2. Close or leave the interaction UI.
3. Look toward the selected villager.
4. Hold the normal Simple Voice Chat push-to-talk key and speak.
5. Stop speaking; after configured silence segmentation, VillAIgence finalizes the utterance.
6. The server revalidates the selected target and sends audio to STT.
7. The AI answer is sanitized and shown through the MCA text conversation path.
8. If TTS is enabled, the same clean text is synthesized and played spatially from the NPC.

## OpenRouter STT

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

VillAIgence supports two TTS response transports:

- `wav`
- raw `pcm`

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

For raw PCM, VillAIgence expects base `Content-Type: audio/pcm`.

Optional MIME parameters are handled when present:

```text
Content-Type: audio/pcm;rate=24000;channels=1
```

Rules:

- `rate` present → use it after validation;
- `rate` absent → use `ttsPcmSampleRate`;
- `channels` absent → assume mono;
- explicit `channels` must equal `1`;
- body byte count must be even;
- samples are signed PCM16 little-endian;
- no fake RIFF/WAV header is added;
- decoded audio is resampled to 48 kHz before Simple Voice Chat playback.

WAV responses continue through the existing WAV decoder.

## Example: OpenRouter voice TTS

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

Model and voice IDs are provider-specific examples, not hardcoded VillAIgence behavior.

Using only `ttsVoice` while leaving incompatible old pools unchanged is not sufficient because persistent profile selection evaluates configured pools first.

## Mood-aware delivery

VillAIgence derives delivery mood from server-owned state:

- panic/high fear → afraid;
- critical health → sad;
- strongly negative trust → angry;
- high trust + affinity → happy;
- reduced health → tired;
- otherwise neutral.

The base voice does not change because of mood.

A bounded `speed` value is included in the provider-neutral speech request. Additional style controls are best-effort and never replace persistent voice identity.

## Structured response sanitation

Before text or speech is published, VillAIgence sanitizes structured AI output.

A valid response may contain:

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

Even when the object contains malformed syntax such as:

```text
"fear": INVALID_VALUE
```

VillAIgence recovers only a syntactically valid top-level JSON string `message`. It never speaks/displays the remaining JSON tail.

If no safe message can be recovered from a JSON-looking response, no unsafe answer is published.

## Text-first failure semantics

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

A TTS HTTP/format/decode/playback failure cannot remove the already-published text answer.

VillAIgence does not automatically retry TTS for the same utterance. Busy player/NPC locks are released normally.

## Important behavior

- Ambient player voice is not sent to STT unless a villager was intentionally selected and addressed.
- Normal player-to-player Simple Voice Chat traffic is not cancelled or modified.
- Additional microphone packets are ignored by VillAIgence while that player's AI request is already in flight; they remain normal voice-chat traffic.
- Voice input is capped by `voiceMaxSeconds`.
- Short audio below `voiceMinMillis` is ignored.
- NPC audio is entity-bound spatial audio with `voiceDistance` range.
- Mutable MCA gender/age/panic/health state is captured on the Minecraft server thread before async TTS work.
- Raw microphone and synthesized audio are processed in memory and are not intentionally persisted.
- API keys remain server-side.

## Internal compatibility names

The public mod is **VillAIgence**, but these remain unchanged:

```text
config/livingworld.json
<world>/livingworld/
net.conczin.mca.livingworld.*
mod id: mca
```

`LivingWorld` therefore remains visible in some internal class/log/config names by design.

## Data flow

```text
microphone
→ Simple Voice Chat Opus packets
→ PCM
→ WAV encoding for STT
→ STT
→ VillAIgence NPC AI
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
- the player selected the NPC first;
- the player is looking toward the NPC;
- Simple Voice Chat is connected;
- STT endpoint/model/key are valid.

### OpenRouter HTTP 402

`402 Payment Required` means the OpenRouter account has no usable credits for the selected request/model. STT and TTS are billed API usage.

### Text works, but NPC is silent

Check:

- `voiceOutputEnabled=true`;
- TTS endpoint/model/key are valid;
- persistent voice pools contain IDs supported by the selected TTS model;
- `ttsResponseFormat` matches endpoint behavior (`auto` is recommended for OpenRouter);
- server logs do not report invalid PCM `Content-Type`, `rate`, `channels` or byte count.

Text working while speech fails is expected fail-open behavior.

### NPC voice changed after switching provider

Expected when the stored voice ID is no longer eligible under new provider-compatible pools. VillAIgence resolves a compatible replacement and persists it.

### NPC voice changed after growing up

Expected when crossing a real MCA age bucket such as child → teen or teen → adult.

### Wrong language recognition

Set `sttLanguage` to an ISO-639-1 code such as `ru`, or leave blank for automatic detection.

### API/TTS decode errors

Inspect the dedicated server log. Diagnostics may include operation/status, model, response format, content type and generation ID. VillAIgence does not intentionally log API keys, Authorization headers, raw audio or full prompts.
