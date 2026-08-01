package net.conczin.mca.entity;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Loader-independent lifecycle policy for replacing an entity while retaining its UUID.
 *
 * <p>The target remains unregistered while source state is snapshotted and copied. The source is
 * discarded before target registration, preventing two live registered entities from sharing the
 * same UUID. Creation failure and already-removed sources fail soft without discarding anything.</p>
 */
final class VillagerConversionIdentityPolicy {
    private VillagerConversionIdentityPolicy() {
    }

    static <T, S> Optional<T> convert(
            boolean sourceRemoved,
            Supplier<? extends T> targetFactory,
            Supplier<? extends S> snapshotFactory,
            BiConsumer<? super T, ? super S> targetPreparer,
            Runnable sourceDiscarder,
            Consumer<? super T> targetRegistrar,
            Consumer<? super T> vehicleRestorer
    ) {
        if (sourceRemoved) {
            return Optional.empty();
        }

        T target = targetFactory.get();
        if (target == null) {
            return Optional.empty();
        }

        S snapshot = snapshotFactory.get();
        targetPreparer.accept(target, snapshot);
        sourceDiscarder.run();
        targetRegistrar.accept(target);
        vehicleRestorer.accept(target);
        return Optional.of(target);
    }
}
