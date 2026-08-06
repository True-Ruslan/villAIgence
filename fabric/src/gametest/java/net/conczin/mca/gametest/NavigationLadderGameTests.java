package net.conczin.mca.gametest;

import net.conczin.mca.entity.ai.navigation.MCAGroundPathNavigation;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class NavigationLadderGameTests implements FabricGameTest {
    private static final double MAX_TERMINAL_DISTANCE_SQUARED = 6.25D;

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 800)
    public void ladderAscentAndDescentReachBoundedTerminalStates(GameTestHelper helper) {
        buildLadderTower(helper);

        ControlledNavigationMob mob = spawnControlledMob(helper, new BlockPos(3, 1, 5));
        MCAGroundPathNavigation navigation = (MCAGroundPathNavigation) mob.getNavigation();
        BlockPos upperTarget = helper.absolutePos(new BlockPos(3, 6, 4));
        BlockPos lowerTarget = helper.absolutePos(new BlockPos(3, 1, 5));
        boolean[] ascentControlObserved = {false};
        boolean[] descentControlObserved = {false};

        helper.startSequence()
                .thenExecuteAfter(2, () -> helper.assertTrue(
                        startNavigation(mob, upperTarget),
                        "Controlled mob must build a path to the upper ladder platform: "
                                + navigationState(mob, upperTarget, navigation)
                ))
                .thenWaitUntil(() -> {
                    ascentControlObserved[0] |= navigation.isControllingClimbable();
                    helper.assertTrue(mob.isAlive(),
                            "Ladder ascent fixture mob must remain alive");
                    helper.assertTrue(reachesUpperTerminal(mob, upperTarget),
                            "Production navigation must reach the upper ladder terminal: "
                                    + navigationState(mob, upperTarget, navigation));
                    helper.assertTrue(ascentControlObserved[0],
                            "Ladder ascent must activate production climbable movement control");
                })
                .thenExecuteAfter(2, () -> helper.assertTrue(
                        startNavigation(mob, lowerTarget),
                        "Controlled mob must build a path back down the ladder: "
                                + navigationState(mob, lowerTarget, navigation)
                ))
                .thenWaitUntil(() -> {
                    descentControlObserved[0] |= navigation.isControllingClimbable();
                    helper.assertTrue(mob.isAlive(),
                            "Ladder descent fixture mob must remain alive");
                    helper.assertTrue(reachesLowerTerminal(mob, lowerTarget),
                            "Production navigation must reach the lower ladder terminal: "
                                    + navigationState(mob, lowerTarget, navigation));
                    helper.assertTrue(descentControlObserved[0],
                            "Ladder descent must activate production climbable movement control");
                    assertLadderIntact(helper);
                })
                .thenSucceed();
    }

    private static void buildLadderTower(GameTestHelper helper) {
        for (int x = 0; x <= 6; x++) {
            for (int z = 0; z <= 6; z++) {
                setBlock(helper, new BlockPos(x, 0, z), Blocks.STONE);
            }
        }

        for (int y = 1; y <= 5; y++) {
            for (int x = 2; x <= 4; x++) {
                setBlock(helper, new BlockPos(x, y, 2), Blocks.STONE);
            }
            setLadder(helper, new BlockPos(3, y, 3));
        }

        for (int x = 2; x <= 4; x++) {
            for (int z = 4; z <= 5; z++) {
                setBlock(helper, new BlockPos(x, 5, z), Blocks.STONE);
            }
        }
    }

    private static void setLadder(GameTestHelper helper, BlockPos relativePos) {
        BlockState ladder = Blocks.LADDER.defaultBlockState()
                .setValue(LadderBlock.FACING, Direction.SOUTH);
        helper.getLevel().setBlockAndUpdate(helper.absolutePos(relativePos), ladder);
        helper.assertTrue(
                helper.getLevel().getBlockState(helper.absolutePos(relativePos)).is(Blocks.LADDER),
                "Ladder fixture block must survive at " + relativePos
        );
    }

    private static void assertLadderIntact(GameTestHelper helper) {
        for (int y = 1; y <= 5; y++) {
            helper.assertTrue(
                    helper.getLevel().getBlockState(
                            helper.absolutePos(new BlockPos(3, y, 3))
                    ).is(Blocks.LADDER),
                    "Ladder navigation must not remove the climbable block at y=" + y
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
                "Ladder fixture mob must be added to the GameTest level");
        helper.assertTrue(mob.getNavigation() instanceof MCAGroundPathNavigation,
                "Ladder fixture must use production MCAGroundPathNavigation");
        return mob;
    }

    private static boolean startNavigation(PathfinderMob mob, BlockPos target) {
        return mob.getNavigation().moveTo(
                target.getX() + 0.5D,
                target.getY(),
                target.getZ() + 0.5D,
                1.0D
        );
    }

    private static boolean reachesUpperTerminal(PathfinderMob mob, BlockPos target) {
        return mob.getY() >= target.getY() - 1.0D && reachesTerminalRange(mob, target);
    }

    private static boolean reachesLowerTerminal(PathfinderMob mob, BlockPos target) {
        return mob.getY() <= target.getY() + 1.0D && reachesTerminalRange(mob, target);
    }

    private static boolean reachesTerminalRange(PathfinderMob mob, BlockPos target) {
        double dx = mob.getX() - (target.getX() + 0.5D);
        double dy = mob.getY() - target.getY();
        double dz = mob.getZ() - (target.getZ() + 0.5D);
        return mob.getNavigation().isDone()
                && dx * dx + dy * dy + dz * dz <= MAX_TERMINAL_DISTANCE_SQUARED;
    }

    private static String navigationState(
            PathfinderMob mob,
            BlockPos target,
            MCAGroundPathNavigation navigation
    ) {
        return "pos=" + mob.position()
                + ", target=" + Vec3.atCenterOf(target)
                + ", velocity=" + mob.getDeltaMovement()
                + ", navigationDone=" + mob.getNavigation().isDone()
                + ", controllingClimbable=" + navigation.isControllingClimbable();
    }

    private static void setBlock(
            GameTestHelper helper,
            BlockPos relativePos,
            Block block
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
            // Test-owned path only. Autonomous goals must not replace ladder navigation.
        }
    }
}
