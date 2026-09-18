package net.conczin.mca.livingworld.memory2;

import net.conczin.mca.livingworld.context.NpcPairDisposition;
import net.conczin.mca.livingworld.context.PersonalitySocialInfluencePolicy;
import net.conczin.mca.livingworld.relationship.NpcSocialState;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Pure bounded selection policy over an already-settlement-scoped listener window. */
final class SettlementSocialKnowledgeRoutingPolicy {
    static final int MAX_ROUTE_CANDIDATES = 4;

    private SettlementSocialKnowledgeRoutingPolicy() {
    }

    static List<UUID> candidateWindow(
            List<UUID> residentWindow,
            UUID speakerNpcId,
            UUID legacyListenerNpcId
    ) {
        if (residentWindow == null || speakerNpcId == null || legacyListenerNpcId == null) {
            return List.of();
        }

        Set<UUID> ordered = new LinkedHashSet<>();
        if (!speakerNpcId.equals(legacyListenerNpcId) && residentWindow.contains(legacyListenerNpcId)) {
            ordered.add(legacyListenerNpcId);
        }
        for (UUID residentId : residentWindow) {
            if (ordered.size() >= MAX_ROUTE_CANDIDATES) break;
            if (residentId == null || speakerNpcId.equals(residentId)) continue;
            ordered.add(residentId);
        }
        return List.copyOf(new ArrayList<>(ordered));
    }

    static Optional<UUID> select(
            List<UUID> orderedCandidates,
            Map<UUID, NpcSocialState> directSocialStates
    ) {
        if (orderedCandidates == null
                || orderedCandidates.isEmpty()
                || directSocialStates == null
                || directSocialStates.isEmpty()) {
            return Optional.empty();
        }

        UUID firstNeutral = null;
        int considered = 0;
        for (UUID candidate : orderedCandidates) {
            if (considered++ >= MAX_ROUTE_CANDIDATES) break;
            if (candidate == null || !directSocialStates.containsKey(candidate)) continue;

            NpcPairDisposition disposition = PersonalitySocialInfluencePolicy.pairDisposition(
                    directSocialStates.get(candidate)
            );
            switch (disposition) {
                case AFFILIATIVE, RESPECTFUL -> {
                    return Optional.of(candidate);
                }
                case NEUTRAL -> {
                    if (firstNeutral == null) firstNeutral = candidate;
                }
                case FEARFUL, DISTRUSTFUL, ANTIPATHETIC -> {
                    // Explicitly ineligible for this routing attempt.
                }
            }
        }
        return Optional.ofNullable(firstNeutral);
    }
}
