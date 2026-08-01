# MCA 7.7.32 Selective Synchronization — S1–S6 Cumulative Acceptance

**Checkpoint date:** 2026-08-01  
**VillAIgence branch:** `1.21.1`  
**Core synchronization head:** `f70e9491050f1f139e526f0904c5ea0695fd2294`  
**Upstream audit target:** `7ba76165dcf8aa9aa79070bdbac141b65519b3e7`  
**Strategy:** selective final-state subsystem ports; no whole-branch merge

## Completed core package chain

| Package | Scope | PR | Merge commit | Automated result |
|---|---|---:|---|---|
| S1 | tombstone item/block-entity data integrity | #72 | `0c776012a1b0cd58221536b09d73c2502379a737` | PASS |
| S2 | UUID-preserving villager ↔ zombie conversion | #73 | `7db7ffaa1405ed0bc6a74a4c078c34951d7725ab` | PASS |
| S3 | occupied-bed HOME POI correctness | #74 | `d3f68bb1986e0ea71330c04f5fedbf02448c9bb5` | PASS |
| S4 | tagged-water and exact collision navigation | #75 | `846979a2f5aef6775ee3ae1471eab12b50246aab` | PASS |
| S5 | stable tagged-climbable navigation | #76 | `e9943dae7ecae38aa66d25402c73f727269642be` | PASS |
| S6 | staggered pathfinding checks with retained watchdog | #77 | `f70e9491050f1f139e526f0904c5ea0695fd2294` | PASS |

## Exact automated evidence

```text
S1 final head: 6703e81e6d5e727f619457146a2d62ac50e2634c
  VillAIgence CI #999 / 30693384627              SUCCESS
  Java Pull Request CI #519 / 30693384610       SUCCESS
  Repository security policy #202 / 30693384623 SUCCESS

S2 final head: 25fa3faf2826772d6c1fa83c607632f46293a5fb
  VillAIgence CI #1006 / 30696029247              SUCCESS
  Java Pull Request CI #525 / 30696029222       SUCCESS
  Repository security policy #216 / 30696029220 SUCCESS

S3 final head: 263f46a03235b3bc80a2cad45f85cb0ae11900f5
  VillAIgence CI #1011 / 30696482650              SUCCESS
  Java Pull Request CI #529 / 30696482680       SUCCESS
  Repository security policy #226 / 30696482668 SUCCESS

S4 final head: 9a82f39430c0209532b3558d48c00bab902516fd
  VillAIgence CI #1024 / 30697185162              SUCCESS
  Java Pull Request CI #541 / 30697185159       SUCCESS
  Repository security policy #252 / 30697185170 SUCCESS

S5 final head: 1cb7578304e2dc093fbec160fba8610845f2180f
  VillAIgence CI #1033 / 30697732922              SUCCESS
  Java Pull Request CI #549 / 30697732930       SUCCESS
  Repository security policy #270 / 30697732925 SUCCESS

S6 final head: c422e3677af0cbea41cca828ee711fd04a55732f
  VillAIgence CI #1038 / 30698177753              SUCCESS
  Java Pull Request CI #553 / 30698177738       SUCCESS
  Repository security policy #280 / 30698177724 SUCCESS
```

Every final package gate executed:

```text
:common:test
:fabric:build
:neoforge:build
Fabric distributable-package verification
repository security policy
```

## Preserved VillAIgence architecture

The selective ports do not replace or weaken:

- normalized AI-provider endpoint and credential policy;
- redirect and response-size controls;
- Memory 2.0 ownership, provenance, idempotency or truth rules;
- deterministic semantic forgetting and decay;
- world-local persistence layout;
- text/voice parity;
- existing six UUID-keyed identity domains;
- dependency locks, dependency verification and immutable workflow references.

Persistent identity domains remain:

```text
memory.json
memory2.json
semantic-memory.json
events.json
relationships.json
voices.json
```

## One cumulative server acceptance scenario

Run on a backed-up copied world using an exact artifact containing merge `f70e9491050f1f139e526f0904c5ea0695fd2294` or a later commit that does not alter S1–S6 behavior.

### Phase 0 — baseline capture

```text
1. Start the server and record exact JAR SHA-256.
2. Confirm server startup, TCP 25565, UDP 24454 and monitor health.
3. Select NPC A and NPC B with distinct UUIDs.
4. Seed or confirm recognizable data for NPC A in all six persistent domains.
5. Record hashes of all six persistence files.
6. Record NPC A UUID, name, gender, HOME and another NBT-backed property.
7. Confirm Text, STT, Chat and TTS production smoke checks.
```

### Phase 1 — S1 tombstone round trip

```text
1. Create or obtain a filled tombstone with a known NPC.
2. Break it and retain the dropped tombstone item.
3. Place it at a different valid position.
4. Verify the same embedded identity and displayed data.
5. Repeat break → item → place once more.
6. Verify no empty item, duplicate grave entry or serialization error.
```

### Phase 2 — S2 identity conversion

```text
1. Infect NPC A.
2. Verify exactly one live replacement entity and the original UUID.
3. Verify all six UUID-keyed domains remain linked.
4. Restart the server.
5. Verify the zombie identity and all six domain links again.
6. Cure NPC A.
7. Verify the restored villager uses the same UUID.
8. Restart again and recheck all links.
9. Run Text and Voice dialogue with the restored NPC.
```

### Phase 3 — S3 HOME POI

```text
1. Prepare two valid unoccupied beds and one occupied bed.
2. Assign NPC A and NPC B to separate available beds.
3. Verify neither selects the occupied bed.
4. Verify the NPCs do not share one HOME.
5. Reassign NPC A to its current HOME and verify its ticket remains valid.
6. Restart and verify both dimension-aware HOME memories remain distinct.
7. Occupy or remove one bed and verify controlled failure/reassignment.
```

### Phase 4 — S4 water and collision navigation

```text
1. Route adult and child NPCs through vanilla water and a tagged modded-water pool.
2. Verify bounded surface/start-node behavior and successful return to land.
3. Route them through obstacle lanes containing lanterns, fences, walls and fence gates.
4. Verify no clipping through partial shapes and no false rejection of a valid open route.
5. Route through a door and verify brain/path door handling still works.
```

### Phase 5 — S5 climbable navigation

```text
1. Run adult and child NPCs up and down a vanilla ladder.
2. Verify stable top and bottom exits.
3. Reverse direction repeatedly near the middle and both exits.
4. Verify no oscillation, jump fighting or permanent stuck state.
5. Repeat on another block included in #minecraft:climbable.
6. Follow a player across a vertical route and verify vertical distance is respected.
7. Verify guard combat still takes precedence over following.
```

### Phase 6 — S6 load distribution and recovery

```text
1. Give several NPCs simultaneous or near-simultaneous movement targets.
2. Verify every NPC begins movement and path checks are not synchronized into one repeating burst.
3. Force one NPC into a recoverable blocked route.
4. Verify WALK_TARGET remains and one bounded recompute occurs after sustained lack of progress.
5. Verify no tight recompute or teleport loop.
6. Repeat once with teleport disabled.
7. In a controlled copy, repeat with teleport enabled and verify distance and safe-destination rules.
```

### Phase 7 — final restart and durability

```text
1. Stop the server cleanly.
2. Compare the six persistence files against expected semantic changes.
3. Restart the server.
4. Re-run representative tombstone, conversion identity, HOME, water, collision, ladder and path-recovery checks.
5. Confirm all expected UUIDs and sourceEventIds remain stable.
6. Confirm no duplicate UUID, duplicate grave, corruption recovery, serialization exception, path loop or OutOfMemory condition.
7. Confirm Text/STT/Chat/TTS and Voice Chat still succeed.
8. Confirm ports and monitor remain healthy.
```

## Required evidence bundle

```text
exact JAR SHA-256
server startup and final restart logs
six persistence hashes before and after
NPC UUID snapshots before infection, after infection, after cure and after restart
tombstone identity evidence after two item round trips
HOME assignments and occupied-bed rejection evidence
water/collision/climbable route observations
multi-NPC scheduling and stuck-recovery observations
Text/STT/Chat/TTS timings and outcomes
absence of duplicate UUID/grave/persistence/navigation errors
```

## Current acceptance boundary

```text
S1–S6 merged into 1.21.1: YES
all package unit tests: PASS
Fabric build/package verification: PASS
NeoForge compile compatibility: PASS
repository security policy: PASS
protected AI/security/memory/dependency paths preserved: YES
cumulative installed-server acceptance: PENDING
release promotion based on S1–S6 live evidence: NOT YET CLAIMED
```

Package-specific evidence:

```text
docs/livingworld/VALIDATION_UPSTREAM_S1_TOMBSTONE.md
docs/livingworld/VALIDATION_UPSTREAM_S2_CONVERSION.md
docs/livingworld/VALIDATION_UPSTREAM_S3_HOME_POI.md
docs/livingworld/VALIDATION_UPSTREAM_S4_NAVIGATION.md
docs/livingworld/VALIDATION_UPSTREAM_S5_CLIMBABLE.md
docs/livingworld/VALIDATION_UPSTREAM_S6_PATH_SCHEDULING.md
```
