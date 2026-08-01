package net.conczin.mca.entity.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ArcherControlPolicyTest {
    @Test
    void stableControlWinsWhenVehicleReplacesActiveControl() {
        Object stable = new Object();
        Object vehicleControl = new Object();

        assertSame(stable, ArcherControlPolicy.select(stable, vehicleControl));
    }

    @Test
    void stableControlAlsoWinsWhenItIsStillActive() {
        Object stable = new Object();

        assertSame(stable, ArcherControlPolicy.select(stable, stable));
    }

    @Test
    void missingStableControlFailsFast() {
        assertThrows(
                IllegalStateException.class,
                () -> ArcherControlPolicy.select(null, new Object())
        );
    }
}
