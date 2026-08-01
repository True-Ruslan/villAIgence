package net.conczin.mca.livingworld.lore.editor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Pure authorization, payload and optimistic-concurrency policy for untrusted lore editor requests. */
public final class OperatorLoreEditorPolicy {
    public static final int MAX_CODE_POINTS = 4_096;
    public static final int MAX_UTF8_BYTES = 12_288;
    public static final int REVISION_HEX_LENGTH = 64;

    private OperatorLoreEditorPolicy() {
    }

    public static boolean canAccess(boolean hasOperatorPermission, Operation operation) {
        return hasOperatorPermission && operation != null;
    }

    public static Decision decideWrite(
            boolean hasOperatorPermission,
            String expectedRevision,
            String currentValue,
            String requestedValue
    ) {
        if (!canAccess(hasOperatorPermission, Operation.WRITE)) {
            return Decision.FORBIDDEN;
        }
        if (!isValidPayload(requestedValue)) {
            return Decision.INVALID;
        }

        String current = canonicalize(currentValue);
        if (!revision(current).equals(expectedRevision)) {
            return Decision.CONFLICT;
        }
        return current.equals(canonicalize(requestedValue))
                ? Decision.UNCHANGED
                : Decision.APPLY;
    }

    public static boolean isValidPayload(String value) {
        if (value == null) {
            return true;
        }
        if (value.codePointCount(0, value.length()) > MAX_CODE_POINTS) {
            return false;
        }
        if (value.getBytes(StandardCharsets.UTF_8).length > MAX_UTF8_BYTES) {
            return false;
        }
        return value.codePoints().noneMatch(codePoint ->
                codePoint < 0x20
                        && codePoint != '\n'
                        && codePoint != '\r'
                        && codePoint != '\t'
        );
    }

    public static String revision(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(canonicalize(value).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    public static String canonicalize(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.replace("\r\n", "\n").replace('\r', '\n');
        return normalized.isBlank() ? "" : normalized;
    }

    public enum Operation {
        READ,
        WRITE
    }

    public enum Decision {
        APPLY,
        UNCHANGED,
        FORBIDDEN,
        CONFLICT,
        INVALID
    }
}
