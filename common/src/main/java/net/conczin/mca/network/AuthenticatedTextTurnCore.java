package net.conczin.mca.network;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Loader-independent exactly-once core for one authenticated text turn. */
final class AuthenticatedTextTurnCore {
    private AuthenticatedTextTurnCore() {
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

    static String normalize(String value) {
        if (value == null || value.isBlank()) return "";
        return value.strip().replaceAll("\\s+", " ");
    }

    static boolean isEligibleMessage(String value) {
        return value != null && !value.isBlank() && !value.startsWith("/");
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
