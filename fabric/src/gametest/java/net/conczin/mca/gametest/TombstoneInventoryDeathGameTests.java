package net.conczin.mca.gametest;

import net.conczin.mca.block.TombstoneBlock;
import net.conczin.mca.entity.VillagerEntityMCA;
import net.conczin.mca.registry.BlocksMCA;
import net.conczin.mca.registry.EntitiesMCA;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

import java.util.Optional;
import java.util.UUID;

public final class TombstoneInventoryDeathGameTests implements FabricGameTest {
    private static final UUID FIXTURE_UUID = UUID.fromString(
            "e76bdf4e-bdd1-4a9c-b09c-b0e69a87cb47"
    );

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void deathIntoEmptyTombstonePreservesInventoryWithoutLooseDuplicates(
            GameTestHelper helper
    ) {
        BlockPos tombstonePos = helper.absolutePos(new BlockPos(2, 1, 2));
        helper.getLevel().setBlockAndUpdate(
                tombstonePos,
                BlocksMCA.UPRIGHT_HEADSTONE.defaultBlockState()
        );

        VillagerEntityMCA villager = helper.spawn(EntitiesMCA.MALE_VILLAGER, 3, 1, 2);
        villager.setUUID(FIXTURE_UUID);
        villager.setCustomName(Component.literal("Inventory Acceptance Villager"));
        villager.getInventory().setItem(0, new ItemStack(Items.EMERALD, 3));
        villager.getInventory().setItem(7, new ItemStack(Items.BREAD, 11));
        villager.getInventory().setItem(26, new ItemStack(Items.IRON_SWORD));

        AABB deathArea = villager.getBoundingBox().inflate(8.0D);
        boolean damaged = villager.hurt(
                helper.getLevel().damageSources().genericKill(),
                Float.MAX_VALUE
        );
        helper.assertTrue(damaged, "Fixture villager must accept lethal damage");
        helper.assertTrue(!villager.isAlive(), "Fixture villager must die");

        helper.assertTrue(
                helper.getLevel().getBlockEntity(tombstonePos) instanceof TombstoneBlock.Data,
                "Configured grave must retain its tombstone block entity"
        );
        TombstoneBlock.Data tombstone = (TombstoneBlock.Data) helper.getLevel()
                .getBlockEntity(tombstonePos);
        helper.assertTrue(tombstone.hasEntity(),
                "Real death path must store the NPC in the selected tombstone");

        Optional<Entity> recreatedEntity = tombstone.createEntity(helper.getLevel(), false);
        helper.assertTrue(recreatedEntity.isPresent(),
                "Stored tombstone entity must be reconstructable");
        helper.assertTrue(recreatedEntity.get() instanceof VillagerEntityMCA,
                "Stored entity must remain an MCA villager");

        VillagerEntityMCA recreated = (VillagerEntityMCA) recreatedEntity.get();
        helper.assertTrue(recreated.getUUID().equals(FIXTURE_UUID),
                "Real death path must preserve NPC UUID");
        assertInventoryCount(helper, recreated, Items.EMERALD, 3);
        assertInventoryCount(helper, recreated, Items.BREAD, 11);
        assertInventoryCount(helper, recreated, Items.IRON_SWORD, 1);
        helper.assertTrue(nonEmptyStackCount(recreated) == 3,
                "Tombstone inventory must contain exactly the three fixture stacks");

        assertLooseItemCount(helper, deathArea, Items.EMERALD, 0);
        assertLooseItemCount(helper, deathArea, Items.BREAD, 0);
        assertLooseItemCount(helper, deathArea, Items.IRON_SWORD, 0);
        helper.succeed();
    }

    private static void assertInventoryCount(
            GameTestHelper helper,
            VillagerEntityMCA villager,
            Item expectedItem,
            int expectedCount
    ) {
        int actualCount = 0;
        for (int slot = 0; slot < villager.getInventory().getContainerSize(); slot++) {
            ItemStack stack = villager.getInventory().getItem(slot);
            if (stack.is(expectedItem)) {
                actualCount += stack.getCount();
            }
        }
        helper.assertTrue(actualCount == expectedCount,
                "Inventory must preserve " + expectedCount + " of " + expectedItem
                        + ", found " + actualCount);
    }

    private static int nonEmptyStackCount(VillagerEntityMCA villager) {
        int count = 0;
        for (int slot = 0; slot < villager.getInventory().getContainerSize(); slot++) {
            if (!villager.getInventory().getItem(slot).isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private static void assertLooseItemCount(
            GameTestHelper helper,
            AABB area,
            Item expectedItem,
            int expectedCount
    ) {
        int actualCount = helper.getLevel()
                .getEntitiesOfClass(ItemEntity.class, area)
                .stream()
                .map(ItemEntity::getItem)
                .filter(stack -> stack.is(expectedItem))
                .mapToInt(ItemStack::getCount)
                .sum();
        helper.assertTrue(actualCount == expectedCount,
                "Loose death drops must contain " + expectedCount + " of " + expectedItem
                        + ", found " + actualCount);
    }
}
