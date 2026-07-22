package net.conczin.mca.livingworld.diagnostics;

import net.conczin.mca.livingworld.admission.AiAdmissionSnapshot;
import net.conczin.mca.livingworld.admission.AiAdmissionStageSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiStatusReportTest {
    @Test
    void formatsConfigurationAndLatestRuntimeResultsDeterministically() {
        AiDiagnosticsConfigSnapshot config = new AiDiagnosticsConfigSnapshot(
                new AiStageConfig(AiConfigState.CONFIGURED, true, true, "openrouter", "chat-model", "openrouter.ai", ""),
                new AiStageConfig(AiConfigState.MISCONFIGURED, true, false, "openrouter", "stt-model", "openrouter.ai", "json_base64"),
                new AiStageConfig(AiConfigState.DISABLED, false, true, "openrouter", "tts-model", "openrouter.ai", "pcm")
        );
        AiDiagnosticsSnapshot runtime = new AiDiagnosticsSnapshot(
                new AiOperationStatus(AiOperationState.SUCCESS, 100L, 824L, "openrouter", "chat-model", "stop", "", "gen-chat", "attempts=1"),
                new AiOperationStatus(AiOperationState.FAILURE, 200L, 391L, "openrouter", "stt-model", "", "http_402", "gen-stt", "Payment Required"),
                AiOperationStatus.never()
        );

        List<String> lines = AiStatusReport.format(config, runtime);
        String report = String.join("\n", lines);

        assertTrue(report.startsWith("VillAIgence AI Status"));
        assertTrue(report.contains("Chat: CONFIGURED"));
        assertTrue(report.contains("last: SUCCESS"));
        assertTrue(report.contains("824 ms"));
        assertTrue(report.contains("finish=stop"));
        assertTrue(report.contains("STT: MISCONFIGURED"));
        assertTrue(report.contains("type=http_402"));
        assertTrue(report.contains("TTS: DISABLED"));
        assertTrue(report.contains("last: NEVER"));
    }

    @Test
    void formatsAdmissionMetricsWithoutActorIdentifiers() {
        AiDiagnosticsConfigSnapshot config = new AiDiagnosticsConfigSnapshot(
                new AiStageConfig(AiConfigState.CONFIGURED, true, true, "openrouter", "chat-model", "openrouter.ai", ""),
                new AiStageConfig(AiConfigState.CONFIGURED, true, true, "openrouter", "stt-model", "openrouter.ai", "json_base64"),
                new AiStageConfig(AiConfigState.CONFIGURED, true, true, "openrouter", "tts-model", "openrouter.ai", "pcm")
        );
        AiAdmissionSnapshot admission = new AiAdmissionSnapshot(
                new AiAdmissionStageSnapshot(1, 4, 3, 0),
                new AiAdmissionStageSnapshot(0, 2, 1, 4_200),
                new AiAdmissionStageSnapshot(0, 2, 0, 0)
        );

        String report = String.join("\n", AiStatusReport.format(config, new AiDiagnosticsSnapshot(null, null, null), admission));

        assertTrue(report.contains("Admission CHAT: active=1/4 | rejected=3 | providerCooldownMs=0"));
        assertTrue(report.contains("Admission STT: active=0/2 | rejected=1 | providerCooldownMs=4200"));
        assertTrue(report.contains("Admission TTS: active=0/2 | rejected=0 | providerCooldownMs=0"));
        assertFalse(report.contains("00000000-0000-0000-0000"));
    }

    @Test
    void reportNeverContainsCredentialSentinelBecauseConfigHasNoCredentialValueField() {
        AiDiagnosticsConfigSnapshot config = new AiDiagnosticsConfigSnapshot(
                new AiStageConfig(AiConfigState.CONFIGURED, true, true, "openrouter", "SECRET_SENTINEL-model", "openrouter.ai", ""),
                new AiStageConfig(AiConfigState.DISABLED, false, false, "", "", "<invalid>", ""),
                new AiStageConfig(AiConfigState.DISABLED, false, false, "", "", "<invalid>", "")
        );

        String report = String.join("\n", AiStatusReport.format(config, new AiDiagnosticsSnapshot(null, null, null)));

        assertTrue(report.contains("SECRET_SENTINEL-model"));
        assertFalse(report.contains("sk-"));
        assertFalse(report.contains("Authorization"));
    }
}
