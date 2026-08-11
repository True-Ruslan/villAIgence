package net.conczin.mca.gametest;

import net.conczin.mca.entity.VillagerEntityMCA;
import net.conczin.mca.entity.ai.relationship.Personality;
import net.conczin.mca.livingworld.context.NpcPairDisposition;
import net.conczin.mca.livingworld.context.PersonalityDialogueStyle;
import net.conczin.mca.livingworld.context.PersonalitySocialDialogueGuidanceRenderer;
import net.conczin.mca.livingworld.context.PersonalitySocialInfluence;
import net.conczin.mca.livingworld.context.PersonalitySocialInfluencePolicy;
import net.conczin.mca.livingworld.context.PersonalitySocialSnapshot;
import net.conczin.mca.livingworld.context.PersonalitySocialSnapshotCapture;
import net.conczin.mca.livingworld.relationship.NpcSocialDelta;
import net.conczin.mca.livingworld.relationship.NpcSocialGraphStore;
import net.conczin.mca.registry.EntitiesMCA;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public final class PersonalitySocialDialogueInfluenceGameTests implements FabricGameTest {
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void liveMcaPersonalityProducesBoundedDialogueGuidanceReadOnly(GameTestHelper helper) throws Exception {
        VillagerEntityMCA villager = helper.spawn(EntitiesMCA.MALE_VILLAGER, 2, 1, 2);
        villager.getVillagerBrain().setPersonality(Personality.FRIENDLY);
        Path worldRoot = Files.createTempDirectory("villaigence-dialogue-influence-gametest-");

        PersonalitySocialSnapshot snapshot = PersonalitySocialSnapshotCapture.capture(worldRoot, villager, null);
        PersonalitySocialInfluence influence = PersonalitySocialInfluencePolicy.evaluate(snapshot);
        List<String> guidance = PersonalitySocialDialogueGuidanceRenderer.render(influence);

        helper.assertTrue(
                influence.personalityStyle() == PersonalityDialogueStyle.WARM,
                "Live FRIENDLY MCA personality must map to WARM dialogue style"
        );
        helper.assertTrue(
                influence.pairDisposition() == NpcPairDisposition.NEUTRAL,
                "Player dialogue without an NPC counterpart must not invent pair disposition"
        );
        helper.assertTrue(
                guidance.size() == 1,
                "Personality-only dialogue influence must emit exactly one bounded guidance line"
        );
        helper.assertTrue(
                villager.getVillagerBrain().getPersonality() == Personality.FRIENDLY,
                "Influence evaluation must not mutate live MCA Personality"
        );
        helper.assertTrue(
                !Files.exists(worldRoot.resolve("livingworld").resolve("npc-social-graph.json")),
                "Personality-only influence must not create NPC social persistence"
        );
        helper.succeed();
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void directedPairInfluenceIsAsymmetricBoundedAndReadOnly(GameTestHelper helper) throws Exception {
        VillagerEntityMCA source = helper.spawn(EntitiesMCA.MALE_VILLAGER, 2, 1, 2);
        VillagerEntityMCA counterpart = helper.spawn(EntitiesMCA.FEMALE_VILLAGER, 4, 1, 2);
        source.getVillagerBrain().setPersonality(Personality.FRIENDLY);
        counterpart.getVillagerBrain().setPersonality(Personality.CRABBY);

        Path worldRoot = Files.createTempDirectory("villaigence-directed-dialogue-influence-gametest-");
        NpcSocialGraphStore store = NpcSocialGraphStore.forWorld(worldRoot);
        store.applyDelta(source.getUUID(), counterpart.getUUID(), new NpcSocialDelta(0, 0, 75, 0), 100);
        store.applyDelta(counterpart.getUUID(), source.getUUID(), new NpcSocialDelta(60, 0, 0, 60), 100);
        Path graphFile = worldRoot.resolve("livingworld").resolve("npc-social-graph.json");
        byte[] graphBefore = Files.readAllBytes(graphFile);

        PersonalitySocialInfluence sourceToCounterpart = PersonalitySocialInfluencePolicy.evaluate(
                PersonalitySocialSnapshotCapture.capture(worldRoot, source, counterpart)
        );
        PersonalitySocialInfluence counterpartToSource = PersonalitySocialInfluencePolicy.evaluate(
                PersonalitySocialSnapshotCapture.capture(worldRoot, counterpart, source)
        );
        List<String> sourceGuidance = PersonalitySocialDialogueGuidanceRenderer.render(sourceToCounterpart);
        List<String> counterpartGuidance = PersonalitySocialDialogueGuidanceRenderer.render(counterpartToSource);

        helper.assertTrue(
                sourceToCounterpart.personalityStyle() == PersonalityDialogueStyle.WARM,
                "Source FRIENDLY personality must remain WARM"
        );
        helper.assertTrue(
                sourceToCounterpart.pairDisposition() == NpcPairDisposition.FEARFUL,
                "Exact A->B fear state must produce FEARFUL disposition"
        );
        helper.assertTrue(
                counterpartToSource.personalityStyle() == PersonalityDialogueStyle.GRUFF,
                "Counterpart CRABBY personality must remain GRUFF"
        );
        helper.assertTrue(
                counterpartToSource.pairDisposition() == NpcPairDisposition.AFFILIATIVE,
                "Exact B->A trust/affinity state must remain independent and AFFILIATIVE"
        );
        helper.assertTrue(
                sourceGuidance.size() == 2 && counterpartGuidance.size() == 2,
                "Personality plus non-neutral pair disposition must remain bounded to two guidance lines"
        );
        helper.assertTrue(
                source.getVillagerBrain().getPersonality() == Personality.FRIENDLY
                        && counterpart.getVillagerBrain().getPersonality() == Personality.CRABBY,
                "Dialogue influence must not mutate either live MCA Personality"
        );
        helper.assertTrue(
                Arrays.equals(graphBefore, Files.readAllBytes(graphFile)),
                "Dialogue influence capture/render must leave npc-social-graph.json byte-identical"
        );
        helper.succeed();
    }
}
