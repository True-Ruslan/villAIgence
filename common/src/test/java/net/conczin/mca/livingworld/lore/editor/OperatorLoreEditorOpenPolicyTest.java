package net.conczin.mca.livingworld.lore.editor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperatorLoreEditorOpenPolicyTest {
    @Test
    void globalOpeningRequiresOperatorPermission() {
        assertFalse(OperatorLoreEditorOpenPolicy.canOpenGlobal(false));
        assertTrue(OperatorLoreEditorOpenPolicy.canOpenGlobal(true));
    }

    @Test
    void targetedOpeningRequiresPermissionAndLiveSameLevelNearbyVillager() {
        assertFalse(OperatorLoreEditorOpenPolicy.canOpenTargeted(
                false,
                true,
                true,
                1.0
        ));
        assertFalse(OperatorLoreEditorOpenPolicy.canOpenTargeted(
                true,
                false,
                false,
                Double.POSITIVE_INFINITY
        ));
        assertFalse(OperatorLoreEditorOpenPolicy.canOpenTargeted(
                true,
                true,
                false,
                1.0
        ));
        assertFalse(OperatorLoreEditorOpenPolicy.canOpenTargeted(
                true,
                true,
                true,
                -1.0
        ));
        assertFalse(OperatorLoreEditorOpenPolicy.canOpenTargeted(
                true,
                true,
                true,
                Double.NaN
        ));
        assertFalse(OperatorLoreEditorOpenPolicy.canOpenTargeted(
                true,
                true,
                true,
                OperatorLoreTargetPolicy.MAX_DISTANCE_SQUARED + 1.0
        ));
        assertTrue(OperatorLoreEditorOpenPolicy.canOpenTargeted(
                true,
                true,
                true,
                OperatorLoreTargetPolicy.MAX_DISTANCE_SQUARED
        ));
    }
}
