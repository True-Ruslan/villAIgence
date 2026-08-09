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

    @Test
    void createsExactCanonicalListenerOwnedNpcToldEvidence() {
        MemoryEvent event = NpcToldDialogueAdapter.create(
                SPEAKER,
                LISTENER,
                SOURCE,
                12_345L,
                "  Bridge\n  destroyed  "
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
        assertEquals("NPC told: Bridge destroyed", event.summary());
    }

    @Test
    void replayIdentityIsStableAndEveryAuthorityDimensionAffectsEvidenceId() {
        MemoryEvent first = NpcToldDialogueAdapter.create(SPEAKER, LISTENER, SOURCE, 77L, "Claim").orElseThrow();
        MemoryEvent replay = NpcToldDialogueAdapter.create(SPEAKER, LISTENER, SOURCE, 77L, "Claim").orElseThrow();

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
    void rejectsInvalidIdentityAndBlankStatementAndBoundsSummaryWithoutWallClock() {
        assertTrue(NpcToldDialogueAdapter.create(null, LISTENER, SOURCE, 1L, "Claim").isEmpty());
        assertTrue(NpcToldDialogueAdapter.create(SPEAKER, null, SOURCE, 1L, "Claim").isEmpty());
        assertTrue(NpcToldDialogueAdapter.create(SPEAKER, LISTENER, null, 1L, "Claim").isEmpty());
        assertTrue(NpcToldDialogueAdapter.create(SPEAKER, SPEAKER, SOURCE, 1L, "Claim").isEmpty());
        assertTrue(NpcToldDialogueAdapter.create(SPEAKER, LISTENER, SOURCE, 1L, " \n\t ").isEmpty());

        MemoryEvent bounded = NpcToldDialogueAdapter.create(
                SPEAKER,
                LISTENER,
                SOURCE,
                -10L,
                "x".repeat(300)
        ).orElseThrow();

        assertEquals(0L, bounded.gameTime());
        assertEquals(0L, bounded.createdAtEpochMillis());
        assertEquals(250, bounded.summary().codePointCount(0, bounded.summary().length()));
    }
}
