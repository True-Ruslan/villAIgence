package net.conczin.mca.livingworld.voice;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAIAudioProviderSourcePolicyTest {
    @Test
    void audioResponsesUsePerOperationBoundsWithoutUnboundedTransfer() throws IOException {
        Path sourcePath = Path.of(
                "src/main/java/net/conczin/mca/livingworld/voice/OpenAIAudioProvider.java"
        );
        assertTrue(Files.isRegularFile(sourcePath), sourcePath.toAbsolutePath().toString());

        String source = Files.readString(sourcePath);

        assertTrue(source.contains("BoundedResponseReader.readBytes"));
        assertTrue(source.contains("ProviderResponseLimits.STT_JSON_BYTES"));
        assertTrue(source.contains("ProviderResponseLimits.TTS_AUDIO_BYTES"));
        assertTrue(source.contains("ProviderResponseLimits.ERROR_BODY_BYTES"));
        assertFalse(source.contains("transferTo("));
        assertFalse(source.contains("private static byte[] readAll("));
    }
}
