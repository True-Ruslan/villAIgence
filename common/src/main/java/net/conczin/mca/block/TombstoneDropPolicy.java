package net.conczin.mca.block;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Loader-independent safety policy for retaining a filled tombstone as an item
 * even when its evaluated loot does not contain the tombstone block itself.
 */
public final class TombstoneDropPolicy {
    private TombstoneDropPolicy() {
    }

    public static <T> List<T> ensurePreservedDrop(
            List<T> drops,
            Predicate<T> isTombstone,
            Supplier<T> fallback,
            Consumer<T> preserve
    ) {
        Objects.requireNonNull(drops, "drops");
        Objects.requireNonNull(isTombstone, "isTombstone");
        Objects.requireNonNull(fallback, "fallback");
        Objects.requireNonNull(preserve, "preserve");

        List<T> result = new ArrayList<>(drops);
        T target = result.stream()
                .filter(isTombstone)
                .findFirst()
                .orElseGet(() -> {
                    T created = Objects.requireNonNull(fallback.get(), "fallback result");
                    result.add(created);
                    return created;
                });
        preserve.accept(target);
        return result;
    }
}
