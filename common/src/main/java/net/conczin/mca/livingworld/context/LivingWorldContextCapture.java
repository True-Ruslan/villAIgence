package net.conczin.mca.livingworld.context;

import net.conczin.mca.Config;
import net.conczin.mca.MCA;
import net.conczin.mca.entity.VillagerEntityMCA;
import net.conczin.mca.entity.ai.Messenger;
import net.conczin.mca.entity.ai.Relationship;
import net.conczin.mca.entity.ai.chatAI.TriggerCommandInfos;
import net.conczin.mca.entity.ai.chatAI.modules.EnvironmentModule;
import net.conczin.mca.entity.ai.chatAI.modules.PersonalityModule;
import net.conczin.mca.entity.ai.chatAI.modules.PlayerModule;
import net.conczin.mca.entity.ai.chatAI.modules.RelationModule;
import net.conczin.mca.entity.ai.chatAI.modules.TraitsModule;
import net.conczin.mca.entity.ai.chatAI.modules.VillageModule;
import net.conczin.mca.entity.ai.relationship.AgeState;
import net.conczin.mca.livingworld.LivingWorldConfig;
import net.conczin.mca.livingworld.knowledge.WorldEvent;
import net.conczin.mca.livingworld.knowledge.WorldEventStore;
import net.conczin.mca.livingworld.relationship.LivingWorldRelationshipActionPolicy;
import net.conczin.mca.livingworld.relationship.LivingWorldRelationshipState;
import net.conczin.mca.livingworld.relationship.LivingWorldRelationshipStore;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Captures all mutable Minecraft facts needed by the direct LivingWorld/OpenAI prompt. */
public final class LivingWorldContextCapture {
    private static final int MAX_ARMOR_FACTS = 8;
    private static final int MAX_INVENTORY_FACTS = 12;
    private static final int MAX_STACK_EXPANSION = 64;

    private LivingWorldContextCapture() {
    }

    /** Must be called on the Minecraft server thread. */
    public static LivingWorldContextSnapshot capture(ServerPlayer player, VillagerEntityMCA villager) {
        LivingWorldConfig livingWorld = LivingWorldConfig.getInstance();
        List<String> context = new ArrayList<>();
        PersonalityModule.apply(context, villager, player);
        TraitsModule.apply(context, villager, player);
        RelationModule.apply(context, villager, player);
        VillageModule.apply(context, villager, player);
        EnvironmentModule.apply(context, villager, player);
        PlayerModule.apply(context, villager, player);

        BlockPos position = villager.blockPosition();
        String dimension = villager.level().dimension().location().toString();
        String biome = villager.level().getBiome(position).unwrapKey()
                .map(key -> key.location().toString())
                .orElse("unknown");
        long gameTime = villager.level().getGameTime();
        Path worldRoot = player.serverLevel().getServer().getWorldPath(LevelResource.ROOT);

        List<String> worldFacts = new ArrayList<>();
        worldFacts.add("Observed dimension: " + dimension + ".");
        worldFacts.add("Observed villager position: " + position.getX() + ", " + position.getY() + ", " + position.getZ() + ".");
        worldFacts.add("Observed biome: " + biome + ".");
        worldFacts.add("Observed time: " + (villager.level().isNight() ? "night" : "day") + ".");
        worldFacts.add("Observed weather: " + weather(villager) + ".");
        worldFacts.add("Player main hand: " + itemId(player.getMainHandItem()) + ".");

        List<String> armorIds = new ArrayList<>();
        for (ItemStack stack : player.getArmorSlots()) addStackCopies(armorIds, stack);
        List<String> armor = WorldFactFormatter.summarizeItems(armorIds, MAX_ARMOR_FACTS);
        if (!armor.isEmpty()) worldFacts.add("Observed player armor: " + String.join(", ", armor) + ".");

        List<String> inventoryIds = new ArrayList<>();
        SimpleContainer inventory = villager.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) addStackCopies(inventoryIds, inventory.getItem(i));
        List<String> inventoryFacts = WorldFactFormatter.summarizeItems(inventoryIds, MAX_INVENTORY_FACTS);
        if (!inventoryFacts.isEmpty()) {
            worldFacts.add("Observed villager inventory (bounded): " + String.join(", ", inventoryFacts) + ".");
        }

        addRecentEventFacts(worldFacts, worldRoot, dimension, position, gameTime);
        LivingWorldRelationshipState relationshipState = loadRelationshipState(livingWorld, worldFacts, worldRoot, villager, player);

        List<LivingWorldContextSnapshot.ActionDescriptor> actions = Config.getInstance().villagerChatAIUseTools
                ? TriggerCommandInfos.triggerCommands.stream()
                .filter(command -> command.isActive == null || command.isActive.test(player, villager))
                .filter(command -> !livingWorld.relationshipStateEnabled
                        || LivingWorldRelationshipActionPolicy.isAllowed(command.command, relationshipState))
                .map(command -> new LivingWorldContextSnapshot.ActionDescriptor(command.command, command.description))
                .toList()
                : List.of();

        AgeState age = villager.getAgeState();
        boolean child = age == AgeState.BABY || age == AgeState.TODDLER || age == AgeState.CHILD;

        return new LivingWorldContextSnapshot(
                player.getUUID(),
                villager.getUUID(),
                Messenger.getName(player),
                villager.getName().getString(),
                context,
                worldFacts,
                actions,
                player.serverLevel().getSeed(),
                gameTime,
                worldRoot,
                child,
                Relationship.IS_RELATIVE.test(villager, player),
                MCA.language
        );
    }

    private static LivingWorldRelationshipState loadRelationshipState(
            LivingWorldConfig config,
            List<String> worldFacts,
            Path worldRoot,
            VillagerEntityMCA villager,
            ServerPlayer player
    ) {
        if (!config.relationshipStateEnabled) return LivingWorldRelationshipState.NEUTRAL;
        try {
            LivingWorldRelationshipState state = LivingWorldRelationshipStore.forWorld(worldRoot)
                    .get(villager.getUUID(), player.getUUID());
            worldFacts.add(state.factualSummary());
            return state;
        } catch (RuntimeException e) {
            MCA.LOGGER.warn("Unable to load LivingWorld relationship state for villager {} and player {}",
                    villager.getUUID(), player.getUUID(), e);
            return LivingWorldRelationshipState.NEUTRAL;
        }
    }

    private static void addRecentEventFacts(List<String> worldFacts, Path worldRoot, String dimension, BlockPos position, long gameTime) {
        LivingWorldConfig config = LivingWorldConfig.getInstance();
        if (!config.eventMemoryEnabled) return;
        try {
            List<WorldEvent> events = WorldEventStore.forWorld(worldRoot).queryRecent(
                    dimension,
                    position.getX(),
                    position.getY(),
                    position.getZ(),
                    gameTime,
                    config.eventMemoryMaxAgeTicks,
                    config.eventContextRadius,
                    config.eventContextMaxEvents
            );
            for (WorldEvent event : events) {
                worldFacts.add("Observed recent local event: " + event.description());
            }
        } catch (RuntimeException e) {
            MCA.LOGGER.warn("Unable to load LivingWorld recent event context for villager snapshot", e);
        }
    }

    private static String weather(VillagerEntityMCA villager) {
        if (villager.level().isThundering()) return "thunderstorm";
        if (villager.level().isRaining()) return "rain";
        return "clear";
    }

    private static void addStackCopies(List<String> output, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        String id = itemId(stack);
        int copies = Math.min(MAX_STACK_EXPANSION, Math.max(1, stack.getCount()));
        for (int i = 0; i < copies; i++) output.add(id);
    }

    private static String itemId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "empty";
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }
}
