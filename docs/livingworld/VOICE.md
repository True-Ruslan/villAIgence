# LivingWorld voice MVP

## Required mods

Server and clients:

- Minecraft 1.21.1
- Fabric Loader / Fabric API
- this MCA LivingWorld fork
- Simple Voice Chat compatible with API 2.6.20

Only the server needs the OpenAI API key.

## How to talk to an NPC

1. Interact normally with an MCA villager to select that villager as your current conversation target.
2. Use your normal Simple Voice Chat microphone / push-to-talk key and speak.
3. Stop speaking briefly. After roughly 800 ms of microphone inactivity, the utterance is finalized.
4. The server runs STT, sends the transcript through the existing MCA/LivingWorld ChatAI context, synthesizes the answer, and plays it spatially from the NPC.
5. The answer is also added to the existing MCA conversation text path.

The selected target expires using MCA's existing conversation distance and timeout rules. Interact with the villager again to explicitly re-select it.

## Important behavior

- Ambient player voice is not transcribed unless the player has intentionally opened a conversation with an MCA villager.
- Normal player-to-player Simple Voice Chat traffic is not cancelled or modified.
- While a player's AI request is in progress, additional microphone packets are ignored by LivingWorld to avoid duplicate STT/LLM requests. They still remain normal voice-chat audio.
- Voice input is capped at 20 seconds per utterance by default.
- Audio shorter than 250 ms is ignored.
- NPC output is played from an entity-bound Simple Voice Chat channel with a default 32-block range.

## Data flow

`microphone -> Simple Voice Chat -> Opus decode -> 48 kHz PCM -> WAV -> OpenAI STT -> MCA ChatAI -> OpenAI TTS WAV -> 48 kHz PCM -> Simple Voice Chat spatial NPC audio`

Raw microphone audio is processed in memory and is not intentionally persisted by LivingWorld.

## Troubleshooting

- No NPC reaction: verify you interacted with the NPC first and that `apiKey` or `OPENAI_API_KEY` is set.
- Text works but voice does not: verify Simple Voice Chat is installed and connected on both server and client.
- API errors: inspect the dedicated server log; secrets are not printed by LivingWorld.
- Wrong language recognition: set `sttLanguage` in `livingworld.json` to an ISO-639-1 code such as `ru`; blank enables automatic detection.
