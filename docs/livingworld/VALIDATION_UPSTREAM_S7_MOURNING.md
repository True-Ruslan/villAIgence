# MCA 7.7.32 Selective Synchronization — S7 Mourning

**Package:** S7  
**VillAIgence base:** `4bc4cc48117e1bcef16770758c2fc9a5959b36e6`  
**Upstream target:** `7ba76165dcf8aa9aa79070bdbac141b65519b3e7`  
**Source commit:** `0f092bd76d26274ac8d45cf8aa9b3421089cd562`  
**Strategy:** extract only server-side graveyard/mourning behavior from a mixed upstream commit

## Adopted behavior

- persistent `MOURNING_SITE` and dimension-aware `MOURNING_POSITION` memories;
- selection of a filled, non-resurrecting tombstone;
- collision-safe standing positions adjacent to the grave;
- deterministic preference order:
  1. approach side rather than the opposite side;
  2. smaller vertical offset;
  3. fewer existing reservations;
  4. shorter distance from the mourner;
  5. UUID-derived stable tie break;
- independent standing-position reservations for multiple mourners;
- controlled walk, flower, look and three-dialogue mourning lifecycle;
- bounded 1,200-tick retry when a valid candidate remains;
- completion when the assigned grave vanished or no periodic candidate exists;
- cleanup of stale mourning/path memories after resurrection.

## Explicit exclusions

The source commit also contains unrelated editor, genetics, eye-texture and rendering changes. None of those files or behaviors are imported by S7.

S7 does not change:

- `OpenAIChatAI` transport, parsing or retry behavior;
- Memory 2.0, semantic memory, relationships persistence or voice identity;
- provider endpoint/security policy;
- dependency locks or verification metadata;
- workflows or scripts;
- S4 water/collision, S5 climbable or S6 scheduling behavior.

## TDD evidence

### RED

```text
head: 67d1de9a87663ec7cc768b29bc22d12cd80ab686
VillAIgence CI #1042 / 30699086058 — EXPECTED FAILURE
boundary: common:compileTestJava
reason: MourningPolicy absent at all test call sites
Java Pull Request CI #555 — SUCCESS
Repository security policy #289 — SUCCESS
```

The failure was limited to the new test source and established the intended policy boundary before production code.

### GREEN production head

```text
head: b2a75ed842752298d8dda74e30806440061ff58b
VillAIgence CI #1055 / 30699480460              SUCCESS
Java Pull Request CI #568 / 30699480463       SUCCESS
Repository security policy #315 / 30699480462 SUCCESS
```

The GREEN gate executed:

```text
:common:test
:fabric:build
:neoforge:build
Fabric distributable-package verification
repository security policy
```

## Accumulated installed-server acceptance

Run together with the final synchronized release candidate rather than as an isolated package blocker.

### Assigned grave

```text
1. Create a filled tombstone for a relative of NPC A.
2. Confirm NPC A enters GRIEVE and remembers the tombstone as MOURNING_SITE.
3. Confirm MOURNING_POSITION is in the same dimension and adjacent to the grave.
4. Confirm NPC A reaches that exact position rather than standing inside/on top of the tombstone.
5. Confirm a flower is equipped only during mourning.
6. Confirm three grieving dialogue emissions and normal schedule restoration.
```

### Multiple mourners

```text
1. Trigger mourning for several related NPCs around one grave.
2. Confirm they choose collision-safe positions.
3. Confirm reservation counts distribute them instead of forcing one occupied position.
4. Confirm each NPC completes or retries independently.
5. Confirm no synchronized path-recompute loop.
```

### Obstruction and retry

```text
1. Block all valid positions around a still-filled grave.
2. Confirm mourning does not complete falsely.
3. Confirm MOURNING_SITE and MOURNING_POSITION are cleared at sequence cleanup.
4. Confirm LAST_GRIEVE schedules the next attempt after approximately 1,200 ticks, not seven days.
5. Remove the obstruction and confirm a later attempt can complete.
```

### Grave removal

```text
1. Remove or empty the assigned grave before arrival.
2. Confirm stale site/position memories are removed.
3. Confirm the villager returns to its normal schedule without a permanent retry loop.
```

### Resurrection

```text
1. Resurrect an MCA villager from a tombstone while mourning memories/path state exist in its saved brain.
2. Confirm LAST_GRIEVE, MOURNING_SITE, MOURNING_POSITION, PATH, WALK_TARGET, LOOK_TARGET and CANT_REACH_WALK_TARGET_SINCE are absent on the restored villager.
3. Confirm normal HOME, identity, Memory 2.0, relationships and voice data remain intact.
```

### Restart durability

```text
1. Restart while an NPC has an assigned mourning site/position.
2. Confirm codecs restore both memories with the correct dimension.
3. Complete or invalidate the target and confirm cleanup remains deterministic.
4. Confirm no missing-memory registration, serialization, duplicate UUID or path-loop errors.
```

## Current boundary

```text
S7 implementation: COMPLETE
policy unit tests: PASS
Fabric build/package verification: PASS
NeoForge compile compatibility: PASS
repository security policy: PASS
mixed client/editor/rendering code imported: NO
installed-server acceptance: PENDING cumulative release-candidate run
release promotion based on S7 live evidence: NOT YET CLAIMED
```
