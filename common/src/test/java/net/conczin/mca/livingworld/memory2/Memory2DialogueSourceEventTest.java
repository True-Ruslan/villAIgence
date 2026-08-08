package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Memory2DialogueSourceEventTest {
    @TempDir
    Path tempDir;

    @Test
    void successfulIngestionReturnsTheExactPersistedDialogueEvent() {
        UUID npc = UUID.randomUUID();
        UUID player = UUID.randomUUID();

        Optional<MemoryEvent> source = Memory2DialogueIngestor.record(
                tempDir,
                npc,
                player,
                120L,
                "The north bridge is unsafe.",
                "I will remember that.",
                16,
                1_700_000_000_120L
        );

        MemoryEvent event = source.orElseThrow();
        List<MemoryEvent> persisted = MemoryEventStore.forWorld(tempDir).getRecent(npc, 16);
        assertEquals(1, persisted.size());
        assertEquals(event.id(), persisted.getFirst().id());
        assertEquals(MemoryEvent.Type.DIALOGUE, event.type());
        assertEquals(MemoryEvent.Provenance.PLAYER_TOLD, event.provenance());
    }

    @Test
    void lifecycleReturnsEmptyWhenDisabledOrAnswerIsUnusable() {
        UUID npc = UUID.randomUUID();
        UUID player = UUID.randomUUID();

        assertTrue(Memory2DialogueLifecycle.recordSuccessful(
                false,
                tempDir,
                npc,
                player,
                120L,
                "hello",
                Optional.of("reply"),
                16,
                1_000L
        ).isEmpty());
        assertTrue(Memory2DialogueLifecycle.recordSuccessful(
                true,
                tempDir,
                npc,
                player,
                121L,
                "hello",
                Optional.empty(),
                16,
                2_000L
        ).isEmpty());
        assertTrue(Memory2DialogueLifecycle.recordSuccessful(
                true,
                tempDir,
                npc,
                player,
                122L,
                "hello",
                Optional.of("   "),
                16,
                3_000L
        ).isEmpty());
    }

    @Test
    void replayReturnsTheSameDeterministicSourceIdentityWithoutDuplicatingStore() {
        UUID npc = UUID.randomUUID();
        UUID player = UUID.randomUUID();

        MemoryEvent first = Memory2DialogueLifecycle.recordSuccessful(
                true,
                tempDir,
                npc,
                player,
                130L,
                "I live by the mill.",
                Optional.of("Understood."),
                16,
                1_000L
        ).orElseThrow();
        MemoryEvent replay = Memory2DialogueLifecycle.recordSuccessful(
                true,
                tempDir,
                npc,
                player,
                130L,
                "  I live by   the mill. ",
                Optional.of("Different presentation should not change source identity."),
                16,
                9_999L
        ).orElseThrow();

        assertEquals(first.id(), replay.id());
        assertEquals(1, MemoryEventStore.forWorld(tempDir).getRecent(npc, 16).size());
    }
}
