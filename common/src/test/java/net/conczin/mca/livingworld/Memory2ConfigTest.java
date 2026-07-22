package net.conczin.mca.livingworld;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Memory2ConfigTest {
    @Test
    void defaultsEnableBoundedMemory2Ingestion() {
        LivingWorldConfig config = new LivingWorldConfig();
        assertTrue(config.memory2Enabled);
        assertEquals(256, config.memory2MaxEventsPerNpc);
    }

    @Test
    void versionTwoConfigAcceptsAndNormalizesMemory2Fields() {
        LivingWorldConfig disabled = LivingWorldConfig.parseJson("""
                {
                  "version": 2,
                  "memory2Enabled": false,
                  "memory2MaxEventsPerNpc": 9999
                }
                """);
        assertFalse(disabled.memory2Enabled);
        assertEquals(512, disabled.memory2MaxEventsPerNpc);

        LivingWorldConfig minimum = LivingWorldConfig.parseJson("""
                {
                  "version": 2,
                  "memory2MaxEventsPerNpc": 0
                }
                """);
        assertEquals(1, minimum.memory2MaxEventsPerNpc);
    }
}
