package net.conczin.mca.livingworld.voice;

import java.util.UUID;

/** Persistent, provider-neutral voice identity for one NPC. */
public record NpcVoiceProfile(
        UUID npcId,
        NpcVoiceGender gender,
        NpcVoiceAgeGroup ageGroup,
        String voiceId
) {
    public NpcVoiceProfile {
        if (npcId == null) throw new IllegalArgumentException("npcId is required");
        gender = gender == null ? NpcVoiceGender.NEUTRAL : gender;
        ageGroup = ageGroup == null ? NpcVoiceAgeGroup.ADULT : ageGroup;
        voiceId = voiceId == null ? "" : voiceId.trim();
        if (voiceId.isBlank()) throw new IllegalArgumentException("voiceId is required");
    }
}
