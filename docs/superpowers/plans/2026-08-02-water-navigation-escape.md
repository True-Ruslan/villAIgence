# Water Navigation Escape Fix Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restore reliable MCA villager escape from water by preserving vanilla `GroundPathNavigation.getTempMobPos()` behavior and applying water-tag-aware logic only at the surface-height hook.

**Architecture:** The installed `0.1.20+1.21.1` failure is traced to the S4 adaptation overriding the entire `getTempMobPos()` path-following/stuck-detection hook. Final upstream narrowed the change to `getSurfaceY()` specifically to avoid replacing that hook. VillAIgence will remove its `getTempMobPos()` override and add a narrow common Mixin that intercepts `GroundPathNavigation.getSurfaceY()` only for `MCAGroundPathNavigation`, avoiding a class-tweaker/dependency-lock change.

**Tech Stack:** Java 21, Minecraft 1.21.1 Mojang mappings, Sponge Mixin, JUnit 5, Fabric Loom, NeoForge, Gradle verification.

## Global Constraints

- Keep the internal mod ID `mca` and package root `net.conczin.mca` unchanged.
- Do not change AI provider, voice, memory, persistence, packet or operator-lore behavior.
- Do not add a class-tweaker change or regenerate transformed-Minecraft dependency identities.
- Preserve modded-water support through `FluidTags.WATER`.
- Preserve the existing bounded 16-layer surface scan.
- Keep ladder/climbable behavior unchanged.
- Require Fabric, NeoForge, distributable-package and repository-security gates.
- Treat installed water-escape validation as pending until an exact candidate JAR is tested.

---

### Task 1: Establish the RED architecture boundary

**Files:**
- Modify: `common/src/test/java/net/conczin/mca/entity/ai/navigation/NavigationFoundationPolicyTest.java`

**Interfaces:**
- Consumes: current `MCAGroundPathNavigation` declared methods.
- Produces: a regression test requiring the class to preserve vanilla `getTempMobPos()` and expose `mca$getWaterAwareSurfaceY()` for the Mixin hook.

- [ ] **Step 1: Add the failing reflection test**

```java
@Test
void waterAwareNavigationPreservesVanillaPathPositionHook() {
    Set<String> declaredMethods = Arrays.stream(MCAGroundPathNavigation.class.getDeclaredMethods())
            .map(Method::getName)
            .collect(Collectors.toSet());

    assertFalse(declaredMethods.contains("getTempMobPos"));
    assertTrue(declaredMethods.contains("mca$getWaterAwareSurfaceY"));
}
```

Add imports for `java.lang.reflect.Method`, `java.util.Arrays` and `java.util.stream.Collectors`.

- [ ] **Step 2: Run the focused test and record RED**

Run:

```bash
./gradlew :common:test --tests net.conczin.mca.entity.ai.navigation.NavigationFoundationPolicyTest
```

Expected: FAIL because `MCAGroundPathNavigation` still declares `getTempMobPos()` and does not declare `mca$getWaterAwareSurfaceY()`.

- [ ] **Step 3: Commit RED**

```bash
git add common/src/test/java/net/conczin/mca/entity/ai/navigation/NavigationFoundationPolicyTest.java
git commit -m "test: preserve vanilla water path position hook"
```

---

### Task 2: Replace the broad hook with a narrow surface-Y Mixin

**Files:**
- Modify: `common/src/main/java/net/conczin/mca/entity/ai/navigation/MCAGroundPathNavigation.java`
- Create: `common/src/main/java/net/conczin/mca/mixin/MixinGroundPathNavigation.java`
- Modify: `common/src/main/resources/mca.mixins.json`

**Interfaces:**
- Produces: `public int MCAGroundPathNavigation.mca$getWaterAwareSurfaceY()`.
- Produces: `MixinGroundPathNavigation` injection into `GroundPathNavigation.getSurfaceY()`.

- [ ] **Step 1: Remove the `getTempMobPos()` override**

Delete the `Vec3` import and replace:

```java
@Override
protected Vec3 getTempMobPos() {
    return new Vec3(this.mob.getX(), getWaterAwareSurfaceY(), this.mob.getZ());
}

private int getWaterAwareSurfaceY() {
```

with:

```java
public int mca$getWaterAwareSurfaceY() {
```

Keep the current `NavigationWaterSurfacePolicy.findSurfaceY(...)` call unchanged.

- [ ] **Step 2: Add the narrow Mixin**

```java
package net.conczin.mca.mixin;

import net.conczin.mca.entity.ai.navigation.MCAGroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GroundPathNavigation.class)
abstract class MixinGroundPathNavigation {
    @Inject(method = "getSurfaceY", at = @At("HEAD"), cancellable = true)
    private void mca$useWaterTagAwareSurface(CallbackInfoReturnable<Integer> cir) {
        if ((Object)this instanceof MCAGroundPathNavigation navigation) {
            cir.setReturnValue(navigation.mca$getWaterAwareSurfaceY());
        }
    }
}
```

- [ ] **Step 3: Register the Mixin**

Add `"MixinGroundPathNavigation"` to the common `mixins` array in `mca.mixins.json`.

- [ ] **Step 4: Run focused and common tests**

```bash
./gradlew :common:test --tests net.conczin.mca.entity.ai.navigation.NavigationFoundationPolicyTest
./gradlew :common:test
```

Expected: PASS.

- [ ] **Step 5: Commit GREEN implementation**

```bash
git add common/src/main/java/net/conczin/mca/entity/ai/navigation/MCAGroundPathNavigation.java \
        common/src/main/java/net/conczin/mca/mixin/MixinGroundPathNavigation.java \
        common/src/main/resources/mca.mixins.json
git commit -m "fix: preserve vanilla water path position hook"
```

---

### Task 3: Enforce the distributable package contract

**Files:**
- Modify: `fabric/build.gradle`

**Interfaces:**
- Consumes: remapped Fabric JAR, `mca.mixins.json`, generated `mca.refmap.json` and class constant pools.
- Produces: permanent build failure if the broad hook returns or the narrow surface hook is missing.

- [ ] **Step 1: Extend `verifyFabricRefmap`**

Require:

```groovy
commonMixins.contains('MixinGroundPathNavigation')
```

Require generated refmap mappings for:

```text
net/conczin/mca/mixin/MixinGroundPathNavigation
getSurfaceY
```

Require the remapped JAR to contain:

```text
net/conczin/mca/mixin/MixinGroundPathNavigation.class
net/conczin/mca/entity/ai/navigation/MCAGroundPathNavigation.class
```

Inspect the navigation class constant pool and require:

```text
mca$getWaterAwareSurfaceY present
getTempMobPos absent
```

- [ ] **Step 2: Run the package gate**

```bash
./gradlew :fabric:check
```

Expected: PASS with valid refmap and class contract.

- [ ] **Step 3: Run all supported build gates**

```bash
./gradlew :common:test :fabric:build :neoforge:build
```

Expected: PASS.

- [ ] **Step 4: Commit package contract**

```bash
git add fabric/build.gradle
git commit -m "test: enforce water surface navigation package contract"
```

---

### Task 4: Record exact validation and live-retest boundary

**Files:**
- Create: `docs/livingworld/VALIDATION_0.1.20_WATER_NAVIGATION_FIX.md`

**Interfaces:**
- Consumes: RED/GREEN commits and exact CI runs.
- Produces: canonical evidence and focused installed acceptance procedure.

- [ ] **Step 1: Record root cause and implementation**

Document:

```text
installed release: 0.1.20+1.21.1
observed failure: NPC became trapped in water, drowned and died
root cause: broad getTempMobPos replacement diverged from final upstream getSurfaceY-only contract
fix: conditional GroundPathNavigation.getSurfaceY Mixin for MCAGroundPathNavigation
class-tweaker/dependency identity change: none
```

- [ ] **Step 2: Record exact RED and GREEN evidence**

Include commit SHAs and workflow run IDs for common tests, Fabric, NeoForge, package verification and repository security policy.

- [ ] **Step 3: Define focused installed acceptance**

Require an exact candidate JAR and test:

```text
shallow one-block water exit
2–4 block deep water exit
water beside full blocks, fences, walls and gates
ordinary and modded FluidTags.WATER
adult and child MCA villagers
idle, follow and panic walk targets
minimum 10-minute observation without air depletion, stuck loop or death
restart and repeat
```

Do not claim live PASS until this procedure succeeds.

- [ ] **Step 4: Commit validation document**

```bash
git add docs/livingworld/VALIDATION_0.1.20_WATER_NAVIGATION_FIX.md
git commit -m "docs: record water navigation fix validation"
```

---

### Task 5: Final verification and PR

**Files:**
- Review all files changed by Tasks 1–4.

- [ ] **Step 1: Run final verification**

```bash
./gradlew :common:test :fabric:check :neoforge:build
```

Expected: all tasks PASS with no dependency-lock or verification metadata mutation.

- [ ] **Step 2: Review scope**

Confirm no changes to provider, voice, memory, persistence, packets, operator lore, graves, packaging version or Chat timeout behavior.

- [ ] **Step 3: Open the PR**

Use title:

```text
fix: preserve vanilla water path position hook
```

The PR must state that repository validation can pass while installed water-escape acceptance remains pending on a new exact candidate JAR.
