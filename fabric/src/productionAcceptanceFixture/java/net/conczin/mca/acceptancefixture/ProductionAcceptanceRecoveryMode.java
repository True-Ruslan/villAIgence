package net.conczin.mca.acceptancefixture;

import net.conczin.mca.livingworld.lore.OperatorLoreKey;
import net.conczin.mca.livingworld.lore.WorldOperatorLoreStore;
import net.conczin.mca.livingworld.memory.ConversationMemoryStore;
import net.conczin.mca.livingworld.memory2.MemoryEvent;
import net.conczin.mca.livingworld.memory2.MemoryEventStore;
import net.conczin.mca.livingworld.memory2.SemanticMemoryEntry;
import net.conczin.mca.livingworld.memory2.SemanticMemoryStore;
import net.conczin.mca.livingworld.relationship.LivingWorldRelationshipDelta;
import net.conczin.mca.livingworld.relationship.LivingWorldRelationshipState;
import net.conczin.mca.livingworld.relationship.LivingWorldRelationshipStore;
import net.conczin.mca.livingworld.voice.NpcVoiceAgeGroup;
import net.conczin.mca.livingworld.voice.NpcVoiceCatalog;
import net.conczin.mca.livingworld.voice.NpcVoiceGender;
import net.conczin.mca.livingworld.voice.PersistentNpcVoiceStore;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/** Test-only fast fixture used only by the derived recovery acceptance JAR. */
public final class ProductionAcceptanceRecoveryMode implements ModInitializer {
    public static final String MODE_PROPERTY = "villaigence.acceptance.mode";
    public static final String MODE = "recovery";
    public static final String RECOVERY_READY_MARKER = "VAI-PERSIST-003-RECOVERY-READY";

    private static final Logger LOGGER = LoggerFactory.getLogger(
            "VillAIgenceProductionRecoveryAcceptance"
    );
    private static final UUID NPC_ID = UUID.fromString(
            "dc3145cb-7921-48e8-a00d-f951a4c80fa0"
    );
    private static final UUID PLAYER_ID = UUID.fromString(
            "d2932adb-aa33-4c7f-8170-b98f38323311"
    );
    private static final UUID EVENT_ID = UUID.fromString(
            "e2f62ce5-b66f-4f63-8a70-25393bf95fc8"
    );
    private static final UUID SEMANTIC_ID = UUID.fromString(
            "4277e884-d194-4a30-b64a-b32561ec89a9"
    );
    private static final String FIXTURE_TEXT =
            "VillAIgence production recovery acceptance fixture";
    private static final List<String> STORES = List.of(
            "memory.json",
            "memory2.json",
            "semantic-memory.json",
            "relationships.json",
            "voices.json",
            "operator-lore.json"
    );

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            if (!enabled()) {
                return;
            }
            Path worldRoot = server.getWorldPath(LevelResource.ROOT);
            initializeConversation(worldRoot);
            initializeMemoryEvent(worldRoot);
            initializeSemanticMemory(worldRoot);
            initializeRelationship(worldRoot);
            initializeVoice(worldRoot);
            initializeOperatorLore(worldRoot);
            verifyStores(server);
            LOGGER.info("{}", RECOVERY_READY_MARKER);
            LOGGER.info("{}", ProductionAcceptanceFixture.READY_MARKER);
        });
    }

    private static boolean enabled() {
        return MODE.equals(System.getProperty(MODE_PROPERTY, ""));
    }

    private static void initializeConversation(Path worldRoot) {
        ConversationMemoryStore store = ConversationMemoryStore.forWorld(worldRoot);
        if (store.getMessages(NPC_ID, PLAYER_ID).isEmpty()) {
            store.appendExchange(
                    NPC_ID,
                    PLAYER_ID,
                    "recovery-fixture-user",
                    "recovery-fixture-assistant",
                    4,
                    64
            );
        }
        if (store.getMessages(NPC_ID, PLAYER_ID).size() != 2) {
            throw new IllegalStateException("conversation recovery fixture is not stable");
        }
    }

    private static void initializeMemoryEvent(Path worldRoot) {
        MemoryEventStore store = MemoryEventStore.forWorld(worldRoot);
        if (store.getRecent(NPC_ID, 8).stream().noneMatch(event -> EVENT_ID.equals(event.id()))) {
            store.append(new MemoryEvent(
                    EVENT_ID,
                    NPC_ID,
                    MemoryEvent.Type.OBSERVATION,
                    FIXTURE_TEXT,
                    List.of(PLAYER_ID),
                    MemoryEvent.Provenance.SYSTEM_OBSERVED,
                    1L,
                    1L,
                    50,
                    0,
                    100,
                    List.of("production-recovery-acceptance")
            ), 8);
        }
        if (store.getRecent(NPC_ID, 8).stream().noneMatch(event -> EVENT_ID.equals(event.id()))) {
            throw new IllegalStateException("memory2 recovery fixture is not stable");
        }
    }

    private static void initializeSemanticMemory(Path worldRoot) {
        SemanticMemoryStore store = SemanticMemoryStore.forWorld(worldRoot);
        if (store.getRecent(NPC_ID, 8).stream().noneMatch(entry -> SEMANTIC_ID.equals(entry.id()))) {
            store.append(new SemanticMemoryEntry(
                    SEMANTIC_ID,
                    NPC_ID,
                    SemanticMemoryEntry.Kind.FACT,
                    FIXTURE_TEXT,
                    List.of(PLAYER_ID),
                    MemoryEvent.Provenance.SYSTEM_OBSERVED,
                    1L,
                    1L,
                    50,
                    100,
                    List.of(EVENT_ID)
            ), 8);
        }
        if (store.getRecent(NPC_ID, 8).stream().noneMatch(entry -> SEMANTIC_ID.equals(entry.id()))) {
            throw new IllegalStateException("semantic recovery fixture is not stable");
        }
    }

    private static void initializeRelationship(Path worldRoot) {
        LivingWorldRelationshipStore store = LivingWorldRelationshipStore.forWorld(worldRoot);
        if (store.get(NPC_ID, PLAYER_ID).equals(LivingWorldRelationshipState.NEUTRAL)) {
            store.applyDelta(
                    NPC_ID,
                    PLAYER_ID,
                    new LivingWorldRelationshipDelta(1, 1, 0, 1),
                    4
            );
        }
        if (store.get(NPC_ID, PLAYER_ID).equals(LivingWorldRelationshipState.NEUTRAL)) {
            throw new IllegalStateException("relationship recovery fixture is not stable");
        }
    }

    private static void initializeVoice(Path worldRoot) {
        NpcVoiceCatalog catalog = new NpcVoiceCatalog(new NpcVoiceCatalog.VoicePools(
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of("production-recovery-fixture-voice"),
                ""
        ));
        String voiceId = PersistentNpcVoiceStore.forWorld(worldRoot)
                .resolve(NPC_ID, NpcVoiceGender.NEUTRAL, NpcVoiceAgeGroup.ADULT, catalog)
                .voiceId();
        if (!"production-recovery-fixture-voice".equals(voiceId)) {
            throw new IllegalStateException("voice recovery fixture is not stable");
        }
    }

    private static void initializeOperatorLore(Path worldRoot) {
        WorldOperatorLoreStore store = WorldOperatorLoreStore.forWorld(worldRoot);
        OperatorLoreKey key = OperatorLoreKey.world();
        if (store.get(key).isBlank()) {
            store.put(key, FIXTURE_TEXT);
        }
        if (!FIXTURE_TEXT.equals(store.get(key))) {
            throw new IllegalStateException("operator lore recovery fixture is not stable");
        }
    }

    private static void verifyStores(MinecraftServer server) {
        Path livingWorld = server.getWorldPath(LevelResource.ROOT).resolve("livingworld");
        for (String store : STORES) {
            Path file = livingWorld.resolve(store);
            if (!Files.isRegularFile(file)) {
                throw new IllegalStateException(
                        "Recovery fixture store was not initialized: " + store
                );
            }
        }
    }
}
