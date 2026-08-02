# Risk-Based Automated Acceptance Design

## Goal

Build a layered acceptance platform for VillAIgence that prevents regression coverage from being limited to previously observed bugs. Coverage is derived from architecture laws, persistent-state boundaries, gameplay invariants and external-system failure modes.

## Design decision

Use a hybrid test pyramid:

1. **Pure/unit contracts** for deterministic policies, serialization and bounded transformations.
2. **Server GameTests** for behavior that requires a real Minecraft server, registries, entities, navigation, blocks and ticks.
3. **Production-JAR gates** for remapping, Mixin/refmap, dependency and startup failures that development GameTests cannot prove.
4. **Restart/persistence scenarios** for state surviving a separate JVM process.
5. **Mock-provider integration** for deterministic Chat/STT/TTS success and failure paths.
6. **Manual canaries** only for physical audio, subjective behavior and true multi-client experience until dedicated harnesses exist.

No single layer may be used as evidence for another layer.

## Risk domains

Every acceptance scenario belongs to exactly one primary domain:

- `BOOT_PACKAGE`: exact artifact identity, dependencies, Mixin/refmap and production startup.
- `IDENTITY_LIFECYCLE`: NPC creation, death, grave, resurrection, conversion and UUID continuity.
- `PERSISTENCE_IDEMPOTENCY`: JSON validity, restart, replay, duplicate suppression and owner isolation.
- `NAVIGATION_SURVIVAL`: land, water, ladders, obstacles, mounts, drowning and recovery.
- `GAMEPLAY_INTERACTION`: dialogue, gifts, fishing, combat/archery and block/item round trips.
- `AI_VOICE_RESILIENCE`: Chat/STT/TTS success, timeout, null/malformed response, retry and body limits.
- `CONCURRENCY_AUTHORIZATION`: simultaneous edits, stale revisions, player/NPC isolation and server authority.

## Scenario catalog

A machine-readable catalog is the source of truth for coverage. Each entry contains:

- stable ID;
- primary risk domain;
- severity (`CRITICAL`, `HIGH`, `MEDIUM`, `LOW`);
- execution layer;
- automation state;
- invariant being proved;
- deterministic oracle;
- timeout budget;
- evidence artifact;
- optional manual-only rationale.

Catalog validation fails when:

- an ID is duplicated;
- a required field is missing;
- a domain has no scenario;
- a `CRITICAL` scenario is not automated and has no explicit accepted manual rationale;
- an automated scenario has no deterministic oracle or timeout;
- a scenario is marked automated but names no CI gate.

This turns test completeness into a maintained contract rather than a prose checklist.

## Server GameTest boundaries

The first executable GameTest suite covers broad primitives rather than only bug reproductions:

1. MCA entity registration and spawn in a real server world.
2. Filled tombstone data round trip using a real `TombstoneBlock.Data`, item component serialization and entity reconstruction.
3. Filled tombstone evaluated drops with Silk Touch contain exactly one portable tombstone with stored entity data.
4. Empty tombstone preserves its normal loot behavior.
5. Two independent NPCs can leave separate water lanes and remain alive.
6. An NPC that exits water can subsequently navigate on land.

Tests use fixed geometry, fixed inventory fixtures, bounded tick budgets and objective assertions. No test depends on random dialogue or external providers.

## CI topology

### Pull requests

Required and merge-blocking:

- scenario catalog validation;
- common unit tests;
- Fabric and NeoForge builds;
- server GameTests;
- remapped Fabric package verification.

### Canonical branch

Additionally required:

- production-JAR startup smoke;
- clean shutdown/restart smoke;
- persistent JSON schema/hash report;
- deterministic mock-provider integration.

### Nightly

Longer parameterized scenarios:

- water depth/shore geometry matrix;
- repeated grave round trips;
- multi-NPC isolation and pressure;
- retry/time-budget matrix;
- concurrency and stale-revision scenarios;
- client UI smoke when stable.

### Release

A release asset is promoted only after:

- exact candidate build;
- GameTests;
- production startup/restart;
- official asset publication;
- re-download and identity/package verification;
- official asset startup smoke.

## Reliability rules

- No real OpenRouter call in merge-blocking CI.
- No wall-clock sleeps when tick/event polling is available.
- Every asynchronous scenario has a hard timeout.
- Randomness is fixed or eliminated.
- Tests assert server-owned state, not rendered appearance.
- Test-only code stays in the `gametest` source set and never enters the production JAR.
- Failures preserve logs, JUnit XML, GameTest reports and the temporary world when possible.
- Flaky tests are quarantined from merge gates only with a tracked reason and owner; they are never silently retried until green.

## Delivery phases

### Phase A — foundation and first broad server suite

- scenario catalog and validator;
- isolated Fabric GameTest source set;
- entity, tombstone and water-navigation tests;
- PR CI integration and evidence artifacts.

### Phase B — installed production acceptance

- production server launcher task;
- startup log oracle;
- controlled shutdown/restart;
- persistent-state report.

### Phase C — deterministic AI/voice integration

- local mock Chat/STT/TTS provider;
- success, malformed, timeout, retry and size-limit scenarios;
- global Chat deadline regression.

### Phase D — concurrency and client acceptance

- two logical clients for lore stale-write conflict;
- client GameTests for editor UI;
- true multi-client/voice canary where CI infrastructure permits.

## Acceptance of this design

The user authorized autonomous implementation on 2026-08-03 after requesting broad, reliable coverage rather than tests limited to previously observed defects. Phase A is the first implementation increment; later phases remain explicit follow-on work and must not be represented as already automated.