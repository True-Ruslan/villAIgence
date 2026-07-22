package net.conczin.mca.livingworld.memory2;

import net.conczin.mca.livingworld.relationship.LivingWorldRelationshipChange;
import net.conczin.mca.livingworld.relationship.LivingWorldRelationshipDelta;
import net.conczin.mca.livingworld.relationship.LivingWorldRelationshipState;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Converts one successfully persisted relationship transition into server-observed Memory 2.0 evidence. */
public final class RelationshipChangeMemoryAdapter {
    private static final String ID_NAMESPACE = "memory2-relationship-change-v1";

    private RelationshipChangeMemoryAdapter() {
    }

    public static Optional<MemoryEvent> toMemoryEvent(
            UUID npcId,
            UUID playerId,
            long gameTime,
            LivingWorldRelationshipChange change,
            long createdAtEpochMillis
    ) {
        if (npcId == null || playerId == null || change == null || !change.changed()) return Optional.empty();
        LivingWorldRelationshipDelta applied = change.appliedDelta();
        if (applied == null || applied.isZero()) return Optional.empty();

        long safeGameTime = Math.max(0L, gameTime);
        UUID id = deterministicId(npcId, playerId, safeGameTime, change.before(), change.after());
        String summary = "Relationship with player changed: trust " + signed(applied.trust())
                + ", respect " + signed(applied.respect())
                + ", fear " + signed(applied.fear())
                + ", affinity " + signed(applied.affinity())
                + "; now trust=" + change.after().trust()
                + ", respect=" + change.after().respect()
                + ", fear=" + change.after().fear()
                + ", affinity=" + change.after().affinity() + ".";

        return Optional.of(new MemoryEvent(
                id,
                npcId,
                MemoryEvent.Type.RELATIONSHIP_CHANGE,
                summary,
                List.of(npcId, playerId),
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                safeGameTime,
                createdAtEpochMillis,
                55,
                0,
                100,
                List.of()
        ));
    }

    private static UUID deterministicId(
            UUID npcId,
            UUID playerId,
            long gameTime,
            LivingWorldRelationshipState before,
            LivingWorldRelationshipState after
    ) {
        String canonical = ID_NAMESPACE
                + '\n' + npcId
                + '\n' + playerId
                + '\n' + gameTime
                + '\n' + tuple(before)
                + '\n' + tuple(after);
        return UUID.nameUUIDFromBytes(canonical.getBytes(StandardCharsets.UTF_8));
    }

    private static String tuple(LivingWorldRelationshipState state) {
        return state.trust() + "," + state.respect() + "," + state.fear() + "," + state.affinity();
    }

    private static String signed(int value) {
        return value > 0 ? "+" + value : Integer.toString(value);
    }
}
