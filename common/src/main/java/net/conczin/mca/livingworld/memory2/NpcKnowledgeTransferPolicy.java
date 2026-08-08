package net.conczin.mca.livingworld.memory2;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Pure fail-closed authority checks for NPC-to-NPC knowledge transfer. */
final class NpcKnowledgeTransferPolicy {
    private static final Comparator<UUID> UUID_ORDER = Comparator.comparing(UUID::toString);

    private NpcKnowledgeTransferPolicy() {
    }

    static boolean validRequest(UUID speakerNpcId, UUID listenerNpcId, SemanticMemoryEntry source) {
        return speakerNpcId != null
                && listenerNpcId != null
                && source != null
                && !speakerNpcId.equals(listenerNpcId)
                && speakerNpcId.equals(source.ownerNpcId())
                && !SemanticMemoryIngestionAdapter.normalizeAndLimitStatement(source.statement()).isBlank();
    }

    static boolean sameSourceSnapshot(SemanticMemoryEntry selected, SemanticMemoryEntry reread) {
        if (selected == null || reread == null) return false;
        return selected.id().equals(reread.id())
                && selected.ownerNpcId().equals(reread.ownerNpcId())
                && selected.kind() == reread.kind()
                && selected.provenance() == reread.provenance()
                && SemanticMemoryIngestionAdapter.normalizeAndLimitStatement(selected.statement())
                .equals(SemanticMemoryIngestionAdapter.normalizeAndLimitStatement(reread.statement()))
                && canonicalIds(selected.relatedEntities()).equals(canonicalIds(reread.relatedEntities()))
                && canonicalIds(selected.sourceEventIds()).equals(canonicalIds(reread.sourceEventIds()));
    }

    static boolean validEvidence(
            MemoryEvent event,
            UUID speakerNpcId,
            UUID listenerNpcId,
            UUID sourceEntryId,
            long authoritativeGameTime,
            String normalizedStatement
    ) {
        if (event == null) return false;
        return NpcToldDialogueAdapter.create(
                        speakerNpcId,
                        listenerNpcId,
                        sourceEntryId,
                        authoritativeGameTime,
                        normalizedStatement
                )
                .map(event::equals)
                .orElse(false);
    }

    static boolean compatibleRetainedBelief(
            SemanticMemoryEntry expectedCandidate,
            SemanticMemoryEntry retained,
            UUID evidenceEventId
    ) {
        return expectedCandidate != null
                && retained != null
                && evidenceEventId != null
                && SemanticMemoryConsolidator.compatible(expectedCandidate, retained)
                && retained.sourceEventIds().contains(evidenceEventId);
    }

    private static List<UUID> canonicalIds(List<UUID> values) {
        if (values == null || values.isEmpty()) return List.of();
        Set<UUID> unique = new LinkedHashSet<>();
        for (UUID value : values) {
            if (value != null) unique.add(value);
        }
        List<UUID> sorted = new ArrayList<>(unique);
        sorted.sort(UUID_ORDER);
        return List.copyOf(sorted);
    }
}
