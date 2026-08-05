# M11 Phase C — Deterministic Mock-Provider Validation

## Goal

Replace repetitive external-provider smoke testing with deterministic CI evidence for the production Chat, STT and TTS HTTP paths.

The first Phase C vertical slice proves:

- OpenAI-compatible Chat requests use the production HTTP transport;
- one empty completion may be retried once;
- all Chat attempts share one connect/read deadline;
- provider authentication and request bodies are emitted as expected;
- synthetic PCM is encoded as WAV and sent through the production STT client;
- the transcript is sent through the production TTS client;
- deterministic PCM is decoded back into the expected sample stream;
- tests use lexical loopback only and never contact a real provider.

This slice does not claim physical microphone capture, Simple Voice Chat playback, client UI rendering or subjective LLM response quality.

## Architecture boundary

Production Chat HTTP ownership is isolated in:

```text
common/src/main/java/net/conczin/mca/livingworld/ai/ChatCompletionHttpClient.java
```

`OpenAIChatAI` remains responsible for Minecraft context, structured-response conversion, logging and gameplay mutation. It delegates the provider request to the isolated transport.

The shared monotonic deadline is implemented in:

```text
common/src/main/java/net/conczin/mca/livingworld/ai/AiRequestDeadline.java
```

The deadline:

- uses `System.nanoTime()` rather than wall-clock time;
- shares one budget across attempts and connect/read phases;
- never converts a positive sub-millisecond remainder into a zero/infinite URLConnection timeout;
- remains correct across `nanoTime` signed wrap-around;
- fails closed when exhausted.

STT and TTS acceptance uses the existing production `OpenAIAudioProvider` with one deterministic loopback HTTP server.

## TDD evidence

### Rejected infrastructure attempt — reflection

The first test attempted to invoke a private transport method reflectively. Repository security correctly rejected it:

```text
commit:  6d25baa4ba25aec5021b4890aab0c1face568101
run:     30993841352 / Repository security policy #794
failure: JAVA_REFLECTION_ACCESS / setAccessible
```

This is not accepted as behavioral RED evidence. Reflection was removed rather than allowlisted.

### Rejected infrastructure attempt — Minecraft-bound class loading

A package-private seam removed reflection, but the common test runtime could not safely load the Minecraft-bound `OpenAIChatAI` class:

```text
commit:  771ca718deb20a7eaf69b7bd54c5d3d58d15c1de
run:     30994312593 / VillAIgence CI #1432
failure: ClassNotFoundException before the target deadline assertion
```

This is also not accepted as behavioral RED evidence. The HTTP transport was isolated into a Minecraft-independent production class.

### Canonical RED

After isolation, the real HTTP test reached the intended assertion:

```text
commit:  48c27d87a763f8dfc4e971ba030ce2df3da6686a
run:     30994782123 / VillAIgence CI #1434
job:     92269137039
failure: ChatCompletionHttpClient integration deadline assertion
```

The first provider request consumed approximately 1.5 seconds and returned an empty completion. The second attempt then received a new full read timeout, so total elapsed time exceeded the single request budget. Production startup/restart and repository security had passed before the common-test failure.

This is the accepted RED signal.

## Deterministic test oracles

### Chat retry success

The loopback provider returns:

1. HTTP 200 with `message.content: null`;
2. HTTP 200 with a fixed Russian response.

Assertions:

- exactly two requests;
- the expected Bearer token on both requests;
- the requested model in the body;
- one final usable completion;
- no external network dependency.

### Shared Chat deadline

The loopback provider:

1. delays the first empty completion;
2. accepts the retry but withholds its response.

Assertions:

- exactly two attempts;
- controlled `AI provider request deadline exceeded` result;
- elapsed time remains below the former two-full-timeout behavior;
- the test itself has an independent hard timeout.

Pure fake-clock tests separately prove rounding, expiration, configured-timeout bounding and `nanoTime` wrap-around without sleeping.

### STT to TTS success path

Input:

```text
16 kHz signed PCM fixture
```

STT assertions:

- exactly one multipart request;
- dedicated STT Bearer token;
- model and language fields;
- `speech.wav` file part containing a RIFF/WAV payload;
- deterministic transcript `Привет, кузнец`.

TTS assertions:

- exactly one JSON request;
- dedicated TTS Bearer token;
- transcript, model, voice and PCM response format in the body;
- deterministic `audio/pcm;rate=24000;channels=1` response;
- exact decoded sample sequence.

## Security and reliability rules

- Only `127.0.0.1` endpoints are permitted by the explicit test configuration.
- No real API key, environment credential or external hostname is used.
- Request counts are exact; silent retries are not accepted.
- Response bodies remain subject to production byte limits.
- Redirect following remains disabled.
- The production JAR must not contain test fixtures or test entrypoints.
- A green mock-provider test does not substitute for physical client audio validation.

## Final evidence

```text
exact implementation head:    PENDING
Repository security policy:   PENDING
Java Pull Request CI:          PENDING
VillAIgence CI:                PENDING
release-workflow dry-run:      PENDING
post-merge canonical CI:       PENDING
```

## Remaining Phase C work

The following are intentionally not claimed by this first slice:

- malformed/oversize/redirect/error matrix across all three provider directions;
- one combined STT → Chat → TTS orchestration deadline;
- retry idempotency at the dialogue-memory commit boundary;
- provider-rate-limit cooldown integration;
- physical microphone, Opus transport and audible spatial playback.

They remain separate follow-on scenarios so this slice has a precise, reviewable proof boundary.
