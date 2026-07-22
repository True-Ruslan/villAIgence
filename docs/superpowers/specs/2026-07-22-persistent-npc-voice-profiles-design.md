# Persistent NPC Voice Profiles

## Goal

Give every LivingWorld NPC a stable voice identity that is independent from the selected chat/LLM model, while adapting voice selection to MCA gender and age and adapting delivery style to current NPC mood/state.

## Voice identity

Each NPC receives a persistent profile keyed by NPC UUID and stored under `<world>/livingworld/voices.json`.

A profile contains:

- NPC UUID;
- normalized gender bucket: `male`, `female`, or `neutral`;
- normalized age bucket: `child`, `teen`, or `adult`;
- assigned provider voice id.

The assigned voice is stable across server restarts and LLM/provider changes. Existing profiles are reused. A profile is reassigned only when the NPC crosses an MCA age bucket boundary, its gender bucket changes, or the stored voice is no longer present in any compatible configured fallback pool.

MCA age mapping:

- `BABY`, `TODDLER`, `CHILD` → `child`;
- `TEEN` → `teen`;
- `ADULT`, `UNASSIGNED` → `adult`.

MCA has no elder age state in the current 1.21.1 codebase, so no synthetic elder classification is introduced.

## Voice catalog and fallback

LivingWorld configuration exposes provider-neutral voice pools for:

- male/female/neutral child;
- male/female/neutral teen;
- male/female/neutral adult;
- global fallback.

Selection is deterministic from NPC UUID within the first non-empty compatible pool. This prevents per-turn random voices while still distributing NPCs across available voices.

Fallback order:

1. exact gender + age bucket;
2. same-gender adult pool for child/teen NPCs;
3. neutral pool for the same age bucket;
4. global fallback;
5. legacy `ttsVoice`.

The built-in defaults use OpenAI-compatible voice ids as LivingWorld defaults only; these classifications are configurable and are not claims that the provider formally labels those voices by gender/age.

## Mood/style

Voice identity never changes because of mood.

A separate `NpcVoiceMood` is derived from authoritative server-owned state captured before asynchronous TTS:

- `AFRAID` for active panic or high persistent fear;
- `SAD` for critically low health;
- `ANGRY` for strongly negative trust;
- `HAPPY` for high trust plus affinity;
- `TIRED` for moderately reduced health;
- otherwise `NEUTRAL`.

The design intentionally avoids having the LLM invent mood.

Mood maps to a provider-neutral `TtsVoiceStyle` containing:

- optional natural-language delivery instructions;
- a bounded speed multiplier.

Age guidance is composed into the style separately from mood so child/teen delivery can be younger without changing the persistent voice identity.

The TTS adapter applies only capabilities supported by the configured provider/model. Instruction-capable OpenAI-compatible models receive style instructions; legacy `tts-1` / `tts-1-hd` omit unsupported instructions and still use bounded speed. Unsupported style features degrade safely without changing the persistent voice id.

## Provider boundary

`TextToSpeechProvider` remains backward-compatible with the existing `synthesize(String)` contract and additionally accepts a default rich `synthesize(TtsRequest)` path. Providers that do not implement rich voice/style capabilities safely fall back to text-only synthesis behavior.

`TtsRequest` contains text, voice id, style/instructions, and speed. NPC profile selection does not inspect or depend on the chat/LLM model.

`OpenAIAudioProvider` translates `TtsRequest` into the OpenAI-compatible speech payload. Provider/model capability checks exist only inside the adapter.

## Credential boundary

Chat, STT and TTS credentials are resolved independently.

For the OpenAI speech endpoint:

1. `OPENAI_API_KEY`;
2. dedicated `ttsApiKey`;
3. main provider key only when the main provider is also `openai`.

An OpenRouter chat key is never reused for an OpenAI TTS endpoint. Custom TTS endpoints may use `ttsApiKey` and then the main provider key as a compatibility fallback.

Endpoint ownership checks use the parsed URI hostname rather than substring matching.

## Runtime flow

1. On the Minecraft server thread, capture NPC UUID, MCA gender, MCA age state, panicking state, and health ratio.
2. Run AI response generation as today using the immutable LivingWorld context snapshot.
3. Always publish the text response.
4. When voice output is enabled, resolve/create the persistent voice profile in `<world>/livingworld/voices.json`.
5. Read server-owned relationship state and derive current `NpcVoiceMood`.
6. Create `TtsRequest(text, persistentVoice, moodStyle)`.
7. TTS adapter synthesizes using the stable voice and best-effort style support.
8. Play spatial audio through Simple Voice Chat.

No mutable Minecraft entity state is read by the asynchronous TTS layer after capture.

## Persistence and safety

- JSON store uses temp-file replacement with atomic move where supported.
- Corrupt/missing store fails open: a deterministic profile is regenerated without breaking conversation.
- Writes are synchronized and bounded to one profile per known NPC UUID.
- API keys remain server-only; `ttsApiKey` is an optional dedicated server-side credential.

## Compatibility

- `ttsVoice` remains the final fallback for old configs.
- Voice output remains optional and controlled by `voiceOutputEnabled`.
- STT behavior is unchanged.
- Existing worlds require no manual migration step; `voices.json` is created lazily.
- Chat/LLM model changes never alter stored NPC voice profiles.
- Existing TTS providers implementing only `synthesize(String)` remain source-compatible through the default rich-request fallback.

## Verification

Tests cover:

- age/gender normalization;
- deterministic assignment;
- stable persistence/reload;
- age/gender-transition reassignment;
- fallback behavior including neutral young NPCs;
- mood resolution and age-aware style composition;
- TTS payload voice/speed/instructions behavior and legacy-model omission;
- independent and hostname-safe TTS credential routing;
- Fabric and NeoForge builds plus LivingWorld distributable JAR smoke-check.
