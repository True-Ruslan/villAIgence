# M11 Phase E — E8 navigation matrix validation

## Status

PASS on 2026-08-06.

This document records exact-head evidence for `VAI-NAV-004`. It does not request a release version, create a tag or publish a release.

## Exact validated head

```text
cc03ebe0b0ef6b97fc45aa298075b4b3debd75bb
```

## Mandatory workflows

| Gate | Run | Result |
| --- | ---: | --- |
| VillAIgence CI | 1690 / `31051349218` | PASS |
| Java Pull Request CI with Gradle | 1076 / `31051349108` | PASS |
| Repository security policy | 1285 / `31051349140` | PASS |
| VillAIgence GitHub Release dry-run | 302 / `31051349047` | PASS |

The release workflow ran in dry-run mode. The `github-release` job was intentionally skipped, so no tag or release was created.

## Server GameTest result

The exact head executed sixteen required Fabric server GameTests. All sixteen passed in both the pull-request build and release dry-run.

`VAI-NAV-004` combines four independent navigation/combat terminal-state families. Each fixture is bounded and uses production server classes rather than a copied pathfinding implementation.

## Obstacle reroute

`NavigationObstacleGameTests` builds a stone arena with a two-block-high wall covering the direct route.

The controlled mob uses the real `MCAGroundPathNavigation` and has no autonomous goals that can replace the assigned path.

The fixture proves:

- the direct lane is physically blocked by an intact wall;
- the production path visibly leaves the direct lane;
- the NPC finishes on the target side of the obstacle;
- navigation reaches its bounded vanilla terminal range and reports done;
- the obstacle remains intact throughout the route.

### Failure-driven oracle correction

The first obstacle fixture reached the far side of the wall and completed navigation, but failed because the test required the mob to finish within 1.5 blocks of the target center. The runtime stopped approximately 2.2 blocks away, which is consistent with the navigation terminal range.

The correction did not remove the reroute requirement. The final oracle requires all of the following simultaneously:

```text
navigation.isDone()
distance to target <= 2.5 blocks
NPC is beyond the wall
maximum lateral deviation >= 1.5 blocks
all wall blocks remain intact
```

This distinguishes a successful bounded reroute from a stuck, teleported or obstacle-destroying fixture.

## Ladder ascent and descent

`NavigationLadderGameTests` builds a five-block real ladder with a supporting wall and upper platform.

The fixture assigns an upper route followed by a lower route through the same production navigation instance and proves:

- a path to the upper ladder platform is created;
- `MCAGroundPathNavigation.isControllingClimbable()` is observed during ascent;
- the NPC reaches the bounded upper terminal state;
- a return path down the ladder is created;
- climbable movement control is observed during descent;
- the NPC reaches the bounded lower terminal state;
- every ladder block remains present.

The fixture does not simulate vertical displacement directly. Movement is produced by the production climbable-node evaluator and navigation tick path.

## Closed-door passage

`NavigationDoorGameTests` builds a one-block-wide corridor containing a real closed two-block oak door.

The controlled mob uses production `MCAGroundPathNavigation` with door opening enabled and a real `OpenDoorGoal` configured to close behind the mob.

The fixture proves:

- the initial door is closed and both halves are valid;
- a path through the closed door can be created;
- the real door block is observed in the open state;
- the NPC passes to the far side and reaches the bounded terminal state;
- the close-behind goal returns the door to the closed state;
- both door halves remain intact and correctly typed.

## Mount and ranged combat

The same exact head also reran `MountedArcherControlGameTests`, previously introduced for E7.

That fixture contributes the mount and ranged-combat terminal states required by `VAI-NAV-004`:

- a vehicle-owned active `MoveControl` replacement remains installed;
- the production `ArcherMovementTask` redirect uses the stable constructor-captured `ArcherMoveControl`;
- mount, dismount and remount do not corrupt controller ownership;
- the MCA NPC emits exactly one real arrow;
- projectile ownership remains the same NPC.

Gameplay inventory/durability evidence remains recorded separately in `VALIDATION_M11_PHASE_E_E7.md`.

## Exact package and restart evidence

Release dry-run artifacts:

```text
production-server-acceptance-302
artifact id: 8948724583
digest: sha256:85765c8e824abc0ffaeba45fd2200ae5bcfba47c1322972078a0f3b70f5cd7d5

persistence-recovery-302
artifact id: 8948813613
digest: sha256:2d4e1199a9ed3427442d1e4f4a77c6e967b08171058531d09b169527f6eaf26f

villaigence-fabric-package
artifact id: 8948848059
digest: sha256:b1c98203b29a6b6861aa7e0c8cd420b428f277f7097dcaa0faff71e1f9970e96
```

The exact head also passed:

- common and deterministic mock-provider tests;
- Fabric and NeoForge builds;
- two isolated production JVM startups against one staged world;
- clean stop, save and restart verification;
- all six persistent-store SHA-256 invariants;
- the six-case destructive recovery matrix;
- real Simple Voice Chat Opus acceptance;
- production package smoke verification;
- production-accepted JAR versus packaged JAR byte-identity verification.

## Failure behavior

The navigation fixtures fail when:

- no path can be created;
- the bounded terminal state is not reached before the GameTest timeout;
- the obstacle route does not visibly leave the blocked lane;
- an obstacle, ladder or door fixture is modified or destroyed;
- ladder climb control is never observed;
- the door never opens or does not close after passage;
- the mounted archer task uses the vehicle controller instead of the stable archer controller;
- no projectile is emitted or projectile ownership changes.

## Catalog decision

`VAI-NAV-004` is `AUTOMATED` at the mandatory `SERVER_GAMETEST` layer.

The matrix runs on every normal VillAIgence CI path and is repeated by exact release validation. It is stronger than the original planned nightly-only placement, so ladders, obstacle rerouting, door passage, mount controller ownership and ranged projectile terminal effects no longer require routine manual regression testing.

The separate `VAI-NAV-001` installed canary remains because CI-controlled entities do not fully replace observation of two ordinary MCA NPC brains in an operator world with the exact released JAR.
