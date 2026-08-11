package net.conczin.mca.gametest;

import net.conczin.mca.entity.VillagerEntityMCA;
import net.conczin.mca.entity.ai.relationship.Personality;
import net.conczin.mca.livingworld.context.PersonalitySocialContextRenderer;
import net.conczin.mca.livingworld.context.PersonalitySocialSnapshot;
import net.conczin.mca.livingworld.context.PersonalitySocialSnapshotCapture;
import net.conczin.mca.livingworld.relationship.NpcSocialDelta;
import net.conczin.mca.livingworld.relationship.NpcSocialGraphStore;
import net.conczin.mca.livingworld.relationship.NpcSocialState;
import net.conczin.mca.registry.EntitiesMCA;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class PersonalitySocialSnapshotGameTests implements FabricGameTest {
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void liveMcaPersonalityAndDirectedPairAreCapturedReadOnly(GameTestHelper helper) throws Exception {
        VillagerEntityMCA source = helper.spawn(EntitiesMCA.MALE_VILLAGER, 2, 1, 2);
        VillagerEntityMCA counterpart = helper.spawn(EntitiesMCA.FEMALE_VILLAGER, 4, 1, 2);
        source.getVillagerBrain().setPersonality(Personality.FRIENDLY);
        counterpart.getVillagerBrain().setPersonality(Personality.CRABBY);

        Path worldRoot = Files.createTempDirectory("villaigence-personality-social-gametest-");
        NpcSocialGraphStore store = NpcSocialGraphStore.forWorld(worldRoot);
        store.applyDelta(source.getUUID(), counterpart.getUUID(), new NpcSocialDelta(12, 5, 1, 9), 100);
        store.applyDelta(counterpart.getUUID(), source.getUUID(), new NpcSocialDelta(-4, 8, 3, -6), 100);

        Path graphFile = worldRoot.resolve("livingworld").resolve("npc-social-graph.json");
        byte[] beforeCapture = Files.readAllBytes(graphFile);

        PersonalitySocialSnapshot sourceToCounterpart = PersonalitySocialSnapshotCapture.capture(
                worldRoot,
                source,
                counterpart
        );
        PersonalitySocialSnapshot counterpartToSource = PersonalitySocialSnapshotCapture.capture(
                worldRoot,
                counterpart,
                source
        );
        PersonalitySocialSnapshot personalityOnly = PersonalitySocialSnapshotCapture.capture(
                worldRoot,
                source,
                null
        );

        helper.assertTrue(
                sourceToCounterpart.personalityToken().equals("friendly"),
                "Live source VillagerBrain personality must become the canonical snapshot token"
        );
        helper.assertTrue(
                counterpartToSource.personalityToken().equals("crabby"),
                "Reverse live MCA personality must be captured independently"
        );
        helper.assertTrue(
                sourceToCounterpart.directedSocialState().equals(new NpcSocialState(12, 5, 1, 9)),
                "A->B must expose exactly the persisted directed pair state"
        );
        helper.assertTrue(
                counterpartToSource.directedSocialState().equals(new NpcSocialState(-4, 8, 3, -6)),
                "B->A must remain independent from A->B"
        );
        helper.assertTrue(
                !personalityOnly.hasCounterpart() && personalityOnly.directedSocialState().equals(NpcSocialState.NEUTRAL),
                "Player-to-NPC style capture without an NPC counterpart must contain no social edge"
        );
        helper.assertTrue(
                source.getVillagerBrain().getPersonality() == Personality.FRIENDLY
                        && counterpart.getVillagerBrain().getPersonality() == Personality.CRABBY,
                "Read-only capture must not mutate tracked MCA Personality"
        );
        helper.assertTrue(
                java.util.Arrays.equals(beforeCapture, Files.readAllBytes(graphFile)),
                "Read-only capture must leave npc-social-graph.json byte-identical"
        );

        List<String> rendered = PersonalitySocialContextRenderer.render(sourceToCounterpart);
        String joined = String.join("\n", rendered);
        helper.assertTrue(rendered.size() == 2, "Directed renderer must remain fixed-size");
        helper.assertTrue(!joined.contains(source.getUUID().toString()), "Rendered context must not expose source UUID");
        helper.assertTrue(!joined.contains(counterpart.getUUID().toString()), "Rendered context must not expose counterpart UUID");
        helper.succeed();
    }
}
