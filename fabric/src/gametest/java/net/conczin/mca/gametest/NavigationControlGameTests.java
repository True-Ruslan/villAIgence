package net.conczin.mca.gametest;

import net.conczin.mca.entity.VillagerEntityMCA;
import net.conczin.mca.registry.EntitiesMCA;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public final class NavigationControlGameTests implements FabricGameTest {
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 600)
    public void freshDryNpcBuildsTheSameLandPath(GameTestHelper helper) {
        for (int x = 1; x <= 6; x++) {
            for (int z = 4; z <= 6; z++) {
                helper.getLevel().setBlockAndUpdate(
                        helper.absolutePos(new BlockPos(x, 1, z)),
                        Blocks.STONE.defaultBlockState()
                );
            }
        }

        VillagerEntityMCA villager = helper.spawn(EntitiesMCA.MALE_VILLAGER, 1, 2, 4);
        villager.setPersistenceRequired();
        BlockPos landTarget = helper.absolutePos(new BlockPos(6, 2, 5));

        helper.startSequence()
                .thenExecuteAfter(2, () -> helper.assertTrue(
                        villager.getNavigation().moveTo(
                                landTarget.getX() + 0.5D,
                                landTarget.getY(),
                                landTarget.getZ() + 0.5D,
                                1.1D
                        ),
                        "Fresh dry NPC must build the control path: "
                                + navigationState(villager, landTarget)
                ))
                .thenWaitUntil(() -> {
                    helper.assertTrue(villager.isAlive(),
                            "Fresh dry navigation control NPC must remain alive");
                    helper.assertTrue(!villager.isInWater(),
                            "Fresh dry navigation control NPC must remain dry");
                    helper.assertTrue(isNear(villager, landTarget),
                            "Fresh dry NPC must reach the control target: "
                                    + navigationState(villager, landTarget));
                })
                .thenSucceed();
    }

    private static String navigationState(VillagerEntityMCA villager, BlockPos target) {
        return "pos=" + villager.position()
                + ", target=" + Vec3.atCenterOf(target)
                + ", inWater=" + villager.isInWater()
                + ", navigationDone=" + villager.getNavigation().isDone();
    }

    private static boolean isNear(VillagerEntityMCA villager, BlockPos target) {
        double dx = villager.getX() - (target.getX() + 0.5D);
        double dy = villager.getY() - target.getY();
        double dz = villager.getZ() - (target.getZ() + 0.5D);
        return dx * dx + dy * dy + dz * dz <= 2.25D;
    }
}
