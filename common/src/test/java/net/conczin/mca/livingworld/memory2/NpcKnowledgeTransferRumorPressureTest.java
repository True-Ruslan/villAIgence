package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcKnowledgeTransferRumorPressureTest {
    @TempDir
    Path tempDir;

    @Test
    void laterDirectEvidenceKeepsAncestrySnapshotAfterOlderPhysicalHopIsForgotten() {
        Path world = tempDir.resolve("older-hop-forgotten");
        UUID a = id(1);
        UUID b = id(2);
        UUID c = id(3);
        UUID source = id(101);
        seedFact(world, a, source, "The quarry road is blocked");

        NpcKnowledgeTransferResult ab = transfer(world, a, b, source, 100L, 64, 64);
        NpcKnowledgeTransferResult bc = transfer(world, b, c, ab.semanticEntryId(), 200L, 64, 64);
        assertEquals(NpcKnowledgeTransferResult.Status.ADMITTED, bc.status());
        MemoryEvent cDirectBefore = MemoryEventStore.forWorld(world)
                .findById(c, bc.evidenceEventId()).orElseThrow();
        KnowledgeTransferProvenance lineageBefore = cDirectBefore.knowledgeTransferProvenance();
        assertEquals(2, lineageBefore.hops().size());

        MemoryEventStore.forWorld(world).append(strongObservedEvent(b, 1_000L), 1);
        assertTrue(MemoryEventStore.forWorld(world).findById(b, ab.evidenceEventId()).isEmpty());

        MemoryEvent cDirectAfter = MemoryEventStore.forWorld(world)
                .findById(c, bc.evidenceEventId()).orElseThrow();
        assertEquals(lineageBefore, cDirectAfter.knowledgeTransferProvenance());
        assertEquals(ab.evidenceEventId(), cDirectAfter.knowledgeTransferProvenance().hops().get(0).evidenceEventId());
        assertEquals(bc.evidenceEventId(), cDirectAfter.knowledgeTransferProvenance().hops().get(1).evidenceEventId());
        assertTrue(SemanticMemoryStore.forWorld(world).findById(c, bc.semanticEntryId()).isPresent());
    }

    @Test
    void lossOfCurrentDirectEvidenceBlocksFurtherPropagationButDoesNotEraseSpeakerBelief() {
        Path world = tempDir.resolve("direct-hop-forgotten");
        UUID a = id(10);
        UUID b = id(11);
        UUID c = id(12);
        UUID d = id(13);
        UUID source = id(110);
        seedFact(world, a, source, "The orchard gate is broken");

        NpcKnowledgeTransferResult ab = transfer(world, a, b, source, 100L, 64, 64);
        NpcKnowledgeTransferResult bc = transfer(world, b, c, ab.semanticEntryId(), 200L, 64, 64);
        assertEquals(NpcKnowledgeTransferResult.Status.ADMITTED, bc.status());
        assertTrue(SemanticMemoryStore.forWorld(world).findById(c, bc.semanticEntryId()).isPresent());

        MemoryEventStore.forWorld(world).append(strongObservedEvent(c, 1_000L), 1);
        assertTrue(MemoryEventStore.forWorld(world).findById(c, bc.evidenceEventId()).isEmpty());
        assertTrue(SemanticMemoryStore.forWorld(world).findById(c, bc.semanticEntryId()).isPresent());

        NpcKnowledgeTransferResult cd = transfer(world, c, d, bc.semanticEntryId(), 1_100L, 64, 64);

        assertEquals(NpcKnowledgeTransferResult.Status.PROVENANCE_UNAVAILABLE, cd.status());
        assertTrue(MemoryEventStore.forWorld(world).getRecent(d, 64).isEmpty());
        assertTrue(SemanticMemoryStore.forWorld(world).getRecent(d, 64).isEmpty());
        assertTrue(SemanticMemoryStore.forWorld(world).findById(c, bc.semanticEntryId()).isPresent());
    }

    @Test
    void rumorBeliefAndEvidenceRemainEvictableUnderExistingPolicies() {
        Path world = tempDir.resolve("rumor-evictable");
        UUID a = id(20);
        UUID b = id(21);
        UUID source = id(120);
        seedFact(world, a, source, "The ferry is delayed");

        NpcKnowledgeTransferResult ab = transfer(world, a, b, source, 100L, 64, 64);
        assertEquals(NpcKnowledgeTransferResult.Status.ADMITTED, ab.status());

        MemoryEventStore eventStore = MemoryEventStore.forWorld(world);
        eventStore.append(strongObservedEvent(b, 2_000L), 1);
        assertTrue(eventStore.findById(b, ab.evidenceEventId()).isEmpty());

        SemanticMemoryStore semanticStore = SemanticMemoryStore.forWorld(world);
        SemanticMemoryEntry strongFact = new SemanticMemoryEntry(
                id(121),
                b,
                SemanticMemoryEntry.Kind.FACT,
                "Server observed a fortified checkpoint",
                List.of(),
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                2_000L,
                0L,
                100,
                100,
                List.of(id(122))
        );
        semanticStore.append(strongFact, 1);
        assertTrue(semanticStore.findById(b, ab.semanticEntryId()).isEmpty());
        assertEquals(List.of(strongFact.id()),
                semanticStore.getRecent(b, 8).stream().map(SemanticMemoryEntry::id).toList());
    }

    private static NpcKnowledgeTransferResult transfer(
            Path world,
            UUID speaker,
            UUID listener,
            UUID source,
            long gameTime,
            int eventCapacity,
            int semanticCapacity
    ) {
        return NpcKnowledgeTransferLifecycle.transfer(
                world, speaker, listener, source, gameTime, eventCapacity, semanticCapacity);
    }

    private static void seedFact(Path world, UUID owner, UUID sourceId, String statement) {
        SemanticMemoryStore.forWorld(world).append(new SemanticMemoryEntry(
                sourceId,
                owner,
                SemanticMemoryEntry.Kind.FACT,
                statement,
                List.of(),
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                10L,
                0L,
                90,
                100,
                List.of(UUID.nameUUIDFromBytes((sourceId + "-origin").getBytes(StandardCharsets.UTF_8)))
        ), 64);
    }

    private static MemoryEvent strongObservedEvent(UUID owner, long gameTime) {
        return new MemoryEvent(
                UUID.nameUUIDFromBytes((owner + "-strong-" + gameTime).getBytes(StandardCharsets.UTF_8)),
                owner,
                MemoryEvent.Type.RELATIONSHIP_CHANGE,
                "Strong server-observed event",
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

    private static UUID id(int value) {
        return UUID.fromString("00000000-0000-0000-0000-" + String.format("%012d", value));
    }
}
