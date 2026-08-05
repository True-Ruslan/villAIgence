# VillAIgence `0.1.24+1.21.1` Release Gate

## Purpose

Promote the merged tombstone-inventory preservation fix only through an exact-artifact release gate, then verify the remaining installed behavior that automated tests cannot fully prove.

This document does not claim that `0.1.24+1.21.1` exists or has passed installed acceptance until the exact tag, official asset identity and operator evidence are recorded below.

## Release scope

The candidate contains the already-merged PR #105 correction:

```text
base inventory-fix merge: 42d8cb3408c53770abe63ced130727c805bc9e8a
release tag:                0.1.24+1.21.1
exact release commit:       PENDING
official JAR SHA-256:       PENDING
release workflow run:       PENDING
installed marker:           PENDING
```

No persistence schema, tombstone item format, AI provider contract, packet schema or world migration is introduced by the inventory fix.

## Confirmed `0.1.23` defect

Installed acceptance showed that MCA dropped and cleared the custom 27-slot inventory before a selected tombstone serialized the NPC:

```text
NPC inventory contains emerald x3
→ InventoryUtils.dropAllItems()
→ loose emerald item entity is spawned
→ NPC custom inventory is cleared
→ tombstone serializes Inventory: []
```

Identity survived resurrection, but the custom inventory did not.

## Corrected ownership contract

When a valid empty tombstone synchronously accepts the NPC:

1. the tombstone serializes the complete custom inventory while it is intact;
2. the dead entity inventory is cleared without loose duplicate drops;
3. resurrection reconstructs the stored stacks exactly once;
4. Silk Touch transport preserves the same stored entity data.

When no tombstone accepts the NPC, or capture fails:

1. the legacy loose-drop path remains active;
2. each stack is emitted exactly once;
3. the dead entity inventory is cleared;
4. no inventory is silently deleted.

The same ownership rule applies to ordinary and infected MCA villagers.

## Existing automated evidence

PR #105 established the production behavior through real Fabric server GameTests.

Canonical RED:

```text
run: 30956855080
job: 92151832908
failure: Inventory must preserve 3 of minecraft:emerald, found 0
```

Verified GREEN before merge:

```text
VillAIgence CI:             30958208026 — SUCCESS
Java Pull Request CI:       30958208020 — SUCCESS
Repository security policy: 30958208021 — SUCCESS
```

Post-merge PR-head verification:

```text
VillAIgence CI:              30958682946 / #1420 — SUCCESS
Java Pull Request CI:        30958682966 / #824 — SUCCESS
Repository security policy:  30958682915 / #771 — SUCCESS
```

The automated scenarios prove:

- captured villager inventory reconstruction with fixed UUID, emerald x3, bread x11 and iron sword x1;
- zero loose duplicates after successful tombstone capture;
- uncaptured hostile zombie villager fallback with emerald x4 and bread x6 dropped exactly once;
- production server startup, clean shutdown and restart in separate JVMs;
- stable paths and SHA-256 values for the six canonical acceptance stores;
- Fabric and NeoForge builds;
- distributable Fabric package verification.

## Exact release workflow gate

The tag-based workflow must now prove all of the following before GitHub Release publication:

```text
valid and unique 0.1.24+1.21.1 tag
exact current 1.21.1 HEAD
embedded Fabric version = tag
manifest Implementation-Version = tag
repository security policy PASS
production harness contract tests PASS
exact remapped candidate staged with release_version
production Fabric server startup PASS
clean shutdown PASS
same-world second JVM restart PASS
production fixture marker on both runs
canonical store validity and restart hashes PASS
risk catalog PASS
common tests PASS
Fabric server GameTests PASS
Fabric build PASS
NeoForge build PASS
release package smoke PASS
production-accepted JAR byte-equal to packaged JAR
JAR/checksum/dependency manifest uploaded
GitHub Release created only after all prior gates PASS
```

A workflow dry-run is not official release evidence. The exact tag run and official uploaded JAR identity must be recorded.

## Pre-installation capture

Before replacing the server JAR, record:

```text
backup path
current installed version
candidate filename
candidate SHA-256
server Java version
Fabric Loader version
Fabric API version
Simple Voice Chat version
hash/path/size of each canonical persistent store
```

Canonical acceptance stores:

```text
memory.json
memory2.json
semantic-memory.json
relationships.json
voices.json
operator-lore.json
```

`events.json` must also remain valid when present, but the current production harness fixture owns the six-store restart oracle above.

## Installed acceptance procedure

### A. Startup and regression surface

1. Install the exact official `villaigence-fabric-0.1.24+1.21.1.jar` on server and client.
2. Start the server and require the normal Minecraft ready marker.
3. Reject Mixin, refmap, mod-resolution, JVM or startup failure signatures.
4. Connect one client.
5. Verify Simple Voice Chat connection and UDP health.
6. Run one Text → NPC dialogue.
7. Run one microphone → STT → Chat → TTS cycle.
8. Require controlled failure behavior if a provider or TTS test intentionally fails.

### B. Focused tombstone inventory canary

Use a dedicated MCA villager with recorded identity and known custom inventory, for example:

```text
emerald x3
bread x11
iron sword x1
```

Then:

1. record NPC UUID and displayed name;
2. place a valid empty tombstone within the supported capture boundary;
3. kill the NPC without collecting any possible loose drops;
4. inspect the area and require zero loose copies of the known stacks;
5. break the filled tombstone with Silk Touch;
6. require exactly one portable data-bearing tombstone item;
7. place the tombstone again;
8. stop the server cleanly;
9. restart the same world;
10. resurrect the NPC;
11. require the same UUID and displayed name;
12. require every known item and count exactly once;
13. require no duplicate loose stacks around the death, grave or resurrection location.

### C. Fallback control

On a disposable test entity or isolated world:

1. ensure no eligible empty tombstone can capture the MCA NPC;
2. give the NPC known custom stacks;
3. kill the NPC;
4. require those stacks to drop exactly once;
5. require no retained duplicate inventory in the dead entity snapshot.

This control confirms the fix did not turn failed capture into item deletion.

### D. Persistence and restart

After the focused canary:

1. stop the server cleanly;
2. parse all canonical JSON stores;
3. compare expected paths and pre/post-restart hashes;
4. explain every intentional mutation rather than requiring unchanged hashes for stores modified by the scenario;
5. restart once more and verify the resurrected NPC remains stable;
6. verify server, Voice Chat and monitoring health.

## PASS criteria

The release may receive a full inventory-fix acceptance marker only when:

- the exact official JAR hash is recorded;
- the official tag workflow is fully green;
- startup and Text/STT/Chat/TTS regression smoke pass;
- successful tombstone capture emits no loose duplicate stacks;
- Silk Touch grave transport retains stored inventory;
- restart before resurrection does not lose identity or inventory;
- resurrection restores UUID, name and known inventory exactly once;
- no-tombstone fallback still drops inventory exactly once;
- canonical persistence remains valid and restart-safe;
- no blocking Mixin/refmap/injection or VillAIgence persistence errors remain.

## Failure handling

On any failure:

1. stop acceptance immediately when continuing could mutate or destroy evidence;
2. preserve the exact official JAR and SHA-256;
3. preserve bounded relevant logs and persistent-file hashes;
4. restore the backed-up known-good server state;
5. do not overwrite or reuse the published `0.1.24+1.21.1` tag;
6. fix the defect under a new sequential version.

## Result

```text
release workflow:             PENDING
exact official JAR identity:  PENDING
installed startup:            PENDING
Text/STT/Chat/TTS smoke:      PENDING
captured inventory:           PENDING
no duplicate loose drops:     PENDING
Silk Touch transport:         PENDING
restart before resurrection:  PENDING
UUID/name preservation:       PENDING
fallback loose drops:         PENDING
persistence validity:         PENDING
final marker:                 PENDING
```
