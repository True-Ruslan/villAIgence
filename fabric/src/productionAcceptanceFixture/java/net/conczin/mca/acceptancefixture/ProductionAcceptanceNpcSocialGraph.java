package net.conczin.mca.acceptancefixture;

import net.conczin.mca.livingworld.relationship.NpcSocialDelta;
import net.conczin.mca.livingworld.relationship.NpcSocialGraphMutation;
import net.conczin.mca.livingworld.relationship.NpcSocialGraphStore;
import net.conczin.mca.livingworld.relationship.NpcSocialState;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/** Test-only production fixture proving the NPC social graph store survives real startup/restart. */
public final class ProductionAcceptanceNpcSocialGraph implements ModInitializer {
    private static final UUID SOURCE_NPC_ID = UUID.fromString(
            "dc3145cb-7921-48e8-a00d-f951a4c80fa0"
    );
    private static final UUID TARGET_NPC_ID = UUID.fromString(
            "4a24bbaa-8fd5-4b72-8095-2911280c23d1"
    );
    private static final NpcSocialState EXPECTED = new NpcSocialState(3, 2, 1, -1);

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            if (ProductionAcceptanceRecoveryMode.MODE.equals(
                    System.getProperty(ProductionAcceptanceRecoveryMode.MODE_PROPERTY, "")
            )) {
                return;
            }
            Path worldRoot = server.getWorldPath(LevelResource.ROOT);
            NpcSocialGraphStore store = NpcSocialGraphStore.forWorld(worldRoot);
            NpcSocialState current = store.get(SOURCE_NPC_ID, TARGET_NPC_ID);
            if (current.equals(NpcSocialState.NEUTRAL)) {
                NpcSocialGraphMutation mutation = store.applyDelta(
                        SOURCE_NPC_ID,
                        TARGET_NPC_ID,
                        new NpcSocialDelta(3, 2, 1, -1),
                        4
                );
                if (mutation.status() != NpcSocialGraphMutation.Status.APPLIED) {
                    throw new IllegalStateException(
                            "NPC social graph production fixture was not admitted: " + mutation.status()
                    );
                }
            }
            if (!EXPECTED.equals(store.get(SOURCE_NPC_ID, TARGET_NPC_ID))) {
                throw new IllegalStateException("NPC social graph production fixture changed across restart");
            }
            Path graph = worldRoot.resolve("livingworld/npc-social-graph.json");
            if (!Files.isRegularFile(graph)) {
                throw new IllegalStateException("NPC social graph production fixture file is missing");
            }
        });
    }
}
