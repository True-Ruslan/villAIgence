package net.conczin.mca.livingworld.context;

import net.conczin.mca.entity.VillagerEntityMCA;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

/** Server-thread adapter from live MCA identity/personality to the read-only fixed-size social snapshot. */
public final class PersonalitySocialSnapshotCapture {
    private PersonalitySocialSnapshotCapture() {
    }

    public static PersonalitySocialSnapshot capture(
            Path worldRoot,
            VillagerEntityMCA source,
            @Nullable VillagerEntityMCA counterpart
    ) {
        if (source == null) {
            throw new IllegalArgumentException("source villager is required");
        }
        return PersonalitySocialSnapshotStoreView.capture(
                worldRoot,
                source.getUUID(),
                source.getVillagerBrain().getPersonality().name(),
                counterpart == null ? null : counterpart.getUUID()
        );
    }
}
