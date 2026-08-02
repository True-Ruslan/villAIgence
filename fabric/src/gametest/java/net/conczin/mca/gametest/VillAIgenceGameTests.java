package net.conczin.mca.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

public final class VillAIgenceGameTests implements FabricGameTest {
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void serverGameTestHarnessIsActive(GameTestHelper helper) {
        throw new AssertionError("RED: Fabric server GameTest harness is active");
    }
}
