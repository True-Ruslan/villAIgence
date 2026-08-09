package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KnowledgeTransferProvenanceResolverTest {
    @TempDir
    Path tempDir;

    @Test
    void resolvesNewestRetainedValidDirectBranchWithoutListenerInput() {
        Path world = tempDir.resolve("resolver-newest");
        UUID speaker = id(1);
        MemoryEvent older = firstHopEvidence(id(2), speaker, id(101), "Bridge destroyed", 100L);
        MemoryEvent newer = firstHopEvidence(id(3), speaker, id(102), "Bridge destroyed", 200L);
        MemoryEventStore eventStore = MemoryEventStore.forWorld(world);
        eventStore.append(older, 64);
        eventStore.append(newer, 64);

        SemanticMemoryEntry source = speakerBelief(speaker, List.of(older.id(), newer.id()), "Bridge destroyed");
        KnowledgeTransferProvenanceResolver.ResolvedSource resolved =
                KnowledgeTransferProvenanceResolver.resolve(eventStore, source).orElseThrow();

        assertEquals(newer.id(), resolved.evidence().id());
        assertEquals(newer.knowledgeTransferProvenance(), resolved.provenance());
    }

    @Test
    void sameGameTimeUsesEvidenceUuidAscendingAndSourceInsertionOrderDoesNotMatter() {
        Path world = tempDir.resolve("resolver-tie");
        UUID speaker = id(10);
        MemoryEvent first = firstHopEvidence(id(11), speaker, id(111), "Same claim", 300L);
        MemoryEvent second = firstHopEvidence(id(12), speaker, id(112), "Same claim", 300L);
        MemoryEventStore eventStore = MemoryEventStore.forWorld(world);
        eventStore.append(first, 64);
        eventStore.append(second, 64);
        UUID expected = first.id().toString().compareTo(second.id().toString()) <= 0 ? first.id() : second.id();

        UUID forward = KnowledgeTransferProvenanceResolver.resolve(
                eventStore,
                speakerBelief(speaker, List.of(first.id(), second.id()), "Same claim")
        ).orElseThrow().evidence().id();
        UUID reverse = KnowledgeTransferProvenanceResolver.resolve(
                eventStore,
                speakerBelief(speaker, List.of(second.id(), first.id()), "Same claim")
        ).orElseThrow().evidence().id();

        assertEquals(expected, forward);
        assertEquals(expected, reverse);
    }

    @Test
    void ignoresMissingUnreferencedAndMalformedBranchesBeforeOrdering() {
        Path world = tempDir.resolve("resolver-filtering");
        UUID speaker = id(20);
        MemoryEvent valid = firstHopEvidence(id(21), speaker, id(121), "Filtered claim", 100L);
        MemoryEvent unreferencedNewer = firstHopEvidence(id(22), speaker, id(122), "Filtered claim", 900L);
        UUID malformedId = id(888);
        MemoryEvent malformedNewer = new MemoryEvent(
                malformedId,
                speaker,
                MemoryEvent.Type.DIALOGUE,
                "NPC told: Filtered claim",
                List.of(speaker, id(23)),
                MemoryEvent.Provenance.NPC_TOLD,
                800L,
                0L,
                50,
                0,
                50,
                List.of(),
                null,
                null,
                null
        );
        MemoryEventStore eventStore = MemoryEventStore.forWorld(world);
        eventStore.append(valid, 64);
        eventStore.append(unreferencedNewer, 64);
        eventStore.append(malformedNewer, 64);

        SemanticMemoryEntry source = speakerBelief(
                speaker,
                List.of(id(777), malformedId, valid.id()),
                "Filtered claim"
        );

        KnowledgeTransferProvenanceResolver.ResolvedSource resolved =
                KnowledgeTransferProvenanceResolver.resolve(eventStore, source).orElseThrow();

        assertEquals(valid.id(), resolved.evidence().id());
    }

    private static SemanticMemoryEntry speakerBelief(
            UUID speaker,
            List<UUID> sourceIds,
            String statement
    ) {
        return new SemanticMemoryEntry(
                id(201),
                speaker,
                SemanticMemoryEntry.Kind.BELIEF,
                statement,
                List.of(),
                MemoryEvent.Provenance.NPC_TOLD,
                500L,
                0L,
                50,
                50,
                sourceIds
        );
    }

    private static MemoryEvent firstHopEvidence(
            UUID origin,
            UUID listener,
            UUID sourceId,
            String statement,
            long gameTime
    ) {
        SemanticMemoryEntry source = new SemanticMemoryEntry(
                sourceId,
                origin,
                SemanticMemoryEntry.Kind.FACT,
                statement,
                List.of(),
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                Math.max(0L, gameTime - 1L),
                0L,
                100,
                100,
                List.of(id(900 + (int) gameTime))
        );
        UUID evidenceId = KnowledgeTransferProvenancePolicy.deterministicEvidenceId(
                origin, listener, sourceId, gameTime);
        KnowledgeTransferProvenance provenance = KnowledgeTransferProvenanceFactory.firstHop(
                source, listener, evidenceId, gameTime).orElseThrow();
        return NpcToldDialogueAdapter.create(
                origin, listener, sourceId, gameTime, statement, provenance).orElseThrow();
    }

    private static UUID id(int value) {
        return UUID.fromString("00000000-0000-0000-0000-" + String.format("%012d", value));
    }
}
