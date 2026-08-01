package net.conczin.mca.livingworld.lore;

import java.util.Objects;
import java.util.UUID;

public record OperatorLoreKey(
        OperatorLoreScope scope,
        String dimension,
        String targetId
) {
    private static final String VILLAGE_SEPARATOR = "|";

    public OperatorLoreKey {
        scope = Objects.requireNonNull(scope, "scope");
        dimension = normalize(dimension);
        targetId = normalize(targetId);

        switch (scope) {
            case WORLD -> {
                if (!dimension.isEmpty() || !targetId.isEmpty()) {
                    throw new IllegalArgumentException("WORLD lore cannot have a dimension or target");
                }
            }
            case VILLAGER, PLAYER -> {
                if (!dimension.isEmpty() || targetId.isEmpty()) {
                    throw new IllegalArgumentException(scope + " lore requires only a target UUID");
                }
            }
            case VILLAGE -> {
                if (dimension.isEmpty() || targetId.isEmpty()) {
                    throw new IllegalArgumentException("VILLAGE lore requires a dimension and village ID");
                }
                if (dimension.contains(VILLAGE_SEPARATOR)) {
                    throw new IllegalArgumentException("dimension contains reserved separator");
                }
            }
        }
    }

    public static OperatorLoreKey world() {
        return new OperatorLoreKey(OperatorLoreScope.WORLD, "", "");
    }

    public static OperatorLoreKey villager(UUID villagerId) {
        return new OperatorLoreKey(
                OperatorLoreScope.VILLAGER,
                "",
                Objects.requireNonNull(villagerId, "villagerId").toString()
        );
    }

    public static OperatorLoreKey player(UUID playerId) {
        return new OperatorLoreKey(
                OperatorLoreScope.PLAYER,
                "",
                Objects.requireNonNull(playerId, "playerId").toString()
        );
    }

    public static OperatorLoreKey village(String dimension, int villageId) {
        return new OperatorLoreKey(
                OperatorLoreScope.VILLAGE,
                dimension,
                Integer.toString(villageId)
        );
    }

    String storageKey() {
        return switch (scope) {
            case WORLD -> "";
            case VILLAGER, PLAYER -> targetId;
            case VILLAGE -> dimension + VILLAGE_SEPARATOR + targetId;
        };
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
