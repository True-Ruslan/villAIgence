package net.conczin.mca.entity;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VillagerConversionIdentityTest {
    @Test
    void sourceDataIsCapturedAndPreparedBeforeSourceDiscard() {
        List<String> events = new ArrayList<>();
        AtomicReference<String> preparedSnapshot = new AtomicReference<>();

        Optional<String> converted = VillagerConversionIdentityPolicy.convert(
                false,
                () -> {
                    events.add("create");
                    return "target";
                },
                () -> {
                    events.add("snapshot");
                    return "conversion-data";
                },
                (target, snapshot) -> {
                    events.add("prepare");
                    preparedSnapshot.set(target + ":" + snapshot);
                },
                () -> events.add("discard"),
                target -> events.add("register"),
                target -> events.add("remount")
        );

        assertEquals(Optional.of("target"), converted);
        assertEquals("target:conversion-data", preparedSnapshot.get());
        assertEquals(List.of("create", "snapshot", "prepare", "discard", "register", "remount"), events);
    }

    @Test
    void removedSourceDoesNotCreateOrDiscardAnything() {
        AtomicInteger calls = new AtomicInteger();

        Optional<String> converted = VillagerConversionIdentityPolicy.convert(
                true,
                () -> {
                    calls.incrementAndGet();
                    return "target";
                },
                () -> {
                    calls.incrementAndGet();
                    return "data";
                },
                (target, snapshot) -> calls.incrementAndGet(),
                calls::incrementAndGet,
                target -> calls.incrementAndGet(),
                target -> calls.incrementAndGet()
        );

        assertTrue(converted.isEmpty());
        assertEquals(0, calls.get());
    }

    @Test
    void creationFailureLeavesSourceRegistered() {
        AtomicBoolean discarded = new AtomicBoolean();
        AtomicBoolean registered = new AtomicBoolean();

        Optional<String> converted = VillagerConversionIdentityPolicy.convert(
                false,
                () -> null,
                () -> "data",
                (target, snapshot) -> {
                },
                () -> discarded.set(true),
                target -> registered.set(true),
                target -> {
                }
        );

        assertTrue(converted.isEmpty());
        assertFalse(discarded.get());
        assertFalse(registered.get());
    }

    @Test
    void targetRegistrationNeverOverlapsTheSourceUuid() {
        AtomicBoolean sourceRegistered = new AtomicBoolean(true);
        AtomicBoolean duplicateUuidObserved = new AtomicBoolean();

        Optional<String> converted = VillagerConversionIdentityPolicy.convert(
                false,
                () -> "same-uuid-target",
                () -> "data",
                (target, snapshot) -> {
                },
                () -> sourceRegistered.set(false),
                target -> {
                    if (sourceRegistered.get()) {
                        duplicateUuidObserved.set(true);
                    }
                },
                target -> {
                }
        );

        assertEquals(Optional.of("same-uuid-target"), converted);
        assertFalse(duplicateUuidObserved.get());
        assertFalse(sourceRegistered.get());
    }
}
