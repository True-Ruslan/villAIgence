package net.conczin.mca.livingworld.ai;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoundedResponseReaderTest {
    @Test
    void rejectsDeclaredLengthAboveLimitBeforeReadingStream() {
        AtomicInteger reads = new AtomicInteger();
        InputStream stream = new InputStream() {
            @Override
            public int read() {
                reads.incrementAndGet();
                return 1;
            }
        };

        BoundedResponseReader.ResponseTooLargeException error = assertThrows(
                BoundedResponseReader.ResponseTooLargeException.class,
                () -> BoundedResponseReader.readBytes(stream, 11L, 10)
        );

        assertEquals(0, reads.get());
        assertEquals(10, error.limitBytes());
        assertEquals(11L, error.observedBytes());
        assertTrue(error.getMessage().contains("10"));
        assertTrue(error.getMessage().contains("11"));
    }

    @Test
    void acceptsBodyAtExactLimit() throws IOException {
        byte[] body = new byte[]{1, 2, 3, 4};

        assertArrayEquals(body, BoundedResponseReader.readBytes(
                new ByteArrayInputStream(body),
                body.length,
                body.length
        ));
    }

    @Test
    void rejectsUnknownLengthStreamAsSoonAsItCrossesLimit() {
        byte[] body = new byte[17];
        Arrays.fill(body, (byte) 7);

        BoundedResponseReader.ResponseTooLargeException error = assertThrows(
                BoundedResponseReader.ResponseTooLargeException.class,
                () -> BoundedResponseReader.readBytes(
                        new ByteArrayInputStream(body),
                        -1L,
                        16
                )
        );

        assertEquals(16, error.limitBytes());
        assertEquals(17L, error.observedBytes());
    }

    @Test
    void fragmentedReadsAreReconstructedWithoutUnboundedHelpers() throws IOException {
        byte[] body = "fragmented-body".getBytes(StandardCharsets.UTF_8);
        InputStream fragmented = new InputStream() {
            private int offset;

            @Override
            public int read() {
                if (offset >= body.length) return -1;
                return body[offset++] & 0xff;
            }

            @Override
            public int read(byte[] target, int off, int len) {
                if (offset >= body.length) return -1;
                target[off] = body[offset++];
                return 1;
            }
        };

        assertArrayEquals(body, BoundedResponseReader.readBytes(fragmented, -1L, 64));
    }

    @Test
    void utf8DecodingUsesByteLimitRatherThanCharacterCount() throws IOException {
        String text = "Привет";
        byte[] utf8 = text.getBytes(StandardCharsets.UTF_8);

        assertEquals(text, BoundedResponseReader.readUtf8(
                new ByteArrayInputStream(utf8),
                utf8.length,
                utf8.length
        ));
        assertThrows(BoundedResponseReader.ResponseTooLargeException.class, () ->
                BoundedResponseReader.readUtf8(
                        new ByteArrayInputStream(utf8),
                        -1L,
                        utf8.length - 1
                ));
    }

    @Test
    void invalidLimitsFailBeforeReading() {
        AtomicInteger reads = new AtomicInteger();
        InputStream stream = new InputStream() {
            @Override
            public int read() {
                reads.incrementAndGet();
                return -1;
            }
        };

        assertThrows(IllegalArgumentException.class, () ->
                BoundedResponseReader.readBytes(stream, -1L, 0));
        assertThrows(IllegalArgumentException.class, () ->
                BoundedResponseReader.readBytes(stream, -1L, -1));
        assertEquals(0, reads.get());
    }

    @Test
    void exceptionNeverContainsProviderPayload() {
        String secretPayload = "SECRET_PROVIDER_BODY";
        byte[] body = secretPayload.getBytes(StandardCharsets.UTF_8);

        Exception error = assertThrows(BoundedResponseReader.ResponseTooLargeException.class, () ->
                BoundedResponseReader.readBytes(new ByteArrayInputStream(body), -1L, 4));

        assertFalse(error.getMessage().contains(secretPayload));
    }
}
