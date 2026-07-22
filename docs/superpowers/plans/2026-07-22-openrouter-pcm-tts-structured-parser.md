# OpenRouter PCM TTS and Resilient Structured Parser Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add reliable OpenRouter raw-PCM TTS support and recover clean NPC messages from partially malformed structured LLM responses without regressing persistent NPC voices.

**Architecture:** Extend the existing OpenAI-compatible audio adapter with provider-neutral `TtsResponseFormat` selection and a separate raw PCM codec. Preserve HTTP response metadata for decoding/diagnostics. Strengthen the existing `StructuredAiResponseParser` with safe JSON-string message recovery rather than introducing a competing parser.

**Tech Stack:** Java 21, Gson, JDK `HttpURLConnection`, JDK `HttpServer` for tests, Fabric 1.21.1, Simple Voice Chat 2.6.20+, JUnit 5.

## Global Constraints

- `voiceOutputEnabled=false` remains the default.
- Persistent per-NPC voices, age/gender pools, mood styling, and server-only credential isolation remain intact.
- `auto` + OpenRouter endpoint resolves to raw PCM; `auto` + other endpoints resolves to WAV.
- Raw PCM is signed 16-bit little-endian mono and is never wrapped in a fake WAV header.
- Text replies must survive TTS failure.
- No automatic retry for the same utterance.
- No raw microphone or synthesized audio persistence.
- Malformed structured metadata must never become visible or audible.
- No model or voice ID is hardcoded in runtime implementation.
- Full LivingWorld CI and official Fabric/NeoForge CI must be green before merge.

---

### Task 1: TTS response-format configuration and endpoint resolution

**Files:**
- Create: `common/src/main/java/net/conczin/mca/livingworld/voice/TtsResponseFormat.java`
- Create: `common/src/test/java/net/conczin/mca/livingworld/voice/TtsResponseFormatTest.java`
- Modify: `common/src/main/java/net/conczin/mca/livingworld/LivingWorldConfig.java`
- Modify: `common/src/test/java/net/conczin/mca/livingworld/LivingWorldConfigTest.java`

**Interfaces:**
- Produces: `TtsResponseFormat.parse(String)` and `TtsResponseFormat.resolve(String endpoint)`.
- Produces config fields `ttsResponseFormat` and `ttsPcmSampleRate`.

- [ ] **Step 1: Add failing format-resolution tests**

Test exact behavior:

```java
assertEquals(TtsResponseFormat.PCM, TtsResponseFormat.AUTO.resolve("https://openrouter.ai/api/v1/audio/speech"));
assertEquals(TtsResponseFormat.WAV, TtsResponseFormat.AUTO.resolve("https://api.openai.com/v1/audio/speech"));
assertEquals(TtsResponseFormat.WAV, TtsResponseFormat.WAV.resolve("https://openrouter.ai/api/v1/audio/speech"));
assertEquals(TtsResponseFormat.PCM, TtsResponseFormat.PCM.resolve("https://api.openai.com/v1/audio/speech"));
assertEquals(TtsResponseFormat.AUTO, TtsResponseFormat.parse("unknown"));
assertEquals(TtsResponseFormat.WAV, TtsResponseFormat.AUTO.resolve("https://evil.example/openrouter.ai/api/v1/audio/speech"));
```

- [ ] **Step 2: Run RED test**

Run:

```bash
./gradlew :common:test --tests '*TtsResponseFormatTest' --no-daemon
```

Expected: compile/test failure because `TtsResponseFormat` does not exist.

- [ ] **Step 3: Implement `TtsResponseFormat`**

Use URI hostname parsing. `AUTO` resolves to PCM only for `openrouter.ai` or its subdomains; explicit values override endpoint.

- [ ] **Step 4: Add failing config tests**

Verify:

```java
LivingWorldConfig defaults = new LivingWorldConfig();
assertEquals("auto", defaults.ttsResponseFormat);
assertEquals(24000, defaults.ttsPcmSampleRate);

LivingWorldConfig invalid = LivingWorldConfig.parseJson("""
{"version":2,"ttsResponseFormat":"bogus","ttsPcmSampleRate":0}
""");
assertEquals("auto", invalid.ttsResponseFormat);
assertEquals(24000, invalid.ttsPcmSampleRate);
```

Also verify `pcm` and `wav` are preserved and a version-2 config omitting new fields receives compatible defaults.

- [ ] **Step 5: Run RED config tests**

```bash
./gradlew :common:test --tests '*LivingWorldConfigTest' --no-daemon
```

Expected: failure because new fields/normalization do not exist.

- [ ] **Step 6: Implement config fields and normalization**

Add:

```java
public String ttsResponseFormat = "auto";
public int ttsPcmSampleRate = 24000;
```

Normalize with `TtsResponseFormat.parse(...).configValue()` and reset non-positive sample rate to `24000`.

- [ ] **Step 7: Run focused tests GREEN**

```bash
./gradlew :common:test --tests '*TtsResponseFormatTest' --tests '*LivingWorldConfigTest' --no-daemon
```

Expected: `BUILD SUCCESSFUL`.

---

### Task 2: Raw PCM codec and metadata-preserving TTS transport

**Files:**
- Create: `common/src/main/java/net/conczin/mca/livingworld/audio/RawPcmCodec.java`
- Create: `common/src/test/java/net/conczin/mca/livingworld/audio/RawPcmCodecTest.java`
- Modify: `common/src/main/java/net/conczin/mca/livingworld/voice/OpenAIAudioProvider.java`
- Modify: `common/src/test/java/net/conczin/mca/livingworld/voice/OpenAIAudioProviderTest.java`

**Interfaces:**
- Consumes: `TtsResponseFormat` and `LivingWorldConfig.ttsPcmSampleRate` from Task 1.
- Produces: `RawPcmCodec.decodePcm16Mono(byte[], int)`.
- Produces internal `AudioHttpResponse(byte[] body, String contentType, String generationId)` metadata result.

- [ ] **Step 1: Add failing raw PCM codec tests**

Cover:

```java
byte[] bytes = {0x01, 0x00, (byte) 0xff, (byte) 0xff, 0x00, (byte) 0x80, (byte) 0xff, 0x7f};
PcmAudio audio = RawPcmCodec.decodePcm16Mono(bytes, 24000);
assertArrayEquals(new short[]{1, -1, Short.MIN_VALUE, Short.MAX_VALUE}, audio.samples());
assertEquals(24000, audio.sampleRate());
```

Also test empty bytes, odd byte count rejection, and non-positive sample rate rejection.

- [ ] **Step 2: Run RED codec test**

```bash
./gradlew :common:test --tests '*RawPcmCodecTest' --no-daemon
```

Expected: failure because codec does not exist.

- [ ] **Step 3: Implement minimal provider-neutral codec**

Decode each pair as:

```java
int lo = bytes[i] & 0xff;
int hi = bytes[i + 1];
samples[i / 2] = (short) ((hi << 8) | lo);
```

Reject odd byte length and invalid sample rate.

- [ ] **Step 4: Add failing TTS request/HTTP integration tests**

Use `com.sun.net.httpserver.HttpServer` bound to loopback/ephemeral port. Tests must verify:

1. auto/OpenRouter-like endpoint sends `"response_format":"pcm"`;
2. `Content-Type: audio/pcm;rate=24000;channels=1` decodes PCM correctly;
3. missing `rate` uses `ttsPcmSampleRate`;
4. missing `channels` assumes mono;
5. explicit `channels=2` rejects;
6. malformed `rate` rejects;
7. odd PCM body rejects;
8. wrong content type for PCM rejects;
9. `X-Generation-Id` is captured for diagnostics;
10. explicit/auto WAV still decodes through `WavCodec`;
11. returned 24 kHz `PcmAudio.resampleTo(48000)` produces 48 kHz output.

- [ ] **Step 5: Run RED provider tests**

```bash
./gradlew :common:test --tests '*OpenAIAudioProviderTest' --no-daemon
```

Expected: failures because request format is hardcoded to WAV and response metadata is discarded.

- [ ] **Step 6: Refactor HTTP helper and TTS decode path**

Implement an internal record:

```java
record AudioHttpResponse(byte[] body, String contentType, String generationId) {}
```

`execute(...)` returns this record on success. STT consumes `.body()` unchanged. TTS resolves `TtsResponseFormat`, builds the matching request body, and dispatches:

```java
case PCM -> decodeRawPcm(response, config.ttsPcmSampleRate);
case WAV -> WavCodec.decodePcm16Mono(response.body());
```

Parse MIME parameters case-insensitively. Missing rate uses config fallback. Missing channels means mono. Explicit channels other than 1 are rejected.

- [ ] **Step 7: Keep errors concise and metadata-safe**

Bound provider error detail length, never include auth or audio bytes, and include format/content-type/generation ID in TTS decode failures where available.

- [ ] **Step 8: Run codec/provider tests GREEN**

```bash
./gradlew :common:test --tests '*RawPcmCodecTest' --tests '*OpenAIAudioProviderTest' --no-daemon
```

Expected: `BUILD SUCCESSFUL`.

---

### Task 3: Recover clean messages from syntactically malformed structured responses

**Files:**
- Modify: `common/src/main/java/net/conczin/mca/livingworld/ai/StructuredAiResponseParser.java`
- Modify: `common/src/test/java/net/conczin/mca/livingworld/ai/StructuredAiResponseParserTest.java`
- Modify: `common/src/main/java/net/conczin/mca/entity/ai/chatAI/OpenAIChatAI.java`

**Interfaces:**
- Produces unchanged public `StructuredAiResponseParser.parse(String)` API.
- Keeps clamping responsibility in `LivingWorldRelationshipStore.applyDelta(...)`.

- [ ] **Step 1: Add failing malformed-object recovery tests**

Add exact cases:

```java
String malformed = """
{"message":"Да, я житель!","optionalCommand":"","relationshipDelta":{"trust":0,"respect":0,"fear":INVALID_VALUE,"affinity":0}}
""";
assertEquals("Да, я житель!", StructuredAiResponseParser.parse(malformed).message());
assertNull(StructuredAiResponseParser.parse(malformed).relationshipDelta());
```

Also cover escaped quotes, Unicode/Russian, code fences, invalid optional command type, unrecoverable JSON, and assert returned message never contains `relationshipDelta`/JSON tail.

- [ ] **Step 2: Run RED parser tests**

```bash
./gradlew :common:test --tests '*StructuredAiResponseParserTest' --no-daemon
```

Expected: the bare invalid-token case fails because whole-object Gson parsing cannot recover `message`.

- [ ] **Step 3: Implement safe JSON-string field recovery**

Add a scanner that locates the JSON key token `"message"`, requires a following colon and quoted JSON string value, walks escapes without regex, extracts exactly that string token, and decodes it using Gson/`JsonParser` string semantics.

Do not recover unquoted/non-string message values.

- [ ] **Step 4: Preserve legacy plain-text behavior without JSON leakage**

If content is plain text, return it. If content is plain-text prefix followed by a JSON object/tail, return only the prefix. If content is JSON-looking and no safe message can be recovered, return no answer.

- [ ] **Step 5: Remove unused `cleanupAnswer()` colon fallback**

Delete the method from `OpenAIChatAI` after confirming no callers remain.

- [ ] **Step 6: Run parser tests GREEN**

```bash
./gradlew :common:test --tests '*StructuredAiResponseParserTest' --no-daemon
```

Expected: `BUILD SUCCESSFUL`.

---

### Task 4: Documentation and OpenRouter-compatible voice configuration

**Files:**
- Modify: `README.md`
- Modify: `docs/livingworld/VOICE.md`
- Modify: `docs/livingworld/CONFIGURATION.md`
- Modify: `docs/superpowers/specs/2026-07-22-openrouter-pcm-tts-structured-parser-design.md` only if implementation review reveals a factual mismatch.

**Interfaces:**
- Documents Tasks 1-3; no runtime API changes beyond documented config fields.

- [ ] **Step 1: Document PCM transport behavior**

Document:

```json
{
  "ttsResponseFormat": "auto",
  "ttsPcmSampleRate": 24000
}
```

Explain `auto`: OpenRouter -> PCM, other endpoints -> WAV; explicit override available.

- [ ] **Step 2: Correct the OpenRouter Grok example for persistent voices**

Keep `ttsModel` and voice IDs as configuration examples only. Explain that provider voice IDs are model-specific and existing default OpenAI-oriented pools must be replaced with compatible pools when using a provider/model with different IDs.

For the Grok example, use only currently documented IDs (`eve`, `ara`, `rex`, `sal`, `leo`) in configured pools and clearly mark them as an example, not runtime hardcoding.

- [ ] **Step 3: Document response semantics and fallback**

Document raw `audio/pcm`, optional MIME parameters, 24 kHz configured fallback, resampling to 48 kHz, server-only keys, paid API usage, and that text remains visible when TTS fails.

- [ ] **Step 4: Document structured-response sanitation**

State that malformed commands/social deltas are ignored independently and only a safely recovered NPC message can reach chat/TTS.

---

### Task 5: Full verification, PR review, merge, and release readiness

**Files:**
- No new runtime files unless verification exposes a defect.

- [ ] **Step 1: Run full local-equivalent CI command**

```bash
./gradlew :common:test :fabric:build --no-daemon
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2: Open focused PR**

Title:

```text
feat(livingworld): add OpenRouter PCM TTS and resilient structured response parsing
```

Base: `1.21.1`
Head: `feature/openrouter-pcm-tts`

- [ ] **Step 3: Review final diff**

Check specifically for:

- accidental removal/regression of persistent voice profile flow;
- hardcoded model/voice IDs in runtime code;
- unsafe endpoint substring matching;
- raw provider payload or key logging;
- TTS exceptions preventing text publication;
- parser paths returning JSON tails.

- [ ] **Step 4: Require fresh CI on final head**

Both must be success on the exact final PR head:

- `LivingWorld CI` including package smoke-check;
- `Java Pull Request CI with Gradle` including Fabric + NeoForge.

- [ ] **Step 5: Merge only after green final-head verification**

Use merge commit with expected head SHA.

- [ ] **Step 6: Prepare `0.1.3+1.21.1` release changelog**

Changelog:

```text
- Added OpenRouter raw PCM text-to-speech support with automatic response-format selection.
- Added PCM sample-rate/content-type validation and existing 48 kHz voice-chat resampling.
- Preserved persistent per-NPC voice identities across the new TTS transport.
- Prevented malformed structured metadata from leaking into NPC text or speech.
- Preserved text replies when TTS generation or decoding fails.
```

Create the release tag only after merge and release-workflow verification.
