package net.conczin.mca.gametest;

import net.conczin.mca.entity.VillagerEntityMCA;
import net.conczin.mca.entity.ai.ArcherMoveControl;
import net.conczin.mca.entity.ai.ArcherMoveControlOwner;
import net.conczin.mca.entity.ai.brain.tasks.ArcherMovementTask;
import net.conczin.mca.gametest.bridge.MobMoveControlBridge;
import net.conczin.mca.registry.EntitiesMCA;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public final class MountedArcherControlGameTests implements FabricGameTest {
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void mountedControllerReplacementRetainsStableArcherControllerAndProjectile(
            GameTestHelper helper
    ) {
        ServerLevel level = helper.getLevel();
        VillagerEntityMCA villager = helper.spawn(EntitiesMCA.MALE_VILLAGER, 2, 1, 2);
        Object transformedVillager = villager;
        helper.assertTrue(transformedVillager instanceof ArcherMoveControlOwner,
                "VillagerEntityMCA must expose the injected stable archer controller");
        helper.assertTrue(transformedVillager instanceof MobMoveControlBridge,
                "GameTest must expose the real Mob.moveControl field through its test-only bridge");
        ArcherMoveControlOwner owner = (ArcherMoveControlOwner) transformedVillager;
        MobMoveControlBridge bridge = (MobMoveControlBridge) transformedVillager;
        ArcherMoveControl stable = owner.mca$getArcherMoveControl();
        helper.assertTrue(bridge.mca$getActiveMoveControl() == stable,
                "Fresh MCA villager field must contain its captured ArcherMoveControl");
        helper.assertTrue(villager.getMoveControl() == stable,
                "Fresh MCA villager must expose its captured ArcherMoveControl as active");

        Horse horse = helper.spawn(EntityType.HORSE, 3, 1, 2);
        helper.assertTrue(villager.startRiding(horse, true),
                "MCA archer must mount the fixture horse");
        assertReplacementKeepsStable(helper, level, villager, bridge, owner, stable);

        Zombie target = helper.spawn(EntityType.ZOMBIE, 8, 1, 2);
        villager.setItemInHand(villager.getDominantHand(), new ItemStack(Items.BOW));
        villager.performRangedAttack(target, 1.0F);
        List<AbstractArrow> arrows = level.getEntitiesOfClass(
                AbstractArrow.class,
                villager.getBoundingBox().inflate(16.0D)
        );
        helper.assertTrue(arrows.size() == 1,
                "Mounted archer must emit exactly one real projectile, found " + arrows.size());
        helper.assertTrue(arrows.getFirst().getOwner() == villager,
                "Mounted archer projectile must remain owned by the MCA NPC");

        villager.stopRiding();
        bridge.mca$setActiveMoveControl(stable);
        helper.assertTrue(bridge.mca$getActiveMoveControl() == stable,
                "Dismount fixture must restore the stable active controller");
        helper.assertTrue(owner.mca$getArcherMoveControl() == stable,
                "Dismount must retain the constructor-captured archer controller");

        helper.assertTrue(villager.startRiding(horse, true),
                "MCA archer must support a second mount cycle");
        assertReplacementKeepsStable(helper, level, villager, bridge, owner, stable);

        helper.succeed();
    }

    private static void assertReplacementKeepsStable(
            GameTestHelper helper,
            ServerLevel level,
            VillagerEntityMCA villager,
            MobMoveControlBridge bridge,
            ArcherMoveControlOwner owner,
            ArcherMoveControl stable
    ) {
        MoveControl vehicleReplacement = new MoveControl(villager);
        bridge.mca$setActiveMoveControl(vehicleReplacement);

        helper.assertTrue(bridge.mca$getActiveMoveControl() == vehicleReplacement,
                "Test bridge must replace the actual Mob.moveControl field");
        helper.assertTrue(owner.mca$getArcherMoveControl() == stable,
                "Mounted archer control lookup must return the stable controller");

        new ProbeArcherMovementTask().stopForTest(level, villager);

        helper.assertTrue(bridge.mca$getActiveMoveControl() == vehicleReplacement,
                "Archer movement task must not overwrite the vehicle-owned controller");
        helper.assertTrue(owner.mca$getArcherMoveControl() == stable,
                "Production archer task redirect must retain the stable ArcherMoveControl");
    }

    private static final class ProbeArcherMovementTask
            extends ArcherMovementTask<VillagerEntityMCA> {
        private ProbeArcherMovementTask() {
            super(16);
        }

        private void stopForTest(ServerLevel level, VillagerEntityMCA villager) {
            super.stop(level, villager, level.getGameTime());
        }
    }
}
