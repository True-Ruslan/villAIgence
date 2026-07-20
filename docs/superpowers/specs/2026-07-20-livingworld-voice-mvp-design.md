# LivingWorld Voice MVP Design

## Goal

Let players on a Fabric 1.21.1 dedicated server speak to the MCA villager they intentionally selected, with all STT/LLM/TTS processing and API credentials on the server.

## User experience

1. Player interacts normally with an MCA villager. That villager becomes the player's active AI conversation target.
2. While the target remains valid and nearby, Simple Voice Chat microphone packets are captured server-side in addition to normal player voice chat.
3. A short pause finalizes one utterance. The server sends that utterance to STT, then into the existing MCA/LivingWorld ChatAI context.
4. The villager's answer is shown through the existing conversation text path and synthesized with TTS.
5. TTS audio is played spatially from the villager entity through Simple Voice Chat.

No API key, Python process, desktop application, or AI configuration is required on player clients.

## Architecture

- `common`: loader-independent audio codec helpers, OpenAI STT/TTS clients, config defaults, and explicit ChatAI conversation-target API.
- `fabric`: optional Simple Voice Chat plugin, microphone session buffering, async orchestration, and entity-bound playback.
- Existing MCA `ChatAI` remains the dialogue/context authority. Voice is another input/output transport, not a second AI stack.

## Activation and cost control

Voice AI is gated by an active MCA conversation target established by normal villager interaction. Ambient voice is never sent to STT. A target expires using the existing MCA conversation timeout/distance rules.

Utterances are segmented by microphone packet inactivity, with minimum and maximum durations. All voice settings have defaults; `apiKey`/`OPENAI_API_KEY` remains the only required LivingWorld setting.

## Default provider configuration

- STT: OpenAI `gpt-4o-mini-transcribe`, `/v1/audio/transcriptions`
- TTS: OpenAI `gpt-4o-mini-tts`, `/v1/audio/speech`, voice `marin`, WAV response
- Input audio: decoded Simple Voice Chat PCM, 48 kHz mono, encoded as WAV for STT
- Output audio: WAV decoded/resampled to 48 kHz mono PCM before Simple Voice Chat playback

## Safety and failure behavior

- Secrets stay server-side and are never logged.
- Network/AI calls never block the Minecraft server thread.
- Empty/too-short utterances are ignored.
- STT/LLM/TTS failure logs server-side diagnostics and does not crash the server or suppress normal voice chat.
- Simple Voice Chat remains optional at class-loading level; without it, text MCA/LivingWorld behavior continues.

## MVP scope exclusions

No ambient NPC hearing, interruptions/barge-in, streaming STT/TTS, NPC-to-NPC speech, voice cloning, per-NPC custom voices, SQLite world memory, rumors, or factions in this milestone.
