package net.conczin.mca.livingworld.memory2;

import net.conczin.mca.livingworld.relationship.LivingWorldRelationshipDelta;
import net.conczin.mca.livingworld.relationship.LivingWorldRelationshipStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SocialEpistemicResolverTest {
    @TempDir
    Path tempDir;

    @Test
    void derivesFromExactListenerNpcAndResolvedSourcePlayerRelationship() {
        Path world = tempDir.resolve("pair");
        UUID npc = id(1);
        UUID sourcePlayer = id(2);
        UUID otherPlayer = id(3);
        SemanticMemoryEntry belief = storePlayerBelief(world, npc, sourcePlayer, "The well is clean", 50);
        LivingWorldRelationshipStore relationships = LivingWorldRelationshipStore.forWorld(world);
        relationships.applyDelta(npc, sourcePlayer, new LivingWorldRelationshipDelta(70, -100, 100, -100), 100);
        relationships.applyDelta(npc, otherPlayer, new LivingWorldRelationshipDelta(-100, 100, -100, 100), 100);

        SocialEpistemicState state = SocialEpistemicResolver.resolve(
                SemanticMemoryStore.forWorld(world),
                MemoryEventStore.forWorld(world),
                relationships,
                belief
        ).orElseThrow();

        assertEquals(sourcePlayer, state.sourcePlayerId());
        assertEquals(70, state.trust());
        assertEquals(7, state.trustDelta());
        assertEquals(57, state.effectiveBeliefConfidence());
        assertEquals(50, belief.confidence());
    }

    @Test
    void relationshipChangeUpdatesDerivedStateWithoutRewritingSemanticMemory() {
        Path world = tempDir.resolve("dynamic");
        UUID npc = id(10);
        UUID player = id(11);
        SemanticMemoryEntry belief = storePlayerBelief(world, npc, player, "The miller is honest", 60);
        LivingWorldRelationshipStore relationships = LivingWorldRelationshipStore.forWorld(world);

        SocialEpistemicState neutral = SocialEpistemicResolver.resolve(
                SemanticMemoryStore.forWorld(world), MemoryEventStore.forWorld(world), relationships, belief
        ).orElseThrow();
        relationships.applyDelta(npc, player, new LivingWorldRelationshipDelta(-80, 100, -100, 100), 100);
        SocialEpistemicState distrusted = SocialEpistemicResolver.resolve(
                SemanticMemoryStore.forWorld(world), MemoryEventStore.forWorld(world), relationships, belief
        ).orElseThrow();

        assertEquals(60, neutral.effectiveBeliefConfidence());
        assertEquals(52, distrusted.effectiveBeliefConfidence());
        assertEquals(60, belief.confidence());
        assertEquals(60, SemanticMemoryStore.forWorld(world).findById(npc, belief.id()).orElseThrow().confidence());
    }

    @Test
    void unresolvedEvidenceProducesNoTrustStateEvenWhenRelationshipExists() {
        Path world = tempDir.resolve("unresolved");
        UUID npc = id(20);
        UUID player = id(21);
        SemanticMemoryEntry unsupported = new SemanticMemoryEntry(
                id(22), npc, SemanticMemoryEntry.Kind.BELIEF, "Unsupported claim", List.of(player),
                MemoryEvent.Provenance.PLAYER_TOLD, 1_000L, 0L, 50, 50, List.of(id(999))
        );
        LivingWorldRelationshipStore relationships = LivingWorldRelationshipStore.forWorld(world);
        relationships.applyDelta(npc, player, new LivingWorldRelationshipDelta(100, 0, 0, 0), 100);

        assertTrue(SocialEpistemicResolver.resolve(
                SemanticMemoryStore.forWorld(world), MemoryEventStore.forWorld(world), relationships, unsupported
        ).isEmpty());
    }

    private static SemanticMemoryEntry storePlayerBelief(
            Path world,
            UUID npc,
            UUID player,
            String statement,
            int confidence
    ) {
        MemoryEvent dialogue = DialogueMemoryAdapter.toMemoryEvent(
                npc, player, 1_000L, statement, "Understood", 0L
        ).orElseThrow();
        MemoryEventStore.forWorld(world).append(dialogue, 64);
        SemanticMemoryEntry belief = new SemanticMemoryEntry(
                id(100 + Math.abs(statement.hashCode() % 1000)),
                npc,
                SemanticMemoryEntry.Kind.BELIEF,
                statement,
                List.of(player),
                MemoryEvent.Provenance.PLAYER_TOLD,
                1_000L,
                0L,
                50,
                confidence,
                List.of(dialogue.id())
        );
        SemanticMemoryStore.forWorld(world).append(belief, 64);
        return SemanticMemoryStore.forWorld(world).findMatching(
                npc, entry -> SemanticMemoryIdentity.canonicalStatement(entry.statement())
                        .equals(SemanticMemoryIdentity.canonicalStatement(statement))
        ).orElseThrow();
    }

    private static UUID id(int value) {
        return UUID.fromString("00000000-0000-0000-0000-" + String.format("%012d", value));
    }
}
