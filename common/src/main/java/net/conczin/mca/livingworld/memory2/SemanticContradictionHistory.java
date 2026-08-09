package net.conczin.mca.livingworld.memory2;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Resolves retained contradiction evidence only while both referenced logical claims remain live and eligible. */
public final class SemanticContradictionHistory {
    private static final Comparator<MemoryEvent> EVENT_ORDER = Comparator
            .comparingLong(MemoryEvent::gameTime).reversed()
            .thenComparing(event -> event.id().toString());

    private SemanticContradictionHistory() {
    }

    public static List<ResolvedSemanticContradiction> load(
            Path worldRoot,
            UUID npcId,
            UUID playerId,
            int maxResults
    ) {
        if (worldRoot == null || npcId == null || maxResults <= 0) return List.of();

        MemoryEventStore eventStore = MemoryEventStore.forWorld(worldRoot);
        SemanticMemoryStore semanticStore = SemanticMemoryStore.forWorld(worldRoot);
        List<MemoryEvent> evidence = new ArrayList<>(eventStore.getRecentMatching(
                npcId,
                Integer.MAX_VALUE,
                event -> event.type() == MemoryEvent.Type.SEMANTIC_CONTRADICTION
                        && SemanticContradictionPolicy.valid(event)
        ));
        evidence.sort(EVENT_ORDER);

        List<ResolvedSemanticContradiction> resolved = new ArrayList<>();
        for (MemoryEvent event : evidence) {
            SemanticContradiction contradiction = event.semanticContradiction();
            Optional<SemanticMemoryEntry> first = resolve(
                    semanticStore,
                    npcId,
                    contradiction.first()
            );
            Optional<SemanticMemoryEntry> second = resolve(
                    semanticStore,
                    npcId,
                    contradiction.second()
            );
            if (first.isEmpty() || second.isEmpty()) continue;
            if (!PlayerScopedMemoryEligibility.semantic(first.get(), npcId, playerId)
                    || !PlayerScopedMemoryEligibility.semantic(second.get(), npcId, playerId)) {
                continue;
            }
            resolved.add(new ResolvedSemanticContradiction(event, first.get(), second.get()));
            if (resolved.size() >= maxResults) break;
        }
        return List.copyOf(resolved);
    }

    private static Optional<SemanticMemoryEntry> resolve(
            SemanticMemoryStore semanticStore,
            UUID npcId,
            SemanticContradiction.ClaimSnapshot snapshot
    ) {
        return semanticStore.findMatching(npcId, entry -> {
            if (!SemanticMemoryIdentity.logicalClaimId(entry).equals(snapshot.logicalClaimId())) return false;
            if (entry.kind() != snapshot.kind() || entry.provenance() != snapshot.provenance()) return false;
            return SemanticMemoryIdentity.canonicalIds(entry.relatedEntities())
                    .equals(snapshot.relatedEntities());
        });
    }

    public record ResolvedSemanticContradiction(
            MemoryEvent evidence,
            SemanticMemoryEntry first,
            SemanticMemoryEntry second
    ) {
        public ResolvedSemanticContradiction {
            if (evidence == null || first == null || second == null) {
                throw new IllegalArgumentException("resolved contradiction requires evidence and both claims");
            }
        }
    }
}
