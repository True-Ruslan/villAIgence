package net.conczin.mca.livingworld.diagnostics;

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
