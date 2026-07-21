package net.conczin.mca.livingworld.voice;

import java.util.UUID;

/** Immutable server-thread capture of mutable NPC state needed for voice output. */
public record NpcVoiceSnapshot(
        UUID npcId,
        NpcVoiceGender gender,
        NpcVoiceAgeGroup ageGroup,
        boolean panicking,
        double healthRatio
) {
    public NpcVoiceSnapshot {
        if (npcId == null) throw new IllegalArgumentException("npcId is required");
        gender = gender == null ? NpcVoiceGender.NEUTRAL : gender;
        ageGroup = ageGroup == null ? NpcVoiceAgeGroup.ADULT : ageGroup;
        if (!Double.isFinite(healthRatio)) healthRatio = 1.0D;
        healthRatio = Math.max(0.0D, Math.min(1.0D, healthRatio));
    }
}
