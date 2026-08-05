package net.conczin.mca.gametest;

import net.conczin.mca.entity.VillagerEntityMCA;
import net.conczin.mca.entity.ai.Memories;
import net.conczin.mca.item.SpecialCaseGift;
import net.conczin.mca.registry.EntitiesMCA;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.GameType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.concurrent.atomic.AtomicInteger;

public final class GameplayInteractionGameTests implements FabricGameTest {
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void specialGiftDispatchPreservesInteractionResultConsumption(GameTestHelper helper) {
        VillagerEntityMCA villager = helper.spawn(EntitiesMCA.MALE_VILLAGER, 2, 1, 2);
        ServerPlayer player = helper.makeMockPlayer(GameType.SURVIVAL);
        Memories memories = new Memories(
                villager.getVillagerBrain(),
                helper.getLevel().getGameTime(),
                player.getUUID()
        );

        assertGiftDecision(helper, villager, player, memories, InteractionResult.PASS, 3);
        assertGiftDecision(helper, villager, player, memories, InteractionResult.FAIL, 3);
        assertGiftDecision(helper, villager, player, memories, InteractionResult.CONSUME, 2);

        helper.succeed();
    }

    private static void assertGiftDecision(
            GameTestHelper helper,
            VillagerEntityMCA villager,
            ServerPlayer player,
            Memories memories,
            InteractionResult result,
            int expectedCount
    ) {
        AtomicInteger calls = new AtomicInteger();
        ItemStack stack = new ItemStack(new FixtureSpecialGift(result, calls), 3);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);

        villager.getRelationships().giveGift(player, memories);

        helper.assertTrue(calls.get() == 1,
                "Special gift handler must be invoked exactly once for " + result
                        + ", found " + calls.get());
        helper.assertTrue(stack.getCount() == expectedCount,
                "Special gift result " + result + " must leave " + expectedCount
                        + " items, found " + stack.getCount());
    }

    private static final class FixtureSpecialGift extends Item implements SpecialCaseGift {
        private final InteractionResult result;
        private final AtomicInteger calls;

        private FixtureSpecialGift(InteractionResult result, AtomicInteger calls) {
            super(new Item.Properties());
            this.result = result;
            this.calls = calls;
        }

        @Override
        public InteractionResult handle(ServerPlayer player, VillagerEntityMCA villager) {
            calls.incrementAndGet();
            return result;
        }
    }
}
