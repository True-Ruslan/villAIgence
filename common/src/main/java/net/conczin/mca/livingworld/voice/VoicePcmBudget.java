package net.conczin.mca.livingworld.voice;

import java.util.concurrent.atomic.AtomicLong;

/** Thread-safe server-wide reservation budget for active microphone PCM bytes. */
public final class VoicePcmBudget {
    private final long maxBytes;
    private final AtomicLong usedBytes = new AtomicLong();

    public VoicePcmBudget(long maxBytes) {
        if (maxBytes <= 0L) throw new IllegalArgumentException("maxBytes must be positive");
        this.maxBytes = maxBytes;
    }

    public boolean tryReserve(long bytes) {
        if (bytes <= 0L) throw new IllegalArgumentException("bytes must be positive");
        while (true) {
            long current = usedBytes.get();
            if (bytes > maxBytes - current) return false;
            if (usedBytes.compareAndSet(current, current + bytes)) return true;
        }
    }

    public void release(long bytes) {
        if (bytes <= 0L) throw new IllegalArgumentException("bytes must be positive");
        while (true) {
            long current = usedBytes.get();
            if (bytes > current) {
                throw new IllegalStateException("Cannot release more PCM bytes than are reserved");
            }
            if (usedBytes.compareAndSet(current, current - bytes)) return;
        }
    }

    public long usedBytes() {
        return usedBytes.get();
    }

    public long maxBytes() {
        return maxBytes;
    }
}
