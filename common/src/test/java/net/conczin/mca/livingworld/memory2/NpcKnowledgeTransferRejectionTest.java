package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NpcKnowledgeTransferRejectionTest {
    @TempDir
    Path tempDir;

    @Test
    void invalidAuthorityInputsRejectWithoutPartialListenerState() {
        UUID speaker = UUID.fromString("00000000-0000-0000-0000-000000040001");
        UUID listener = UUID.fromString("00000000-0000-0000-0000-000000040002");
        UUID sourceId = UUID.fromString("00000000-0000-0000-0000-000000040003");

        assertEquals(NpcKnowledgeTransferResult.Status.REJECTED,
                NpcKnowledgeTransferLifecycle.transfer(null, speaker, listener, sourceId, 10L, 16, 16).status());

        Path world = tempDir.resolve("invalid-inputs");
        assertRejectedWithoutListenerState(world, null, listener, sourceId);
        assertRejectedWithoutListenerState(world, speaker, null, sourceId);
        assertRejectedWithoutListenerState(world, speaker, listener, null);
        assertRejectedWithoutListenerState(world, speaker, speaker, sourceId);
        assertRejectedWithoutListenerState(world, speaker, listener, UUID.randomUUID());
    }

    @Test
    void sourceOwnedByAnotherNpcCannotBeTransferredUnderClaimedSpeakerIdentity() {
        Path world = tempDir.resolve("wrong-owner");
        UUID claimedSpeaker = UUID.fromString("00000000-0000-0000-0000-000000041001");
        UUID actualOwner = UUID.fromString("00000000-0000-0000-0000-000000041002");
        UUID listener = UUID.fromString("00000000-0000-0000-0000-000000041003");
        UUID sourceId = UUID.fromString("00000000-0000-0000-0000-000000041004");
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000041005");

        SemanticMemoryStore.forWorld(world).append(new SemanticMemoryEntry(
                sourceId,
                actualOwner,
                SemanticMemoryEntry.Kind.FACT,
                "The gate is locked",
                List.of(player),
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                10L,
                100L,
                80,
                100,
                List.of(UUID.randomUUID())
        ), 16);

        NpcKnowledgeTransferResult result = NpcKnowledgeTransferLifecycle.transfer(
                world, claimedSpeaker, listener, sourceId, 20L, 16, 16
        );

        assertEquals(NpcKnowledgeTransferResult.Status.REJECTED, result.status());
        assertEquals(List.of(), MemoryEventStore.forWorld(world).getRecent(listener, 16));
        assertEquals(List.of(), SemanticMemoryStore.forWorld(world).getRecent(listener, 16));
        assertEquals(1, SemanticMemoryStore.forWorld(world).getRecent(actualOwner, 16).size());
    }

    private static void assertRejectedWithoutListenerState(
            Path world,
            UUID speaker,
            UUID listener,
            UUID sourceId
    ) {
        NpcKnowledgeTransferResult result = NpcKnowledgeTransferLifecycle.transfer(
                world, speaker, listener, sourceId, 10L, 16, 16
        );
        assertEquals(NpcKnowledgeTransferResult.Status.REJECTED, result.status());
        if (listener != null) {
            assertEquals(List.of(), MemoryEventStore.forWorld(world).getRecent(listener, 16));
            assertEquals(List.of(), SemanticMemoryStore.forWorld(world).getRecent(listener, 16));
        }
    }
}
