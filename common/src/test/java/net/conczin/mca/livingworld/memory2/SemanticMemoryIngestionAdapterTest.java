package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SemanticMemoryIngestionAdapterTest {
    @Test
    void convertsEligibleServerObservedEventToDeterministicBoundedFact() {
        UUID npc = UUID.randomUUID();
        UUID player = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        String longStatement = "Observed\n\t" + "x".repeat(300);
        MemoryEvent source = new MemoryEvent(
                sourceId,
                npc,
                MemoryEvent.Type.ACTION,
                longStatement,
                List.of(npc, player),
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                123L,
                1_700_000_000_123L,
                77,
                -10,
                93,
                List.of()
        );

        SemanticMemoryEntry first = SemanticMemoryIngestionAdapter.toFact(source).orElseThrow();
        SemanticMemoryEntry replay = SemanticMemoryIngestionAdapter.toFact(source).orElseThrow();

        assertEquals(first.id(), replay.id());
        assertEquals(npc, first.ownerNpcId());
        assertEquals(SemanticMemoryEntry.Kind.FACT, first.kind());
        assertEquals(MemoryEvent.Provenance.SYSTEM_OBSERVED, first.provenance());
        assertEquals(List.of(npc, player), first.relatedEntities());
        assertEquals(List.of(sourceId), first.sourceEventIds());
        assertEquals(123L, first.gameTime());
        assertEquals(1_700_000_000_123L, first.createdAtEpochMillis());
        assertEquals(77, first.importance());
        assertEquals(93, first.confidence());
        assertEquals(240, first.statement().codePointCount(0, first.statement().length()));
        assertFalse(first.statement().contains("\n"));
        assertFalse(first.statement().contains("\t"));
    }

    @Test
    void acceptsOnlyEligibleAuthoritativeEventTypes() {
        UUID npc = UUID.randomUUID();
        UUID participant = UUID.randomUUID();

        for (MemoryEvent.Type type : List.of(
                MemoryEvent.Type.ACTION,
                MemoryEvent.Type.OBSERVATION,
                MemoryEvent.Type.RELATIONSHIP_CHANGE
        )) {
            assertTrue(SemanticMemoryIngestionAdapter.toFact(event(npc, participant, type,
                    MemoryEvent.Provenance.SYSTEM_OBSERVED)).isPresent());
        }

        assertTrue(SemanticMemoryIngestionAdapter.toFact(event(npc, participant,
                MemoryEvent.Type.DIALOGUE, MemoryEvent.Provenance.SYSTEM_OBSERVED)).isEmpty());
        assertTrue(SemanticMemoryIngestionAdapter.toFact(event(npc, participant,
                MemoryEvent.Type.ACTION, MemoryEvent.Provenance.PLAYER_TOLD)).isEmpty());
        assertTrue(SemanticMemoryIngestionAdapter.toFact(null).isEmpty());
    }

    @Test
    void createsExplicitSourcedBeliefWithoutWeakeningProvenance() {
        UUID npc = UUID.randomUUID();
        UUID player = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        SemanticBeliefSource source = new SemanticBeliefSource(
                npc,
                "  Player   said this is their home.  ",
                List.of(npc, player, player),
                MemoryEvent.Provenance.PLAYER_TOLD,
                50L,
                1_700_000_000_050L,
                65,
                70,
                List.of(sourceId, sourceId)
        );

        SemanticMemoryEntry first = SemanticMemoryIngestionAdapter.toBelief(source);
        SemanticMemoryEntry replay = SemanticMemoryIngestionAdapter.toBelief(source);

        assertEquals(first.id(), replay.id());
        assertEquals(SemanticMemoryEntry.Kind.BELIEF, first.kind());
        assertEquals(MemoryEvent.Provenance.PLAYER_TOLD, first.provenance());
        assertEquals("Player said this is their home.", first.statement());
        assertEquals(List.of(npc, player), first.relatedEntities());
        assertEquals(List.of(sourceId), first.sourceEventIds());
    }

    @Test
    void rejectsAuthoritativeOrUnsourcedBeliefInput() {
        UUID npc = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class, () -> new SemanticBeliefSource(
                npc,
                "Observed fact",
                List.of(npc),
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                1L,
                1L,
                50,
                100,
                List.of(sourceId)
        ));

        assertThrows(IllegalArgumentException.class, () -> new SemanticBeliefSource(
                npc,
                "Unsourced claim",
                List.of(npc),
                MemoryEvent.Provenance.INFERRED,
                1L,
                1L,
                50,
                50,
                List.of()
        ));
    }

    private static MemoryEvent event(
            UUID npc,
            UUID participant,
            MemoryEvent.Type type,
            MemoryEvent.Provenance provenance
    ) {
        return new MemoryEvent(
                UUID.randomUUID(),
                npc,
                type,
                "Eligible event",
                List.of(npc, participant),
                provenance,
                10L,
                20L,
                50,
                0,
                80,
                List.of()
        );
    }
}
