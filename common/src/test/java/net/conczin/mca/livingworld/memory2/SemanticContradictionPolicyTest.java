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
    void deterministicIdentityIsOrderIndependentAndNamespaced() {
        UUID owner = id(1);
        UUID first = id(10);
        UUID second = id(20);
        long gameTime = 123L;
        String canonical = "semantic-contradiction-v1\n"
                + owner + "\n" + first + "\n" + second + "\n" + gameTime;
        UUID expected = UUID.nameUUIDFromBytes(canonical.getBytes(StandardCharsets.UTF_8));

        assertEquals(expected,
                SemanticContradictionPolicy.deterministicEventId(owner, first, second, gameTime));
        assertEquals(expected,
                SemanticContradictionPolicy.deterministicEventId(owner, second, first, gameTime));
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
