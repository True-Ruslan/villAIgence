# VillAIgence 0.1.19 Startup Hotfix Validation

## Status

```text
date: 2026-08-01
release: 0.1.19+1.21.1
release tag: 0.1.19+1.21.1
release commit: 5a6765223b8f3e3c52715fb506d65bc0dafe9fa7
repository implementation: PASS
automated build/package/refmap validation: PASS
official release asset verification: PASS
installed server/client acceptance: PENDING
S1-S10c cumulative gameplay acceptance: PENDING
```

`0.1.19+1.21.1` is the corrective replacement for the blocked `0.1.18+1.21.1` candidate. It restores Fabric startup packaging and the intended controlled-climb Mixin target without changing world persistence schemas, AI provider behavior, Memory 2.0, Semantic Memory or operator-lore semantics.

## 0.1.18 incident

The `0.1.18+1.21.1` server failed before world load:

```text
MixinMob.mca$suppressJumpDuringControlledClimb
could not find target method_6100
No refMap loaded
```

The distributable JAR declared `mca.refmap.json` in `mca.mixins.json`, but the refmap file was absent. The controlled-climb injector also targeted `setJumping` from `@Mixin(Mob.class)`, although intermediary `method_6100` belongs to `LivingEntity`.

Operator-provided rollback evidence:

```text
0.1.18 removed from mods
server restored to 0.1.16+1.21.1
server started
Monitor restored
persistent hashes unchanged
rollback archive:
  backups/pre-villaigence-0.1.18-20260801-224500/server-state.tar.gz
marker:
  V0118_BLOCKED_SAFE_ROLLBACK_VERIFIED
```

No client or gameplay acceptance was attempted after the startup failure. Lore editor, Chat/STT/TTS, navigation, mourning, gifts, fishing and mounted archer remained untested for the synchronized S1-S10c train.

## Root causes

1. Fabric Loom legacy Mixin annotation processing was not enabled, so no distributable refmap was generated.
2. `mca$suppressJumpDuringControlledClimb` was placed in `MixinMob`, while `setJumping` is owned by `LivingEntity`.
3. Enabling the real Mixin annotation processor exposed MCA-owned targets that needed explicit `remap = false` declarations.
4. One inherited `getMoveControl` shadow was invalid for annotation-processor mapping and was replaced with a direct inherited call.
5. Newly active annotation-processor configurations required exact dependency locks and SHA-256 verification metadata.
6. Loom's legacy Mixin annotation processor is incompatible with the configured Gradle configuration cache; configuration cache is disabled while ordinary build caching remains enabled.

## Implemented hotfix

### Fabric refmap generation

The Fabric build now configures:

```groovy
loom {
    mixin {
        useLegacyMixinAp = true
        defaultRefmapName = "mca.refmap.json"
    }
}
```

### Correct target ownership

The controlled-climb jump suppression was removed from `MixinMob` and moved to `MixinLivingEntity`.

The behavior policy itself is unchanged:

```text
normal jump request + no controlled climb -> jump allowed
normal jump request + navigation-controlled climb -> jump suppressed
```

### Permanent package contract

`:fabric:check` now opens the remapped distributable JAR and requires all of the following:

```text
mca.mixins.json exists
mca.mixins.json declares mca.refmap.json
mca.refmap.json exists and parses
MixinLivingEntity.setJumping maps to:
  Lnet/minecraft/class_1309;method_6100(Z)V
MixinMob has no setJumping mapping
```

This makes the original `0.1.18` packaging defect a permanent CI failure rather than a server-startup discovery.

### Supply-chain state

Exact locks and SHA-256 metadata were added for the Fabric Mixin annotation-processor classpaths, including the datagen processor configuration. No dependency version was loosened or dynamically selected.

## TDD evidence

Canonical RED:

```text
head: 1a822373fe04b7bb869b746b104039089370d030
Java Pull Request CI #657 / 30716352577: expected FAILURE
reason: distributable Fabric JAR missing mca.refmap.json
Repository security policy #513: SUCCESS
```

Final hotfix PR:

```text
PR: #92
final PR head: f94b348c794dc184204ebb3d0cc39dac6def5771
merge commit: 5a6765223b8f3e3c52715fb506d65bc0dafe9fa7
```

Final exact-head GREEN:

```text
VillAIgence CI #1208 / 30718459423: SUCCESS
Java Pull Request CI #681 / 30718459458: SUCCESS
Supply-chain verification #57 / 30718459434: SUCCESS
Repository security policy #559 / 30718459430: SUCCESS
VillAIgence GitHub Release dry-run #102 / 30718459442: SUCCESS
```

Validated automatically:

```text
common unit tests
Fabric build
NeoForge build
strict dependency locks
strict dependency verification
repository security policy
remapped distributable package
mca.mixins.json/refmap declaration
embedded refmap mapping contents
release packaging smoke checks
```

## Official 0.1.19 release identity

```text
tag: 0.1.19+1.21.1
commit: 5a6765223b8f3e3c52715fb506d65bc0dafe9fa7
published: 2026-08-01T21:25:38Z
file: villaigence-fabric-0.1.19+1.21.1.jar
size: 11602846 bytes
SHA-256: 662447696b94b58aa6344084be36f0ab016122c591ba50610c5de99f3e3633a3
embedded mca.refmap.json SHA-256:
  10905bbb371043aeced8eb2ee5e9d6231f5dbabd86e675a89ad7c6d78a6714cc
```

Published assets:

```text
villaigence-fabric-0.1.19+1.21.1.jar
villaigence-fabric-0.1.19+1.21.1.jar.sha256
villaigence-dependencies-0.1.19+1.21.1.txt
```

The official release asset was downloaded again after publication and independently verified:

```text
tag points to expected canonical commit: PASS
canonical branch still points to expected commit: PASS
exactly one release JAR: PASS
checksum asset matches downloaded JAR: PASS
mca.mixins.json declares mca.refmap.json: PASS
mca.refmap.json present: PASS
MixinLivingEntity.setJumping mapping exact: PASS
MixinMob.setJumping mapping absent: PASS
```

## Preserved boundaries

```text
operator-lore.json schema changed: no
memory.json/memory2.json schema changed: no
semantic-memory.json schema changed: no
relationships/events/voices schema changed: no
AI provider/parser/retry changed: no
STT/TTS changed: no
server authority changed: no
world migration required: no
```

`0.1.19` must not be described as live-proven until it starts on the backed-up server and the interrupted cumulative S1-S10c acceptance completes.

## Resume procedure

1. Keep the verified rollback archive and the working `0.1.16` JAR outside the active `mods` directory.
2. Download the official `villaigence-fabric-0.1.19+1.21.1.jar` and verify its SHA-256 exactly.
3. Replace the VillAIgence JAR on both server and client. Do not install original MCA Reborn alongside VillAIgence because the internal mod ID remains `mca`.
4. Start the server before launching gameplay acceptance.
5. Confirm that startup contains neither `No refMap loaded` nor the `MixinMob ... method_6100` target failure.
6. Confirm server `STARTED`, Monitor active, ports available and persistent hashes unchanged before client connection.
7. Continue the previously approved cumulative plan from the first client/UI step; do not skip the scenarios that were not reached under `0.1.18`.
8. After the full test, record exact JAR SHA, pre/post-restart hashes, Chat/STT/TTS results, all S1-S10c gameplay results and rollback-copy loading.

Suggested startup checkpoint marker:

```text
V0119_REFMAP_STARTUP_PASS
```

Suggested complete cumulative acceptance marker:

```text
V0119_S1_S10C_CUMULATIVE_ACCEPTANCE_PASS
```

## Next development boundary

No further feature development is required before this acceptance. Legacy `memory.json` migration and the next product phase remain deferred until the exact `0.1.19` JAR passes installed server/client acceptance.
