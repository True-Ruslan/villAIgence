package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KnowledgeTransferProvenanceModelTest {
    private static final UUID ORIGIN_NPC = UUID.fromString("00000000-0000-0000-0000-000000090001");
    private static final UUID ORIGIN_ENTRY = UUID.fromString("00000000-0000-0000-0000-000000090002");
    private static final UUID LISTENER = UUID.fromString("00000000-0000-0000-0000-000000090003");
    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000090004");
    private static final UUID EVIDENCE = UUID.fromString("00000000-0000-0000-0000-000000090005");
    private static final UUID OTHER = UUID.fromString("00000000-0000-0000-0000-000000090007");

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
        KnowledgeTransferProvenance.Hop hop = hop();
        KnowledgeTransferProvenance provenance = new KnowledgeTransferProvenance(origin, List.of(hop));

        assertEquals(origin, provenance.origin());
        assertEquals(List.of(hop), provenance.hops());
        assertEquals(EVIDENCE, provenance.hops().getFirst().evidenceEventId());
    }

    @Test
    void snapshotsMutableCollectionInputsAndDoesNotExposeMutableAuthorityState() {
        ArrayList<UUID> related = new ArrayList<>(List.of(PLAYER));
        ArrayList<KnowledgeTransferProvenance.Hop> hops = new ArrayList<>(List.of(hop()));
        KnowledgeTransferProvenance.Origin origin = new KnowledgeTransferProvenance.Origin(
                ORIGIN_NPC,
                ORIGIN_ENTRY,
                SemanticMemoryEntry.Kind.FACT,
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                "Bridge destroyed",
                related
        );
        KnowledgeTransferProvenance provenance = new KnowledgeTransferProvenance(origin, hops);

        related.add(OTHER);
        hops.clear();

        assertEquals(List.of(PLAYER), provenance.origin().relatedEntities());
        assertEquals(List.of(hop()), provenance.hops());
        assertThrows(UnsupportedOperationException.class,
                () -> provenance.origin().relatedEntities().add(OTHER));
        assertThrows(UnsupportedOperationException.class,
                () -> provenance.hops().clear());
    }

    @Test
    void defensiveSnapshotsPreserveMalformedNullsForFailClosedPolicyInsteadOfRepairingThem() {
        ArrayList<UUID> related = new ArrayList<>();
        related.add(PLAYER);
        related.add(null);
        ArrayList<KnowledgeTransferProvenance.Hop> hops = new ArrayList<>();
        hops.add(hop());
        hops.add(null);

        KnowledgeTransferProvenance provenance = new KnowledgeTransferProvenance(
                new KnowledgeTransferProvenance.Origin(
                        ORIGIN_NPC,
                        ORIGIN_ENTRY,
                        SemanticMemoryEntry.Kind.FACT,
                        MemoryEvent.Provenance.SYSTEM_OBSERVED,
                        "Bridge destroyed",
                        related
                ),
                hops
        );

        assertEquals(2, provenance.origin().relatedEntities().size());
        assertNull(provenance.origin().relatedEntities().get(1));
        assertEquals(2, provenance.hops().size());
        assertNull(provenance.hops().get(1));
        assertFalse(KnowledgeTransferProvenancePolicy.valid(provenance));
    }

    private static KnowledgeTransferProvenance.Hop hop() {
        return new KnowledgeTransferProvenance.Hop(
                ORIGIN_NPC,
                LISTENER,
                ORIGIN_ENTRY,
                EVIDENCE,
                100L
        );
    }
}
