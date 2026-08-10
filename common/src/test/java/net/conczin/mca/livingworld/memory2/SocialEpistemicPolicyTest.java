package net.conczin.mca.livingworld.memory2;

import net.conczin.mca.livingworld.relationship.LivingWorldRelationshipState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SocialEpistemicPolicyTest {
    @Test
    void trustAdjustsOnlyDerivedBeliefConfidenceWithinTenPoints() {
        UUID npc = id(1);
        UUID player = id(2);
        SemanticMemoryEntry belief = belief(npc, player, 50);

        SocialEpistemicState distrusted = SocialEpistemicPolicy.derive(
                belief,
                player,
                new LivingWorldRelationshipState(-100, 100, -100, 100)
        ).orElseThrow();
        SocialEpistemicState neutral = SocialEpistemicPolicy.derive(
                belief,
                player,
                new LivingWorldRelationshipState(0, -100, 100, -100)
        ).orElseThrow();
        SocialEpistemicState trusted = SocialEpistemicPolicy.derive(
                belief,
                player,
                new LivingWorldRelationshipState(100, -100, 100, -100)
        ).orElseThrow();

        assertEquals(-10, distrusted.trustDelta());
        assertEquals(40, distrusted.effectiveBeliefConfidence());
        assertEquals(0, neutral.trustDelta());
        assertEquals(50, neutral.effectiveBeliefConfidence());
        assertEquals(10, trusted.trustDelta());
        assertEquals(60, trusted.effectiveBeliefConfidence());

        assertEquals(50, belief.confidence(), "derived trust must not mutate persisted confidence");
    }

    @Test
    void arithmeticUsesTrustOnlyAndIgnoresOtherRelationshipDimensions() {
        UUID npc = id(10);
        UUID player = id(11);
        SemanticMemoryEntry belief = belief(npc, player, 60);

        SocialEpistemicState first = SocialEpistemicPolicy.derive(
                belief,
                player,
                new LivingWorldRelationshipState(55, -100, 100, -100)
        ).orElseThrow();
        SocialEpistemicState second = SocialEpistemicPolicy.derive(
                belief,
                player,
                new LivingWorldRelationshipState(55, 100, -100, 100)
        ).orElseThrow();

        assertEquals(first, second);
        assertEquals(5, first.trustDelta());
        assertEquals(65, first.effectiveBeliefConfidence());
    }

    @Test
    void effectiveConfidenceRemainsClampedToSemanticConfidenceDomain() {
        UUID npc = id(20);
        UUID player = id(21);

        SocialEpistemicState low = SocialEpistemicPolicy.derive(
                belief(npc, player, 3),
                player,
                new LivingWorldRelationshipState(-100, 0, 0, 0)
        ).orElseThrow();
        SocialEpistemicState high = SocialEpistemicPolicy.derive(
                belief(npc, player, 97),
                player,
                new LivingWorldRelationshipState(100, 0, 0, 0)
        ).orElseThrow();

        assertEquals(0, low.effectiveBeliefConfidence());
        assertEquals(100, high.effectiveBeliefConfidence());
    }

    @Test
    void factAndMissingSourcePlayerNeverReceiveSocialEpistemicState() {
        UUID npc = id(30);
        UUID player = id(31);
        LivingWorldRelationshipState trusted = new LivingWorldRelationshipState(100, 100, 100, 100);

        assertTrue(SocialEpistemicPolicy.derive(fact(npc, player), player, trusted).isEmpty());
        assertTrue(SocialEpistemicPolicy.derive(belief(npc, player, 50), null, trusted).isEmpty());
    }

    private static SemanticMemoryEntry belief(UUID npc, UUID player, int confidence) {
        return new SemanticMemoryEntry(
                id(100),
                npc,
                SemanticMemoryEntry.Kind.BELIEF,
                "The old mine is safe",
                List.of(player),
                MemoryEvent.Provenance.PLAYER_TOLD,
                1_000L,
                0L,
                50,
                confidence,
                List.of(id(101))
        );
    }

    private static SemanticMemoryEntry fact(UUID npc, UUID player) {
        return new SemanticMemoryEntry(
                id(200),
                npc,
                SemanticMemoryEntry.Kind.FACT,
                "The old mine is open",
                List.of(player),
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                1_000L,
                0L,
                90,
                100,
                List.of(id(201))
        );
    }

    private static UUID id(int value) {
        return UUID.fromString("00000000-0000-0000-0000-" + String.format("%012d", value));
    }
}
