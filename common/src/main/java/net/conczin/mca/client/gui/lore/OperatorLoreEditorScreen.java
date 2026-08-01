package net.conczin.mca.client.gui.lore;

import net.conczin.mca.livingworld.lore.editor.OperatorLoreEditorModel;
import net.conczin.mca.network.s2c.OperatorLoreResponse;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Compile-safe shell; Task 5 adds the complete responsive editor UI. */
public final class OperatorLoreEditorScreen extends Screen {
    private final OperatorLoreEditorModel model;
    private final OperatorLoreEditorController controller;

    public OperatorLoreEditorScreen(OperatorLoreEditorOpenContext context) {
        super(Component.translatable("gui.operator_lore.title"));
        model = OperatorLoreEditorModel.open(context.villagerEntityId());
        controller = new OperatorLoreEditorController(model);
    }

    public void accept(OperatorLoreResponse response) {
        controller.accept(response);
    }

    public OperatorLoreEditorModel model() {
        return model;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
