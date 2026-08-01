# Upstream S6 Pathfinding Scheduling Validation

**Implementation date:** 2026-08-01  
**Package:** S6 — staggered pathfinding checks with retained progress recovery  
**Pull request:** #77  
**VillAIgence base:** `e9943dae7ecae38aa66d25402c73f727269642be`  
**Upstream source:** `a652878c5dbfac0ebc4bf46fe6c1b9417e28f86f`  
**Upstream audit target:** `7ba76165dcf8aa9aa79070bdbac141b65519b3e7`

## Imported scheduling behavior

Expensive pathfinding start-condition checks retain a seven-invocation cadence but no longer begin in the same scheduler phase for every NPC.

```text
first cooldown per NPC
→ floorMod(entityId, 7)

cooldown > 0
→ decrement
→ skip expensive start check

cooldown == 0
→ run start check
→ reset to 6
```

Seven consecutive entity IDs cover all offsets `0..6`, distributing initial work instead of concentrating it in one tick.

## VillAIgence adaptation

Upstream stores one cooldown field in the task. VillAIgence already stores per-Mob progress trackers in the same behavior, so S6 uses a `WeakHashMap<Mob, Integer>` for scheduling state.

This provides:

- independent cooldown phase per NPC;
- deterministic initialization from entity ID;
- no strong references preventing entity cleanup;
- removal of both progress and scheduling state when `WALK_TARGET` disappears.

The existing recovery pipeline remains authoritative and unchanged:

```text
same WALK_TARGET
→ track meaningful 3D movement
→ tolerate short pauses
→ after 8 seconds without 0.5-block progress
→ request one navigation path recomputation
→ reset the stationary window
```

The following are also unchanged:

- WALK_TARGET intent is retained during recovery;
- teleporting remains configuration-gated;
- minimum teleport distance remains enforced;
- destination path type, floor blacklist and entity collision checks remain enforced;
- S4 water/collision and S5 climbable behavior remain intact.

No AI provider, Memory 2.0, persistence schema, security workflow, dependency or lockfile is changed.

## TDD evidence

### Canonical RED

```text
head: 1a4c1fa934f67d150d4339812c8c24b20ecb52bb
VillAIgence CI #1035 / 30697905856
result: expected FAILURE
boundary: common:compileTestJava
reason: PathfindingSchedulePolicy was absent at test call sites
```

### GREEN

```text
head: 1737cd034aea3280f4a441f94068c29980c4ac4d
VillAIgence CI #1037 / 30698032145              SUCCESS
Java Pull Request CI #552 / 30698032146       SUCCESS
Repository security policy #278 / 30698032148 SUCCESS
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

`PathfindingSchedulePolicyTest` proves:

1. deterministic initial offsets for positive, wrapped and negative entity IDs;
2. seven consecutive IDs cover every scheduling phase;
3. positive cooldowns skip and decrement;
4. zero cooldown runs and resets to interval minus one;
5. every initialized entity runs exactly once per interval;
6. invalid intervals fail explicitly.

Existing `PathfindingProgressTrackerTest` remains active and proves:

- bounded stationary recovery;
- meaningful movement resets the stationary counter;
- changing WALK_TARGET starts a fresh recovery window.

## Deferred cumulative server acceptance

Per operator decision, S6 completes the automated core synchronization train and its live test is combined with S1–S5.

Required cumulative S6 segment:

```text
spawn or select multiple NPCs with distinct entity IDs
→ issue simultaneous or near-simultaneous movement targets
→ observe path start/recalculation activity across multiple ticks
→ verify movement still begins for every NPC

force one NPC into a recoverable blocked route
→ retain WALK_TARGET
→ verify bounded recompute after sustained lack of progress
→ verify no tight recompute loop
→ verify other NPCs remain independent

with teleport disabled
→ confirm recovery does not teleport

with teleport enabled in controlled test
→ confirm minimum distance and safe-destination checks remain active

restart
→ repeat representative multi-NPC and stuck-route scenarios
→ verify no burst loop, memory leak symptom, crash or path corruption
```

## Acceptance boundary

```text
repository implementation: PASS
automated scheduling and watchdog tests: PASS
Fabric build/package verification: PASS
NeoForge compile compatibility: PASS
repository security policy: PASS
isolated live S6 validation: intentionally deferred
cumulative S1–S6 server validation: PENDING
release promotion based on live core-sync evidence: NOT YET CLAIMED
```
