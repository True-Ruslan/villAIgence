package net.conczin.mca.acceptancefixture;

import net.conczin.mca.livingworld.memory2.MemoryEvent;
import net.conczin.mca.livingworld.memory2.MemoryEventStore;
import net.conczin.mca.livingworld.memory2.NpcIdentityAuthority;
import net.conczin.mca.livingworld.memory2.NpcSocialMutationLifecycle;
import net.conczin.mca.livingworld.memory2.NpcSocialMutationLifecycleResult;
import net.conczin.mca.livingworld.relationship.NpcSocialDelta;
import net.conczin.mca.livingworld.relationship.NpcSocialGraphStore;
import net.conczin.mca.livingworld.relationship.NpcSocialMutationIdentity;
import net.conczin.mca.livingworld.relationship.NpcSocialState;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/** Test-only production fixture proving causal NPC social mutation survives real startup/restart. */
public final class ProductionAcceptanceNpcSocialGraph implements ModInitializer {
    private static final UUID SOURCE_NPC_ID = UUID.fromString(
            "dc3145cb-7921-48e8-a00d-f951a4c80fa0"
    );
    private static final UUID TARGET_NPC_ID = UUID.fromString(
            "4a24bbaa-8fd5-4b72-8095-2911280c23d1"
    );
    private static final UUID CAUSE_EVENT_ID = UUID.fromString(
            "b2a33ad0-1c89-46c3-8391-88824a5783cb"
    );
    private static final long CAUSE_GAME_TIME = 100L;
    private static final int MAX_EVENTS_PER_NPC = 256;
    private static final NpcSocialDelta DELTA = new NpcSocialDelta(3, 2, 1, -1);
    private static final NpcSocialState EXPECTED = new NpcSocialState(3, 2, 1, -1);
    private static final NpcIdentityAuthority FIXTURE_IDENTITIES = npcId ->
            SOURCE_NPC_ID.equals(npcId) || TARGET_NPC_ID.equals(npcId);

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            if (ProductionAcceptanceRecoveryMode.MODE.equals(
                    System.getProperty(ProductionAcceptanceRecoveryMode.MODE_PROPERTY, "")
            )) {
                return;
            }

            Path worldRoot = server.getWorldPath(LevelResource.ROOT);
            MemoryEventStore memory = MemoryEventStore.forWorld(worldRoot);
            if (memory.findById(SOURCE_NPC_ID, CAUSE_EVENT_ID).isEmpty()) {
                memory.append(new MemoryEvent(
                        CAUSE_EVENT_ID,
                        SOURCE_NPC_ID,
                        MemoryEvent.Type.OBSERVATION,
                        "Production acceptance NPC observed the target.",
                        List.of(TARGET_NPC_ID),
                        MemoryEvent.Provenance.SYSTEM_OBSERVED,
                        CAUSE_GAME_TIME,
                        1_000L,
                        80,
                        0,
                        100,
                        List.of()
                ), MAX_EVENTS_PER_NPC);
            }

            NpcSocialMutationLifecycleResult lifecycle = NpcSocialMutationLifecycle.apply(
                    worldRoot,
                    SOURCE_NPC_ID,
                    TARGET_NPC_ID,
                    CAUSE_EVENT_ID,
                    DELTA,
                    4,
                    MAX_EVENTS_PER_NPC,
                    2_000L,
                    FIXTURE_IDENTITIES
            );
            if (lifecycle.status() != NpcSocialMutationLifecycleResult.Status.APPLIED
                    && lifecycle.status() != NpcSocialMutationLifecycleResult.Status.REPLAYED) {
                throw new IllegalStateException(
                        "Causal NPC social mutation fixture failed: " + lifecycle.status()
                );
            }

            NpcSocialGraphStore graphStore = NpcSocialGraphStore.forWorld(worldRoot);
            if (!EXPECTED.equals(graphStore.get(SOURCE_NPC_ID, TARGET_NPC_ID))) {
                throw new IllegalStateException("Causal NPC social graph changed across restart");
            }

            UUID mutationId = NpcSocialMutationIdentity.forCause(SOURCE_NPC_ID, CAUSE_EVENT_ID);
            MemoryEvent audit = memory.findById(SOURCE_NPC_ID, mutationId)
                    .orElseThrow(() -> new IllegalStateException(
                            "Causal NPC social mutation audit event is missing"
                    ));
            if (audit.type() != MemoryEvent.Type.NPC_SOCIAL_CHANGE
                    || audit.npcSocialMutation() == null
                    || !mutationId.equals(audit.npcSocialMutation().mutationId())
                    || !TARGET_NPC_ID.equals(audit.npcSocialMutation().targetNpcId())
                    || !CAUSE_EVENT_ID.equals(audit.npcSocialMutation().causeEventId())
                    || audit.npcSocialMutation().causeGameTime() != CAUSE_GAME_TIME
                    || !EXPECTED.equals(audit.npcSocialMutation().after())) {
                throw new IllegalStateException("Causal NPC social mutation audit evidence changed across restart");
            }

            Path graph = worldRoot.resolve("livingworld/npc-social-graph.json");
            Path memoryFile = worldRoot.resolve("livingworld/memory2.json");
            if (!Files.isRegularFile(graph) || !Files.isRegularFile(memoryFile)) {
                throw new IllegalStateException("Causal NPC social mutation fixture persistence is missing");
            }
        });
    }
}
