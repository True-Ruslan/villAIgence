# M11 Phase E — Automation Completion Design

## Status

Approved for implementation on 2026-08-05.

## Problem

VillAIgence already has strong unit, integration, GameTest, production-JAR startup/restart and release-identity coverage, but the acceptance catalog still contains manual canaries that partially duplicate deterministic server behavior. The project has insufficient people and time for repeated broad manual regression. Manual work must be reduced to observations that intrinsically require physical hardware, a real graphical client, or human perception.

## Goal

Move every deterministic, reproducible acceptance invariant into CI while preserving a small installed smoke boundary for:

1. operating-system microphone permission and a physical microphone;
2. audible and spatial TTS perception;
3. final visual review of client screens and conflict messaging;
4. one exact released-JAR installation smoke in the operator modpack.

## Non-goals

- Do not publish or request `0.1.26+1.21.1` during Phase E.
- Do not replace deterministic provider fixtures with paid external providers in CI.
- Do not weaken timeouts, security limits, release identity checks or exactly-once persistence to make tests green.
- Do not introduce sleeps as correctness oracles.
- Do not require secrets for pull-request CI.

## Architecture

Phase E extends the existing risk catalog and four current automation layers rather than creating a parallel test framework:

1. **Common tests** prove pure policy, authorization, persistence and provider contracts.
2. **Fabric GameTests** prove real Minecraft/MCA entity, block, inventory, navigation and gameplay behavior.
3. **Production acceptance** starts the exact remapped candidate in isolated JVMs and verifies process, restart and world-local persistence invariants.
4. **Release acceptance** reuses the exact accepted artifact and blocks publication on all mandatory gates.

Longer and destructive scenarios are separated into a deterministic nightly suite. Release requests must execute both the normal pull-request gate and the nightly/release-only suite against the exact candidate.

## Workstreams

### E0 — Acceptance infrastructure hardening

`stageProductionAcceptanceRuntime` must not query `Project` during task execution. Configuration-time providers and captured immutable values become declared task inputs. The task must pass with configuration cache and remain compatible with Gradle 10.

### E1 — Identity duplication prevention

Automate `VAI-LIFE-005` with a real server GameTest. Replaying resurrection or restoration for one stored UUID must leave exactly one live entity with that UUID. The original tombstone data remains authoritative and inventory is not duplicated.

### E2 — Full lifecycle across restart

Extend the production fixture to exercise:

`real MCA death -> tombstone capture -> portable grave data -> placement/restoration -> resurrection -> stop -> second JVM -> identity and inventory verification`.

The report must include fixture UUID, name, expected inventory multiset, live entity count and before/after restart evidence.

### E3 — Corrupt persistence recovery

Create a destructive temporary-world matrix for all auxiliary stores. Each case must verify bounded startup, backup creation, canonical valid JSON regeneration, isolation from other stores and idempotent second startup. No real user world is used.

### E4 — Text packet end-to-end

Use an authenticated synthetic player and production packet handlers to prove target resolution, authorization, one provider turn, one dialogue effect, one response and correct NPC ownership.

### E5 — Networked two-client Operator Lore

Use two independent logical network sessions against one server. The first write succeeds, the stale writer receives canonical conflict data, an explicit retry succeeds exactly once, unauthorized scope access is rejected and drafts remain client-owned.

### E6 — Voice transport integration

Exercise Simple Voice Chat integration and local codec/transport boundaries without real hardware: Opus encode/decode, bounded PCM, loopback transport, ordering, loss, disconnect and cancellation. Existing deterministic STT/Chat/TTS provider fixtures remain authoritative.

### E7 — Gameplay and navigation matrix

Automate `VAI-NAV-004` and `VAI-GAME-002` in a nightly suite: ladders, doors, obstacles, mounts, ranged combat, gifts and fishing. Each fixture has an explicit terminal oracle and bounded timeout.

### E8 — Real MCA-brain navigation

Add nightly scenarios using actual MCA villagers and goals in separate lanes. Controlled navigation fixtures remain fast PR primitives; the real-brain suite detects integration regressions.

### E9 — Soak and release-risk selection

Add deterministic repeated restart, concurrency, memory-pressure and provider-failure scenarios. A risk selector maps changed paths to mandatory focused suites, while the release gate always runs the complete mandatory set.

## Evidence model

Every automated scenario records:

- stable scenario ID;
- exact commit and candidate SHA-256;
- layer and timeout;
- deterministic fixture inputs;
- explicit oracle values;
- PASS/FAIL status;
- machine-readable artifact where the layer supports it.

The acceptance catalog is the canonical mapping from product risk to proof. A scenario may move from `MANUAL_CANARY` or `PLANNED` to `AUTOMATED` only after exact-head CI evidence exists.

## Failure policy

- Fixture setup failure is a test failure, not a skip.
- Timeout is a failure with captured diagnostics.
- Recovery tests operate only in generated temporary worlds.
- Network tests fail closed on missing authenticated identity or scope.
- Provider integration remains bounded by one monotonic deadline and response-size limits.
- Flaky retries are not permitted as a substitute for deterministic fixtures.

## CI topology

### Pull-request gate

Common tests, security policy, focused provider integration, focused GameTests, Fabric/NeoForge builds, production startup/restart and package verification.

### Nightly gate

Corruption matrix, expanded gameplay/navigation, real MCA brain, concurrency stress and repeated lifecycle runs.

### Release gate

All pull-request and nightly mandatory scenarios against the exact versioned candidate, followed by artifact re-download, byte identity and startup/restart of the downloaded artifact.

## Completion criteria

Phase E is complete when:

1. all four `PLANNED` scenarios are automated or explicitly replaced by stronger automated scenarios;
2. deterministic portions of current manual canaries are automated and catalog rationale is narrowed to genuinely physical/visual boundaries;
3. production acceptance runs without execution-time `Project` access warnings;
4. pull-request, nightly and release gates emit reviewable evidence;
5. canonical project state and roadmap identify only the irreducible installed smoke steps;
6. no release is published solely because tests were added—the exact candidate still passes the final release gate.