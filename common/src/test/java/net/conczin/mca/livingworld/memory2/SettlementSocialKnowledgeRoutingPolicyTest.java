package net.conczin.mca.livingworld.memory2;

import net.conczin.mca.livingworld.relationship.NpcSocialState;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettlementSocialKnowledgeRoutingPolicyTest {
    @Test
    void candidateWindowIsHardBoundedToFourAndKeepsLegacyTargetFirst() {
        UUID speaker = id(1);
        UUID legacyTarget = id(7);
        List<UUID> residents = List.of(
                speaker, id(2), id(3), id(4), id(5), id(6), legacyTarget, id(8)
        );

        List<UUID> window = SettlementSocialKnowledgeRoutingPolicy.candidateWindow(
                residents,
                speaker,
                legacyTarget
        );

        assertEquals(SettlementSocialKnowledgeRoutingPolicy.MAX_ROUTE_CANDIDATES, window.size());
        assertEquals(legacyTarget, window.getFirst());
        assertFalse(window.contains(speaker));
        assertEquals(window.size(), window.stream().distinct().count());
    }

    @Test
    void positiveStateOutsideBoundedWindowIsInvisible() {
        UUID speaker = id(10);
        UUID legacyTarget = id(11);
        UUID outside = id(19);
        List<UUID> residents = List.of(
                speaker, legacyTarget, id(12), id(13), id(14), id(15), id(16), id(17), outside
        );
        List<UUID> window = SettlementSocialKnowledgeRoutingPolicy.candidateWindow(
                residents,
                speaker,
                legacyTarget
        );

        Map<UUID, NpcSocialState> states = new LinkedHashMap<>();
        for (UUID candidate : window) {
            states.put(candidate, NpcSocialState.NEUTRAL);
        }
        states.put(outside, new NpcSocialState(0, 100, 0, 0));

        UUID selected = SettlementSocialKnowledgeRoutingPolicy.select(window, states).orElseThrow();

        assertEquals(legacyTarget, selected);
        assertFalse(window.contains(outside));
        assertFalse(selected.equals(outside));
    }

    @Test
    void positiveRouteBeatsNeutralWhileAdverseRoutesAreExcludedInStableOrder() {
        UUID adverse = id(21);
        UUID neutral = id(22);
        UUID respected = id(23);
        UUID affiliative = id(24);
        List<UUID> candidates = List.of(adverse, neutral, respected, affiliative);

        Map<UUID, NpcSocialState> states = new LinkedHashMap<>();
        states.put(adverse, new NpcSocialState(-80, 0, 0, 0));
        states.put(neutral, NpcSocialState.NEUTRAL);
        states.put(respected, new NpcSocialState(0, 80, 0, 0));
        states.put(affiliative, new NpcSocialState(80, 0, 0, 80));

        assertEquals(
                respected,
                SettlementSocialKnowledgeRoutingPolicy.select(candidates, states).orElseThrow()
        );

        states.put(respected, new NpcSocialState(0, 0, 80, 0));
        states.put(affiliative, new NpcSocialState(0, 0, 80, 0));

        assertEquals(
                neutral,
                SettlementSocialKnowledgeRoutingPolicy.select(candidates, states).orElseThrow()
        );
    }

    @Test
    void allAdverseCandidatesProduceNoRoute() {
        List<UUID> candidates = List.of(id(31), id(32), id(33));
        Map<UUID, NpcSocialState> states = Map.of(
                candidates.get(0), new NpcSocialState(-80, 0, 0, 0),
                candidates.get(1), new NpcSocialState(0, 0, 80, 0),
                candidates.get(2), new NpcSocialState(0, 0, 0, -80)
        );

        assertTrue(SettlementSocialKnowledgeRoutingPolicy.select(candidates, states).isEmpty());
    }

    private static UUID id(int value) {
        return UUID.fromString("00000000-0000-0000-0000-" + String.format("%012d", value));
    }
}
