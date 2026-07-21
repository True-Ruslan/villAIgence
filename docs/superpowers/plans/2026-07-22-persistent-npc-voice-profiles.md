# Persistent NPC Voice Profiles Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

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
- Full LivingWorld CI and official Fabric/NeoForge CI must pass before merge.

---

### Task 1: Provider-neutral voice identity model and deterministic catalog

**Files:**
- Create: `common/src/main/java/net/conczin/mca/livingworld/voice/NpcVoiceGender.java`
- Create: `common/src/main/java/net/conczin/mca/livingworld/voice/NpcVoiceAgeGroup.java`
- Create: `common/src/main/java/net/conczin/mca/livingworld/voice/NpcVoiceProfile.java`
- Create: `common/src/main/java/net/conczin/mca/livingworld/voice/NpcVoiceCatalog.java`
- Test: `common/src/test/java/net/conczin/mca/livingworld/voice/NpcVoiceCatalogTest.java`

**Produces:** deterministic `NpcVoiceCatalog.select(UUID, gender, age, config)` and immutable `NpcVoiceProfile`.

- [ ] Write tests proving same UUID/bucket always selects the same voice, different UUIDs distribute across a pool, empty exact pools follow fallback order, and `ttsVoice` is the terminal fallback.
- [ ] Implement gender and age normalization enums independent of Minecraft classes so isolated unit tests do not load game classes.
- [ ] Implement immutable profile record and deterministic floor-mod UUID hashing.
- [ ] Run `./gradlew :common:test --tests '*NpcVoiceCatalogTest'` and require PASS.

### Task 2: Persistent voice store and age-transition semantics

**Files:**
- Create: `common/src/main/java/net/conczin/mca/livingworld/voice/PersistentNpcVoiceStore.java`
- Test: `common/src/test/java/net/conczin/mca/livingworld/voice/PersistentNpcVoiceStoreTest.java`

**Produces:** `resolve(Path worldRoot, UUID npcId, NpcVoiceGender gender, NpcVoiceAgeGroup age, NpcVoiceCatalog catalog)`.

- [ ] Write tests for create/reload stability, one profile per UUID, atomic persistence, and reassignment only when age/gender bucket changes or stored voice is invalid for all fallbacks.
- [ ] Implement synchronized lazy load, temp-file + atomic replace, fail-open recovery and pretty JSON.
- [ ] Run focused tests and require PASS.

### Task 3: Configurable voice pools with backward-compatible defaults

**Files:**
- Modify: `common/src/main/java/net/conczin/mca/livingworld/LivingWorldConfig.java`
- Modify: `common/src/test/java/net/conczin/mca/livingworld/LivingWorldConfigTest.java`

**Produces:** normalized voice-pool lists for child/teen/adult by gender plus global fallback.

- [ ] Add failing tests for non-null normalized pools and `ttsVoice` compatibility.
- [ ] Add configurable lists with conservative built-in OpenAI voice ids; document that classification is a LivingWorld default, not provider metadata.
- [ ] Normalize null/blank/duplicate entries without making config version migration destructive.
- [ ] Run focused config tests and require PASS.

### Task 4: Provider-neutral mood/style request contract

**Files:**
- Create: `common/src/main/java/net/conczin/mca/livingworld/voice/NpcVoiceMood.java`
- Create: `common/src/main/java/net/conczin/mca/livingworld/voice/TtsVoiceStyle.java`
- Create: `common/src/main/java/net/conczin/mca/livingworld/voice/TtsRequest.java`
- Create: `common/src/main/java/net/conczin/mca/livingworld/voice/NpcVoiceMoodResolver.java`
- Modify: `common/src/main/java/net/conczin/mca/livingworld/voice/TextToSpeechProvider.java`
- Test: `common/src/test/java/net/conczin/mca/livingworld/voice/NpcVoiceMoodResolverTest.java`

**Produces:** immutable mood snapshot inputs and `TtsRequest` consumed by providers.

- [ ] Test `AFRAID > SAD > TIRED > NEUTRAL` priority from immutable booleans/health ratio.
- [ ] Map moods to bounded speed and concise delivery instructions; child/teen age guidance is composed separately from mood.
- [ ] Change the functional TTS interface to one abstract `synthesize(TtsRequest)` method and keep a backward-compatible default `synthesize(String)` helper.
- [ ] Run focused tests and require PASS.

### Task 5: OpenAI-compatible adapter capabilities

**Files:**
- Modify: `common/src/main/java/net/conczin/mca/livingworld/voice/OpenAIAudioProvider.java`
- Modify: `common/src/test/java/net/conczin/mca/livingworld/voice/OpenAIAudioProviderTest.java`

**Produces:** TTS JSON payload with per-NPC voice, bounded speed, and best-effort instructions.

- [ ] Add tests that `voice` always comes from `TtsRequest`, speed is included, `gpt-4o-mini-tts*` includes instructions, and `tts-1`/`tts-1-hd` omit unsupported instructions.
- [ ] Implement pure `createSpeechBody(TtsRequest, model)` helper for testability.
- [ ] Keep endpoint/auth/WAV behavior unchanged.
- [ ] Run focused tests and require PASS.

### Task 6: Server-thread capture and runtime integration

**Files:**
- Create: `common/src/main/java/net/conczin/mca/livingworld/voice/NpcVoiceSnapshot.java`
- Modify: `fabric/src/main/java/net/conczin/mca/fabric/livingworld/voice/VoiceConversationService.java`

**Produces:** immutable voice snapshot captured before async AI/TTS and stable profile resolution.

- [ ] On the server thread capture UUID, `getGenetics().getGender()`, `getAgeState()`, `getVillagerBrain().isPanicking()`, and health ratio.
- [ ] Normalize MCA-specific values into provider-neutral enums immediately.
- [ ] Resolve persistent profile using `snapshot.worldRoot()` before TTS; pass only immutable profile/style data to async synthesis.
- [ ] Preserve text publication before TTS, busy gates, target validation and spatial playback.
- [ ] Build Fabric and NeoForge and require compilation success.

### Task 7: Documentation and full verification

**Files:**
- Modify: `README.md`
- Modify: `docs/livingworld/CONFIGURATION.md`
- Modify: `docs/livingworld/VOICE.md`

- [ ] Document `voices.json`, stable UUID assignment, age/gender pools, mood best-effort behavior, provider/model independence and fallback semantics.
- [ ] Explain that OpenAI built-in voices are classified by LivingWorld configuration and can be overridden.
- [ ] Run `./gradlew :common:test :fabric:build`.
- [ ] Run the repository LivingWorld package smoke-check and official Fabric/NeoForge CI.
- [ ] Open focused PR, review diff, and merge only after all required checks are green.
