package net.conczin.mca.network.c2s;

import net.conczin.mca.resources.Rank;

/** Pure authorization/validation policy for untrusted blueprint client requests. */
public final class BlueprintPermissionPolicy {
    private static final int MAX_VILLAGE_NAME_LENGTH = 32;

    private BlueprintPermissionPolicy() {
    }

    public static boolean can(Rank rank, Operation operation) {
        if (rank == null || operation == null) return false;
        return rank.isAtLeast(operation.minimumRank);
    }

    public static boolean isValidRatio(float value) {
        return Float.isFinite(value) && value >= 0.0f && value <= 1.0f;
    }

    public static String sanitizeName(String value) {
        if (value == null) return "";
        String sanitized = value.codePoints()
                .filter(codePoint -> !Character.isISOControl(codePoint))
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString()
                .strip();
        int codePoints = sanitized.codePointCount(0, sanitized.length());
        if (codePoints > MAX_VILLAGE_NAME_LENGTH) {
            int end = sanitized.offsetByCodePoints(0, MAX_VILLAGE_NAME_LENGTH);
            sanitized = sanitized.substring(0, end);
        }
        return sanitized;
    }

    public enum Operation {
        ADD_BUILDING(Rank.PEASANT),
        ADD_ROOM(Rank.PEASANT),
        CHANGE_TAXES(Rank.MERCHANT),
        CHANGE_POPULATION(Rank.NOBLE),
        CHANGE_MARRIAGE(Rank.MAYOR),
        RENAME(Rank.MAYOR),
        REMOVE_BUILDING(Rank.MAYOR),
        FORCE_BUILDING_TYPE(Rank.MAYOR),
        TOGGLE_AUTO_SCAN(Rank.MAYOR),
        FULL_SCAN(Rank.MAYOR);

        private final Rank minimumRank;

        Operation(Rank minimumRank) {
            this.minimumRank = minimumRank;
        }

        public Rank minimumRank() {
            return minimumRank;
        }
    }
}
