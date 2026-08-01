package net.conczin.mca.livingworld.lore.editor;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/** Minimal client mailbox for the later S10c editor UI. */
public final class OperatorLoreClientState {
    private static final AtomicReference<OperatorLoreEditorResult> LATEST = new AtomicReference<>();

    private OperatorLoreClientState() {
    }

    public static void accept(OperatorLoreEditorResult result) {
        LATEST.set(result);
    }

    public static Optional<OperatorLoreEditorResult> latest() {
        return Optional.ofNullable(LATEST.get());
    }

    public static void clear() {
        LATEST.set(null);
    }
}
