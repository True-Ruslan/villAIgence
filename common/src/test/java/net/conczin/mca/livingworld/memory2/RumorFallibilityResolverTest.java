package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RumorFallibilityResolverTest {
    @TempDir
    Path tempDir;

    @Test
    void usesSameNewestValidCanonicalDirectBranchAsProvenanceResolver() {
        Path world = tempDir.resolve("canonical-branch");
        UUID a = id(1);
        UUID b = id(2);
        UUID c = id(3);
        UUID d = id(4);
        UUID source = id(101);
        seedFact(world, a, source, "The mill bridge is closed");

        NpcKnowledgeTransferResult direct = transfer(world, a, b, source, 100L, 64, 64);
        NpcKnowledgeTransferResult ac = transfer(world, a, c, source, 110L, 64, 64);
        NpcKnowledgeTransferResult cd = transfer(world, c, d, ac.semanticEntryId(), 120L, 64, 64);
        NpcKnowledgeTransferResult db = transfer(world, d, b, cd.semanticEntryId(), 130L, 64, 64);
        assertEquals(NpcKnowledgeTransferResult.Status.ADMITTED, direct.status());
        assertEquals(NpcKnowledgeTransferResult.Status.ADMITTED, db.status());

        SemanticMemoryEntry bRumor = SemanticMemoryStore.forWorld(world)
                .findMatching(b, entry -> entry.kind() == SemanticMemoryEntry.Kind.BELIEF
                        && entry.provenance() == MemoryEvent.Provenance.NPC_TOLD
                        && "The mill bridge is closed".equals(entry.statement()))
                .orElseThrow();
        assertTrue(bRumor.sourceEventIds().contains(direct.evidenceEventId()));
        assertTrue(bRumor.sourceEventIds().contains(db.evidenceEventId()));

        RumorFallibilityState state = RumorFallibilityResolver.resolve(
                MemoryEventStore.forWorld(world), bRumor).orElseThrow();

        assertEquals(RumorFallibilityState.SourcePath.RESOLVED, state.sourcePath());
        assertEquals(3, state.sourceDistanceHops());
        assertEquals(0, state.transformationsUsed());
    }

    @Test
    void retainedRumorWithForgottenDirectEvidenceIsExplicitlyUnresolved() {
        Path world = tempDir.resolve("forgotten-direct");
        UUID a = id(10);
        UUID b = id(11);
        UUID source = id(110);
        seedFact(world, a, source, "The orchard gate is broken");

        NpcKnowledgeTransferResult transfer = transfer(world, a, b, source, 100L, 64, 64);
        SemanticMemoryEntry rumor = SemanticMemoryStore.forWorld(world)
                .findById(b, transfer.semanticEntryId()).orElseThrow();
        MemoryEventStore.forWorld(world).append(strongObservedEvent(b, 1_000L), 1);
        assertTrue(MemoryEventStore.forWorld(world).findById(b, transfer.evidenceEventId()).isEmpty());

        RumorFallibilityState state = RumorFallibilityResolver.resolve(
                MemoryEventStore.forWorld(world), rumor).orElseThrow();

        assertEquals(RumorFallibilityState.SourcePath.UNRESOLVED, state.sourcePath());
        assertEquals(0, state.sourceDistanceHops());
        assertEquals(0, state.transformationsUsed());
    }

    @Test
    void nonNpcToldSemanticEntriesHaveNoRumorFallibilityState() {
        MemoryEventStore events = MemoryEventStore.forWorld(tempDir.resolve("not-applicable"));
        SemanticMemoryEntry fact = semantic(id(201), id(20), SemanticMemoryEntry.Kind.FACT,
                MemoryEvent.Provenance.SYSTEM_OBSERVED);
        SemanticMemoryEntry playerTold = semantic(id(202), id(20), SemanticMemoryEntry.Kind.BELIEF,
                MemoryEvent.Provenance.PLAYER_TOLD);
        SemanticMemoryEntry inferred = semantic(id(203), id(20), SemanticMemoryEntry.Kind.BELIEF,
                MemoryEvent.Provenance.INFERRED);

        assertEquals(Optional.empty(), RumorFallibilityResolver.resolve(events, fact));
        assertEquals(Optional.empty(), RumorFallibilityResolver.resolve(events, playerTold));
        assertEquals(Optional.empty(), RumorFallibilityResolver.resolve(events, inferred));
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

    private static SemanticMemoryEntry semantic(
            UUID id,
            UUID owner,
            SemanticMemoryEntry.Kind kind,
            MemoryEvent.Provenance provenance
    ) {
        return new SemanticMemoryEntry(
                id, owner, kind, "Claim", List.of(), provenance,
                10L, 0L, 50, 50, List.of(id(999))
        );
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
