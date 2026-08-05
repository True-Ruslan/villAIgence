# VillAIgence `0.1.25+1.21.1` Release Gate

## Purpose

Publish the first VillAIgence artifact produced by the complete exact-production release gate, without rewriting the already-consumed `0.1.24+1.21.1` tag.

This document distinguishes:

1. repository and GameTest evidence;
2. release-workflow exact-artifact evidence;
3. installed server/client canary evidence.

No layer may be claimed from evidence belonging only to another layer.

## Release identity

```text
previous immutable tag:       0.1.24+1.21.1
previous tag commit:          42d8cb3408c53770abe63ced130727c805bc9e8a
release-gate merge:           193f1a0ed3882f0c8e925c5ae16d59f5bacb489c
requested tag:                0.1.25+1.21.1
exact release commit:         PENDING
release workflow run:         PENDING
official JAR SHA-256:         PENDING
installed acceptance marker:  PENDING
```

`0.1.25+1.21.1` contains no new persistence schema or world migration. Its release purpose is to promote the existing inventory-preservation runtime through the stronger immutable exact-artifact gate merged after `0.1.24` was published.

## Included runtime correction

The confirmed `0.1.23` defect was:

```text
NPC custom inventory
→ loose death drops created
→ custom inventory cleared
→ selected tombstone serializes Inventory: []
```

PR #105 changed ownership ordering:

- a valid tombstone serializes the NPC while the custom inventory is intact;
- successful capture clears the dead container without loose duplicates;
- resurrection reconstructs the stored stacks exactly once;
- Silk Touch grave transport retains the stored entity data;
- absent or failed capture preserves the legacy single loose-drop behavior;
- ordinary and infected MCA villagers use the same rule.

Canonical fix merge:

```text
42d8cb3408c53770abe63ced130727c805bc9e8a
```

## Existing automated evidence

Canonical RED:

```text
run:      30956855080
job:      92151832908
failure:  Inventory must preserve 3 of minecraft:emerald, found 0
```

Verified PR #105 GREEN:

```text
VillAIgence CI:              30958208026 — SUCCESS
Java Pull Request CI:        30958208020 — SUCCESS
Repository security policy:  30958208021 — SUCCESS
```

Post-merge verification on the fix head:

```text
VillAIgence CI:              30958682946 / #1420 — SUCCESS
Java Pull Request CI:        30958682966 / #824 — SUCCESS
Repository security policy:  30958682915 / #771 — SUCCESS
```

The GameTests prove:

- UUID and complete known inventory reconstruction after successful capture;
- zero loose duplicates after successful capture;
- single loose-drop fallback when no tombstone accepts the NPC;
- Fabric and NeoForge build compatibility;
- production startup and restart of the test candidate;
- valid stable canonical acceptance stores when the fixture makes no intentional mutation.

## Release-request authority

Canonical request:

```text
docs/releases/NEXT_RELEASE.txt
```

Required value:

```text
0.1.25+1.21.1
```

The pull-request workflow must treat this as validation only. It may build with the requested embedded version, but it must not create a tag or GitHub Release.

After squash merge, a push to `1.21.1` that changes the request file may publish only after the exact merge commit passes the complete gate. The workflow then creates one annotated tag on that exact commit and publishes the verified package.

A tag that already exists at another commit fails closed. Existing published tags are never moved or overwritten.

## Exact release workflow gate

Before publication, the exact candidate must pass:

```text
release tag format and Minecraft suffix
current 1.21.1 HEAD identity
unused or same-commit idempotent tag state
embedded fabric.mod.json version
manifest Implementation-Version
repository security policy
production harness contract tests
exact remapped Fabric candidate staging
production Fabric server startup
clean shutdown
same-world second JVM restart
fixture marker on both runs
canonical persistent-store validity
restart path and SHA-256 comparison
acceptance risk catalog
common tests
Fabric server GameTests
Fabric build
NeoForge build
release package smoke verification
accepted/published JAR byte identity
JAR, checksum and dependency manifest upload
annotated tag creation
GitHub Release publication
```

The release workflow run ID, exact tag commit and official JAR SHA-256 must be recorded after publication. PR and dry-run hashes are not official release identities.

## Expected assets

```text
villaigence-fabric-0.1.25+1.21.1.jar
villaigence-fabric-0.1.25+1.21.1.jar.sha256
villaigence-dependencies-0.1.25+1.21.1.txt
```

## Installed acceptance preparation

Before replacing the server JAR, record:

```text
backup path
currently installed version
candidate filename
candidate SHA-256
Java version
Fabric Loader version
Fabric API version
Simple Voice Chat version
path, size and SHA-256 of every canonical persistent store
```

Canonical stores used by the production harness:

```text
memory.json
memory2.json
semantic-memory.json
relationships.json
voices.json
operator-lore.json
```

`events.json` must also remain valid when present.

## Installed canary

### Startup and regression smoke

1. Install the exact official JAR on server and client.
2. Require normal Minecraft startup with no loader, Mixin, refmap or JVM failure.
3. Connect a client and verify Simple Voice Chat.
4. Run one Text → NPC dialogue.
5. Run one microphone → STT → Chat → TTS cycle.
6. Require controlled fail-soft behavior for any intentionally induced provider or TTS failure.

### Tombstone inventory ownership

Use one dedicated MCA villager with recorded UUID, name and known custom inventory, for example:

```text
emerald x3
bread x11
iron sword x1
```

Then:

1. place a valid empty tombstone within the supported capture boundary;
2. kill the NPC without collecting possible drops;
3. require zero loose copies of the known stacks;
4. break the filled tombstone with Silk Touch;
5. require exactly one portable data-bearing grave item;
6. place the grave again;
7. stop and restart the same world;
8. resurrect the NPC;
9. require the same UUID and displayed name;
10. require all known item counts exactly once;
11. require no duplicate loose stacks near the death, grave or resurrection location.

### No-tombstone fallback

On a disposable NPC or isolated world:

1. ensure no eligible tombstone can capture the NPC;
2. assign known custom stacks;
3. kill the NPC;
4. require every stack to drop exactly once;
5. require no duplicate retained inventory snapshot.

### Persistence and restart

1. stop the server cleanly;
2. parse all canonical JSON stores;
3. compare paths and hashes while explaining intentional scenario mutations;
4. restart once more;
5. verify the resurrected NPC, server, Voice Chat and monitoring remain stable.

## PASS criteria

A full acceptance marker requires:

- exact official tag commit recorded;
- release workflow fully green;
- official JAR SHA-256 recorded;
- startup and Text/STT/Chat/TTS smoke PASS;
- no loose duplicate stacks after successful capture;
- Silk Touch grave transport preserves stored inventory;
- restart before resurrection preserves identity and inventory;
- resurrection restores UUID, name and known stacks exactly once;
- no-tombstone fallback drops stacks exactly once;
- canonical persistence remains valid and restart-safe;
- no blocking VillAIgence, Mixin, refmap or storage errors.

## Failure handling

On failure:

1. stop when further execution could mutate evidence;
2. preserve the exact official JAR, hash, logs and persistent-state evidence;
3. restore the backed-up known-good server state;
4. never move or overwrite `0.1.25+1.21.1` after publication;
5. correct the defect under the next sequential version.

## Result

```text
release request PR:           IN PROGRESS
exact release commit:         PENDING
release workflow:             PENDING
official JAR identity:        PENDING
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
