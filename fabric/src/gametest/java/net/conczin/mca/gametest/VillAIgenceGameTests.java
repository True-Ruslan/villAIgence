package net.conczin.mca.gametest;

import net.conczin.mca.block.TombstoneBlock;
import net.conczin.mca.entity.VillagerEntityMCA;
import net.conczin.mca.entity.ai.navigation.MCAGroundPathNavigation;
import net.conczin.mca.registry.BlocksMCA;
import net.conczin.mca.registry.EntitiesMCA;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;
import java.util.UUID;

public final class VillAIgenceGameTests implements FabricGameTest {
    private static final UUID TOMBSTONE_FIXTURE_UUID = UUID.fromString(
            "5ac86058-2caf-47a0-8f65-bf2f13e35472"
    );

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void mcaVillagerRegistryAndNavigationBoot(GameTestHelper helper) {
        VillagerEntityMCA villager = helper.spawn(EntitiesMCA.MALE_VILLAGER, 2, 1, 2);

        helper.assertTrue(villager.isAlive(), "Spawned MCA villager must be alive");
        helper.assertTrue(villager.level() == helper.getLevel(),
                "Spawned MCA villager must belong to the GameTest server level");
        helper.assertTrue(villager.getNavigation() instanceof MCAGroundPathNavigation,
                "MCA villager must use MCAGroundPathNavigation");
        helper.succeed();
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void tombstoneItemRoundTripPreservesNpcIdentityAndInventory(GameTestHelper helper) {
        VillagerEntityMCA original = EntitiesMCA.MALE_VILLAGER.create(helper.getLevel());
        helper.assertTrue(original != null, "MCA villager fixture must be creatable");

        original.setUUID(TOMBSTONE_FIXTURE_UUID);
        original.setCustomName(Component.literal("Acceptance Basiliso"));
        original.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 3));
        original.getInventory().setItem(7, new ItemStack(Items.BREAD, 11));
        original.getInventory().setItem(26, new ItemStack(Items.IRON_SWORD));

        BlockState tombstoneState = BlocksMCA.UPRIGHT_HEADSTONE.defaultBlockState();
        TombstoneBlock.Data sourceData = new TombstoneBlock.Data(
                helper.absolutePos(new BlockPos(2, 1, 2)),
                tombstoneState
        );
        sourceData.setEntity(original);

        ItemStack portableTombstone = new ItemStack(BlocksMCA.UPRIGHT_HEADSTONE.asItem());
        sourceData.writeToStack(portableTombstone);
        helper.assertTrue(portableTombstone.has(DataComponents.BLOCK_ENTITY_DATA),
                "Portable tombstone must contain block-entity data");

        TombstoneBlock.Data restoredData = new TombstoneBlock.Data(
                helper.absolutePos(new BlockPos(4, 1, 2)),
                tombstoneState
        );
        restoredData.readFromStack(portableTombstone);
        helper.assertTrue(restoredData.hasEntity(),
                "Placed tombstone data must recover a stored entity");

        Optional<Entity> recreatedEntity = restoredData.createEntity(helper.getLevel(), false);
        helper.assertTrue(recreatedEntity.isPresent(),
                "Stored tombstone entity must be reconstructable");
        helper.assertTrue(recreatedEntity.get() instanceof VillagerEntityMCA,
                "Reconstructed entity must remain an MCA villager");

        VillagerEntityMCA recreated = (VillagerEntityMCA) recreatedEntity.get();
        helper.assertTrue(recreated.getUUID().equals(TOMBSTONE_FIXTURE_UUID),
                "Tombstone round trip must preserve NPC UUID");
        helper.assertTrue(recreated.getName().getString().equals("Acceptance Basiliso"),
                "Tombstone round trip must preserve NPC name");
        assertInventoryCount(helper, recreated, Items.DIAMOND, 3);
        assertInventoryCount(helper, recreated, Items.BREAD, 11);
        assertInventoryCount(helper, recreated, Items.IRON_SWORD, 1);
        helper.assertTrue(nonEmptyStackCount(recreated) == 3,
                "Tombstone round trip must neither lose nor duplicate inventory stacks");
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
}
