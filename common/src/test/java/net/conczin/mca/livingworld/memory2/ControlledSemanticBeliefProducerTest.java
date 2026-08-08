package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ControlledSemanticBeliefProducerTest {
    @TempDir
    Path tempDir;

    @Test
    void exactReplayIsIdempotentAndDisabledProducerWritesNothing() {
        UUID npc = UUID.randomUUID();
        UUID player = UUID.randomUUID();
        MemoryEvent source = dialogue(UUID.randomUUID(), npc, player, 10L);

        ControlledSemanticBeliefProducer.recordIfEnabled(
                true,
                tempDir,
                source,
                "The north bridge is unsafe.",
                List.of(player),
                MemoryEvent.Provenance.PLAYER_TOLD,
                70,
                65,
                8
        );
        ControlledSemanticBeliefProducer.recordIfEnabled(
                true,
                tempDir,
                source,
                "The north bridge is unsafe.",
                List.of(player),
                MemoryEvent.Provenance.PLAYER_TOLD,
                70,
                65,
                8
        );
        ControlledSemanticBeliefProducer.recordIfEnabled(
                false,
                tempDir,
                dialogue(UUID.randomUUID(), npc, player, 20L),
                "Disabled claim",
                List.of(player),
                MemoryEvent.Provenance.PLAYER_TOLD,
                50,
                50,
                8
        );

        List<SemanticMemoryEntry> entries = SemanticMemoryStore.forWorld(tempDir).getRecent(npc, 8);
        assertEquals(1, entries.size());
        assertEquals(SemanticMemoryEntry.Kind.BELIEF, entries.getFirst().kind());
        assertEquals(List.of(source.id()), entries.getFirst().sourceEventIds());
    }

    @Test
    void corroboratingClaimsConsolidateSourceEvidenceInsteadOfDuplicatingBelief() {
        UUID npc = UUID.randomUUID();
        UUID player = UUID.randomUUID();
        MemoryEvent first = dialogue(UUID.randomUUID(), npc, player, 30L);
        MemoryEvent second = dialogue(UUID.randomUUID(), npc, player, 40L);

        record(first, player);
        record(second, player);

        List<SemanticMemoryEntry> entries = SemanticMemoryStore.forWorld(tempDir).getRecent(npc, 8);
        assertEquals(1, entries.size());
        assertEquals(2, entries.getFirst().sourceEventIds().size());
        assertEquals(
                List.of(first.id(), second.id()).stream().sorted().toList(),
                entries.getFirst().sourceEventIds().stream().sorted().toList()
        );
    }

    @Test
    void rejectedAdmissionDoesNotCreateSemanticEntry() {
        UUID npc = UUID.randomUUID();
        UUID player = UUID.randomUUID();
        MemoryEvent source = dialogue(UUID.randomUUID(), npc, player, 50L);

        ControlledSemanticBeliefProducer.recordIfEnabled(
                true,
                tempDir,
                source,
                "This must not become authoritative truth.",
                List.of(player),
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                100,
                100,
                8
        );

        assertEquals(List.of(), SemanticMemoryStore.forWorld(tempDir).getRecent(npc, 8));
    }

    private void record(MemoryEvent source, UUID player) {
        ControlledSemanticBeliefProducer.recordIfEnabled(
                true,
                tempDir,
                source,
                "The north bridge is unsafe.",
                List.of(player),
                MemoryEvent.Provenance.PLAYER_TOLD,
                70,
                65,
                8
        );
    }

    private static MemoryEvent dialogue(UUID id, UUID npc, UUID player, long gameTime) {
        return new MemoryEvent(
                id,
                npc,
                MemoryEvent.Type.DIALOGUE,
                "Dialogue evidence",
                List.of(npc, player),
                MemoryEvent.Provenance.PLAYER_TOLD,
                gameTime,
                1_700_000_000_000L + gameTime,
                40,
                0,
                60,
                List.of(),
                new MemoryEvent.DialogueExchange("Source utterance", "NPC reply")
        );
    }
}
