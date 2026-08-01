# MCA 7.7.32 Selective Synchronization — S8c Mounted Archer Control

**VillAIgence base:** `75c2c0c77508571c010c0c5838c8a8b62ab6afaf`  
**Upstream target:** `7ba76165dcf8aa9aa79070bdbac141b65519b3e7`  
**Source commit:** `6987ad0a5329f68baf979e199a45f99ad070722c`

## Problem

A mounted MCA archer may have its active vanilla `moveControl` replaced by vehicle behavior. The existing archer task casts the current active controller to `ArcherMoveControl` and throws when it is no longer that type.

## Adaptation

The upstream fix stores a stable archer controller directly in `VillagerEntityMCA`. VillAIgence adopts the same invariant with a smaller diff:

- `MixinVillagerEntityMCA` captures the original `ArcherMoveControl` at constructor completion;
- the stable controller is exposed through `ArcherMoveControlOwner`;
- `MixinArcherMovementTask` redirects only the private helper's `Mob.getMoveControl()` call;
- mounted/vehicle control may remain active for vanilla movement while archer state uses the stable controller;
- construction fails immediately if an MCA villager was not initialized with the required archer controller.

The large entity and archer task classes are not replaced.

## TDD evidence

### RED

```text
head: aff25855595d96623bde74f0b571c74b273fa1a3
VillAIgence CI #1076 / 30701126861 — EXPECTED FAILURE
boundary: common:compileTestJava
reason: ArcherControlPolicy absent at test call sites
Java Pull Request CI #586 — SUCCESS
Repository security policy #357 — SUCCESS
```

### GREEN production head

```text
head: ae4b9bfa14f0e879c29a0ce4745c73830fb1af4c
VillAIgence CI #1081 / 30701301093              SUCCESS
Java Pull Request CI #591 / 30701301085       SUCCESS
Repository security policy #367 / 30701301087 SUCCESS
```

The GREEN gate executed:

```text
:common:test
:fabric:build
:neoforge:build
Fabric distributable-package verification
repository security policy
```

## Preserved behavior

No changes were made to:

- archer targeting and state selection;
- approach, kite, emergency flee, strafe or hold algorithms;
- navigation and pathfinding policies;
- bow/crossbow detection;
- AI providers, memory or persistence;
- dependencies, workflows or scripts.

## Accumulated installed-server acceptance

### Unmounted baseline

```text
1. Spawn or configure an MCA guard/archer with a bow.
2. Trigger combat at approach, kite and strafe ranges.
3. Confirm normal movement and no behavior regression.
4. Repeat with a crossbow.
```

### Mounted archer

```text
1. Mount the MCA archer on a horse or supported vehicle.
2. Trigger combat while mounted.
3. Confirm no IllegalStateException about ArcherMoveControl.
4. Confirm archer state can enter/leave emergency flee and strafe without crashing.
5. Dismount and confirm normal movement resumes.
6. Remount repeatedly and repeat with bow and crossbow.
```

### Interruption and restart

```text
1. Mount during an active archer state and dismount during another state.
2. Remove the attack target and verify task stop resets emergency state.
3. Restart with the NPC mounted if the vehicle relationship is persisted.
4. Confirm the stable controller is recaptured on entity construction and combat works.
5. Confirm no duplicate controller, mixin application or constructor errors.
```

## Current boundary

```text
implementation: COMPLETE
unit tests: PASS
Fabric build/package verification: PASS
NeoForge build: PASS
repository security policy: PASS
installed-server acceptance: PENDING cumulative release-candidate run
```
