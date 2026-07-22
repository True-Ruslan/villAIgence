# OpenRouter PCM TTS and Resilient Structured Response Parsing

## Goal

Make the complete LivingWorld voice path work reliably with OpenRouter TTS while guaranteeing that malformed structured LLM metadata can never leak raw JSON into visible or spoken NPC replies.

Target release: `0.1.3+1.21.1`.

## Current state and verified problem

The current `OpenAIAudioProvider` always sends `response_format: "wav"` and always decodes the successful TTS body with `WavCodec.decodePcm16Mono()`. This is incompatible with the preferred OpenRouter real-time TTS path, whose current public TTS guide documents `pcm` as the default raw output format and identifies `audio/pcm` as the PCM response content type.

OpenRouter documentation is not fully uniform: the general speech/model pages also mention WAV among possible model/provider formats. Therefore LivingWorld must not claim that OpenRouter can never return WAV. Instead, `auto` will deliberately choose raw PCM for `openrouter.ai` endpoints because that is the documented low-latency/default path, while explicit `wav` remains supported.

The current `StructuredAiResponseParser` already protects against typed malformed metadata such as `"fear":"none"`, but a syntactically malformed JSON object such as an unquoted invalid token can make whole-object parsing fail and lose an otherwise valid `message`. The old colon-based `cleanupAnswer()` helper still exists in `OpenAIChatAI` and is unsafe as a fallback pattern because it can expose JSON tails if reused.

## Scope corrections relative to the proposed PR text

The proposal is directionally correct but partially outdated against the current repository:

- persistent per-NPC voices, gender/age pools, mood-aware delivery, `TtsRequest`, and independent TTS credentials already exist and must be preserved;
- this release must not revert to one global voice;
- voice IDs are provider/model-specific; the implementation must not hardcode Grok model or voice IDs;
- the OpenRouter Grok example in documentation must use Grok-compatible configured voice pools (or otherwise explicitly configure compatible voice IDs), because the existing default pools contain OpenAI-oriented IDs;
- OpenRouter officially guarantees `audio/pcm` for PCM, but does not publicly guarantee `rate` and `channels` MIME parameters on every response. LivingWorld will parse those parameters when present, use configured sample-rate fallback when `rate` is absent, assume mono when `channels` is absent, and reject an explicit non-mono channel count.

## Architecture

### 1. TTS response format policy

Add provider-neutral `TtsResponseFormat`:

- `AUTO`
- `WAV`
- `PCM`

Configuration:

```java
public String ttsResponseFormat = "auto";
public int ttsPcmSampleRate = 24000;
```

Resolution:

- explicit `pcm` -> PCM;
- explicit `wav` -> WAV;
- `auto` + hostname `openrouter.ai` or subdomain -> PCM;
- `auto` + other endpoint -> WAV;
- unknown value -> `auto`;
- invalid/non-positive `ttsPcmSampleRate` -> `24000`.

Endpoint detection must use parsed URI hostname, not substring matching.

### 2. Raw PCM codec

Add `RawPcmCodec` in `livingworld/audio`.

Contract:

```java
public static PcmAudio decodePcm16Mono(byte[] bytes, int sampleRate)
```

Rules:

- signed PCM16 little-endian;
- even byte count required;
- positive sample rate required;
- empty byte array is valid and yields zero samples;
- codec is provider-agnostic and knows nothing about HTTP or OpenRouter.

### 3. Audio HTTP response metadata

Refactor successful audio HTTP execution to preserve:

```java
record AudioHttpResponse(
    byte[] body,
    String contentType,
    String generationId
) {}
```

The adapter must retain status handling and sanitized errors while exposing response metadata needed by codec selection.

For TTS:

- request `response_format` is the resolved lower-case format;
- PCM path requires `Content-Type` base type `audio/pcm`;
- WAV path continues using `WavCodec.decodePcm16Mono()` and accepts WAV-compatible content types;
- parse MIME parameters case-insensitively;
- `rate` present -> positive integer required;
- `rate` absent -> `ttsPcmSampleRate` fallback;
- `channels` present -> must equal `1`;
- `channels` absent -> assume mono;
- raw PCM is decoded directly; no fake WAV header is added or removed.

The existing caller continues to resample returned `PcmAudio` to Simple Voice Chat's 48 kHz rate.

### 4. Request headers and credentials

Preserve existing server-only credential resolution.

For OpenRouter endpoints:

- use the resolved server-side TTS key;
- continue sending `X-OpenRouter-Title: LivingWorld`;
- accept raw audio response types including `audio/pcm` and `audio/wav` plus JSON error responses.

No API key, Authorization header, raw audio, or complete provider payload may be logged.

### 5. Persistent voice compatibility

This release preserves the persistent profile system introduced previously.

The TTS adapter never chooses or hardcodes a model/voice. `TtsRequest.voiceId()` remains authoritative.

Because voice identifiers are provider-specific, documentation for `x-ai/grok-voice-tts-1.0` must show compatible voice-pool configuration using supported IDs. The implementation does not infer gender from provider voices and does not silently rewrite configured voice IDs.

### 6. Resilient structured response parser

Strengthen the existing `StructuredAiResponseParser`; do not introduce a second competing parser.

Algorithm:

1. Strip optional Markdown code fences.
2. Find the first JSON-object span when braces can be balanced safely.
3. Attempt normal JSON object parsing.
4. Parse `message`, `optionalCommand`, and `relationshipDelta` independently.
5. Invalid command -> empty command, preserve message.
6. Invalid relationship delta -> null/ignored delta, preserve message.
7. If whole-object parsing fails, scan only for a syntactically valid JSON string value assigned to the `message` field.
8. Decode that string with JSON escaping semantics, including escaped quotes and Unicode escapes.
9. Never return metadata or a JSON tail as visible text.
10. If no safe message can be recovered from a JSON-looking structured response, return no answer.
11. Plain non-JSON text remains a valid fallback for legacy/unstructured responses.

Relationship delta values remain integer-only. Clamping remains server-authoritative in `LivingWorldRelationshipStore.applyDelta(..., relationshipMaxDeltaPerTurn)` rather than duplicated in the parser.

Remove the unused colon-based `cleanupAnswer()` helper so it cannot become an unsafe future fallback.

## Failure semantics

The current conversation ordering is preserved:

1. AI answer is obtained.
2. Clean text is scheduled into MCA conversation UI.
3. TTS is attempted only afterward when enabled/configured.
4. TTS failure is logged and does not remove or invalidate the text reply.
5. Busy player/NPC locks are released in `finally`.

No retry is attempted for the same utterance.

Normal player-to-player Simple Voice Chat traffic remains untouched.

Raw microphone and synthesized audio remain in memory only and are not intentionally persisted.

## Logging

TTS failure logs should include concise context where available:

- operation;
- HTTP status;
- configured TTS model;
- resolved response format;
- response content type;
- generation ID;
- NPC UUID and player UUID at the conversation layer.

Do not log:

- API keys;
- Authorization headers;
- raw audio;
- full system prompts;
- complete provider error payloads.

Provider error details must be sanitized and length-bounded.

## Tests

### Configuration and format resolution

- default `ttsResponseFormat=auto`;
- `auto`, `pcm`, `wav` preserved;
- unknown -> `auto`;
- old version-2 config without new fields remains compatible;
- invalid sample rate -> `24000`;
- OpenRouter auto -> PCM;
- non-OpenRouter auto -> WAV;
- explicit format overrides endpoint.

### Raw PCM codec

- little-endian positive and negative samples;
- min/max sample values;
- empty body;
- odd byte count rejected;
- invalid sample rate rejected.

### TTS HTTP integration

Use an in-process JDK HTTP server.

Verify:

- OpenRouter-like auto request sends `"response_format":"pcm"`;
- `audio/pcm;rate=24000;channels=1` decodes to 24 kHz mono PCM;
- missing rate uses config fallback;
- missing channels assumes mono;
- explicit stereo is rejected;
- malformed rate/channels are rejected;
- wrong content type for requested PCM is rejected;
- generation ID is retained for diagnostics;
- WAV behavior remains operational;
- caller resampling to 48 kHz remains operational.

### Structured response parsing

- fully valid structured response;
- non-numeric typed fear (`"none"`) preserves message and ignores delta;
- syntactically malformed bare `fear: INVALID_VALUE` recovers only message;
- malformed optional command preserves message;
- escaped quotes inside message;
- Unicode/Russian message;
- Markdown code fences;
- plain text plus metadata tail returns only plain text;
- unrecoverable JSON returns no answer;
- no returned message contains leaked JSON tail.

## Documentation

Update:

- `README.md` where needed;
- `docs/livingworld/VOICE.md`;
- `docs/livingworld/CONFIGURATION.md`.

Document:

- OpenRouter PCM auto behavior;
- `ttsResponseFormat` and `ttsPcmSampleRate`;
- raw PCM / 24 kHz fallback / 48 kHz resampling;
- example OpenRouter Grok configuration with provider-compatible voice pools;
- server-only credentials and paid usage;
- text survives TTS failure;
- OpenRouter docs may support additional formats by model/provider, but LivingWorld intentionally supports PCM and WAV in this release.

## Verification gate

Required before merge:

```bash
./gradlew :common:test :fabric:build
```

Repository CI must also be green for:

- LivingWorld CI including distributable package smoke-check;
- official Fabric + NeoForge Gradle CI.

Manual acceptance remains a post-build server test for microphone -> STT -> clean NPC text -> TTS -> spatial playback, including forced TTS failure and normal player voice-chat coexistence.
