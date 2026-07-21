# LivingWorld microphone and NPC voice modes

## Required mods

Server and clients:

- Minecraft 1.21.1
- Fabric Loader / Fabric API
- this LivingWorld fork
- Simple Voice Chat 2.6.20 or newer

Only the server needs OpenAI/OpenRouter credentials.

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

## How to talk to an NPC

1. Interact normally with an MCA villager to select it as the current conversation target.
2. Look toward that villager and use the normal Simple Voice Chat microphone / push-to-talk key.
3. Stop speaking briefly. After roughly 800 ms of microphone inactivity, the utterance is finalized.
4. The server verifies that the target is still selected, visible and in the player's view before sending audio to STT.
5. The server runs STT and routes the transcript through the immutable LivingWorld context snapshot.
6. The answer is added to the existing MCA conversation text path.
7. When `voiceOutputEnabled=true`, LivingWorld additionally runs TTS and plays the result spatially from the NPC.

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

## Important behavior

- Ambient player voice is not sent to STT unless a villager was intentionally selected and the player is addressing that target.
- Normal player-to-player Simple Voice Chat traffic is not cancelled or modified.
- While a player's AI request is running, additional microphone packets are ignored by LivingWorld to avoid duplicate STT/LLM requests. They remain normal voice-chat audio.
- Voice input is capped at 20 seconds per utterance by default.
- Audio shorter than 250 ms is ignored.
- Text answers are produced independently from TTS availability.
- NPC spatial audio uses an entity-bound Simple Voice Chat channel with a default 32-block range only when output is enabled.
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
→ TTS
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

This is expected when `voiceOutputEnabled=false`. Enable it only if TTS output and its cost are desired.

### Wrong language recognition

Set `sttLanguage` to an ISO-639-1 code such as `ru`. Leave it blank for automatic detection.

### API errors

Inspect the dedicated server log. LivingWorld includes status/provider details but never prints API keys.
