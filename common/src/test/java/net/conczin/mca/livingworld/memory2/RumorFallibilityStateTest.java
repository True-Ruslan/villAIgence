package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RumorFallibilityStateTest {
    @Test
    void derivesResolvedFirstHopWithoutInventingTruthScore() {
        KnowledgeTransferProvenance provenance = lineage(1);

        RumorFallibilityState state = RumorFallibilityPolicy.resolve(provenance).orElseThrow();

        assertEquals(RumorFallibilityState.SourcePath.RESOLVED, state.sourcePath());
        assertEquals(1, state.sourceDistanceHops());
        assertEquals(0, state.transformationsUsed());
    }

    @Test
    void derivesExactEightHopDistanceFromCanonicalProvenance() {
        KnowledgeTransferProvenance provenance = lineage(8);

        RumorFallibilityState state = RumorFallibilityPolicy.resolve(provenance).orElseThrow();

        assertEquals(RumorFallibilityState.SourcePath.RESOLVED, state.sourcePath());
        assertEquals(8, state.sourceDistanceHops());
        assertEquals(0, state.transformationsUsed());
    }

    @Test
    void invalidProvenanceCannotProduceResolvedState() {
        KnowledgeTransferProvenance valid = lineage(2);
        KnowledgeTransferProvenance broken = new KnowledgeTransferProvenance(
                valid.origin(),
                List.of(valid.hops().getFirst(), new KnowledgeTransferProvenance.Hop(
                        id(99),
                        valid.hops().get(1).listenerNpcId(),
                        valid.hops().get(1).speakerSemanticEntryId(),
                        valid.hops().get(1).evidenceEventId(),
                        valid.hops().get(1).gameTime()
                ))
        );

        Optional<RumorFallibilityState> resolved = RumorFallibilityPolicy.resolve(broken);

        assertTrue(resolved.isEmpty());
    }

    @Test
    void unresolvedStateCarriesNoFabricatedDistance() {
        RumorFallibilityState unresolved = RumorFallibilityState.unresolved();

        assertEquals(RumorFallibilityState.SourcePath.UNRESOLVED, unresolved.sourcePath());
        assertEquals(0, unresolved.sourceDistanceHops());
        assertEquals(0, unresolved.transformationsUsed());
    }

    private static KnowledgeTransferProvenance lineage(int hopCount) {
        List<UUID> npcs = new ArrayList<>();
        for (int index = 0; index <= hopCount; index++) npcs.add(id(10 + index));
        UUID originEntry = id(100);
        KnowledgeTransferProvenance.Origin origin = new KnowledgeTransferProvenance.Origin(
                npcs.getFirst(),
                originEntry,
                SemanticMemoryEntry.Kind.FACT,
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                "Bridge destroyed",
                List.of()
        );
        List<KnowledgeTransferProvenance.Hop> hops = new ArrayList<>();
        for (int index = 0; index < hopCount; index++) {
            UUID sourceEntry = index == 0 ? originEntry : id(100 + index);
            long gameTime = 200L + index;
            UUID evidenceId = KnowledgeTransferProvenancePolicy.deterministicEvidenceId(
                    npcs.get(index), npcs.get(index + 1), sourceEntry, gameTime);
            hops.add(new KnowledgeTransferProvenance.Hop(
                    npcs.get(index), npcs.get(index + 1), sourceEntry, evidenceId, gameTime));
        }
        KnowledgeTransferProvenance provenance = new KnowledgeTransferProvenance(origin, List.copyOf(hops));
        assertTrue(KnowledgeTransferProvenancePolicy.valid(provenance));
        return provenance;
    }

    private static UUID id(int value) {
        return UUID.fromString("00000000-0000-0000-0000-" + String.format("%012d", value));
    }
}
