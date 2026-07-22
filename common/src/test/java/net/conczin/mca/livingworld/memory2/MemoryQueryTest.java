package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MemoryQueryTest {
    @Test
    void normalizesSetsTimesAndLimits() {
        UUID npc = UUID.randomUUID();
        UUID participant = UUID.randomUUID();
        LinkedHashSet<UUID> participants = new LinkedHashSet<>();
        participants.add(participant);
        participants.add(null);
        participants.add(participant);

        LinkedHashSet<MemoryEvent.Type> types = new LinkedHashSet<>();
        types.add(MemoryEvent.Type.ACTION);
        types.add(null);
        types.add(MemoryEvent.Type.ACTION);

        MemoryQuery query = new MemoryQuery(npc, participants, types, -10L, 0L, 999, 999);

        assertEquals(Set.of(participant), query.participants());
        assertEquals(Set.of(MemoryEvent.Type.ACTION), query.preferredTypes());
        assertEquals(0L, query.nowGameTime());
        assertEquals(1L, query.recencyHorizonTicks());
        assertEquals(512, query.candidateLimit());
        assertEquals(512, query.maxResults());
    }

    @Test
    void maxResultsCannotExceedCandidateLimitOrDropBelowOne() {
        UUID npc = UUID.randomUUID();

        assertEquals(5, new MemoryQuery(npc, Set.of(), Set.of(), 10L, 100L, 5, 20).maxResults());
        assertEquals(1, new MemoryQuery(npc, Set.of(), Set.of(), 10L, 100L, 5, 0).maxResults());
    }

    @Test
    void requiresNpcId() {
        assertThrows(IllegalArgumentException.class, () -> new MemoryQuery(
                null, Set.of(), Set.of(), 0L, 100L, 10, 5
        ));
    }
}
