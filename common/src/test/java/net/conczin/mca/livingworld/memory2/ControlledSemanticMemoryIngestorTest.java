package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ControlledSemanticMemoryIngestorTest {
    @TempDir
    Path tempDir;

    @Test
    void persistsFactIdempotentlyAndHonorsDisabledFlag() {
        UUID npc = UUID.randomUUID();
        MemoryEvent source = event(UUID.randomUUID(), npc, 10L, "first");
        MemoryEvent disabled = event(UUID.randomUUID(), npc, 20L, "disabled");

        ControlledSemanticMemoryIngestor.recordFact(tempDir, source, 8);
        ControlledSemanticMemoryIngestor.recordFact(tempDir, source, 8);
        ControlledSemanticMemoryIngestor.recordFactIfEnabled(false, tempDir, disabled, 8);

        List<SemanticMemoryEntry> entries = SemanticMemoryStore.forWorld(tempDir).getRecent(npc, 8);
        assertEquals(1, entries.size());
        assertEquals(List.of(source.id()), entries.getFirst().sourceEventIds());
        assertEquals("first", entries.getFirst().statement());
    }

    @Test
    void persistsExplicitBeliefWithOriginalProvenance() {
        UUID npc = UUID.randomUUID();
        UUID player = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        SemanticBeliefSource source = new SemanticBeliefSource(
                npc,
                "Player says the bridge is unsafe.",
                List.of(npc, player),
                MemoryEvent.Provenance.PLAYER_TOLD,
                30L,
                40L,
                70,
                65,
                List.of(sourceId)
        );

        ControlledSemanticMemoryIngestor.recordBelief(tempDir, source, 8);
        ControlledSemanticMemoryIngestor.recordBelief(tempDir, source, 8);

        List<SemanticMemoryEntry> entries = SemanticMemoryStore.forWorld(tempDir).getRecent(npc, 8);
        assertEquals(1, entries.size());
        assertEquals(SemanticMemoryEntry.Kind.BELIEF, entries.getFirst().kind());
        assertEquals(MemoryEvent.Provenance.PLAYER_TOLD, entries.getFirst().provenance());
        assertEquals(List.of(sourceId), entries.getFirst().sourceEventIds());
    }

    @Test
    void ineligibleAutomaticSourceWritesNothing() {
        UUID npc = UUID.randomUUID();
        MemoryEvent dialogue = new MemoryEvent(
                UUID.randomUUID(),
                npc,
                MemoryEvent.Type.DIALOGUE,
                "Player said something",
                List.of(npc, UUID.randomUUID()),
                MemoryEvent.Provenance.PLAYER_TOLD,
                10L,
                20L,
                40,
                0,
                60,
                List.of()
        );

        ControlledSemanticMemoryIngestor.recordFact(tempDir, dialogue, 8);

        assertEquals(List.of(), SemanticMemoryStore.forWorld(tempDir).getRecent(npc, 8));
    }

    private static MemoryEvent event(UUID id, UUID npc, long gameTime, String summary) {
        return new MemoryEvent(
                id,
                npc,
                MemoryEvent.Type.ACTION,
                summary,
                List.of(npc, UUID.randomUUID()),
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                gameTime,
                1_700_000_000_000L + gameTime,
                60,
                0,
                100,
                List.of()
        );
    }
}
