# M11 Phase E — E7 gameplay interaction validation

## Status

PASS on 2026-08-06.

This document records exact-head evidence for `VAI-GAME-002`. It does not request a release version, create a tag or publish a release.

## Exact validated head

```text
a8842a4cbb0ed3b577652dae1556d0558e6c1147
```

## Mandatory workflows

| Gate | Run | Result |
| --- | ---: | --- |
| VillAIgence CI | 1681 / `31049057275` | PASS |
| Java Pull Request CI with Gradle | 1067 / `31049057426` | PASS |
| Repository security policy | 1267 / `31049057333` | PASS |
| VillAIgence GitHub Release dry-run | 293 / `31049057343` | PASS |

The release workflow ran in dry-run mode. No tag or GitHub release was created.

## Server GameTest result

The Fabric server executed thirteen required GameTests. All thirteen passed.

The new E7 fixtures exercise real server-owned terminal effects rather than only policy helpers.

### Special gifts

`GameplayInteractionGameTests` sends three registered test-only `SpecialCaseGift` items through the real `BreedableRelationship.giveGift` path.

The fixture proves:

- `PASS` invokes the special handler once and delegates to ordinary gift handling;
- `FAIL` invokes the special handler once and does not consume the item;
- `CONSUME` invokes the special handler once and consumes exactly one item;
- test items are registered during Fabric initialization, before registry freeze;
- the fixture uses a real server-side mock player rather than a client-only or unregistered item shortcut.

### Fishing

`FishingCatchGameTests` passes a deterministic catch through the production `FishingCatchApplication` terminal-effect seam used by `FishingTask`.

The fixture proves:

- the caught stack is transferred into the MCA inventory;
- the exact held fishing rod is damaged once;
- durability is applied through the NPC dominant equipment slot, including the production handedness contract;
- catch chance, timers and loot-table selection remain owned by `FishingTask` and are not replaced by the test seam.

### Mounted archer

`MountedArcherControlGameTests` mounts a real MCA villager, replaces the actual `Mob.moveControl` field with a test-only vehicle controller and invokes the production `ArcherMovementTask.stop` lifecycle method.

Without `MixinArcherMovementTask`, the task's private controller lookup would observe the replacement and throw because it is not an `ArcherMoveControl`. The accepted runtime instead redirects that lookup to the constructor-captured stable controller.

The fixture proves:

- the actual active controller field contains the vehicle replacement;
- the production archer task completes through the Mixin redirect;
- the archer task does not overwrite the vehicle-owned controller;
- the constructor-captured `ArcherMoveControl` remains stable across mount, dismount and remount;
- the mounted MCA NPC emits exactly one real arrow;
- the projectile owner remains the same MCA NPC.

The field bridge and its Mixin implementation exist only in the GameTest source set and do not enter the distributable JAR.

## Exact package and restart evidence

Release dry-run artifacts:

```text
production-server-acceptance-293
artifact id: 8947824238
digest: sha256:874df65cf1394c3cd6ddba3e832d1495271dc58a03a081ef48e2d770a5b7b473

persistence-recovery-293
artifact id: 8947916211
digest: sha256:862ac8af142da995ed267d67c827da3e1c870ca782328e72665561b03dbd095f

villaigence-fabric-package
artifact id: 8947950017
digest: sha256:769d76ce3c9ce1f9a9fe41968438996ec6efeebef6a1d626d5773f55cd06ef1b
```

The same exact head also passed:

- two isolated production JVM startups against one staged world;
- clean stop, save and restart verification;
- all six persistent-store SHA-256 invariants;
- the six-case destructive recovery matrix;
- real Simple Voice Chat Opus acceptance;
- Fabric and NeoForge builds;
- package smoke verification;
- production-accepted JAR versus packaged JAR byte-identity verification.

## Failure-driven fixture corrections

The fixture was not weakened to obtain a green pipeline. The development sequence exposed and corrected four test-boundary errors:

1. a generic GameTest player was incorrectly assumed to be a `ServerPlayer`;
2. test items were initially created after registry freeze;
3. a public test bridge was initially placed inside a Mixin-owned package;
4. the first mounted oracle asserted an unrelated public getter instead of invoking the actual `ArcherMovementTask` redirect.

The final test checks the production task boundary directly and keeps the stronger field, lifecycle and projectile assertions.

## Catalog decision

`VAI-GAME-002` is `AUTOMATED` at the `SERVER_GAMETEST` layer.

No separate manual gift, fishing or mounted-archer release procedure is required for the server-owned inventory, durability, catch, stable-controller and projectile invariants covered here. A future installed gameplay smoke may still detect visual animation, client rendering or input issues, but those are not substitutes for the deterministic server assertions.
