package net.conczin.mca.livingworld.context;

import net.conczin.mca.entity.ai.relationship.Personality;
import net.conczin.mca.livingworld.relationship.NpcSocialState;

import java.util.UUID;

/** Fixed-size immutable server-owned personality and optional direct NPC-pair social state. */
public record PersonalitySocialSnapshot(
        UUID sourceNpcId,
        Personality personality,
        UUID counterpartNpcId,
        NpcSocialState directedSocialState
) {
    public PersonalitySocialSnapshot {
        if (sourceNpcId == null) {
            throw new IllegalArgumentException("sourceNpcId is required");
        }
        if (sourceNpcId.equals(counterpartNpcId)) {
            throw new IllegalArgumentException("NPC social snapshot cannot target itself");
        }
        personality = personality == null ? Personality.UNASSIGNED : personality;
        directedSocialState = counterpartNpcId == null
                ? NpcSocialState.NEUTRAL
                : directedSocialState == null ? NpcSocialState.NEUTRAL : directedSocialState;
    }

    public boolean hasCounterpart() {
        return counterpartNpcId != null;
    }
}
