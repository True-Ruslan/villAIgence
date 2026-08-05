package net.conczin.mca.security;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionVoiceTransportFixturePolicyTest {
    private static final Path ROOT = Path.of("..").toAbsolutePath().normalize();

    @Test
    void acceptanceMetadataRegistersRealVoicechatPluginOnlyInFixture() throws IOException {
        String fixtureMetadata = source(
                "fabric/src/productionAcceptanceFixture/resources/fabric.mod.json"
        );
        String productionMetadata = source("fabric/src/main/resources/fabric.mod.json");

        assertTrue(fixtureMetadata.contains("\"voicechat\""));
        assertTrue(fixtureMetadata.contains("ProductionAcceptanceVoiceTransportPlugin"));
        assertTrue(fixtureMetadata.contains("ProductionAcceptanceVoiceEvidenceWriter"));
        assertTrue(fixtureMetadata.contains("\"voicechat_api\": \">=2.6.20\""));
        assertFalse(productionMetadata.contains("ProductionAcceptanceVoiceTransportPlugin"));
        assertFalse(productionMetadata.contains("ProductionAcceptanceVoiceEvidenceWriter"));
    }

    @Test
    void codecMatrixUsesRealApiLossConcealmentBudgetAndClosure() throws IOException {
        String plugin = source(
                "fabric/src/productionAcceptanceFixture/java/net/conczin/mca/acceptancefixture/ProductionAcceptanceVoiceTransportPlugin.java"
        );

        assertTrue(plugin.contains("api.createEncoder()"));
        assertTrue(plugin.contains("api.createDecoder()"));
        assertTrue(plugin.contains("encoder.encode(frame(frame))"));
        assertTrue(plugin.contains("decoder.decode(null)"));
        assertTrue(plugin.contains("new VoicePcmBudget(PCM_BUDGET_BYTES)"));
        assertTrue(plugin.contains("primary.cancel()"));
        assertTrue(plugin.contains("disconnect.disconnect()"));
        assertTrue(plugin.contains("!encoder.isClosed()"));
        assertTrue(plugin.contains("!primaryDecoder.isClosed()"));
        assertTrue(plugin.contains("!disconnectDecoder.isClosed()"));
        assertTrue(plugin.contains("villaigence.acceptance.mode"));
        assertTrue(plugin.contains("\"recovery\""));
    }

    @Test
    void evidenceWriterFailsClosedAndUsesBoundedServerTicks() throws IOException {
        String writer = source(
                "fabric/src/productionAcceptanceFixture/java/net/conczin/mca/acceptancefixture/ProductionAcceptanceVoiceEvidenceWriter.java"
        );

        assertTrue(writer.contains("MAX_PENDING_TICKS = 200"));
        assertTrue(writer.contains("ProductionAcceptanceVoiceTransportState.failure()"));
        assertTrue(writer.contains("acceptance-voice-transport.json"));
        assertTrue(writer.contains("PASS_MARKER"));
        assertTrue(writer.contains("StandardCopyOption.ATOMIC_MOVE"));
        assertTrue(writer.contains("villaigence.acceptance.mode"));
    }

    private static String source(String relativePath) throws IOException {
        return Files.readString(ROOT.resolve(relativePath));
    }
}
