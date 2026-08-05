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
import net.minecraft.world.phys.Vec3;

public final class NavigationObstacleGameTests implements FabricGameTest {
    private static final double MIN_REROUTE_DEVIATION = 1.5D;

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 400)
    public void obstacleRerouteReachesBoundedTerminalState(GameTestHelper helper) {
        buildArena(helper);

        ControlledNavigationMob mob = spawnControlledMob(helper, new BlockPos(1, 1, 4));
        BlockPos target = helper.absolutePos(new BlockPos(9, 1, 4));
        double routeCenterZ = mob.getZ();
        double[] maximumDeviation = {0.0D};

        helper.startSequence()
                .thenExecuteAfter(2, () -> helper.assertTrue(
                        startNavigation(mob, target),
                        "Controlled mob must build a path around the intact obstacle: "
                                + navigationState(mob, target)
                ))
                .thenWaitUntil(() -> {
                    maximumDeviation[0] = Math.max(
                            maximumDeviation[0],
                            Math.abs(mob.getZ() - routeCenterZ)
                    );
                    helper.assertTrue(mob.isAlive(),
                            "Obstacle fixture mob must remain alive");
                    helper.assertTrue(isNear(mob, target),
                            "Production navigation must reach the target around the obstacle: "
                                    + navigationState(mob, target));
                    helper.assertTrue(maximumDeviation[0] >= MIN_REROUTE_DEVIATION,
                            "Path must visibly leave the blocked direct lane; maximum Z deviation was "
                                    + maximumDeviation[0]);
                    assertObstacleIntact(helper);
                })
                .thenSucceed();
    }

    private static void buildArena(GameTestHelper helper) {
        for (int x = 0; x <= 10; x++) {
            for (int z = 0; z <= 8; z++) {
                setBlock(helper, new BlockPos(x, 0, z), Blocks.STONE);
            }
        }

        for (int z = 2; z <= 6; z++) {
            setBlock(helper, new BlockPos(5, 1, z), Blocks.STONE);
            setBlock(helper, new BlockPos(5, 2, z), Blocks.STONE);
        }
    }

    private static void assertObstacleIntact(GameTestHelper helper) {
        for (int z = 2; z <= 6; z++) {
            helper.assertTrue(
                    helper.getLevel().getBlockState(
                            helper.absolutePos(new BlockPos(5, 1, z))
                    ).is(Blocks.STONE),
                    "Obstacle reroute must not remove or pass through the lower wall at z=" + z
            );
            helper.assertTrue(
                    helper.getLevel().getBlockState(
                            helper.absolutePos(new BlockPos(5, 2, z))
                    ).is(Blocks.STONE),
                    "Obstacle reroute must not remove or pass through the upper wall at z=" + z
            );
        }
    }

    private static ControlledNavigationMob spawnControlledMob(
            GameTestHelper helper,
            BlockPos relativePos
    ) {
        ControlledNavigationMob mob = new ControlledNavigationMob(helper.getLevel());
        BlockPos absolutePos = helper.absolutePos(relativePos);
        mob.setPos(
                absolutePos.getX() + 0.5D,
                absolutePos.getY(),
                absolutePos.getZ() + 0.5D
        );
        mob.setPersistenceRequired();
        helper.assertTrue(helper.getLevel().addFreshEntity(mob),
                "Obstacle fixture mob must be added to the GameTest level");
        helper.assertTrue(mob.getNavigation() instanceof MCAGroundPathNavigation,
                "Obstacle fixture must use production MCAGroundPathNavigation");
        return mob;
    }

    private static boolean startNavigation(PathfinderMob mob, BlockPos target) {
        return mob.getNavigation().moveTo(
                target.getX() + 0.5D,
                target.getY(),
                target.getZ() + 0.5D,
                1.1D
        );
    }

    private static boolean isNear(PathfinderMob mob, BlockPos target) {
        double dx = mob.getX() - (target.getX() + 0.5D);
        double dy = mob.getY() - target.getY();
        double dz = mob.getZ() - (target.getZ() + 0.5D);
        return dx * dx + dy * dy + dz * dz <= 2.25D;
    }

    private static String navigationState(PathfinderMob mob, BlockPos target) {
        return "pos=" + mob.position()
                + ", target=" + Vec3.atCenterOf(target)
                + ", velocity=" + mob.getDeltaMovement()
                + ", navigationDone=" + mob.getNavigation().isDone();
    }

    private static void setBlock(
            GameTestHelper helper,
            BlockPos relativePos,
            net.minecraft.world.level.block.Block block
    ) {
        helper.getLevel().setBlockAndUpdate(
                helper.absolutePos(relativePos),
                block.defaultBlockState()
        );
    }

    private static final class ControlledNavigationMob extends PathfinderMob {
        private ControlledNavigationMob(Level level) {
            super(EntityType.VILLAGER, level);
        }

        @Override
        protected PathNavigation createNavigation(Level level) {
            return new MCAGroundPathNavigation(this, level);
        }

        @Override
        protected void registerGoals() {
            // Test-owned path only. Autonomous goals must not replace the assigned route.
        }
    }
}
