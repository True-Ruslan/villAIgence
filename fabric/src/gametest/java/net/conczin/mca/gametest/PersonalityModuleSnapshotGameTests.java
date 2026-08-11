package net.conczin.mca.gametest;

import net.conczin.mca.entity.VillagerEntityMCA;
import net.conczin.mca.entity.ai.chatAI.OpenAIChatAI;
import net.conczin.mca.entity.ai.chatAI.modules.PersonalityModule;
import net.conczin.mca.entity.ai.relationship.Personality;
import net.conczin.mca.registry.EntitiesMCA;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

import java.util.ArrayList;
import java.util.List;

public final class PersonalityModuleSnapshotGameTests implements FabricGameTest {
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void snapshotBaseOmitsPersonalityWhileLegacyOutputRemainsUnchanged(GameTestHelper helper) {
        VillagerEntityMCA villager = helper.spawn(EntitiesMCA.MALE_VILLAGER, 2, 1, 2);
        villager.getVillagerBrain().setPersonality(Personality.FRIENDLY);

        List<String> legacy = new ArrayList<>();
        PersonalityModule.apply(legacy, villager, null);

        List<String> snapshotBase = new ArrayList<>();
        PersonalityModule.applySnapshotBase(snapshotBase, villager, null);

        String mood = OpenAIChatAI.translate(villager.getVillagerBrain().getMood().getName());
        helper.assertTrue(
                legacy.get(1).equals("$villager is friendly and " + mood + ". "),
                "Legacy PersonalityModule output must preserve personality + mood wording"
        );
        helper.assertTrue(
                snapshotBase.get(1).equals("$villager is " + mood + ". "),
                "Snapshot base must retain mood while omitting the personality token"
        );
        helper.assertTrue(
                legacy.size() == snapshotBase.size(),
                "Snapshot de-duplication must not remove identity/age/profession context lines"
        );
        helper.assertTrue(
                legacy.get(0).equals(snapshotBase.get(0)),
                "Conversation identity line must remain unchanged on the snapshot path"
        );
        for (int i = 2; i < legacy.size(); i++) {
            helper.assertTrue(
                    legacy.get(i).equals(snapshotBase.get(i)),
                    "Age/profession context must remain unchanged at index " + i
            );
        }
        helper.assertTrue(
                snapshotBase.stream().noneMatch(line -> line.contains("friendly")),
                "Snapshot base must not duplicate the typed personality token"
        );
        helper.succeed();
    }
}
