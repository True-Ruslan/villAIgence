package net.conczin.mca.network;

import net.conczin.mca.MCA;
import net.conczin.mca.entity.VillagerEntityMCA;
import net.conczin.mca.entity.ai.chatAI.ChatAI;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Server-owned transport boundary for one authenticated text turn.
 *
 * <p>The public entrypoint accepts only the authenticated connection player and an NPC already
 * resolved from live server state. The deterministic core remains package-private so acceptance
 * tests can prove exactly-once provider and response ownership without exposing test-only APIs.</p>
 */
public final class AuthenticatedTextTurn {
    private AuthenticatedTextTurn() {
    }

    /**
     * Dispatches one text turn using the production ChatAI and conversation response path.
     * The caller must resolve {@code villager} from trusted server state before invoking this method.
     */
    public static void dispatch(
            ServerPlayer player,
            VillagerEntityMCA villager,
            String message
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(villager, "villager");

        Session session = new Session(player.getUUID(), villager.getUUID());
        String normalizedMessage = normalize(message);
        if (!isEligibleMessage(normalizedMessage)) return;

        CompletableFuture.runAsync(() -> execute(
                session,
                normalizedMessage,
                (ignored, text) -> ChatAI.answer(player, villager, text),
                (ignored, response) -> villager.conversationManager.addMessage(
                        player,
                        Component.literal(response)
                )
        )).exceptionally(failure -> {
            MCA.LOGGER.warn(
                    "Authenticated text turn failed for player {} and villager {}",
                    session.authenticatedPlayerId(),
                    session.resolvedNpcId(),
                    failure
            );
            return null;
        });
    }

    static Outcome execute(
            Session session,
            String message,
            AnswerProvider provider,
            ResponseSink responseSink
    ) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(provider, "provider");
        Objects.requireNonNull(responseSink, "responseSink");

        String normalizedMessage = normalize(message);
        if (!isEligibleMessage(normalizedMessage)) {
            return new Outcome(Status.REJECTED, session);
        }

        Optional<String> answer = provider.answer(session, normalizedMessage);
        if (answer == null || answer.isEmpty()) {
            return new Outcome(Status.NO_RESPONSE, session);
        }

        String response = answer.get();
        if (response == null || response.isBlank()) {
            return new Outcome(Status.NO_RESPONSE, session);
        }

        responseSink.deliver(session, response);
        return new Outcome(Status.DELIVERED, session);
    }

    private static String normalize(String value) {
        return StringUtils.normalizeSpace(value == null ? "" : value);
    }

    private static boolean isEligibleMessage(String value) {
        return !value.isBlank() && !value.startsWith("/");
    }

    record Session(UUID authenticatedPlayerId, UUID resolvedNpcId) {
        Session {
            Objects.requireNonNull(authenticatedPlayerId, "authenticatedPlayerId");
            Objects.requireNonNull(resolvedNpcId, "resolvedNpcId");
        }
    }

    record Outcome(Status status, Session session) {
        Outcome {
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(session, "session");
        }
    }

    enum Status {
        DELIVERED,
        NO_RESPONSE,
        REJECTED
    }

    @FunctionalInterface
    interface AnswerProvider {
        Optional<String> answer(Session session, String normalizedMessage);
    }

    @FunctionalInterface
    interface ResponseSink {
        void deliver(Session session, String response);
    }
}
