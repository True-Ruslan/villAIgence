package net.conczin.mca.gametest;

import net.conczin.mca.entity.VillagerEntityMCA;
import net.conczin.mca.entity.ai.ArcherMoveControl;
import net.conczin.mca.entity.ai.ArcherMoveControlOwner;
import net.conczin.mca.entity.ai.relationship.Gender;
import net.conczin.mca.registry.EntitiesMCA;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
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
        MountedArcherFixture villager = new MountedArcherFixture(level);
        BlockPos villagerPos = helper.absolutePos(new BlockPos(2, 1, 2));
        villager.moveTo(
                villagerPos.getX() + 0.5D,
                villagerPos.getY(),
                villagerPos.getZ() + 0.5D,
                0.0F,
                0.0F
        );
        helper.assertTrue(level.addFreshEntity(villager),
                "Mounted archer fixture must be added to the server level");
        helper.assertTrue(villager instanceof ArcherMoveControlOwner,
                "VillagerEntityMCA must expose the injected stable archer controller");
        ArcherMoveControlOwner owner = (ArcherMoveControlOwner) villager;
        ArcherMoveControl stable = owner.mca$getArcherMoveControl();
        helper.assertTrue(villager.getMoveControl() == stable,
                "Fresh MCA villager must start with its captured ArcherMoveControl active");

        Horse horse = helper.spawn(EntityType.HORSE, 3, 1, 2);
        helper.assertTrue(villager.startRiding(horse, true),
                "MCA archer must mount the fixture horse");
        assertReplacementKeepsStable(helper, villager, owner, stable);

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
        villager.replaceActiveMoveControl(stable);
        helper.assertTrue(owner.mca$getArcherMoveControl() == stable,
                "Dismount must retain the constructor-captured archer controller");

        helper.assertTrue(villager.startRiding(horse, true),
                "MCA archer must support a second mount cycle");
        assertReplacementKeepsStable(helper, villager, owner, stable);

        helper.succeed();
    }

    private static void assertReplacementKeepsStable(
            GameTestHelper helper,
            MountedArcherFixture villager,
            ArcherMoveControlOwner owner,
            ArcherMoveControl stable
    ) {
        MoveControl vehicleReplacement = new MoveControl(villager);
        villager.replaceActiveMoveControl(vehicleReplacement);

        helper.assertTrue(villager.getMoveControl() == vehicleReplacement,
                "Fixture must reproduce a vehicle-owned active MoveControl replacement");
        helper.assertTrue(owner.mca$getArcherMoveControl() == stable,
                "Mounted archer control lookup must return the stable controller");
        helper.assertTrue(owner.mca$getArcherMoveControl() != villager.getMoveControl(),
                "Archer state control must remain independent from vehicle movement control");
    }

    private static final class MountedArcherFixture extends VillagerEntityMCA {
        private MountedArcherFixture(ServerLevel level) {
            super(EntitiesMCA.MALE_VILLAGER, level, Gender.MALE);
        }

        private void replaceActiveMoveControl(MoveControl replacement) {
            this.moveControl = replacement;
        }
    }
}
