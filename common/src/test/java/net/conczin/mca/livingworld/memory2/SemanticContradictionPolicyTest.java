package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SemanticContradictionPolicyTest {
    @Test
    void deterministicIdentityIsOrderIndependentAndBindsCanonicalSnapshots() {
        UUID owner = id(1);
        SemanticContradiction forward = new SemanticContradiction(
                snapshot(id(20), id(120), SemanticMemoryEntry.Kind.BELIEF,
                        MemoryEvent.Provenance.NPC_TOLD, List.of(id(91), id(90))),
                snapshot(id(10), id(110), SemanticMemoryEntry.Kind.FACT,
                        MemoryEvent.Provenance.SYSTEM_OBSERVED, List.of(id(90), id(91)))
        );
        SemanticContradiction reverse = new SemanticContradiction(forward.second(), forward.first());
        long gameTime = 123L;
        String canonical = "semantic-contradiction-v1\n"
                + owner + "\n"
                + snapshotCanonical(forward.first()) + "\n"
                + snapshotCanonical(forward.second()) + "\n"
                + gameTime;
        UUID expected = UUID.nameUUIDFromBytes(canonical.getBytes(StandardCharsets.UTF_8));

        assertEquals(expected, SemanticContradictionPolicy.deterministicEventId(owner, forward, gameTime));
        assertEquals(expected, SemanticContradictionPolicy.deterministicEventId(owner, reverse, gameTime));
    }

    @Test
    void canonicalEvidencePassesAndFieldMutationsFailClosed() {
        SemanticMemoryEntry first = new SemanticMemoryEntry(
                id(100), id(1), SemanticMemoryEntry.Kind.FACT, "North gate open", List.of(id(90)),
                MemoryEvent.Provenance.SYSTEM_OBSERVED, 10L, 0L, 100, 100, List.of(id(900)));
        SemanticMemoryEntry second = new SemanticMemoryEntry(
                id(101), id(1), SemanticMemoryEntry.Kind.BELIEF, "North gate closed", List.of(id(90)),
                MemoryEvent.Provenance.NPC_TOLD, 11L, 0L, 50, 50, List.of(id(901)));
        MemoryEvent canonical = SemanticContradictionAdapter.create(first, second, 200L).orElseThrow();
        assertTrue(SemanticContradictionPolicy.valid(canonical));

        assertFalse(SemanticContradictionPolicy.valid(copy(
                canonical, id(2), canonical.type(), canonical.summary(), canonical.provenance(),
                canonical.gameTime(), canonical.participants(), canonical.semanticContradiction())));
        assertFalse(SemanticContradictionPolicy.valid(copy(
                canonical, canonical.ownerNpcId(), MemoryEvent.Type.OBSERVATION, canonical.summary(),
                canonical.provenance(), canonical.gameTime(), canonical.participants(),
                canonical.semanticContradiction())));
        assertFalse(SemanticContradictionPolicy.valid(copy(
                canonical, canonical.ownerNpcId(), canonical.type(), "Different summary",
                canonical.provenance(), canonical.gameTime(), canonical.participants(),
                canonical.semanticContradiction())));
        assertFalse(SemanticContradictionPolicy.valid(copy(
                canonical, canonical.ownerNpcId(), canonical.type(), canonical.summary(),
                MemoryEvent.Provenance.NPC_TOLD, canonical.gameTime(), canonical.participants(),
                canonical.semanticContradiction())));
        assertFalse(SemanticContradictionPolicy.valid(copy(
                canonical, canonical.ownerNpcId(), canonical.type(), canonical.summary(), canonical.provenance(),
                canonical.gameTime() + 1L, canonical.participants(), canonical.semanticContradiction())));
        assertFalse(SemanticContradictionPolicy.valid(copy(
                canonical, canonical.ownerNpcId(), canonical.type(), canonical.summary(), canonical.provenance(),
                canonical.gameTime(), List.of(canonical.ownerNpcId(), id(99)),
                canonical.semanticContradiction())));

        SemanticContradiction payload = canonical.semanticContradiction();
        SemanticContradiction.ClaimSnapshot firstSnapshot = payload.first();
        SemanticContradiction changedScope = new SemanticContradiction(
                new SemanticContradiction.ClaimSnapshot(
                        firstSnapshot.logicalClaimId(), firstSnapshot.detectedSemanticEntryId(),
                        firstSnapshot.kind(), firstSnapshot.provenance(), List.of(id(91))),
                payload.second()
        );
        assertFalse(SemanticContradictionPolicy.valid(copy(
                canonical, canonical.ownerNpcId(), canonical.type(), canonical.summary(), canonical.provenance(),
                canonical.gameTime(), canonical.participants(), changedScope)));

        SemanticContradiction changedKind = new SemanticContradiction(
                new SemanticContradiction.ClaimSnapshot(
                        firstSnapshot.logicalClaimId(), firstSnapshot.detectedSemanticEntryId(),
                        SemanticMemoryEntry.Kind.BELIEF, MemoryEvent.Provenance.NPC_TOLD,
                        firstSnapshot.relatedEntities()),
                payload.second()
        );
        assertFalse(SemanticContradictionPolicy.valid(copy(
                canonical, canonical.ownerNpcId(), canonical.type(), canonical.summary(), canonical.provenance(),
                canonical.gameTime(), canonical.participants(), changedKind)));

        SemanticContradiction changedDetectedEntry = new SemanticContradiction(
                new SemanticContradiction.ClaimSnapshot(
                        firstSnapshot.logicalClaimId(), id(777), firstSnapshot.kind(), firstSnapshot.provenance(),
                        firstSnapshot.relatedEntities()),
                payload.second()
        );
        assertFalse(SemanticContradictionPolicy.valid(copy(
                canonical, canonical.ownerNpcId(), canonical.type(), canonical.summary(), canonical.provenance(),
                canonical.gameTime(), canonical.participants(), changedDetectedEntry)));
    }

    private static SemanticContradiction.ClaimSnapshot snapshot(
            UUID logical,
            UUID detected,
            SemanticMemoryEntry.Kind kind,
            MemoryEvent.Provenance provenance,
            List<UUID> scope
    ) {
        return new SemanticContradiction.ClaimSnapshot(logical, detected, kind, provenance, scope);
    }

    private static String snapshotCanonical(SemanticContradiction.ClaimSnapshot snapshot) {
        StringBuilder result = new StringBuilder()
                .append(snapshot.logicalClaimId()).append('\n')
                .append(snapshot.detectedSemanticEntryId()).append('\n')
                .append(snapshot.kind()).append('\n')
                .append(snapshot.provenance());
        for (UUID related : snapshot.relatedEntities()) result.append('\n').append(related);
        return result.toString();
    }

    private static MemoryEvent copy(
            MemoryEvent source,
            UUID owner,
            MemoryEvent.Type type,
            String summary,
            MemoryEvent.Provenance provenance,
            long gameTime,
            List<UUID> participants,
            SemanticContradiction contradiction
    ) {
        return new MemoryEvent(
                source.id(), owner, type, summary, participants, provenance, gameTime,
                source.createdAtEpochMillis(), source.importance(), source.emotionalWeight(), source.confidence(),
                source.relationshipReasons(), source.dialogue(), source.relationshipTransition(),
                source.relationshipCause(), source.knowledgeTransferProvenance(), contradiction
        );
    }

    private static UUID id(int value) {
        return UUID.fromString("00000000-0000-0000-0000-" + String.format("%012d", value));
    }
}
