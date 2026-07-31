# H2 Bounded Network I/O and Voice Memory Budget Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:test-driven-development and superpowers:verification-before-completion for every work package.

**Goal:** Prevent faulty or malicious AI providers and extreme voice configuration from consuming unbounded server heap or retaining arbitrary outbound-network authority.

**Security findings:** SEC-003, SEC-004 and SEC-007 from `docs/security/SECURITY_AUDIT_2026-07-31.md`.

**Baseline:** branch `1.21.1` at H1 merge `787f1a781b5970d4bafb851bfb3c7cba7c21fc0a`.

## Security invariants

1. Chat JSON responses are capped at 1 MiB.
2. STT JSON responses are capped at 512 KiB.
3. TTS audio responses are capped at 32 MiB.
4. Provider error bodies are capped at 64 KiB.
5. Limits are enforced while streaming even when `Content-Length` is absent or incorrect.
6. A declared body larger than its limit is rejected before reading.
7. Exact-limit bodies remain valid.
8. Oversized provider responses fail before parser, persistence or gameplay mutation.
9. TTS failure does not remove the already-produced text response.
10. `OpenAIChatAI.verify(String)` is deleted because no production call site requires arbitrary URL fetching.
11. `voiceMaxSeconds` is always clamped to `1..60`.
12. Active microphone capture across all players is constrained by a server-wide byte budget.
13. Budget reservations are released exactly once on finish, failure, idle flush and shutdown.
14. Existing config and world-data versions remain unchanged.

## Non-goals

- no endpoint-policy redesign beyond H1;
- no provider-specific streaming protocol;
- no replacement of `HttpURLConnection`;
- no change to Memory 2.0 schemas;
- no TTS voice identity redesign;
- no automatic retry of oversized responses;
- no general-purpose URL fetcher or SSRF allowlist because the unused helper will be removed.

## Proposed components

```text
BoundedResponseReader
├── readBytes(InputStream, declaredLength, maxBytes)
└── readUtf8(InputStream, declaredLength, maxBytes)

ProviderResponseLimits
├── CHAT_JSON_BYTES  = 1 MiB
├── STT_JSON_BYTES   = 512 KiB
├── TTS_AUDIO_BYTES  = 32 MiB
└── ERROR_BODY_BYTES = 64 KiB

VoicePcmBudget
├── tryReserve(bytes)
├── release(bytes)
├── usedBytes()
└── maxBytes()
```

## Work package 1 — Bounded reader RED/GREEN

**Create:**

- `common/src/main/java/net/conczin/mca/livingworld/ai/BoundedResponseReader.java`
- `common/src/main/java/net/conczin/mca/livingworld/ai/ProviderResponseLimits.java`
- `common/src/test/java/net/conczin/mca/livingworld/ai/BoundedResponseReaderTest.java`

**RED tests:**

- declared length above limit is rejected before first stream read;
- exact limit accepted;
- unknown/chunked stream crossing limit rejected;
- short fragmented reads reconstruct the payload;
- zero and negative limits rejected;
- UTF-8 decoding works at the byte boundary;
- exception messages contain limit/observed byte counts but no payload.

**GREEN implementation:**

- bounded incremental byte buffer;
- no call to `readAllBytes`, `transferTo` or unbounded `IOUtils.toString`;
- deterministic `ResponseTooLargeException extends IOException`;
- close ownership remains with the caller.

## Work package 2 — Provider integration RED/GREEN

**Modify:**

- `common/src/main/java/net/conczin/mca/entity/ai/chatAI/OpenAIChatAI.java`
- `common/src/main/java/net/conczin/mca/livingworld/voice/OpenAIAudioProvider.java`
- corresponding common tests.

**Chat behavior:**

```text
2xx response       → CHAT_JSON_BYTES
non-2xx response   → ERROR_BODY_BYTES
oversized response → controlled provider error; no retry side effect
```

**Audio behavior:**

```text
STT 2xx             → STT_JSON_BYTES
TTS 2xx             → TTS_AUDIO_BYTES
non-2xx             → ERROR_BODY_BYTES
oversized response  → IOException with safe metadata only
```

**Integration tests:**

- declared oversized `Content-Length`;
- chunked body crossing the limit;
- exact-limit accepted by the shared boundary;
- oversized TTS fails without exposing raw body or key;
- oversized STT fails before JSON parsing;
- no response body is read after an over-limit declared length.

## Work package 3 — Remove arbitrary URL helper

**Modify:**

- delete `OpenAIChatAI.verify(String encodedURL)`;
- remove now-unused URI/import code;
- extend Chat source-policy regression test.

**Proof:**

- full-tree search has no call site requiring the helper;
- source guard asserts the method signature and arbitrary GET construction are absent;
- Chat POST behavior remains covered.

## Work package 4 — Voice configuration clamp RED/GREEN

**Modify:**

- `LivingWorldConfig`;
- config tests;
- operator documentation.

**Add:**

```java
public int voiceMaxActivePcmBytes = 32 * 1024 * 1024;
```

**Normalization:**

```text
voiceMaxSeconds        → clamp 1..60
voiceMaxActivePcmBytes → clamp 1 MiB..256 MiB
```

Missing config fields retain secure defaults and config version remains `2`.

## Work package 5 — Aggregate PCM budget RED/GREEN

**Create:**

- `common/src/main/java/net/conczin/mca/livingworld/voice/VoicePcmBudget.java`
- `common/src/test/java/net/conczin/mca/livingworld/voice/VoicePcmBudgetTest.java`

**Modify:**

- `fabric/.../VoiceCaptureManager.java`.

**Budget semantics:**

- atomic CAS reservation;
- non-positive reservation rejected;
- reservation cannot exceed maximum or overflow;
- release clamps programming errors by throwing rather than silently going negative;
- each capture session tracks its own reserved bytes;
- reservation occurs before PCM bytes are appended;
- budget exhaustion drops and closes that capture session without STT;
- close/finish/error/shutdown release once;
- no microphone content is logged.

## Verification gates

1. targeted RED evidence for each new component;
2. targeted GREEN tests;
3. `:common:test`;
4. `:fabric:build` and release package smoke check;
5. `:neoforge:build` through the Java PR workflow;
6. source review confirming all former unbounded response reads are removed from Chat/Audio;
7. source review confirming `verify(String)` is absent;
8. exact final-head CI evidence recorded in `STEP_1_TRACKER.md` and PR body.

## Live validation boundary

After merge, a real server release candidate must prove:

- ordinary Chat/STT/TTS responses still work;
- oversized test provider responses fail without OOM or persistent side effects;
- TTS failure preserves text output;
- concurrent microphone sessions respect the aggregate budget;
- Chat/Voice operation, persistence and restart remain healthy;
- no provider body, prompt, transcript or credential is logged.
