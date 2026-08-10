package net.conczin.mca.livingworld.memory2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Selects a strictly bounded deterministic set of settlement knowledge-transfer opportunities. */
final class SettlementKnowledgeFlowSelector {
    static final long CYCLE_TICKS = 1_200L;
    static final int MAX_RESIDENTS_PER_CYCLE = 16;
    static final int MAX_SPEAKERS_PER_CYCLE = 4;
    static final int MAX_SOURCE_CANDIDATES_PER_SPEAKER = 2;
    static final int MAX_OPPORTUNITIES_PER_CYCLE = 4;
    static final int MAX_FANOUT_PER_SOURCE_PER_CYCLE = 1;

    private static final Comparator<UUID> UUID_ORDER = Comparator.comparing(UUID::toString);

    private SettlementKnowledgeFlowSelector() {
    }

    static SelectionResult select(
            SemanticMemoryStore store,
            int villageId,
            long gameTime,
            Collection<UUID> residentIds
    ) {
        if (store == null || residentIds == null || residentIds.isEmpty()) {
            return SelectionResult.empty();
        }

        List<UUID> residentWindow = boundedResidentWindow(villageId, gameTime, residentIds);
        if (residentWindow.size() < 2) {
            return new SelectionResult(residentWindow, residentWindow.size(), List.of());
        }

        long cycleIndex = Math.floorDiv(gameTime, CYCLE_TICKS);
        long cycleStart = cycleIndex * CYCLE_TICKS;
        int speakersConsidered = Math.min(MAX_SPEAKERS_PER_CYCLE, residentWindow.size());
        List<Opportunity> opportunities = new ArrayList<>(MAX_OPPORTUNITIES_PER_CYCLE);
        Set<KnowledgeKey> allocatedKnowledge = new LinkedHashSet<>();

        for (int speakerIndex = 0;
             speakerIndex < speakersConsidered && opportunities.size() < MAX_OPPORTUNITIES_PER_CYCLE;
             speakerIndex++) {
            UUID speakerNpcId = residentWindow.get(speakerIndex);
            List<SemanticMemoryEntry> sourceCandidates = store.getRecentMatching(
                    speakerNpcId,
                    MAX_SOURCE_CANDIDATES_PER_SPEAKER,
                    entry -> transferableCandidate(entry, cycleStart)
            );
            if (sourceCandidates.isEmpty()) continue;

            SemanticMemoryEntry source = sourceCandidates.get(indexFor(
                    sourceCandidates.size(),
                    villageId,
                    cycleIndex,
                    speakerNpcId
            ));
            KnowledgeKey knowledgeKey = KnowledgeKey.of(source);
            if (!allocatedKnowledge.add(knowledgeKey)) continue;

            UUID listenerNpcId = deterministicListener(
                    residentWindow,
                    speakerNpcId,
                    villageId,
                    cycleIndex,
                    SemanticMemoryIdentity.logicalClaimId(source)
            );
            if (listenerNpcId == null || listenerAlreadyKnows(store, listenerNpcId, source)) continue;

            opportunities.add(new Opportunity(speakerNpcId, listenerNpcId, source.id()));
        }

        return new SelectionResult(residentWindow, speakersConsidered, opportunities);
    }

    private static List<UUID> boundedResidentWindow(
            int villageId,
            long gameTime,
            Collection<UUID> residentIds
    ) {
        Set<UUID> unique = new LinkedHashSet<>();
        for (UUID residentId : residentIds) {
            if (residentId != null) unique.add(residentId);
        }
        if (unique.isEmpty()) return List.of();

        List<UUID> sorted = new ArrayList<>(unique);
        sorted.sort(UUID_ORDER);
        long cycleIndex = Math.floorDiv(gameTime, CYCLE_TICKS);
        int start = indexFor(sorted.size(), villageId, cycleIndex, null);
        int limit = Math.min(MAX_RESIDENTS_PER_CYCLE, sorted.size());
        List<UUID> window = new ArrayList<>(limit);
        for (int offset = 0; offset < limit; offset++) {
            window.add(sorted.get((start + offset) % sorted.size()));
        }
        return List.copyOf(window);
    }

    private static boolean transferableCandidate(SemanticMemoryEntry entry, long cycleStart) {
        return entry != null
                && entry.kind() != null
                && entry.provenance() != null
                && entry.gameTime() < cycleStart
                && !SemanticMemoryIdentity.canonicalStatement(entry.statement()).isBlank();
    }

    private static UUID deterministicListener(
            List<UUID> residentWindow,
            UUID speakerNpcId,
            int villageId,
            long cycleIndex,
            UUID sourceLogicalId
    ) {
        List<UUID> listeners = residentWindow.stream()
                .filter(id -> !id.equals(speakerNpcId))
                .toList();
        if (listeners.isEmpty()) return null;
        return listeners.get(indexFor(listeners.size(), villageId, cycleIndex, sourceLogicalId));
    }

    private static boolean listenerAlreadyKnows(
            SemanticMemoryStore store,
            UUID listenerNpcId,
            SemanticMemoryEntry source
    ) {
        KnowledgeKey sourceKey = KnowledgeKey.of(source);
        return store.findMatching(
                listenerNpcId,
                entry -> sourceKey.equals(KnowledgeKey.of(entry))
        ).isPresent();
    }

    private static int indexFor(int size, int villageId, long cycleIndex, UUID identity) {
        if (size <= 1) return 0;
        long seed = 31L * villageId + cycleIndex;
        if (identity != null) {
            seed = 31L * seed + identity.getMostSignificantBits();
            seed = 31L * seed + identity.getLeastSignificantBits();
        }
        return (int) Math.floorMod(seed, (long) size);
    }

    record Opportunity(UUID speakerNpcId, UUID listenerNpcId, UUID sourceSemanticEntryId) {
        Opportunity {
            if (speakerNpcId == null || listenerNpcId == null || sourceSemanticEntryId == null) {
                throw new IllegalArgumentException("opportunity ids are required");
            }
            if (speakerNpcId.equals(listenerNpcId)) {
                throw new IllegalArgumentException("speaker and listener must differ");
            }
        }
    }

    private record KnowledgeKey(String statement, List<UUID> scope) {
        private static KnowledgeKey of(SemanticMemoryEntry entry) {
            if (entry == null) throw new IllegalArgumentException("entry is required");
            return new KnowledgeKey(
                    SemanticMemoryIdentity.canonicalStatement(entry.statement()),
                    SemanticMemoryIdentity.canonicalIds(entry.relatedEntities())
            );
        }
    }

    record SelectionResult(
            List<UUID> residentWindow,
            int speakersConsidered,
            List<Opportunity> opportunities
    ) {
        SelectionResult {
            residentWindow = residentWindow == null ? List.of() : List.copyOf(residentWindow);
            speakersConsidered = Math.max(0, Math.min(MAX_SPEAKERS_PER_CYCLE, speakersConsidered));
            opportunities = opportunities == null ? List.of() : List.copyOf(opportunities);
            if (opportunities.size() > MAX_OPPORTUNITIES_PER_CYCLE) {
                throw new IllegalArgumentException("too many opportunities");
            }
        }

        static SelectionResult empty() {
            return new SelectionResult(List.of(), 0, List.of());
        }
    }
}
