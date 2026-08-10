package net.conczin.mca.livingworld.memory2;

import net.conczin.mca.livingworld.relationship.LivingWorldRelationshipDelta;
import net.conczin.mca.livingworld.relationship.LivingWorldRelationshipStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SocialEpistemicPreservationTest {
    @TempDir
    Path tempDir;

    @Test
    void freshRootReloadProducesIdenticalDerivedSocialMetadata() throws Exception {
        Path original = tempDir.resolve("restart-original");
        Path restored = tempDir.resolve("restart-restored");
        UUID npc = id(1);
        UUID player = id(2);
        String statement = "The northern road is safe";

        SemanticMemoryEntry belief = storePlayerBelief(
                original, npc, player, id(10), statement, List.of(player), 50, 1_000L
        );
        LivingWorldRelationshipStore.forWorld(original).applyDelta(
                npc, player, new LivingWorldRelationshipDelta(90, 100, -100, -100), 100
        );

        List<String> before = SemanticMemoryContextProvider.load(original, npc, player, 2_000L);
        assertEquals(1, before.size());
        assertTrue(before.getFirst().contains("effectiveBeliefConfidence=59"));
        assertEquals(50, belief.confidence());

        copyLivingWorldFile(original, restored, "memory2.json");
        copyLivingWorldFile(original, restored, "semantic-memory.json");
        copyLivingWorldFile(original, restored, "relationships.json");

        List<String> after = SemanticMemoryContextProvider.load(restored, npc, player, 2_000L);
        assertEquals(before, after);
        assertEquals(50, SemanticMemoryStore.forWorld(restored).findById(npc, belief.id()).orElseThrow().confidence());
    }

    @Test
    void foreignPrivateClaimsAndRelationshipRecordsConsumeNoPromptSlotsBeforeSocialDerivation() {
        Path world = tempDir.resolve("privacy-pressure");
        UUID npc = id(100);
        UUID currentPlayer = id(101);
        LivingWorldRelationshipStore relationships = LivingWorldRelationshipStore.forWorld(world);

        storePlayerBelief(
                world, npc, currentPlayer, id(102), "Current player's retained claim",
                List.of(currentPlayer), 40, 1_000L
        );
        relationships.applyDelta(
                npc, currentPlayer, new LivingWorldRelationshipDelta(100, 0, 0, 0), 100
        );

        for (int index = 0; index < 24; index++) {
            UUID foreignPlayer = id(200 + index);
            storePlayerBelief(
                    world,
                    npc,
                    foreignPlayer,
                    id(300 + index),
                    "Foreign private claim " + index,
                    List.of(foreignPlayer),
                    100,
                    1_100L + index
            );
            relationships.applyDelta(
                    npc,
                    foreignPlayer,
                    new LivingWorldRelationshipDelta(100, 100, -100, 100),
                    100
            );
        }

        List<String> context = SemanticMemoryContextProvider.load(world, npc, currentPlayer, 3_000L);

        assertEquals(1, context.size(), "foreign-player claims must be excluded before rank-to-6 allocation");
        assertTrue(context.getFirst().contains("Current player's retained claim"));
        assertTrue(context.getFirst().contains("socialEpistemics={trustDelta=10, effectiveBeliefConfidence=50}"));
        assertFalse(context.stream().anyMatch(line -> line.contains("Foreign private claim")));
    }

    @Test
    void trustChangesNeitherContradictionExistenceNorEitherPersistedBelief() {
        Path world = tempDir.resolve("contradiction");
        UUID npc = id(400);
        UUID trustedPlayer = id(401);
        UUID distrustedPlayer = id(402);
        UUID observingPlayer = id(403);

        SemanticMemoryEntry positive = storePlayerBelief(
                world, npc, trustedPlayer, id(410), "The west gate is safe", List.of(), 50, 1_000L
        );
        SemanticMemoryEntry negative = storePlayerBelief(
                world, npc, distrustedPlayer, id(411), "The west gate is not safe", List.of(), 50, 1_100L
        );
        LivingWorldRelationshipStore relationships = LivingWorldRelationshipStore.forWorld(world);
        relationships.applyDelta(npc, trustedPlayer, new LivingWorldRelationshipDelta(100, 0, 0, 0), 100);
        relationships.applyDelta(npc, distrustedPlayer, new LivingWorldRelationshipDelta(-100, 0, 0, 0), 100);

        SemanticContradictionResult contradiction = SemanticContradictionLifecycle.record(
                world, npc, positive.id(), negative.id(), 1_200L, 128
        );
        assertEquals(SemanticContradictionResult.Status.RECORDED, contradiction.status());

        List<String> semanticContext = SemanticMemoryContextProvider.load(world, npc, observingPlayer, 2_000L);
        assertEquals(2, semanticContext.size());
        assertTrue(semanticContext.stream()
                .filter(line -> line.contains("The west gate is safe"))
                .anyMatch(line -> line.contains("effectiveBeliefConfidence=60")));
        assertTrue(semanticContext.stream()
                .filter(line -> line.contains("The west gate is not safe"))
                .anyMatch(line -> line.contains("effectiveBeliefConfidence=40")));

        List<String> disagreement = SemanticContradictionContextProvider.load(world, npc, observingPlayer);
        assertEquals(1, disagreement.size(), "trust must not delete or select a contradiction winner");
        assertTrue(disagreement.getFirst().contains("The west gate is safe"));
        assertTrue(disagreement.getFirst().contains("The west gate is not safe"));

        assertEquals(50, SemanticMemoryStore.forWorld(world).findById(npc, positive.id()).orElseThrow().confidence());
        assertEquals(50, SemanticMemoryStore.forWorld(world).findById(npc, negative.id()).orElseThrow().confidence());
    }

    @Test
    void transformedPlayerOriginRumorKeepsFallibilityAndAddsOnlyDerivedTrustMetadata() {
        Path world = tempDir.resolve("transformation");
        UUID speaker = id(500);
        UUID listener = id(501);
        UUID player = id(502);
        String originalStatement = "The quarry is unsafe. Avoid the lower tunnel.";

        SemanticMemoryEntry origin = storePlayerBelief(
                world, speaker, player, id(510), originalStatement, List.of(player), 50, 1_000L
        );
        NpcKnowledgeTransferResult transfer = NpcKnowledgeTransferLifecycle.transferOmittingTrailingSentence(
                world, speaker, listener, origin.id(), 2_400L, 128, 128
        );
        assertEquals(NpcKnowledgeTransferResult.Status.ADMITTED, transfer.status());
        LivingWorldRelationshipStore.forWorld(world).applyDelta(
                listener, player, new LivingWorldRelationshipDelta(70, 0, 0, 0), 100
        );

        List<String> context = SemanticMemoryContextProvider.load(world, listener, player, 3_000L);

        assertEquals(1, context.size());
        String line = context.getFirst();
        assertTrue(line.contains("provenance=NPC_TOLD"));
        assertTrue(line.contains("fallibility={sourcePath=RESOLVED, sourceDistanceHops=1, transformationsUsed=1}"));
        assertTrue(line.contains("socialEpistemics={trustDelta=7, effectiveBeliefConfidence=57}"));
        assertTrue(line.contains("statement=\"The quarry is unsafe.\""));
        assertFalse(line.contains(player.toString()));
    }

    private static SemanticMemoryEntry storePlayerBelief(
            Path world,
            UUID npc,
            UUID player,
            UUID semanticId,
            String statement,
            List<UUID> scope,
            int confidence,
            long gameTime
    ) {
        MemoryEvent dialogue = DialogueMemoryAdapter.toMemoryEvent(
                npc, player, gameTime, statement, "Understood", 0L
        ).orElseThrow();
        MemoryEventStore.forWorld(world).append(dialogue, 256);
        SemanticMemoryEntry belief = new SemanticMemoryEntry(
                semanticId,
                npc,
                SemanticMemoryEntry.Kind.BELIEF,
                statement,
                scope,
                MemoryEvent.Provenance.PLAYER_TOLD,
                gameTime,
                0L,
                50,
                confidence,
                List.of(dialogue.id())
        );
        SemanticMemoryStore.forWorld(world).append(belief, 256);
        return SemanticMemoryStore.forWorld(world).findById(npc, semanticId).orElseThrow();
    }

    private static void copyLivingWorldFile(Path sourceWorld, Path targetWorld, String fileName) throws IOException {
        Path source = sourceWorld.resolve("livingworld").resolve(fileName);
        Path target = targetWorld.resolve("livingworld").resolve(fileName);
        Files.createDirectories(target.getParent());
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
    }

    private static UUID id(int value) {
        return UUID.fromString("00000000-0000-0000-0000-" + String.format("%012d", value));
    }
}
