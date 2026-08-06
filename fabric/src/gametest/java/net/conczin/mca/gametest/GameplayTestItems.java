package net.conczin.mca.gametest;

import net.conczin.mca.entity.VillagerEntityMCA;
import net.conczin.mca.item.SpecialCaseGift;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;

import java.util.concurrent.atomic.AtomicInteger;

/** Test-only registered items for exercising the real special-gift dispatch path. */
public final class GameplayTestItems implements ModInitializer {
    public static final FixtureSpecialGift PASS_GIFT =
            new FixtureSpecialGift(InteractionResult.PASS);
    public static final FixtureSpecialGift FAIL_GIFT =
            new FixtureSpecialGift(InteractionResult.FAIL);
    public static final FixtureSpecialGift CONSUME_GIFT =
            new FixtureSpecialGift(InteractionResult.CONSUME);

    @Override
    public void onInitialize() {
        register("pass_gift", PASS_GIFT);
        register("fail_gift", FAIL_GIFT);
        register("consume_gift", CONSUME_GIFT);
    }

    private static void register(String path, Item item) {
        Registry.register(
                BuiltInRegistries.ITEM,
                ResourceLocation.fromNamespaceAndPath("mca-acceptance-test", path),
                item
        );
    }

    public static final class FixtureSpecialGift extends Item implements SpecialCaseGift {
        private final InteractionResult result;
        private final AtomicInteger calls = new AtomicInteger();

        private FixtureSpecialGift(InteractionResult result) {
            super(new Item.Properties());
            this.result = result;
        }

        @Override
        public InteractionResult handle(ServerPlayer player, VillagerEntityMCA villager) {
            calls.incrementAndGet();
            return result;
        }

        InteractionResult result() {
            return result;
        }

        int resetAndGetCalls() {
            calls.set(0);
            return calls.get();
        }

        int calls() {
            return calls.get();
        }
    }
}
