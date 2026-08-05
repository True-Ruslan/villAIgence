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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

import java.util.Optional;
import java.util.UUID;

public final class TombstoneIdentityReplayGameTests implements FabricGameTest {
    private static final UUID FIXTURE_UUID = UUID.fromString(
            "5cf53206-ec2c-4c88-ad11-a8bbc56f514e"
    );
    private static final String FIXTURE_NAME = "Identity Replay Acceptance";

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void rejectedDuplicateReplayPreservesGraveForOneLaterRestoration(
            GameTestHelper helper
    ) {
        VillagerEntityMCA authoritative = createFixture(helper);
        BlockPos authoritativePos = helper.absolutePos(new BlockPos(2, 1, 2));
        authoritative.setPos(
                authoritativePos.getX() + 0.5D,
                authoritativePos.getY(),
                authoritativePos.getZ() + 0.5D
        );
        helper.assertTrue(
                helper.getLevel().addFreshEntity(authoritative),
                "Authoritative fixture must enter the server level"
        );

        TombstoneBlock.Data tombstone = new TombstoneBlock.Data(
                helper.absolutePos(new BlockPos(4, 1, 2)),
                BlocksMCA.UPRIGHT_HEADSTONE.defaultBlockState()
        );
        tombstone.setEntity(createFixture(helper));

        Optional<Entity> rejectedReplay = tombstone.createEntity(helper.getLevel(), true);
        helper.assertTrue(
                rejectedReplay.isEmpty(),
                "A consuming resurrection replay must be rejected before returning a duplicate identity"
        );
        helper.assertTrue(
                tombstone.hasEntity(),
                "Rejected duplicate identity replay must not consume the authoritative grave data"
        );
        assertLiveIdentityCount(helper, 1);
        assertInventory(helper, authoritative);

        authoritative.discard();
        assertLiveIdentityCount(helper, 0);

        Optional<Entity> acceptedRestoration = tombstone.createEntity(helper.getLevel(), true);
        helper.assertTrue(
                acceptedRestoration.isPresent(),
                "The preserved grave must restore the NPC after the conflicting live identity is removed"
        );
        helper.assertTrue(
                acceptedRestoration.get() instanceof VillagerEntityMCA,
                "The accepted restoration must remain an MCA villager"
        );
        helper.assertTrue(
                !tombstone.hasEntity(),
                "A successful consuming restoration must clear the grave exactly once"
        );

        VillagerEntityMCA restored = (VillagerEntityMCA) acceptedRestoration.get();
        BlockPos restoredPos = helper.absolutePos(new BlockPos(5, 1, 2));
        restored.setPos(
                restoredPos.getX() + 0.5D,
                restoredPos.getY(),
                restoredPos.getZ() + 0.5D
        );
        helper.assertTrue(
                helper.getLevel().addFreshEntity(restored),
                "The first non-conflicting restoration must enter the server level"
        );
        assertLiveIdentityCount(helper, 1);
        assertInventory(helper, restored);
        helper.succeed();
    }

    private static VillagerEntityMCA createFixture(GameTestHelper helper) {
        VillagerEntityMCA villager = EntitiesMCA.MALE_VILLAGER.create(helper.getLevel());
        helper.assertTrue(villager != null, "MCA villager fixture must be creatable");
        villager.setUUID(FIXTURE_UUID);
        villager.setCustomName(Component.literal(FIXTURE_NAME));
        villager.getInventory().setItem(0, new ItemStack(Items.EMERALD, 3));
        villager.getInventory().setItem(7, new ItemStack(Items.BREAD, 11));
        villager.getInventory().setItem(26, new ItemStack(Items.IRON_SWORD));
        return villager;
    }

    private static void assertLiveIdentityCount(GameTestHelper helper, int expectedCount) {
        BlockPos minimum = helper.absolutePos(new BlockPos(-16, -8, -16));
        BlockPos maximum = helper.absolutePos(new BlockPos(16, 16, 16));
        AABB area = new AABB(
                minimum.getX(),
                minimum.getY(),
                minimum.getZ(),
                maximum.getX() + 1.0D,
                maximum.getY() + 1.0D,
                maximum.getZ() + 1.0D
        );
        long actualCount = helper.getLevel()
                .getEntitiesOfClass(VillagerEntityMCA.class, area)
                .stream()
                .filter(Entity::isAlive)
                .filter(entity -> FIXTURE_UUID.equals(entity.getUUID()))
                .count();
        helper.assertTrue(
                actualCount == expectedCount,
                "Expected " + expectedCount + " live NPC with UUID " + FIXTURE_UUID
                        + ", found " + actualCount
        );
    }

    private static void assertInventory(
            GameTestHelper helper,
            VillagerEntityMCA villager
    ) {
        helper.assertTrue(
                FIXTURE_UUID.equals(villager.getUUID()),
                "Identity replay must preserve the fixture UUID"
        );
        helper.assertTrue(
                FIXTURE_NAME.equals(villager.getName().getString()),
                "Identity replay must preserve the fixture name"
        );
        assertInventoryCount(helper, villager, Items.EMERALD, 3);
        assertInventoryCount(helper, villager, Items.BREAD, 11);
        assertInventoryCount(helper, villager, Items.IRON_SWORD, 1);
    }

    private static void assertInventoryCount(
            GameTestHelper helper,
            VillagerEntityMCA villager,
            Item item,
            int expectedCount
    ) {
        int actualCount = 0;
        for (int slot = 0; slot < villager.getInventory().getContainerSize(); slot++) {
            ItemStack stack = villager.getInventory().getItem(slot);
            if (stack.is(item)) {
                actualCount += stack.getCount();
            }
        }
        helper.assertTrue(
                actualCount == expectedCount,
                "Expected " + expectedCount + " of " + item + ", found " + actualCount
        );
    }
}
