package net.conczin.mca.livingworld.voice;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VoicePcmBudgetTest {
    @Test
    void reservesUpToExactMaximumAndRejectsOverflowWithoutMutation() {
        VoicePcmBudget budget = new VoicePcmBudget(10);

        assertTrue(budget.tryReserve(4));
        assertEquals(4, budget.usedBytes());
        assertTrue(budget.tryReserve(6));
        assertEquals(10, budget.usedBytes());
        assertFalse(budget.tryReserve(1));
        assertEquals(10, budget.usedBytes());
    }

    @Test
    void releaseReturnsCapacityAndRejectsProgrammingErrors() {
        VoicePcmBudget budget = new VoicePcmBudget(10);
        assertTrue(budget.tryReserve(8));

        budget.release(3);
        assertEquals(5, budget.usedBytes());
        assertTrue(budget.tryReserve(5));
        assertEquals(10, budget.usedBytes());

        assertThrows(IllegalStateException.class, () -> budget.release(11));
        assertEquals(10, budget.usedBytes());
    }

    @Test
    void nonPositiveInputsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new VoicePcmBudget(0));
        assertThrows(IllegalArgumentException.class, () -> new VoicePcmBudget(-1));

        VoicePcmBudget budget = new VoicePcmBudget(10);
        assertThrows(IllegalArgumentException.class, () -> budget.tryReserve(0));
        assertThrows(IllegalArgumentException.class, () -> budget.tryReserve(-1));
        assertThrows(IllegalArgumentException.class, () -> budget.release(0));
        assertThrows(IllegalArgumentException.class, () -> budget.release(-1));
    }

    @Test
    void concurrentReservationsNeverExceedMaximum() throws Exception {
        VoicePcmBudget budget = new VoicePcmBudget(1_000);
        int workers = 20;
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        CountDownLatch ready = new CountDownLatch(workers);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger accepted = new AtomicInteger();

        try {
            for (int i = 0; i < workers; i++) {
                executor.submit(() -> {
                    ready.countDown();
                    try {
                        start.await();
                        if (budget.tryReserve(100)) accepted.incrementAndGet();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            executor.shutdown();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

            assertEquals(10, accepted.get());
            assertEquals(1_000, budget.usedBytes());
            assertTrue(budget.usedBytes() <= budget.maxBytes());
        } finally {
            executor.shutdownNow();
        }
    }
}
