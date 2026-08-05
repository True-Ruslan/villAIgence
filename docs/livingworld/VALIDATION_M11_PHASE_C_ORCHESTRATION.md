# M11 Phase C — Voice orchestration deadline acceptance

## Purpose

This gate proves that one captured VillAIgence voice turn uses one monotonic total budget across queueing, speech-to-text, Chat retries and optional text-to-speech. It also proves that provider retries cannot own or repeat gameplay persistence side effects.

The authoritative implementation and CI evidence is PR #110. Exact commit and workflow run identifiers belong in the PR evidence because writing them into this file would change the commit being verified.

## Production contract

- `VoiceConversationService` creates exactly one `AiRequestDeadline` before the first server-thread queue handoff.
- The same deadline is passed to production STT, snapshot Chat and TTS calls.
- Chat retries share the remaining turn budget; no retry receives a fresh connect/read budget.
- Streaming response bodies are bounded by both byte limits and the same monotonic deadline.
- A zero Java URL-connection timeout is never emitted because zero means unlimited waiting.
- HTTP redirects remain disabled.
- Every Chat and audio `HttpURLConnection` is disconnected on success and failure.
- `voiceConversationTimeoutSeconds` defaults to 120 seconds and is normalized to the inclusive range 10–300 seconds without changing the config format version.

## Deterministic integration scenarios

The common test suite runs against one loopback OpenAI-compatible provider and performs real production HTTP calls.

### Successful turn with Chat retry

1. Synthetic PCM is sent through the production STT client.
2. STT returns one deterministic transcript.
3. The first Chat completion contains no usable content.
4. The production Chat transport performs its one bounded retry.
5. The second completion contains a structured NPC reply and relationship delta.
6. The successful result is committed once to the real Memory 2.0 and relationship stores.
7. The production TTS client returns deterministic PCM.

Required assertions:

- STT requests: exactly 1;
- Chat requests: exactly 2;
- TTS requests: exactly 1;
- `DIALOGUE` events: exactly 1;
- `RELATIONSHIP_CHANGE` events: exactly 1;
- relationship delta applied exactly once;
- returned PCM samples match exactly.

### TTS blocked after a successful Chat commit

1. STT and the bounded Chat retry complete inside the shared budget.
2. The final Chat result is committed once.
3. TTS accepts the request but does not return a response.
4. TTS fails when the original voice-turn deadline is exhausted rather than receiving a fresh timeout.

Required assertions:

- the total operation remains bounded by the original budget;
- the successful text/relationship commit remains present;
- no second dialogue or relationship event is created;
- busy player/NPC state is released by the production service failure path.

## Structural policy

A source-policy test prevents future refactors from silently weakening the contract:

- exactly one total deadline is created in the production voice turn;
- it is created before queueing;
- STT, Chat and TTS receive that same object;
- `ChatCompletionHttpClient` contains no Memory 2.0 or relationship-store dependency;
- snapshot dialogue and relationship commits remain after the final provider result.

## CI placement

These deterministic common/provider tests run in the fail-fast stage before the expensive production-server acceptance, GameTests and loader builds. A failure therefore blocks the remaining gate without spending several minutes starting two Minecraft JVMs.

## Manual boundary

This gate does not claim to test:

- a physical microphone;
- Simple Voice Chat UDP transport or Opus playback;
- audible spatial TTS on a real client;
- client UI rendering;
- subjective LLM response quality;
- compatibility with a particular installed third-party modpack.

Those remain short installed-client canaries. Provider orchestration, retry budgeting and persistence idempotence are automated CI responsibilities.
