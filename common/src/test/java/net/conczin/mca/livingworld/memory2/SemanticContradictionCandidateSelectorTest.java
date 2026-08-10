package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SemanticContradictionCandidateSelectorTest {
    @TempDir
    Path tempDir;

    @Test
    void filtersScopeAndEquivalentClaimsBeforeBoundedAllocation() {
        UUID npc = id(1);
        UUID player = id(90);
        UUID foreignPlayer = id(91);
        SemanticMemoryStore store = SemanticMemoryStore.forWorld(tempDir);

        for (int i = 0; i < 20; i++) {
            store.append(belief(id(100 + i), npc, "Foreign claim " + i, List.of(foreignPlayer), 1_000L + i), 64);
        }

        SemanticMemoryEntry oldestEligible = belief(id(10), npc, "The gate is not open", List.of(player), 10L);
        SemanticMemoryEntry middleEligible = belief(id(11), npc, "The bridge is not safe", List.of(player), 20L);
        SemanticMemoryEntry newestEligible = belief(id(12), npc, "The mill is not running", List.of(player), 30L);
        store.append(oldestEligible, 64);
        store.append(middleEligible, 64);
        store.append(newestEligible, 64);

        SemanticMemoryEntry equivalent = belief(id(13), npc, "  THE   GATE IS OPEN  ", List.of(player), 40L);
        store.append(equivalent, 64);

        SemanticMemoryEntry subject = fact(id(20), npc, "The gate is open", List.of(player), 50L);
        store.append(subject, 64);

        List<SemanticMemoryEntry> selected = SemanticContradictionCandidateSelector.select(store, subject);

        assertEquals(3, selected.size());
        assertEquals(List.of(newestEligible.id(), middleEligible.id(), oldestEligible.id()),
                selected.stream().map(SemanticMemoryEntry::id).toList());
        assertFalse(selected.stream().anyMatch(entry -> entry.relatedEntities().contains(foreignPlayer)));
        assertFalse(selected.stream().anyMatch(entry -> entry.id().equals(equivalent.id())));
    }

    @Test
    void candidateCountIsHardBoundedAndCrossOwnerCannotEnter() {
        UUID npc = id(1);
        UUID otherNpc = id(2);
        UUID player = id(90);
        SemanticMemoryStore store = SemanticMemoryStore.forWorld(tempDir.resolve("bounded"));

        for (int i = 0; i < 30; i++) {
            store.append(belief(id(200 + i), npc, "Claim " + i, List.of(player), 100L + i), 64);
            store.append(belief(id(400 + i), otherNpc, "Other NPC " + i, List.of(player), 100L + i), 64);
        }
        SemanticMemoryEntry subject = fact(id(500), npc, "Subject claim", List.of(player), 1_000L);
        store.append(subject, 64);

        List<SemanticMemoryEntry> selected = SemanticContradictionCandidateSelector.select(store, subject);

        assertEquals(SemanticContradictionCandidateSelector.MAX_CANDIDATES_PER_ADMISSION, selected.size());
        assertTrue(selected.stream().allMatch(entry -> entry.ownerNpcId().equals(npc)));
    }

    private static SemanticMemoryEntry fact(UUID entryId, UUID npc, String statement, List<UUID> scope, long gameTime) {
        return new SemanticMemoryEntry(
                entryId,
                npc,
                SemanticMemoryEntry.Kind.FACT,
                statement,
                scope,
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                gameTime,
                0L,
                90,
                100,
                List.of(id(9000 + (int) gameTime))
        );
    }

    private static SemanticMemoryEntry belief(UUID entryId, UUID npc, String statement, List<UUID> scope, long gameTime) {
        return new SemanticMemoryEntry(
                entryId,
                npc,
                SemanticMemoryEntry.Kind.BELIEF,
                statement,
                scope,
                MemoryEvent.Provenance.NPC_TOLD,
                gameTime,
                0L,
                50,
                50,
                List.of(id(8000 + (int) gameTime))
        );
    }

    private static UUID id(int value) {
        return UUID.fromString("00000000-0000-0000-0000-" + String.format("%012d", value));
    }
}
