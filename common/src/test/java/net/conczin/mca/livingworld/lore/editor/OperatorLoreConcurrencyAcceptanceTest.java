package net.conczin.mca.livingworld.lore.editor;

import net.conczin.mca.livingworld.lore.OperatorLoreKey;
import net.conczin.mca.livingworld.lore.OperatorLoreScope;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

import static net.conczin.mca.livingworld.lore.editor.OperatorLoreEditorResult.Status.CONFLICT;
import static net.conczin.mca.livingworld.lore.editor.OperatorLoreEditorResult.Status.FORBIDDEN;
import static net.conczin.mca.livingworld.lore.editor.OperatorLoreEditorResult.Status.OK;
import static net.conczin.mca.livingworld.lore.editor.OperatorLoreEditorResult.Status.UNCHANGED;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperatorLoreConcurrencyAcceptanceTest {
    private static final OperatorLoreScope SCOPE = OperatorLoreScope.WORLD;
    private static final OperatorLoreKey KEY = OperatorLoreKey.world();
    private static final int ENTITY_ID = -1;

    @TempDir
    Path worldRoot;

    @Test
    void twoLogicalClientsRejectStaleWriterAndCompleteExplicitConflictRecovery() throws IOException {
        OperatorLoreEditorModel clientA = OperatorLoreEditorModel.open(ENTITY_ID);
        OperatorLoreEditorModel clientB = OperatorLoreEditorModel.open(ENTITY_ID);

        OperatorLoreEditorModel.LoadCommand loadA = clientA.beginLoad(SCOPE);
        OperatorLoreEditorModel.LoadCommand loadB = clientB.beginLoad(SCOPE);
        OperatorLoreEditorResult initialA = read(true);
        OperatorLoreEditorResult initialB = read(true);

        assertTrue(clientA.applyResponse(loadA.requestId(), initialA));
        assertTrue(clientB.applyResponse(loadB.requestId(), initialB));
        assertEquals(initialA.revision(), initialB.revision());

        clientA.edit("Client A canonical value");
        OperatorLoreEditorModel.SaveCommand saveA = clientA.beginSave().orElseThrow();
        OperatorLoreEditorResult acceptedA = write(
                true,
                saveA.revision(),
                saveA.value()
        );
        assertEquals(OK, acceptedA.status());
        assertTrue(clientA.applyResponse(saveA.requestId(), acceptedA));

        Path file = loreFile();
        byte[] afterFirstWrite = Files.readAllBytes(file);

        clientB.edit("Client B retained draft");
        OperatorLoreEditorModel.SaveCommand staleSaveB = clientB.beginSave().orElseThrow();
        OperatorLoreEditorResult conflict = write(
                true,
                staleSaveB.revision(),
                staleSaveB.value()
        );

        assertEquals(CONFLICT, conflict.status());
        assertEquals("Client A canonical value", conflict.value());
        assertEquals(acceptedA.revision(), conflict.revision());
        assertArrayEquals(afterFirstWrite, Files.readAllBytes(file));
        assertTrue(clientB.applyResponse(staleSaveB.requestId(), conflict));
        assertEquals(OperatorLoreEditorModel.State.CONFLICT, clientB.state());
        assertEquals("Client B retained draft", clientB.workingValue());

        clientB.keepDraft();
        assertEquals(OperatorLoreEditorModel.State.DIRTY, clientB.state());
        assertEquals(acceptedA.revision(), clientB.revision());

        OperatorLoreEditorModel.SaveCommand recoveredSaveB = clientB.beginSave().orElseThrow();
        OperatorLoreEditorResult acceptedB = write(
                true,
                recoveredSaveB.revision(),
                recoveredSaveB.value()
        );
        assertEquals(OK, acceptedB.status());
        assertNotEquals(acceptedA.revision(), acceptedB.revision());
        assertTrue(clientB.applyResponse(recoveredSaveB.requestId(), acceptedB));

        OperatorLoreEditorResult canonical = read(true);
        assertEquals("Client B retained draft", canonical.value());
        assertEquals(acceptedB.revision(), canonical.revision());
        assertEquals(OperatorLoreEditorModel.State.LOADED, clientB.state());
        assertFalse(clientB.isDirty());
    }

    @Test
    void exactReplayIsUnchangedAndDoesNotRewriteThePersistentFile() throws IOException {
        OperatorLoreEditorResult initial = read(true);
        OperatorLoreEditorResult first = write(true, initial.revision(), "Stable value");
        assertEquals(OK, first.status());

        Path file = loreFile();
        byte[] beforeReplay = Files.readAllBytes(file);
        FileTime fixedTime = FileTime.fromMillis(1_000L);
        Files.setLastModifiedTime(file, fixedTime);

        OperatorLoreEditorResult replay = write(true, first.revision(), "Stable value");

        assertEquals(UNCHANGED, replay.status());
        assertEquals(first.revision(), replay.revision());
        assertArrayEquals(beforeReplay, Files.readAllBytes(file));
        assertEquals(fixedTime, Files.getLastModifiedTime(file));
    }

    @Test
    void unauthorizedLogicalClientCannotReadDiscloseOrMutateCanonicalLore() throws IOException {
        OperatorLoreEditorResult deniedRead = read(false);
        OperatorLoreEditorResult deniedInitialWrite = write(
                false,
                OperatorLoreEditorPolicy.revision(""),
                "forbidden"
        );

        assertEquals(FORBIDDEN, deniedRead.status());
        assertEquals("", deniedRead.value());
        assertEquals("", deniedRead.revision());
        assertEquals(FORBIDDEN, deniedInitialWrite.status());
        assertFalse(Files.exists(loreFile()));

        OperatorLoreEditorResult initial = read(true);
        OperatorLoreEditorResult accepted = write(true, initial.revision(), "canonical");
        byte[] canonicalBytes = Files.readAllBytes(loreFile());

        OperatorLoreEditorResult deniedOverwrite = write(
                false,
                accepted.revision(),
                "forbidden overwrite"
        );

        assertEquals(FORBIDDEN, deniedOverwrite.status());
        assertArrayEquals(canonicalBytes, Files.readAllBytes(loreFile()));
        assertEquals("canonical", read(true).value());
    }

    @Test
    void clearUsesTheSameRevisionProtectedWritePath() {
        OperatorLoreEditorResult empty = read(true);
        assertEquals(OK, write(true, empty.revision(), "initial canonical").status());

        OperatorLoreEditorModel clientA = loadedClient();
        OperatorLoreEditorModel clientB = loadedClient();

        clientA.edit("new canonical");
        OperatorLoreEditorModel.SaveCommand saveA = clientA.beginSave().orElseThrow();
        OperatorLoreEditorResult acceptedA = write(true, saveA.revision(), saveA.value());
        assertTrue(clientA.applyResponse(saveA.requestId(), acceptedA));

        clientB.clear();
        OperatorLoreEditorModel.SaveCommand staleClear = clientB.beginSave().orElseThrow();
        assertEquals("", staleClear.value());
        OperatorLoreEditorResult conflict = write(true, staleClear.revision(), staleClear.value());
        assertEquals(CONFLICT, conflict.status());
        assertEquals("new canonical", conflict.value());
        assertTrue(clientB.applyResponse(staleClear.requestId(), conflict));

        clientB.keepDraft();
        OperatorLoreEditorModel.SaveCommand currentClear = clientB.beginSave().orElseThrow();
        assertEquals(acceptedA.revision(), currentClear.revision());
        OperatorLoreEditorResult cleared = write(true, currentClear.revision(), currentClear.value());

        assertEquals(OK, cleared.status());
        assertEquals("", cleared.value());
        assertTrue(clientB.applyResponse(currentClear.requestId(), cleared));
        assertEquals("", read(true).value());
    }

    private OperatorLoreEditorModel loadedClient() {
        OperatorLoreEditorModel client = OperatorLoreEditorModel.open(ENTITY_ID);
        OperatorLoreEditorModel.LoadCommand load = client.beginLoad(SCOPE);
        assertTrue(client.applyResponse(load.requestId(), read(true)));
        return client;
    }

    private OperatorLoreEditorResult read(boolean authorized) {
        return OperatorLoreResolvedAuthority.read(
                authorized,
                worldRoot,
                SCOPE,
                ENTITY_ID,
                KEY
        );
    }

    private OperatorLoreEditorResult write(
            boolean authorized,
            String expectedRevision,
            String requestedValue
    ) {
        return OperatorLoreResolvedAuthority.write(
                authorized,
                worldRoot,
                SCOPE,
                ENTITY_ID,
                KEY,
                expectedRevision,
                requestedValue
        );
    }

    private Path loreFile() {
        return worldRoot.resolve("livingworld/operator-lore.json");
    }
}
