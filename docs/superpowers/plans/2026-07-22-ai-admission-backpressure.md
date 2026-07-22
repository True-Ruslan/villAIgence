# VillAIgence AI Admission / Backpressure Implementation Plan

**Goal:** Add non-blocking per-stage concurrency limits, per-player/per-stage cooldowns and provider 429 cooldowns for Chat/STT/TTS, with operator visibility.

**Architecture:** Pure common-module admission controller returns an idempotent permit or a fast rejection. Provider boundaries never wait for capacity. Config supplies limits. HTTP 429/rate-limit signals activate stage-local cooldowns. `/villaigence ai status` exposes process-local admission metrics.

## Constraints

- Never block Minecraft server thread waiting for AI capacity.
- Never create an unbounded queue.
- Rejected requests must not call external providers.
- Preserve valid text when TTS is rejected.
- Preserve `mca`, `net.conczin.mca`, `config/livingworld.json`, `<world>/livingworld/`.
- No request/player content in admission diagnostics.

---

## Task 1 — Pure admission controller (TDD)

Create:

- `common/src/main/java/net/conczin/mca/livingworld/admission/AiAdmissionDecision.java`
- `AiAdmissionSettings.java`
- `AiAdmissionResult.java`
- `AiAdmissionSnapshot.java`
- `AiAdmissionController.java`
- tests under `common/src/test/.../admission/`

Required tests:

- first request is allowed;
- saturated stage rejects immediately;
- closing permit restores capacity;
- closing permit twice is safe;
- same player + same stage is cooldown-limited;
- another stage is independent;
- another player is independent;
- provider cooldown rejects before expiry and allows after expiry;
- repeated rate-limit signal only extends deadline;
- snapshot reports active, rejected and remaining cooldown.

Use package-private deterministic overloads with supplied `nowNanos` for tests; public APIs use `System.nanoTime()`.

Commit RED tests before production types, verify expected compile failure, then GREEN.

---

## Task 2 — Server configuration (TDD)

Modify `LivingWorldConfig` with defaults:

```text
aiChatMaxConcurrentRequests=4
aiSttMaxConcurrentRequests=2
aiTtsMaxConcurrentRequests=2
aiPerPlayerCooldownMillis=750
aiProviderRateLimitCooldownMillis=5000
```

Clamp:

```text
concurrency 1..64
player cooldown 0..60_000
provider cooldown 0..300_000
```

Tests must verify defaults, malformed extremes, and version-2 JSON without new fields retaining defaults.

`AiAdmissionSettings.from(config)` maps stage limits and cooldowns without secrets.

---

## Task 3 — Admission diagnostics/status (TDD)

Extend diagnostics with a safe local-rejection recorder and status output.

Requirements:

- local rejection uses controlled error type such as `admission_saturated`, `admission_player_cooldown`, `admission_provider_cooldown`;
- never claim a provider failure for local rejection;
- `/villaigence ai status` adds per-stage admission metrics:
  - active/max;
  - total rejected;
  - provider cooldown remaining ms;
- no UUIDs/content/secrets shown.

Keep existing two-argument report formatter compatibility if practical; add an overload for admission snapshot.

---

## Task 4 — Chat integration

Modify `DiagnosticsOpenAIChatAI`:

1. resolve settings;
2. non-blocking `tryAcquire(CHAT, player UUID, settings)`;
3. if rejected, record local admission failure and return existing controlled empty/fallback path without provider call;
4. if allowed, execute existing tracked chat lifecycle under try-with-resources permit;
5. permit always closes.

At actual Chat HTTP boundary, status `429` invokes:

```text
AiAdmissionController.onRateLimited(CHAT, configured cooldown)
```

No blind provider retry is introduced.

---

## Task 5 — STT/TTS integration

Modify `VoiceConversationService`:

### STT

- acquire STT permit immediately before `transcribe`;
- reject fast without provider call;
- release busy player/villager state;
- record local rejection diagnostics;
- permit closes around provider operation.

### TTS

- acquire TTS permit immediately before synthesis;
- on rejection, keep already-published NPC text;
- do not throw for expected backpressure;
- record local rejection diagnostics;
- permit closes around synthesis/resample.

When `VoiceDiagnosticsRecorder` classifies `http_429`, notify stage provider cooldown.

---

## Task 6 — Documentation and canonical state

Update:

- `README.md`;
- `docs/livingworld/CONFIGURATION.md`;
- `docs/livingworld/DIAGNOSTICS.md`;
- `docs/PROJECT_STATE.md`.

Document defaults, non-blocking behavior, rejection meanings and tuning guidance.

Remove rate-limit/backpressure/cooldown from pending immediate priorities after merge; next priorities become multiplayer concurrency/soak and restart/reconnect/data-integrity validation.

---

## Task 7 — Final verification

Require exact-final-head green:

```text
VillAIgence CI
- :common:test
- Fabric build
- distributable Fabric JAR smoke-check

Java Pull Request CI with Gradle
- NeoForge build
- Fabric build
```

Review final diff for:

- permit leaks;
- counter underflow;
- blocking waits/semaphores;
- accidental request content/UUID exposure;
- provider calls on rejected paths;
- unrelated MCA/Fabric changes.

Merge only with exact expected head and no unresolved critical/important review findings.
