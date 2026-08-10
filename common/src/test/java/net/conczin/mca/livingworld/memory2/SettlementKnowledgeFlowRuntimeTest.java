package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettlementKnowledgeFlowRuntimeTest {
    @TempDir
    Path tempDir;

    @Test
    void disabledRuntimeAdapterIsStrictNoOp() {
        Path world = tempDir.resolve("disabled");
        UUID speaker = id(1);
        UUID listener = id(2);
        SemanticMemoryStore.forWorld(world).append(fact(id(100), speaker, 100L), 64);

        SettlementKnowledgeFlowRuntime.runIfEnabled(
                false, world, 1, 2_400L, List.of(speaker, listener), 64);

        assertTrue(SemanticMemoryStore.forWorld(world).getRecent(listener, 64).isEmpty());
        assertTrue(MemoryEventStore.forWorld(world).getRecent(listener, 64).isEmpty());
    }

    @Test
    void enabledRuntimeAdapterDelegatesOneBoundedCycle() {
        Path world = tempDir.resolve("enabled");
        UUID speaker = id(10);
        UUID listener = id(11);
        SemanticMemoryStore.forWorld(world).append(fact(id(110), speaker, 100L), 64);

        SettlementKnowledgeFlowRuntime.runIfEnabled(
                true, world, 2, 2_400L, List.of(speaker, listener), 64);

        assertEquals(1, SemanticMemoryStore.forWorld(world).getRecent(listener, 64).size());
        assertEquals(SemanticMemoryEntry.Kind.BELIEF,
                SemanticMemoryStore.forWorld(world).getRecent(listener, 64).getFirst().kind());
    }

    private static SemanticMemoryEntry fact(UUID id, UUID npc, long gameTime) {
        return new SemanticMemoryEntry(
                id,
                npc,
                SemanticMemoryEntry.Kind.FACT,
                "The market is closed",
                List.of(),
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                gameTime,
                0L,
                90,
                100,
                List.of(UUID.nameUUIDFromBytes(("source-" + id).getBytes()))
        );
    }

    private static UUID id(int value) {
        return UUID.fromString("00000000-0000-0000-0000-" + String.format("%012d", value));
    }
}
