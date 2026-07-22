package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SemanticMemoryEntryTest {
    @Test
    void factRequiresSystemObservedProvenance() {
        assertThrows(IllegalArgumentException.class, () -> entry(
                SemanticMemoryEntry.Kind.FACT,
                MemoryEvent.Provenance.PLAYER_TOLD,
                50,
                50,
                List.of(),
                List.of()
        ));
    }

    @Test
    void beliefCannotUseSystemObservedProvenance() {
        assertThrows(IllegalArgumentException.class, () -> entry(
                SemanticMemoryEntry.Kind.BELIEF,
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                50,
                100,
                List.of(),
                List.of()
        ));
    }

    @Test
    void normalizesScoresTimesAndDefensivelyCopiesIds() {
        UUID related = UUID.randomUUID();
        UUID source = UUID.randomUUID();
        List<UUID> relatedEntities = new ArrayList<>(List.of(related, related));
        List<UUID> sourceEventIds = new ArrayList<>(List.of(source, source));

        SemanticMemoryEntry entry = new SemanticMemoryEntry(
                UUID.randomUUID(),
                UUID.randomUUID(),
                SemanticMemoryEntry.Kind.BELIEF,
                "  The player claims to be a blacksmith.  ",
                relatedEntities,
                MemoryEvent.Provenance.PLAYER_TOLD,
                -10L,
                -20L,
                150,
                -5,
                sourceEventIds
        );

        relatedEntities.clear();
        sourceEventIds.clear();

        assertEquals("The player claims to be a blacksmith.", entry.statement());
        assertEquals(List.of(related), entry.relatedEntities());
        assertEquals(List.of(source), entry.sourceEventIds());
        assertEquals(0L, entry.gameTime());
        assertEquals(0L, entry.createdAtEpochMillis());
        assertEquals(100, entry.importance());
        assertEquals(0, entry.confidence());
    }

    private static SemanticMemoryEntry entry(
            SemanticMemoryEntry.Kind kind,
            MemoryEvent.Provenance provenance,
            int importance,
            int confidence,
            List<UUID> related,
            List<UUID> sourceIds
    ) {
        return new SemanticMemoryEntry(
                UUID.randomUUID(),
                UUID.randomUUID(),
                kind,
                "statement",
                related,
                provenance,
                100L,
                1_700_000_000_000L,
                importance,
                confidence,
                sourceIds
        );
    }
}
