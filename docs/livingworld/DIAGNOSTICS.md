# VillAIgence AI diagnostics

VillAIgence provides a read-only server-operator status command for the AI pipeline:

```text
/villaigence ai status
```

The command is available to permission level 2+ operators and to the integrated single-player server owner.

## What the command does

It combines two kinds of information:

1. **configuration readiness** for Chat, STT and TTS;
2. the **latest runtime result** observed for each stage during the current server process.

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

The latest logical operation failed.

Chat may report safe structured metadata such as provider error type, `finish_reason`, generation ID, bounded attempt count, or controlled result codes such as `empty_response`.

STT/TTS classify failures into controlled types where possible, including:

```text
http_402
http_429
timeout
io_error
runtime_error
```

Raw provider error bodies are not copied into the status surface.

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
- raw provider request/response payloads.

Credential status is represented only as a boolean such as `credential=true`.

Endpoints are reduced to their host name for display. Query parameters, embedded credentials and paths are not shown.

## Retry behavior

Chat diagnostics represent the **final logical request**, not every intermediate retry as a separate user-visible status.

For the bounded empty-completion retry path, the final recorded metadata comes from the latest provider attempt and the detail includes the total attempt count. The retry still occurs entirely before memory, relationship changes, actions and TTS, preserving the existing no-duplicate-side-effects guarantee.

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
```

Typical interpretations:

- `MISCONFIGURED` + `credential=false` — fix server-side credentials;
- `FAILURE type=http_402` — provider account/balance problem;
- `FAILURE type=http_429` — rate limiting; the later roadmap backpressure/cooldown layer is the next reliability milestone;
- `FAILURE detail=empty_response` — provider returned no usable chat content after the bounded retry policy;
- `CONFIGURED` + `last: NEVER` — no completed request for that stage has been observed since restart;
- TTS `DISABLED` — expected when `voiceOutputEnabled=false`.

The dedicated server log remains the source for deeper bounded operational diagnostics when needed.
