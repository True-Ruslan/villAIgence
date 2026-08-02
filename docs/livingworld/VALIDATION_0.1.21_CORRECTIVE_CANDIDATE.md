# VillAIgence 0.1.21 Corrective Candidate Validation

## Status

```text
date: 2026-08-02
candidate version: 0.1.21+1.21.1
repository implementation: PASS
automated validation: PASS
official release asset: PENDING
installed server/client acceptance: PENDING
release promotion beyond candidate: PENDING
```

This candidate combines the release-blocking corrections discovered during installed acceptance of `0.1.20+1.21.1`:

```text
water navigation: merged PR #99
filled grave portable drop: merged PR #100
exact embedded release identity: PR #101
```

No installed PASS is claimed until the exact official `0.1.21+1.21.1` JAR is downloaded, hash-verified and tested.

---

## Candidate scope

### Water navigation

The broad `MCAGroundPathNavigation.getTempMobPos()` replacement was removed. MCA water-tag behavior now applies only through the narrow `GroundPathNavigation.getSurfaceY()` hook, preserving inherited Minecraft path-position and stuck-detection behavior.

Expected installed result:

```text
an MCA NPC entering water can select and follow an escape path
ordinary ground navigation remains functional
no drowning caused by a navigation deadlock
```

### Filled grave drop preservation

A filled tombstone now guarantees one portable tombstone item even when the evaluated loot list omits the block item. Existing tombstone drops are reused without duplication; stored entity data is serialized through the existing block-entity item format.

Expected installed result:

```text
breaking a filled grave with Silk Touch produces one tombstone item
placing that item restores the same deceased NPC
stored inventory and identity survive the break/place round trip
empty grave behavior remains unchanged
```

### Exact release identity

Gradle accepts a validated explicit:

```text
-Prelease_version=<mod_version>+<minecraft_version>
```

Tag builds pass `GITHUB_REF_NAME` explicitly. Packaging extracts and compares:

```text
fabric.mod.json -> version
META-INF/MANIFEST.MF -> Implementation-Version
requested release tag
```

A release package is rejected when any value is missing, mismatched or still a snapshot.

Expected installed identity:

```text
filename: villaigence-fabric-0.1.21+1.21.1.jar
Fabric metadata version: 0.1.21+1.21.1
Implementation-Version: 0.1.21+1.21.1
in-game mod version: 0.1.21+1.21.1
```

---

## TDD evidence

### Canonical RED

```text
head: 52f5950c2dba1e3299dc28e2b4b5041b19232b0a
VillAIgence CI #1280 / 30753012925: EXPECTED FAILURE
Java Pull Request CI #717 / 30753012928: SUCCESS
Repository security policy #631 / 30753012927: SUCCESS
```

Failure boundary:

```text
Verify exact release identity contract: FAILURE
Expected 9.8.7+1.21.1
Found 1.21.1-SNAPSHOT
```

The failure occurred before loader builds and proved that the explicit version input was ignored.

### Exact-head GREEN before documentation

```text
head: 19e1847e23f4df16c126296c451999bbacf4216c
VillAIgence CI #1288 / 30753288923: SUCCESS
Java Pull Request CI #721 / 30753288938: SUCCESS
Repository security policy #639 / 30753288920: SUCCESS
Supply-chain verification #67 / 30753288921: SUCCESS
VillAIgence GitHub Release dry-run #106 / 30753288924: SUCCESS
```

Validated boundaries:

```text
explicit Gradle release identity: PASS
snapshot-named-inside-release rejection: PASS
valid exact embedded identity fixture: PASS
common unit tests: PASS
Fabric build: PASS
NeoForge build: PASS
Fabric distributable package verification: PASS
release dry-run packaging/upload: PASS
security policy: PASS
supply-chain verification: PASS
```

---

## Preserved boundaries

```text
AI provider/parser/retry changed: no
Chat/STT/TTS/Voice Chat changed: no
memory or relationship schemas changed: no
operator lore changed: no
packets changed: no
world migration required: no
tombstone item schema changed: no
empty grave loot changed: no
loader or dependency versions changed: no
internal mod ID changed: no
compatibility-sensitive LivingWorld paths changed: no
```

Development builds without an explicit release version or exact release tag remain:

```text
1.21.1-SNAPSHOT
```

---

## Exact installed acceptance procedure

### Phase 0 — Safety and identity

1. Stop the server cleanly.
2. Create a complete backup of the server directory and world.
3. Record SHA-256 values for:
   - `memory.json`;
   - `memory2.json`;
   - `semantic-memory.json`;
   - `relationships.json`;
   - `voices.json`;
   - `operator-lore.json` when present.
4. Download only the official `villaigence-fabric-0.1.21+1.21.1.jar`.
5. Verify its published SHA-256.
6. Remove the previous VillAIgence/MCA JAR; do not keep both versions.
7. Inspect the JAR identity:

```bash
unzip -p villaigence-fabric-0.1.21+1.21.1.jar fabric.mod.json
unzip -p villaigence-fabric-0.1.21+1.21.1.jar META-INF/MANIFEST.MF | grep Implementation-Version
```

Required values:

```text
fabric.mod.json version = 0.1.21+1.21.1
Implementation-Version = 0.1.21+1.21.1
```

### Phase 1 — Server startup gate

1. Install the exact candidate on the server first.
2. Start without connecting a client.
3. Require:
   - no Mixin apply error;
   - no refmap error;
   - no `MixinTombstoneData` error;
   - no `MixinGroundPathNavigation` injection error;
   - world load completes;
   - server reaches `STARTED`;
   - TCP `25565` responds;
   - UDP `24454`/Voice Chat initializes;
   - monitor remains active;
   - no persistent-file corruption or recovery event.
4. Stop cleanly and compare persistent hashes with the pre-install values.

Failure action:

```text
stop immediately
preserve logs
restore the backup and previous stable JAR
verify persistent hashes
```

Suggested startup marker:

```text
V0121_STARTUP_GATE_PASS
```

### Phase 2 — Water navigation regression

Use a living MCA NPC in a controlled shallow-water area with an accessible shore.

1. Record the NPC identity.
2. Make the NPC enter water naturally or by controlled placement.
3. Observe for at least two minutes.
4. Require:
   - the NPC selects movement toward a reachable shore;
   - movement continues instead of oscillating in place;
   - the NPC exits the water;
   - the NPC does not drown;
   - ordinary land navigation still works afterward.
5. Repeat with a second NPC to exclude an entity-specific state.

Suggested marker:

```text
V0121_WATER_ESCAPE_PASS
```

### Phase 3 — Filled grave Silk Touch regression

Use a disposable test NPC carrying clearly identifiable items.

1. Record NPC name and UUID when available.
2. Record the exact inventory placed on the NPC.
3. Create a filled MCA grave through the normal death path.
4. Break it with the same Silk Touch pickaxe class that reproduced `0.1.20`.
5. Require:
   - exactly one tombstone item appears;
   - the item is collectible;
   - no duplicate tombstone appears;
   - unrelated remains drops are not lost or duplicated.
6. Place the dropped tombstone item.
7. Require the grave to be filled, not empty.
8. Complete the normal resurrection/opening path used for verification.
9. Require:
   - same NPC identity/name;
   - expected UUID continuity when the game path preserves it;
   - all recorded inventory items restored;
   - no duplicate NPC or duplicated inventory.
10. Repeat once after a server restart using a newly created filled grave.

Control checks:

```text
empty grave break behavior unchanged
filled grave with ordinary allowed tool still produces one portable item
inappropriate tool does not cause silent body loss
```

Suggested marker:

```text
V0121_FILLED_GRAVE_ROUND_TRIP_PASS
```

### Phase 4 — Existing functional regression

Verify a concise cumulative set:

```text
text dialogue -> DIALOGUE
voice -> STT -> Chat -> TTS -> DIALOGUE
NPC A/B memory remains isolated
operator lore editor opens and saves
ladder navigation works
mounted archer works
fishing rod use and durability work
gifts work
```

Record Chat duration. A single request approaching the prior approximately `272 s` remains a performance defect even if it eventually succeeds.

### Phase 5 — Restart and persistence

1. Stop cleanly.
2. Recalculate all persistent hashes.
3. Restart with the same exact JAR.
4. Require:
   - world loads;
   - tested NPC identities remain valid;
   - restored grave/NPC state remains valid;
   - persistent JSON files parse normally;
   - no corruption/recovery warnings;
   - ports and monitor return.
5. Compare hashes, allowing only changes caused by the explicitly executed test actions.
6. Load a copy of the rollback world originating from the previously stable release and verify startup without mutating the primary world.

Final marker only when all phases pass:

```text
V0121_CORRECTIVE_CUMULATIVE_ACCEPTANCE_PASS
```

---

## Release decision

```text
repository and automated promotion: allowed
candidate publication: allowed after exact-head merge
installed production acceptance: blocked until operator test
new feature development: deferred until destructive regressions pass
```
