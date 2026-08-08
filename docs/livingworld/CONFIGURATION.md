# VillAIgence configuration

VillAIgence keeps AI, STT and TTS credentials on the dedicated server. Clients never need API keys.

The file remains named `config/livingworld.json` because **LivingWorld is the internal engine namespace**. This compatibility-sensitive path is intentionally not renamed by the VillAIgence rebrand.

## First run

1. Start the server once with VillAIgence installed.
2. Stop the server.
3. Edit `config/livingworld.json`.
4. Configure provider/model/credentials.
5. Start the server again.

Environment variables are preferred:

```bash
export OPENAI_API_KEY="sk-..."
export OPENROUTER_API_KEY="sk-or-v1-..."
```

Never commit real keys or distribute them in a modpack.

## Main AI provider

`provider` accepts:

- `openai`
- `openrouter`

Main chat credential resolution:

- `provider=openrouter`: `OPENROUTER_API_KEY`, then `apiKey`;
- `provider=openai`: `OPENAI_API_KEY`, then `apiKey`.

## AI admission, backpressure and cooldowns

VillAIgence protects Chat, STT and TTS provider boundaries with non-blocking admission control. Requests never wait for capacity on the Minecraft server thread and no unbounded AI queue is created.

Defaults:

```json
{
  "aiChatMaxConcurrentRequests": 4,
  "aiSttMaxConcurrentRequests": 2,
  "aiTtsMaxConcurrentRequests": 2,
  "aiPerPlayerCooldownMillis": 750,
  "aiProviderRateLimitCooldownMillis": 5000
}
```

| Setting | Default | Normalized range | Meaning |
|---|---:|---:|---|
| `aiChatMaxConcurrentRequests` | `4` | `1..64` | maximum concurrent Chat/LLM requests |
| `aiSttMaxConcurrentRequests` | `2` | `1..64` | maximum concurrent STT requests |
| `aiTtsMaxConcurrentRequests` | `2` | `1..64` | maximum concurrent TTS requests |
| `aiPerPlayerCooldownMillis` | `750` | `0..60000` | minimum interval for the same player within the same AI stage |
| `aiProviderRateLimitCooldownMillis` | `5000` | `0..300000` | temporary stage-local cooldown after a detected `429`/rate-limit signal |

Admission checks are non-blocking:

```text
provider cooldown
→ stage concurrency capacity
→ same-player/same-stage cooldown
→ allow immediately or reject immediately
```

Chat, STT and TTS use separate stage state, so the normal voice chain `STT → Chat → TTS` does not block itself simply by moving between stages.

On local rejection, no provider request is made. `/villaigence ai status` exposes safe `admission_*` rejection types and process-local admission metrics.

Tuning guidance:

- keep defaults unless a real multiplayer load test shows a reason to change them;
- lower concurrency for providers with strict quotas or expensive speech endpoints;
- raise concurrency gradually and watch rate limits, latency and server behavior;
- `aiPerPlayerCooldownMillis=0` disables only the per-player interval, not concurrency protection;
- `aiProviderRateLimitCooldownMillis=0` disables local cooldown after a detected rate-limit signal;
- TTS admission happens after the text response is published, so TTS backpressure does not remove valid NPC text.

Existing `version=2` config files do not require migration. Missing admission fields receive the defaults above.

## Memory 2.0

Memory 2.0 is the canonical persistent NPC-memory system. Persistent dialogue is stored and recalled from `<world>/livingworld/memory2.json`; the experimental pre-0.2 `memory.json` store is no longer read, written, recovered or migrated.

Current controls:

```json
{
  "memory2Enabled": true,
  "memory2MaxEventsPerNpc": 256,
  "semanticBeliefExtractionEnabled": false,
  "semanticBeliefMaxCandidatesPerTurn": 3
}
```

| Setting | Default | Normalized range | Meaning |
|---|---:|---:|---|
| `memory2Enabled` | `true` | boolean | enables Memory 2.0 persistence and persistent dialogue recall |
| `memory2MaxEventsPerNpc` | `256` | `1..512` | maximum persisted Memory 2.0 events retained for one NPC |
| `semanticBeliefExtractionEnabled` | `false` | boolean | opt-in extraction of bounded non-authoritative `PLAYER_TOLD` BELIEF candidates from the existing structured chat response |
| `semanticBeliefMaxCandidatesPerTurn` | `3` | `1..8` | maximum candidate statements accepted from one successful player dialogue turn |

Semantic BELIEF extraction is deliberately disabled by default. When enabled, it is active only while `memory2Enabled=true` and reuses the existing Chat provider response; it does **not** create a second provider request.

The model may propose only bounded statement strings. Server-owned code fixes the NPC owner, current player UUID, `PLAYER_TOLD` provenance and exact persisted source DIALOGUE event. This path cannot create or promote `FACT`; `SYSTEM_OBSERVED` remains reserved for the authoritative FACT path. Candidate statements are normalized/deduplicated, capped at 240 Unicode code points, and bounded by the configured per-turn limit with a hard maximum of 8.

Successful text and voice dialogue use one post-success persistence boundary:

```text
usable AI result
→ Memory2DialogueLifecycle
→ structured DIALOGUE MemoryEvent
→ memory2.json
```

With BELIEF extraction enabled, semantic admission happens only after the successful DIALOGUE event has been persisted:

```text
usable structured AI result
→ persist exact PLAYER_TOLD DIALOGUE event
→ bounded beliefCandidates metadata
→ server-owned PLAYER_TOLD admission
→ semantic-memory.json
```

If the visible response is unusable, DIALOGUE persistence fails, candidate metadata is malformed/empty, or semantic admission rejects a candidate, no BELIEF is written. Semantic persistence remains fail-soft and cannot remove an already successful dialogue response/source event.

The next prompt reconstructs recent dialogue from structured Memory 2.0 DIALOGUE events for the exact NPC/player pair. It never parses the human-readable event summary and never reads a second persistent conversation format.

The historical serialized field `persistentMemoryEnabled` is retained for configuration compatibility and currently acts as the outer prompt-recall switch in the inherited chat path. When it is `false`, the inherited process-local transient dialogue fallback is used; no legacy persistent file is created. `memory2Enabled=false` also prevents persistent Memory 2.0 dialogue recall and writes.

The historical `persistentMemoryMaxMessages` and `persistentMemoryMaxCharsPerMessage` fields remain deserializable for version-2 config compatibility but no longer size a separate persistent conversation store. Memory 2.0 uses its own event bound plus hard Working Memory prompt bounds.

Authoritative action ingestion remains intentionally narrow:

```text
successful whitelisted NPC action
→ SYSTEM_OBSERVED events.json record succeeds
→ actor-owned ACTION memory in memory2.json
```

`eventMemoryEnabled` and `memory2Enabled` remain separate controls:

- `eventMemoryEnabled=false` disables creation of the authoritative factual action event, so this ingestion path has no source event to copy;
- `eventMemoryEnabled=true` + `memory2Enabled=false` keeps the existing `events.json` factual event behavior but skips the secondary action-memory entry;
- both enabled gives the normal authoritative event → actor-owned memory flow.

Memory 2.0 failure is fail-soft after factual event persistence. It does not undo the successful NPC action or invalidate the existing `events.json` record.

The source `WorldEvent` UUID is reused as the `MemoryEvent` UUID, so retries/redelivery of the same source event cannot create duplicate action-memory entries for the acting NPC. Dialogue has its own deterministic turn identity and is also idempotent.

Current relationship numeric deltas can be stored as `RELATIONSHIP_CHANGE` events after the server successfully applies the bounded transition. The system still does **not** invent an authoritative causal reason when one was not separately validated.

Config version remains `2`; existing version-2 configs require no schema migration. Missing semantic BELIEF extraction fields receive the safe defaults above. The Memory 2.0 clean cutover is a **world-data** compatibility decision: pre-0.2 experimental conversation history is intentionally not imported. Installed validation should use a clean LivingWorld test state.

See [MEMORY_2.md](MEMORY_2.md) and [SEMANTIC_INGESTION.md](SEMANTIC_INGESTION.md) for the persistence, provenance, ranking, extraction and truth-boundary design.

## Voice switches

| Setting | Default | Behavior |
|---|---:|---|
| `voiceInputEnabled` | `true` | microphone → STT → NPC AI |
| `voiceOutputEnabled` | `false` | synthesize and spatially play NPC speech |

Recommended text-only mode:

```json
{
  "voiceInputEnabled": true,
  "voiceOutputEnabled": false
}
```

Full voice mode:

```json
{
  "voiceInputEnabled": true,
  "voiceOutputEnabled": true
}
```

No TTS request is made while `voiceOutputEnabled=false`.

## OpenRouter STT

```json
{
  "provider": "openrouter",
  "endpoint": "https://openrouter.ai/api/v1/chat/completions",
  "model": "your-chat-model-slug",

  "voiceInputEnabled": true,
  "sttEndpoint": "https://openrouter.ai/api/v1/audio/transcriptions",
  "sttModel": "openai/gpt-4o-mini-transcribe",
  "sttRequestFormat": "json_base64",
  "sttLanguage": "ru"
}
```

`sttRequestFormat` accepts:

- `auto` — JSON/Base64 for OpenRouter, multipart WAV upload otherwise;
- `json_base64` — force JSON/Base64;
- `multipart` — force multipart file upload.

Unknown values normalize to `auto`.

STT credential resolution:

1. `OPENROUTER_API_KEY` for an OpenRouter STT endpoint;
2. `sttApiKey`;
3. resolved main provider key.

OpenRouter STT is billed API usage. HTTP `402 Payment Required` means the account has no usable credits for that request.

## TTS response format

```json
{
  "ttsResponseFormat": "auto",
  "ttsPcmSampleRate": 24000
}
```

`ttsResponseFormat` accepts:

- `auto` — raw PCM for `openrouter.ai`, WAV for other OpenAI-compatible endpoints;
- `pcm` — force headerless PCM16 little-endian decoding;
- `wav` — force WAV decoding.

Unknown values normalize to `auto`.

`ttsPcmSampleRate` is used when a raw PCM response does not provide a valid `rate` parameter in `Content-Type`. Invalid/non-positive values normalize to `24000`.

For PCM responses VillAIgence:

1. requires base `Content-Type` `audio/pcm`;
2. uses MIME `rate` when present, otherwise `ttsPcmSampleRate`;
3. accepts missing `channels` as mono;
4. rejects explicit channel count other than `1`;
5. decodes signed PCM16 little-endian directly;
6. resamples to Simple Voice Chat's 48 kHz playback rate.

No fake WAV header is added. WAV mode preserves the existing WAV decoder path.

## OpenRouter TTS example

Example using a provider/model whose supported voice IDs are `eve`, `ara`, `rex`, `sal`, `leo`:

```json
{
  "provider": "openrouter",
  "voiceInputEnabled": true,
  "voiceOutputEnabled": true,

  "sttEndpoint": "https://openrouter.ai/api/v1/audio/transcriptions",
  "sttModel": "openai/gpt-4o-mini-transcribe",
  "sttRequestFormat": "json_base64",
  "sttLanguage": "ru",

  "ttsEndpoint": "https://openrouter.ai/api/v1/audio/speech",
  "ttsModel": "x-ai/grok-voice-tts-1.0",
  "ttsResponseFormat": "auto",
  "ttsPcmSampleRate": 24000,

  "maleChildVoices": ["eve", "ara", "rex", "sal", "leo"],
  "femaleChildVoices": ["eve", "ara", "rex", "sal", "leo"],
  "neutralChildVoices": ["eve", "ara", "rex", "sal", "leo"],
  "maleTeenVoices": ["eve", "ara", "rex", "sal", "leo"],
  "femaleTeenVoices": ["eve", "ara", "rex", "sal", "leo"],
  "neutralTeenVoices": ["eve", "ara", "rex", "sal", "leo"],
  "maleAdultVoices": ["eve", "ara", "rex", "sal", "leo"],
  "femaleAdultVoices": ["eve", "ara", "rex", "sal", "leo"],
  "neutralAdultVoices": ["eve", "ara", "rex", "sal", "leo"],
  "globalVoiceFallbacks": ["eve", "ara", "rex", "sal", "leo"],
  "ttsVoice": "rex",

  "voiceDistance": 32
}
```

Model and voice IDs are configuration examples only; runtime code does not hardcode them.

Voice IDs are provider/model-specific. The repeated pools above intentionally avoid claiming provider-defined gender or age classifications. Server owners can classify validated voices into narrower VillAIgence pools.

When switching TTS providers, update voice pools as well as `ttsVoice`. A stored NPC profile whose voice is no longer eligible is automatically resolved to a compatible configured replacement.

## Persistent NPC voices

Profiles are stored at:

```text
<world>/livingworld/voices.json
```

The `livingworld` path remains unchanged for backward compatibility.

Each profile is keyed by NPC UUID and stores normalized gender, age bucket and selected voice ID.

MCA 1.21.1 age mapping:

- `BABY`, `TODDLER`, `CHILD` → child;
- `TEEN` → teen;
- `ADULT`, `UNASSIGNED` → adult.

MCA currently has no separate elder state.

Selection fallback order:

1. exact gender + age pool;
2. same-gender adult pool for child/teen NPCs;
3. neutral pool for the same age;
4. global fallback pool;
5. legacy `ttsVoice`.

Built-in groupings are VillAIgence defaults, not provider-supplied gender labels.

## Mood-aware delivery

Mood changes delivery, not persistent voice identity.

Current server-owned signals include panic, health, trust, fear and affinity. They resolve to neutral/happy/sad/angry/afraid/tired delivery styles.

Supported controls are applied best-effort. `speed` is bounded. For OpenAI speech models routed through OpenRouter, OpenAI-specific `instructions` are placed under `provider.options.openai`.

## TTS credentials

Credential selection follows the **TTS endpoint**.

For OpenRouter speech:

1. `OPENROUTER_API_KEY`;
2. `ttsApiKey`;
3. main provider key only when the main provider is `openrouter`.

For OpenAI speech:

1. `OPENAI_API_KEY`;
2. `ttsApiKey`;
3. main provider key only when the main provider is `openai`.

For another custom TTS endpoint:

1. `ttsApiKey`;
2. resolved main provider key.

This prevents accidental cross-provider credential leakage.

## Structured AI response safety

VillAIgence treats the visible NPC message independently from optional command, relationship and semantic BELIEF metadata.

- malformed `optionalCommand` is ignored without discarding a valid message;
- malformed relationship fields are ignored;
- relationship values must be integers before acceptance;
- server-side relationship application performs configured per-turn clamping;
- malformed/missing/non-array `beliefCandidates` metadata produces no candidates without discarding a valid message;
- BELIEF candidate strings are bounded/normalized before admission and cannot supply provenance, source IDs or FACT authority;
- syntactically malformed JSON may recover only a safe top-level JSON string `message`;
- JSON metadata/tails are never used as visible fallback text;
- unrecoverable JSON-looking responses produce no unsafe answer.

The same sanitized message is used for text and TTS.

## Text survives TTS failure

The conversation path is text-first:

```text
AI answer
→ sanitize structured response
→ publish MCA text reply
→ attempt TTS
→ spatial playback
```

A TTS HTTP/format/decode failure or local TTS admission rejection does not remove the text reply, disconnect the player or automatically retry the same utterance.

## AI diagnostics status

Server operators can inspect non-secret configuration readiness, the latest runtime outcome and admission/backpressure state of Chat, STT and TTS with:

```text
/villaigence ai status
```

The command is read-only and performs no provider request. It therefore does not consume tokens/credits, retry failed requests or mutate NPC/world state.

Configuration readiness is reported as:

- `CONFIGURED` — enabled and required configuration/credential resolution is available;
- `MISCONFIGURED` — enabled but required configuration is missing or unusable;
- `DISABLED` — intentionally disabled by configuration.

Runtime status is process-local. `last: NEVER` means no completed operation for that stage has been observed since the current server process started.

Admission lines report current `active/max`, total local `rejected` count and remaining `providerCooldownMs` for each stage.

Credential values, Authorization headers, prompts, transcripts, NPC answers, TTS input, reasoning content, raw provider payloads and player UUIDs are not included. Credential presence is represented only as a boolean and endpoints are reduced to host names.

See [DIAGNOSTICS.md](DIAGNOSTICS.md) for field meanings, privacy guarantees and troubleshooting examples.

## Existing config migration

Config version remains `2`. Existing version-2 files do not require config-schema migration: missing newer fields receive safe defaults.

Legacy version-1 `voiceEnabled` migration remains unchanged.

The VillAIgence rebrand does **not** rename `config/livingworld.json` or serialized field names.

## Persistent server data

```text
<world>/livingworld/
├── memory2.json
├── semantic-memory.json
├── events.json
├── relationships.json
├── voices.json
└── operator-lore.json
```

`memory2.json`, `semantic-memory.json`, `relationships.json`, `voices.json`, and `operator-lore.json` form the current five-store production corruption/recovery matrix. `events.json` is authoritative factual event history and has its own validation path; it is not a replacement for Memory 2.0 dialogue.

The removed pre-0.2 `memory.json` conversation file is not imported. For this clean-cutover development package, start installed acceptance from a clean LivingWorld test state rather than copying legacy conversation data forward.

Back the active files up with the Minecraft world.

Raw microphone and synthesized audio are processed in memory and are not intentionally persisted.

## Voice requirements

Simple Voice Chat 2.6.20+ is required on server and clients when microphone input is used. The same VillAIgence release JAR must be installed on server and clients.

See [VOICE.md](VOICE.md) for interaction flow and troubleshooting.