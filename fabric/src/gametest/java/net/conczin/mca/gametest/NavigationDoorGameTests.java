package net.conczin.mca.gametest;

import net.conczin.mca.entity.ai.navigation.MCAGroundPathNavigation;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;

public final class NavigationDoorGameTests implements FabricGameTest {
    private static final BlockPos RELATIVE_DOOR_LOWER = new BlockPos(5, 1, 3);
    private static final double MAX_TERMINAL_DISTANCE_SQUARED = 6.25D;

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 600)
    public void closedDoorOpensForProductionNavigationAndClosesAfterPassage(
            GameTestHelper helper
    ) {
        buildCorridor(helper);

        ControlledDoorMob mob = spawnControlledMob(helper, new BlockPos(1, 1, 3));
        BlockPos target = helper.absolutePos(new BlockPos(9, 1, 3));
        boolean[] openedObserved = {false};

        helper.startSequence()
                .thenExecuteAfter(2, () -> helper.assertTrue(
                        startNavigation(mob, target),
                        "Controlled mob must build a path through the closed wooden door: "
                                + navigationState(mob, target, helper)
                ))
                .thenWaitUntil(() -> {
                    openedObserved[0] |= isDoorOpen(helper);
                    helper.assertTrue(mob.isAlive(),
                            "Door fixture mob must remain alive");
                    helper.assertTrue(reachesTerminalRange(mob, target),
                            "Production navigation must pass through the door and reach the target: "
                                    + navigationState(mob, target, helper));
                    helper.assertTrue(openedObserved[0],
                            "Door passage must visibly open the real door block");
                    helper.assertTrue(
                            mob.getX() > helper.absolutePos(new BlockPos(6, 1, 3)).getX() + 0.5D,
                            "Mob must finish on the far side of the door: "
                                    + navigationState(mob, target, helper)
                    );
                })
                .thenWaitUntil(() -> helper.assertTrue(
                        !isDoorOpen(helper),
                        "Close-behind goal must return the real door to its closed state"
                ))
                .thenExecute(() -> assertDoorIntact(helper))
                .thenSucceed();
    }

    private static void buildCorridor(GameTestHelper helper) {
        for (int x = 0; x <= 10; x++) {
            for (int z = 2; z <= 4; z++) {
                setBlock(helper, new BlockPos(x, 0, z), Blocks.STONE);
            }
            setBlock(helper, new BlockPos(x, 1, 2), Blocks.STONE);
            setBlock(helper, new BlockPos(x, 2, 2), Blocks.STONE);
            setBlock(helper, new BlockPos(x, 1, 4), Blocks.STONE);
            setBlock(helper, new BlockPos(x, 2, 4), Blocks.STONE);
        }

        BlockState lower = Blocks.OAK_DOOR.defaultBlockState()
                .setValue(DoorBlock.FACING, Direction.EAST)
                .setValue(DoorBlock.OPEN, false)
                .setValue(DoorBlock.HINGE, DoorHingeSide.LEFT)
                .setValue(DoorBlock.POWERED, false)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
        BlockState upper = lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER);
        helper.getLevel().setBlockAndUpdate(helper.absolutePos(RELATIVE_DOOR_LOWER), lower);
        helper.getLevel().setBlockAndUpdate(helper.absolutePos(RELATIVE_DOOR_LOWER.above()), upper);
        assertDoorIntact(helper);
    }

    private static void assertDoorIntact(GameTestHelper helper) {
        BlockState lower = helper.getLevel().getBlockState(
                helper.absolutePos(RELATIVE_DOOR_LOWER)
        );
        BlockState upper = helper.getLevel().getBlockState(
                helper.absolutePos(RELATIVE_DOOR_LOWER.above())
        );
        helper.assertTrue(lower.is(Blocks.OAK_DOOR),
                "Door fixture must retain the lower oak door block");
        helper.assertTrue(upper.is(Blocks.OAK_DOOR),
                "Door fixture must retain the upper oak door block");
        helper.assertTrue(lower.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER,
                "Door fixture lower half must remain LOWER");
        helper.assertTrue(upper.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER,
                "Door fixture upper half must remain UPPER");
    }

    private static boolean isDoorOpen(GameTestHelper helper) {
        BlockState state = helper.getLevel().getBlockState(
                helper.absolutePos(RELATIVE_DOOR_LOWER)
        );
        return state.is(Blocks.OAK_DOOR) && state.getValue(DoorBlock.OPEN);
    }

    private static ControlledDoorMob spawnControlledMob(
            GameTestHelper helper,
            BlockPos relativePos
    ) {
        ControlledDoorMob mob = new ControlledDoorMob(helper.getLevel());
        BlockPos absolutePos = helper.absolutePos(relativePos);
        mob.setPos(
                absolutePos.getX() + 0.5D,
                absolutePos.getY(),
                absolutePos.getZ() + 0.5D
        );
        mob.setPersistenceRequired();
        helper.assertTrue(helper.getLevel().addFreshEntity(mob),
                "Door fixture mob must be added to the GameTest level");
        helper.assertTrue(mob.getNavigation() instanceof MCAGroundPathNavigation,
                "Door fixture must use production MCAGroundPathNavigation");
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
            GameTestHelper helper
    ) {
        return "pos=" + mob.position()
                + ", target=" + Vec3.atCenterOf(target)
                + ", velocity=" + mob.getDeltaMovement()
                + ", navigationDone=" + mob.getNavigation().isDone()
                + ", doorOpen=" + isDoorOpen(helper);
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

    private static final class ControlledDoorMob extends PathfinderMob {
        private ControlledDoorMob(Level level) {
            super(EntityType.VILLAGER, level);
        }

        @Override
        protected PathNavigation createNavigation(Level level) {
            MCAGroundPathNavigation navigation = new MCAGroundPathNavigation(this, level);
            navigation.setCanOpenDoors(true);
            return navigation;
        }

        @Override
        protected void registerGoals() {
            goalSelector.addGoal(1, new OpenDoorGoal(this, true));
        }
    }
}
