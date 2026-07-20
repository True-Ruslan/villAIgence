# LivingWorld Voice MVP Design

## Goal

Let players on a Fabric 1.21.1 dedicated server speak to the MCA villager they intentionally selected, with all STT/LLM/TTS processing and API credentials on the server.

## User experience

1. Player interacts normally with an MCA villager. That villager becomes the player's active AI conversation target.
2. Player looks toward the selected villager and speaks using their normal Simple Voice Chat microphone/PTT flow.
3. A short pause finalizes one utterance. Before any paid STT call, the server verifies that the target is still nearby, visible, and in the player's view.
4. The server sends the utterance to STT, then into the existing MCA/LivingWorld ChatAI context.
5. The villager's answer is shown through the existing conversation text path and synthesized with TTS.
6. TTS audio is played spatially from the villager entity through Simple Voice Chat.

No API key, Python process, desktop AI application, or AI-provider configuration is required on player clients.

## Architecture

- `common`: loader-independent audio codec helpers, OpenAI STT/TTS clients, config defaults, and explicit ChatAI conversation-target API.
- `fabric`: Simple Voice Chat plugin integration, microphone session buffering, async orchestration, and entity-bound playback.
- Existing MCA `ChatAI` remains the dialogue/context authority. Voice is another input/output transport, not a second AI stack.

## Activation and cost control

Voice AI is gated by an active MCA conversation target established by normal villager interaction and a view/line-of-sight check before transcription. Ordinary proximity voice is never cancelled or modified. A target expires using the existing MCA conversation timeout/distance rules.

Utterances are segmented by microphone packet inactivity, with minimum and maximum durations. All voice settings have defaults; `apiKey`/`OPENAI_API_KEY` remains the only required LivingWorld AI setting.

## Default provider configuration

- LLM: OpenAI `gpt-4.1-mini`, `/v1/chat/completions`
- STT: OpenAI `gpt-4o-mini-transcribe`, `/v1/audio/transcriptions`
- TTS: OpenAI `tts-1`, `/v1/audio/speech`, voice `marin`, WAV response
- Input audio: decoded Simple Voice Chat PCM, 48 kHz mono, encoded as WAV for STT
- Output audio: WAV decoded/resampled to 48 kHz mono PCM before Simple Voice Chat playback

## Dependency policy

The Fabric LivingWorld voice build requires Simple Voice Chat API `2.6.20` or newer. This follows the upstream API recommendation to reject incompatible older versions at mod-load time rather than fail later when the voice plugin is initialized.

## Safety and failure behavior

- Secrets stay server-side and are never logged.
- Network/AI calls never block the Minecraft server thread.
- Empty/too-short utterances are ignored.
- STT/LLM/TTS failure logs server-side diagnostics and does not crash the server or suppress normal voice chat.
- One player may have at most one in-flight LivingWorld voice AI request in the MVP.

## MVP scope exclusions

No ambient NPC hearing, interruptions/barge-in, streaming STT/TTS, NPC-to-NPC speech, voice cloning, per-NPC custom voices, SQLite world memory, rumors, or factions in this milestone.
