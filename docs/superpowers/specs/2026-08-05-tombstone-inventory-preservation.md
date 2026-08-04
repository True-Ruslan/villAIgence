# Tombstone Inventory Preservation

## Confirmed defect

In `0.1.23+1.21.1`, `VillagerEntityMCA.die()` invokes the MCA inventory death-drop before the selected tombstone serializes the NPC. The live acceptance log showed three emeralds before death, a separate three-emerald item entity after death, and `Inventory: []` inside the tombstone `EntityData`.

## Required behavior

When an MCA villager is successfully captured by an empty tombstone:

1. the tombstone owns the complete custom 27-slot MCA inventory snapshot;
2. the same stacks are not emitted as loose death drops;
3. resurrection restores slot contents and counts exactly once;
4. Silk Touch grave transport preserves the same inventory;
5. a villager death with no eligible tombstone retains legacy loose-item death-drop behavior;
6. a failed tombstone capture must fail open to legacy loose-item drops rather than delete inventory.

## Acceptance boundary

The fix is not complete until a real server GameTest exercises the actual death path, tombstone selection, serialization and resurrection. Direct `Data.setEntity()` round-trip tests are necessary but insufficient because they bypass the observed ordering defect.
