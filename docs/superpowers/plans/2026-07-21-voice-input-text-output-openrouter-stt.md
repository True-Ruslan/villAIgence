# Voice Input / Text Output with OpenRouter STT Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add independently configurable microphone input and NPC speech output, with OpenRouter JSON/Base64 STT support and text-only answers as the new-install default.

**Architecture:** Migrate LivingWorld config to v2, preserving v1 voice behavior while exposing separate input/output switches. Resolve STT transport independently from chat/TTS, keep existing multipart support, and short-circuit synthesis when text-only output is selected.

**Tech Stack:** Java 21, Gson, Fabric 1.21.1, Simple Voice Chat API 2.6.20, JUnit 5, OpenRouter `/api/v1/audio/transcriptions`.

## Global Constraints

- New defaults: `voiceInputEnabled=true`, `voiceOutputEnabled=false`.
- Existing v1 `voiceEnabled` values migrate to both new switches.
- OpenAI multipart STT remains supported.
- OpenRouter JSON contains raw Base64 WAV bytes, not a data URI.
- No TTS request occurs when `voiceOutputEnabled=false`.
- Full LivingWorld CI plus official Fabric/NeoForge CI before merge.

---

### Task 1: Config v2, provider keys and migration

**Files:**
- Modify: `common/src/main/java/net/conczin/mca/livingworld/LivingWorldConfig.java`
- Modify: `common/src/test/java/net/conczin/mca/livingworld/LivingWorldConfigTest.java`

**Produces:** `isVoiceInputConfigured()`, `isVoiceOutputConfigured()`, `resolvedSttApiKey()`, config v1→v2 migration.

- [ ] Add failing/default tests for independent switches, `sttRequestFormat=auto`, and provider-specific key resolution.
- [ ] Add v1 JSON migration tests for `voiceEnabled=true` and `voiceEnabled=false`.
- [ ] Bump config version to 2 and parse the raw JSON object before deserializing so legacy field presence is detectable.
- [ ] Accept `provider=openai` and `provider=openrouter`.
- [ ] Add `OPENROUTER_API_KEY`, optional `sttApiKey`, and safe fallback to the main key.
- [ ] Normalize invalid STT request format to `auto`.

### Task 2: STT transport abstraction and OpenRouter payload

**Files:**
- Create: `common/src/main/java/net/conczin/mca/livingworld/voice/SttRequestFormat.java`
- Create: `common/src/test/java/net/conczin/mca/livingworld/voice/SttRequestFormatTest.java`
- Modify: `common/src/main/java/net/conczin/mca/livingworld/voice/OpenAIAudioProvider.java`

**Produces:** automatic endpoint-based transport resolution and JSON/Base64 request encoding.

- [ ] Test explicit `multipart`, explicit `json_base64`, invalid→auto, OpenRouter auto detection and non-OpenRouter multipart fallback.
- [ ] Encode WAV once, then branch to multipart or JSON/Base64.
- [ ] Use `input_audio.data` with raw `Base64.getEncoder()` output and `input_audio.format=wav`.
- [ ] Use the dedicated resolved STT credential for transcription.
- [ ] Improve HTTP 402 diagnostics for OpenRouter credits.
- [ ] Keep TTS request/response behavior unchanged.

### Task 3: Runtime input/output split

**Files:**
- Modify: `fabric/src/main/java/net/conczin/mca/fabric/livingworld/voice/VoiceCaptureManager.java`
- Modify: `fabric/src/main/java/net/conczin/mca/fabric/livingworld/voice/VoiceConversationService.java`

- [ ] Capture microphone packets only when `isVoiceInputConfigured()`.
- [ ] Always publish a non-empty NPC answer into the conversation/chat UI.
- [ ] Return immediately after text publication when `voiceOutputEnabled=false`.
- [ ] Synthesize and spatially play only when `isVoiceOutputConfigured()`.
- [ ] Preserve busy gates, target validation and server-thread boundaries.

### Task 4: Documentation and verification

**Files:**
- Modify: `README.md`
- Modify: `docs/livingworld/CONFIGURATION.md`
- Modify: `docs/livingworld/VOICE.md`

- [ ] Add a copy-paste OpenRouter text-only configuration example.
- [ ] Document `OPENROUTER_API_KEY`, model slug `openai/gpt-4o-mini-transcribe`, `json_base64`, and HTTP 402 balance requirement.
- [ ] Explain that Simple Voice Chat remains required on clients for microphone capture even when NPC TTS is disabled.
- [ ] Open a focused PR and require all CI workflows to pass before merge.
