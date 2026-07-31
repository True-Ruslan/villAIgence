# H3 Supply-Chain Hardening Evidence — 2026-07-31

## Scope

This record covers Step 1 H3: immutable and verified build inputs.

Implementation branch:

```text
agent/h3-supply-chain-verification
```

Final automated-validation head before documentation-only closure updates:

```text
4d00ff296819196bd12fd5e3f16fd93820b5cf9c
```

Merged through PR #61 as squash commit:

```text
4cf9aef2e5c31a5682a7cad8544219154330e056
```

The change does not modify gameplay behavior, provider behavior, Memory 2.0 schemas or persistent world formats.

## Implemented controls

- Fabric Loom moved from a snapshot to stable `1.17.17`.
- Gradle wrapper `9.6.1-bin` is pinned with the official distribution SHA-256.
- Gradle wrapper validation is enabled in action-based CI and release workflows.
- External GitHub Actions are pinned to full 40-character commit SHAs with version comments.
- Dependency locking is enabled for all Gradle projects.
- Committed lockfiles exist for `common`, `fabric` and `neoforge`.
- Gradle dependency verification metadata contains SHA-256 checksums for resolved external artifacts and metadata.
- Modrinth and MaxHenkel Maven repositories are restricted to required groups.
- The unused broad BlameJared Maven repository was removed.
- Release builds generate a sorted, timestamp-free dependency manifest from committed lockfiles.
- The release package contains the JAR, JAR SHA-256 and dependency manifest.
- A controlled dependency-update procedure forbids unreviewed manual checksum or lockfile edits.
- A read-only supply-chain workflow performs cold dependency refresh, common tests, Fabric build, NeoForge build, wrapper validation and verification-input immutability checks.

## Fabric Loom generated-artifact boundary

Fabric Loom creates local development artifacts after verifying and transforming external inputs. Their bytes can vary between clean runners or cache states even when the external source dependencies are unchanged.

Three narrowly scoped Gradle `trusted-artifacts` rules are therefore used:

1. `loom:mappings:layered+hash.*` JARs generated from checksum-verified mapping inputs;
2. JARs under Loom's synthetic `remapped.*` groups;
3. the synthetic `net.minecraft:minecraft-merged-<hash>` development JAR for Minecraft `1.21.1` and the configured layered mappings.

The exemption is restricted to generated JAR files. External Fabric, Loom, Minecraft metadata, mapping inputs, plugins, modules and POM files remain checksum verified.

Regression policy enforces that:

- only these three reviewed trust-rule classes exist;
- the whole `loom` or `net.fabricmc` groups are never broadly trusted;
- project build files cannot directly declare dependencies using Loom's synthetic `remapped.*` coordinates.

## Deterministic dependency manifest

The manifest is generated from the committed project lockfiles rather than live Gradle `Project` or `Configuration` objects.

This provides:

- deterministic ordering;
- timestamp-free output;
- configuration-cache compatibility;
- a direct relationship between the released dependency inventory and reviewed lockfiles;
- no additional dependency resolution side effects during manifest generation.

Plugin and build-tool artifacts remain represented in `gradle/verification-metadata.xml` even when they are not project lockfile entries.

## Final automated evidence

Final code head:

```text
4d00ff296819196bd12fd5e3f16fd93820b5cf9c
```

Successful workflows:

```text
VillAIgence CI #875
run 30631724664
SUCCESS

Java Pull Request CI with Gradle #413
run 30631724636
SUCCESS

VillAIgence GitHub Release #69
run 30631724672
SUCCESS

Supply-chain verification #29
run 30631724653
SUCCESS
```

The final supply-chain workflow ran with `contents: read` and verified:

- the Gradle wrapper;
- dependency checksums;
- dependency locks;
- common unit tests;
- Fabric build;
- NeoForge build;
- deterministic dependency manifest generation;
- no mutation of committed verification metadata or lockfiles.

## Finding status

**SEC-005 — Closed.**

Closure evidence:

- implementation and regression policy merged through PR #61;
- exact squash commit `4cf9aef2e5c31a5682a7cad8544219154330e056`;
- all four final automated workflows succeeded on the validated code head;
- the merged controls are build/release controls and do not require a game-server runtime smoke test;
- the merged default branch contains the dated evidence, controlled update procedure and canonical tracker entries.

H4 CI security coverage and H5 whole-tree legacy-tool closure are separate work packages and are not claimed complete by this record.
