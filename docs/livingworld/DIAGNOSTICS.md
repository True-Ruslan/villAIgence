# VillAIgence AI diagnostics

VillAIgence provides a read-only server-operator status command for the AI pipeline:

```text
/villaigence ai status
```

The command is available to permission level 2+ operators and to the integrated single-player server owner.

## What the command does

It combines three kinds of information:

1. **configuration readiness** for Chat, STT and TTS;
2. the **latest runtime result** observed for each stage during the current server process;
3. **admission/backpressure state** for each external AI stage.

Running the command does **not** call OpenAI, OpenRouter or another provider. It does not spend tokens, consume speech credits, retry a failed request, mutate NPC state or write persistent data.

Example shape:

```text
VillAIgence AI Status
Chat: CONFIGURED | enabled=true | credential=true | provider=openrouter | model=... | endpoint=openrouter.ai
  last: SUCCESS | 824 ms | provider=openrouter | model=... | finish=stop | generation=gen-... | detail=success; attempts=1; reasoningPresent=false
STT: CONFIGURED | enabled=true | credential=true | provider=openrouter | model=... | endpoint=openrouter.ai | format=json_base64
  last: FAILURE | 391 ms | provider=openrouter | model=... | type=http_402 | detail=format=json_base64
TTS: DISABLED | enabled=false | credential=true | provider=openrouter | model=... | endpoint=openrouter.ai | format=pcm
  last: NEVER
Admission CHAT: active=1/4 | rejected=3 | providerCooldownMs=0
Admission STT: active=0/2 | rejected=1 | providerCooldownMs=4200
Admission TTS: active=0/2 | rejected=0 | providerCooldownMs=0
```

Exact values depend on the configured providers and what has happened since the server process started.

## Configuration states

### `CONFIGURED`

The stage is enabled and VillAIgence can resolve the required non-secret configuration and credentials.

This is a **configuration readiness** signal, not a paid network health probe. A configured provider may still fail because of balance, rate limits, model availability or network problems.

### `MISCONFIGURED`

The stage is enabled but required configuration is missing or unusable, for example no resolvable credential.

### `DISABLED`

The stage is intentionally disabled by configuration.

For example, `voiceOutputEnabled=false` makes TTS `DISABLED` even when a TTS endpoint/model/key is present.

## Runtime states

### `NEVER`

No completed operation for that stage has been recorded in the current server process.

Diagnostics are intentionally process-local and reset on server restart. They are not saved into the world.

### `SUCCESS`

The latest logical operation completed successfully.

For Chat, safe metadata can include:

- provider and model;
- duration;
- `finish_reason`;
- generation ID;
- final logical attempt count;
- whether reasoning metadata was present as a boolean only.

For STT/TTS, safe metadata can include:

- provider and model;
- resolved request/response format;
- duration.

### `FAILURE`

The latest logical operation failed or was locally rejected before a provider call.

Chat may report safe structured metadata such as provider error type, `finish_reason`, generation ID, bounded attempt count, or controlled result codes such as `empty_response`.

STT/TTS classify provider failures into controlled types where possible, including:

```text
http_402
http_429
timeout
io_error
runtime_error
```

Local admission rejection types are separate from provider failures:

```text
admission_saturated
admission_player_cooldown
admission_provider_cooldown
```

A local rejection has `detail=local_rejection` and intentionally carries no provider/model result metadata. It means VillAIgence rejected the request before making that external call.

Raw provider error bodies are not copied into the status surface.

## Admission/backpressure metrics

Each stage has a non-blocking gate. VillAIgence never waits for AI capacity on the Minecraft server thread and never creates an unbounded provider queue.

Status fields:

- `active=X/Y` — current in-flight external requests versus configured maximum;
- `rejected=N` — total local rejections for that stage since process start;
- `providerCooldownMs=N` — remaining stage-local cooldown after a rate-limit signal.

Admission checks happen in this order:

```text
provider cooldown
→ concurrency capacity
→ per-player/per-stage cooldown
→ allow with permit or reject immediately
```

Chat, STT and TTS use separate stage capacity/cooldown state. The normal `STT → Chat → TTS` voice chain therefore does not self-block merely because it advances between stages.

HTTP/provider `429` rate-limit signals activate a temporary stage cooldown. While it is active, new requests for that stage are rejected locally with `admission_provider_cooldown` instead of repeatedly hitting the provider.

For TTS, admission happens after the valid NPC text reply has already been published. TTS backpressure therefore never removes a valid text answer.

## Privacy and secret-safety

`/villaigence ai status` never intentionally displays or stores in its diagnostics snapshot:

- `apiKey`, `sttApiKey` or `ttsApiKey` values;
- environment credential values;
- `Authorization` headers;
- prompts or message bodies;
- player speech transcripts;
- NPC-visible answers;
- TTS input text;
- reasoning content;
- raw provider request/response payloads;
- player UUIDs in admission metrics.

Credential status is represented only as a boolean such as `credential=true`.

Endpoints are reduced to their host name for display. Query parameters, embedded credentials and paths are not shown.

## Retry behavior

Chat diagnostics represent the **final logical request**, not every intermediate retry as a separate user-visible status.

For the bounded empty-completion retry path, the final recorded metadata comes from the latest provider attempt and the detail includes the total attempt count. The retry still occurs entirely before memory, relationship changes, actions and TTS, preserving the existing no-duplicate-side-effects guarantee.

Admission rejection itself does not introduce retries.

## Troubleshooting flow

When an NPC cannot answer, run:

```text
/villaigence ai status
```

Then read the stages in order:

```text
Chat configuration/runtime
STT configuration/runtime   (for microphone input)
TTS configuration/runtime   (when voice output is enabled)
Admission CHAT/STT/TTS
```

Typical interpretations:

- `MISCONFIGURED` + `credential=false` — fix server-side credentials;
- `FAILURE type=http_402` — provider account/balance problem;
- `FAILURE type=http_429` — the provider rate-limited the request; VillAIgence activates temporary local cooldown for that stage;
- `FAILURE type=admission_saturated` — the configured concurrent request limit is currently full;
- `FAILURE type=admission_player_cooldown` — the same player repeated that stage faster than the configured interval;
- `FAILURE type=admission_provider_cooldown` — VillAIgence is still locally cooling down that stage after rate limiting;
- `FAILURE detail=empty_response` — provider returned no usable chat content after the bounded retry policy;
- `CONFIGURED` + `last: NEVER` — no completed request for that stage has been observed since restart;
- TTS `DISABLED` — expected when `voiceOutputEnabled=false`.

The dedicated server log remains the source for deeper bounded operational diagnostics when needed.
