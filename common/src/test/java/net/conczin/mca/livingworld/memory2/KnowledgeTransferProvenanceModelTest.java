package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class KnowledgeTransferProvenanceModelTest {
    private static final UUID ORIGIN_NPC = UUID.fromString("00000000-0000-0000-0000-000000090001");
    private static final UUID ORIGIN_ENTRY = UUID.fromString("00000000-0000-0000-0000-000000090002");
    private static final UUID LISTENER = UUID.fromString("00000000-0000-0000-0000-000000090003");
    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000090004");
    private static final UUID EVIDENCE = UUID.fromString("00000000-0000-0000-0000-000000090005");

    @Test
    void ordinaryMemoryEventKeepsTransferProvenanceAbsent() {
        MemoryEvent event = new MemoryEvent(
                UUID.fromString("00000000-0000-0000-0000-000000090006"),
                ORIGIN_NPC,
                MemoryEvent.Type.OBSERVATION,
                "Observed bridge state",
                List.of(ORIGIN_NPC),
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                10L,
                0L,
                50,
                0,
                100,
                List.of()
        );

        assertNull(event.knowledgeTransferProvenance());
    }

    @Test
    void provenanceModelRetainsExactOriginAndOrderedHops() {
        KnowledgeTransferProvenance.Origin origin = new KnowledgeTransferProvenance.Origin(
                ORIGIN_NPC,
                ORIGIN_ENTRY,
                SemanticMemoryEntry.Kind.FACT,
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                "Bridge destroyed",
                List.of(PLAYER)
        );
        KnowledgeTransferProvenance.Hop hop = new KnowledgeTransferProvenance.Hop(
                ORIGIN_NPC,
                LISTENER,
                ORIGIN_ENTRY,
                EVIDENCE,
                100L
        );
        KnowledgeTransferProvenance provenance = new KnowledgeTransferProvenance(origin, List.of(hop));

        assertEquals(origin, provenance.origin());
        assertEquals(List.of(hop), provenance.hops());
        assertEquals(EVIDENCE, provenance.hops().getFirst().evidenceEventId());
    }
}
