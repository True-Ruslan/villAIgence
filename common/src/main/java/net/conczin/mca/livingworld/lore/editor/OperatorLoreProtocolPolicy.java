package net.conczin.mca.livingworld.lore.editor;

/** Pure correlation policy. Request IDs have no authorization or persistence meaning. */
public final class OperatorLoreProtocolPolicy {
    private OperatorLoreProtocolPolicy() {
    }

    public static int nextRequestId(int current) {
        return current <= 0 || current == Integer.MAX_VALUE ? 1 : current + 1;
    }

    public static boolean matches(int expected, int received) {
        return expected > 0 && expected == received;
    }

    public static int echo(int requestId) {
        return requestId;
    }
}
