package net.conczin.mca.livingworld.memory2;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/** Pure deterministic bounded selector that reserves part of the candidate window for durable older memory. */
final class LongHorizonCandidateSelector {
    private LongHorizonCandidateSelector() {
    }

    static <T> List<T> select(
            List<T> eligible,
            int candidateLimit,
            Comparator<T> newestFirst,
            Comparator<T> durableFirst,
            Function<T, UUID> idExtractor
    ) {
        if (eligible == null
                || eligible.isEmpty()
                || candidateLimit <= 0
                || newestFirst == null
                || durableFirst == null
                || idExtractor == null) {
            return List.of();
        }

        List<T> recentOrder = new ArrayList<>(eligible);
        recentOrder.sort(newestFirst);

        if (candidateLimit == 1) {
            return List.of(recentOrder.getFirst());
        }

        int durableQuota = Math.max(1, candidateLimit / 4);
        int recentQuota = candidateLimit - durableQuota;

        List<T> selected = new ArrayList<>(Math.min(candidateLimit, eligible.size()));
        Set<UUID> selectedIds = new HashSet<>();
        for (T candidate : recentOrder) {
            if (selected.size() >= recentQuota) break;
            UUID id = idExtractor.apply(candidate);
            if (id != null && selectedIds.add(id)) selected.add(candidate);
        }

        List<T> durableOrder = new ArrayList<>(eligible);
        durableOrder.sort(durableFirst);
        int durableAdded = 0;
        for (T candidate : durableOrder) {
            if (selected.size() >= candidateLimit || durableAdded >= durableQuota) break;
            UUID id = idExtractor.apply(candidate);
            if (id != null && selectedIds.add(id)) {
                selected.add(candidate);
                durableAdded++;
            }
        }

        return List.copyOf(selected);
    }
}
