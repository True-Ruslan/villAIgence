package net.conczin.mca.livingworld.lore.editor;

import net.conczin.mca.MCA;
import net.conczin.mca.entity.VillagerEntityMCA;
import net.conczin.mca.livingworld.lore.OperatorLoreKey;
import net.conczin.mca.livingworld.lore.OperatorLoreScope;
import net.conczin.mca.livingworld.lore.WorldOperatorLoreStore;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;

/**
 * Resolves all editor identities from trusted server state and applies permission/revision checks.
 * Callers must invoke this service on the Minecraft server thread.
 */
public final class OperatorLoreServerAuthority {
    public static final int REQUIRED_PERMISSION_LEVEL = 2;

    private OperatorLoreServerAuthority() {
    }

    public static OperatorLoreEditorResult read(
            ServerPlayer player,
            String scopeName,
            int villagerEntityId
    ) {
        if (player == null || !player.hasPermissions(REQUIRED_PERMISSION_LEVEL)) {
            return OperatorLoreEditorResult.denied(scopeName, villagerEntityId);
        }

        Optional<OperatorLoreScope> parsedScope = parseScope(scopeName);
        if (parsedScope.isEmpty()) {
            return result(
                    OperatorLoreScope.WORLD,
                    villagerEntityId,
                    OperatorLoreEditorResult.Status.INVALID,
                    ""
            );
        }

        Optional<ResolvedTarget> target = resolve(player, parsedScope.get(), villagerEntityId);
        if (target.isEmpty()) {
            return result(
                    parsedScope.get(),
                    villagerEntityId,
                    OperatorLoreEditorResult.Status.NOT_FOUND,
                    ""
            );
        }

        try {
            String value = store(player).get(target.get().key());
            return result(
                    parsedScope.get(),
                    target.get().canonicalEntityId(),
                    OperatorLoreEditorResult.Status.OK,
                    value
            );
        } catch (RuntimeException failure) {
            MCA.LOGGER.warn("Unable to read operator lore for player {} scope {}", player.getUUID(), parsedScope.get(), failure);
            return result(
                    parsedScope.get(),
                    target.get().canonicalEntityId(),
                    OperatorLoreEditorResult.Status.ERROR,
                    ""
            );
        }
    }

    public static OperatorLoreEditorResult write(
            ServerPlayer player,
            String scopeName,
            int villagerEntityId,
            String expectedRevision,
            String requestedValue
    ) {
        if (player == null || !player.hasPermissions(REQUIRED_PERMISSION_LEVEL)) {
            return OperatorLoreEditorResult.denied(scopeName, villagerEntityId);
        }

        Optional<OperatorLoreScope> parsedScope = parseScope(scopeName);
        if (parsedScope.isEmpty()) {
            return result(
                    OperatorLoreScope.WORLD,
                    villagerEntityId,
                    OperatorLoreEditorResult.Status.INVALID,
                    ""
            );
        }

        Optional<ResolvedTarget> target = resolve(player, parsedScope.get(), villagerEntityId);
        if (target.isEmpty()) {
            return result(
                    parsedScope.get(),
                    villagerEntityId,
                    OperatorLoreEditorResult.Status.NOT_FOUND,
                    ""
            );
        }

        try {
            WorldOperatorLoreStore store = store(player);
            String current = store.get(target.get().key());
            OperatorLoreEditorPolicy.Decision decision = OperatorLoreEditorPolicy.decideWrite(
                    true,
                    expectedRevision,
                    current,
                    requestedValue
            );

            return switch (decision) {
                case APPLY -> {
                    store.put(target.get().key(), OperatorLoreEditorPolicy.canonicalize(requestedValue));
                    String canonical = store.get(target.get().key());
                    yield result(
                            parsedScope.get(),
                            target.get().canonicalEntityId(),
                            OperatorLoreEditorResult.Status.OK,
                            canonical
                    );
                }
                case UNCHANGED -> result(
                        parsedScope.get(),
                        target.get().canonicalEntityId(),
                        OperatorLoreEditorResult.Status.UNCHANGED,
                        current
                );
                case CONFLICT -> result(
                        parsedScope.get(),
                        target.get().canonicalEntityId(),
                        OperatorLoreEditorResult.Status.CONFLICT,
                        current
                );
                case INVALID -> result(
                        parsedScope.get(),
                        target.get().canonicalEntityId(),
                        OperatorLoreEditorResult.Status.INVALID,
                        current
                );
                case FORBIDDEN -> result(
                        parsedScope.get(),
                        target.get().canonicalEntityId(),
                        OperatorLoreEditorResult.Status.FORBIDDEN,
                        ""
                );
            };
        } catch (RuntimeException failure) {
            MCA.LOGGER.warn("Unable to write operator lore for player {} scope {}", player.getUUID(), parsedScope.get(), failure);
            return result(
                    parsedScope.get(),
                    target.get().canonicalEntityId(),
                    OperatorLoreEditorResult.Status.ERROR,
                    ""
            );
        }
    }

    private static Optional<ResolvedTarget> resolve(
            ServerPlayer player,
            OperatorLoreScope scope,
            int villagerEntityId
    ) {
        if (scope == OperatorLoreScope.WORLD) {
            return Optional.of(new ResolvedTarget(OperatorLoreKey.world(), -1));
        }
        if (scope == OperatorLoreScope.PLAYER) {
            return Optional.of(new ResolvedTarget(OperatorLoreKey.player(player.getUUID()), -1));
        }

        Entity entity = player.serverLevel().getEntity(villagerEntityId);
        VillagerEntityMCA villager = entity instanceof VillagerEntityMCA candidate ? candidate : null;
        boolean villagerPresent = villager != null && villager.isAlive() && !villager.isRemoved();
        boolean sameLevel = villagerPresent && villager.level() == player.level();
        double distanceSquared = villagerPresent
                ? player.distanceToSqr(villager)
                : Double.POSITIVE_INFINITY;
        boolean homeVillagePresent = villagerPresent
                && villager.getResidency().getHomeVillage().isPresent();

        if (!OperatorLoreTargetPolicy.canResolve(
                scope,
                villagerPresent,
                sameLevel,
                distanceSquared,
                homeVillagePresent
        )) {
            return Optional.empty();
        }

        if (scope == OperatorLoreScope.VILLAGER) {
            return Optional.of(new ResolvedTarget(
                    OperatorLoreKey.villager(villager.getUUID()),
                    villager.getId()
            ));
        }

        return villager.getResidency().getHomeVillage().map(village -> new ResolvedTarget(
                OperatorLoreKey.village(
                        villager.level().dimension().location().toString(),
                        village.getId()
                ),
                villager.getId()
        ));
    }

    private static Optional<OperatorLoreScope> parseScope(String scopeName) {
        if (scopeName == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(OperatorLoreScope.valueOf(scopeName.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException invalid) {
            return Optional.empty();
        }
    }

    private static WorldOperatorLoreStore store(ServerPlayer player) {
        Path worldRoot = player.serverLevel().getServer().getWorldPath(LevelResource.ROOT);
        return WorldOperatorLoreStore.forWorld(worldRoot);
    }

    private static OperatorLoreEditorResult result(
            OperatorLoreScope scope,
            int entityId,
            OperatorLoreEditorResult.Status status,
            String value
    ) {
        String canonical = value == null ? "" : value;
        return new OperatorLoreEditorResult(
                scope,
                entityId,
                status,
                canonical,
                OperatorLoreEditorPolicy.revision(canonical)
        );
    }

    private record ResolvedTarget(OperatorLoreKey key, int canonicalEntityId) {
    }
}
