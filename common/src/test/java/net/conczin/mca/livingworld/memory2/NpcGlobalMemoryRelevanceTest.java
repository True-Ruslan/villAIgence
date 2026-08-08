package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NpcGlobalMemoryRelevanceTest {
    private static final UUID NPC = UUID.fromString("00000000-0000-0000-0000-000000000801");
    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000802");

    @Test
    void npcGlobalSemanticMemoryHasFullRelevanceForPlayerScopedQuery() {
        SemanticMemoryEntry globalFact = new SemanticMemoryEntry(
                UUID.fromString("00000000-0000-0000-0000-000000000803"),
                NPC,
                SemanticMemoryEntry.Kind.FACT,
                "NPC-global observed fact",
                List.of(),
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                100L,
                1_700_000_000_100L,
                90,
                100,
                List.of(UUID.fromString("00000000-0000-0000-0000-000000000804"))
        );
        SemanticMemoryQuery query = new SemanticMemoryQuery(
                NPC,
                Set.of(PLAYER),
                200L,
                168_000L,
                32,
                6
        );

        RankedSemanticMemory ranked = SemanticMemoryRetriever.rank(globalFact, query);

        assertEquals(100, ranked.relevanceScore());
    }

    @Test
    void npcGlobalEpisodicMemoryHasFullParticipantRelevanceForPlayerScopedQuery() {
        MemoryEvent globalObservation = new MemoryEvent(
                UUID.fromString("00000000-0000-0000-0000-000000000805"),
                NPC,
                MemoryEvent.Type.OBSERVATION,
                "NPC-global observed event",
                List.of(NPC),
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                100L,
                1_700_000_000_100L,
                90,
                25,
                100,
                List.of()
        );
        MemoryQuery query = new MemoryQuery(
                NPC,
                Set.of(PLAYER),
                Set.of(),
                200L,
                168_000L,
                32,
                6
        );

        RankedMemory ranked = MemoryRetriever.rank(globalObservation, query);

        assertEquals(100, ranked.relevanceScore());
    }
}
