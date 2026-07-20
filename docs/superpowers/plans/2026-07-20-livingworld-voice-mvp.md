# LivingWorld Voice MVP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add intentional voice conversations with MCA villagers using Simple Voice Chat, server-side OpenAI STT/LLM/TTS, and spatial NPC audio.

**Architecture:** Keep AI/context in existing MCA ChatAI. Add loader-independent audio/provider utilities in `common` and isolate Simple Voice Chat API usage in `fabric`. Voice is accepted only for an explicitly selected MCA target and is validated for line-of-sight/view direction before STT.

**Tech Stack:** Java 21, Minecraft 1.21.1, Fabric API, MCA ChatAI, Simple Voice Chat API 2.6.20, OpenAI Audio API, Gson, JUnit 5.

## Global Constraints

- Players configure no API keys or external AI applications.
- `apiKey` or `OPENAI_API_KEY` is the only required LivingWorld AI setting.
- Network calls must not block the Minecraft server thread.
- Normal Simple Voice Chat audio must continue unchanged.
- Fabric voice MVP requires `voicechat_api >= 2.6.20`.
- No ambient transcription in this milestone.

---

### Task 1: Audio and provider core

**Files:**
- Create: `common/src/main/java/net/conczin/mca/livingworld/audio/PcmAudio.java`
- Create: `common/src/main/java/net/conczin/mca/livingworld/audio/WavCodec.java`
- Create: `common/src/main/java/net/conczin/mca/livingworld/voice/SpeechToTextProvider.java`
- Create: `common/src/main/java/net/conczin/mca/livingworld/voice/TextToSpeechProvider.java`
- Create: `common/src/main/java/net/conczin/mca/livingworld/voice/OpenAIAudioProvider.java`
- Test: `common/src/test/java/net/conczin/mca/livingworld/audio/WavCodecTest.java`

- [x] Write tests for WAV round-trip and 24/48 kHz resampling.
- [x] Implement minimal PCM/WAV utilities.
- [x] Implement OpenAI multipart transcription and JSON/WAV speech requests with server-side key and timeouts.
- [x] Independently compile/run the pure PCM/WAV core on Java 21.

### Task 2: Voice configuration and explicit conversation target

**Files:**
- Modify: `common/src/main/java/net/conczin/mca/livingworld/LivingWorldConfig.java`
- Modify: `common/src/main/java/net/conczin/mca/entity/ai/chatAI/ChatAI.java`
- Modify: `common/src/main/java/net/conczin/mca/entity/interaction/EntityCommandHandler.java`
- Test: `common/src/test/java/net/conczin/mca/livingworld/LivingWorldConfigTest.java`

- [x] Add stable defaults for LLM/STT/TTS, segmentation, duration, and spatial distance.
- [x] Add `ChatAI.openConversation`, cheap packet gate, and active-target lookup using existing timeout/distance semantics.
- [x] Mark normal server-side MCA villager interaction as the active target.
- [x] Preserve existing text ChatAI behavior.

### Task 3: Fabric Simple Voice Chat bridge

**Files:**
- Modify: `gradle.properties`
- Modify: `fabric/build.gradle`
- Modify: `fabric/src/main/resources/fabric.mod.json`
- Create: `fabric/src/main/java/net/conczin/mca/fabric/livingworld/voice/LivingWorldVoicechatPlugin.java`
- Create: `fabric/src/main/java/net/conczin/mca/fabric/livingworld/voice/VoiceCaptureManager.java`
- Create: `fabric/src/main/java/net/conczin/mca/fabric/livingworld/voice/VoiceConversationService.java`

- [x] Add Simple Voice Chat API 2.6.20 dependency and Fabric voicechat entrypoint.
- [x] Decode microphone Opus only when LivingWorld is configured and a conversation target exists.
- [x] Buffer per-player PCM and finalize on silence/max duration.
- [x] Validate target visibility/view direction before any STT call.
- [x] Run STT → existing ChatAI → subtitle → TTS asynchronously.
- [x] Resample TTS WAV to 48 kHz and play from a unique entity audio channel.
- [x] Close decoders/encoders, serialize requests per player, and isolate failures.

### Task 4: Documentation and verification

**Files:**
- Create: `docs/livingworld/VOICE.md`
- Modify: `docs/livingworld/CONFIGURATION.md`

- [x] Document required server/client mods and one-key configuration.
- [x] Document intentional activation and fallback behavior.
- [ ] Run `./gradlew :common:test :fabric:build --stacktrace --no-daemon` when a full dependency-resolving runner is available.
- [x] Review Simple Voice Chat calls against official API 2.6.20 documentation.
- [x] Review OpenAI model/endpoint defaults against current official API documentation.
- [ ] Final full-PR verification and merge.
