# LivingWorld Voice MVP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add intentional voice conversations with MCA villagers using Simple Voice Chat, server-side OpenAI STT/LLM/TTS, and spatial NPC audio.

**Architecture:** Keep AI/context in existing MCA ChatAI. Add loader-independent audio/provider utilities in `common` and isolate Simple Voice Chat API usage in `fabric`. Voice capture is active only while a player has an explicitly selected MCA conversation target.

**Tech Stack:** Java 21, Minecraft 1.21.1, Fabric API, MCA ChatAI, Simple Voice Chat API 2.6.20, OpenAI Audio API, Gson, JUnit 5.

## Global Constraints

- Players configure no API keys or external AI applications.
- `apiKey` or `OPENAI_API_KEY` is the only required LivingWorld AI setting.
- Network calls must not block the Minecraft server thread.
- Normal Simple Voice Chat audio must continue unchanged.
- Text-only MCA/LivingWorld must keep working when Simple Voice Chat is absent.
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

- [ ] Write tests for WAV round-trip and 24/48 kHz resampling.
- [ ] Implement minimal PCM/WAV utilities.
- [ ] Implement OpenAI multipart transcription and JSON/WAV speech requests with server-side key and timeouts.
- [ ] Verify pure unit tests.

### Task 2: Voice configuration and explicit conversation target

**Files:**
- Modify: `common/src/main/java/net/conczin/mca/livingworld/LivingWorldConfig.java`
- Modify: `common/src/main/java/net/conczin/mca/entity/ai/chatAI/ChatAI.java`
- Modify: `common/src/main/java/net/conczin/mca/entity/VillagerEntityMCA.java`
- Test: existing LivingWorld config tests.

- [ ] Add voice defaults for STT/TTS endpoints/models/voice, segmentation, duration, and distance.
- [ ] Add `ChatAI.openConversation` and active-target lookup using existing timeout/distance semantics.
- [ ] Mark a normal server-side villager interaction as the active target.
- [ ] Preserve existing text ChatAI behavior.

### Task 3: Fabric Simple Voice Chat bridge

**Files:**
- Modify: `gradle.properties`
- Modify: `fabric/build.gradle`
- Modify: `fabric/src/main/resources/fabric.mod.json`
- Create: `fabric/src/main/java/net/conczin/mca/fabric/livingworld/voice/LivingWorldVoicechatPlugin.java`
- Create: `fabric/src/main/java/net/conczin/mca/fabric/livingworld/voice/VoiceCaptureManager.java`
- Create: `fabric/src/main/java/net/conczin/mca/fabric/livingworld/voice/VoiceConversationService.java`

- [ ] Add Simple Voice Chat API compile dependency and optional Fabric entrypoint.
- [ ] Decode microphone Opus only when LivingWorld is configured and an active MCA target exists.
- [ ] Buffer per-player PCM and finalize on silence/max duration.
- [ ] Run STT → existing ChatAI → subtitle → TTS asynchronously.
- [ ] Resample TTS WAV to 48 kHz and play from a unique entity audio channel.
- [ ] Close decoders/encoders and isolate failures.

### Task 4: Documentation and verification

**Files:**
- Create: `docs/livingworld/VOICE.md`
- Modify: `docs/livingworld/CONFIGURATION.md`

- [ ] Document required server/client mods and one-key configuration.
- [ ] Document intentional activation and fallback behavior.
- [ ] Run `./gradlew :common:test :fabric:build --stacktrace --no-daemon` through available CI.
- [ ] Review full PR diff for secrets, thread-safety, dependency leakage, and MCA fallback.
