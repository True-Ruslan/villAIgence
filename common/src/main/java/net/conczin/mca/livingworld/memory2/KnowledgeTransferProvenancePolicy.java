package net.conczin.mca.livingworld.memory2;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Pure fail-closed validation and deterministic v2 identity for NPC transfer provenance. */
final class KnowledgeTransferProvenancePolicy {
    static final int MAX_HOPS = 8;
    private static final String ID_NAMESPACE = "npc-knowledge-transfer-v2";
    private static final Comparator<UUID> UUID_ORDER = Comparator.comparing(UUID::toString);

    private KnowledgeTransferProvenancePolicy() {
    }

    static UUID deterministicEvidenceId(
            UUID speakerNpcId,
            UUID listenerNpcId,
            UUID speakerSemanticEntryId,
            long authoritativeGameTime
    ) {
        if (speakerNpcId == null || listenerNpcId == null || speakerSemanticEntryId == null) {
            return null;
        }
        long safeGameTime = Math.max(0L, authoritativeGameTime);
        String canonical = ID_NAMESPACE
                + '\n' + listenerNpcId
                + '\n' + speakerNpcId
                + '\n' + speakerSemanticEntryId
                + '\n' + safeGameTime;
        return UUID.nameUUIDFromBytes(canonical.getBytes(StandardCharsets.UTF_8));
    }

    static boolean valid(KnowledgeTransferProvenance provenance) {
        if (provenance == null || !validOrigin(provenance.origin())) return false;
        List<KnowledgeTransferProvenance.Hop> hops = provenance.hops();
        if (hops == null || hops.isEmpty() || hops.size() > MAX_HOPS) return false;

        KnowledgeTransferProvenance.Origin origin = provenance.origin();
        Set<UUID> path = new HashSet<>();
        UUID previousListener = null;
        for (int index = 0; index < hops.size(); index++) {
            KnowledgeTransferProvenance.Hop hop = hops.get(index);
            if (!validHop(hop)) return false;
            if (index == 0) {
                if (!origin.originNpcId().equals(hop.speakerNpcId())
                        || !origin.originSemanticEntryId().equals(hop.speakerSemanticEntryId())) {
                    return false;
                }
                if (!path.add(hop.speakerNpcId())) return false;
            } else if (!previousListener.equals(hop.speakerNpcId())) {
                return false;
            }
            if (!path.add(hop.listenerNpcId())) return false;
            previousListener = hop.listenerNpcId();
        }
        return true;
    }

    static boolean originMatchesSource(
            KnowledgeTransferProvenance provenance,
            SemanticMemoryEntry source
    ) {
        if (!valid(provenance) || source == null) return false;
        KnowledgeTransferProvenance.Origin origin = provenance.origin();
        return origin.originNpcId().equals(source.ownerNpcId())
                && origin.originSemanticEntryId().equals(source.id())
                && origin.originKind() == source.kind()
                && origin.originProvenance() == source.provenance()
                && contentMatchesOrigin(provenance, source);
    }

    static boolean directEvidenceMatches(
            KnowledgeTransferProvenance provenance,
            MemoryEvent evidence,
            SemanticMemoryEntry currentSpeakerSource
    ) {
        if (!valid(provenance) || evidence == null || currentSpeakerSource == null) return false;
        if (currentSpeakerSource.kind() != SemanticMemoryEntry.Kind.BELIEF
                || currentSpeakerSource.provenance() != MemoryEvent.Provenance.NPC_TOLD
                || !contentMatchesOrigin(provenance, currentSpeakerSource)) {
            return false;
        }
        KnowledgeTransferProvenance.Hop last = provenance.hops().getLast();
        return currentSpeakerSource.ownerNpcId().equals(evidence.ownerNpcId())
                && currentSpeakerSource.sourceEventIds().contains(evidence.id())
                && evidence.type() == MemoryEvent.Type.DIALOGUE
                && evidence.provenance() == MemoryEvent.Provenance.NPC_TOLD
                && evidence.id().equals(last.evidenceEventId())
                && currentSpeakerSource.ownerNpcId().equals(last.listenerNpcId());
    }

    static boolean contentMatchesOrigin(
            KnowledgeTransferProvenance provenance,
            SemanticMemoryEntry source
    ) {
        if (provenance == null || provenance.origin() == null || source == null) return false;
        String normalized = SemanticMemoryIngestionAdapter.normalizeAndLimitStatement(source.statement());
        return !normalized.isBlank()
                && normalized.equals(provenance.origin().statement())
                && canonicalIds(source.relatedEntities()).equals(provenance.origin().relatedEntities());
    }

    static boolean wouldCycle(KnowledgeTransferProvenance provenance, UUID proposedListenerNpcId) {
        if (!valid(provenance) || proposedListenerNpcId == null) return false;
        for (KnowledgeTransferProvenance.Hop hop : provenance.hops()) {
            if (proposedListenerNpcId.equals(hop.speakerNpcId())
                    || proposedListenerNpcId.equals(hop.listenerNpcId())) {
                return true;
            }
        }
        return false;
    }

    static boolean atHopLimit(KnowledgeTransferProvenance provenance) {
        return valid(provenance) && provenance.hops().size() >= MAX_HOPS;
    }

    static List<UUID> canonicalIds(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        Set<UUID> unique = new LinkedHashSet<>();
        for (UUID id : ids) {
            if (id != null) unique.add(id);
        }
        List<UUID> sorted = new ArrayList<>(unique);
        sorted.sort(UUID_ORDER);
        return List.copyOf(sorted);
    }

    private static boolean validOrigin(KnowledgeTransferProvenance.Origin origin) {
        if (origin == null
                || origin.originNpcId() == null
                || origin.originSemanticEntryId() == null
                || origin.originKind() == null
                || origin.originProvenance() == null
                || origin.statement() == null
                || origin.relatedEntities() == null) {
            return false;
        }
        String normalized = SemanticMemoryIngestionAdapter.normalizeAndLimitStatement(origin.statement());
        if (normalized.isBlank() || !normalized.equals(origin.statement())) return false;
        if (!canonicalIds(origin.relatedEntities()).equals(origin.relatedEntities())) return false;
        return (origin.originKind() == SemanticMemoryEntry.Kind.FACT
                && origin.originProvenance() == MemoryEvent.Provenance.SYSTEM_OBSERVED)
                || (origin.originKind() == SemanticMemoryEntry.Kind.BELIEF
                && (origin.originProvenance() == MemoryEvent.Provenance.PLAYER_TOLD
                || origin.originProvenance() == MemoryEvent.Provenance.INFERRED));
    }

    private static boolean validHop(KnowledgeTransferProvenance.Hop hop) {
        if (hop == null
                || hop.speakerNpcId() == null
                || hop.listenerNpcId() == null
                || hop.speakerSemanticEntryId() == null
                || hop.evidenceEventId() == null
                || hop.gameTime() < 0L
                || hop.speakerNpcId().equals(hop.listenerNpcId())) {
            return false;
        }
        UUID expected = deterministicEvidenceId(
                hop.speakerNpcId(),
                hop.listenerNpcId(),
                hop.speakerSemanticEntryId(),
                hop.gameTime()
        );
        return hop.evidenceEventId().equals(expected);
    }
}
