# Tombstone Inventory Preservation

## Confirmed defect

In `0.1.23+1.21.1`, `VillagerEntityMCA.die()` invoked the MCA inventory death-drop before the selected tombstone serialized the NPC. The live acceptance log showed three emeralds before death, a separate three-emerald item entity after death, and `Inventory: []` inside the tombstone `EntityData`.

The same ordering existed in `ZombieVillagerEntityMCA.die()`:

1. `InventoryUtils.dropAllItems()` spawned every custom inventory stack and cleared the 27-slot container;
2. `Relationship.onDeath()` subsequently selected the tombstone;
3. `TombstoneBlock.Data.setEntity()` serialized an already-empty NPC.

## Required behavior

When an MCA NPC is successfully captured by an empty tombstone:

1. the tombstone owns the complete custom 27-slot MCA inventory snapshot;
2. the same stacks are not emitted as loose death drops;
3. resurrection restores slot contents and counts exactly once;
4. Silk Touch grave transport preserves the same inventory;
5. an MCA NPC death with no eligible tombstone retains legacy loose-item death-drop behavior;
6. a failed tombstone capture fails open to legacy loose-item drops rather than deleting inventory.

## Implemented ownership rule

`Relationship.onDeath()` now returns whether a valid tombstone synchronously accepted the NPC snapshot.

- On successful capture, serialization occurs while the custom inventory is intact; the dead entity container is then cleared without spawning loose copies.
- If no tombstone is selected, the block entity is invalid, or capture does not produce stored entity data, the existing `InventoryUtils.dropAllItems()` path runs unchanged.
- The same rule is used by ordinary and infected MCA villagers.

## TDD evidence

The first real death-path GameTest compiled and failed before the production change with:

```text
Inventory must preserve 3 of minecraft:emerald, found 0
```

Canonical RED evidence: VillAIgence focused GameTest run `30956855080`, job `92151832908`.

The completed server tests exercise both ownership outcomes:

1. a real MCA villager death next to an empty tombstone preserves fixed UUID, three emeralds, eleven bread and one iron sword in the reconstructed NPC while emitting zero loose copies;
2. a hostile MCA zombie villager with no eligible tombstone emits exactly four emeralds and six bread once and serializes an empty dead inventory.

## Verified boundaries

VillAIgence CI run `30958208026` passed on head `a63784fbb87b3d1aff85cde428ab84d896c0324f`:

- both separate production JVM runs and six-store restart hash comparison;
- the complete server GameTest suite including both inventory ownership scenarios;
- common tests;
- Fabric and NeoForge builds;
- distributable package verification.

Java Pull Request CI run `30958208020` and Repository security policy run `30958208021` also passed on the same head.

## Acceptance boundary

Automated death-path coverage closes the confirmed ordering defect at the code and real server-test layers. The exact fixed release remains subject to an installed manual canary: kill an MCA villager carrying known stacks, resurrect through the grave flow, and compare the restored inventory without collecting any loose duplicate stacks.