package net.conczin.mca.livingworld.voice;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Deterministic voice selection and fallback policy for NPC voice profiles. */
public final class NpcVoiceCatalog {
    private final VoicePools pools;

    public NpcVoiceCatalog(VoicePools pools) {
        this.pools = Objects.requireNonNull(pools, "pools").normalized();
    }

    public String select(UUID npcId, NpcVoiceGender gender, NpcVoiceAgeGroup ageGroup) {
        Objects.requireNonNull(npcId, "npcId");
        List<String> pool = firstNonEmpty(candidatePools(gender, ageGroup));
        if (pool.isEmpty()) {
            throw new IllegalStateException("No TTS voice is configured");
        }
        int hash = npcId.hashCode();
        return pool.get(Math.floorMod(hash, pool.size()));
    }

    public boolean isEligible(String voiceId, NpcVoiceGender gender, NpcVoiceAgeGroup ageGroup) {
        if (voiceId == null || voiceId.isBlank()) return false;
        return candidatePools(gender, ageGroup).stream().anyMatch(pool -> pool.contains(voiceId));
    }

    private List<List<String>> candidatePools(NpcVoiceGender gender, NpcVoiceAgeGroup ageGroup) {
        NpcVoiceGender safeGender = gender == null ? NpcVoiceGender.NEUTRAL : gender;
        NpcVoiceAgeGroup safeAge = ageGroup == null ? NpcVoiceAgeGroup.ADULT : ageGroup;
        List<List<String>> result = new ArrayList<>();
        result.add(exactPool(safeGender, safeAge));
        if (safeAge != NpcVoiceAgeGroup.ADULT && safeGender != NpcVoiceGender.NEUTRAL) {
            result.add(exactPool(safeGender, NpcVoiceAgeGroup.ADULT));
        }
        if (safeGender != NpcVoiceGender.NEUTRAL) {
            result.add(exactPool(NpcVoiceGender.NEUTRAL, safeAge));
        }
        result.add(pools.globalFallback());
        if (!pools.legacyFallback().isBlank()) result.add(List.of(pools.legacyFallback()));
        return result;
    }

    private List<String> exactPool(NpcVoiceGender gender, NpcVoiceAgeGroup ageGroup) {
        return switch (ageGroup) {
            case CHILD -> switch (gender) {
                case MALE -> pools.maleChild();
                case FEMALE -> pools.femaleChild();
                case NEUTRAL -> pools.neutralChild();
            };
            case TEEN -> switch (gender) {
                case MALE -> pools.maleTeen();
                case FEMALE -> pools.femaleTeen();
                case NEUTRAL -> pools.neutralTeen();
            };
            case ADULT -> switch (gender) {
                case MALE -> pools.maleAdult();
                case FEMALE -> pools.femaleAdult();
                case NEUTRAL -> pools.neutralAdult();
            };
        };
    }

    private static List<String> firstNonEmpty(List<List<String>> candidates) {
        for (List<String> candidate : candidates) {
            if (!candidate.isEmpty()) return candidate;
        }
        return List.of();
    }

    public record VoicePools(
            List<String> maleChild,
            List<String> femaleChild,
            List<String> neutralChild,
            List<String> maleTeen,
            List<String> femaleTeen,
            List<String> neutralTeen,
            List<String> maleAdult,
            List<String> femaleAdult,
            List<String> neutralAdult,
            List<String> globalFallback,
            String legacyFallback
    ) {
        VoicePools normalized() {
            return new VoicePools(
                    normalize(maleChild), normalize(femaleChild), normalize(neutralChild),
                    normalize(maleTeen), normalize(femaleTeen), normalize(neutralTeen),
                    normalize(maleAdult), normalize(femaleAdult), normalize(neutralAdult),
                    normalize(globalFallback), legacyFallback == null ? "" : legacyFallback.trim()
            );
        }

        private static List<String> normalize(List<String> values) {
            if (values == null || values.isEmpty()) return List.of();
            LinkedHashSet<String> unique = new LinkedHashSet<>();
            for (String value : values) {
                if (value != null && !value.isBlank()) unique.add(value.trim());
            }
            return List.copyOf(unique);
        }
    }
}
