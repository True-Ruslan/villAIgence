package net.conczin.mca.gametest;

import net.conczin.mca.entity.VillagerEntityMCA;
import net.conczin.mca.entity.ai.navigation.MCAGroundPathNavigation;
import net.conczin.mca.registry.EntitiesMCA;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

public final class VillAIgenceGameTests implements FabricGameTest {
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
}
