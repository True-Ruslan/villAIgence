package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcToldDialogueAdapterTest {
    private static final UUID SPEAKER = UUID.fromString("00000000-0000-0000-0000-000000020001");
    private static final UUID LISTENER = UUID.fromString("00000000-0000-0000-0000-000000020002");
    private static final UUID SOURCE = UUID.fromString("00000000-0000-0000-0000-000000020003");
    private static final UUID SOURCE_EVENT = UUID.fromString("00000000-0000-0000-0000-000000020010");

    @Test
    void createsExactCanonicalListenerOwnedNpcToldEvidence() {
        KnowledgeTransferProvenance provenance = firstHop("Bridge destroyed", 12_345L);
        MemoryEvent event = NpcToldDialogueAdapter.create(
                SPEAKER,
                LISTENER,
                SOURCE,
                12_345L,
                "  Bridge\n  destroyed  ",
                provenance
        ).orElseThrow();

        String canonical = "npc-knowledge-transfer-v2\n"
                + LISTENER + "\n"
                + SPEAKER + "\n"
                + SOURCE + "\n"
                + 12_345L;
        UUID expectedId = UUID.nameUUIDFromBytes(canonical.getBytes(StandardCharsets.UTF_8));

        assertEquals(expectedId, event.id());
        assertEquals(LISTENER, event.ownerNpcId());
        assertEquals(MemoryEvent.Type.DIALOGUE, event.type());
        assertEquals(List.of(LISTENER, SPEAKER), event.participants());
        assertEquals(MemoryEvent.Provenance.NPC_TOLD, event.provenance());
        assertEquals(12_345L, event.gameTime());
        assertEquals(0L, event.createdAtEpochMillis());
        assertEquals(50, event.importance());
        assertEquals(0, event.emotionalWeight());
        assertEquals(50, event.confidence());
        assertEquals(List.of(), event.relationshipReasons());
        assertNull(event.dialogue());
        assertNull(event.relationshipTransition());
        assertNull(event.relationshipCause());
        assertNotNull(event.knowledgeTransferProvenance());
        assertEquals(provenance, event.knowledgeTransferProvenance());
        assertEquals("NPC told: Bridge destroyed", event.summary());
    }

    @Test
    void replayIdentityIsStableAndEveryAuthorityDimensionAffectsEvidenceId() {
        KnowledgeTransferProvenance provenance = firstHop("Claim", 77L);
        MemoryEvent first = NpcToldDialogueAdapter.create(
                SPEAKER, LISTENER, SOURCE, 77L, "Claim", provenance).orElseThrow();
        MemoryEvent replay = NpcToldDialogueAdapter.create(
                SPEAKER, LISTENER, SOURCE, 77L, "Claim", provenance).orElseThrow();

        assertEquals(first, replay);
        assertEquals(first.id(), NpcToldDialogueAdapter.deterministicEvidenceId(SPEAKER, LISTENER, SOURCE, 77L));
        assertNotEquals(first.id(), NpcToldDialogueAdapter.deterministicEvidenceId(
                UUID.fromString("00000000-0000-0000-0000-000000020004"), LISTENER, SOURCE, 77L));
        assertNotEquals(first.id(), NpcToldDialogueAdapter.deterministicEvidenceId(
                SPEAKER, UUID.fromString("00000000-0000-0000-0000-000000020005"), SOURCE, 77L));
        assertNotEquals(first.id(), NpcToldDialogueAdapter.deterministicEvidenceId(
                SPEAKER, LISTENER, UUID.fromString("00000000-0000-0000-0000-000000020006"), 77L));
        assertNotEquals(first.id(), NpcToldDialogueAdapter.deterministicEvidenceId(SPEAKER, LISTENER, SOURCE, 78L));
    }

    @Test
    void rejectsInvalidIdentityBlankStatementOrMismatchedProvenanceAndBoundsSummaryWithoutWallClock() {
        KnowledgeTransferProvenance valid = firstHop("Claim", 1L);
        assertTrue(NpcToldDialogueAdapter.create(null, LISTENER, SOURCE, 1L, "Claim", valid).isEmpty());
        assertTrue(NpcToldDialogueAdapter.create(SPEAKER, null, SOURCE, 1L, "Claim", valid).isEmpty());
        assertTrue(NpcToldDialogueAdapter.create(SPEAKER, LISTENER, null, 1L, "Claim", valid).isEmpty());
        assertTrue(NpcToldDialogueAdapter.create(SPEAKER, SPEAKER, SOURCE, 1L, "Claim", valid).isEmpty());
        assertTrue(NpcToldDialogueAdapter.create(SPEAKER, LISTENER, SOURCE, 1L, " \n\t ", valid).isEmpty());
        assertTrue(NpcToldDialogueAdapter.create(SPEAKER, LISTENER, SOURCE, 1L, "Other", valid).isEmpty());
        assertTrue(NpcToldDialogueAdapter.create(SPEAKER, LISTENER, SOURCE, 1L, "Claim", null).isEmpty());

        KnowledgeTransferProvenance boundedProvenance = firstHop("x".repeat(300), 0L);
        MemoryEvent bounded = NpcToldDialogueAdapter.create(
                SPEAKER,
                LISTENER,
                SOURCE,
                -10L,
                "x".repeat(300),
                boundedProvenance
        ).orElseThrow();

        assertEquals(0L, bounded.gameTime());
        assertEquals(0L, bounded.createdAtEpochMillis());
        assertEquals(250, bounded.summary().codePointCount(0, bounded.summary().length()));
    }

    private static KnowledgeTransferProvenance firstHop(String statement, long gameTime) {
        SemanticMemoryEntry source = new SemanticMemoryEntry(
                SOURCE,
                SPEAKER,
                SemanticMemoryEntry.Kind.FACT,
                statement,
                List.of(),
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                1L,
                0L,
                100,
                100,
                List.of(SOURCE_EVENT)
        );
        long safeTime = Math.max(0L, gameTime);
        UUID evidenceId = KnowledgeTransferProvenancePolicy.deterministicEvidenceId(
                SPEAKER, LISTENER, SOURCE, safeTime);
        return KnowledgeTransferProvenanceFactory.firstHop(
                source, LISTENER, evidenceId, safeTime).orElseThrow();
    }
}
