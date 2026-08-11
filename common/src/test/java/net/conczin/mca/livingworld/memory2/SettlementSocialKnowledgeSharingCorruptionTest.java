package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettlementSocialKnowledgeSharingCorruptionTest {
    @TempDir
    Path tempDir;

    @Test
    void malformedSocialGraphFailsClosedWithoutRecoveryMutationOrTransfer() throws IOException {
        Path world = tempDir.resolve("malformed-graph");
        UUID speaker = id(1);
        UUID listener = id(2);
        appendSourceFact(world, speaker, id(100), "The north gate is open");

        Path graphFile = world.resolve("livingworld").resolve("npc-social-graph.json");
        Files.createDirectories(graphFile.getParent());
        Files.writeString(graphFile, "{ definitely-not-valid-json");
        byte[] before = Files.readAllBytes(graphFile);

        SettlementKnowledgeFlowLifecycle.CycleResult result = SettlementKnowledgeFlowLifecycle.runCycle(
                world, 31, 2_400L, List.of(speaker, listener), 64, 64);

        assertEquals(1, result.opportunities());
        assertEquals(1, result.sociallySuppressedTransfers());
        assertEquals(0, result.attemptedTransfers());
        assertEquals(0, result.successfulTransfers());
        assertEquals(List.of(), result.statuses());
        assertTrue(SemanticMemoryStore.forWorld(world).getRecent(listener, 64).isEmpty());
        assertArrayEquals(before, Files.readAllBytes(graphFile));
        assertFalse(Files.exists(graphFile.resolveSibling("npc-social-graph.json.corrupt")));
    }

    private static void appendSourceFact(Path world, UUID speaker, UUID sourceId, String statement) {
        SemanticMemoryStore.forWorld(world).append(new SemanticMemoryEntry(
                sourceId,
                speaker,
                SemanticMemoryEntry.Kind.FACT,
                statement,
                List.of(),
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                100L,
                0L,
                90,
                100,
                List.of(id(900))
        ), 64);
    }

    private static UUID id(int value) {
        return UUID.fromString("00000000-0000-0000-0000-" + String.format("%012d", value));
    }
}
