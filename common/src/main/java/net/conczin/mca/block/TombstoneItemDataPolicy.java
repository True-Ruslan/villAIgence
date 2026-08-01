package net.conczin.mca.block;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Loader-independent selection and copy policy for tombstone item data components.
 *
 * <p>The Minecraft adapter supplies the current BLOCK_ENTITY_DATA component first and the
 * historical ENTITY_DATA component second. Values are copied at the policy boundary so neither
 * reads nor writes expose mutable component payloads to callers.</p>
 */
final class TombstoneItemDataPolicy {
    private TombstoneItemDataPolicy() {
    }

    static <T> Optional<T> read(
            Supplier<? extends T> currentReader,
            Supplier<? extends T> legacyReader,
            UnaryOperator<T> copier
    ) {
        T current = currentReader.get();
        if (current != null) {
            return Optional.of(copier.apply(current));
        }

        T legacy = legacyReader.get();
        return legacy == null ? Optional.empty() : Optional.of(copier.apply(legacy));
    }

    static <T> void write(T value, Consumer<? super T> currentWriter, UnaryOperator<T> copier) {
        currentWriter.accept(copier.apply(value));
    }
}
