# VillAIgence 0.1.x Reliability Playtest Checklist

Use this checklist to close the final live-server validation gate for the `0.1.x` reliability milestone before starting the `0.2 Memory 2.0` implementation branch.

Automated CI covers unit regressions, Fabric packaging/smoke-checks and Fabric/NeoForge compilation. It does **not** replace live Minecraft evidence for multiplayer concurrency, real microphone traffic, provider throttling, reconnect behavior or world backup/restore.

## Baseline

Record before testing:

```text
VillAIgence version/tag:
Minecraft version: 1.21.1
Fabric Loader version:
Fabric API version:
Simple Voice Chat version:
Java version: 21.x
AI provider / chat model:
STT provider / model:
TTS enabled: yes/no
TTS provider / model / response format:
Number of simultaneous players:
```

Back up the world before destructive recovery/restore scenarios.

## 1. Startup and diagnostics sanity

1. Start the server with the intended production-like `config/livingworld.json`.
2. Run:

```text
/villaigence ai status
```

3. Verify Chat/STT/TTS readiness is correct for the configuration.
4. Verify no API key, Authorization header, prompt, transcript, NPC answer, TTS input or reasoning text is displayed.
5. Before first provider traffic, runtime state may legitimately be `NEVER`.
6. After successful requests, verify the corresponding stages report `SUCCESS` with bounded safe metadata.

**Pass:** configuration/readiness is accurate and diagnostics reveal only the documented safe metadata.

## 2. Repeated voice-dialogue soak

Recommended minimum: 30 consecutive completed interactions with the same player and at least 3 different NPCs.

For each interaction:

```text
microphone
→ Simple Voice Chat
→ STT
→ selected NPC
→ Chat/LLM
→ visible text reply
→ optional TTS/spatial voice
```

Verify throughout:

- no player or villager remains permanently busy after success/failure;
- text replies continue after repeated requests;
- a TTS failure or local TTS rejection never removes a valid text reply;
- NPC voice identity remains stable for the same NPC;
- malformed/empty provider output never leaks raw JSON or reasoning;
- server tick responsiveness remains normal.

**Pass:** all 30+ interactions complete or fail through controlled fallbacks without a stuck conversation state or server-impacting request storm.

## 3. Multiplayer concurrency and admission limits

Use at least 2 players; 3-4 is preferred when practical.

Trigger overlapping voice/text interactions so Chat/STT/TTS concurrency approaches or exceeds configured limits.

Verify `/villaigence ai status` admission metrics:

```text
active/max
rejected
providerCooldownMs
```

Expected controlled local rejection types include:

```text
admission_saturated
admission_player_cooldown
admission_provider_cooldown
```

Verify:

- the Minecraft server thread does not block waiting for AI capacity;
- rejected requests do not create an unbounded queue;
- a rejected STT request releases player/villager busy state;
- stage-local player cooldown does not break the normal `STT → Chat → TTS` pipeline;
- one player's saturation does not corrupt another player's NPC state.

**Pass:** concurrency is bounded, rejections are controlled, and normal traffic resumes after capacity/cooldown clears.

## 4. Provider 429 / cooldown recovery

Use a provider/test condition that can safely produce an HTTP 429 or equivalent rate-limit response. Do not intentionally spend excessive credits to force this.

Verify:

1. the affected stage records a controlled rate-limit failure;
2. temporary provider cooldown becomes visible in status metrics;
3. requests during cooldown are locally rejected instead of repeatedly hitting the provider;
4. after cooldown expiry, a normal request can succeed again;
5. no memory/action/relationship side effect is duplicated because of failed/retried provider calls.

**Pass:** provider throttling produces bounded local backpressure and automatic recovery.

## 5. Restart/reconnect persistence

Create known state before shutdown:

- at least one persistent dialogue exchange in `memory.json`;
- at least one relationship delta in `relationships.json`;
- at least one factual event in `events.json`;
- at least one assigned NPC voice in `voices.json`.

Stop the server cleanly, restart it, reconnect the same player, and interact with the same NPCs.

Verify:

- important prior dialogue/context is still available where expected;
- relationship state persists;
- factual event context persists within configured bounds/TTL;
- NPC voice assignment remains stable;
- no duplicate state appears solely because of restart/reconnect.

**Pass:** all four world-local stores survive restart and reconnect with expected bounded semantics.

## 6. Malformed auxiliary JSON recovery

This behavior is regression-tested automatically, but may be smoke-tested on a disposable world/server copy.

For exactly one auxiliary file at a time:

```text
<world>/livingworld/memory.json
<world>/livingworld/events.json
<world>/livingworld/relationships.json
<world>/livingworld/voices.json
```

replace the file with malformed JSON while the server is stopped, then start the server and exercise the affected subsystem.

Expected behavior:

- the affected auxiliary state fails open to an empty/neutral/re-resolved state;
- conversation/gameplay does not crash because that JSON cannot be parsed;
- the next normal mutation may replace the malformed file with valid current-format JSON.

**Important:** this is availability recovery, not data reconstruction. Restore from backup when preserving the old data matters.

**Pass:** malformed auxiliary persistence does not break the AI conversation path or require manual file deletion to resume operation.

## 7. World backup and restore

1. Stop the server cleanly.
2. Back up the complete world, including `<world>/livingworld/`.
3. Start the server, create additional VillAIgence state, then stop it.
4. Restore the earlier complete world backup.
5. Start the server again.

Verify the restored VillAIgence state matches the restored world snapshot rather than the later discarded state.

**Pass:** VillAIgence state follows world backup/restore boundaries consistently.

## 8. Final evidence to record

For milestone closure, record:

```text
[ ] Startup/status sanity passed
[ ] 30+ voice-dialogue soak passed
[ ] Multiplayer concurrency/admission passed
[ ] 429/cooldown recovery passed or provider could not safely reproduce 429
[ ] Restart/reconnect persistence passed
[ ] Malformed JSON recovery automated tests green; optional live smoke passed
[ ] World backup/restore passed
[ ] No unresolved crash/data-corruption regression found
```

Attach only privacy-safe evidence. Do not publish API keys, Authorization headers, prompts, private voice transcripts, NPC responses intended to remain private, raw provider payloads or reasoning content.

## Milestone decision

When the checklist passes without a release-blocking defect, `0.1.x Reliability / Provider Hardening` can be considered stable enough for normal playtesting and the next major feature branch should begin `0.2 Memory 2.0`.

Any defect found here should be fixed as a narrow `0.1.x` reliability patch rather than mixed into the Memory 2.0 schema/architecture branch.
