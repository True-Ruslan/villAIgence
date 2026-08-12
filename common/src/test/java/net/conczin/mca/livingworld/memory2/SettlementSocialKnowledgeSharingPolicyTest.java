package net.conczin.mca.livingworld.memory2;

import net.conczin.mca.livingworld.context.NpcPairDisposition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettlementSocialKnowledgeSharingPolicyTest {
    @Test
    void adverseDirectPairDispositionsSuppressSharing() {
        assertFalse(SettlementSocialKnowledgeSharingPolicy.isAllowed(NpcPairDisposition.FEARFUL));
        assertFalse(SettlementSocialKnowledgeSharingPolicy.isAllowed(NpcPairDisposition.DISTRUSTFUL));
        assertFalse(SettlementSocialKnowledgeSharingPolicy.isAllowed(NpcPairDisposition.ANTIPATHETIC));
    }

    @Test
    void neutralAndPositivePairDispositionsPreserveSharing() {
        assertTrue(SettlementSocialKnowledgeSharingPolicy.isAllowed(null));
        assertTrue(SettlementSocialKnowledgeSharingPolicy.isAllowed(NpcPairDisposition.NEUTRAL));
        assertTrue(SettlementSocialKnowledgeSharingPolicy.isAllowed(NpcPairDisposition.AFFILIATIVE));
        assertTrue(SettlementSocialKnowledgeSharingPolicy.isAllowed(NpcPairDisposition.RESPECTFUL));
    }
}
