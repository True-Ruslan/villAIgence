package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MemoryEventTest {
    @Test
    void normalizesListsAndClampsScores() {
        UUID id = UUID.randomUUID();
        UUID owner = UUID.randomUUID();
        UUID participantA = UUID.randomUUID();
        UUID participantB = UUID.randomUUID();

        MemoryEvent event = new MemoryEvent(
                id,
                owner,
                MemoryEvent.Type.RELATIONSHIP_CHANGE,
                "  Ruslan protected my family.  ",
                List.of(participantA, participantA, participantB),
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                120L,
                1_700_000_000_000L,
                150,
                -150,
                120,
                List.of("  protected family  ", "", "protected family")
        );

        assertEquals("Ruslan protected my family.", event.summary());
        assertEquals(List.of(participantA, participantB), event.participants());
        assertEquals(100, event.importance());
        assertEquals(-100, event.emotionalWeight());
        assertEquals(100, event.confidence());
        assertEquals(List.of("protected family"), event.relationshipReasons());
    }

    @Test
    void rejectsMissingRequiredFields() {
        UUID id = UUID.randomUUID();
        UUID owner = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class, () -> new MemoryEvent(
                null, owner, MemoryEvent.Type.DIALOGUE, "summary", List.of(),
                MemoryEvent.Provenance.PLAYER_TOLD, 0L, 0L, 50, 0, 50, List.of()
        ));
        assertThrows(IllegalArgumentException.class, () -> new MemoryEvent(
                id, null, MemoryEvent.Type.DIALOGUE, "summary", List.of(),
                MemoryEvent.Provenance.PLAYER_TOLD, 0L, 0L, 50, 0, 50, List.of()
        ));
        assertThrows(IllegalArgumentException.class, () -> new MemoryEvent(
                id, owner, null, "summary", List.of(),
                MemoryEvent.Provenance.PLAYER_TOLD, 0L, 0L, 50, 0, 50, List.of()
        ));
        assertThrows(IllegalArgumentException.class, () -> new MemoryEvent(
                id, owner, MemoryEvent.Type.DIALOGUE, "   ", List.of(),
                MemoryEvent.Provenance.PLAYER_TOLD, 0L, 0L, 50, 0, 50, List.of()
        ));
        assertThrows(IllegalArgumentException.class, () -> new MemoryEvent(
                id, owner, MemoryEvent.Type.DIALOGUE, "summary", List.of(),
                null, 0L, 0L, 50, 0, 50, List.of()
        ));
    }
}
