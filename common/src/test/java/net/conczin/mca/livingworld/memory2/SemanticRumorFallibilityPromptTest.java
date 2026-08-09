package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SemanticRumorFallibilityPromptTest {
    @TempDir
    Path tempDir;

    @Test
    void selectedNpcToldRumorRendersResolvedSourceDistanceInline() {
        Path world = tempDir.resolve("resolved");
        UUID a = id(1);
        UUID b = id(2);
        UUID player = id(90);
        UUID source = id(101);
        seedFact(world, a, source, "The north bridge is closed", List.of(player));

        NpcKnowledgeTransferResult result = NpcKnowledgeTransferLifecycle.transfer(
                world, a, b, source, 100L, 64, 64);
        assertEquals(NpcKnowledgeTransferResult.Status.ADMITTED, result.status());

        List<String> context = SemanticMemoryContextProvider.load(world, b, player, 200L);

        assertEquals(1, context.size());
        assertEquals(
                "BELIEF | provenance=NPC_TOLD | confidence=50 | fallibility={sourcePath=RESOLVED, sourceDistanceHops=1, transformationsUsed=0} | statement=\"The north bridge is closed\"",
                context.getFirst()
        );
        assertTrue(SemanticMemoryContextFormatter.promptSection(context)
                .contains("Fallibility metadata describes the source path only"));
    }

    @Test
    void retainedRumorWithForgottenDirectEvidenceRendersUnresolvedWithoutInventedDistance() {
        Path world = tempDir.resolve("unresolved");
        UUID a = id(10);
        UUID b = id(11);
        UUID player = id(91);
        UUID source = id(110);
        seedFact(world, a, source, "The orchard gate is broken", List.of(player));

        NpcKnowledgeTransferResult result = NpcKnowledgeTransferLifecycle.transfer(
                world, a, b, source, 100L, 64, 64);
        assertEquals(NpcKnowledgeTransferResult.Status.ADMITTED, result.status());
        MemoryEventStore.forWorld(world).append(strongObservedEvent(b, 1_000L), 1);
        assertTrue(MemoryEventStore.forWorld(world).findById(b, result.evidenceEventId()).isEmpty());

        List<String> context = SemanticMemoryContextProvider.load(world, b, player, 1_100L);

        assertEquals(1, context.size());
        assertEquals(
                "BELIEF | provenance=NPC_TOLD | confidence=50 | fallibility={sourcePath=UNRESOLVED, transformationsUsed=0} | statement=\"The orchard gate is broken\"",
                context.getFirst()
        );
    }

    @Test
    void nonRumorSemanticPromptRenderingRemainsByteCompatibleAndAddsNoFallibilityGuidance() {
        Path world = tempDir.resolve("ordinary");
        UUID npc = id(20);
        UUID player = id(92);
        SemanticMemoryStore.forWorld(world).append(new SemanticMemoryEntry(
                id(201),
                npc,
                SemanticMemoryEntry.Kind.BELIEF,
                "Player says the market opens at dawn",
                List.of(player),
                MemoryEvent.Provenance.PLAYER_TOLD,
                100L,
                0L,
                55,
                73,
                List.of(id(301))
        ), 64);

        List<String> context = SemanticMemoryContextProvider.load(world, npc, player, 200L);

        assertEquals(List.of(
                "BELIEF | provenance=PLAYER_TOLD | confidence=73 | statement=\"Player says the market opens at dawn\""
        ), context);
        assertFalse(SemanticMemoryContextFormatter.promptSection(context)
                .contains("Fallibility metadata describes the source path only"));
    }

    private static void seedFact(
            Path world,
            UUID owner,
            UUID sourceId,
            String statement,
            List<UUID> related
    ) {
        SemanticMemoryStore.forWorld(world).append(new SemanticMemoryEntry(
                sourceId,
                owner,
                SemanticMemoryEntry.Kind.FACT,
                statement,
                related,
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                10L,
                0L,
                90,
                100,
                List.of(UUID.nameUUIDFromBytes((sourceId + "-origin").getBytes(StandardCharsets.UTF_8)))
        ), 64);
    }

    private static MemoryEvent strongObservedEvent(UUID owner, long gameTime) {
        return new MemoryEvent(
                UUID.nameUUIDFromBytes((owner + "-strong-" + gameTime).getBytes(StandardCharsets.UTF_8)),
                owner,
                MemoryEvent.Type.RELATIONSHIP_CHANGE,
                "Strong server-observed event",
                List.of(owner),
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                gameTime,
                0L,
                100,
                100,
                100,
                List.of()
        );
    }

    private static UUID id(int value) {
        return UUID.fromString("00000000-0000-0000-0000-" + String.format("%012d", value));
    }
}
