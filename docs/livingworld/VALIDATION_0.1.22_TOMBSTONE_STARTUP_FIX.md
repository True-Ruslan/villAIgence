# VillAIgence 0.1.22 Tombstone Startup Fix Validation

## Status

```text
date: 2026-08-02
failed installed release: 0.1.21+1.21.1
failed installed marker: V0121_STARTUP_GATE_FAIL
rollback marker: V0121_STARTUP_BLOCKER_ROLLBACK_PASS
repository fix: PASS
automated validation: PASS
candidate version: 0.1.22+1.21.1
installed startup acceptance: PENDING
water/grave/gameplay acceptance: BLOCKED UNTIL STARTUP PASS
```

## Installed failure

The exact official `0.1.21+1.21.1` artifact passed filename, checksum and embedded-version verification, but failed before world startup:

```text
MixinTombstoneBlock
InvalidInjectionException:
could not find any targets matching 'getDrops'
```

The server never reached the gameplay test boundary. Water navigation, filled-grave round-trip and cumulative gameplay checks were correctly not executed.

The operator restored `0.1.20` and verified:

```text
all six persistent hashes matched
server reached STARTED
TCP 25565 available
UDP 24454 / Voice Chat available
monitor restored
problematic JAR and startup log preserved
backup: /home/server/minecraft/backups/pre-villaigence-0.1.21-20260802-183243.tar.gz
```

## Root cause

`MixinTombstoneBlock` injected into:

```java
@Inject(method = "getDrops", at = @At("RETURN"), cancellable = true, remap = false)
```

`TombstoneBlock.getDrops(...)` overrides a Minecraft method. Its development name is remapped in the production Fabric namespace. Setting `remap = false` forced Mixin to search for the literal development name `getDrops` at runtime, where no such target existed.

The repository's previous tests validated the pure drop policy and source registration, but did not validate that the production runtime could resolve this injection target.

## Implemented correction

The runtime injection has been removed entirely.

`TombstoneBlock.getDrops(...)` now calls `TombstoneDropPolicy.ensurePreservedDrop(...)` directly in owned source code. The existing safety behavior remains:

```text
filled tombstone with existing block drop -> reuse that item
filled tombstone without block drop -> append exactly one fallback item
stored entity data -> serialize through Data.writeToStack
unrelated drops -> preserve order
empty tombstone -> retain original loot result
```

Removed surfaces:

```text
MixinTombstoneBlock.java
MixinTombstoneBlock registration
MixinTombstoneBlock refmap dependency
```

No tombstone item schema, block-entity schema, resurrection path, graveyard persistence, navigation, AI, memory, packet or provider behavior changed.

## TDD evidence

### RED

```text
head: eba7dff90d4ab548ad2723d2aadd08f92c9d0b7c
VillAIgence CI #1294 / 30754840794: EXPECTED FAILURE
Java Pull Request CI #723 / 30754840800: SUCCESS
Repository security policy #645 / 30754840806: SUCCESS
```

Expected failing test:

```text
TombstoneDropPolicyTest > runtimeWiringUsesOwnedTombstoneSourceWithoutMixinInjection() FAILED
```

### GREEN before this document

```text
head: f34c6655e24b7db053e80c608e87d1a4d9bd4cbb
VillAIgence CI #1306 / 30755211623: SUCCESS
Java Pull Request CI #729 / 30755211652: SUCCESS
Repository security policy #657 / 30755211614: SUCCESS
Supply-chain verification #70 / 30755211633: SUCCESS
```

VillAIgence CI explicitly completed:

```text
exact release identity contract: SUCCESS
unit tests and supported loader builds: SUCCESS
Fabric distributable package verification: SUCCESS
```

## New package-level regression boundary

The remapped Fabric JAR is rejected unless all conditions hold:

```text
mca.mixins.json does not register MixinTombstoneBlock
MixinTombstoneBlock.class is absent
mca.refmap.json contains no MixinTombstoneBlock mapping
TombstoneBlock.class exists
TombstoneBlock.class references TombstoneDropPolicy
TombstoneBlock.class references ensurePreservedDrop
```

This closes the exact gap that allowed `0.1.21` to pass build/package validation but fail during installed startup.

## Exact installed test order for 0.1.22

### Phase 0 — Artifact and backup

1. Stop the server cleanly.
2. Create a new full backup; do not reuse the `0.1.21` backup as the only copy.
3. Record SHA-256 values for:
   - `memory.json`;
   - `memory2.json`;
   - `operator-lore.json`;
   - `relationships.json`;
   - `semantic-memory.json`;
   - `voices.json`.
4. Download only the official `villaigence-fabric-0.1.22+1.21.1.jar`.
5. Verify the published checksum and embedded version.
6. Remove `0.1.20` and `0.1.21` from the mods directory; keep exactly one VillAIgence/MCA JAR.

### Phase 1 — Startup-only gate

Start the server without connecting a client.

Required:

```text
no MixinTombstoneBlock line at all
no InvalidInjectionException
no Mixin apply/refmap error
no MixinGroundPathNavigation error
no MixinTombstoneData error
world load completes
server reaches STARTED
TCP 25565 available
UDP 24454 / Voice Chat initialized
monitor active
no persistent corruption/recovery warning
```

Then stop cleanly and compare the six persistent hashes.

Expected marker:

```text
V0122_STARTUP_GATE_PASS
```

On any startup failure:

```text
stop immediately
preserve full startup log
restore 0.1.20 and the new backup
verify all six persistent hashes
use V0122_STARTUP_GATE_FAIL and V0122_STARTUP_BLOCKER_ROLLBACK_PASS
```

### Phase 2 — Focused regressions after startup PASS only

1. Water escape with two NPCs.
2. Filled grave broken with the same Silk Touch pickaxe.
3. Grave item pickup, placement and NPC/inventory restoration.
4. Repeat grave round-trip after restart.
5. Empty-grave and ordinary-tool controls.

Expected markers:

```text
V0122_WATER_ESCAPE_PASS
V0122_FILLED_GRAVE_ROUND_TRIP_PASS
```

### Phase 3 — Cumulative smoke and persistence

```text
Text -> DIALOGUE
Voice -> STT -> Chat -> TTS -> DIALOGUE
NPC A/B memory isolation
Operator Lore Editor open/save
ordinary and ladder navigation
gifts
fishing
mounted archer
clean restart
six persistent JSON files valid
ports and monitor restored
```

Record every Chat duration. A response near the previously observed 272 seconds remains a separate performance defect.

Final marker only after all phases pass:

```text
V0122_CORRECTIVE_CUMULATIVE_ACCEPTANCE_PASS
```
