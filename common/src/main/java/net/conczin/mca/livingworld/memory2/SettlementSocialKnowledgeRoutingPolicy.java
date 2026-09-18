package net.conczin.mca.livingworld.memory2;

import net.conczin.mca.livingworld.context.NpcPairDisposition;
import net.conczin.mca.livingworld.context.PersonalitySocialInfluencePolicy;
import net.conczin.mca.livingworld.relationship.NpcSocialState;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Chooses one route from a strictly bounded ordered listener window using direct server-owned social state. */
final class SettlementSocialKnowledgeRoutingPolicy {
    static final int MAX_CANDIDATES = 4;

    private SettlementSocialKnowledgeRoutingPolicy() {
    }

    static Optional<UUID> select(
            List<UUID> candidateListenerIds,
            Map<UUID, NpcSocialState> directedStates
    ) {
        if (candidateListenerIds == null || candidateListenerIds.isEmpty()) {
            return Optional.empty();
        }

        UUID firstNeutral = null;
        int limit = Math.min(MAX_CANDIDATES, candidateListenerIds.size());
        for (int index = 0; index < limit; index++) {
            UUID candidate = candidateListenerIds.get(index);
            if (candidate == null) continue;

            if (directedStates == null || !directedStates.containsKey(candidate)) {
                continue;
            }
            NpcSocialState state = directedStates.get(candidate);
            if (state == null) {
                continue;
            }
            NpcPairDisposition disposition = PersonalitySocialInfluencePolicy.pairDisposition(state);
            if (!SettlementSocialKnowledgeSharingPolicy.isAllowed(disposition)) {
                continue;
            }
            if (disposition == NpcPairDisposition.AFFILIATIVE
                    || disposition == NpcPairDisposition.RESPECTFUL) {
                return Optional.of(candidate);
            }
            if (firstNeutral == null) {
                firstNeutral = candidate;
            }
        }
        return Optional.ofNullable(firstNeutral);
    }
}
