package net.conczin.mca.livingworld.memory2;

import net.conczin.mca.livingworld.relationship.LivingWorldRelationshipDelta;
import net.conczin.mca.livingworld.relationship.LivingWorldRelationshipStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SocialEpistemicContextTest {
    @TempDir
    Path tempDir;

    @Test
    void formatterKeepsPersistedConfidenceAndAddsDerivedSocialMetadataWithoutPlayerUuid() {
        UUID npc = id(1);
        UUID player = id(2);
        SemanticMemoryEntry belief = belief(
                id(3), npc, player, "The orchard is safe", 50, 1_000L, List.of(id(4))
        );
        SocialEpistemicState social = new SocialEpistemicState(player, -55, -5, 45);

        String line = SemanticMemoryContextFormatter.formatEntry(belief, null, social);

        assertTrue(line.contains("BELIEF | provenance=PLAYER_TOLD | confidence=50"));
        assertTrue(line.contains("socialEpistemics={trustDelta=-5, effectiveBeliefConfidence=45}"));
        assertTrue(line.contains("statement=\"The orchard is safe\""));
        assertFalse(line.contains(player.toString()));
        assertEquals(50, belief.confidence());
    }

    @Test
    void promptGuidanceAppearsOnlyWhenValidatedSocialMetadataIsPresent() {
        UUID npc = id(10);
        UUID player = id(11);
        SemanticMemoryEntry belief = belief(
                id(12), npc, player, "The bell tower is stable", 60, 1_000L, List.of(id(13))
        );

        String plain = SemanticMemoryContextFormatter.promptSection(List.of(
                SemanticMemoryContextFormatter.formatEntry(belief)
        ));
        String social = SemanticMemoryContextFormatter.promptSection(List.of(
                SemanticMemoryContextFormatter.formatEntry(
                        belief, null, new SocialEpistemicState(player, 80, 8, 68)
                )
        ));

        assertFalse(plain.contains("Social epistemic metadata"));
        assertTrue(social.contains("Social epistemic metadata is the NPC's personal trust adjustment"));
        assertTrue(social.contains("never turns BELIEF into FACT"));
    }

    @Test
    void providerAppliesTrustOnlyAfterExistingSelectionAndDoesNotPullLowRankedBeliefIntoTopSix() {
        Path world = tempDir.resolve("post-ranking");
        UUID npc = id(20);
        LivingWorldRelationshipStore relationships = LivingWorldRelationshipStore.forWorld(world);
        List<UUID> sourcePlayers = new ArrayList<>();
        List<String> statements = new ArrayList<>();

        for (int index = 0; index < 7; index++) {
            UUID sourcePlayer = id(30 + index);
            sourcePlayers.add(sourcePlayer);
            String statement = "Global claim " + index;
            statements.add(statement);
            MemoryEvent source = DialogueMemoryAdapter.toMemoryEvent(
                    npc, sourcePlayer, 1_000L + index, statement, "Noted", 0L
            ).orElseThrow();
            MemoryEventStore.forWorld(world).append(source, 128);
            SemanticMemoryStore.forWorld(world).append(new SemanticMemoryEntry(
                    id(100 + index), npc, SemanticMemoryEntry.Kind.BELIEF, statement, List.of(),
                    MemoryEvent.Provenance.PLAYER_TOLD, 1_000L + index, 0L,
                    90 - index, 90 - index, List.of(source.id())
            ), 128);
        }

        relationships.applyDelta(npc, sourcePlayers.get(0), new LivingWorldRelationshipDelta(-100, 0, 0, 0), 100);
        relationships.applyDelta(npc, sourcePlayers.get(6), new LivingWorldRelationshipDelta(100, 0, 0, 0), 100);

        List<String> context = SemanticMemoryContextProvider.load(world, npc, null, 2_000L);

        assertEquals(SemanticMemoryContextProvider.MAX_RESULTS, context.size());
        assertTrue(context.stream().anyMatch(line -> line.contains("Global claim 0")));
        assertFalse(context.stream().anyMatch(line -> line.contains("Global claim 6")),
                "trust must not pull an otherwise unselected low-ranked BELIEF into a prompt slot");
        assertTrue(context.stream()
                .filter(line -> line.contains("Global claim 0"))
                .allMatch(line -> line.contains("effectiveBeliefConfidence=80")));
    }

    @Test
    void playerOriginRumorKeepsExistingFallibilityBesideSocialMetadata() {
        Path world = tempDir.resolve("rumor");
        UUID speaker = id(200);
        UUID listener = id(201);
        UUID player = id(202);
        MemoryEvent source = DialogueMemoryAdapter.toMemoryEvent(
                speaker, player, 1_000L, "The western mine is unsafe", "Understood", 0L
        ).orElseThrow();
        MemoryEventStore.forWorld(world).append(source, 128);
        SemanticMemoryEntry origin = belief(
                id(203), speaker, player, "The western mine is unsafe", 50, 1_000L, List.of(source.id())
        );
        SemanticMemoryStore.forWorld(world).append(origin, 128);
        NpcKnowledgeTransferResult transfer = NpcKnowledgeTransferLifecycle.transfer(
                world, speaker, listener, origin.id(), 2_400L, 128, 128
        );
        assertEquals(NpcKnowledgeTransferResult.Status.ADMITTED, transfer.status());
        LivingWorldRelationshipStore.forWorld(world).applyDelta(
                listener, player, new LivingWorldRelationshipDelta(80, 0, 0, 0), 100
        );

        List<String> context = SemanticMemoryContextProvider.load(world, listener, player, 3_000L);

        assertEquals(1, context.size());
        String line = context.getFirst();
        assertTrue(line.contains("provenance=NPC_TOLD"));
        assertTrue(line.contains("fallibility={sourcePath=RESOLVED, sourceDistanceHops=1, transformationsUsed=0}"));
        assertTrue(line.contains("socialEpistemics={trustDelta=8, effectiveBeliefConfidence=58}"));
        assertFalse(line.contains(player.toString()));
    }

    @Test
    void factLineNeverReceivesSocialEpistemicMetadata() {
        UUID npc = id(300);
        UUID player = id(301);
        SemanticMemoryEntry fact = new SemanticMemoryEntry(
                id(302), npc, SemanticMemoryEntry.Kind.FACT, "The sun is up", List.of(player),
                MemoryEvent.Provenance.SYSTEM_OBSERVED, 1_000L, 0L, 100, 100, List.of(id(303))
        );

        String line = SemanticMemoryContextFormatter.formatEntry(
                fact, null, new SocialEpistemicState(player, 100, 10, 100)
        );

        assertFalse(line.contains("socialEpistemics="), "formatter must not annotate FACT even with supplied state");
        assertTrue(line.contains("FACT | provenance=SYSTEM_OBSERVED | confidence=100"));
    }

    private static SemanticMemoryEntry belief(
            UUID entryId,
            UUID npc,
            UUID player,
            String statement,
            int confidence,
            long gameTime,
            List<UUID> sourceEventIds
    ) {
        return new SemanticMemoryEntry(
                entryId, npc, SemanticMemoryEntry.Kind.BELIEF, statement, List.of(player),
                MemoryEvent.Provenance.PLAYER_TOLD, gameTime, 0L, 50, confidence, sourceEventIds
        );
    }

    private static UUID id(int value) {
        return UUID.fromString("00000000-0000-0000-0000-" + String.format("%012d", value));
    }
}
