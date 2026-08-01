# MCA 7.7.32 Selective Synchronization — Core Checkpoint

**Date:** 2026-08-01  
**VillAIgence branch:** `1.21.1`  
**Checkpoint head:** `f70e9491050f1f139e526f0904c5ea0695fd2294`  
**Upstream target:** `7ba76165dcf8aa9aa79070bdbac141b65519b3e7`

## Status

The first implementation train of the approved MCA 7.7.32 selective synchronization plan is complete.

```text
S1 tombstone data integrity            merged / automated PASS / live deferred
S2 UUID-preserving conversion          merged / automated PASS / live deferred
S3 HOME occupied-bed correctness       merged / automated PASS / live deferred
S4 water and collision navigation      merged / automated PASS / live deferred
S5 climbable navigation                merged / automated PASS / live deferred
S6 path scheduling and recovery        merged / automated PASS / live deferred
```

No whole-branch merge or sequential blind cherry-pick was performed.

## Merge sequence

```text
#72 → 0c776012a1b0cd58221536b09d73c2502379a737
#73 → 7db7ffaa1405ed0bc6a74a4c078c34951d7725ab
#74 → d3f68bb1986e0ea71330c04f5fedbf02448c9bb5
#75 → 846979a2f5aef6775ee3ae1471eab12b50246aab
#76 → e9943dae7ecae38aa66d25402c73f727269642be
#77 → f70e9491050f1f139e526f0904c5ea0695fd2294
```

## Resulting product behavior

- filled tombstone items preserve block-entity NPC data and read legacy items;
- villager/zombie conversion preserves the UUID before target registration;
- HOME claims reject occupied beds and avoid releasing an unchanged HOME ticket;
- water navigation supports tagged water with bounded surface scans;
- candidate nodes use actual NPC dimensions and exact partial-collision checks;
- fences, walls, gates and lanterns participate in special collision validation;
- redundant goal-based door handling is removed while brain/path handling remains;
- tagged climbables participate in the path graph;
- ladder ascent, descent and exit motion are direction-controlled;
- jumping is suppressed only during controlled climbable navigation;
- player following accounts for vertical separation;
- pathfinding start checks are distributed by entity ID;
- the existing deterministic progress watchdog remains active and independent per NPC.

## Preserved boundaries

```text
AI transport/security paths changed: no
Memory 2.0 truth rules changed: no
persistent JSON schemas changed: no
workflow action references changed: no
dependency versions changed: no
lockfiles changed: no
```

One investigated S4 implementation initially widened `GroundPathNavigation.getSurfaceY`, which changed the derived Fabric Minecraft coordinate and conflicted with the checked-in dependency lock. That approach was removed. The final implementation uses the existing protected `getTempMobPos` hook, retaining the original lock identity.

## Validation strategy change

The operator cannot execute a separate installed-server scenario for every package. Therefore:

- each package retains mandatory RED/GREEN tests, both loader builds, package verification and repository security checks;
- live acceptance is not silently waived;
- live scenarios are accumulated into one exact-artifact S1–S6 test;
- no live-validation or release-promotion claim is made before that cumulative run.

Canonical cumulative procedure:

```text
docs/livingworld/VALIDATION_UPSTREAM_S1_S6_CUMULATIVE.md
```

## Next synchronization group

The next packages remain intentionally separate from the completed core train:

```text
S7 mourning and graveyard AI
S8 focused fishing/gameplay fixes
S9 operator-authored lore foundation
S10 Context Editor UI and networking
S11 generated personality background — deferred to 0.3
S12 Destiny and modded-village compatibility
S13 Skin Library
S14 EMF/rendering compatibility
```

Recommended immediate next package: **S7 mourning and graveyard AI**, after reconciling its final upstream state with the current GraveyardManager and LivingWorld event/memory architecture.

## Acceptance boundary

```text
core synchronization implementation: COMPLETE
core package automated verification: PASS
cumulative installed-server verification: PENDING
subsequent optional/product synchronization groups: NOT YET IMPLEMENTED
```
