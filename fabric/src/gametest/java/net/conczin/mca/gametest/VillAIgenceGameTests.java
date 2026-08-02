package net.conczin.mca.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

public final class VillAIgenceGameTests {
    @GameTest
    public void serverGameTestHarnessIsActive(GameTestHelper helper) {
        throw new AssertionError("RED: Fabric server GameTest harness is active");
    }
}
