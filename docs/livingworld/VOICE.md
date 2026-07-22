# LivingWorld microphone and NPC voice modes

## Required mods

Server and clients:

- Minecraft 1.21.1
- Fabric Loader / Fabric API
- this LivingWorld fork
- Simple Voice Chat 2.6.20 or newer

Only the server needs AI/STT/TTS credentials.

Simple Voice Chat is still required when NPC TTS is disabled because LivingWorld uses its microphone packets and Opus decoder for voice input.

## Recommended mode

```text
player microphone → STT → LivingWorld NPC AI → text in MCA conversation/chat UI
```

```json
{
  "voiceInputEnabled": true,
  "voiceOutputEnabled": false
}
```

The answer is shown as text. LivingWorld does not call TTS and does not create spatial NPC audio.

## Full voice mode

```text
player microphone → STT → LivingWorld NPC AI → text + TTS spatial NPC audio
```

```json
{
  "voiceInputEnabled": true,
  "voiceOutputEnabled": true
}
```

## Stable NPC voice identity

TTS voice selection is independent from the chat/LLM model.

For every NPC, LivingWorld uses:

```text
NPC UUID + MCA gender + MCA age state
→ compatible configured voice pool
→ deterministic voice selection
→ persistent profile in <world>/livingworld/voices.json
```

The profile survives server restarts. Changing from one chat model/provider to another does not change the NPC's stored voice.

MCA age mapping:

- `BABY`, `TODDLER`, `CHILD` → child voice profile;
- `TEEN` → teen voice profile;
- `ADULT`, `UNASSIGNED` → adult voice profile.

There is no separate elder state in current MCA 1.21.1, so no synthetic elder classification is used.

The configured male/female/neutral voice pools are LivingWorld defaults only. They can be replaced with any IDs supported by the chosen TTS provider. LivingWorld does not rely on a provider formally labeling a voice as male, female, child or teen.

A voice profile is resolved again only when the NPC changes gender/age bucket or the stored voice no longer belongs to the configured compatible fallback pools.

## Mood-aware delivery

Mood changes **how** an NPC speaks, never **which persistent voice** belongs to that NPC.

LivingWorld derives delivery mood from server-owned state, not from arbitrary LLM text. Current signals include:

- panic / high fear;
- current health ratio;
- persistent trust and affinity with the player.

The resulting moods are neutral, happy, sad, angry, afraid or tired. They map to bounded speaking speed and natural-language delivery hints such as restrained irritation, subdued sadness or tense fear.

The TTS adapter applies capabilities on a best-effort basis:

- instruction-capable speech models receive style instructions plus speed;
- legacy models that do not support instructions still receive the persistent voice and supported speed control;
- unsupported style features do not replace or randomize the NPC voice.

## How to talk to an NPC

1. Interact normally with an MCA villager to select it as the current conversation target.
2. Look toward that villager and use the normal Simple Voice Chat microphone / push-to-talk key.
3. Stop speaking briefly. After roughly 800 ms of microphone inactivity, the utterance is finalized.
4. The server verifies that the target is still selected, visible and in the player's view before sending audio to STT.
5. The server runs STT and routes the transcript through the immutable LivingWorld context snapshot.
6. The answer is added to the existing MCA conversation text path.
7. When `voiceOutputEnabled=true`, LivingWorld resolves the NPC's stable voice profile, derives current delivery mood, runs TTS and plays the result spatially from that NPC.

The selected target expires using MCA's existing conversation distance and timeout rules. Interact with the villager again to re-select it.

## OpenRouter STT

Recommended server environment variable:

```bash
export OPENROUTER_API_KEY="sk-or-v1-..."
```

Recommended config:

```json
{
  "sttEndpoint": "https://openrouter.ai/api/v1/audio/transcriptions",
  "sttModel": "openai/gpt-4o-mini-transcribe",
  "sttRequestFormat": "json_base64",
  "sttLanguage": "ru"
}
```

OpenRouter receives:

```json
{
  "model": "openai/gpt-4o-mini-transcribe",
  "input_audio": {
    "data": "<raw-base64-wav>",
    "format": "wav"
  },
  "language": "ru"
}
```

The Base64 value is raw audio bytes, not a `data:audio/...` URI.

`sttRequestFormat="auto"` selects `json_base64` automatically for `openrouter.ai`; non-OpenRouter endpoints retain multipart WAV upload compatibility.

## Using OpenRouter chat with OpenAI TTS

Chat and TTS credentials are resolved independently. A common full-voice setup can use:

```text
OpenRouter key → chat/LLM
OpenRouter or another STT key → transcription
OpenAI key → TTS
```

For the default OpenAI speech endpoint, set:

```bash
export OPENAI_API_KEY="sk-..."
```

or configure a dedicated server-only `ttsApiKey`.

This prevents an OpenRouter chat key from being incorrectly sent to the OpenAI speech endpoint.

## Important behavior

- Ambient player voice is not sent to STT unless a villager was intentionally selected and the player is addressing that target.
- Normal player-to-player Simple Voice Chat traffic is not cancelled or modified.
- While a player's AI request is running, additional microphone packets are ignored by LivingWorld to avoid duplicate STT/LLM requests. They remain normal voice-chat audio.
- Voice input is capped at 20 seconds per utterance by default.
- Audio shorter than 250 ms is ignored.
- Text answers are produced independently from TTS availability.
- NPC spatial audio uses an entity-bound Simple Voice Chat channel with a default 32-block range only when output is enabled.
- Mutable MCA gender/age/panic/health state is captured on the Minecraft server thread before asynchronous TTS work.
- Raw microphone audio is processed in memory and is not intentionally persisted.

## Data flows

Text-only mode:

```text
microphone
→ Simple Voice Chat
→ Opus decode
→ target validation
→ 48 kHz PCM
→ WAV
→ STT
→ LivingWorld ChatAI
→ MCA conversation text
```

Full voice mode adds:

```text
NPC text
+ persistent NPC voice profile
+ server-derived mood style
→ provider-neutral TTS request
→ TTS adapter
→ PCM/WAV decode
→ 48 kHz resample
→ Simple Voice Chat spatial NPC audio
```

## Troubleshooting

### No NPC reaction

Verify:

- `voiceInputEnabled=true`;
- the player interacted with the NPC first;
- the player is looking toward the NPC while speaking;
- Simple Voice Chat is connected;
- the STT endpoint, model and key are valid.

### OpenRouter HTTP 402

`402 Payment Required` means the OpenRouter account has no usable credits for the selected STT model. Add balance and retry. It does not indicate an invalid WAV/Base64 payload.

### Text works, but the NPC is silent

Verify:

- `voiceOutputEnabled=true`;
- the TTS endpoint/model supports the configured voice IDs;
- the TTS credential is configured. For the default OpenAI TTS endpoint, use `OPENAI_API_KEY` or `ttsApiKey` even when chat uses OpenRouter.

### NPC got a different voice after growing up

This is expected only when the NPC crosses a real MCA age bucket, for example child → teen or teen → adult. The new compatible voice is persisted for the new age bucket.

### NPC voices need different classification

Edit the `male*Voices`, `female*Voices`, `neutral*Voices` and `globalVoiceFallbacks` arrays in `config/livingworld.json`. The built-in grouping is a LivingWorld default, not provider gender metadata.

### Wrong language recognition

Set `sttLanguage` to an ISO-639-1 code such as `ru`. Leave it blank for automatic detection.

### API errors

Inspect the dedicated server log. LivingWorld includes status/provider details but never prints API keys.
