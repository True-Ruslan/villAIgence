# Upstream S1 Tombstone Data Integrity Validation

**Implementation date:** 2026-08-01  
**Package:** S1 — tombstone item/block-entity data integrity  
**Pull request:** #72  
**VillAIgence base:** `5a67e4f79661e569c9958caa14cc89d998a1b57b`  
**Upstream source:** `4ee4741c861a3a5b5561246dcc99005c41f45160`  
**Upstream audit target:** `7ba76165dcf8aa9aa79070bdbac141b65519b3e7`

## Defect

A filled tombstone serialized its embedded NPC data into a copied `ENTITY_DATA` tag but never wrote that mutated copy back to the dropped item. The item therefore lost the tombstone block-entity payload across break and placement.

The historical component was also the wrong persistence surface for a block item. The correct current representation is `DataComponents.BLOCK_ENTITY_DATA`, written through `BlockItem.setBlockEntityData` so the block-entity type identifier is attached consistently.

## Implemented behavior

```text
write:
EntityData fields
→ fresh CompoundTag
→ defensive copy policy
→ BlockItem.setBlockEntityData
→ BLOCK_ENTITY_DATA

read:
BLOCK_ENTITY_DATA when present
→ otherwise legacy ENTITY_DATA
→ defensive copy
→ require EntityData compound
→ reconstruct tombstone EntityData
```

Properties:

- current `BLOCK_ENTITY_DATA` has precedence over legacy `ENTITY_DATA`;
- legacy items remain readable;
- all new writes use `BLOCK_ENTITY_DATA`;
- read and write boundaries use defensive copies;
- absent, null or malformed item data fails soft to an empty tombstone payload;
- resurrection, graveyard registration and NPC NBT structure are unchanged;
- no VillAIgence persistent JSON schema is changed;
- no AI provider, network-security, workflow, script or dependency path is changed.

## TDD evidence

### Canonical RED

```text
head: 92fe2fd22a457168e29c8589a3ffebbf65625506
workflow: VillAIgence CI #994
run: 30692753640
result: FAILURE
failure boundary: common:compileTestJava
reason: TombstoneItemDataPolicy did not exist at the five policy call sites
```

The preceding experimental test attempted to instantiate Minecraft classes directly from `common:test`; it exposed that the common unit-test classpath intentionally excludes Minecraft runtime classes. The test was corrected to exercise a loader-independent policy, while NeoForge/Fabric builds compile the actual Minecraft adapter.

### GREEN

```text
head: 165e4f29db4ef1e0e47de306a72ab07bf2a8c9c3
VillAIgence CI #998 / 30693237041             SUCCESS
Java Pull Request CI #518 / 30693237043      SUCCESS
Repository security policy #200 / 30693237046 SUCCESS
```

The successful gate executes:

```text
:common:test
:fabric:build
:neoforge:build
repository security policy
Fabric distributable-package verification
```

## Automated regression coverage

`TombstoneEntityDataCodecTest` covers:

1. current component precedence over legacy data;
2. legacy fallback when current data is absent;
3. empty result when both components are absent;
4. defensive copying on read;
5. exactly one defensive write to the current component boundary.

## Required real-server acceptance

Automated checks prove the serialization policy and both loader builds. They do not replace a world-level break/place/restart scenario.

Run on a backed-up test world using a release or exact PR artifact containing S1:

```text
1. Obtain or create a filled MCA tombstone containing a known NPC.
2. Record the NPC name, gender and another recognizable NBT-backed property.
3. Break the tombstone and retain the dropped tombstone item.
4. Place the item at a different valid location.
5. Confirm the tombstone is still filled and displays the same NPC identity.
6. Restart the server.
7. Confirm the same identity and data again.
8. Break, place and restart a second time.
9. Confirm no data loss, empty tombstone, duplicate grave entry or persistence error.
10. Optionally complete resurrection and verify that the expected entity can still be created.
```

Expected result:

```text
item retains BLOCK_ENTITY_DATA
placed tombstone restores EntityData
identity remains stable across two break/place cycles and restart
no duplicate grave record
no serialization or persistence exception
```

## Acceptance boundary

```text
repository implementation and automated validation: PASS
real-server break/place/restart validation: PENDING
release promotion based on S1 live evidence: NOT YET CLAIMED
```
