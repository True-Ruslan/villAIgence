# VillAIgence 0.1.20 Tombstone Startup Hotfix Validation

## Status

```text
date: 2026-08-02
release: 0.1.20+1.21.1
release tag: 0.1.20+1.21.1
release commit: 3244bc98fb6d2f8e1b7a3c7715ca0c35970a58f9
repository implementation: PASS
automated build/package validation: PASS
official release asset verification: PASS
installed server startup acceptance: PENDING
installed client/gameplay acceptance: PENDING
S1-S10c cumulative acceptance: PENDING
```

`0.1.20+1.21.1` is the corrective replacement for the startup-failed `0.1.19+1.21.1` release. It removes the compiler-fragile tombstone resurrection Mixin and preserves the same mourning cleanup through a direct call in project-owned code.

Neither `0.1.18` nor `0.1.19` is suitable for installed-server acceptance. The exact `0.1.20` JAR must pass the server startup gate before client or gameplay scenarios resume.

## 0.1.19 installed-server incident

The official `0.1.19+1.21.1` JAR was verified before installation:

```text
file: villaigence-fabric-0.1.19+1.21.1.jar
SHA-256: 662447696b94b58aa6344084be36f0ab016122c591ba50610c5de99f3e3633a3
mca.refmap.json: present and valid
old MixinMob / method_6100 / No refMap loaded failure: absent
```

Server startup still failed before world load:

```text
MixinTombstoneData
mca$clearMourningAfterResurrection
failed injection check, (0/1) succeeded
Scanned 0 target(s)
Using refmap mca.refmap.json
```

Operator-provided safe rollback evidence:

```text
0.1.19 unsuitable for server acceptance
world did not load or change
server restored to stable 0.1.16+1.21.1
persistent hashes matched
TCP 25565: available
UDP 24454: available
Monitor: active
backup:
  backups/pre-villaigence-0.1.19-20260802-121156
marker:
  V0119_STARTUP_FAIL_SAFE_ROLLBACK_PASS
```

No client or gameplay testing was attempted. Operator lore, Chat/STT/TTS, navigation, graves, mourning, gifts, fishing and mounted archer therefore remained pending.

## Root cause

The exact remapped `0.1.19` artifact was inspected at bytecode level.

The source expression was inside the lambda passed to `Optional.ifPresent(...)`. Java compilation produced this shape:

```text
TombstoneBlock$Data.tick()
  -> Optional.ifPresent(Consumer)

TombstoneBlock$Data.lambda$tick$1(Entity)
  -> CompassionateEntity.getRelationships()
```

`MixinTombstoneData` declared:

```text
@Redirect(method = "tick", target = CompassionateEntity.getRelationships)
```

The target invocation did not exist in `tick()` itself. It existed only in compiler-generated `lambda$tick$1(...)`, so Mixin scanned zero matching instructions and enforced `defaultRequire = 1`, aborting startup.

Redirecting a synthetic lambda name would remain dependent on compiler output and source reshaping. Because `TombstoneBlock` belongs to the project, the durable fix is a normal source-level call rather than another Mixin target.

## Implemented hotfix

### Direct resurrection cleanup

After possible zombie-villager conversion and immediately before the existing `deceased=false` relationship update, `TombstoneBlock.Data` now performs:

```java
if (entity instanceof VillagerEntityMCA villager) {
    MourningMemoryLifecycle.clearAfterResurrection(villager);
}
```

The existing order remains:

```text
create restored entity
→ restore living state
→ apply possible zombie-villager conversion
→ clear MCA mourning memory
→ clear deceased family flag
→ apply infection state
→ add entity if not already spawned
```

### Removed compiler-fragile Mixin

The hotfix removes both:

```text
common/src/main/java/net/conczin/mca/mixin/MixinTombstoneData.java
MixinTombstoneData registration from mca.mixins.json
```

The class is also absent from the distributable JAR.

### Permanent package contract

`:fabric:check` now verifies the remapped distributable JAR and requires:

```text
mca.mixins.json declares mca.refmap.json
mca.refmap.json exists and parses
MixinLivingEntity.setJumping maps exactly to:
  Lnet/minecraft/class_1309;method_6100(Z)V
MixinMob has no setJumping mapping
MixinTombstoneData is not registered
MixinTombstoneData.class is not packaged
TombstoneBlock$Data.class directly references:
  MourningMemoryLifecycle
  clearAfterResurrection
```

This converts both startup failures discovered in `0.1.18` and `0.1.19` into permanent build/package failures.

## TDD evidence

Canonical RED:

```text
head: 4ea65958915f4d77157a972c0502e68f6116ab14
Java Pull Request CI #689 / 30741557467: expected FAILURE
NeoForge build: SUCCESS
Fabric failure task: :fabric:verifyFabricRefmap
reason:
  mca.mixins.json must not inject into
  TombstoneBlock.Data synthetic resurrection lambda
Repository security policy #571: SUCCESS
```

Final implementation PR:

```text
PR: #95
final PR head: 3430a6073d3593629f6a7123ac67755fec1a1f45
squash merge: 3244bc98fb6d2f8e1b7a3c7715ca0c35970a58f9
changed files: 4
additions: 28
deletions: 33
```

Final exact-head GREEN:

```text
VillAIgence CI #1226 / 30741736016: SUCCESS
Java Pull Request CI #692 / 30741736008: SUCCESS
Supply-chain verification #61 / 30741736004: SUCCESS
Repository security policy #577 / 30741736006: SUCCESS
```

## Release-candidate verification

An isolated operation branch built the exact candidate from canonical commit:

```text
commit: 3244bc98fb6d2f8e1b7a3c7715ca0c35970a58f9
workflow: Isolated 0.1.20 release dry-run #1
run: 30742146200
result: SUCCESS
artifact ID: 8831650376
```

The artifact was downloaded outside the workflow and independently inspected:

```text
file: villaigence-fabric-0.1.20+1.21.1.jar
size: 11601999 bytes
SHA-256:
  3d00e5b7e5f5ace0f10b9455038d45f1aaf55546b634673f1cbae7d8c677a1ac
embedded mca.refmap.json SHA-256:
  10905bbb371043aeced8eb2ee5e9d6231f5dbabd86e675a89ad7c6d78a6714cc
mca.refmap.json declaration: PASS
MixinLivingEntity.setJumping mapping: PASS
MixinMob.setJumping mapping absent: PASS
MixinTombstoneData registration absent: PASS
MixinTombstoneData.class absent: PASS
direct MourningMemoryLifecycle marker present: PASS
direct clearAfterResurrection marker present: PASS
```

## Official release identity

```text
tag: 0.1.20+1.21.1
commit: 3244bc98fb6d2f8e1b7a3c7715ca0c35970a58f9
published: 2026-08-02T09:44:42Z
file: villaigence-fabric-0.1.20+1.21.1.jar
size: 11601999 bytes
SHA-256:
  3d00e5b7e5f5ace0f10b9455038d45f1aaf55546b634673f1cbae7d8c677a1ac
embedded mca.refmap.json SHA-256:
  10905bbb371043aeced8eb2ee5e9d6231f5dbabd86e675a89ad7c6d78a6714cc
```

Published assets:

```text
villaigence-fabric-0.1.20+1.21.1.jar
villaigence-fabric-0.1.20+1.21.1.jar.sha256
villaigence-dependencies-0.1.20+1.21.1.txt
```

Release operation evidence:

```text
ops PR: #96
final diff: empty
merged: no
exact candidate publisher: 30742364197 — SUCCESS
official asset verifier: 30742285727 — SUCCESS
official verification artifact ID: 8831707819
```

The official assets were downloaded again after publication and verified independently:

```text
tag points to exact canonical commit: PASS
canonical branch matched release commit at publication: PASS
exactly one official JAR: PASS
checksum asset matches official JAR: PASS
dependency manifest present: PASS
refmap and climb mappings: PASS
MixinTombstoneData absent: PASS
direct tombstone mourning cleanup markers present: PASS
```

## Preserved boundaries

```text
operator-lore.json schema changed: no
memory.json/memory2.json schema changed: no
semantic-memory.json schema changed: no
relationships/events/voices schema changed: no
AI provider/parser/retry changed: no
STT/TTS changed: no
network packet schema changed: no
server authority changed: no
world migration required: no
```

Gameplay behavior changes only in implementation mechanism: the intended mourning cleanup after resurrection is now called directly instead of through a startup-failing Mixin redirect.

## Server startup resume procedure

1. Preserve the verified rollback backup and working `0.1.16` JAR outside the active `mods` directory.
2. Download the official `villaigence-fabric-0.1.20+1.21.1.jar`.
3. Verify SHA-256 exactly:

```text
3d00e5b7e5f5ace0f10b9455038d45f1aaf55546b634673f1cbae7d8c677a1ac
```

4. Replace only the server VillAIgence JAR. Do not install original MCA Reborn alongside VillAIgence because the internal mod ID remains `mca`.
5. Start the server before connecting a client.
6. Confirm that startup contains none of:

```text
No refMap loaded
MixinMob ... method_6100
MixinTombstoneData
mca$clearMourningAfterResurrection failed injection check
```

7. Confirm:

```text
server STARTED
world loaded
Monitor active
TCP 25565 available
UDP 24454 available
persistent hashes unchanged before client interaction
```

8. Only after this gate passes, install the same exact JAR on the client and resume the cumulative S1-S10c gameplay plan.
9. If startup fails, stop immediately, restore `0.1.16`, preserve logs and verify persistent hashes before any client connection.

Suggested startup checkpoint marker:

```text
V0120_STARTUP_GATE_PASS
```

Suggested safe rollback marker if another startup blocker appears:

```text
V0120_STARTUP_FAIL_SAFE_ROLLBACK_PASS
```

Suggested complete cumulative acceptance marker:

```text
V0120_S1_S10C_CUMULATIVE_ACCEPTANCE_PASS
```

## Next development boundary

No new feature development is required before this startup and cumulative acceptance.

The order remains:

```text
0.1.20 exact server startup gate
→ same exact JAR on client
→ cumulative S1-S10c gameplay acceptance
→ Chat/STT/TTS regression checks
→ restart and persistent-file hash verification
→ rollback-copy load verification
→ record live evidence
```

Legacy `memory.json` migration and the next product phase remain deferred until the exact `0.1.20` JAR passes installed server/client acceptance.
