package net.conczin.mca.livingworld.memory2;

/**
 * Turn-local query text bridge for synchronous memory capture.
 *
 * <p>The value is never persisted and must be scoped with try-with-resources so unrelated turns cannot inherit it.</p>
 */
public final class MemoryRecallQueryContext {
    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private MemoryRecallQueryContext() {
    }

    public static Scope open(String queryText) {
        String previous = CURRENT.get();
        CURRENT.set(MemoryQueryTextMatcher.boundQuery(queryText));
        return new Scope(previous);
    }

    public static String current() {
        String current = CURRENT.get();
        return current == null ? "" : current;
    }

    public static final class Scope implements AutoCloseable {
        private final String previous;
        private boolean closed;

        private Scope(String previous) {
            this.previous = previous;
        }

        @Override
        public void close() {
            if (closed) return;
            closed = true;
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }
}
