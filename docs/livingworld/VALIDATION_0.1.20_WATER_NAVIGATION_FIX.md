# VillAIgence 0.1.20 Water Navigation Escape Fix Validation

## Status

```text
date: 2026-08-02
installed source release: 0.1.20+1.21.1
installed acceptance result: PARTIAL PASS — RELEASE DEFECTS FOUND
observed defect: MCA NPC became trapped in water, drowned and died
repository implementation: PASS
automated unit/build/package validation: PASS
installed water-escape acceptance: PENDING
release promotion: PENDING
```

This package fixes only the release-blocking water navigation defect discovered during installed acceptance of `0.1.20+1.21.1`.

It does not claim that the defect is live-fixed until a new exact candidate JAR passes the focused installed procedure in this document.

---

## Installed failure

During cumulative acceptance of the exact official `0.1.20+1.21.1` JAR, ordinary navigation and ladders passed, but an MCA NPC entered water, became trapped, exhausted its air and died.

The failure therefore was not a generic server-startup or entity-construction problem. It was specific to the water path-following/escape behavior exercised by the installed artifact.

Canonical installed evidence:

```text
docs/livingworld/VALIDATION_0.1.20_INSTALLED_ACCEPTANCE.md
```

---

## Root cause

The earlier S4 synchronization needed water detection to use `FluidTags.WATER`, including modded fluids that belong to the Minecraft water tag.

VillAIgence implemented that behavior by overriding the complete path-position hook:

```java
@Override
protected Vec3 getTempMobPos() {
    return new Vec3(this.mob.getX(), getWaterAwareSurfaceY(), this.mob.getZ());
}
```

Final MCA upstream did not retain that architecture. It narrowed the customization to `GroundPathNavigation.getSurfaceY()` specifically so water-tag-aware height calculation would not replace `getTempMobPos()`.

The installed drowning failure proves that the broad VillAIgence adaptation was not behaviorally equivalent to preserving the vanilla position hook. `getTempMobPos()` participates in path following and stuck detection; replacing it changed more than the intended water surface calculation.

---

## Implemented fix

### Preserve vanilla path position behavior

`MCAGroundPathNavigation` no longer declares or overrides `getTempMobPos()`.

All ordinary path-following and stuck-detection calls therefore resolve to the inherited Minecraft implementation.

### Retain bounded water-tag surface detection

The existing policy remains available through:

```java
public int mca$getWaterAwareSurfaceY()
```

It preserves:

- `FluidTags.WATER` rather than literal vanilla-water block checks;
- the existing `canFloat()` boundary;
- the existing maximum scan of 16 vertical layers;
- the vanilla fallback height when the entity is not floating in water.

### Narrow surface-Y interception

A new common Mixin intercepts only:

```text
GroundPathNavigation.getSurfaceY()I
```

It replaces the return value only when the navigation instance is `MCAGroundPathNavigation`:

```java
if ((Object)this instanceof MCAGroundPathNavigation navigation) {
    cir.setReturnValue(navigation.mca$getWaterAwareSurfaceY());
}
```

Other `GroundPathNavigation` subclasses remain unchanged.

### No class-tweaker expansion

The repository does not add a new class-tweaker entry for `getSurfaceY()`.

This avoids changing transformed-Minecraft dependency identities, locks or verification metadata for a one-method runtime adaptation.

---

## TDD evidence

### Discarded harness attempt

The first test attempted to reflectively load `MCAGroundPathNavigation` from the loader-independent common test runtime. It failed with `ClassNotFoundException`, not the intended architecture assertion, and is not treated as canonical RED evidence.

```text
head: f701f91c3ee5deda08f7c97fa420fba24e260aac
VillAIgence CI #1241 / 30750619290: FAILURE
reason: invalid test harness boundary; Minecraft class unavailable in common unit runtime
```

The test was corrected before production implementation.

### Canonical RED

```text
head: b37938ed010afd801aef9d798328a09976e2983b
VillAIgence CI #1243 / 30750735661: EXPECTED FAILURE
Java Pull Request CI #702 / 30750735668: SUCCESS
Repository security policy #594 / 30750735646: SUCCESS
```

Expected failing test:

```text
NavigationFoundationPolicyTest
waterAwareNavigationPreservesVanillaPathPositionHook
```

The loader-independent source contract required:

```text
MCAGroundPathNavigation must not declare protected Vec3 getTempMobPos(...)
MCAGroundPathNavigation must declare public int mca$getWaterAwareSurfaceY(...)
```

### Initial implementation GREEN

```text
head: d0b5a2c795145829c37fcacd554d6d3c002b23a2
VillAIgence CI #1251 / 30751007545: SUCCESS
Java Pull Request CI #706 / 30751007541: SUCCESS
Repository security policy #602 / 30751007549: SUCCESS
```

The initial GREEN established:

- common unit tests pass;
- Fabric build passes;
- NeoForge build passes;
- the existing distributable package gate passes;
- security policy passes.

### Final package-contract GREEN

```text
head: 33c459a073debb337bb91053ae0cf4c91b19d8eb
VillAIgence CI #1253 / 30751161193: SUCCESS
Java Pull Request CI #707 / 30751161173: SUCCESS
Supply-chain verification #62 / 30751161164: SUCCESS
Repository security policy #604 / 30751161202: SUCCESS
```

VillAIgence CI explicitly completed:

```text
unit tests and supported loader builds: SUCCESS
distributable VillAIgence Fabric package verification: SUCCESS
```

---

## Permanent distributable-JAR contract

`:fabric:check` now requires the exact remapped JAR to satisfy all of the following:

```text
mca.mixins.json registers MixinGroundPathNavigation
mca.refmap.json contains MixinGroundPathNavigation.getSurfaceY mapping
the mapped target returns int
MixinGroundPathNavigation.class is packaged
MCAGroundPathNavigation.class is packaged
MCAGroundPathNavigation declares mca$getWaterAwareSurfaceY()I
MCAGroundPathNavigation does not declare getTempMobPos(...)
```

The last two conditions are inspected from the remapped class structure using ASM, not by ambiguous constant-pool string matching.

This converts accidental restoration of the broad water position override into a build/package failure.

---

## Preserved boundaries

```text
AI provider/parser/retry changed: no
STT/TTS/Voice Chat changed: no
Chat timeout or latency policy changed: no
memory schemas changed: no
operator-lore schema or packets changed: no
relationship/events/voice persistence changed: no
grave or Silk Touch behavior changed: no
embedded release version changed: no
ladder/climb policy changed: no
path collision policy changed: no
dependency locks changed: no
dependency verification metadata changed: no
world migration required: no
```

The only runtime behavior change is the ownership of the water path-position hook:

```text
before: MCA overrides complete getTempMobPos behavior
after: vanilla getTempMobPos retained; MCA customizes only water surface Y
```

---

## Focused installed acceptance

A repository GREEN result is not sufficient to close the installed defect.

Build one exact candidate JAR from the accepted commit and record its SHA-256 before testing.

### Startup gate

1. Back up the world and all `<world>/livingworld/` files.
2. Install the exact candidate on the server first.
3. Start without connecting a client.
4. Confirm server startup, world load, monitor, TCP `25565` and UDP `24454`.
5. Confirm no Mixin application or injection errors involving `GroundPathNavigation`.

### Required water scenarios

Test at minimum:

```text
1. one-block-deep water with an immediately reachable full-block shore
2. two-to-four-block-deep water with a reachable shore
3. water adjacent to full blocks
4. water adjacent to fences
5. water adjacent to walls
6. water adjacent to fence gates
7. ordinary minecraft:water
8. any available modded fluid included in FluidTags.WATER
9. adult MCA villager
10. child MCA villager
11. idle/random movement target
12. follow movement target
13. panic or threat-driven movement target
```

For each scenario confirm:

```text
NPC reaches or remains at the water surface
NPC continues horizontal movement toward a valid exit
path does not oscillate indefinitely
stuck detection does not freeze the entity below the surface
air supply does not continuously deplete
NPC exits reachable water without operator teleport
NPC remains alive
```

### Observation duration

Observe at least one representative deep-water scenario continuously for ten minutes or repeat enough entries/exits to exceed the previous drowning interval.

No drowning, persistent submersion or non-progress loop is acceptable.

### Regression surface

After water scenarios, repeat:

```text
ordinary dry-land navigation
stairs/slabs and collision-sensitive blocks
ladder ascent and descent
mounted archer movement
restart and repeat one water-exit scenario
```

### Persistent safety

Before and after restart compare the same persistent files used by the installed `0.1.20` acceptance. The navigation fix must not modify their schemas or produce corruption.

---

## Acceptance decision

Repository status:

```text
implementation: COMPLETE
automated validation: PASS
package contract: PASS
installed water-escape acceptance: PENDING
```

A future candidate may receive a focused water-navigation PASS only after the exact JAR completes the procedure above.

The separate filled-grave Silk Touch defect remains release-blocking and is not addressed by this package.
