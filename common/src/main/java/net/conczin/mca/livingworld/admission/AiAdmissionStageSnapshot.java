package net.conczin.mca.livingworld.admission;

/** Process-local admission metrics for one AI stage. */
public record AiAdmissionStageSnapshot(
        int active,
        int maxConcurrent,
        long rejected,
        long providerCooldownRemainingMillis
) {
}
