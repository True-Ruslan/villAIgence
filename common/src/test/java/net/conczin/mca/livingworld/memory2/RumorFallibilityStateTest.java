package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    void resolvedTransformationCountComesOnlyFromValidatedTransformationEvidence() {
        KnowledgeTransferProvenance provenance = lineage(2);
        KnowledgeTransferTransformation transformation = transformationOnLastHop(provenance);

        RumorFallibilityState state = RumorFallibilityPolicy.resolve(
                provenance,
                transformation
        ).orElseThrow();

        assertEquals(RumorFallibilityState.SourcePath.RESOLVED, state.sourcePath());
        assertEquals(2, state.sourceDistanceHops());
        assertEquals(1, state.transformationsUsed());
        assertThrows(IllegalArgumentException.class, () -> new RumorFallibilityState(
                RumorFallibilityState.SourcePath.RESOLVED,
                2,
                KnowledgeTransferTransformationPolicy.MAX_TRANSFORMATIONS + 1
        ));
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
    void unresolvedStateCarriesNoFabricatedDistanceOrTransformationCount() {
        RumorFallibilityState unresolved = RumorFallibilityState.unresolved();

        assertEquals(RumorFallibilityState.SourcePath.UNRESOLVED, unresolved.sourcePath());
        assertEquals(0, unresolved.sourceDistanceHops());
        assertEquals(RumorFallibilityState.UNKNOWN_TRANSFORMATIONS, unresolved.transformationsUsed());
        assertThrows(IllegalArgumentException.class, () -> new RumorFallibilityState(
                RumorFallibilityState.SourcePath.UNRESOLVED,
                0,
                0
        ));
    }

    private static KnowledgeTransferTransformation transformationOnLastHop(
            KnowledgeTransferProvenance provenance
    ) {
        KnowledgeTransferProvenance.Hop hop = provenance.hops().getLast();
        return new KnowledgeTransferTransformation(List.of(new KnowledgeTransferTransformation.Step(
                KnowledgeTransferTransformation.Kind.OMIT_TRAILING_SENTENCE,
                provenance.origin().statement(),
                "Bridge destroyed.",
                hop.speakerNpcId(),
                hop.listenerNpcId(),
                hop.speakerSemanticEntryId(),
                hop.evidenceEventId(),
                hop.gameTime()
        )));
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
                "Bridge destroyed. Repairs pending.",
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
