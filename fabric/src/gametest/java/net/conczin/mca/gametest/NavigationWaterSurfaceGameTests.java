package net.conczin.mca.gametest;

import net.conczin.mca.entity.ai.navigation.MCAGroundPathNavigation;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

public final class NavigationWaterSurfaceGameTests implements FabricGameTest {
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 80)
    public void waterAwareSurfaceUsesActualFluidTags(GameTestHelper helper) {
        BlockPos relativeWaterPos = new BlockPos(2, 1, 2);
        BlockPos absoluteWaterPos = helper.absolutePos(relativeWaterPos);
        helper.setBlock(new BlockPos(2, 0, 2), Blocks.STONE);
        helper.setBlock(relativeWaterPos, Blocks.WATER);

        SurfaceProbeMob mob = new SurfaceProbeMob(helper.getLevel());
        mob.setPos(
                absoluteWaterPos.getX() + 0.5D,
                absoluteWaterPos.getY(),
                absoluteWaterPos.getZ() + 0.5D
        );
        mob.setPersistenceRequired();
        helper.assertTrue(helper.getLevel().addFreshEntity(mob),
                "Surface probe mob must be added to the GameTest server level");
        helper.assertTrue(mob.getNavigation() instanceof MCAGroundPathNavigation,
                "Surface probe must use production MCAGroundPathNavigation");

        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    helper.assertTrue(mob.isInWater(),
                            "Surface probe must observe the real water fluid tag");
                    MCAGroundPathNavigation navigation =
                            (MCAGroundPathNavigation) mob.getNavigation();
                    int expectedSurfaceY = absoluteWaterPos.getY() + 1;
                    int actualSurfaceY = navigation.mca$getWaterAwareSurfaceY();
                    helper.assertTrue(actualSurfaceY == expectedSurfaceY,
                            "Water-aware navigation must resolve the first dry block above the fluid column: expected "
                                    + expectedSurfaceY + ", found " + actualSurfaceY);
                })
                .thenSucceed();
    }

    private static final class SurfaceProbeMob extends PathfinderMob {
        private SurfaceProbeMob(Level level) {
            super(EntityType.VILLAGER, level);
        }

        @Override
        protected PathNavigation createNavigation(Level level) {
            MCAGroundPathNavigation navigation = new MCAGroundPathNavigation(this, level);
            navigation.setCanFloat(true);
            return navigation;
        }

        @Override
        protected void registerGoals() {
            // No autonomous behavior is required for a deterministic surface calculation probe.
        }
    }
}
