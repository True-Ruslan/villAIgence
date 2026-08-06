package net.conczin.mca.livingworld.lore.editor;

import net.conczin.mca.livingworld.lore.OperatorLoreKey;
import net.conczin.mca.livingworld.lore.OperatorLoreScope;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static net.conczin.mca.livingworld.lore.editor.OperatorLoreEditorResult.Status.CONFLICT;
import static net.conczin.mca.livingworld.lore.editor.OperatorLoreEditorResult.Status.FORBIDDEN;
import static net.conczin.mca.livingworld.lore.editor.OperatorLoreEditorResult.Status.INVALID;
import static net.conczin.mca.livingworld.lore.editor.OperatorLoreEditorResult.Status.OK;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperatorLoreNetworkSessionAcceptanceTest {
    private static final UUID PLAYER_A = UUID.fromString("f293c40f-f012-48d2-a193-e6fdf781fdb3");
    private static final UUID PLAYER_B = UUID.fromString("952ee6e4-f328-43b8-829f-187d357b5655");
    private static final UUID INTRUDER = UUID.fromString("d7d89ca3-c515-4bc9-b326-c1d27de51a34");
    private static final OperatorLoreScope SCOPE = OperatorLoreScope.WORLD;
    private static final OperatorLoreKey KEY = OperatorLoreKey.world();
    private static final int ENTITY_ID = -1;

    @TempDir
    Path worldRoot;

    @Test
    void twoAuthenticatedSessionsReceiveOnlyTheirOwnConflictAndRetryResponses() throws IOException {
        List<OperatorLoreNetworkSessionCore.Envelope> inboxA = new ArrayList<>();
        List<OperatorLoreNetworkSessionCore.Envelope> inboxB = new ArrayList<>();
        OperatorLoreNetworkSessionCore.Session sessionA = session(PLAYER_A, inboxA);
        OperatorLoreNetworkSessionCore.Session sessionB = session(PLAYER_B, inboxB);
        OperatorLoreEditorModel clientA = OperatorLoreEditorModel.open(ENTITY_ID);
        OperatorLoreEditorModel clientB = OperatorLoreEditorModel.open(ENTITY_ID);

        OperatorLoreEditorModel.LoadCommand loadA = clientA.beginLoad(SCOPE);
        OperatorLoreEditorModel.LoadCommand loadB = clientB.beginLoad(SCOPE);
        apply(clientA, sendRead(sessionA, loadA.requestId(), true));
        apply(clientB, sendRead(sessionB, loadB.requestId(), true));
        assertEquals(clientA.revision(), clientB.revision());

        clientA.edit("Session A canonical value");
        OperatorLoreEditorModel.SaveCommand saveA = clientA.beginSave().orElseThrow();
        OperatorLoreNetworkSessionCore.Envelope acceptedA = sendWrite(
                sessionA,
                saveA.requestId(),
                true,
                saveA.revision(),
                saveA.value()
        );
        assertEquals(OK, acceptedA.result().status());
        apply(clientA, acceptedA);

        byte[] afterFirstWrite = Files.readAllBytes(loreFile());
        clientB.edit("Session B retained draft");
        OperatorLoreEditorModel.SaveCommand staleB = clientB.beginSave().orElseThrow();
        OperatorLoreNetworkSessionCore.Envelope conflictB = sendWrite(
                sessionB,
                staleB.requestId(),
                true,
                staleB.revision(),
                staleB.value()
        );

        assertEquals(CONFLICT, conflictB.result().status());
        assertEquals("Session A canonical value", conflictB.result().value());
        assertEquals(acceptedA.result().revision(), conflictB.result().revision());
        assertArrayEquals(afterFirstWrite, Files.readAllBytes(loreFile()));
        apply(clientB, conflictB);
        assertEquals(OperatorLoreEditorModel.State.CONFLICT, clientB.state());
        assertEquals("Session B retained draft", clientB.workingValue());

        clientB.keepDraft();
        OperatorLoreEditorModel.SaveCommand retryB = clientB.beginSave().orElseThrow();
        OperatorLoreNetworkSessionCore.Envelope acceptedB = sendWrite(
                sessionB,
                retryB.requestId(),
                true,
                retryB.revision(),
                retryB.value()
        );
        assertEquals(OK, acceptedB.result().status());
        apply(clientB, acceptedB);

        assertEquals("Session B retained draft", readCanonical().value());
        assertEquals(2, inboxA.size());
        assertEquals(3, inboxB.size());
        assertTrue(inboxA.stream().allMatch(value -> PLAYER_A.equals(value.authenticatedPlayerId())));
        assertTrue(inboxB.stream().allMatch(value -> PLAYER_B.equals(value.authenticatedPlayerId())));
        assertFalse(inboxA.stream().anyMatch(value -> PLAYER_B.equals(value.authenticatedPlayerId())));
        assertFalse(inboxB.stream().anyMatch(value -> PLAYER_A.equals(value.authenticatedPlayerId())));
    }

    @Test
    void authorityIsInvokedOnceAndRequestCorrelationIsPreserved() {
        List<OperatorLoreNetworkSessionCore.Envelope> inbox = new ArrayList<>();
        OperatorLoreNetworkSessionCore.Session session = session(PLAYER_A, inbox);
        AtomicInteger authorityCalls = new AtomicInteger();

        OperatorLoreNetworkSessionCore.Envelope envelope = OperatorLoreNetworkSessionCore.execute(
                session,
                77,
                () -> {
                    authorityCalls.incrementAndGet();
                    return OperatorLoreResolvedAuthority.read(
                            true,
                            worldRoot,
                            SCOPE,
                            ENTITY_ID,
                            KEY
                    );
                }
        );

        assertEquals(1, authorityCalls.get());
        assertEquals(77, envelope.requestId());
        assertEquals(PLAYER_A, envelope.authenticatedPlayerId());
        assertEquals(List.of(envelope), inbox);
    }

    @Test
    void unauthorizedAndInvalidRequestsDiscloseNothingAndCannotMutateCanonicalState() throws IOException {
        OperatorLoreEditorResult initial = readCanonical();
        OperatorLoreEditorResult seeded = OperatorLoreResolvedAuthority.write(
                true,
                worldRoot,
                SCOPE,
                ENTITY_ID,
                KEY,
                initial.revision(),
                "canonical"
        );
        byte[] before = Files.readAllBytes(loreFile());

        List<OperatorLoreNetworkSessionCore.Envelope> inbox = new ArrayList<>();
        OperatorLoreNetworkSessionCore.Session intruder = session(INTRUDER, inbox);
        OperatorLoreNetworkSessionCore.Envelope deniedRead = sendRead(intruder, 1, false);
        OperatorLoreNetworkSessionCore.Envelope deniedWrite = sendWrite(
                intruder,
                2,
                false,
                seeded.revision(),
                "forbidden overwrite"
        );
        OperatorLoreNetworkSessionCore.Envelope invalidScope = OperatorLoreNetworkSessionCore.execute(
                intruder,
                3,
                () -> OperatorLoreResolvedAuthority.result(
                        SCOPE,
                        ENTITY_ID,
                        INVALID,
                        ""
                )
        );

        assertEquals(FORBIDDEN, deniedRead.result().status());
        assertEquals("", deniedRead.result().value());
        assertEquals("", deniedRead.result().revision());
        assertEquals(FORBIDDEN, deniedWrite.result().status());
        assertEquals(INVALID, invalidScope.result().status());
        assertArrayEquals(before, Files.readAllBytes(loreFile()));
        assertEquals("canonical", readCanonical().value());
        assertEquals(List.of(deniedRead, deniedWrite, invalidScope), inbox);
    }

    private OperatorLoreNetworkSessionCore.Session session(
            UUID playerId,
            List<OperatorLoreNetworkSessionCore.Envelope> inbox
    ) {
        return new OperatorLoreNetworkSessionCore.Session(playerId, inbox::add);
    }

    private OperatorLoreNetworkSessionCore.Envelope sendRead(
            OperatorLoreNetworkSessionCore.Session session,
            int requestId,
            boolean authorized
    ) {
        return OperatorLoreNetworkSessionCore.execute(
                session,
                requestId,
                () -> OperatorLoreResolvedAuthority.read(
                        authorized,
                        worldRoot,
                        SCOPE,
                        ENTITY_ID,
                        KEY
                )
        );
    }

    private OperatorLoreNetworkSessionCore.Envelope sendWrite(
            OperatorLoreNetworkSessionCore.Session session,
            int requestId,
            boolean authorized,
            String expectedRevision,
            String value
    ) {
        return OperatorLoreNetworkSessionCore.execute(
                session,
                requestId,
                () -> OperatorLoreResolvedAuthority.write(
                        authorized,
                        worldRoot,
                        SCOPE,
                        ENTITY_ID,
                        KEY,
                        expectedRevision,
                        value
                )
        );
    }

    private void apply(
            OperatorLoreEditorModel client,
            OperatorLoreNetworkSessionCore.Envelope envelope
    ) {
        assertTrue(client.applyResponse(envelope.requestId(), envelope.result()));
    }

    private OperatorLoreEditorResult readCanonical() {
        return OperatorLoreResolvedAuthority.read(
                true,
                worldRoot,
                SCOPE,
                ENTITY_ID,
                KEY
        );
    }

    private Path loreFile() {
        return worldRoot.resolve("livingworld/operator-lore.json");
    }
}
