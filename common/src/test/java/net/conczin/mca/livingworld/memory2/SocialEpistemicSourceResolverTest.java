package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SocialEpistemicSourceResolverTest {
    @TempDir
    Path tempDir;

    @Test
    void directPlayerToldBeliefResolvesPlayerOnlyFromRetainedDialogueEvidence() {
        Path world = tempDir.resolve("direct");
        UUID npc = id(1);
        UUID player = id(2);
        MemoryEvent dialogue = dialogue(npc, player, 1_000L, "The east bridge is closed");
        MemoryEventStore.forWorld(world).append(dialogue, 64);
        SemanticMemoryEntry belief = belief(
                id(10), npc, MemoryEvent.Provenance.PLAYER_TOLD,
                "The east bridge is closed", List.of(player), 1_000L, List.of(dialogue.id())
        );
        SemanticMemoryStore.forWorld(world).append(belief, 64);

        assertEquals(
                player,
                SocialEpistemicSourceResolver.resolvePlayer(
                        SemanticMemoryStore.forWorld(world),
                        MemoryEventStore.forWorld(world),
                        belief
                ).orElseThrow()
        );
    }

    @Test
    void missingOrMalformedDirectEvidenceFailsClosedEvenWhenScopeContainsAPlayer() {
        Path world = tempDir.resolve("missing");
        UUID npc = id(20);
        UUID player = id(21);
        SemanticMemoryEntry missing = belief(
                id(22), npc, MemoryEvent.Provenance.PLAYER_TOLD,
                "The east bridge is closed", List.of(player), 1_000L, List.of(id(999))
        );

        assertTrue(SocialEpistemicSourceResolver.resolvePlayer(
                SemanticMemoryStore.forWorld(world),
                MemoryEventStore.forWorld(world),
                missing
        ).isEmpty());

        MemoryEvent malformed = new MemoryEvent(
                id(23), npc, MemoryEvent.Type.OBSERVATION, "Observed something",
                List.of(npc, player), MemoryEvent.Provenance.PLAYER_TOLD,
                1_000L, 0L, 50, 0, 50, List.of()
        );
        MemoryEventStore.forWorld(world).append(malformed, 64);
        SemanticMemoryEntry malformedSource = belief(
                id(24), npc, MemoryEvent.Provenance.PLAYER_TOLD,
                "The east bridge is closed", List.of(player), 1_000L, List.of(malformed.id())
        );

        assertTrue(SocialEpistemicSourceResolver.resolvePlayer(
                SemanticMemoryStore.forWorld(world),
                MemoryEventStore.forWorld(world),
                malformedSource
        ).isEmpty());
    }

    @Test
    void conflictingRetainedPlayerSourcesFailClosedInsteadOfChoosingOne() {
        Path world = tempDir.resolve("conflicting");
        UUID npc = id(30);
        UUID playerA = id(31);
        UUID playerB = id(32);
        MemoryEvent first = dialogue(npc, playerA, 1_000L, "The bell is broken");
        MemoryEvent second = dialogue(npc, playerB, 1_100L, "The bell is broken");
        MemoryEventStore.forWorld(world).append(first, 64);
        MemoryEventStore.forWorld(world).append(second, 64);
        SemanticMemoryEntry belief = belief(
                id(33), npc, MemoryEvent.Provenance.PLAYER_TOLD,
                "The bell is broken", List.of(), 1_100L, List.of(first.id(), second.id())
        );

        assertTrue(SocialEpistemicSourceResolver.resolvePlayer(
                SemanticMemoryStore.forWorld(world),
                MemoryEventStore.forWorld(world),
                belief
        ).isEmpty());
    }

    @Test
    void npcToldRumorWithPlayerToldOriginResolvesOriginalPlayerThroughRealTransferEvidence() {
        Path world = tempDir.resolve("rumor");
        UUID speaker = id(40);
        UUID listener = id(41);
        UUID player = id(42);
        MemoryEvent dialogue = dialogue(speaker, player, 1_000L, "The quarry is unsafe");
        MemoryEventStore.forWorld(world).append(dialogue, 64);
        SemanticMemoryEntry origin = belief(
                id(43), speaker, MemoryEvent.Provenance.PLAYER_TOLD,
                "The quarry is unsafe", List.of(player), 1_000L, List.of(dialogue.id())
        );
        SemanticMemoryStore.forWorld(world).append(origin, 64);

        NpcKnowledgeTransferResult transfer = NpcKnowledgeTransferLifecycle.transfer(
                world, speaker, listener, origin.id(), 2_400L, 64, 64
        );
        assertEquals(NpcKnowledgeTransferResult.Status.ADMITTED, transfer.status());
        SemanticMemoryEntry rumor = SemanticMemoryStore.forWorld(world)
                .findById(listener, transfer.semanticEntryId())
                .orElseThrow();

        assertEquals(
                player,
                SocialEpistemicSourceResolver.resolvePlayer(
                        SemanticMemoryStore.forWorld(world),
                        MemoryEventStore.forWorld(world),
                        rumor
                ).orElseThrow()
        );
    }

    @Test
    void npcToldRumorWithFactOrInferredOriginHasNoPlayerTrustSource() {
        Path world = tempDir.resolve("non-player-origin");
        UUID speakerFact = id(50);
        UUID listenerFact = id(51);
        UUID speakerInferred = id(52);
        UUID listenerInferred = id(53);

        SemanticMemoryEntry fact = new SemanticMemoryEntry(
                id(54), speakerFact, SemanticMemoryEntry.Kind.FACT,
                "The gate is open", List.of(), MemoryEvent.Provenance.SYSTEM_OBSERVED,
                1_000L, 0L, 90, 100, List.of(id(55))
        );
        SemanticMemoryStore.forWorld(world).append(fact, 64);
        NpcKnowledgeTransferResult factTransfer = NpcKnowledgeTransferLifecycle.transfer(
                world, speakerFact, listenerFact, fact.id(), 2_400L, 64, 64
        );
        assertEquals(NpcKnowledgeTransferResult.Status.ADMITTED, factTransfer.status());
        SemanticMemoryEntry factRumor = SemanticMemoryStore.forWorld(world)
                .findById(listenerFact, factTransfer.semanticEntryId()).orElseThrow();
        assertTrue(SocialEpistemicSourceResolver.resolvePlayer(
                SemanticMemoryStore.forWorld(world), MemoryEventStore.forWorld(world), factRumor
        ).isEmpty());

        MemoryEvent inferenceEvidence = new MemoryEvent(
                id(56), speakerInferred, MemoryEvent.Type.OBSERVATION, "Saw tracks",
                List.of(speakerInferred), MemoryEvent.Provenance.SYSTEM_OBSERVED,
                1_000L, 0L, 60, 0, 90, List.of()
        );
        MemoryEventStore.forWorld(world).append(inferenceEvidence, 64);
        SemanticMemoryEntry inferred = belief(
                id(57), speakerInferred, MemoryEvent.Provenance.INFERRED,
                "Someone visited the mine", List.of(), 1_000L, List.of(inferenceEvidence.id())
        );
        SemanticMemoryStore.forWorld(world).append(inferred, 64);
        NpcKnowledgeTransferResult inferredTransfer = NpcKnowledgeTransferLifecycle.transfer(
                world, speakerInferred, listenerInferred, inferred.id(), 2_400L, 64, 64
        );
        assertEquals(NpcKnowledgeTransferResult.Status.ADMITTED, inferredTransfer.status());
        SemanticMemoryEntry inferredRumor = SemanticMemoryStore.forWorld(world)
                .findById(listenerInferred, inferredTransfer.semanticEntryId()).orElseThrow();
        assertTrue(SocialEpistemicSourceResolver.resolvePlayer(
                SemanticMemoryStore.forWorld(world), MemoryEventStore.forWorld(world), inferredRumor
        ).isEmpty());
    }

    private static MemoryEvent dialogue(UUID npc, UUID player, long gameTime, String playerMessage) {
        return DialogueMemoryAdapter.toMemoryEvent(
                npc, player, gameTime, playerMessage, "Understood", 0L
        ).orElseThrow();
    }

    private static SemanticMemoryEntry belief(
            UUID id,
            UUID npc,
            MemoryEvent.Provenance provenance,
            String statement,
            List<UUID> scope,
            long gameTime,
            List<UUID> sourceEventIds
    ) {
        return new SemanticMemoryEntry(
                id, npc, SemanticMemoryEntry.Kind.BELIEF, statement, scope,
                provenance, gameTime, 0L, 50, 50, sourceEventIds
        );
    }

    private static UUID id(int value) {
        return UUID.fromString("00000000-0000-0000-0000-" + String.format("%012d", value));
    }
}
