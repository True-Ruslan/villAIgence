package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerToldBeliefLifecycleTest {
    @TempDir
    Path tempDir;

    @Test
    void persistedDialogueSourceCreatesServerOwnedPlayerToldBelief() {
        UUID npc = UUID.randomUUID();
        UUID player = UUID.randomUUID();
        MemoryEvent source = dialogue(UUID.randomUUID(), npc, player, 100L, 70, 65);

        PlayerToldBeliefLifecycle.recordCandidatesIfEnabled(
                true,
                tempDir,
                source,
                player,
                List.of("The north bridge is unsafe."),
                3,
                16
        );

        List<SemanticMemoryEntry> entries = SemanticMemoryStore.forWorld(tempDir).getRecent(npc, 16);
        assertEquals(1, entries.size());
        SemanticMemoryEntry entry = entries.getFirst();
        assertEquals(SemanticMemoryEntry.Kind.BELIEF, entry.kind());
        assertEquals(MemoryEvent.Provenance.PLAYER_TOLD, entry.provenance());
        assertEquals(npc, entry.ownerNpcId());
        assertEquals(List.of(player), entry.relatedEntities());
        assertEquals(List.of(source.id()), entry.sourceEventIds());
        assertEquals(source.importance(), entry.importance());
        assertEquals(source.confidence(), entry.confidence());
    }

    @Test
    void disabledMissingInvalidOrEmptySourceWritesNothing() {
        UUID npc = UUID.randomUUID();
        UUID player = UUID.randomUUID();
        MemoryEvent source = dialogue(UUID.randomUUID(), npc, player, 110L, 50, 50);
        MemoryEvent wrongSource = new MemoryEvent(
                UUID.randomUUID(),
                npc,
                MemoryEvent.Type.ACTION,
                "NPC opened a door.",
                List.of(npc, player),
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                111L,
                1_700_000_000_111L,
                50,
                0,
                100,
                List.of()
        );

        PlayerToldBeliefLifecycle.recordCandidatesIfEnabled(false, tempDir, source, player, List.of("claim"), 3, 16);
        PlayerToldBeliefLifecycle.recordCandidatesIfEnabled(true, tempDir, null, player, List.of("claim"), 3, 16);
        PlayerToldBeliefLifecycle.recordCandidatesIfEnabled(true, tempDir, source, player, List.of(), 3, 16);
        PlayerToldBeliefLifecycle.recordCandidatesIfEnabled(true, tempDir, wrongSource, player, List.of("claim"), 3, 16);

        assertEquals(List.of(), SemanticMemoryStore.forWorld(tempDir).getRecent(npc, 16));
    }

    @Test
    void configuredCandidateLimitIsAppliedAfterProviderHardBound() {
        UUID npc = UUID.randomUUID();
        UUID player = UUID.randomUUID();
        MemoryEvent source = dialogue(UUID.randomUUID(), npc, player, 120L, 40, 45);

        PlayerToldBeliefLifecycle.recordCandidatesIfEnabled(
                true,
                tempDir,
                source,
                player,
                List.of("one", "two", "three", "four"),
                2,
                16
        );

        List<SemanticMemoryEntry> entries = SemanticMemoryStore.forWorld(tempDir).getRecent(npc, 16);
        assertEquals(2, entries.size());
    }

    @Test
    void replayDoesNotDuplicateAndCorroborationUnionsSourceEvidence() {
        UUID npc = UUID.randomUUID();
        UUID player = UUID.randomUUID();
        MemoryEvent first = dialogue(UUID.randomUUID(), npc, player, 130L, 60, 55);
        MemoryEvent second = dialogue(UUID.randomUUID(), npc, player, 140L, 65, 60);

        record(first, player, "The north bridge is unsafe.");
        record(first, player, "The north bridge is unsafe.");
        record(second, player, "The north bridge is unsafe.");

        List<SemanticMemoryEntry> entries = SemanticMemoryStore.forWorld(tempDir).getRecent(npc, 16);
        assertEquals(1, entries.size());
        assertEquals(
                List.of(first.id(), second.id()).stream().sorted().toList(),
                entries.getFirst().sourceEventIds().stream().sorted().toList()
        );
    }

    private void record(MemoryEvent source, UUID player, String candidate) {
        PlayerToldBeliefLifecycle.recordCandidatesIfEnabled(
                true,
                tempDir,
                source,
                player,
                List.of(candidate),
                3,
                16
        );
    }

    private static MemoryEvent dialogue(
            UUID id,
            UUID npc,
            UUID player,
            long gameTime,
            int importance,
            int confidence
    ) {
        return new MemoryEvent(
                id,
                npc,
                MemoryEvent.Type.DIALOGUE,
                "Dialogue evidence",
                List.of(npc, player),
                MemoryEvent.Provenance.PLAYER_TOLD,
                gameTime,
                1_700_000_000_000L + gameTime,
                importance,
                0,
                confidence,
                List.of(),
                new MemoryEvent.DialogueExchange("Source utterance", "NPC reply")
        );
    }
}
