package net.conczin.mca.livingworld.lore.editor;

import net.conczin.mca.livingworld.lore.OperatorLoreScope;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;

/**
 * Pure client editor state. It owns no networking, rendering, persistence or authority decisions.
 */
public final class OperatorLoreEditorModel {
    private final int villagerEntityId;

    private OperatorLoreScope scope;
    private State state = State.IDLE;
    private int requestGeneration;
    private String serverValue = "";
    private String workingValue = "";
    private String revision = "";
    private String conflictServerValue = "";
    private String conflictRevision = "";

    private OperatorLoreEditorModel(int villagerEntityId) {
        this.villagerEntityId = villagerEntityId;
        scope = villagerEntityId >= 0 ? OperatorLoreScope.VILLAGER : OperatorLoreScope.WORLD;
    }

    public static OperatorLoreEditorModel open(int villagerEntityId) {
        return new OperatorLoreEditorModel(villagerEntityId);
    }

    public LoadCommand beginLoad(OperatorLoreScope requestedScope) {
        ensureOpen();
        Objects.requireNonNull(requestedScope, "requestedScope");
        if (!isScopeAvailable(requestedScope)) {
            throw new IllegalArgumentException("Unavailable operator lore scope: " + requestedScope);
        }

        scope = requestedScope;
        requestGeneration = OperatorLoreProtocolPolicy.nextRequestId(requestGeneration);
        state = State.LOADING;
        clearConflict();
        return new LoadCommand(requestGeneration, scope, requestEntityId());
    }

    public Optional<SaveCommand> beginSave() {
        if (!canSave()) {
            return Optional.empty();
        }

        requestGeneration = OperatorLoreProtocolPolicy.nextRequestId(requestGeneration);
        state = State.SAVING;
        return Optional.of(new SaveCommand(
                requestGeneration,
                scope,
                requestEntityId(),
                revision,
                OperatorLoreEditorPolicy.canonicalize(workingValue)
        ));
    }

    public void edit(String value) {
        if (state == State.CLOSED
                || state == State.LOADING
                || state == State.SAVING
                || state == State.FORBIDDEN
                || state == State.NOT_FOUND) {
            return;
        }

        workingValue = value == null ? "" : value;
        if (state != State.CONFLICT) {
            state = isDirty() ? State.DIRTY : State.LOADED;
        }
    }

    public void clear() {
        edit("");
    }

    public boolean applyResponse(int requestId, OperatorLoreEditorResult result) {
        if (state == State.CLOSED
                || result == null
                || !OperatorLoreProtocolPolicy.matches(requestGeneration, requestId)
                || result.scope() != scope
                || result.villagerEntityId() != expectedResponseEntityId()) {
            return false;
        }

        switch (result.status()) {
            case OK, UNCHANGED -> adoptCanonicalServerState(result);
            case CONFLICT -> {
                conflictServerValue = OperatorLoreEditorPolicy.canonicalize(result.value());
                conflictRevision = result.revision();
                state = State.CONFLICT;
            }
            case FORBIDDEN -> state = State.FORBIDDEN;
            case NOT_FOUND -> state = State.NOT_FOUND;
            case INVALID -> state = State.INVALID;
            case ERROR -> state = State.ERROR;
        }
        return true;
    }

    public void useServerVersion() {
        if (state != State.CONFLICT) {
            return;
        }

        serverValue = conflictServerValue;
        workingValue = conflictServerValue;
        revision = conflictRevision;
        clearConflict();
        state = State.LOADED;
    }

    public void keepDraft() {
        if (state != State.CONFLICT) {
            return;
        }

        serverValue = conflictServerValue;
        revision = conflictRevision;
        clearConflict();
        state = isDirty() ? State.DIRTY : State.LOADED;
    }

    public void close() {
        state = State.CLOSED;
        requestGeneration = OperatorLoreProtocolPolicy.nextRequestId(requestGeneration);
    }

    public boolean isScopeAvailable(OperatorLoreScope candidate) {
        if (candidate == null) {
            return false;
        }
        return candidate == OperatorLoreScope.WORLD
                || candidate == OperatorLoreScope.PLAYER
                || villagerEntityId >= 0;
    }

    public boolean isDirty() {
        return !OperatorLoreEditorPolicy.canonicalize(workingValue)
                .equals(OperatorLoreEditorPolicy.canonicalize(serverValue));
    }

    public boolean isPayloadValid() {
        return OperatorLoreEditorPolicy.isValidPayload(workingValue);
    }

    public boolean canSave() {
        return state == State.DIRTY && isDirty() && isPayloadValid();
    }

    public int codePointCount() {
        return workingValue.codePointCount(0, workingValue.length());
    }

    public int utf8ByteCount() {
        return workingValue.getBytes(StandardCharsets.UTF_8).length;
    }

    public State state() {
        return state;
    }

    public OperatorLoreScope scope() {
        return scope;
    }

    public int villagerEntityId() {
        return villagerEntityId;
    }

    public String serverValue() {
        return serverValue;
    }

    public String workingValue() {
        return workingValue;
    }

    public String revision() {
        return revision;
    }

    public String conflictServerValue() {
        return conflictServerValue;
    }

    public String conflictRevision() {
        return conflictRevision;
    }

    public int requestGeneration() {
        return requestGeneration;
    }

    private void adoptCanonicalServerState(OperatorLoreEditorResult result) {
        String canonical = OperatorLoreEditorPolicy.canonicalize(result.value());
        serverValue = canonical;
        workingValue = canonical;
        revision = result.revision();
        clearConflict();
        state = State.LOADED;
    }

    private int requestEntityId() {
        return scope == OperatorLoreScope.VILLAGER || scope == OperatorLoreScope.VILLAGE
                ? villagerEntityId
                : -1;
    }

    private int expectedResponseEntityId() {
        return requestEntityId();
    }

    private void clearConflict() {
        conflictServerValue = "";
        conflictRevision = "";
    }

    private void ensureOpen() {
        if (state == State.CLOSED) {
            throw new IllegalStateException("Operator lore editor is closed");
        }
    }

    public enum State {
        IDLE,
        LOADING,
        LOADED,
        DIRTY,
        SAVING,
        CONFLICT,
        FORBIDDEN,
        NOT_FOUND,
        INVALID,
        ERROR,
        CLOSED
    }

    public record LoadCommand(
            int requestId,
            OperatorLoreScope scope,
            int villagerEntityId
    ) {
    }

    public record SaveCommand(
            int requestId,
            OperatorLoreScope scope,
            int villagerEntityId,
            String revision,
            String value
    ) {
    }
}
