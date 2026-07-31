package net.conczin.mca.livingworld.voice;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Source-boundary regression guard for the Fabric voice-capture cleanup order. */
class VoiceCaptureManagerSourcePolicyTest {
    @Test
    void pcmReservationIsReleasedEvenWhenDecoderCloseFails() throws IOException {
        Path sourcePath = Path.of(
                "..",
                "fabric",
                "src",
                "main",
                "java",
                "net",
                "conczin",
                "mca",
                "fabric",
                "livingworld",
                "voice",
                "VoiceCaptureManager.java"
        );
        assertTrue(Files.isRegularFile(sourcePath), sourcePath.toAbsolutePath().toString());

        String source = Files.readString(sourcePath);
        int closeMethod = source.indexOf("public synchronized void close()");
        int decoderClose = source.indexOf("decoder.close();", closeMethod);
        int finallyBlock = source.indexOf("} finally {", decoderClose);
        int budgetRelease = source.indexOf("pcmBudget.release(releaseBytes);", finallyBlock);

        assertTrue(closeMethod >= 0, "CaptureSession.close() must exist");
        assertTrue(decoderClose > closeMethod, "decoder cleanup must remain in CaptureSession.close()");
        assertTrue(finallyBlock > decoderClose, "PCM cleanup must be protected by finally");
        assertTrue(budgetRelease > finallyBlock, "PCM reservation must be released inside the finally block");
    }
}
