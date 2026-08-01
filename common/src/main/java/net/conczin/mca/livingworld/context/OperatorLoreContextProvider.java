package net.conczin.mca.livingworld.context;

import net.conczin.mca.livingworld.lore.OperatorLoreFormatter;
import net.conczin.mca.livingworld.lore.OperatorLoreKey;
import net.conczin.mca.livingworld.lore.OperatorLoreSnapshot;
import net.conczin.mca.livingworld.lore.WorldOperatorLoreStore;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/** Loads explicit operator-authored lore without mixing it into observed or learned memory. */
public final class OperatorLoreContextProvider {
    private OperatorLoreContextProvider() {
    }

    public static List<String> load(
            Path worldRoot,
            String dimension,
            UUID villagerId,
            UUID playerId,
            int villageId
    ) {
        WorldOperatorLoreStore store = WorldOperatorLoreStore.forWorld(worldRoot);
        OperatorLoreSnapshot snapshot;
        if (villageId >= 0) {
            snapshot = store.snapshot(dimension, villagerId, playerId, villageId);
        } else {
            snapshot = new OperatorLoreSnapshot(
                    store.get(OperatorLoreKey.world()),
                    villagerId == null ? "" : store.get(OperatorLoreKey.villager(villagerId)),
                    playerId == null ? "" : store.get(OperatorLoreKey.player(playerId)),
                    ""
            );
        }
        return OperatorLoreFormatter.format(snapshot);
    }
}
