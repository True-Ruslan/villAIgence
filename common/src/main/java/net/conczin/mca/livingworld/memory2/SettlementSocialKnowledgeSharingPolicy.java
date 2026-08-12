package net.conczin.mca.livingworld.memory2;

import net.conczin.mca.livingworld.context.NpcPairDisposition;

/** Conservative server-owned gate for sharing knowledge with an already-selected NPC counterpart. */
final class SettlementSocialKnowledgeSharingPolicy {
    private SettlementSocialKnowledgeSharingPolicy() {
    }

    static boolean isAllowed(NpcPairDisposition disposition) {
        NpcPairDisposition canonical = disposition == null ? NpcPairDisposition.NEUTRAL : disposition;
        return switch (canonical) {
            case FEARFUL, DISTRUSTFUL, ANTIPATHETIC -> false;
            case NEUTRAL, AFFILIATIVE, RESPECTFUL -> true;
        };
    }
}
