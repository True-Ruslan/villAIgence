# Upstream S4 Water and Collision Navigation Validation

**Implementation date:** 2026-08-01  
**Package:** S4 — water/collision navigation foundation  
**Pull request:** #75  
**VillAIgence base:** `d3f68bb1986e0ea71330c04f5fedbf02448c9bb5`  
**Upstream source state:** `b4c6381fd3e206b5c3c2bedff9b63f06186cea19`, `29d26d9db5e9cb9ad2e6134f680a7e7e50cdbbc8`, `7ba76165dcf8aa9aa79070bdbac141b65519b3e7`

## Imported final-state behavior

S4 consumes the final upstream navigation foundation rather than intermediate implementations.

```text
water-aware temporary navigation position
→ recognize all fluids tagged #minecraft:water
→ scan upward with a hard 16-layer bound
→ retain vanilla fallback height outside floating-water navigation

water start node
→ if floating inside tagged water, begin at the top water layer
→ otherwise retain vanilla start-node behavior

collision clearance
→ translate the NPC's actual bounding box to the candidate node
→ reject full collision blocks immediately
→ invoke exact noBlockCollision only when partial collision shapes exist
→ cache the candidate-node result
```

The special collision tag now includes:

```text
#minecraft:fence_gates
#minecraft:fences
#minecraft:walls
minecraft:lantern
minecraft:soul_lantern
```

The explicit MCA `OpenDoorGoal` is no longer registered. Existing brain door behavior and path evaluator door support remain active.

## VillAIgence adaptations

- The obsolete intermediate fixed `1×2` clearance box was not imported.
- The actual mob bounding box is retained, protecting adult/child and mutable-dimension semantics.
- Ladder/climbable graph and motion code are excluded for the separate S5 package.
- Water-aware positioning uses the already available `getTempMobPos` hook instead of widening `GroundPathNavigation.getSurfaceY`.
- Therefore no dependency lock, dependency verification metadata or transformed-Minecraft coordinate was changed.
- AI, Memory 2.0, relationships, voices and network-security code are untouched.

## TDD evidence

### Canonical RED

```text
head: da1265dcc97c96313594eacab3d2f02e85ce3698
VillAIgence CI #1013 / 30696697882
result: expected FAILURE
boundary: common:compileTestJava
reason: NavigationWaterSurfacePolicy and NavigationCollisionPolicy were absent
```

### Investigated intermediate build failure

```text
head: 9b409bf6e10669769df0742907ca06b6df815a66
NeoForge: SUCCESS
Fabric: FAILURE before source compilation
reason: added class-tweak changed the derived minecraft-merged coordinate,
        conflicting with the checked-in Fabric dependency lock
```

Root-cause correction:

```text
remove new getSurfaceY class-tweak
→ restore original transformed-Minecraft identity
→ implement equivalent bounded water positioning through protected getTempMobPos
→ leave fabric/gradle.lockfile unchanged
```

### GREEN

```text
head: 33b7df4a90e7cee4dd62bf0daf279f407918bfec
VillAIgence CI #1023 / 30697064255              SUCCESS
Java Pull Request CI #540 / 30697064250       SUCCESS
Repository security policy #250 / 30697064246 SUCCESS
```

The GREEN gate executes:

```text
:common:test
:fabric:build
:neoforge:build
Fabric distributable-package verification
repository security policy
```

## Automated regression coverage

`NavigationFoundationPolicyTest` proves:

1. water surface selection stops at the first non-water layer;
2. an all-water column is bounded and fails back to the start height;
3. a non-floating mob retains the vanilla fallback height;
4. exact integer AABB maxima do not include the next block;
5. fractional maxima include their containing block;
6. full-block collision rejects clearance immediately;
7. only non-empty partial shapes require the expensive exact shape fallback.

Both loader builds compile the actual Minecraft path navigation, node evaluator, collision tag and mixin registration.

## Deferred cumulative server acceptance

Per operator decision, S4 is accumulated with S1–S6.

Required cumulative S4 segment:

```text
vanilla water pool and tagged modded-water pool
→ command NPC across/through water and back to land
→ verify bounded progress and no submerged start-node loop

lantern, fence, wall and fence-gate obstacle lanes
→ verify no clipping through partial shapes
→ verify valid open route remains usable

adult and child NPC
→ run the same narrow-route scenarios
→ verify actual entity dimensions are respected

door route
→ verify brain/path door handling still works without duplicate OpenDoorGoal

restart
→ repeat representative water and partial-collision routes
→ verify no stuck loop, crash or path-memory corruption
```

## Acceptance boundary

```text
repository implementation: PASS
automated water/collision policy tests: PASS
Fabric build/package verification: PASS
NeoForge compile compatibility: PASS
repository security policy: PASS
isolated live S4 validation: intentionally deferred
cumulative S1–S6 server validation: PENDING
release promotion based on live navigation evidence: NOT YET CLAIMED
```
