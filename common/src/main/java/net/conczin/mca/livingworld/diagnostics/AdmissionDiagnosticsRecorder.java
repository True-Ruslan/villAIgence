package net.conczin.mca.livingworld.diagnostics;

import net.conczin.mca.livingworld.admission.AiAdmissionDecision;

/** Records local VillAIgence admission rejections without mislabeling them as provider failures. */
public final class AdmissionDiagnosticsRecorder {
    private AdmissionDiagnosticsRecorder() {
    }

    public static void recordRejected(AiOperation operation, AiAdmissionDecision decision) {
        if (decision == null || decision == AiAdmissionDecision.ALLOWED) return;
        AiDiagnostics.recordFailure(
                operation,
                0L,
                "",
                "",
                null,
                errorType(decision),
                null,
                "local_rejection"
        );
    }

    static String errorType(AiAdmissionDecision decision) {
        return switch (decision) {
            case SATURATED -> "admission_saturated";
            case PLAYER_COOLDOWN -> "admission_player_cooldown";
            case PROVIDER_COOLDOWN -> "admission_provider_cooldown";
            case ALLOWED -> "";
        };
    }
}
