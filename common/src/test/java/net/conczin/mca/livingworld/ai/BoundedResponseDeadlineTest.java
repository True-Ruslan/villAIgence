package net.conczin.mca.livingworld.ai;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoundedResponseDeadlineTest {
    @Test
    void slowDripStreamCannotExtendBodyReadIndefinitely() {
        AtomicLong clock = new AtomicLong();
        InputStream slowDrip = new InputStream() {
            private int remaining = 32;

            @Override
            public int read() {
                if (remaining-- <= 0) return -1;
                clock.addAndGet(4L);
                return 1;
            }

            @Override
            public int read(byte[] target, int offset, int length) throws IOException {
                int value = read();
                if (value < 0) return -1;
                target[offset] = (byte) value;
                return 1;
            }
        };

        BoundedResponseReader.ResponseDeadlineExceededException error = assertThrows(
                BoundedResponseReader.ResponseDeadlineExceededException.class,
                () -> BoundedResponseReader.readBytes(
                        slowDrip,
                        -1L,
                        1024,
                        10L,
                        clock::get
                )
        );

        assertEquals(10L, error.limitNanos());
        assertTrue(error.elapsedNanos() >= 10L);
        assertTrue(error.getMessage().contains("deadline"));
    }

    @Test
    void fastBodyWithinDeadlineStillSucceeds() throws IOException {
        AtomicLong clock = new AtomicLong();
        InputStream fast = new InputStream() {
            private int remaining = 4;

            @Override
            public int read() {
                if (remaining-- <= 0) return -1;
                clock.incrementAndGet();
                return 7;
            }

            @Override
            public int read(byte[] target, int offset, int length) throws IOException {
                int value = read();
                if (value < 0) return -1;
                target[offset] = (byte) value;
                return 1;
            }
        };

        byte[] body = BoundedResponseReader.readBytes(fast, -1L, 16, 10L, clock::get);
        assertEquals(4, body.length);
    }
}
