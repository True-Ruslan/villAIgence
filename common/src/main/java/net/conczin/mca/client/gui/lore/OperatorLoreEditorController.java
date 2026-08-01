package net.conczin.mca.client.gui.lore;

import net.conczin.mca.livingworld.lore.OperatorLoreScope;
import net.conczin.mca.livingworld.lore.editor.OperatorLoreEditorModel;
import net.conczin.mca.network.Network;
import net.conczin.mca.network.c2s.OperatorLoreReadRequest;
import net.conczin.mca.network.c2s.OperatorLoreWriteRequest;
import net.conczin.mca.network.s2c.OperatorLoreResponse;

import java.util.Objects;

/** Thin client transport adapter around the pure editor model. */
public final class OperatorLoreEditorController {
    private final OperatorLoreEditorModel model;

    public OperatorLoreEditorController(OperatorLoreEditorModel model) {
        this.model = Objects.requireNonNull(model, "model");
    }

    public void load(OperatorLoreScope scope) {
        OperatorLoreEditorModel.LoadCommand command = model.beginLoad(scope);
        Network.sendToServer(new OperatorLoreReadRequest(
                command.requestId(),
                command.scope().name(),
                command.villagerEntityId()
        ));
    }

    public void save() {
        model.beginSave().ifPresent(command -> Network.sendToServer(new OperatorLoreWriteRequest(
                command.requestId(),
                command.scope().name(),
                command.villagerEntityId(),
                command.revision(),
                command.value()
        )));
    }

    public void accept(OperatorLoreResponse response) {
        if (response != null) {
            model.applyResponse(response.requestId(), response.toResult());
        }
    }

    public OperatorLoreEditorModel model() {
        return model;
    }
}
