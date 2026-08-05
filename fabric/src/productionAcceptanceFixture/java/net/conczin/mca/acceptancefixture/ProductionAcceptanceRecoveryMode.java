package net.conczin.mca.acceptancefixture;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Test-only fast mode for destructive persistence recovery acceptance.
 *
 * <p>The normal fixture remains the authoritative two-JVM NPC lifecycle gate.
 * Recovery mode initializes the same six production stores but cancels the
 * unrelated 500-tick tombstone lifecycle before the first server tick.</p>
 */
public final class ProductionAcceptanceRecoveryMode implements ModInitializer {
    public static final String MODE_PROPERTY = "villaigence.acceptance.mode";
    public static final String MODE = "recovery";
    public static final String RECOVERY_READY_MARKER = "VAI-PERSIST-003-RECOVERY-READY";

    private static final Logger LOGGER = LoggerFactory.getLogger(
            "VillAIgenceProductionRecoveryAcceptance"
    );
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
            cancelLifecycle();
            verifyStores(server);
            LOGGER.info("{}", RECOVERY_READY_MARKER);
            LOGGER.info("{}", ProductionAcceptanceFixture.READY_MARKER);
        });
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            if (enabled()) {
                cancelLifecycle();
            }
        });
    }

    private static boolean enabled() {
        return MODE.equals(System.getProperty(MODE_PROPERTY, ""));
    }

    private static void cancelLifecycle() {
        try {
            Field field = ProductionAcceptanceFixture.class.getDeclaredField("activeLifecycle");
            field.setAccessible(true);
            field.set(null, null);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(
                    "Unable to disable unrelated lifecycle fixture in recovery mode",
                    exception
            );
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
