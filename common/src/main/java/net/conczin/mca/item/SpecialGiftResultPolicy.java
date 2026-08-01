package net.conczin.mca.item;

/**
 * Loader-independent dispatch decision for Mojang InteractionResult values.
 */
final class SpecialGiftResultPolicy {
    private SpecialGiftResultPolicy() {
    }

    record Decision(boolean handled, boolean consume) {
    }

    static Decision decide(boolean pass, boolean consume) {
        if (pass) {
            return new Decision(false, false);
        }
        return new Decision(true, consume);
    }
}
