package net.conczin.mca.item;

/**
 * Loader-independent dispatch decision for Mojang InteractionResult values.
 */
public final class SpecialGiftResultPolicy {
    private SpecialGiftResultPolicy() {
    }

    public record Decision(boolean handled, boolean consume) {
    }

    public static Decision decide(boolean pass, boolean consume) {
        if (pass) {
            return new Decision(false, false);
        }
        return new Decision(true, consume);
    }
}
