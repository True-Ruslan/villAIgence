# Persistent NPC Voice Profiles Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Give each NPC a persistent, gender/age-aware voice identity and apply dynamic mood styling without coupling voice identity to the chat/LLM model.

**Architecture:** Add provider-neutral voice profile, catalog, mood, style and TTS request types in `common`; persist profiles by NPC UUID under the world directory; capture mutable MCA state on the server thread; translate the immutable request in the OpenAI-compatible adapter. Voice selection and persistence never inspect the LLM model.

**Tech Stack:** Java 21, Gson, Fabric 1.21.1, MCA age/gender state, Simple Voice Chat 2.6.20+, JUnit 5.

## Global Constraints

- Voice identity is independent from the chat/LLM model.
- One stable voice is persisted per NPC UUID.
- `BABY/TODDLER/CHILD` use child profiles, `TEEN` teen profiles, `ADULT/UNASSIGNED` adult profiles.
- Mood changes delivery style, never base voice identity.
- Mutable Minecraft entity state is captured only on the server thread.
- `ttsVoice` remains the final backward-compatible fallback.
- TTS credentials are resolved independently from chat credentials.
- Full LivingWorld CI and official Fabric/NeoForge CI must pass before merge.

---

### Task 1: Provider-neutral voice identity model and deterministic catalog — complete

**Files:**
- `common/src/main/java/net/conczin/mca/livingworld/voice/NpcVoiceGender.java`
- `common/src/main/java/net/conczin/mca/livingworld/voice/NpcVoiceAgeGroup.java`
- `common/src/main/java/net/conczin/mca/livingworld/voice/NpcVoiceProfile.java`
- `common/src/main/java/net/conczin/mca/livingworld/voice/NpcVoiceCatalog.java`
- `common/src/test/java/net/conczin/mca/livingworld/voice/NpcVoiceCatalogTest.java`

Implemented deterministic UUID-based selection, exact/fallback pools, neutral-young fallback, and legacy `ttsVoice` fallback.

### Task 2: Persistent voice store and age-transition semantics — complete

**Files:**
- `common/src/main/java/net/conczin/mca/livingworld/voice/PersistentNpcVoiceStore.java`
- `common/src/test/java/net/conczin/mca/livingworld/voice/PersistentNpcVoiceStoreTest.java`

Implemented synchronized world-local persistence, reload stability, age/gender transition reassignment, temp-file replacement, atomic move where supported, and corrupt-file fail-open recovery.

### Task 3: Configurable voice pools with backward-compatible defaults — complete

**Files:**
- `common/src/main/java/net/conczin/mca/livingworld/LivingWorldConfig.java`
- `common/src/test/java/net/conczin/mca/livingworld/LivingWorldConfigTest.java`

Implemented configurable normalized child/teen/adult pools by gender, global fallbacks, and `ttsVoice` compatibility. Built-in classifications are explicitly documented as LivingWorld defaults rather than provider metadata.

### Task 4: Provider-neutral mood/style request contract — complete

**Files:**
- `common/src/main/java/net/conczin/mca/livingworld/voice/NpcVoiceMood.java`
- `common/src/main/java/net/conczin/mca/livingworld/voice/TtsVoiceStyle.java`
- `common/src/main/java/net/conczin/mca/livingworld/voice/TtsRequest.java`
- `common/src/main/java/net/conczin/mca/livingworld/voice/NpcVoiceMoodResolver.java`
- `common/src/main/java/net/conczin/mca/livingworld/voice/TextToSpeechProvider.java`
- `common/src/test/java/net/conczin/mca/livingworld/voice/NpcVoiceMoodResolverTest.java`

Implemented authoritative mood resolution for afraid/sad/angry/happy/tired/neutral and age-aware style composition. The existing abstract `synthesize(String)` contract is preserved for compatibility; rich `synthesize(TtsRequest)` is a default extension point.

### Task 5: OpenAI-compatible adapter capabilities and credential isolation — complete

**Files:**
- `common/src/main/java/net/conczin/mca/livingworld/voice/OpenAIAudioProvider.java`
- `common/src/main/java/net/conczin/mca/livingworld/LivingWorldConfig.java`
- related tests

Implemented per-NPC voice, bounded speed, model-capability-based instructions, legacy-model omission, dedicated `ttsApiKey`, provider-aware fallback, and hostname-safe OpenAI endpoint detection. OpenRouter chat credentials are not reused for the OpenAI speech endpoint.

### Task 6: Server-thread capture and runtime integration — complete

**Files:**
- `common/src/main/java/net/conczin/mca/livingworld/voice/NpcVoiceSnapshot.java`
- `fabric/src/main/java/net/conczin/mca/fabric/livingworld/voice/VoiceConversationService.java`

Implemented server-thread capture of UUID, MCA gender/age, panic and health; stable profile resolution; relationship-informed mood; provider-neutral TTS requests; text publication before TTS; existing busy gates, target validation and spatial playback preserved.

### Task 7: Documentation and final verification — implementation complete, CI gate pending

**Files:**
- `README.md`
- `docs/livingworld/CONFIGURATION.md`
- `docs/livingworld/VOICE.md`

Documented `voices.json`, stable UUID assignment, age/gender pools, mood best-effort behavior, model/provider independence, TTS credential isolation and fallback semantics.

Final merge gate remains:

- `LivingWorld CI` green on the final PR head;
- official Fabric + NeoForge Gradle CI green on the same final PR head;
- focused PR diff review with no unresolved critical/important issues.
