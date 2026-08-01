package net.conczin.mca.block;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TombstoneEntityDataCodecTest {
    @Test
    void currentComponentTakesPrecedenceOverLegacyComponent() {
        Map<String, String> current = payload("current");
        Map<String, String> legacy = payload("legacy");

        Optional<Map<String, String>> selected = TombstoneItemDataPolicy.read(
                () -> current,
                () -> legacy,
                HashMap::new
        );

        assertEquals("current", selected.orElseThrow().get("EntityName"));
    }

    @Test
    void legacyComponentIsReadWhenCurrentComponentIsAbsent() {
        Map<String, String> legacy = payload("legacy");

        Optional<Map<String, String>> selected = TombstoneItemDataPolicy.read(
                () -> null,
                () -> legacy,
                HashMap::new
        );

        assertEquals("legacy", selected.orElseThrow().get("EntityName"));
    }

    @Test
    void absentComponentsReturnEmpty() {
        assertTrue(TombstoneItemDataPolicy.read(
                () -> null,
                () -> null,
                HashMap::new
        ).isEmpty());
    }

    @Test
    void readReturnsADefensiveCopy() {
        Map<String, String> current = payload("Pio");

        Map<String, String> selected = TombstoneItemDataPolicy.read(
                () -> current,
                () -> null,
                HashMap::new
        ).orElseThrow();

        assertNotSame(current, selected);
        selected.put("EntityName", "mutated-read");
        assertEquals("Pio", current.get("EntityName"));
    }

    @Test
    void writePublishesExactlyOneDefensiveCopyToTheCurrentComponent() {
        Map<String, String> source = payload("Pio");
        AtomicReference<Map<String, String>> written = new AtomicReference<>();
        AtomicInteger writes = new AtomicInteger();

        TombstoneItemDataPolicy.write(
                source,
                value -> {
                    writes.incrementAndGet();
                    written.set(value);
                },
                HashMap::new
        );
        source.put("EntityName", "mutated-after-write");

        assertEquals(1, writes.get());
        assertEquals("Pio", written.get().get("EntityName"));
        assertNotSame(source, written.get());
    }

    private static Map<String, String> payload(String name) {
        Map<String, String> payload = new HashMap<>();
        payload.put("EntityName", name);
        payload.put("EntityGender", "0");
        payload.put("EntityData.id", "mca:villager");
        return payload;
    }
}
