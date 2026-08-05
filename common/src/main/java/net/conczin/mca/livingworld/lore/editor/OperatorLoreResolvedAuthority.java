package net.conczin.mca.livingworld.lore.editor;

import net.conczin.mca.livingworld.lore.OperatorLoreKey;
import net.conczin.mca.livingworld.lore.OperatorLoreScope;
import net.conczin.mca.livingworld.lore.WorldOperatorLoreStore;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Applies the canonical Operator Lore permission, revision and persistence contract after the
 * Minecraft server has resolved the target identity from trusted state.
 *
 * <p>The seam is deliberately package-private. It owns no packet fields, client identity or target
 * lookup. Each operation reloads the world-local store so sequential server requests observe the
 * latest canonical revision.</p>
 */
final class OperatorLoreResolvedAuthority {
    private OperatorLoreResolvedAuthority() {
    }

    static OperatorLoreEditorResult read(
            boolean hasOperatorPermission,
            Path worldRoot,
            OperatorLoreScope scope,
            int canonicalEntityId,
            OperatorLoreKey key
    ) {
        if (!hasOperatorPermission) {
            return denied(scope, canonicalEntityId);
        }

        WorldOperatorLoreStore store = store(worldRoot, scope, key);
        return result(
                scope,
                canonicalEntityId,
                OperatorLoreEditorResult.Status.OK,
                store.get(key)
        );
    }

    static OperatorLoreEditorResult write(
            boolean hasOperatorPermission,
            Path worldRoot,
            OperatorLoreScope scope,
            int canonicalEntityId,
            OperatorLoreKey key,
            String expectedRevision,
            String requestedValue
    ) {
        if (!hasOperatorPermission) {
            return denied(scope, canonicalEntityId);
        }

        WorldOperatorLoreStore store = store(worldRoot, scope, key);
        String current = store.get(key);
        OperatorLoreEditorPolicy.Decision decision = OperatorLoreEditorPolicy.decideWrite(
                true,
                expectedRevision,
                current,
                requestedValue
        );

        return switch (decision) {
            case APPLY -> {
                store.put(key, OperatorLoreEditorPolicy.canonicalize(requestedValue));
                yield result(
                        scope,
                        canonicalEntityId,
                        OperatorLoreEditorResult.Status.OK,
                        store.get(key)
                );
            }
            case UNCHANGED -> result(
                    scope,
                    canonicalEntityId,
                    OperatorLoreEditorResult.Status.UNCHANGED,
                    current
            );
            case CONFLICT -> result(
                    scope,
                    canonicalEntityId,
                    OperatorLoreEditorResult.Status.CONFLICT,
                    current
            );
            case INVALID -> result(
                    scope,
                    canonicalEntityId,
                    OperatorLoreEditorResult.Status.INVALID,
                    current
            );
            case FORBIDDEN -> denied(scope, canonicalEntityId);
        };
    }

    static OperatorLoreEditorResult result(
            OperatorLoreScope scope,
            int canonicalEntityId,
            OperatorLoreEditorResult.Status status,
            String value
    ) {
        OperatorLoreEditorPolicy.NetworkView view = OperatorLoreEditorPolicy.networkView(value);
        OperatorLoreEditorResult.Status safeStatus = view.representable()
                ? status
                : OperatorLoreEditorResult.Status.INVALID;
        return new OperatorLoreEditorResult(
                scope,
                canonicalEntityId,
                safeStatus,
                view.value(),
                view.revision()
        );
    }

    private static OperatorLoreEditorResult denied(
            OperatorLoreScope scope,
            int canonicalEntityId
    ) {
        return new OperatorLoreEditorResult(
                scope,
                canonicalEntityId,
                OperatorLoreEditorResult.Status.FORBIDDEN,
                "",
                ""
        );
    }

    private static WorldOperatorLoreStore store(
            Path worldRoot,
            OperatorLoreScope scope,
            OperatorLoreKey key
    ) {
        Objects.requireNonNull(worldRoot, "worldRoot");
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(key, "key");
        if (key.scope() != scope) {
            throw new IllegalArgumentException(
                    "Resolved Operator Lore key scope " + key.scope() + " does not match " + scope
            );
        }
        return WorldOperatorLoreStore.forWorld(worldRoot);
    }
}
