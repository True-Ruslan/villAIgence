package net.conczin.mca.livingworld.lore.editor;

import net.conczin.mca.livingworld.lore.OperatorLoreScope;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static net.conczin.mca.livingworld.lore.editor.OperatorLoreEditorResult.Status.CONFLICT;
import static net.conczin.mca.livingworld.lore.editor.OperatorLoreEditorResult.Status.ERROR;
import static net.conczin.mca.livingworld.lore.editor.OperatorLoreEditorResult.Status.FORBIDDEN;
import static net.conczin.mca.livingworld.lore.editor.OperatorLoreEditorResult.Status.INVALID;
import static net.conczin.mca.livingworld.lore.editor.OperatorLoreEditorResult.Status.NOT_FOUND;
import static net.conczin.mca.livingworld.lore.editor.OperatorLoreEditorResult.Status.OK;
import static net.conczin.mca.livingworld.lore.editor.OperatorLoreEditorResult.Status.UNCHANGED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperatorLoreEditorModelTest {
    @Test
    void targetlessEditorStartsOnWorldAndDisablesTargetScopes() {
        OperatorLoreEditorModel model = OperatorLoreEditorModel.open(-1);

        assertEquals(OperatorLoreScope.WORLD, model.scope());
        assertEquals(OperatorLoreEditorModel.State.IDLE, model.state());
        assertTrue(model.isScopeAvailable(OperatorLoreScope.WORLD));
        assertTrue(model.isScopeAvailable(OperatorLoreScope.PLAYER));
        assertFalse(model.isScopeAvailable(OperatorLoreScope.VILLAGER));
        assertFalse(model.isScopeAvailable(OperatorLoreScope.VILLAGE));
    }

    @Test
    void targetedEditorStartsOnVillagerAndEnablesAllScopes() {
        OperatorLoreEditorModel model = OperatorLoreEditorModel.open(77);

        assertEquals(OperatorLoreScope.VILLAGER, model.scope());
        assertEquals(77, model.villagerEntityId());
        for (OperatorLoreScope scope : OperatorLoreScope.values()) {
            assertTrue(model.isScopeAvailable(scope));
        }
    }

    @Test
    void loadAdvancesRequestAndAdoptsCanonicalServerState() {
        OperatorLoreEditorModel model = OperatorLoreEditorModel.open(-1);

        OperatorLoreEditorModel.LoadCommand first = model.beginLoad(OperatorLoreScope.WORLD);
        assertEquals(1, first.requestId());
        assertEquals(OperatorLoreEditorModel.State.LOADING, model.state());

        OperatorLoreEditorModel.LoadCommand second = model.beginLoad(OperatorLoreScope.PLAYER);
        assertEquals(2, second.requestId());
        assertEquals(OperatorLoreScope.PLAYER, second.scope());

        String revision = OperatorLoreEditorPolicy.revision("Server lore\n");
        assertTrue(model.applyResponse(
                second.requestId(),
                new OperatorLoreEditorResult(OperatorLoreScope.PLAYER, -1, OK, "Server lore\r\n", revision)
        ));

        assertEquals(OperatorLoreEditorModel.State.LOADED, model.state());
        assertEquals("Server lore\n", model.serverValue());
        assertEquals("Server lore\n", model.workingValue());
        assertEquals(revision, model.revision());
        assertFalse(model.isDirty());
    }

    @Test
    void canonicalNewlinesDoNotCreateFalseDirtyState() {
        OperatorLoreEditorModel model = loaded("Line 1\nLine 2");

        model.edit("Line 1\r\nLine 2");

        assertFalse(model.isDirty());
        assertEquals(OperatorLoreEditorModel.State.LOADED, model.state());
        assertFalse(model.canSave());
    }

    @Test
    void editingAndClearingCreateExplicitDirtyState() {
        OperatorLoreEditorModel model = loaded("Existing");

        model.edit("Changed");
        assertTrue(model.isDirty());
        assertEquals(OperatorLoreEditorModel.State.DIRTY, model.state());
        assertTrue(model.canSave());

        model.clear();
        assertEquals("", model.workingValue());
        assertTrue(model.isDirty());
        assertTrue(model.canSave());
    }

    @Test
    void payloadLimitsUseCodePointsUtf8BytesAndControlCharacters() {
        OperatorLoreEditorModel model = loaded("");

        model.edit("a".repeat(OperatorLoreEditorPolicy.MAX_CODE_POINTS));
        assertEquals(OperatorLoreEditorPolicy.MAX_CODE_POINTS, model.codePointCount());
        assertEquals(OperatorLoreEditorPolicy.MAX_CODE_POINTS, model.utf8ByteCount());
        assertTrue(model.isPayloadValid());
        assertTrue(model.canSave());

        model.edit("a".repeat(OperatorLoreEditorPolicy.MAX_CODE_POINTS + 1));
        assertFalse(model.isPayloadValid());
        assertFalse(model.canSave());

        model.edit("🏰".repeat(OperatorLoreEditorPolicy.MAX_CODE_POINTS));
        assertEquals(OperatorLoreEditorPolicy.MAX_CODE_POINTS, model.codePointCount());
        assertTrue(model.utf8ByteCount() > OperatorLoreEditorPolicy.MAX_UTF8_BYTES);
        assertFalse(model.isPayloadValid());

        model.edit("valid\u0000invalid");
        assertFalse(model.isPayloadValid());
        assertFalse(model.canSave());
    }

    @Test
    void staleScopeEntityAndClosedResponsesAreIgnored() {
        OperatorLoreEditorModel model = OperatorLoreEditorModel.open(77);
        OperatorLoreEditorModel.LoadCommand current = model.beginLoad(OperatorLoreScope.VILLAGER);
        OperatorLoreEditorResult correct = result(OperatorLoreScope.VILLAGER, 77, OK, "server");

        assertFalse(model.applyResponse(current.requestId() - 1, correct));
        assertEquals(OperatorLoreEditorModel.State.LOADING, model.state());

        assertFalse(model.applyResponse(
                current.requestId(),
                result(OperatorLoreScope.VILLAGE, 77, OK, "wrong scope")
        ));
        assertFalse(model.applyResponse(
                current.requestId(),
                result(OperatorLoreScope.VILLAGER, 78, OK, "wrong entity")
        ));

        model.close();
        assertFalse(model.applyResponse(current.requestId(), correct));
        assertEquals(OperatorLoreEditorModel.State.CLOSED, model.state());
    }

    @Test
    void saveIsAvailableOnlyForValidDirtyContentAndEntersSaving() {
        OperatorLoreEditorModel model = loaded("old");
        assertEquals(Optional.empty(), model.beginSave());

        model.edit("new");
        OperatorLoreEditorModel.SaveCommand save = model.beginSave().orElseThrow();

        assertEquals(2, save.requestId());
        assertEquals(OperatorLoreScope.WORLD, save.scope());
        assertEquals(-1, save.villagerEntityId());
        assertEquals(OperatorLoreEditorPolicy.revision("old"), save.revision());
        assertEquals("new", save.value());
        assertEquals(OperatorLoreEditorModel.State.SAVING, model.state());
        assertFalse(model.canSave());
    }

    @Test
    void okAndUnchangedSaveResponsesReturnToCleanLoadedState() {
        OperatorLoreEditorModel okModel = loaded("old");
        okModel.edit("new");
        OperatorLoreEditorModel.SaveCommand okSave = okModel.beginSave().orElseThrow();
        String newRevision = OperatorLoreEditorPolicy.revision("new");

        assertTrue(okModel.applyResponse(
                okSave.requestId(),
                new OperatorLoreEditorResult(OperatorLoreScope.WORLD, -1, OK, "new", newRevision)
        ));
        assertEquals(OperatorLoreEditorModel.State.LOADED, okModel.state());
        assertEquals("new", okModel.serverValue());
        assertEquals("new", okModel.workingValue());
        assertEquals(newRevision, okModel.revision());
        assertFalse(okModel.isDirty());

        OperatorLoreEditorModel unchangedModel = loaded("same");
        unchangedModel.edit("same\r\n");
        unchangedModel.edit("different");
        OperatorLoreEditorModel.SaveCommand unchangedSave = unchangedModel.beginSave().orElseThrow();
        assertTrue(unchangedModel.applyResponse(
                unchangedSave.requestId(),
                result(OperatorLoreScope.WORLD, -1, UNCHANGED, "same")
        ));
        assertEquals(OperatorLoreEditorModel.State.LOADED, unchangedModel.state());
        assertFalse(unchangedModel.isDirty());
    }

    @Test
    void conflictPreservesDraftAndSupportsBothExplicitChoices() {
        OperatorLoreEditorModel useServer = loaded("old");
        useServer.edit("mine");
        OperatorLoreEditorModel.SaveCommand firstSave = useServer.beginSave().orElseThrow();
        OperatorLoreEditorResult conflict = result(OperatorLoreScope.WORLD, -1, CONFLICT, "theirs");

        assertTrue(useServer.applyResponse(firstSave.requestId(), conflict));
        assertEquals(OperatorLoreEditorModel.State.CONFLICT, useServer.state());
        assertEquals("mine", useServer.workingValue());
        assertEquals("theirs", useServer.conflictServerValue());
        assertEquals(OperatorLoreEditorPolicy.revision("theirs"), useServer.conflictRevision());

        useServer.useServerVersion();
        assertEquals(OperatorLoreEditorModel.State.LOADED, useServer.state());
        assertEquals("theirs", useServer.serverValue());
        assertEquals("theirs", useServer.workingValue());
        assertFalse(useServer.isDirty());

        OperatorLoreEditorModel keepDraft = loaded("old");
        keepDraft.edit("mine");
        OperatorLoreEditorModel.SaveCommand secondSave = keepDraft.beginSave().orElseThrow();
        assertTrue(keepDraft.applyResponse(secondSave.requestId(), conflict));

        keepDraft.keepDraft();
        assertEquals(OperatorLoreEditorModel.State.DIRTY, keepDraft.state());
        assertEquals("theirs", keepDraft.serverValue());
        assertEquals("mine", keepDraft.workingValue());
        assertEquals(OperatorLoreEditorPolicy.revision("theirs"), keepDraft.revision());
        assertTrue(keepDraft.canSave());
    }

    @Test
    void serverFailuresMapToExplicitStatesWithoutDiscardingDraft() {
        assertFailureState(FORBIDDEN, OperatorLoreEditorModel.State.FORBIDDEN);
        assertFailureState(NOT_FOUND, OperatorLoreEditorModel.State.NOT_FOUND);
        assertFailureState(INVALID, OperatorLoreEditorModel.State.INVALID);
        assertFailureState(ERROR, OperatorLoreEditorModel.State.ERROR);
    }

    private static void assertFailureState(
            OperatorLoreEditorResult.Status status,
            OperatorLoreEditorModel.State expectedState
    ) {
        OperatorLoreEditorModel model = loaded("old");
        model.edit("draft");
        OperatorLoreEditorModel.SaveCommand save = model.beginSave().orElseThrow();

        assertTrue(model.applyResponse(
                save.requestId(),
                result(OperatorLoreScope.WORLD, -1, status, "server")
        ));
        assertEquals(expectedState, model.state());
        assertEquals("draft", model.workingValue());
    }

    private static OperatorLoreEditorModel loaded(String value) {
        OperatorLoreEditorModel model = OperatorLoreEditorModel.open(-1);
        OperatorLoreEditorModel.LoadCommand load = model.beginLoad(OperatorLoreScope.WORLD);
        assertTrue(model.applyResponse(load.requestId(), result(OperatorLoreScope.WORLD, -1, OK, value)));
        return model;
    }

    private static OperatorLoreEditorResult result(
            OperatorLoreScope scope,
            int entityId,
            OperatorLoreEditorResult.Status status,
            String value
    ) {
        return new OperatorLoreEditorResult(
                scope,
                entityId,
                status,
                value,
                OperatorLoreEditorPolicy.revision(value)
        );
    }
}
