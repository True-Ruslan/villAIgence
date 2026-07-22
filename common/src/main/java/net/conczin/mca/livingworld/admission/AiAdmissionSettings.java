package net.conczin.mca.livingworld.admission;

import net.conczin.mca.livingworld.diagnostics.AiOperation;

/** Immutable non-secret limits used by AI admission control. */
public record AiAdmissionSettings(
        int chatMaxConcurrentRequests,
        int sttMaxConcurrentRequests,
        int ttsMaxConcurrentRequests,
        long perPlayerCooldownMillis,
        long providerRateLimitCooldownMillis
) {
    public int maxConcurrent(AiOperation operation) {
        return switch (operation) {
            case CHAT -> chatMaxConcurrentRequests;
            case STT -> sttMaxConcurrentRequests;
            case TTS -> ttsMaxConcurrentRequests;
        };
    }
}
