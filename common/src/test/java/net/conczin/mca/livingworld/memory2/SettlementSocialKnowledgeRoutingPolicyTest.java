package net.conczin.mca.livingworld.memory2;

import net.conczin.mca.livingworld.relationship.NpcSocialState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettlementSocialKnowledgeRoutingPolicyTest {
    @Test
    void positiveDirectedRouteIsPreferredOverEarlierNeutralCandidate() {
        UUID neutral = id(1);
        UUID respected = id(2);
        UUID affiliative = id(3);

        Optional<UUID> selected = SettlementSocialKnowledgeRoutingPolicy.select(
                List.of(neutral, respected, affiliative),
                Map.of(
                        neutral, NpcSocialState.NEUTRAL,
                        respected, new NpcSocialState(0, 80, 0, 0),
                        affiliative, new NpcSocialState(70, 0, 0, 70)
                )
        );

        assertEquals(Optional.of(respected), selected);
    }

    @Test
    void adverseRoutesAreExcludedAndNeutralOrderRemainsDeterministic() {
        UUID fearful = id(10);
        UUID distrustful = id(11);
        UUID neutralA = id(12);
        UUID neutralB = id(13);

        Optional<UUID> selected = SettlementSocialKnowledgeRoutingPolicy.select(
                List.of(fearful, distrustful, neutralA, neutralB),
                Map.of(
                        fearful, new NpcSocialState(0, 0, 80, 0),
                        distrustful, new NpcSocialState(-80, 0, 0, 0),
                        neutralA, NpcSocialState.NEUTRAL,
                        neutralB, NpcSocialState.NEUTRAL
                )
        );

        assertEquals(Optional.of(neutralA), selected);
    }

    @Test
    void routingWindowIsStrictlyBoundedToFourCandidates() {
        UUID adverseA = id(20);
        UUID adverseB = id(21);
        UUID adverseC = id(22);
        UUID adverseD = id(23);
        UUID positiveOutsideWindow = id(24);

        Optional<UUID> selected = SettlementSocialKnowledgeRoutingPolicy.select(
                List.of(adverseA, adverseB, adverseC, adverseD, positiveOutsideWindow),
                Map.of(
                        adverseA, new NpcSocialState(-80, 0, 0, 0),
                        adverseB, new NpcSocialState(0, 0, 80, 0),
                        adverseC, new NpcSocialState(0, 0, 0, -80),
                        adverseD, new NpcSocialState(-90, 0, 0, 0),
                        positiveOutsideWindow, new NpcSocialState(80, 0, 0, 80)
                )
        );

        assertTrue(selected.isEmpty());
    }

    @Test
    void noAllowedRouteProducesNoFallback() {
        UUID fearful = id(30);
        UUID antipathic = id(31);

        Optional<UUID> selected = SettlementSocialKnowledgeRoutingPolicy.select(
                List.of(fearful, antipathic),
                Map.of(
                        fearful, new NpcSocialState(0, 0, 90, 0),
                        antipathic, new NpcSocialState(0, 0, 0, -90)
                )
        );

        assertTrue(selected.isEmpty());
    }

    private static UUID id(int value) {
        return UUID.fromString("00000000-0000-0000-0000-" + String.format("%012d", value));
    }
}
