package net.conczin.mca.client.gui.lore;

import net.conczin.mca.network.s2c.OperatorLoreResponse;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.network.chat.Component;

import java.util.Objects;
import java.util.function.Consumer;

/** Confirmation modal that keeps forwarding in-flight lore responses to its owning editor. */
public final class OperatorLoreConfirmScreen extends ConfirmScreen implements OperatorLoreResponseReceiver {
    private final OperatorLoreEditorScreen owner;

    public OperatorLoreConfirmScreen(
            OperatorLoreEditorScreen owner,
            Consumer<Boolean> callback,
            Component title,
            Component message
    ) {
        super(confirmed -> callback.accept(confirmed), title, message);
        this.owner = Objects.requireNonNull(owner, "owner");
    }

    @Override
    public void accept(OperatorLoreResponse response) {
        owner.accept(response);
    }
}
