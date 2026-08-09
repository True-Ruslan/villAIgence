package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcKnowledgeTransferLifecyclePolicyTest {
    @TempDir
    Path tempDir;

    @Test
    void rejectsFirstHopSelfTransferAsProvenanceCycle() {
        Path world = tempDir.resolve("self-cycle");
        UUID npc = id(1);
        SemanticMemoryEntry source = fact(id(101), npc, "Self cycle claim");
        SemanticMemoryStore.forWorld(world).append(source, 64);

        NpcKnowledgeTransferResult result = NpcKnowledgeTransferLifecycle.transfer(
                world, npc, npc, source.id(), 100L, 64, 64);

        assertEquals(NpcKnowledgeTransferResult.Status.PROVENANCE_CYCLE, result.status());
        assertTrue(MemoryEventStore.forWorld(world).getRecent(npc, 64).isEmpty());
    }

    @Test
    void rejectsReturnToAnyNpcAlreadyInSelectedPath() {
        Path world = tempDir.resolve("path-cycle");
        UUID a = id(10);
        UUID b = id(11);
        UUID c = id(12);
        SemanticMemoryEntry source = fact(id(110), a, "Cycle claim");
        SemanticMemoryStore.forWorld(world).append(source, 64);

        NpcKnowledgeTransferResult ab = transfer(world, a, b, source.id(), 100L);
        NpcKnowledgeTransferResult bc = transfer(world, b, c, ab.semanticEntryId(), 200L);
        NpcKnowledgeTransferResult ca = transfer(world, c, a, bc.semanticEntryId(), 300L);
        NpcKnowledgeTransferResult cb = transfer(world, c, b, bc.semanticEntryId(), 301L);

        assertEquals(NpcKnowledgeTransferResult.Status.PROVENANCE_CYCLE, ca.status());
        assertEquals(NpcKnowledgeTransferResult.Status.PROVENANCE_CYCLE, cb.status());
        assertTrue(MemoryEventStore.forWorld(world).findById(
                a,
                NpcToldDialogueAdapter.deterministicEvidenceId(c, a, bc.semanticEntryId(), 300L)
        ).isEmpty());
        assertTrue(MemoryEventStore.forWorld(world).findById(
                b,
                NpcToldDialogueAdapter.deterministicEvidenceId(c, b, bc.semanticEntryId(), 301L)
        ).isEmpty());
    }

    @Test
    void admitsExactlyEightHopsThenRejectsNinthAndCycleWinsAtLimit() {
        Path world = tempDir.resolve("hop-limit");
        List<UUID> npcs = new ArrayList<>();
        for (int index = 0; index < 10; index++) npcs.add(id(200 + index));
        SemanticMemoryEntry source = fact(id(300), npcs.getFirst(), "Eight hop claim");
        SemanticMemoryStore.forWorld(world).append(source, 256);

        UUID currentSource = source.id();
        for (int hop = 0; hop < 8; hop++) {
            NpcKnowledgeTransferResult result = NpcKnowledgeTransferLifecycle.transfer(
                    world,
                    npcs.get(hop),
                    npcs.get(hop + 1),
                    currentSource,
                    1_000L + hop,
                    256,
                    256
            );
            assertEquals(NpcKnowledgeTransferResult.Status.ADMITTED, result.status(), "hop " + (hop + 1));
            currentSource = result.semanticEntryId();
        }

        NpcKnowledgeTransferResult ninth = NpcKnowledgeTransferLifecycle.transfer(
                world, npcs.get(8), npcs.get(9), currentSource, 2_000L, 256, 256);
        assertEquals(NpcKnowledgeTransferResult.Status.PROVENANCE_LIMIT_REACHED, ninth.status());

        NpcKnowledgeTransferResult cycleAtLimit = NpcKnowledgeTransferLifecycle.transfer(
                world, npcs.get(8), npcs.getFirst(), currentSource, 2_001L, 256, 256);
        assertEquals(NpcKnowledgeTransferResult.Status.PROVENANCE_CYCLE, cycleAtLimit.status());
    }

    @Test
    void doesNotFallbackToLowerBranchWhenCanonicalBranchCyclesWithListener() {
        Path world = tempDir.resolve("no-fallback");
        UUID a = id(400);
        UUID x = id(401);
        UUID b = id(402);
        SemanticMemoryEntry sourceA = fact(id(410), a, "Shared claim");
        SemanticMemoryEntry sourceX = fact(id(411), x, "Shared claim");
        SemanticMemoryStore semanticStore = SemanticMemoryStore.forWorld(world);
        semanticStore.append(sourceA, 64);
        semanticStore.append(sourceX, 64);

        NpcKnowledgeTransferResult ab = transfer(world, a, b, sourceA.id(), 500L);
        NpcKnowledgeTransferResult xb = transfer(world, x, b, sourceX.id(), 400L);
        assertEquals(NpcKnowledgeTransferResult.Status.ADMITTED, ab.status());
        assertEquals(NpcKnowledgeTransferResult.Status.ADMITTED, xb.status());

        SemanticMemoryEntry consolidated = semanticStore.getRecent(b, 64).stream()
                .filter(entry -> entry.statement().equals("Shared claim"))
                .findFirst()
                .orElseThrow();
        assertTrue(consolidated.sourceEventIds().contains(ab.evidenceEventId()));
        assertTrue(consolidated.sourceEventIds().contains(xb.evidenceEventId()));

        NpcKnowledgeTransferResult ba = transfer(world, b, a, consolidated.id(), 600L);

        assertEquals(NpcKnowledgeTransferResult.Status.PROVENANCE_CYCLE, ba.status());
        UUID wouldBeEvidence = NpcToldDialogueAdapter.deterministicEvidenceId(
                b, a, consolidated.id(), 600L);
        assertTrue(MemoryEventStore.forWorld(world).findById(a, wouldBeEvidence).isEmpty());
    }

    private static NpcKnowledgeTransferResult transfer(
            Path world,
            UUID speaker,
            UUID listener,
            UUID sourceId,
            long gameTime
    ) {
        return NpcKnowledgeTransferLifecycle.transfer(
                world, speaker, listener, sourceId, gameTime, 256, 256);
    }

    private static SemanticMemoryEntry fact(UUID entryId, UUID owner, String statement) {
        return new SemanticMemoryEntry(
                entryId,
                owner,
                SemanticMemoryEntry.Kind.FACT,
                statement,
                List.of(),
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                10L,
                0L,
                100,
                100,
                List.of(id(9000 + Math.floorMod(entryId.hashCode(), 500)))
        );
    }

    private static UUID id(int value) {
        return UUID.fromString("00000000-0000-0000-0000-" + String.format("%012d", value));
    }
}
