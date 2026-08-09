package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SemanticContradictionModelTest {
    @TempDir
    Path tempDir;

    @Test
    void payloadCanonicalizesOrderAndScopeWithoutDuplicatingClaimText() {
        UUID firstLogical = id(10);
        UUID secondLogical = id(20);
        UUID playerA = id(90);
        UUID playerB = id(91);

        SemanticContradiction.ClaimSnapshot larger = new SemanticContradiction.ClaimSnapshot(
                secondLogical,
                id(120),
                SemanticMemoryEntry.Kind.BELIEF,
                MemoryEvent.Provenance.NPC_TOLD,
                List.of(playerB, playerA, playerB)
        );
        SemanticContradiction.ClaimSnapshot smaller = new SemanticContradiction.ClaimSnapshot(
                firstLogical,
                id(110),
                SemanticMemoryEntry.Kind.FACT,
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                List.of(playerA, playerB)
        );

        SemanticContradiction payload = new SemanticContradiction(larger, smaller);

        assertEquals(firstLogical, payload.first().logicalClaimId());
        assertEquals(secondLogical, payload.second().logicalClaimId());
        assertEquals(List.of(playerA, playerB), payload.first().relatedEntities());
        assertEquals(List.of(playerA, playerB), payload.second().relatedEntities());
        assertThrows(UnsupportedOperationException.class,
                () -> payload.first().relatedEntities().add(id(999)));
    }

    @Test
    void rejectsMissingOrDuplicateLogicalClaims() {
        SemanticContradiction.ClaimSnapshot first = snapshot(id(30), id(130));
        SemanticContradiction.ClaimSnapshot sameLogical = snapshot(id(30), id(131));

        assertThrows(IllegalArgumentException.class, () -> new SemanticContradiction(null, first));
        assertThrows(IllegalArgumentException.class, () -> new SemanticContradiction(first, null));
        assertThrows(IllegalArgumentException.class, () -> new SemanticContradiction(first, sameLogical));
    }

    @Test
    void memoryEventKeepsHistoricalConstructorsAndNewPayloadOptional() {
        MemoryEvent historical = new MemoryEvent(
                id(200),
                id(1),
                MemoryEvent.Type.OBSERVATION,
                "Historical observation",
                List.of(id(1)),
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                10L,
                0L,
                80,
                0,
                100,
                List.of()
        );
        assertNull(historical.semanticContradiction());

        SemanticContradiction payload = new SemanticContradiction(
                snapshot(id(40), id(140)),
                snapshot(id(41), id(141))
        );
        MemoryEvent contradiction = contradictionEvent(id(201), id(1), 20L, payload);
        assertEquals(payload, contradiction.semanticContradiction());
        assertEquals(MemoryEvent.Type.SEMANTIC_CONTRADICTION, contradiction.type());
        assertNull(contradiction.dialogue());
        assertNull(contradiction.relationshipTransition());
        assertNull(contradiction.relationshipCause());
        assertNull(contradiction.knowledgeTransferProvenance());
    }

    @Test
    void contradictionHasObservationTierRetentionButNeverBecomesSemanticFact() {
        UUID owner = id(1);
        SemanticContradiction payload = new SemanticContradiction(
                snapshot(id(50), id(150)),
                snapshot(id(51), id(151))
        );
        MemoryEvent contradiction = contradictionEvent(id(210), owner, 100L, payload);
        MemoryEvent observation = new MemoryEvent(
                id(211), owner, MemoryEvent.Type.OBSERVATION, "Observed", List.of(owner),
                MemoryEvent.Provenance.SYSTEM_OBSERVED, 100L, 0L, 60, 0, 100, List.of()
        );

        assertEquals(
                MemoryEventRetentionPolicy.typeContribution(MemoryEvent.Type.OBSERVATION),
                MemoryEventRetentionPolicy.typeContribution(MemoryEvent.Type.SEMANTIC_CONTRADICTION)
        );
        assertEquals(
                MemoryEventRetentionPolicy.durabilityScore(observation),
                MemoryEventRetentionPolicy.durabilityScore(contradiction)
        );
        assertTrue(SemanticMemoryIngestionAdapter.toFact(contradiction).isEmpty());
    }

    @Test
    void contradictionEvidenceIsExcludedFromGenericEpisodicPrompt() {
        Path world = tempDir.resolve("prompt-isolation");
        UUID owner = id(1);
        UUID player = id(90);
        SemanticContradiction payload = new SemanticContradiction(
                new SemanticContradiction.ClaimSnapshot(
                        id(60), id(160), SemanticMemoryEntry.Kind.BELIEF,
                        MemoryEvent.Provenance.NPC_TOLD, List.of(player)),
                new SemanticContradiction.ClaimSnapshot(
                        id(61), id(161), SemanticMemoryEntry.Kind.BELIEF,
                        MemoryEvent.Provenance.PLAYER_TOLD, List.of(player))
        );
        MemoryEvent contradiction = contradictionEvent(id(220), owner, 200L, payload);
        MemoryEventStore.forWorld(world).append(contradiction, 64);

        assertNotNull(MemoryEventStore.forWorld(world).findById(owner, contradiction.id()).orElse(null));
        List<String> context = Memory2ContextProvider.load(world, owner, player, 300L);
        assertTrue(context.stream().noneMatch(line -> line.contains("SEMANTIC_CONTRADICTION")));
        assertTrue(context.stream().noneMatch(line -> line.contains("Semantic contradiction recorded")));
    }

    private static SemanticContradiction.ClaimSnapshot snapshot(UUID logicalId, UUID detectedId) {
        return new SemanticContradiction.ClaimSnapshot(
                logicalId,
                detectedId,
                SemanticMemoryEntry.Kind.BELIEF,
                MemoryEvent.Provenance.NPC_TOLD,
                List.of()
        );
    }

    private static MemoryEvent contradictionEvent(
            UUID eventId,
            UUID owner,
            long gameTime,
            SemanticContradiction payload
    ) {
        return new MemoryEvent(
                eventId,
                owner,
                MemoryEvent.Type.SEMANTIC_CONTRADICTION,
                "Semantic contradiction recorded",
                List.of(owner),
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                gameTime,
                0L,
                60,
                0,
                100,
                List.of(),
                null,
                null,
                null,
                null,
                payload
        );
    }

    private static UUID id(int value) {
        return UUID.fromString("00000000-0000-0000-0000-" + String.format("%012d", value));
    }
}
