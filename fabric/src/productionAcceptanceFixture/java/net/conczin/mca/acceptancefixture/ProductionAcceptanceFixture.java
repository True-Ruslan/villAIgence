package net.conczin.mca.acceptancefixture;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.conczin.mca.block.TombstoneBlock;
import net.conczin.mca.entity.VillagerEntityMCA;
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
import net.conczin.mca.registry.BlocksMCA;
import net.conczin.mca.registry.EntitiesMCA;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/** Test-only production-namespace fixture for persistent-store startup/restart acceptance. */
public final class ProductionAcceptanceFixture implements ModInitializer {
    public static final String READY_MARKER = "VILLAIGENCE_PRODUCTION_FIXTURE_READY";
    public static final String LIFECYCLE_CREATED_MARKER = "VAI-LIFE-002-CREATED";
    public static final String LIFECYCLE_RESTART_MARKER = "VAI-LIFE-002-RESTART-VERIFIED";

    private static final Logger LOGGER = LoggerFactory.getLogger("VillAIgenceProductionAcceptance");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final UUID NPC_ID = UUID.fromString("dc3145cb-7921-48e8-a00d-f951a4c80fa0");
    private static final UUID PLAYER_ID = UUID.fromString("d2932adb-aa33-4c7f-8170-b98f38323311");
    private static final UUID EVENT_ID = UUID.fromString("e2f62ce5-b66f-4f63-8a70-25393bf95fc8");
    private static final UUID SEMANTIC_ID = UUID.fromString("4277e884-d194-4a30-b64a-b32561ec89a9");
    private static final UUID LIFECYCLE_NPC_ID = UUID.fromString(
            "5cf53206-ec2c-4c88-ad11-a8bbc56f514e"
    );
    private static final String LIFECYCLE_NPC_NAME = "Production Lifecycle Acceptance";
    private static final String FIXTURE_TEXT = "VillAIgence production acceptance fixture";
    private static final Map<String, Integer> LIFECYCLE_INVENTORY = Map.of(
            "minecraft:bread", 11,
            "minecraft:emerald", 3,
            "minecraft:iron_sword", 1
    );

    private static LifecycleRun activeLifecycle;

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            Path worldRoot = server.getWorldPath(LevelResource.ROOT);
            initializeConversation(worldRoot);
            initializeMemoryEvent(worldRoot);
            initializeSemanticMemory(worldRoot);
            initializeRelationship(worldRoot);
            initializeVoice(worldRoot);
            initializeOperatorLore(worldRoot);
            verifyPersistentStores(worldRoot);
            activeLifecycle = new LifecycleRun(server, worldRoot);
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            LifecycleRun lifecycle = activeLifecycle;
            if (lifecycle == null || lifecycle.server != server) {
                return;
            }
            if (lifecycle.tick()) {
                activeLifecycle = null;
            }
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            LifecycleRun lifecycle = activeLifecycle;
            if (lifecycle != null && lifecycle.server == server) {
                activeLifecycle = null;
            }
        });
    }

    private static void initializeConversation(Path worldRoot) {
        ConversationMemoryStore store = ConversationMemoryStore.forWorld(worldRoot);
        if (store.getMessages(NPC_ID, PLAYER_ID).isEmpty()) {
            store.appendExchange(
                    NPC_ID,
                    PLAYER_ID,
                    "fixture-user",
                    "fixture-assistant",
                    4,
                    64
            );
        }
        if (store.getMessages(NPC_ID, PLAYER_ID).size() != 2) {
            throw new IllegalStateException("conversation fixture is not stable");
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
                    List.of("production-acceptance")
            ), 8);
        }
        if (store.getRecent(NPC_ID, 8).stream().noneMatch(event -> EVENT_ID.equals(event.id()))) {
            throw new IllegalStateException("memory2 fixture is not stable");
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
            throw new IllegalStateException("semantic-memory fixture is not stable");
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
            throw new IllegalStateException("relationship fixture is not stable");
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
                List.of("production-fixture-voice"),
                ""
        ));
        String voiceId = PersistentNpcVoiceStore.forWorld(worldRoot)
                .resolve(NPC_ID, NpcVoiceGender.NEUTRAL, NpcVoiceAgeGroup.ADULT, catalog)
                .voiceId();
        if (!"production-fixture-voice".equals(voiceId)) {
            throw new IllegalStateException("voice fixture is not stable");
        }
    }

    private static void initializeOperatorLore(Path worldRoot) {
        WorldOperatorLoreStore store = WorldOperatorLoreStore.forWorld(worldRoot);
        OperatorLoreKey key = OperatorLoreKey.world();
        if (store.get(key).isBlank()) {
            store.put(key, FIXTURE_TEXT);
        }
        if (!FIXTURE_TEXT.equals(store.get(key))) {
            throw new IllegalStateException("operator-lore fixture is not stable");
        }
    }

    private static VillagerEntityMCA createLifecycleVillager(ServerLevel level) {
        VillagerEntityMCA villager = EntitiesMCA.MALE_VILLAGER.create(level);
        if (villager == null) {
            throw new IllegalStateException("lifecycle MCA villager fixture is not creatable");
        }
        villager.setUUID(LIFECYCLE_NPC_ID);
        villager.setCustomName(Component.literal(LIFECYCLE_NPC_NAME));
        villager.getInventory().setItem(0, new ItemStack(Items.EMERALD, 3));
        villager.getInventory().setItem(7, new ItemStack(Items.BREAD, 11));
        villager.getInventory().setItem(26, new ItemStack(Items.IRON_SWORD));
        return villager;
    }

    private static void prepareTombstone(ServerLevel level, BlockPos position) {
        level.setBlockAndUpdate(position.below(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(position, BlocksMCA.UPRIGHT_HEADSTONE.defaultBlockState());
        requireTombstoneData(level, position);
    }

    private static TombstoneBlock.Data requireTombstoneData(
            ServerLevel level,
            BlockPos position
    ) {
        if (!(level.getBlockEntity(position) instanceof TombstoneBlock.Data data)) {
            throw new IllegalStateException("lifecycle tombstone block entity was not created");
        }
        return data;
    }

    private static int countLooseFixtureInventory(ServerLevel level, BlockPos position) {
        AABB area = new AABB(
                position.getX() - 4.0D,
                position.getY() - 4.0D,
                position.getZ() - 4.0D,
                position.getX() + 5.0D,
                position.getY() + 5.0D,
                position.getZ() + 5.0D
        );
        int count = 0;
        for (ItemEntity itemEntity : level.getEntitiesOfClass(ItemEntity.class, area)) {
            Item item = itemEntity.getItem().getItem();
            if (item == Items.EMERALD || item == Items.BREAD || item == Items.IRON_SWORD) {
                count += itemEntity.getItem().getCount();
            }
        }
        return count;
    }

    private static boolean hasRegisteredLifecycleIdentity(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(LIFECYCLE_NPC_ID);
            if (entity != null && !entity.isRemoved()) {
                return true;
            }
        }
        return false;
    }

    private static VillagerEntityMCA requireLifecycleEntity(MinecraftServer server) {
        Entity found = null;
        for (ServerLevel level : server.getAllLevels()) {
            Entity candidate = level.getEntity(LIFECYCLE_NPC_ID);
            if (candidate == null || candidate.isRemoved()) {
                continue;
            }
            if (found != null) {
                throw new IllegalStateException("duplicate lifecycle UUID exists across server levels");
            }
            found = candidate;
        }
        if (!(found instanceof VillagerEntityMCA villager) || !villager.isAlive()) {
            throw new IllegalStateException("authoritative lifecycle MCA villager is missing");
        }
        return villager;
    }

    private static void verifyLifecycleEntity(
            MinecraftServer server,
            VillagerEntityMCA villager
    ) {
        if (!LIFECYCLE_NPC_ID.equals(villager.getUUID())) {
            throw new IllegalStateException("lifecycle UUID changed");
        }
        if (!LIFECYCLE_NPC_NAME.equals(villager.getName().getString())) {
            throw new IllegalStateException("lifecycle name changed");
        }
        Map<String, Integer> inventory = inventoryMultiset(villager);
        if (!LIFECYCLE_INVENTORY.equals(inventory)) {
            throw new IllegalStateException(
                    "lifecycle inventory changed: expected "
                            + LIFECYCLE_INVENTORY + ", found " + inventory
            );
        }
        if (countLiveLifecycleEntities(server) != 1) {
            throw new IllegalStateException("lifecycle UUID must resolve to exactly one live entity");
        }
    }

    private static int countLiveLifecycleEntities(MinecraftServer server) {
        int count = 0;
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(LIFECYCLE_NPC_ID);
            if (entity != null && entity.isAlive() && !entity.isRemoved()) {
                count++;
            }
        }
        return count;
    }

    private static Map<String, Integer> inventoryMultiset(VillagerEntityMCA villager) {
        Map<String, Integer> inventory = new TreeMap<>();
        for (int slot = 0; slot < villager.getInventory().getContainerSize(); slot++) {
            ItemStack stack = villager.getInventory().getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            inventory.merge(itemId, stack.getCount(), Integer::sum);
        }
        return inventory;
    }

    private static JsonObject lifecycleSnapshot(
            MinecraftServer server,
            VillagerEntityMCA villager,
            String phase,
            boolean portableGraveConsumed
    ) {
        JsonObject snapshot = new JsonObject();
        snapshot.addProperty("schema", 1);
        snapshot.addProperty("phase", phase);
        snapshot.addProperty("npcUuid", villager.getUUID().toString());
        snapshot.addProperty("npcName", villager.getName().getString());
        JsonObject inventory = new JsonObject();
        inventoryMultiset(villager).forEach(inventory::addProperty);
        snapshot.add("inventory", inventory);
        snapshot.addProperty("nonEmptyStacks", inventoryMultiset(villager).size());
        snapshot.addProperty("liveEntityCount", countLiveLifecycleEntities(server));
        snapshot.addProperty("portableGraveConsumed", portableGraveConsumed);
        return snapshot;
    }

    private static void verifySnapshot(JsonObject snapshot, String expectedPhase) {
        if (snapshot.get("schema").getAsInt() != 1) {
            throw new IllegalStateException("lifecycle evidence schema changed");
        }
        if (!expectedPhase.equals(snapshot.get("phase").getAsString())) {
            throw new IllegalStateException("lifecycle evidence phase changed");
        }
        if (!LIFECYCLE_NPC_ID.toString().equals(snapshot.get("npcUuid").getAsString())) {
            throw new IllegalStateException("lifecycle evidence UUID changed");
        }
        if (!LIFECYCLE_NPC_NAME.equals(snapshot.get("npcName").getAsString())) {
            throw new IllegalStateException("lifecycle evidence name changed");
        }
        JsonObject inventory = snapshot.getAsJsonObject("inventory");
        Map<String, Integer> actualInventory = new TreeMap<>();
        inventory.entrySet().forEach(entry ->
                actualInventory.put(entry.getKey(), entry.getValue().getAsInt())
        );
        if (!LIFECYCLE_INVENTORY.equals(actualInventory)) {
            throw new IllegalStateException("lifecycle evidence inventory changed");
        }
        if (snapshot.get("nonEmptyStacks").getAsInt() != LIFECYCLE_INVENTORY.size()) {
            throw new IllegalStateException("lifecycle evidence stack count changed");
        }
        if (snapshot.get("liveEntityCount").getAsInt() != 1) {
            throw new IllegalStateException("lifecycle evidence live entity count changed");
        }
        if (!snapshot.get("portableGraveConsumed").getAsBoolean()) {
            throw new IllegalStateException("lifecycle portable grave was not consumed");
        }
    }

    private static JsonArray requiredHistory(JsonObject root) {
        if (!root.has("history") || !root.get("history").isJsonArray()) {
            throw new IllegalStateException("lifecycle evidence history is missing");
        }
        return root.getAsJsonArray("history");
    }

    private static JsonObject readLifecycleEvidence(Path path) {
        try {
            JsonObject root = GSON.fromJson(Files.readString(path), JsonObject.class);
            if (root == null) {
                throw new IllegalStateException("lifecycle evidence is empty");
            }
            return root;
        } catch (IOException exception) {
            throw new IllegalStateException("cannot read lifecycle evidence", exception);
        }
    }

    private static void writeLifecycleEvidence(Path path, JsonObject root) {
        try {
            Files.createDirectories(path.getParent());
            Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
            Files.writeString(temporary, GSON.toJson(root) + System.lineSeparator());
            try {
                Files.move(
                        temporary,
                        path,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING
                );
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("cannot write lifecycle evidence", exception);
        }
    }

    private static void verifyPersistentStores(Path worldRoot) {
        Path livingWorld = worldRoot.resolve("livingworld");
        for (String basename : List.of(
                "memory.json",
                "memory2.json",
                "semantic-memory.json",
                "relationships.json",
                "voices.json",
                "operator-lore.json"
        )) {
            Path file = livingWorld.resolve(basename);
            if (!Files.isRegularFile(file)) {
                throw new IllegalStateException("missing fixture store " + basename);
            }
        }
    }

    private static void verifyCanonicalFiles(Path worldRoot) {
        verifyPersistentStores(worldRoot);
        Path lifecycle = worldRoot.resolve("livingworld/acceptance-lifecycle.json");
        if (!Files.isRegularFile(lifecycle)) {
            throw new IllegalStateException("missing fixture store acceptance-lifecycle.json");
        }
    }

    private enum Phase {
        SPAWN,
        KILL,
        WAIT_FOR_REMOVAL,
        RESTORE,
        VERIFY_RESTORED,
        VERIFY_RESTART
    }

    private static final class LifecycleRun {
        private static final int MAX_TICKS = 800;

        private final MinecraftServer server;
        private final Path worldRoot;
        private final Path evidencePath;
        private final ServerLevel level;
        private final BlockPos capturePos;
        private final BlockPos restorePos;
        private Phase phase;
        private int ticks;
        private ItemStack portableGrave = ItemStack.EMPTY;

        private LifecycleRun(MinecraftServer server, Path worldRoot) {
            this.server = server;
            this.worldRoot = worldRoot;
            this.evidencePath = worldRoot.resolve("livingworld/acceptance-lifecycle.json");
            this.level = server.overworld();
            BlockPos origin = level.getSharedSpawnPos().offset(6, 0, 6);
            this.capturePos = origin;
            this.restorePos = origin.offset(3, 0, 0);
            this.phase = Files.exists(evidencePath) ? Phase.VERIFY_RESTART : Phase.SPAWN;
        }

        private boolean tick() {
            ticks++;
            if (ticks > MAX_TICKS) {
                throw new IllegalStateException(
                        "lifecycle fixture exceeded " + MAX_TICKS + " server ticks in phase " + phase
                );
            }
            return switch (phase) {
                case SPAWN -> spawn();
                case KILL -> kill();
                case WAIT_FOR_REMOVAL -> waitForRemoval();
                case RESTORE -> restore();
                case VERIFY_RESTORED -> verifyRestored();
                case VERIFY_RESTART -> verifyRestart();
            };
        }

        private boolean spawn() {
            if (hasRegisteredLifecycleIdentity(server)) {
                throw new IllegalStateException("lifecycle UUID already exists before fixture spawn");
            }
            prepareTombstone(level, capturePos);
            VillagerEntityMCA villager = createLifecycleVillager(level);
            villager.setPos(
                    capturePos.getX() + 0.5D,
                    capturePos.getY(),
                    capturePos.getZ() + 1.5D
            );
            if (!level.addFreshEntity(villager)) {
                throw new IllegalStateException("lifecycle fixture villager could not enter the level");
            }
            verifyLifecycleEntity(server, villager);
            phase = Phase.KILL;
            return false;
        }

        private boolean kill() {
            VillagerEntityMCA villager = requireLifecycleEntity(server);
            if (!villager.hurt(level.damageSources().genericKill(), Float.MAX_VALUE)) {
                throw new IllegalStateException("lifecycle fixture death path rejected fatal damage");
            }
            TombstoneBlock.Data captured = requireTombstoneData(level, capturePos);
            if (!captured.hasEntity()) {
                throw new IllegalStateException("lifecycle fixture death was not captured by the tombstone");
            }
            portableGrave = new ItemStack(BlocksMCA.UPRIGHT_HEADSTONE.asItem());
            captured.writeToStack(portableGrave);
            if (portableGrave.isEmpty()) {
                throw new IllegalStateException("lifecycle portable grave was not created");
            }
            phase = Phase.WAIT_FOR_REMOVAL;
            return false;
        }

        private boolean waitForRemoval() {
            if (hasRegisteredLifecycleIdentity(server)) {
                return false;
            }
            if (countLooseFixtureInventory(level, capturePos) != 0) {
                throw new IllegalStateException("captured lifecycle inventory was also emitted as loose drops");
            }
            phase = Phase.RESTORE;
            return false;
        }

        private boolean restore() {
            level.setBlockAndUpdate(capturePos, Blocks.AIR.defaultBlockState());
            prepareTombstone(level, restorePos);
            TombstoneBlock.Data portableData = requireTombstoneData(level, restorePos);
            portableData.readFromStack(portableGrave);
            if (!portableData.hasEntity()) {
                throw new IllegalStateException("portable lifecycle grave did not retain the NPC data");
            }
            portableData.startResurrecting(false);
            phase = Phase.VERIFY_RESTORED;
            return false;
        }

        private boolean verifyRestored() {
            if (!hasRegisteredLifecycleIdentity(server)) {
                return false;
            }
            TombstoneBlock.Data portableData = requireTombstoneData(level, restorePos);
            if (portableData.hasEntity()) {
                throw new IllegalStateException("portable lifecycle grave was not consumed exactly once");
            }
            VillagerEntityMCA restored = requireLifecycleEntity(server);
            verifyLifecycleEntity(server, restored);
            level.setBlockAndUpdate(restorePos, Blocks.AIR.defaultBlockState());

            JsonObject created = lifecycleSnapshot(server, restored, "CREATED", true);
            JsonArray history = new JsonArray();
            history.add(created.deepCopy());
            JsonObject root = created.deepCopy();
            root.add("history", history);
            writeLifecycleEvidence(evidencePath, root);
            verifyCanonicalFiles(worldRoot);
            LOGGER.info("{}", LIFECYCLE_CREATED_MARKER);
            LOGGER.info("{}", READY_MARKER);
            return true;
        }

        private boolean verifyRestart() {
            JsonObject root = readLifecycleEvidence(evidencePath);
            JsonArray history = requiredHistory(root);
            VillagerEntityMCA restored = requireLifecycleEntity(server);
            verifyLifecycleEntity(server, restored);

            if (history.size() == 1) {
                JsonObject created = history.get(0).getAsJsonObject();
                verifySnapshot(created, "CREATED");
                JsonObject restarted = lifecycleSnapshot(
                        server,
                        restored,
                        "RESTART_VERIFIED",
                        true
                );
                JsonArray completedHistory = new JsonArray();
                completedHistory.add(created.deepCopy());
                completedHistory.add(restarted.deepCopy());
                JsonObject completed = restarted.deepCopy();
                completed.add("history", completedHistory);
                writeLifecycleEvidence(evidencePath, completed);
            } else if (history.size() == 2) {
                verifySnapshot(history.get(0).getAsJsonObject(), "CREATED");
                verifySnapshot(history.get(1).getAsJsonObject(), "RESTART_VERIFIED");
                verifySnapshot(root, "RESTART_VERIFIED");
            } else {
                throw new IllegalStateException("lifecycle history must contain one or two snapshots");
            }

            verifyCanonicalFiles(worldRoot);
            LOGGER.info("{}", LIFECYCLE_RESTART_MARKER);
            LOGGER.info("{}", READY_MARKER);
            return true;
        }
    }
}
