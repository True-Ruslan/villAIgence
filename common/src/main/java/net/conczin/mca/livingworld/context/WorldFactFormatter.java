package net.conczin.mca.livingworld.context;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Pure formatting helpers for bounded facts supplied to the LLM. */
public final class WorldFactFormatter {
    private WorldFactFormatter() {
    }

    public static List<String> summarizeItems(List<String> itemIds, int maxEntries) {
        if (itemIds == null || itemIds.isEmpty() || maxEntries <= 0) return List.of();

        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String itemId : itemIds) {
            if (itemId == null || itemId.isBlank()) continue;
            counts.merge(itemId, 1, Integer::sum);
        }

        List<String> output = new ArrayList<>(Math.min(counts.size(), maxEntries));
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (output.size() >= maxEntries) break;
            output.add(entry.getKey() + " x" + entry.getValue());
        }
        return List.copyOf(output);
    }
}
