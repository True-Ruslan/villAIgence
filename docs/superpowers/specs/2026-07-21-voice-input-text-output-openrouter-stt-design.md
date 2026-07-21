# Voice Input / Text Output with OpenRouter STT

## Goal

Support the recommended low-cost interaction mode:

`player microphone → STT → LivingWorld NPC AI → text in Minecraft chat`

NPC speech synthesis and spatial playback must be independently disableable without disabling microphone input.

## Configuration

LivingWorld config moves to version 2 and replaces the single voice switch with:

- `voiceInputEnabled` — capture microphone packets, segment speech and run STT;
- `voiceOutputEnabled` — synthesize NPC answers and play spatial audio;
- `sttRequestFormat` — `auto`, `multipart`, or `json_base64`;
- `sttApiKey` — optional dedicated STT credential.

New-install defaults are `voiceInputEnabled=true` and `voiceOutputEnabled=false`. Existing v1 configs migrate `voiceEnabled` to both new switches to preserve their previous behavior.

## Provider and credential behavior

OpenAI multipart STT remains supported. OpenRouter uses JSON with raw Base64 WAV bytes:

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

`sttRequestFormat=auto` selects `json_base64` for an `openrouter.ai` STT endpoint and `multipart` otherwise.

Credential priority for OpenRouter STT:

1. `OPENROUTER_API_KEY` environment variable;
2. `sttApiKey`;
3. main LivingWorld API key.

For non-OpenRouter STT, `sttApiKey` overrides the main provider key.

The main `provider` setting accepts both `openai` and `openrouter`; OpenRouter remains OpenAI-compatible for chat calls.

## Runtime flow

1. Simple Voice Chat microphone packets are captured only when `voiceInputEnabled` and LivingWorld AI are configured.
2. Audio is decoded and segmented as before.
3. STT transport is resolved from configuration.
4. Transcript is routed through the existing immutable snapshot-aware NPC AI path.
5. The NPC answer is always added to the conversation/chat UI.
6. TTS and spatial playback occur only when `voiceOutputEnabled=true`.

No TTS request is made in text-only mode.

## Error handling

- HTTP errors include provider-neutral diagnostics.
- OpenRouter HTTP 402 explicitly explains that the account needs credits/balance.
- Invalid `sttRequestFormat` normalizes to `auto`.
- Missing STT credentials fail before sending audio.
- Existing server/thread safety and busy-player/busy-villager gates remain unchanged.

## Compatibility

- Config v1 is migrated and rewritten as v2.
- OpenAI multipart STT remains functional.
- Existing full voice users retain full voice after v1 migration.
- New installations default to text answers to avoid TTS cost.
- Clients still require Simple Voice Chat because microphone capture uses its API.

## Verification

- unit tests for defaults, provider key resolution, config v1 migration and STT format resolution;
- Fabric and NeoForge compile/build;
- release-package smoke check;
- documentation for OpenRouter setup and HTTP 402 troubleshooting.
