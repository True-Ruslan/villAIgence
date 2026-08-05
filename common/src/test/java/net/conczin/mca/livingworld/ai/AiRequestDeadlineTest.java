package net.conczin.mca.livingworld.ai;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiRequestDeadlineTest {
    @Test
    void budgetIsSharedAcrossConnectAndReadPhases() throws Exception {
        AtomicLong clock = new AtomicLong(10_000L);
        AiRequestDeadline deadline = AiRequestDeadline.start(500, 2_000, clock::get);

        assertEquals(500, deadline.boundedTimeoutMillis(500));
        clock.addAndGet(TimeUnit.MILLISECONDS.toNanos(1_700L));
        assertEquals(800, deadline.boundedTimeoutMillis(2_000));
        assertFalse(deadline.isExpired());
    }

    @Test
    void subMillisecondRemainderRoundsUpInsteadOfBecomingInfiniteTimeout() throws Exception {
        AtomicLong clock = new AtomicLong();
        AiRequestDeadline deadline = AiRequestDeadline.start(1, 1, clock::get);

        clock.set(TimeUnit.MILLISECONDS.toNanos(2L) - 1L);
        assertEquals(1, deadline.boundedTimeoutMillis(1_000));
    }

    @Test
    void exhaustedBudgetFailsClosed() {
        AtomicLong clock = new AtomicLong();
        AiRequestDeadline deadline = AiRequestDeadline.start(100, 200, clock::get);

        clock.set(TimeUnit.MILLISECONDS.toNanos(300L));
        assertTrue(deadline.isExpired());
        assertThrows(
                AiRequestDeadline.DeadlineExceededException.class,
                () -> deadline.boundedTimeoutMillis(200)
        );
    }

    @Test
    void nanoTimeWraparoundDoesNotExtendTheBudget() throws Exception {
        AtomicLong clock = new AtomicLong(Long.MAX_VALUE - TimeUnit.MILLISECONDS.toNanos(50L));
        AiRequestDeadline deadline = AiRequestDeadline.start(100, 100, clock::get);

        clock.addAndGet(TimeUnit.MILLISECONDS.toNanos(150L));
        assertEquals(50, deadline.boundedTimeoutMillis(100));
        clock.addAndGet(TimeUnit.MILLISECONDS.toNanos(50L));
        assertTrue(deadline.isExpired());
    }

    @Test
    void nonPositiveConfiguredTimeoutIsRejected() {
        AtomicLong clock = new AtomicLong();
        assertThrows(
                IllegalArgumentException.class,
                () -> AiRequestDeadline.start(0, 100, clock::get)
        );
        AiRequestDeadline deadline = AiRequestDeadline.start(100, 100, clock::get);
        assertThrows(
                IllegalArgumentException.class,
                () -> deadline.boundedTimeoutMillis(0)
        );
    }
}
