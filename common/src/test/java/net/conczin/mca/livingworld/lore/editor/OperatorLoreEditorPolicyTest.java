package net.conczin.mca.livingworld.lore.editor;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperatorLoreEditorPolicyTest {
    @Test
    void nonOperatorCannotReadOrWrite() {
        assertFalse(OperatorLoreEditorPolicy.canAccess(false, OperatorLoreEditorPolicy.Operation.READ));
        assertFalse(OperatorLoreEditorPolicy.canAccess(false, OperatorLoreEditorPolicy.Operation.WRITE));
        assertTrue(OperatorLoreEditorPolicy.canAccess(true, OperatorLoreEditorPolicy.Operation.READ));
        assertTrue(OperatorLoreEditorPolicy.canAccess(true, OperatorLoreEditorPolicy.Operation.WRITE));
    }

    @Test
    void currentRevisionMustMatchBeforeMutation() {
        String current = "Existing lore";
        String revision = OperatorLoreEditorPolicy.revision(current);

        assertEquals(
                OperatorLoreEditorPolicy.Decision.APPLY,
                OperatorLoreEditorPolicy.decideWrite(true, revision, current, "Updated lore")
        );
        assertEquals(
                OperatorLoreEditorPolicy.Decision.CONFLICT,
                OperatorLoreEditorPolicy.decideWrite(true, "stale", current, "Updated lore")
        );
        assertEquals(
                OperatorLoreEditorPolicy.Decision.FORBIDDEN,
                OperatorLoreEditorPolicy.decideWrite(false, revision, current, "Updated lore")
        );
    }

    @Test
    void exactReplayIsARecognizedNoOp() {
        String current = "Stable lore";
        assertEquals(
                OperatorLoreEditorPolicy.Decision.UNCHANGED,
                OperatorLoreEditorPolicy.decideWrite(
                        true,
                        OperatorLoreEditorPolicy.revision(current),
                        current,
                        current
                )
        );
    }

    @Test
    void revisionIsDeterministicAndContentSensitive() {
        assertEquals(
                OperatorLoreEditorPolicy.revision("same"),
                OperatorLoreEditorPolicy.revision("same")
        );
        assertNotEquals(
                OperatorLoreEditorPolicy.revision("same"),
                OperatorLoreEditorPolicy.revision("different")
        );
        assertEquals(64, OperatorLoreEditorPolicy.revision("").length());
    }

    @Test
    void networkPayloadIsBoundedByCodePointsAndUtf8Bytes() {
        String maxAscii = "a".repeat(OperatorLoreEditorPolicy.MAX_CODE_POINTS);
        String tooManyCodePoints = maxAscii + "a";
        String maxEmoji = "🏰".repeat(OperatorLoreEditorPolicy.MAX_CODE_POINTS);

        assertTrue(OperatorLoreEditorPolicy.isValidPayload(maxAscii));
        assertFalse(OperatorLoreEditorPolicy.isValidPayload(tooManyCodePoints));
        assertTrue(maxEmoji.getBytes(StandardCharsets.UTF_8).length > OperatorLoreEditorPolicy.MAX_UTF8_BYTES);
        assertFalse(OperatorLoreEditorPolicy.isValidPayload(maxEmoji));
    }

    @Test
    void invalidPayloadIsRejectedBeforeRevisionComparison() {
        String current = "Current";
        String invalid = "🏰".repeat(OperatorLoreEditorPolicy.MAX_CODE_POINTS);

        assertEquals(
                OperatorLoreEditorPolicy.Decision.INVALID,
                OperatorLoreEditorPolicy.decideWrite(
                        true,
                        OperatorLoreEditorPolicy.revision(current),
                        current,
                        invalid
                )
        );
    }

    @Test
    void oversizedStoredLoreProducesSafeEmptyViewWithOriginalRevision() {
        String oversized = "🏰".repeat(OperatorLoreEditorPolicy.MAX_CODE_POINTS);

        OperatorLoreEditorPolicy.NetworkView view = OperatorLoreEditorPolicy.networkView(oversized);

        assertFalse(view.representable());
        assertEquals("", view.value());
        assertEquals(OperatorLoreEditorPolicy.revision(oversized), view.revision());
    }

    @Test
    void normalStoredLoreRemainsCanonicalInNetworkView() {
        OperatorLoreEditorPolicy.NetworkView view = OperatorLoreEditorPolicy.networkView("Line 1\r\nLine 2");

        assertTrue(view.representable());
        assertEquals("Line 1\nLine 2", view.value());
        assertEquals(OperatorLoreEditorPolicy.revision("Line 1\nLine 2"), view.revision());
    }
}
