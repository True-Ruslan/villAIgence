package net.conczin.mca.livingworld.memory2;

import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

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
        int safeMax = Math.max(1, maxEntriesPerNpc);
        SemanticMemoryIngestionAdapter.toFact(source)
                .flatMap(entry -> persistAndResolve(worldRoot, entry, safeMax))
                .ifPresent(retained -> BoundedSemanticContradictionProducer.produce(
                        worldRoot,
                        retained,
                        safeMax
                ));
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
        int safeMax = Math.max(1, maxEntriesPerNpc);
        persistAndResolve(
                worldRoot,
                SemanticMemoryIngestionAdapter.toBelief(source),
                safeMax
        ).ifPresent(retained -> BoundedSemanticContradictionProducer.produce(
                worldRoot,
                retained,
                safeMax
        ));
    }

    static Optional<SemanticMemoryEntry> persistAndResolve(
            Path worldRoot,
            SemanticMemoryEntry entry,
            int maxEntriesPerNpc
    ) {
        if (worldRoot == null || entry == null) return Optional.empty();
        SemanticMemoryStore store = SemanticMemoryStore.forWorld(worldRoot);
        UUID logicalClaimId = SemanticMemoryIdentity.logicalClaimId(entry);
        store.append(entry, Math.max(1, maxEntriesPerNpc));
        return store.findMatching(
                entry.ownerNpcId(),
                retained -> logicalClaimId.equals(SemanticMemoryIdentity.logicalClaimId(retained))
        );
    }
}
