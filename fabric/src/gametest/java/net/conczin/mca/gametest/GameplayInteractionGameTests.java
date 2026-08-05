package net.conczin.mca.gametest;

import net.conczin.mca.entity.VillagerEntityMCA;
import net.conczin.mca.entity.ai.Memories;
import net.conczin.mca.registry.EntitiesMCA;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public final class GameplayInteractionGameTests implements FabricGameTest {
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void specialGiftDispatchPreservesInteractionResultConsumption(GameTestHelper helper) {
        VillagerEntityMCA villager = helper.spawn(EntitiesMCA.MALE_VILLAGER, 2, 1, 2);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Memories memories = new Memories(
                villager.getVillagerBrain(),
                helper.getLevel().getGameTime(),
                player.getUUID()
        );

        assertGiftDecision(helper, villager, player, memories, GameplayTestItems.PASS_GIFT, 3);
        assertGiftDecision(helper, villager, player, memories, GameplayTestItems.FAIL_GIFT, 3);
        assertGiftDecision(helper, villager, player, memories, GameplayTestItems.CONSUME_GIFT, 2);

        helper.succeed();
    }

    private static void assertGiftDecision(
            GameTestHelper helper,
            VillagerEntityMCA villager,
            ServerPlayer player,
            Memories memories,
            GameplayTestItems.FixtureSpecialGift gift,
            int expectedCount
    ) {
        gift.resetAndGetCalls();
        ItemStack stack = new ItemStack(gift, 3);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);

        villager.getRelationships().giveGift(player, memories);

        helper.assertTrue(gift.calls() == 1,
                "Special gift handler must be invoked exactly once for " + gift.result()
                        + ", found " + gift.calls());
        helper.assertTrue(stack.getCount() == expectedCount,
                "Special gift result " + gift.result() + " must leave " + expectedCount
                        + " items, found " + stack.getCount());
    }
}
