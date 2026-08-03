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
6. **Manual canaries** only for physical audio, subjective behavior, installed release artifacts and true multi-client experience until dedicated harnesses exist.

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

The source of truth is the dependency-free, diff-friendly TSV catalog:

```text
common/src/test/resources/acceptance/scenarios.tsv
```

The canonical column order is:

```text
id	domain	severity	state	layer	gate	invariant	oracle	timeoutSeconds	evidence	manualRationale
```

Each entry contains:

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

A JUnit validator parses the catalog using only the Java standard library. This avoids changing dependency locks or expanding the trusted build surface for test metadata.

Catalog validation fails when:

- an ID is duplicated;
- a required field is missing;
- a domain has fewer than three scenarios;
- a `CRITICAL` scenario is neither automated nor an explicit accepted manual canary;
- an automated scenario has no deterministic oracle or timeout;
- a scenario is marked automated but names no CI gate.

This turns test completeness into a maintained contract rather than a prose checklist.

## Server GameTest boundaries

The first executable GameTest suite covers broad primitives rather than only bug reproductions:

1. MCA entity registration and production navigation wiring in a real Fabric server world.
2. Tombstone item round trip preserving UUID, name and the complete inventory multiset.
3. Filled tombstone evaluated drops with Silk Touch containing exactly one portable tombstone with stored entity data.
4. Empty tombstone negative control: no synthesized NPC data and no duplicate tombstone item.
5. Two isolated `MCAGroundPathNavigation` instances leaving separate bounded water lanes without shared-state interference.
6. The same production navigation implementation reaching a second dry target after deterministic water escape.
7. Direct water-surface hook validation against actual Minecraft water fluid tags in the GameTest world.

The suite deliberately separates two navigation questions:

- **real MCA integration wiring** is proved by spawning a registered MCA villager and asserting that it uses `MCAGroundPathNavigation`;
- **algorithmic water movement properties** are proved with a test-only no-brain `PathfinderMob` using the same production navigation class.

The controlled navigation fixture has no autonomous goals that can replace the assigned path. It supplies only deterministic buoyancy while a path is active, replacing the random timing of vanilla `FloatGoal`. That buoyancy does not construct, replace or advance paths. Full MCA brain/goal integration remains covered by the installed release canary and must not be inferred from the controlled fixture.

Tests use fixed geometry, fixed inventory fixtures, bounded tick budgets and objective assertions. No test depends on random dialogue or external providers. Inventory assertions intentionally verify items, totals and duplicate absence rather than sparse slot positions because MCA serializes its container as an item list and does not promise preservation of empty gaps.

## CI topology

### Pull requests

Required and merge-blocking:

- scenario catalog validation;
- common unit tests;
- explicit Fabric server GameTest execution;
- Fabric and NeoForge builds;
- remapped Fabric package verification;
- rejection of GameTest classes, metadata or entrypoints in the production JAR.

### Canonical branch — Phase B target

Additionally required after implementation:

- production-JAR startup smoke;
- clean shutdown/restart smoke;
- persistent JSON schema/hash report;
- deterministic mock-provider integration.

### Nightly target

Longer parameterized scenarios:

- water depth/shore geometry matrix;
- repeated grave round trips;
- multi-NPC isolation and pressure;
- retry/time-budget matrix;
- concurrency and stale-revision scenarios;
- client UI smoke when stable.

### Release target

A future release asset should be promoted only after:

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
- Test fixtures isolate the subsystem under test instead of mixing production navigation with unrelated autonomous brain scheduling.
- Controlled fixtures may supply deterministic physical prerequisites, but may not construct, replace or repeatedly refresh the production path under test.
- Test-only code stays in the `gametest` source set and never enters the production JAR.
- Failures preserve CI logs and available JUnit/GameTest diagnostics.
- Flaky tests are quarantined from merge gates only with a tracked reason and owner; they are never silently retried until green.
- Installed release evidence remains distinct from development GameTest evidence.

## Delivery phases

### Phase A — foundation and first broad server suite

Implemented in PR #103:

- TSV scenario catalog and dependency-free validator;
- isolated Fabric GameTest source set and test mod;
- real-server MCA wiring, tombstone lifecycle/drop/control tests;
- deterministic production-navigation water movement and surface-hook tests;
- explicit PR CI execution;
- production-JAR leakage guard;
- accurate linkage to installed `0.1.22` partial acceptance.

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
