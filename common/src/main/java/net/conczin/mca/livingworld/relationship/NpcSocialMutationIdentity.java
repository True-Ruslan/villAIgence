package net.conczin.mca.livingworld.relationship;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

/** Stable identity for one source-NPC causal social mutation. */
public final class NpcSocialMutationIdentity {
    private static final String ID_NAMESPACE = "npc-social-causal-mutation-v1";

    private NpcSocialMutationIdentity() {
    }

    public static UUID forCause(UUID sourceNpcId, UUID causeEventId) {
        Objects.requireNonNull(sourceNpcId, "sourceNpcId");
        Objects.requireNonNull(causeEventId, "causeEventId");
        String canonical = ID_NAMESPACE + '\n' + sourceNpcId + '\n' + causeEventId;
        return UUID.nameUUIDFromBytes(canonical.getBytes(StandardCharsets.UTF_8));
    }
}
