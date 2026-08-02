package net.conczin.mca.block;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TombstoneDropPolicyTest {
    @Test
    void createsPreservedTombstoneWhenLootOmitsBlock() {
        FakeDrop remains = new FakeDrop("remains", false);
        FakeDrop created = new FakeDrop("tombstone", true);
        List<FakeDrop> input = List.of(remains);

        List<FakeDrop> result = TombstoneDropPolicy.ensurePreservedDrop(
                input,
                FakeDrop::isTombstone,
                () -> created,
                drop -> drop.preserved = true
        );

        assertEquals(List.of(remains, created), result);
        assertTrue(created.preserved);
        assertEquals(List.of(remains), input);
    }

    @Test
    void reusesExistingTombstoneWithoutCreatingDuplicate() {
        FakeDrop remains = new FakeDrop("remains", false);
        FakeDrop tombstone = new FakeDrop("tombstone", true);
        AtomicInteger fallbackCalls = new AtomicInteger();

        List<FakeDrop> result = TombstoneDropPolicy.ensurePreservedDrop(
                List.of(remains, tombstone),
                FakeDrop::isTombstone,
                () -> {
                    fallbackCalls.incrementAndGet();
                    return new FakeDrop("duplicate", true);
                },
                drop -> drop.preserved = true
        );

        assertEquals(List.of(remains, tombstone), result);
        assertSame(tombstone, result.get(1));
        assertTrue(tombstone.preserved);
        assertEquals(0, fallbackCalls.get());
    }

    @Test
    void preservesUnrelatedDropOrder() {
        FakeDrop first = new FakeDrop("first", false);
        FakeDrop tombstone = new FakeDrop("tombstone", true);
        FakeDrop last = new FakeDrop("last", false);

        List<FakeDrop> result = TombstoneDropPolicy.ensurePreservedDrop(
                List.of(first, tombstone, last),
                FakeDrop::isTombstone,
                () -> new FakeDrop("fallback", true),
                drop -> drop.preserved = true
        );

        assertEquals(List.of(first, tombstone, last), result);
        assertFalse(first.preserved);
        assertFalse(last.preserved);
    }

    @Test
    void serializesExactlyOneTombstoneDrop() {
        FakeDrop firstTombstone = new FakeDrop("first", true);
        FakeDrop secondTombstone = new FakeDrop("second", true);
        AtomicInteger preserveCalls = new AtomicInteger();

        List<FakeDrop> result = TombstoneDropPolicy.ensurePreservedDrop(
                List.of(firstTombstone, secondTombstone),
                FakeDrop::isTombstone,
                () -> new FakeDrop("fallback", true),
                drop -> {
                    preserveCalls.incrementAndGet();
                    drop.preserved = true;
                }
        );

        assertEquals(2, result.size());
        assertTrue(firstTombstone.preserved);
        assertFalse(secondTombstone.preserved);
        assertEquals(1, preserveCalls.get());
    }

    @Test
    void runtimeWiringUsesOwnedTombstoneSourceWithoutMixinInjection() throws IOException {
        Path root = Path.of("src/main");
        String mixinConfig = Files.readString(root.resolve("resources/mca.mixins.json"));
        Path obsoleteMixin = root.resolve("java/net/conczin/mca/mixin/MixinTombstoneBlock.java");
        String tombstoneSource = Files.readString(
                root.resolve("java/net/conczin/mca/block/TombstoneBlock.java")
        );

        assertFalse(mixinConfig.contains("\"MixinTombstoneBlock\""),
                "Owned TombstoneBlock behavior must not depend on a runtime Mixin target");
        assertFalse(Files.exists(obsoleteMixin),
                "Obsolete MixinTombstoneBlock source must be removed");
        assertTrue(tombstoneSource.contains("TombstoneDropPolicy.ensurePreservedDrop("));
        assertTrue(tombstoneSource.contains("filter(Data::hasEntity)"));
        assertTrue(tombstoneSource.contains("data::writeToStack"));
    }

    private static final class FakeDrop {
        private final String name;
        private final boolean tombstone;
        private boolean preserved;

        private FakeDrop(String name, boolean tombstone) {
            this.name = name;
            this.tombstone = tombstone;
        }

        private boolean isTombstone() {
            return tombstone;
        }

        @Override
        public boolean equals(Object other) {
            return this == other;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(this);
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
