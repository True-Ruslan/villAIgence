# VillAIgence AI Admission / Backpressure Design

## Goal

Close the next `0.1.x` reliability gap by preventing provider storms and uncontrolled concurrent AI traffic without ever blocking the Minecraft server thread.

The system covers the three external AI stages:

```text
CHAT
STT
TTS
```

and provides:

- bounded concurrent requests per stage;
- per-player/per-stage minimum request interval;
- temporary provider cooldown after HTTP 429 / rate-limit signals;
- fast, controlled rejection instead of unbounded waiting;
- operator visibility through `/villaigence ai status`.

## Core rule: no blocking queue on the server thread

A traditional blocking semaphore/queue is unsafe because some chat entry paths can originate from gameplay/server-thread code.

VillAIgence therefore uses **non-blocking admission control**:

```text
request arrives
→ provider cooldown check
→ concurrency slot check
→ per-player stage cooldown check
→ ALLOW with permit
   or
→ REJECT immediately with controlled reason
```

Rejection reasons:

```text
PLAYER_COOLDOWN
SATURATED
PROVIDER_COOLDOWN
```

This is backpressure: overload is pushed back at the boundary instead of accumulating unbounded work.

## Configuration

Add server-side config fields with safe defaults:

```text
aiChatMaxConcurrentRequests = 4
aiSttMaxConcurrentRequests = 2
aiTtsMaxConcurrentRequests = 2
aiPerPlayerCooldownMillis = 750
aiProviderRateLimitCooldownMillis = 5000
```

Normalization:

- concurrency: clamp to `1..64`;
- player cooldown: clamp to `0..60_000 ms`;
- provider rate-limit cooldown: clamp to `0..300_000 ms`.

Missing fields in existing version-2 config files use defaults; no config-version migration is required.

## Pure common admission controller

Add `net.conczin.mca.livingworld.admission`:

```text
AiAdmissionController
AiAdmissionSettings
AiAdmissionDecision
AiAdmissionResult
AiAdmissionSnapshot
```

### Admission semantics

`tryAcquire(operation, actorId, settings)` is non-blocking.

On `ALLOWED`, it returns an idempotent `Permit` that **must** be closed in `finally` / try-with-resources.

Concurrency slots are per stage.

Per-player cooldown keys are:

```text
(actor UUID, operation)
```

so the normal voice chain:

```text
STT → CHAT → TTS
```

does not self-block across different stages.

### Provider cooldown

`onRateLimited(operation, cooldownMillis)` advances a stage-local cooldown deadline using max/monotonic semantics.

While active, new requests for that stage return `PROVIDER_COOLDOWN` without calling the provider.

Rate-limit signals:

- Chat: HTTP 429 at the actual HTTP response boundary;
- STT/TTS: controlled `http_429` classification at the voice provider boundary.

## Integration

### Chat

`DiagnosticsOpenAIChatAI` acquires a CHAT permit before executing `OpenAIChatAI`.

On rejection:

- no provider call;
- no memory/action/relationship/TTS side effect;
- returns controlled empty result through the existing fallback path;
- diagnostics record local admission rejection, not a fake provider error.

Permit closes in `finally`.

### STT

`VoiceConversationService` acquires an STT permit immediately before transcription.

On rejection:

- no STT provider call;
- player/villager busy state is released;
- diagnostics record the local admission reason.

### TTS

Acquire immediately before synthesis.

On rejection:

- valid NPC text remains published;
- no TTS provider call;
- diagnostics record local admission reason;
- no exception is required for expected backpressure.

## Diagnostics/status

Extend `/villaigence ai status` with admission state per stage:

```text
Admission CHAT: active=1/4 | rejected=3 | providerCooldownMs=0
Admission STT: active=0/2 | rejected=1 | providerCooldownMs=4200
Admission TTS: active=0/2 | rejected=0 | providerCooldownMs=0
```

Counters are process-local and reset on restart.

No player UUIDs or request contents are shown.

## Safety invariants

- never block waiting for capacity;
- never create an unbounded queue;
- permit close is idempotent and cannot make active counts negative;
- rejected requests never call the provider;
- provider cooldown is stage-local;
- per-player cooldown is stage-local;
- no API keys/prompts/transcripts/answers are stored by admission state;
- admission failure is not treated as a provider failure;
- existing compatibility identifiers remain unchanged.

## TDD strategy

1. Pure controller RED/GREEN:
   - first request allowed;
   - saturation rejected immediately;
   - permit release restores capacity;
   - idempotent close;
   - per-player cooldown rejects same stage but not another stage/player;
   - provider cooldown rejects until expiry;
   - rate-limit deadline only extends, never shortens;
   - snapshot active/rejected/cooldown values.
2. Config RED/GREEN:
   - defaults;
   - clamping;
   - missing JSON fields preserve defaults.
3. Diagnostics/report RED/GREEN:
   - admission rejection is visible without provider data;
   - status report includes process-local gate metrics.
4. Integration compile/build gates:
   - chat permit lifecycle;
   - STT/TTS permit lifecycle;
   - HTTP 429 notification path;
   - all existing tests remain green.

## Exit criteria

- configured concurrent caps cannot be exceeded per stage;
- repeated same-player requests are locally throttled per stage;
- provider 429 starts a temporary stage cooldown;
- overload/rate-limit cooldown never blocks the server thread;
- rejected requests do not reach external providers;
- text survives TTS backpressure;
- `/villaigence ai status` exposes current active/rejection/cooldown metrics;
- final Fabric + NeoForge CI is green.
