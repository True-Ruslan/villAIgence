package net.conczin.mca.livingworld.memory2;

import java.nio.file.Path;

/** Persists only explicitly controlled semantic FACT/BELIEF inputs. */
public final class ControlledSemanticMemoryIngestor {
    private ControlledSemanticMemoryIngestor() {
    }

    public static void recordFactIfEnabled(
            boolean enabled,
            Path worldRoot,
            MemoryEvent source,
            int maxEntriesPerNpc
    ) {
        if (!enabled) return;
        recordFact(worldRoot, source, maxEntriesPerNpc);
    }

    public static void recordFact(Path worldRoot, MemoryEvent source, int maxEntriesPerNpc) {
        if (worldRoot == null) return;
        SemanticMemoryIngestionAdapter.toFact(source)
                .ifPresent(entry -> SemanticMemoryStore.forWorld(worldRoot).append(entry, maxEntriesPerNpc));
    }

    public static void recordBeliefIfEnabled(
            boolean enabled,
            Path worldRoot,
            SemanticBeliefSource source,
            int maxEntriesPerNpc
    ) {
        if (!enabled) return;
        recordBelief(worldRoot, source, maxEntriesPerNpc);
    }

    public static void recordBelief(Path worldRoot, SemanticBeliefSource source, int maxEntriesPerNpc) {
        if (worldRoot == null || source == null) return;
        SemanticMemoryStore.forWorld(worldRoot).append(
                SemanticMemoryIngestionAdapter.toBelief(source),
                maxEntriesPerNpc
        );
    }
}
