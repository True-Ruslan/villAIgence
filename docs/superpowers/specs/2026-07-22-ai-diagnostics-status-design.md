# VillAIgence AI Diagnostics Status Design

## Goal

Close the next `0.1.x` reliability gap by giving server operators one safe, read-only status surface for the current AI pipeline:

```text
/villaigence ai status
```

The command must answer two different questions without performing provider calls:

1. Is the current server configuration capable of using chat/STT/TTS?
2. What happened on the most recent runtime chat/STT/TTS attempt?

This is diagnostics, not a synthetic health check. Running the command must never spend tokens, consume speech credits, mutate NPC state, or create provider side effects.

## Scope

### Included

- new public root command `/villaigence`;
- operator-only `/villaigence ai status` subcommand;
- immutable safe configuration snapshot;
- process-local last-result diagnostics for CHAT, STT and TTS;
- bounded/sanitized metadata only;
- chat diagnostics for success, provider error, empty response and transport failure;
- STT/TTS diagnostics for success and failure;
- concise text report usable from player chat or server console;
- unit tests for state transitions, secret redaction/sanitization and report formatting;
- documentation and `PROJECT_STATE.md` update.

### Not included

- active provider probing;
- queue/backpressure metrics (next reliability slice);
- persistence of diagnostics across restart;
- API keys, Authorization headers, prompts, transcripts, generated answers, reasoning text or raw provider payloads;
- Memory 2.0 changes;
- renaming compatibility-sensitive `mca` / `livingworld` identifiers.

## Architecture

### 1. Pure diagnostics core

Add `net.conczin.mca.livingworld.diagnostics` with small pure-Java units:

```text
AiDiagnostics
├── AtomicReference<AiOperationStatus> CHAT
├── AtomicReference<AiOperationStatus> STT
└── AtomicReference<AiOperationStatus> TTS

AiDiagnosticsSnapshot
├── chat
├── stt
└── tts

AiOperationStatus
├── state: NEVER | SUCCESS | FAILURE
├── completedAtEpochMillis
├── durationMillis
├── provider
├── model
├── finishReason
├── errorType
├── generationId
└── detail
```

`AiDiagnostics` owns only process-local operational observations. Writes are lock-free atomic replacements so async provider work can record results safely.

No field may contain credentials, prompt content, transcript text, NPC response text or reasoning text.

### 2. Safe configuration snapshot

Add `AiDiagnosticsConfigSnapshot` built from `LivingWorldConfig` using only non-secret configuration:

- `enabled`;
- normalized provider name;
- chat model and endpoint host;
- whether a usable chat key resolves (`keyConfigured: boolean`, never the key);
- voice input/output enabled flags;
- STT/TTS models and endpoint hosts;
- whether STT/TTS credentials resolve (`boolean` only);
- resolved STT request format and TTS response format where safe.

Invalid endpoint URIs produce a bounded `<invalid>` host marker instead of throwing.

### 3. Runtime instrumentation

#### Chat

Instrument the existing `OpenAIChatAI.post(...)` provider boundary.

Record exactly one final observation per logical chat request:

- `SUCCESS` after a usable final assistant answer;
- `FAILURE` on explicit provider/transport error;
- `FAILURE` with `detail=empty_response` after bounded retry exhaustion.

Intermediate retry attempts are logged as today but do not overwrite the final operation status until the logical request finishes.

Safe metadata may include `finish_reason`, provider error type, generation ID and final attempt count. Reasoning presence may be reduced to a boolean/detail flag, never content.

#### STT/TTS

Instrument `OpenAIAudioProvider.transcribe(...)` and `synthesize(...)` at the public operation boundary.

Record elapsed duration and final success/failure. Failure detail is bounded and sanitized from the thrown exception message. It must not include request bodies, audio, transcript or synthesized text.

### 4. Report formatter

Add pure `AiStatusReport` that converts:

```text
AiDiagnosticsConfigSnapshot + AiDiagnosticsSnapshot
```

into a deterministic list of lines.

Example shape:

```text
VillAIgence AI Status
Chat: CONFIGURED | provider=openrouter | model=... | endpoint=openrouter.ai
  last: SUCCESS | 824 ms | finish=stop | generation=gen-...
STT: CONFIGURED | enabled=true | model=... | format=json_base64
  last: FAILURE | 391 ms | type=http_402 | detail=Payment Required
TTS: DISABLED | enabled=false | model=... | format=pcm
  last: NEVER
```

The formatter must never receive raw keys, so secret leakage is structurally prevented rather than relying only on string replacement.

### 5. Command integration

Add `VillAIgenceCommand` under the existing common command package and register it from Fabric alongside the existing `AdminCommand` and MCA `Command`.

Command tree:

```text
/villaigence
└── ai
    └── status
```

Permission policy:

```text
permission level >= 2 OR integrated single-player server
```

The command is read-only and works for both players and server console using `CommandSourceStack.sendSuccess(...)`.

The existing `/mca` command tree remains unchanged.

## Security and privacy constraints

The status surface must never expose:

- `apiKey`, `sttApiKey`, `ttsApiKey`;
- resolved environment credential values;
- Authorization headers;
- prompt/message bodies;
- STT transcript text;
- TTS input text;
- reasoning content;
- full provider payloads.

Free-form diagnostic strings are normalized to one line and bounded to 160 characters. Configuration reports only endpoint host, not query parameters or embedded credentials.

## Failure behavior

Diagnostics must fail soft:

- recording diagnostics can never change provider request success/failure;
- report generation must tolerate `NEVER` values and malformed endpoint strings;
- command execution performs no network I/O;
- if diagnostics data is absent, the report says `NEVER`, not `OK`;
- configuration state distinguishes `DISABLED`, `MISCONFIGURED` and `CONFIGURED` where possible.

## Compatibility

Do not rename:

```text
mod id: mca
Java package root: net.conczin.mca
config/livingworld.json
<world>/livingworld/
```

Public command branding is `/villaigence` only.

Fabric is the primary runtime integration target. Common diagnostics code must remain NeoForge-compilable through the existing shared build.

## Testing strategy

TDD sequence:

1. `AiDiagnosticsTest`
   - initial status is `NEVER`;
   - success replaces prior state;
   - failure replaces prior state;
   - free-form values are single-line and bounded;
   - no operation can store credential-like raw input through the public API beyond bounded safe metadata fields.
2. `AiDiagnosticsConfigSnapshotTest`
   - only booleans indicate credential availability;
   - endpoint output is host-only;
   - invalid endpoints fail soft;
   - voice disabled/misconfigured/configured states are derived correctly.
3. `AiStatusReportTest`
   - deterministic configured/disabled/never/success/failure output;
   - output contains no supplied secret sentinel.
4. Existing chat parser/retry/audio tests remain green.
5. Full CI gates:
   - `:common:test`;
   - Fabric build + distributable JAR smoke-check;
   - official NeoForge + Fabric Gradle PR CI.

## Exit criteria for this slice

- operator can run `/villaigence ai status` without triggering any provider request;
- current chat/STT/TTS configuration is understandable at a glance;
- latest runtime outcome for each stage is visible after real use;
- no credentials or user/NPC content can appear in status output;
- command and instrumentation do not alter gameplay/provider behavior;
- all existing project CI gates are green.
