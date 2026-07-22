package net.conczin.mca.livingworld.diagnostics;

import net.conczin.mca.livingworld.admission.AiAdmissionDecision;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdmissionDiagnosticsRecorderTest {
    @BeforeEach
    void reset() {
        AiDiagnostics.resetForTests();
    }

    @Test
    void recordsControlledLocalRejectionWithoutPretendingProviderFailure() {
        AdmissionDiagnosticsRecorder.recordRejected(AiOperation.CHAT, AiAdmissionDecision.SATURATED);

        AiOperationStatus status = AiDiagnostics.snapshot().chat();
        assertEquals(AiOperationState.FAILURE, status.state());
        assertEquals("admission_saturated", status.errorType());
        assertTrue(status.detail().contains("local_rejection"));
        assertTrue(status.provider().isBlank());
        assertTrue(status.model().isBlank());
        assertFalse(status.toString().contains("Authorization"));
    }

    @Test
    void mapsAllAdmissionReasonsToStableErrorTypes() {
        AdmissionDiagnosticsRecorder.recordRejected(AiOperation.CHAT, AiAdmissionDecision.PLAYER_COOLDOWN);
        assertEquals("admission_player_cooldown", AiDiagnostics.snapshot().chat().errorType());

        AdmissionDiagnosticsRecorder.recordRejected(AiOperation.STT, AiAdmissionDecision.PROVIDER_COOLDOWN);
        assertEquals("admission_provider_cooldown", AiDiagnostics.snapshot().stt().errorType());
    }
}
