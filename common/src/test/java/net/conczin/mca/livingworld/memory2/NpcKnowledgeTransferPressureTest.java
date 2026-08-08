package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcKnowledgeTransferPressureTest {
    @TempDir
    Path tempDir;

    @Test
    void reportsSourceNotRetainedWhenCanonicalTransferEvidenceLosesEventPressure() {
        Path world = tempDir.resolve("event-pressure");
        UUID speaker = UUID.fromString("00000000-0000-0000-0000-000000060001");
        UUID listener = UUID.fromString("00000000-0000-0000-0000-000000060002");
        UUID thirdNpc = UUID.fromString("00000000-0000-0000-0000-000000060003");
        UUID sourceId = UUID.fromString("00000000-0000-0000-0000-000000060004");
        seedSpeakerFact(world, speaker, sourceId, "The river crossing is flooded");

        MemoryEvent strongListenerEvent = strongObservedEvent("listener-strong", listener, 100L);
        MemoryEvent thirdNpcEvent = strongObservedEvent("third-unchanged", thirdNpc, 90L);
        MemoryEventStore eventStore = MemoryEventStore.forWorld(world);
        eventStore.append(strongListenerEvent, 1);
        eventStore.append(thirdNpcEvent, 1);

        NpcKnowledgeTransferResult result = NpcKnowledgeTransferLifecycle.transfer(
                world, speaker, listener, sourceId, 200L, 1, 16
        );
        UUID attemptedEvidenceId = NpcToldDialogueAdapter.deterministicEvidenceId(
                speaker, listener, sourceId, 200L
        );

        assertEquals(NpcKnowledgeTransferResult.Status.SOURCE_NOT_RETAINED, result.status());
        assertEquals(attemptedEvidenceId, result.evidenceEventId());
        assertTrue(eventStore.findById(listener, attemptedEvidenceId).isEmpty());
        assertEquals(List.of(strongListenerEvent.id()),
                eventStore.getRecent(listener, 16).stream().map(MemoryEvent::id).toList());
        assertEquals(List.of(), SemanticMemoryStore.forWorld(world).getRecent(listener, 16));
        assertEquals(List.of(thirdNpcEvent.id()),
                eventStore.getRecent(thirdNpc, 16).stream().map(MemoryEvent::id).toList());
        assertEquals(1, SemanticMemoryStore.forWorld(world).getRecent(speaker, 16).size());
    }

    @Test
    void reportsBeliefNotRetainedWithoutRollingBackLegitimateTransferEvidence() {
        Path world = tempDir.resolve("semantic-pressure");
        UUID speaker = UUID.fromString("00000000-0000-0000-0000-000000061001");
        UUID listener = UUID.fromString("00000000-0000-0000-0000-000000061002");
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000061003");
        UUID sourceId = UUID.fromString("00000000-0000-0000-0000-000000061004");
        seedSpeakerFact(world, speaker, sourceId, "The east road is unsafe");

        SemanticMemoryEntry strongListenerFact = new SemanticMemoryEntry(
                UUID.fromString("00000000-0000-0000-0000-000000061005"),
                listener,
                SemanticMemoryEntry.Kind.FACT,
                "Server observed the fortified gate",
                List.of(player),
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                100L,
                0L,
                100,
                100,
                List.of(UUID.fromString("00000000-0000-0000-0000-000000061006"))
        );
        SemanticMemoryStore semanticStore = SemanticMemoryStore.forWorld(world);
        semanticStore.append(strongListenerFact, 1);

        NpcKnowledgeTransferResult result = NpcKnowledgeTransferLifecycle.transfer(
                world, speaker, listener, sourceId, 200L, 16, 1
        );
        UUID evidenceId = NpcToldDialogueAdapter.deterministicEvidenceId(
                speaker, listener, sourceId, 200L
        );

        assertEquals(NpcKnowledgeTransferResult.Status.BELIEF_NOT_RETAINED, result.status());
        assertEquals(evidenceId, result.evidenceEventId());
        assertTrue(MemoryEventStore.forWorld(world).findById(listener, evidenceId).isPresent());
        assertEquals(List.of(strongListenerFact.id()),
                semanticStore.getRecent(listener, 16).stream().map(SemanticMemoryEntry::id).toList());
        assertTrue(semanticStore.findMatching(
                listener,
                entry -> entry.kind() == SemanticMemoryEntry.Kind.BELIEF
                        && entry.provenance() == MemoryEvent.Provenance.NPC_TOLD
                        && entry.sourceEventIds().contains(evidenceId)
        ).isEmpty());
        assertEquals(1, semanticStore.getRecent(speaker, 16).size());
    }

    private static void seedSpeakerFact(Path world, UUID speaker, UUID sourceId, String statement) {
        SemanticMemoryStore.forWorld(world).append(new SemanticMemoryEntry(
                sourceId,
                speaker,
                SemanticMemoryEntry.Kind.FACT,
                statement,
                List.of(),
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                10L,
                0L,
                80,
                100,
                List.of(UUID.nameUUIDFromBytes((sourceId + "-source").getBytes(StandardCharsets.UTF_8)))
        ), 16);
    }

    private static MemoryEvent strongObservedEvent(String seed, UUID owner, long gameTime) {
        return new MemoryEvent(
                UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)),
                owner,
                MemoryEvent.Type.RELATIONSHIP_CHANGE,
                seed,
                List.of(owner),
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                gameTime,
                0L,
                100,
                100,
                100,
                List.of()
        );
    }
}
