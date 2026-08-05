# M11 Phase C — Deterministic Mock-Provider Validation

## Goal

Replace repetitive external-provider smoke testing with deterministic CI evidence for the production Chat, STT and TTS HTTP paths.

The first Phase C vertical slice proves:

- OpenAI-compatible Chat requests use the production HTTP transport;
- one empty completion may be retried once;
- all Chat attempts share one connect/read deadline;
- a phase timeout that occurs before the shared deadline is not misreported as deadline exhaustion;
- provider authentication and request bodies are emitted as expected;
- synthetic PCM is encoded as WAV and sent through the production STT client;
- the transcript is sent through the production TTS client;
- deterministic PCM is decoded back into the expected sample stream;
- tests use literal loopback only and never contact a real provider.

This slice does not claim physical microphone capture, Simple Voice Chat playback, client UI rendering or subjective LLM response quality.

## Architecture boundary

Production Chat HTTP ownership is isolated in:

```text
common/src/main/java/net/conczin/mca/livingworld/ai/ChatCompletionHttpClient.java
```

`OpenAIChatAI` remains responsible for Minecraft context, structured-response conversion, logging and gameplay mutation. It delegates provider transport to the Minecraft-independent client.

The package-private monotonic deadline is implemented in:

```text
common/src/main/java/net/conczin/mca/livingworld/ai/AiRequestDeadline.java
```

The deadline:

- uses `System.nanoTime()` rather than wall-clock time;
- shares one budget across retry attempts and connect/read phases;
- never converts a positive sub-millisecond remainder into a zero/infinite URLConnection timeout;
- remains correct across `nanoTime` signed wrap-around;
- fails closed when exhausted;
- does not broaden the public API surface.

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
failure: ChatCompletionHttpClient shared-deadline assertion
```

The first provider request consumed part of the budget and returned an empty completion. The second attempt then received a new full read timeout, so total elapsed time exceeded the single request budget. Production startup/restart and repository security had passed before the common-test failure.

This is the accepted behavioral RED signal.

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

- exactly two network requests;
- controlled `AI provider request deadline exceeded` result;
- elapsed time remains below the former two-full-timeout behavior;
- the test itself has an independent hard timeout;
- timing margins allow substantial CI runner scheduling delay before the second request.

Pure fake-clock tests separately prove rounding, expiration, configured-timeout bounding and `nanoTime` wrap-around without sleeping.

### Phase-timeout classification

A separate provider fixture accepts one request but delays its first response past the configured read timeout while the shared request budget still has time remaining.

Assertions:

- exactly one request and one attempt;
- the configured read timeout is enforced;
- the result remains the controlled generic request failure;
- the error is not falsely classified as shared-deadline exhaustion.

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

## CI integration

The primary `VillAIgence CI` workflow now runs `:common:test` in a named fail-fast step before production-JAR staging and two-JVM startup/restart acceptance.

A source-policy test enforces that:

- the deterministic provider step exists;
- it executes common tests in bounded CI mode;
- it remains ordered before expensive production acceptance;
- the later combined GameTest/Fabric/NeoForge gate remains intact.

The risk catalog records the implemented boundary as `VAI-AI-004`. The broader combined orchestration deadline remains `VAI-AI-003` and stays explicitly planned.

## Security and reliability rules

- Only literal `127.0.0.1` endpoints are permitted by explicit test configuration.
- No real API key, environment credential or external hostname is used.
- Request counts are exact; silent retries are not accepted.
- Response bodies remain subject to production byte limits.
- Redirect following remains disabled.
- The production JAR must not contain test fixtures or test entrypoints.
- A green mock-provider test does not substitute for physical client audio validation.

## Verification record

The canonical RED commits and run IDs are recorded above. The authoritative final exact-head checks, squash merge SHA and post-merge canonical checks are recorded in PR #108 and its GitHub Actions history.

They are intentionally not embedded as self-referential values in this file: committing a newly discovered head SHA or run ID would create another head requiring a new exact-head verification cycle.

A pre-review full-suite baseline was already green before the final timeout-classification and CI-policy hardening:

```text
head:                         acb17c08648f022dfe7b08b86f97c171383038d3
Repository security policy:   30996397419 / #842 — SUCCESS
Java Pull Request CI:         30996397408 / #855 — SUCCESS
VillAIgence CI:               30996397360 / #1454 — SUCCESS
```

Final acceptance still requires all mandatory checks on the exact PR head and again on the canonical squash merge. A release workflow dry-run is not claimed because this slice does not modify the release workflow or release request.

## Remaining Phase C work

The following are intentionally not claimed by this first slice:

- integrated malformed/oversize/redirect/error matrix across all three provider directions;
- one combined STT → Chat → TTS orchestration deadline;
- retry idempotency at the dialogue-memory commit boundary under the mock provider;
- provider-rate-limit cooldown integration;
- physical microphone, Opus transport and audible spatial playback.

They remain separate follow-on scenarios so this slice has a precise, reviewable proof boundary.
