package net.conczin.mca.livingworld.lore.editor;

import net.conczin.mca.livingworld.lore.OperatorLoreScope;

/** Canonical server result returned to a lore editor client. */
public record OperatorLoreEditorResult(
        OperatorLoreScope scope,
        int villagerEntityId,
        Status status,
        String value,
        String revision
) {
    public OperatorLoreEditorResult {
        scope = scope == null ? OperatorLoreScope.WORLD : scope;
        status = status == null ? Status.ERROR : status;
        value = value == null ? "" : value;
        revision = revision == null ? "" : revision;
    }

    public static OperatorLoreEditorResult denied(String scopeName, int entityId) {
        return new OperatorLoreEditorResult(
                parseScope(scopeName),
                entityId,
                Status.FORBIDDEN,
                "",
                ""
        );
    }

    public static OperatorLoreScope parseScope(String value) {
        if (value == null) {
            return OperatorLoreScope.WORLD;
        }
        try {
            return OperatorLoreScope.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException invalid) {
            return OperatorLoreScope.WORLD;
        }
    }

    public enum Status {
        OK,
        UNCHANGED,
        FORBIDDEN,
        CONFLICT,
        INVALID,
        NOT_FOUND,
        ERROR
    }
}
