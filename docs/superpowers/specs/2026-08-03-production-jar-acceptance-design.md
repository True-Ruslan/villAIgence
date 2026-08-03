# Production-JAR Acceptance Design

## Goal

Close the boundary that development mappings, unit tests and Fabric GameTests cannot prove: the exact remapped VillAIgence Fabric JAR must start in an isolated production Fabric server, stop cleanly, start the same world again and preserve valid persistent state.

## Evidence boundary

This harness proves:

- the remapped distributable JAR is the only `mca` implementation in `mods/`;
- official Fabric Loader starts Minecraft `1.21.1` in production namespace;
- required server-side dependencies resolve as normal Fabric mods;
- Mixin/refmap targets resolve during real startup;
- the server reaches Minecraft's ready marker;
- stdin `stop` causes a bounded clean shutdown;
- a second JVM starts the same world with the same candidate JAR;
- persistent JSON files are valid and unchanged when no scenario intentionally mutates them.

It does not prove client rendering, external provider behavior, physical microphone/audio behavior or true multi-client interaction.

## Runtime assembly

Gradle creates a deterministic staging bundle under `fabric/build/production-acceptance/stage` containing:

```text
manifest.json
installer/fabric-installer.jar
mods/villaigence-under-test.jar
mods/fabric-api-*.jar
mods/simple-voice-chat-*.jar
```

Rules:

- the VillAIgence candidate comes only from `remapJar`;
- Fabric Installer uses an explicit pinned version and Gradle dependency verification;
- runtime mods use the same locked coordinates already declared by the project;
- the staging task rejects duplicate or unexpected candidate/mod sets;
- no development classes or GameTest classes enter the server `mods/` directory.

The installer is executed in CLI server mode with explicit Minecraft and Loader versions and `-downloadMinecraft`. Fabric's installer/server launcher then resolves official Minecraft libraries using upstream metadata and checksums.

## Harness implementation

A Python standard-library program owns process and evidence handling:

```text
scripts/ci/production_server_acceptance.py
```

It performs:

1. strict staging-manifest validation and path confinement;
2. fresh temporary server installation;
3. EULA and deterministic low-resource `server.properties` generation;
4. candidate/runtime mod copy with duplicate-ID guard inputs;
5. first JVM startup with a hard timeout;
6. required/forbidden log signature evaluation;
7. bounded stdin `stop` and exit-code verification;
8. persistent JSON discovery, parsing and SHA-256 report;
9. second JVM startup against the same world;
10. second clean shutdown and persistent hash comparison;
11. deterministic JSON evidence output.

The harness never uses shell interpolation for Java arguments and never executes paths from the manifest as commands.

## Startup oracle

Required evidence:

- Fabric Loader reports Minecraft `1.21.1`;
- the loaded-mod inventory contains `mca` and the expected candidate version;
- Minecraft reports `Done (` and `For help, type "help"`;
- both runs exit with code `0` after `stop`;
- save completion appears before process exit.

Forbidden evidence includes:

- `InvalidInjectionException`;
- `MixinApplyError`;
- `MixinTransformerError`;
- `No refMap loaded`;
- failed `MixinGroundPathNavigation`, `MixinTombstoneData` or obsolete `MixinTombstoneBlock` application;
- incompatible/missing required mods;
- failed server initialization;
- `OutOfMemoryError`;
- abnormal JVM termination.

Patterns are centralized and unit-tested. A failure report includes only bounded log excerpts and contains no environment-variable dump.

## Persistence oracle

Canonical stores:

```text
memory.json
memory2.json
semantic-memory.json
relationships.json
voices.json
operator-lore.json
```

After each clean shutdown the harness recursively discovers these basenames beneath the isolated server root.

For every discovered store it records:

- path relative to the server root;
- byte size;
- SHA-256;
- parsed JSON root type.

Acceptance requires:

- exactly one file per canonical basename;
- valid UTF-8 JSON;
- the same relative path before and after restart;
- identical SHA-256 when no mutation fixture was executed.

If a fresh startup does not create a store, the first implementation must report that as an explicit missing-store failure. The test must not silently fabricate application state. A later deterministic server-side fixture may be added only if startup alone does not exercise store creation.

## Reliability and security

- Java 21 only.
- Hard timeout for installer, startup and shutdown.
- No `shell=True` and no dynamically constructed shell command.
- Temporary directory is deleted only after evidence has been copied.
- Candidate and staged dependency hashes are recorded before launch.
- Paths in `manifest.json` must be relative, normalized and confined to the staging root.
- The server binds only for the duration of the isolated run; online mode is disabled and no player joins.
- No provider credentials are supplied.
- CI uploads bounded evidence on success and failure.
- The repository security inventory explicitly classifies the network-capable harness.

## CI topology

Phase B begins as a separate draft PR and a dedicated workflow step. Once stable, the production acceptance command becomes merge-blocking for changes that can affect:

- Fabric packaging/runtime;
- Mixins/refmap;
- navigation/tombstone startup boundaries;
- persistence loaders/writers;
- Gradle dependencies and lock metadata;
- the harness itself.

The existing GameTest matrix remains mandatory and is not replaced.

## TDD sequence

1. Unit RED for manifest confinement, log oracle, timeout state and persistence comparison.
2. Unit GREEN with no Minecraft process involved.
3. Gradle staging RED on missing dependency lock/verification metadata.
4. Staging GREEN with exact bundle inventory.
5. Production startup RED if the real runtime exposes an unresolved dependency, startup signature or missing store.
6. Correct only the discovered boundary.
7. Require repeated exact-head GREEN in independent CI paths before merge.
