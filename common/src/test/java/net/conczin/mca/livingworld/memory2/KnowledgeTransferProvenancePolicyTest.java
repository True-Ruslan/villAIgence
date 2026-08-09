package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnowledgeTransferProvenancePolicyTest {
    private static final UUID A = id(1);
    private static final UUID B = id(2);
    private static final UUID C = id(3);
    private static final UUID D = id(4);
    private static final UUID PLAYER = id(90);
    private static final UUID A_ENTRY = id(101);

    @Test
    void deterministicEvidenceIdUsesV2Namespace() {
        String canonical = "npc-knowledge-transfer-v2\n" + B + "\n" + A + "\n" + A_ENTRY + "\n100";
        UUID expected = UUID.nameUUIDFromBytes(canonical.getBytes(StandardCharsets.UTF_8));

        assertEquals(expected, KnowledgeTransferProvenancePolicy.deterministicEvidenceId(A, B, A_ENTRY, 100L));
    }

    @Test
    void validatesCanonicalTwoHopLineageAndCycleQueries() {
        KnowledgeTransferProvenance lineage = twoHopLineage();

        assertTrue(KnowledgeTransferProvenancePolicy.valid(lineage));
        assertTrue(KnowledgeTransferProvenancePolicy.wouldCycle(lineage, A));
        assertTrue(KnowledgeTransferProvenancePolicy.wouldCycle(lineage, B));
        assertTrue(KnowledgeTransferProvenancePolicy.wouldCycle(lineage, C));
        assertFalse(KnowledgeTransferProvenancePolicy.wouldCycle(lineage, D));
        assertFalse(KnowledgeTransferProvenancePolicy.atHopLimit(lineage));
    }

    @Test
    void rejectsBrokenContinuityRepeatedNpcAndNpcToldOriginReset() {
        KnowledgeTransferProvenance valid = twoHopLineage();
        KnowledgeTransferProvenance.Hop first = valid.hops().get(0);
        KnowledgeTransferProvenance.Hop second = valid.hops().get(1);

        KnowledgeTransferProvenance broken = new KnowledgeTransferProvenance(
                valid.origin(),
                List.of(first, new KnowledgeTransferProvenance.Hop(
                        D, C, second.speakerSemanticEntryId(),
                        KnowledgeTransferProvenancePolicy.deterministicEvidenceId(
                                D, C, second.speakerSemanticEntryId(), second.gameTime()),
                        second.gameTime()))
        );
        KnowledgeTransferProvenance repeated = new KnowledgeTransferProvenance(
                valid.origin(),
                List.of(first, new KnowledgeTransferProvenance.Hop(
                        B, A, second.speakerSemanticEntryId(),
                        KnowledgeTransferProvenancePolicy.deterministicEvidenceId(
                                B, A, second.speakerSemanticEntryId(), second.gameTime()),
                        second.gameTime()))
        );
        KnowledgeTransferProvenance reset = new KnowledgeTransferProvenance(
                new KnowledgeTransferProvenance.Origin(
                        A, A_ENTRY, SemanticMemoryEntry.Kind.BELIEF,
                        MemoryEvent.Provenance.NPC_TOLD,
                        "Bridge destroyed", List.of(PLAYER)),
                valid.hops()
        );

        assertFalse(KnowledgeTransferProvenancePolicy.valid(broken));
        assertFalse(KnowledgeTransferProvenancePolicy.valid(repeated));
        assertFalse(KnowledgeTransferProvenancePolicy.valid(reset));
    }

    @Test
    void firstHopFactoryAcceptsOnlyNonNpcToldOriginsAndCanonicalizesScope() {
        SemanticMemoryEntry fact = semantic(
                A_ENTRY, A, SemanticMemoryEntry.Kind.FACT,
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                "  Bridge\t destroyed  ", List.of(PLAYER, PLAYER));
        UUID evidence = KnowledgeTransferProvenancePolicy.deterministicEvidenceId(A, B, A_ENTRY, 100L);

        KnowledgeTransferProvenance lineage = KnowledgeTransferProvenanceFactory.firstHop(
                fact, B, evidence, 100L).orElseThrow();

        assertEquals("Bridge destroyed", lineage.origin().statement());
        assertEquals(List.of(PLAYER), lineage.origin().relatedEntities());
        assertEquals(1, lineage.hops().size());
        assertTrue(KnowledgeTransferProvenancePolicy.valid(lineage));

        SemanticMemoryEntry playerTold = semantic(
                id(102), A, SemanticMemoryEntry.Kind.BELIEF,
                MemoryEvent.Provenance.PLAYER_TOLD, "Player claim", List.of());
        SemanticMemoryEntry inferred = semantic(
                id(103), A, SemanticMemoryEntry.Kind.BELIEF,
                MemoryEvent.Provenance.INFERRED, "Inference", List.of());
        SemanticMemoryEntry npcTold = semantic(
                id(104), A, SemanticMemoryEntry.Kind.BELIEF,
                MemoryEvent.Provenance.NPC_TOLD, "Rumor", List.of());

        assertTrue(KnowledgeTransferProvenanceFactory.firstHop(
                playerTold, B,
                KnowledgeTransferProvenancePolicy.deterministicEvidenceId(A, B, playerTold.id(), 101L),
                101L).isPresent());
        assertTrue(KnowledgeTransferProvenanceFactory.firstHop(
                inferred, B,
                KnowledgeTransferProvenancePolicy.deterministicEvidenceId(A, B, inferred.id(), 102L),
                102L).isPresent());
        assertTrue(KnowledgeTransferProvenanceFactory.firstHop(
                npcTold, B,
                KnowledgeTransferProvenancePolicy.deterministicEvidenceId(A, B, npcTold.id(), 103L),
                103L).isEmpty());
    }

    @Test
    void exactlyEightHopsAreAllowedAndReportedAtLimit() {
        List<UUID> npcs = new ArrayList<>();
        for (int index = 0; index < 9; index++) npcs.add(id(200 + index));
        UUID originEntry = id(300);
        KnowledgeTransferProvenance.Origin origin = new KnowledgeTransferProvenance.Origin(
                npcs.get(0), originEntry, SemanticMemoryEntry.Kind.FACT,
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                "Eight hop rumor", List.of());
        List<KnowledgeTransferProvenance.Hop> hops = new ArrayList<>();
        for (int index = 0; index < 8; index++) {
            UUID sourceEntry = index == 0 ? originEntry : id(300 + index);
            long gameTime = 100L + index;
            hops.add(new KnowledgeTransferProvenance.Hop(
                    npcs.get(index), npcs.get(index + 1), sourceEntry,
                    KnowledgeTransferProvenancePolicy.deterministicEvidenceId(
                            npcs.get(index), npcs.get(index + 1), sourceEntry, gameTime),
                    gameTime));
        }
        KnowledgeTransferProvenance lineage = new KnowledgeTransferProvenance(origin, List.copyOf(hops));

        assertTrue(KnowledgeTransferProvenancePolicy.valid(lineage));
        assertTrue(KnowledgeTransferProvenancePolicy.atHopLimit(lineage));
    }

    private static KnowledgeTransferProvenance twoHopLineage() {
        UUID ab = KnowledgeTransferProvenancePolicy.deterministicEvidenceId(A, B, A_ENTRY, 100L);
        UUID bEntry = id(102);
        UUID bc = KnowledgeTransferProvenancePolicy.deterministicEvidenceId(B, C, bEntry, 200L);
        return new KnowledgeTransferProvenance(
                new KnowledgeTransferProvenance.Origin(
                        A, A_ENTRY, SemanticMemoryEntry.Kind.FACT,
                        MemoryEvent.Provenance.SYSTEM_OBSERVED,
                        "Bridge destroyed", List.of(PLAYER)),
                List.of(
                        new KnowledgeTransferProvenance.Hop(A, B, A_ENTRY, ab, 100L),
                        new KnowledgeTransferProvenance.Hop(B, C, bEntry, bc, 200L)
                )
        );
    }

    private static SemanticMemoryEntry semantic(
            UUID entryId,
            UUID owner,
            SemanticMemoryEntry.Kind kind,
            MemoryEvent.Provenance provenance,
            String statement,
            List<UUID> related
    ) {
        return new SemanticMemoryEntry(
                entryId, owner, kind, statement, related, provenance,
                10L, 0L, 50, 50, List.of(id(999))
        );
    }

    private static UUID id(int value) {
        return UUID.fromString("00000000-0000-0000-0000-" + String.format("%012d", value));
    }
}
