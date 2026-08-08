package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class NpcKnowledgeTransferIdempotencyTest {
    @TempDir
    Path tempDir;

    @Test
    void exactRetryIsByteIdempotentAndLaterTransferConsolidatesExactEvidenceSources() throws Exception {
        Path world = tempDir.resolve("retry");
        UUID speaker = UUID.fromString("00000000-0000-0000-0000-000000050001");
        UUID listener = UUID.fromString("00000000-0000-0000-0000-000000050002");
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000050003");
        UUID sourceId = UUID.fromString("00000000-0000-0000-0000-000000050004");
        seedSpeakerFact(world, speaker, sourceId, List.of(player), "The northern road is blocked");

        NpcKnowledgeTransferResult first = NpcKnowledgeTransferLifecycle.transfer(
                world, speaker, listener, sourceId, 100L, 64, 64
        );
        assertEquals(NpcKnowledgeTransferResult.Status.ADMITTED, first.status());
        byte[] memoryAfterFirst = Files.readAllBytes(world.resolve("livingworld/memory2.json"));
        byte[] semanticAfterFirst = Files.readAllBytes(world.resolve("livingworld/semantic-memory.json"));

        NpcKnowledgeTransferResult replay = NpcKnowledgeTransferLifecycle.transfer(
                world, speaker, listener, sourceId, 100L, 64, 64
        );

        assertEquals(NpcKnowledgeTransferResult.Status.ADMITTED, replay.status());
        assertEquals(first.evidenceEventId(), replay.evidenceEventId());
        assertEquals(first.semanticEntryId(), replay.semanticEntryId());
        assertArrayEquals(memoryAfterFirst, Files.readAllBytes(world.resolve("livingworld/memory2.json")));
        assertArrayEquals(semanticAfterFirst, Files.readAllBytes(world.resolve("livingworld/semantic-memory.json")));
        assertEquals(1, MemoryEventStore.forWorld(world).getRecent(listener, 64).size());
        assertEquals(1, SemanticMemoryStore.forWorld(world).getRecent(listener, 64).size());

        NpcKnowledgeTransferResult later = NpcKnowledgeTransferLifecycle.transfer(
                world, speaker, listener, sourceId, 200L, 64, 64
        );

        assertEquals(NpcKnowledgeTransferResult.Status.ADMITTED, later.status());
        assertNotEquals(first.evidenceEventId(), later.evidenceEventId());
        assertEquals(2, MemoryEventStore.forWorld(world).getRecent(listener, 64).size());
        List<SemanticMemoryEntry> listenerMemory = SemanticMemoryStore.forWorld(world).getRecent(listener, 64);
        assertEquals(1, listenerMemory.size());
        SemanticMemoryEntry consolidated = listenerMemory.getFirst();
        assertEquals(later.semanticEntryId(), consolidated.id());
        assertEquals(sorted(first.evidenceEventId(), later.evidenceEventId()), consolidated.sourceEventIds());
        assertEquals(SemanticMemoryEntry.Kind.BELIEF, consolidated.kind());
        assertEquals(MemoryEvent.Provenance.NPC_TOLD, consolidated.provenance());
    }

    @Test
    void consolidationTreatsSemanticSubjectScopeAsCanonicalUuidSet() {
        Path world = tempDir.resolve("scope");
        UUID speaker = UUID.fromString("00000000-0000-0000-0000-000000051001");
        UUID listener = UUID.fromString("00000000-0000-0000-0000-000000051002");
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000051003");
        UUID entity = UUID.fromString("00000000-0000-0000-0000-000000051004");
        UUID sourceId = UUID.fromString("00000000-0000-0000-0000-000000051005");
        UUID existingEvidence = UUID.fromString("00000000-0000-0000-0000-000000051006");
        seedSpeakerFact(world, speaker, sourceId, List.of(entity, player), "The market opens at dawn");

        SemanticMemoryStore.forWorld(world).append(new SemanticMemoryEntry(
                UUID.fromString("00000000-0000-0000-0000-000000051007"),
                listener,
                SemanticMemoryEntry.Kind.BELIEF,
                "the   market opens at dawn",
                List.of(player, entity),
                MemoryEvent.Provenance.NPC_TOLD,
                50L,
                0L,
                50,
                50,
                List.of(existingEvidence)
        ), 64);

        NpcKnowledgeTransferResult result = NpcKnowledgeTransferLifecycle.transfer(
                world, speaker, listener, sourceId, 100L, 64, 64
        );

        assertEquals(NpcKnowledgeTransferResult.Status.ADMITTED, result.status());
        List<SemanticMemoryEntry> listenerMemory = SemanticMemoryStore.forWorld(world).getRecent(listener, 64);
        assertEquals(1, listenerMemory.size());
        SemanticMemoryEntry consolidated = listenerMemory.getFirst();
        assertEquals(sorted(player, entity), sorted(consolidated.relatedEntities()));
        assertEquals(sorted(existingEvidence, result.evidenceEventId()), consolidated.sourceEventIds());
        assertEquals(result.semanticEntryId(), consolidated.id());
    }

    private static void seedSpeakerFact(
            Path world,
            UUID speaker,
            UUID sourceId,
            List<UUID> related,
            String statement
    ) {
        SemanticMemoryStore.forWorld(world).append(new SemanticMemoryEntry(
                sourceId,
                speaker,
                SemanticMemoryEntry.Kind.FACT,
                statement,
                related,
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                10L,
                100L,
                80,
                100,
                List.of(UUID.nameUUIDFromBytes((sourceId + "-evidence").getBytes()))
        ), 64);
    }

    private static List<UUID> sorted(UUID... values) {
        return sorted(List.of(values));
    }

    private static List<UUID> sorted(List<UUID> values) {
        List<UUID> result = new ArrayList<>(values);
        result.sort(Comparator.comparing(UUID::toString));
        return List.copyOf(result);
    }
}
