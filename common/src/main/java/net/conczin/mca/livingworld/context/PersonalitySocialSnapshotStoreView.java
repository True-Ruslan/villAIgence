package net.conczin.mca.livingworld.context;

import net.conczin.mca.livingworld.relationship.NpcSocialGraphStore;
import net.conczin.mca.livingworld.relationship.NpcSocialState;

import java.nio.file.Path;
import java.util.UUID;

/** Read-only fixed-pair view over the persistent directed NPC social graph. */
public final class PersonalitySocialSnapshotStoreView {
    private PersonalitySocialSnapshotStoreView() {
    }

    public static PersonalitySocialSnapshot capture(
            Path worldRoot,
            UUID sourceNpcId,
            String personalityToken,
            UUID counterpartNpcId
    ) {
        if (counterpartNpcId == null) {
            return new PersonalitySocialSnapshot(
                    sourceNpcId,
                    personalityToken,
                    null,
                    NpcSocialState.NEUTRAL
            );
        }

        if (worldRoot == null) {
            throw new IllegalArgumentException("worldRoot is required when counterpartNpcId is present");
        }
        NpcSocialState directed = NpcSocialGraphStore.forWorld(worldRoot).get(sourceNpcId, counterpartNpcId);
        return new PersonalitySocialSnapshot(
                sourceNpcId,
                personalityToken,
                counterpartNpcId,
                directed
        );
    }
}
