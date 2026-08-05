package net.conczin.mca.network;

import net.conczin.mca.livingworld.memory2.Memory2DialogueLifecycle;
import net.conczin.mca.livingworld.memory2.MemoryEvent;
import net.conczin.mca.livingworld.memory2.MemoryEventStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthenticatedTextTurnAcceptanceTest {
    private static final UUID PLAYER_ID = UUID.fromString("0f09be8d-f06c-48fb-b7d9-7aa6123f30e1");
    private static final UUID NPC_ID = UUID.fromString("45c3bc3d-faf5-472a-ae75-f35f8d256e70");

    @TempDir
    Path worldRoot;

    @Test
    void authenticatedServerResolvedTurnCommitsOneDialogueAndOneOwnedResponse() {
        AuthenticatedTextTurn.Session session = new AuthenticatedTextTurn.Session(PLAYER_ID, NPC_ID);
        AtomicInteger providerCalls = new AtomicInteger();
        List<Delivery> deliveries = new ArrayList<>();

        AuthenticatedTextTurn.Outcome outcome = AuthenticatedTextTurn.execute(
                session,
                "  Привет,   житель  ",
                (actualSession, normalizedMessage) -> {
                    providerCalls.incrementAndGet();
                    assertEquals(session, actualSession);
                    assertEquals("Привет, житель", normalizedMessage);
                    Optional<String> answer = Optional.of("Привет, игрок");
                    Memory2DialogueLifecycle.recordSuccessful(
                            true,
                            worldRoot,
                            actualSession.resolvedNpcId(),
                            actualSession.authenticatedPlayerId(),
                            42L,
                            normalizedMessage,
                            answer,
                            32,
                            1_725_000_000_000L
                    );
                    return answer;
                },
                (actualSession, response) -> deliveries.add(new Delivery(actualSession, response))
        );

        assertEquals(AuthenticatedTextTurn.Status.DELIVERED, outcome.status());
        assertEquals(session, outcome.session());
        assertEquals(1, providerCalls.get());
        assertEquals(List.of(new Delivery(session, "Привет, игрок")), deliveries);

        List<MemoryEvent> events = MemoryEventStore.forWorld(worldRoot).getRecent(NPC_ID, 10);
        assertEquals(1, events.size());
        MemoryEvent event = events.getFirst();
        assertEquals(NPC_ID, event.ownerNpcId());
        assertEquals(MemoryEvent.Type.DIALOGUE, event.type());
        assertEquals(List.of(PLAYER_ID), event.participants());
    }

    @Test
    void emptyProviderResultProducesNoDialogueAndNoResponse() {
        AuthenticatedTextTurn.Session session = new AuthenticatedTextTurn.Session(PLAYER_ID, NPC_ID);
        AtomicInteger providerCalls = new AtomicInteger();
        AtomicInteger deliveries = new AtomicInteger();

        AuthenticatedTextTurn.Outcome outcome = AuthenticatedTextTurn.execute(
                session,
                "Сообщение",
                (actualSession, normalizedMessage) -> {
                    providerCalls.incrementAndGet();
                    return Optional.empty();
                },
                (actualSession, response) -> deliveries.incrementAndGet()
        );

        assertEquals(AuthenticatedTextTurn.Status.NO_RESPONSE, outcome.status());
        assertEquals(1, providerCalls.get());
        assertEquals(0, deliveries.get());
        assertEquals(List.of(), MemoryEventStore.forWorld(worldRoot).getRecent(NPC_ID, 10));
    }

    @Test
    void missingAuthenticatedOrResolvedIdentityIsRejectedBeforeProviderInvocation() {
        assertThrows(
                NullPointerException.class,
                () -> new AuthenticatedTextTurn.Session(null, NPC_ID)
        );
        assertThrows(
                NullPointerException.class,
                () -> new AuthenticatedTextTurn.Session(PLAYER_ID, null)
        );
    }

    private record Delivery(AuthenticatedTextTurn.Session session, String response) {
    }
}
