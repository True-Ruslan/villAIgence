# MCA 7.7.32 Selective Synchronization — S8b Fishing Compatibility

**VillAIgence base:** `0d833659626f1a91d6e86c7e0cae0b2a7689b755`  
**Upstream target:** `7ba76165dcf8aa9aa79070bdbac141b65519b3e7`  
**Source commit:** `71cbb13f7a2cf406a86f15ea6a33a2dfd5911b8f`

## Implemented behavior

- an already held `FishingRodItem` is retained, including compatible modded subclasses;
- otherwise a rod is equipped from inventory;
- absence of any rod abandons the chore with the existing `chore.fishing.norod` message;
- loot is generated at catch time rather than cached at task start;
- loot origin is the center of the selected `targetWater` block;
- loot `TOOL` is the actual held rod stack;
- an empty loot result safely falls back to one cod;
- the selected catch is copied before insertion;
- durability damage applies to the actual dominant-hand stack and dominant equipment slot;
- existing water search, cast timing, catch chance, movement and stop cleanup remain unchanged.

## TDD evidence

### RED

```text
head: ff23e359bf786996d37e89b29723d5ef77349f15
VillAIgence CI #1071 / 30700550454 — EXPECTED FAILURE
boundary: common:compileTestJava
reason: FishingTaskPolicy absent at test call sites
Java Pull Request CI #582 — SUCCESS
Repository security policy #347 — SUCCESS
```

### GREEN production head

```text
head: cb37877e45d4cf4a32ad853a17a78fb9c686aac2
VillAIgence CI #1073 / 30700724032              SUCCESS
Java Pull Request CI #584 / 30700724037       SUCCESS
Repository security policy #351 / 30700724035 SUCCESS
```

The GREEN gate executed:

```text
:common:test
:fabric:build
:neoforge:build
Fabric distributable-package verification
repository security policy
```

## Scope boundary

Changed runtime files are limited to:

```text
FishingTask.java
FishingTaskPolicy.java
```

No chore framework, inventory utility, AI provider, LivingWorld persistence, navigation, dependency, workflow or script changes are included.

## Accumulated installed-server acceptance

### Vanilla rod

```text
1. Give an NPC a vanilla fishing rod in inventory and start the fishing chore.
2. Confirm the rod is equipped in the dominant hand.
3. Confirm the NPC finds water, approaches, casts and catches items.
4. Confirm durability decreases on the equipped rod.
5. Confirm each catch enters the NPC inventory once.
```

### Already held rod

```text
1. Start with a fishing rod already in the dominant hand.
2. Confirm the task does not replace it with another inventory rod.
3. Confirm that exact stack is supplied as the loot TOOL and receives durability damage.
```

### Modded rod / AquaCulture

```text
1. Install the supported mod and place a modded FishingRodItem subclass in inventory.
2. Confirm it is recognized and equipped.
3. Confirm modded fishing loot modifiers observe the actual tool stack.
4. Confirm no hard-coded vanilla rod is passed to the loot table.
```

### Loot origin

```text
1. Use a loot modifier whose result depends on biome/location or inspect a debug hook.
2. Confirm ORIGIN is the center of targetWater, not the villager's standing position.
```

### Empty loot and missing rod

```text
1. Use a controlled fishing loot table returning no items.
2. Confirm the NPC receives one cod and no list-index exception occurs.
3. Remove all rods during the chore.
4. Confirm the job is abandoned with the existing message and no invalid slot access occurs.
```

### Handedness and restart

```text
1. Test a left-dominant NPC if available.
2. Confirm the left/dominant stack is damaged rather than MAINHAND unconditionally.
3. Restart before and after a catch and confirm no persistent-state regression or duplicate catch.
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
