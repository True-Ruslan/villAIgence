package net.conczin.mca.livingworld.lore.editor;

import java.util.Objects;
import java.util.UUID;

/** Loader-independent request correlation and response ownership boundary. */
final class OperatorLoreNetworkSessionCore {
    private OperatorLoreNetworkSessionCore() {
    }

    static Envelope execute(
            Session session,
            int requestId,
            AuthorityCall authority
    ) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(authority, "authority");

        OperatorLoreEditorResult result = Objects.requireNonNull(
                authority.invoke(),
                "authority result"
        );
        Envelope envelope = new Envelope(
                session.authenticatedPlayerId(),
                OperatorLoreProtocolPolicy.echo(requestId),
                result
        );
        session.responseSink().deliver(envelope);
        return envelope;
    }

    record Session(UUID authenticatedPlayerId, ResponseSink responseSink) {
        Session {
            Objects.requireNonNull(authenticatedPlayerId, "authenticatedPlayerId");
            Objects.requireNonNull(responseSink, "responseSink");
        }
    }

    record Envelope(
            UUID authenticatedPlayerId,
            int requestId,
            OperatorLoreEditorResult result
    ) {
        Envelope {
            Objects.requireNonNull(authenticatedPlayerId, "authenticatedPlayerId");
            Objects.requireNonNull(result, "result");
        }
    }

    @FunctionalInterface
    interface AuthorityCall {
        OperatorLoreEditorResult invoke();
    }

    @FunctionalInterface
    interface ResponseSink {
        void deliver(Envelope envelope);
    }
}
