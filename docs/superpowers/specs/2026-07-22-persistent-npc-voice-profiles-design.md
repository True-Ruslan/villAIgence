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

The assigned voice is stable across server restarts and LLM/provider changes. Existing profiles are reused. A profile is reassigned only when the NPC crosses an MCA age bucket boundary or the stored voice is no longer present in any configured fallback pool for that bucket.

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
2. same gender + adjacent/general adult-compatible pool;
3. neutral pool for the same age bucket;
4. global fallback;
5. legacy `ttsVoice`.

The built-in defaults use current OpenAI built-in voice ids as LivingWorld defaults only; these classifications are configurable and are not claims that the provider formally labels those voices by gender/age.

## Mood/style

Voice identity never changes because of mood.

A separate `NpcVoiceMood` is captured from authoritative server state before asynchronous TTS:

- `AFRAID` when the villager brain is panicking;
- `SAD` when health is critically low;
- `TIRED` when health is moderately low;
- otherwise `NEUTRAL`.

The design intentionally avoids having the LLM invent mood.

Mood maps to a provider-neutral `TtsVoiceStyle` containing:

- optional natural-language delivery instructions;
- a bounded speed multiplier.

The TTS adapter applies only capabilities supported by the configured provider/model. OpenAI-compatible models that support `instructions` receive them; legacy `tts-1` / `tts-1-hd` omit unsupported instructions and still use bounded speed. Unsupported style features degrade safely without changing the persistent voice id.

## Provider boundary

`TextToSpeechProvider` consumes a provider-neutral `TtsRequest` containing text, voice id, style/instructions, and speed. NPC profile selection does not inspect or depend on the chat/LLM model.

`OpenAIAudioProvider` translates `TtsRequest` into the OpenAI-compatible speech payload. Provider/model capability checks exist only inside the adapter.

## Runtime flow

1. On the Minecraft server thread, capture NPC UUID, MCA gender, MCA age state, panicking state, and health ratio.
2. Resolve/create the persistent voice profile in `<world>/livingworld/voices.json`.
3. Run AI response generation as today.
4. Always publish the text response.
5. When voice output is enabled, create `TtsRequest(text, persistentVoice, moodStyle)`.
6. TTS adapter synthesizes using the stable voice and best-effort style support.
7. Play spatial audio through Simple Voice Chat.

No mutable Minecraft entity state is read by the asynchronous TTS layer.

## Persistence and safety

- JSON store uses atomic temp-file replacement.
- Corrupt/missing store fails open: a deterministic profile is regenerated without breaking conversation.
- Writes are synchronized and bounded to one profile per known NPC UUID.
- API keys are unchanged and remain server-only.

## Compatibility

- `ttsVoice` remains the final fallback for old configs.
- Voice output remains optional and controlled by `voiceOutputEnabled`.
- STT behavior is unchanged.
- Existing worlds require no migration step; `voices.json` is created lazily.
- Chat/LLM model changes never alter stored NPC voice profiles.

## Verification

Tests cover:

- age/gender normalization;
- deterministic assignment;
- stable persistence/reload;
- age-transition reassignment;
- fallback behavior;
- mood resolution;
- TTS payload voice/speed/instructions behavior and legacy-model omission;
- Fabric and NeoForge builds plus LivingWorld distributable JAR smoke-check.
