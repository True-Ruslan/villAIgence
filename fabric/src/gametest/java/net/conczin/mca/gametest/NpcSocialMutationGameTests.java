package net.conczin.mca.gametest;

import net.conczin.mca.entity.VillagerEntityMCA;
import net.conczin.mca.livingworld.memory2.ServerLevelNpcIdentityAuthority;
import net.conczin.mca.registry.EntitiesMCA;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.UUID;

public final class NpcSocialMutationGameTests implements FabricGameTest {
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void serverIdentityAuthorityAcceptsOnlyLiveMcaNpc(GameTestHelper helper) {
        VillagerEntityMCA villager = helper.spawn(EntitiesMCA.MALE_VILLAGER, 2, 1, 2);
        Entity cow = helper.spawn(EntityType.COW, 4, 1, 2);
        ServerLevelNpcIdentityAuthority authority = new ServerLevelNpcIdentityAuthority(helper.getLevel());

        helper.assertTrue(
                authority.isNpc(villager.getUUID()),
                "Live MCA villager must be accepted as an NPC social identity"
        );
        helper.assertTrue(
                !authority.isNpc(cow.getUUID()),
                "Non-MCA entity must not be accepted as an NPC social identity"
        );
        helper.assertTrue(
                !authority.isNpc(UUID.fromString("12345678-1234-1234-1234-123456789abc")),
                "Unknown UUID must not be accepted as an NPC social identity"
        );
        helper.succeed();
    }
}
