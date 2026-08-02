# VillAIgence 0.1.22 Tombstone Startup Fix Validation

## Status

```text
date: 2026-08-03
failed installed release: 0.1.21+1.21.1
failed installed marker: V0121_STARTUP_GATE_FAIL
rollback marker: V0121_STARTUP_BLOCKER_ROLLBACK_PASS
repository fix: PASS
automated package validation: PASS
candidate version: 0.1.22+1.21.1
installed startup acceptance: PASS
installed two-NPC water escape: PASS
installed filled-grave round trip: PASS
installed grave round trip after restart: PASS
cumulative gameplay acceptance: PENDING
```

Current installed markers:

```text
V0122_STARTUP_GATE_PASS
V0122_WATER_ESCAPE_PASS
V0122_FILLED_GRAVE_ROUND_TRIP_PASS
V0122_CORRECTIVE_CUMULATIVE_ACCEPTANCE_PENDING
```

## Installed 0.1.22 partial acceptance

The operator confirmed the following scenarios against the exact installed `0.1.22+1.21.1` candidate:

```text
two independent NPCs leave water successfully
filled grave survives break with the same Silk Touch pickaxe
portable grave item can be picked up and placed again
restored NPC preserves name, UUID and inventory
grave round trip succeeds again after server restart
```

This is sufficient to promote startup, water escape and the focused filled-grave correction to installed `PASS`.

It is not sufficient for cumulative acceptance. The following release-level scenarios remain unreported for `0.1.22`:

```text
Text -> DIALOGUE
Voice -> STT -> Chat -> TTS -> DIALOGUE
NPC A/B memory isolation
Operator Lore Editor open/save
ordinary and ladder navigation
gifts
fishing
mounted archer
final clean restart with six persistent JSON files validated
ports and monitor after the final cumulative restart
```

The M11 automated acceptance work complements this installed evidence with deterministic server GameTests for registry boot, tombstone item serialization, evaluated Silk Touch drops, empty-grave negative control, independent water lanes and post-water land navigation. GameTest success does not replace a production-JAR startup/restart test; that remains a separate Phase B boundary.

## Installed 0.1.21 failure

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

## Corrective TDD evidence

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

### GREEN

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

## Package-level regression boundary

The remapped Fabric JAR is rejected unless all conditions hold:

```text
mca.mixins.json does not register MixinTombstoneBlock
MixinTombstoneBlock.class is absent
mca.refmap.json contains no MixinTombstoneBlock mapping
TombstoneBlock.class exists
TombstoneBlock.class references TombstoneDropPolicy
TombstoneBlock.class references ensurePreservedDrop
production fabric.mod.json retains id mca
production fabric.mod.json has no fabric-gametest entrypoint
production JAR contains no net/conczin/mca/gametest classes
```

This closes the exact gap that allowed `0.1.21` to pass build/package validation but fail during installed startup, while also preventing test-only acceptance code from leaking into release artifacts.

## Remaining cumulative installed test order

The already accepted water and grave scenarios do not need destructive repetition unless a later change touches those boundaries. Complete the remaining smoke set on `0.1.22`:

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

Final marker only after every remaining cumulative scenario passes:

```text
V0122_CORRECTIVE_CUMULATIVE_ACCEPTANCE_PASS
```
