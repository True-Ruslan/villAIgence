package net.conczin.mca.client.gui.lore;

import net.conczin.mca.livingworld.lore.OperatorLoreScope;
import net.conczin.mca.livingworld.lore.editor.OperatorLoreEditorModel;
import net.conczin.mca.network.s2c.OperatorLoreResponse;
import net.conczin.mca.util.compat.ButtonWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** Dedicated presentation layer for the server-authoritative operator lore API. */
public final class OperatorLoreEditorScreen extends Screen {
    private static final int PANEL_COLOR = 0xE0101010;
    private static final int TEXT_COLOR = 0xFFFFFF;
    private static final int MUTED_COLOR = 0xB0B0B0;
    private static final int INVALID_COLOR = 0xFF6B6B;
    private static final int UI_CHARACTER_LIMIT = 16_384;

    private final OperatorLoreEditorModel model;
    private final OperatorLoreEditorController controller;
    private final Map<OperatorLoreScope, ButtonWidget> scopeButtons = new EnumMap<>(OperatorLoreScope.class);

    private MultiLineEditBox editor;
    private ButtonWidget reloadButton;
    private ButtonWidget clearButton;
    private ButtonWidget saveButton;
    private ButtonWidget closeButton;
    private ButtonWidget useServerButton;
    private ButtonWidget keepDraftButton;
    private ButtonWidget conflictCloseButton;

    private int panelLeft;
    private int panelTop;
    private int panelWidth;
    private int panelHeight;
    private int textBottom;
    private boolean syncingEditor;

    public OperatorLoreEditorScreen(OperatorLoreEditorOpenContext context) {
        super(Component.translatable("gui.operator_lore.title"));
        Objects.requireNonNull(context, "context");
        model = OperatorLoreEditorModel.open(context.villagerEntityId());
        controller = new OperatorLoreEditorController(model);
    }

    @Override
    protected void init() {
        boolean restoreFocus = editor != null && editor.isFocused();
        scopeButtons.clear();
        calculateLayout();
        addScopeButtons();
        addEditor(restoreFocus);
        addFooterButtons();

        if (model.state() == OperatorLoreEditorModel.State.IDLE) {
            controller.load(model.scope());
        }
        refreshControls();
    }

    private void calculateLayout() {
        int margin = 16;
        panelWidth = width < 352
                ? Math.max(200, width - 8)
                : Math.max(320, Math.min(720, width - margin * 2));
        panelHeight = Math.max(220, height - margin * 2);
        if (panelHeight > height - 8) {
            panelHeight = Math.max(180, height - 8);
        }
        panelLeft = width < 352 ? 4 : (width - panelWidth) / 2;
        panelTop = Math.max(4, (height - panelHeight) / 2);
        textBottom = panelTop + panelHeight - 70;
    }

    private void addScopeButtons() {
        int contentWidth = panelWidth - 24;
        int gap = 4;
        int buttonWidth = Math.max(44, (contentWidth - gap * 3) / 4);
        int x = panelLeft + 12;
        int y = panelTop + 24;

        for (OperatorLoreScope scope : OperatorLoreScope.values()) {
            Component label = Component.translatable("gui.operator_lore.scope." + scope.name().toLowerCase());
            Component tooltip = Component.translatable("gui.operator_lore.disabled.no_target");
            ButtonWidget button = addRenderableWidget(new ButtonWidget(
                    x,
                    y,
                    buttonWidth,
                    20,
                    label,
                    ignored -> requestScopeChange(scope),
                    tooltip
            ));
            scopeButtons.put(scope, button);
            x += buttonWidth + gap;
        }
    }

    private void addEditor(boolean restoreFocus) {
        int textX = panelLeft + 12;
        int textTop = panelTop + 70;
        int textWidth = panelWidth - 24;
        int textHeight = Math.max(72, textBottom - textTop);

        editor = addRenderableWidget(new MultiLineEditBox(
                font,
                textX,
                textTop,
                textWidth,
                textHeight,
                Component.translatable("gui.operator_lore.editor.placeholder"),
                Component.translatable("gui.operator_lore.editor.narration")
        ));
        editor.setCharacterLimit(UI_CHARACTER_LIMIT);
        editor.setValue(model.workingValue());
        editor.setValueListener(value -> {
            if (!syncingEditor) {
                model.edit(value);
                refreshControls();
            }
        });
        if (restoreFocus) {
            editor.setFocused(true);
        }
    }

    private void addFooterButtons() {
        int gap = 4;
        int y = panelTop + panelHeight - 26;
        int contentWidth = panelWidth - 24;
        int normalWidth = Math.max(48, (contentWidth - gap * 3) / 4);
        int x = panelLeft + 12;

        reloadButton = addRenderableWidget(new ButtonWidget(
                x,
                y,
                normalWidth,
                20,
                Component.translatable("gui.operator_lore.button.reload"),
                ignored -> requestReload()
        ));
        x += normalWidth + gap;
        clearButton = addRenderableWidget(new ButtonWidget(
                x,
                y,
                normalWidth,
                20,
                Component.translatable("gui.operator_lore.button.clear"),
                ignored -> {
                    model.clear();
                    syncEditorFromModel();
                    refreshControls();
                }
        ));
        x += normalWidth + gap;
        saveButton = addRenderableWidget(new ButtonWidget(
                x,
                y,
                normalWidth,
                20,
                Component.translatable("gui.operator_lore.button.save"),
                ignored -> {
                    controller.save();
                    refreshControls();
                }
        ));
        x += normalWidth + gap;
        closeButton = addRenderableWidget(new ButtonWidget(
                x,
                y,
                normalWidth,
                20,
                Component.translatable("gui.operator_lore.button.close"),
                ignored -> onClose()
        ));

        int conflictWidth = Math.max(64, (contentWidth - gap * 2) / 3);
        x = panelLeft + 12;
        useServerButton = addRenderableWidget(new ButtonWidget(
                x,
                y,
                conflictWidth,
                20,
                Component.translatable("gui.operator_lore.button.use_server"),
                ignored -> {
                    model.useServerVersion();
                    syncEditorFromModel();
                    refreshControls();
                }
        ));
        x += conflictWidth + gap;
        keepDraftButton = addRenderableWidget(new ButtonWidget(
                x,
                y,
                conflictWidth,
                20,
                Component.translatable("gui.operator_lore.button.keep_draft"),
                ignored -> {
                    model.keepDraft();
                    syncEditorFromModel();
                    refreshControls();
                }
        ));
        x += conflictWidth + gap;
        conflictCloseButton = addRenderableWidget(new ButtonWidget(
                x,
                y,
                conflictWidth,
                20,
                Component.translatable("gui.operator_lore.button.close"),
                ignored -> onClose()
        ));
    }

    private void requestScopeChange(OperatorLoreScope requestedScope) {
        if (requestedScope == model.scope() || !model.isScopeAvailable(requestedScope)) {
            return;
        }
        if (model.isDirty()) {
            showDiscardConfirmation(
                    Component.translatable("gui.operator_lore.discard_scope_message"),
                    () -> beginLoad(requestedScope)
            );
        } else {
            beginLoad(requestedScope);
        }
    }

    private void requestReload() {
        if (model.isDirty()) {
            showDiscardConfirmation(
                    Component.translatable("gui.operator_lore.discard_reload_message"),
                    () -> beginLoad(model.scope())
            );
        } else {
            beginLoad(model.scope());
        }
    }

    private void beginLoad(OperatorLoreScope scope) {
        controller.load(scope);
        refreshControls();
    }

    private void showDiscardConfirmation(Component message, Runnable confirmedAction) {
        Objects.requireNonNull(minecraft).setScreen(new ConfirmScreen(
                confirmed -> {
                    Objects.requireNonNull(minecraft).setScreen(this);
                    if (confirmed) {
                        confirmedAction.run();
                    }
                },
                Component.translatable("gui.operator_lore.discard_title"),
                message
        ));
    }

    @Override
    public void onClose() {
        if (model.state() == OperatorLoreEditorModel.State.SAVING) {
            showCloseConfirmation(Component.translatable("gui.operator_lore.saving_close_message"));
            return;
        }
        if (model.isDirty()) {
            showCloseConfirmation(Component.translatable("gui.operator_lore.discard_close_message"));
            return;
        }
        closeNow();
    }

    private void showCloseConfirmation(Component message) {
        Objects.requireNonNull(minecraft).setScreen(new ConfirmScreen(
                confirmed -> {
                    if (confirmed) {
                        closeNow();
                    } else {
                        Objects.requireNonNull(minecraft).setScreen(this);
                    }
                },
                Component.translatable("gui.operator_lore.discard_title"),
                message
        ));
    }

    private void closeNow() {
        model.close();
        Objects.requireNonNull(minecraft).setScreen(null);
    }

    public void accept(OperatorLoreResponse response) {
        OperatorLoreEditorModel.State before = model.state();
        controller.accept(response);
        if (model.state() == OperatorLoreEditorModel.State.LOADED
                && before != OperatorLoreEditorModel.State.LOADED) {
            syncEditorFromModel();
        }
        refreshControls();
    }

    private void syncEditorFromModel() {
        if (editor == null || editor.getValue().equals(model.workingValue())) {
            return;
        }
        syncingEditor = true;
        editor.setValue(model.workingValue());
        syncingEditor = false;
    }

    private void refreshControls() {
        if (editor == null) {
            return;
        }

        OperatorLoreEditorModel.State state = model.state();
        boolean busy = state == OperatorLoreEditorModel.State.LOADING
                || state == OperatorLoreEditorModel.State.SAVING
                || state == OperatorLoreEditorModel.State.CLOSED;
        boolean conflict = state == OperatorLoreEditorModel.State.CONFLICT;

        for (Map.Entry<OperatorLoreScope, ButtonWidget> entry : scopeButtons.entrySet()) {
            entry.getValue().active = !busy && model.isScopeAvailable(entry.getKey());
        }

        editor.active = switch (state) {
            case LOADED, DIRTY, INVALID, ERROR, CONFLICT -> true;
            default -> false;
        };
        reloadButton.active = !busy;
        clearButton.active = !busy
                && state != OperatorLoreEditorModel.State.FORBIDDEN
                && state != OperatorLoreEditorModel.State.NOT_FOUND;
        saveButton.active = model.canSave();
        closeButton.active = state != OperatorLoreEditorModel.State.CLOSED;
        conflictCloseButton.active = state != OperatorLoreEditorModel.State.CLOSED;

        reloadButton.visible = !conflict;
        clearButton.visible = !conflict;
        saveButton.visible = !conflict;
        closeButton.visible = !conflict;
        useServerButton.visible = conflict;
        keepDraftButton.visible = conflict;
        conflictCloseButton.visible = conflict;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (hasControlDown() && keyCode == GLFW.GLFW_KEY_S && model.canSave()) {
            controller.save();
            refreshControls();
            return true;
        }
        if (hasControlDown() && keyCode == GLFW.GLFW_KEY_R) {
            requestReload();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(
                panelLeft,
                panelTop,
                panelLeft + panelWidth,
                panelTop + panelHeight,
                PANEL_COLOR
        );
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.drawCenteredString(
                font,
                Component.translatable("gui.operator_lore.title"),
                width / 2,
                panelTop + 8,
                TEXT_COLOR
        );
        graphics.drawString(
                font,
                scopeLabel(),
                panelLeft + 12,
                panelTop + 49,
                TEXT_COLOR
        );
        graphics.drawString(
                font,
                targetLabel(),
                panelLeft + 12,
                panelTop + 59,
                MUTED_COLOR
        );

        int counterColor = model.isPayloadValid() ? MUTED_COLOR : INVALID_COLOR;
        graphics.drawString(
                font,
                Component.translatable("gui.operator_lore.counter.code_points", model.codePointCount()),
                panelLeft + 12,
                textBottom + 5,
                counterColor
        );
        graphics.drawString(
                font,
                Component.translatable("gui.operator_lore.counter.utf8", model.utf8ByteCount()),
                panelLeft + Math.max(12, panelWidth / 2),
                textBottom + 5,
                counterColor
        );
        graphics.drawString(
                font,
                statusLabel(),
                panelLeft + 12,
                textBottom + 17,
                statusColor()
        );
    }

    private Component scopeLabel() {
        return Component.translatable(
                "gui.operator_lore.scope_label",
                Component.translatable("gui.operator_lore.scope." + model.scope().name().toLowerCase())
        );
    }

    private Component targetLabel() {
        return Component.translatable("gui.operator_lore.target." + model.scope().name().toLowerCase());
    }

    private Component statusLabel() {
        return Component.translatable("gui.operator_lore.status." + model.state().name().toLowerCase());
    }

    private int statusColor() {
        return switch (model.state()) {
            case FORBIDDEN, NOT_FOUND, INVALID, ERROR, CONFLICT -> INVALID_COLOR;
            default -> MUTED_COLOR;
        };
    }

    public OperatorLoreEditorModel model() {
        return model;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
