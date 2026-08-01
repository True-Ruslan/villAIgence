# Upstream S3 HOME POI Validation

**Implementation date:** 2026-08-01  
**Package:** S3 — occupied-bed HOME POI correctness  
**Pull request:** #74  
**VillAIgence base:** `7db7ffaa1405ed0bc6a74a4c078c34951d7725ab`  
**Upstream source:** `0691c82d27e9255fafc7c0300190b23a4f667793`  
**Upstream audit target:** `7ba76165dcf8aa9aa79070bdbac141b65519b3e7`

## Defect

The previous `Residency.setHome` flow first searched for a HOME POI and only later attempted to take it. It did not validate the live bed block state, so an occupied bed could be selected and multiple villagers could crowd around it. Reassigning HOME also released the previous ticket before the new claim lifecycle was fully resolved.

## Implemented behavior

```text
candidate HOME POI
→ block is tagged as a bed
→ block exposes BedBlock.OCCUPIED
→ occupied == false
→ PoiManager.take performs selection and claim atomically
```

After a successful claim:

```text
same previous dimension and position
→ do not release the previous HOME ticket

different previous HOME
→ release the previous HOME ticket

then
→ erase old HOME memory
→ store dimension-aware GlobalPos
→ set FORCED_HOME
→ continue existing village/home seeking behavior
```

On failure, `FORCED_HOME` is erased and the existing localized failure messages are preserved.

No LivingWorld persistent JSON schema, AI provider path, security policy, workflow or dependency is changed.

## TDD evidence

### Canonical RED

```text
head: 6ba7aba05d8479b0ae5373366aac3c92b5a44564
VillAIgence CI #1008 / 30696221469
result: expected FAILURE
boundary: common:compileTestJava
reason: ResidencyBedClaimPolicy was absent at eight test call sites
```

### GREEN

```text
head: 4455d5ad22df2deef07476aa3bf2867ee3c862b2
VillAIgence CI #1010 / 30696350986              SUCCESS
Java Pull Request CI #528 / 30696351014       SUCCESS
Repository security policy #224 / 30696351002 SUCCESS
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

`ResidencyBedClaimPolicyTest` proves:

1. an unoccupied tagged bed is claimable;
2. an occupied bed is rejected;
3. a non-bed is rejected;
4. a state without the occupied property is rejected;
5. reclaiming the same HOME does not release its POI ticket;
6. changing position releases the previous HOME;
7. changing dimension releases the previous HOME;
8. an absent previous HOME requires no release.

Both loader builds compile the actual `BlockState`, `PoiManager.take` and `GlobalPos` adapter.

## Deferred cumulative server acceptance

Per operator decision, the world-level S3 test is accumulated with S1–S6.

Required cumulative S3 segment:

```text
place two valid unoccupied beds and one occupied bed
→ assign NPC A near bed A
→ assign NPC B near bed B
→ verify neither selects the occupied bed
→ verify A and B do not share one HOME
→ reassign A to its existing HOME
→ verify POI ticket remains valid
→ restart
→ verify both HOME memories and movement targets remain distinct
→ break/occupy one bed and verify controlled reassignment/failure behavior
```

## Acceptance boundary

```text
repository implementation: PASS
automated claim/release policy tests: PASS
Fabric build/package verification: PASS
NeoForge compile compatibility: PASS
repository security policy: PASS
isolated live S3 validation: intentionally deferred
cumulative S1–S6 server validation: PENDING
release promotion based on live HOME evidence: NOT YET CLAIMED
```
