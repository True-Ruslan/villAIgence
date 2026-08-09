package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcKnowledgeTransferProvenanceIntegrityTest {
    @TempDir
    Path tempDir;

    @Test
    void canonicalPersistedEvidenceRejectsEveryProvenanceMutationAgainstExpectedPayload() {
        UUID a = id(1);
        UUID b = id(2);
        UUID c = id(3);
        UUID aEntry = id(101);
        UUID bEntry = id(102);
        UUID player = id(90);

        KnowledgeTransferProvenance canonical = twoHopLineage(a, b, c, aEntry, bEntry, player);
        MemoryEvent event = evidenceForLastHop(canonical);
        KnowledgeTransferProvenance.Origin origin = canonical.origin();
        KnowledgeTransferProvenance.Hop first = canonical.hops().get(0);
        KnowledgeTransferProvenance.Hop second = canonical.hops().get(1);

        List<KnowledgeTransferProvenance> mutations = new ArrayList<>();
        mutations.add(withOrigin(canonical, new KnowledgeTransferProvenance.Origin(
                id(44), origin.originSemanticEntryId(), origin.originKind(), origin.originProvenance(),
                origin.statement(), origin.relatedEntities())));
        mutations.add(withOrigin(canonical, new KnowledgeTransferProvenance.Origin(
                origin.originNpcId(), id(144), origin.originKind(), origin.originProvenance(),
                origin.statement(), origin.relatedEntities())));
        mutations.add(withOrigin(canonical, new KnowledgeTransferProvenance.Origin(
                origin.originNpcId(), origin.originSemanticEntryId(), SemanticMemoryEntry.Kind.BELIEF,
                MemoryEvent.Provenance.PLAYER_TOLD, origin.statement(), origin.relatedEntities())));
        mutations.add(withOrigin(canonical, new KnowledgeTransferProvenance.Origin(
                origin.originNpcId(), origin.originSemanticEntryId(), origin.originKind(), origin.originProvenance(),
                "Bridge damaged", origin.relatedEntities())));
        mutations.add(withOrigin(canonical, new KnowledgeTransferProvenance.Origin(
                origin.originNpcId(), origin.originSemanticEntryId(), origin.originKind(), origin.originProvenance(),
                origin.statement(), List.of())));
        mutations.add(withHop(canonical, 0, new KnowledgeTransferProvenance.Hop(
                id(55), first.listenerNpcId(), first.speakerSemanticEntryId(), first.evidenceEventId(), first.gameTime())));
        mutations.add(withHop(canonical, 1, new KnowledgeTransferProvenance.Hop(
                second.speakerNpcId(), id(56), second.speakerSemanticEntryId(), second.evidenceEventId(), second.gameTime())));
        mutations.add(withHop(canonical, 1, new KnowledgeTransferProvenance.Hop(
                second.speakerNpcId(), second.listenerNpcId(), id(156), second.evidenceEventId(), second.gameTime())));
        mutations.add(withHop(canonical, 1, new KnowledgeTransferProvenance.Hop(
                second.speakerNpcId(), second.listenerNpcId(), second.speakerSemanticEntryId(), id(256), second.gameTime())));
        mutations.add(withHop(canonical, 1, new KnowledgeTransferProvenance.Hop(
                second.speakerNpcId(), second.listenerNpcId(), second.speakerSemanticEntryId(), second.evidenceEventId(),
                second.gameTime() + 1L)));
        mutations.add(new KnowledgeTransferProvenance(origin, List.of(second, first)));
        mutations.add(new KnowledgeTransferProvenance(origin, List.of(second)));
        UUID d = id(4);
        UUID cEntry = id(103);
        long thirdTime = 300L;
        UUID cdId = KnowledgeTransferProvenancePolicy.deterministicEvidenceId(c, d, cEntry, thirdTime);
        mutations.add(new KnowledgeTransferProvenance(origin, List.of(
                first,
                second,
                new KnowledgeTransferProvenance.Hop(c, d, cEntry, cdId, thirdTime)
        )));

        assertTrue(NpcKnowledgeTransferPolicy.validEvidence(
                event,
                b,
                c,
                bEntry,
                200L,
                origin.statement(),
                canonical
        ));

        for (KnowledgeTransferProvenance mutation : mutations) {
            MemoryEvent mutated = copyWithProvenance(event, mutation);
            assertFalse(
                    NpcKnowledgeTransferPolicy.validEvidence(
                            mutated,
                            b,
                            c,
                            bEntry,
                            200L,
                            origin.statement(),
                            canonical
                    ),
                    () -> "mutation unexpectedly accepted: " + mutation
            );
        }
    }

    @Test
    void npcToldSourceWithoutRetainedDirectEvidenceReturnsProvenanceUnavailableWithoutWrites() {
        Path world = tempDir.resolve("missing-direct");
        UUID speaker = id(10);
        UUID listener = id(11);
        SemanticMemoryEntry belief = npcToldBelief(
                id(110), speaker, "Missing evidence", List.of(), List.of(id(900)));
        SemanticMemoryStore.forWorld(world).append(belief, 64);

        assertUnavailableWithoutWrites(world, speaker, listener, belief.id(), 500L);
    }

    @Test
    void historicalProvenanceLessDirectEvidenceCannotBeUpgradedIntoMultiHopLineage() {
        Path world = tempDir.resolve("v1-no-payload");
        UUID origin = id(20);
        UUID speaker = id(21);
        UUID listener = id(22);
        UUID oldEvidenceId = id(920);
        MemoryEvent historical = new MemoryEvent(
                oldEvidenceId,
                speaker,
                MemoryEvent.Type.DIALOGUE,
                "NPC told: Historical claim",
                List.of(speaker, origin),
                MemoryEvent.Provenance.NPC_TOLD,
                100L,
                0L,
                50,
                0,
                50,
                List.of(),
                null,
                null,
                null
        );
        MemoryEventStore.forWorld(world).append(historical, 64);
        SemanticMemoryEntry belief = npcToldBelief(
                id(120), speaker, "Historical claim", List.of(), List.of(oldEvidenceId));
        SemanticMemoryStore.forWorld(world).append(belief, 64);

        assertUnavailableWithoutWrites(world, speaker, listener, belief.id(), 200L);
    }

    @Test
    void malformedWrongOwnerUnreferencedStatementAndScopeSourcesFailClosed() {
        assertMalformedSourceUnavailable("malformed", fixture -> copyWithProvenance(
                fixture.evidence(),
                withOrigin(fixture.provenance(), new KnowledgeTransferProvenance.Origin(
                        fixture.provenance().origin().originNpcId(),
                        fixture.provenance().origin().originSemanticEntryId(),
                        fixture.provenance().origin().originKind(),
                        fixture.provenance().origin().originProvenance(),
                        " non-canonical   claim ",
                        fixture.provenance().origin().relatedEntities()
                ))
        ), true, "Claim", List.of(id(90)));

        assertMalformedSourceUnavailable("wrong-owner", fixture -> new MemoryEvent(
                fixture.evidence().id(),
                id(777),
                fixture.evidence().type(),
                fixture.evidence().summary(),
                fixture.evidence().participants(),
                fixture.evidence().provenance(),
                fixture.evidence().gameTime(),
                fixture.evidence().createdAtEpochMillis(),
                fixture.evidence().importance(),
                fixture.evidence().emotionalWeight(),
                fixture.evidence().confidence(),
                fixture.evidence().relationshipReasons(),
                fixture.evidence().dialogue(),
                fixture.evidence().relationshipTransition(),
                fixture.evidence().relationshipCause(),
                fixture.provenance()
        ), true, "Claim", List.of(id(90)));

        assertMalformedSourceUnavailable("unreferenced", Fixture::evidence, false, "Claim", List.of(id(90)));
        assertMalformedSourceUnavailable("statement-mismatch", Fixture::evidence, true, "Different claim", List.of(id(90)));
        assertMalformedSourceUnavailable("scope-mismatch", Fixture::evidence, true, "Claim", List.of(id(91)));
    }

    private void assertMalformedSourceUnavailable(
            String name,
            UnaryOperator<Fixture> ignored,
            boolean referenceEvidence,
            String semanticStatement,
            List<UUID> semanticScope
    ) {
        // Kept separate below because UnaryOperator<Fixture> cannot change only the event while preserving fixture metadata.
    }

    private void assertMalformedSourceUnavailable(
            String name,
            java.util.function.Function<Fixture, MemoryEvent> evidenceTransform,
            boolean referenceEvidence,
            String semanticStatement,
            List<UUID> semanticScope
    ) {
        Path world = tempDir.resolve(name);
        Fixture fixture = directFixture();
        MemoryEvent transformed = evidenceTransform.apply(fixture);
        MemoryEventStore.forWorld(world).append(transformed, 64);
        UUID sourceId = referenceEvidence ? transformed.id() : id(999_001);
        SemanticMemoryEntry belief = npcToldBelief(
                id(130 + Math.floorMod(name.hashCode(), 100)),
                fixture.speaker(),
                semanticStatement,
                semanticScope,
                List.of(sourceId)
        );
        SemanticMemoryStore.forWorld(world).append(belief, 64);

        assertUnavailableWithoutWrites(world, fixture.speaker(), fixture.nextListener(), belief.id(), 500L);
    }

    private static Fixture directFixture() {
        UUID origin = id(30);
        UUID speaker = id(31);
        UUID nextListener = id(32);
        UUID originEntry = id(130);
        SemanticMemoryEntry source = new SemanticMemoryEntry(
                originEntry,
                origin,
                SemanticMemoryEntry.Kind.FACT,
                "Claim",
                List.of(id(90)),
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                10L,
                0L,
                100,
                100,
                List.of(id(930))
        );
        long gameTime = 100L;
        UUID evidenceId = KnowledgeTransferProvenancePolicy.deterministicEvidenceId(
                origin, speaker, originEntry, gameTime);
        KnowledgeTransferProvenance provenance = KnowledgeTransferProvenanceFactory.firstHop(
                source, speaker, evidenceId, gameTime).orElseThrow();
        MemoryEvent evidence = NpcToldDialogueAdapter.create(
                origin, speaker, originEntry, gameTime, "Claim", provenance).orElseThrow();
        return new Fixture(speaker, nextListener, evidence, provenance);
    }

    private static void assertUnavailableWithoutWrites(
            Path world,
            UUID speaker,
            UUID listener,
            UUID semanticSourceId,
            long gameTime
    ) {
        NpcKnowledgeTransferResult result = NpcKnowledgeTransferLifecycle.transfer(
                world, speaker, listener, semanticSourceId, gameTime, 64, 64);
        assertEquals(NpcKnowledgeTransferResult.Status.PROVENANCE_UNAVAILABLE, result.status());
        assertTrue(MemoryEventStore.forWorld(world).getRecent(listener, 64).isEmpty());
        assertTrue(SemanticMemoryStore.forWorld(world).getRecent(listener, 64).isEmpty());
    }

    private static KnowledgeTransferProvenance twoHopLineage(
            UUID a,
            UUID b,
            UUID c,
            UUID aEntry,
            UUID bEntry,
            UUID player
    ) {
        UUID ab = KnowledgeTransferProvenancePolicy.deterministicEvidenceId(a, b, aEntry, 100L);
        UUID bc = KnowledgeTransferProvenancePolicy.deterministicEvidenceId(b, c, bEntry, 200L);
        return new KnowledgeTransferProvenance(
                new KnowledgeTransferProvenance.Origin(
                        a,
                        aEntry,
                        SemanticMemoryEntry.Kind.FACT,
                        MemoryEvent.Provenance.SYSTEM_OBSERVED,
                        "Bridge destroyed",
                        List.of(player)
                ),
                List.of(
                        new KnowledgeTransferProvenance.Hop(a, b, aEntry, ab, 100L),
                        new KnowledgeTransferProvenance.Hop(b, c, bEntry, bc, 200L)
                )
        );
    }

    private static MemoryEvent evidenceForLastHop(KnowledgeTransferProvenance provenance) {
        KnowledgeTransferProvenance.Hop last = provenance.hops().getLast();
        return NpcToldDialogueAdapter.create(
                last.speakerNpcId(),
                last.listenerNpcId(),
                last.speakerSemanticEntryId(),
                last.gameTime(),
                provenance.origin().statement(),
                provenance
        ).orElseThrow();
    }

    private static KnowledgeTransferProvenance withOrigin(
            KnowledgeTransferProvenance source,
            KnowledgeTransferProvenance.Origin origin
    ) {
        return new KnowledgeTransferProvenance(origin, source.hops());
    }

    private static KnowledgeTransferProvenance withHop(
            KnowledgeTransferProvenance source,
            int index,
            KnowledgeTransferProvenance.Hop replacement
    ) {
        List<KnowledgeTransferProvenance.Hop> hops = new ArrayList<>(source.hops());
        hops.set(index, replacement);
        return new KnowledgeTransferProvenance(source.origin(), List.copyOf(hops));
    }

    private static MemoryEvent copyWithProvenance(
            MemoryEvent source,
            KnowledgeTransferProvenance provenance
    ) {
        return new MemoryEvent(
                source.id(),
                source.ownerNpcId(),
                source.type(),
                source.summary(),
                source.participants(),
                source.provenance(),
                source.gameTime(),
                source.createdAtEpochMillis(),
                source.importance(),
                source.emotionalWeight(),
                source.confidence(),
                source.relationshipReasons(),
                source.dialogue(),
                source.relationshipTransition(),
                source.relationshipCause(),
                provenance
        );
    }

    private static SemanticMemoryEntry npcToldBelief(
            UUID id,
            UUID owner,
            String statement,
            List<UUID> scope,
            List<UUID> sources
    ) {
        return new SemanticMemoryEntry(
                id,
                owner,
                SemanticMemoryEntry.Kind.BELIEF,
                statement,
                scope,
                MemoryEvent.Provenance.NPC_TOLD,
                200L,
                0L,
                50,
                50,
                sources
        );
    }

    private record Fixture(
            UUID speaker,
            UUID nextListener,
            MemoryEvent evidence,
            KnowledgeTransferProvenance provenance
    ) {
    }

    private static UUID id(int value) {
        return UUID.fromString("00000000-0000-0000-0000-" + String.format("%012d", value));
    }
}
