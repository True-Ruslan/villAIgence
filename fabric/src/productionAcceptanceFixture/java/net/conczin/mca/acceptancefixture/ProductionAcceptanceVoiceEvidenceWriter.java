package net.conczin.mca.acceptancefixture;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Writes deterministic real-codec evidence and fails closed before lifecycle readiness. */
public final class ProductionAcceptanceVoiceEvidenceWriter implements ModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(
            "VillAIgenceVoiceTransportAcceptance"
    );
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int MAX_PENDING_TICKS = 200;

    private int pendingTicks;
    private boolean completed;

    @Override
    public void onInitialize() {
        if ("recovery".equals(System.getProperty("villaigence.acceptance.mode"))) {
            completed = true;
            return;
        }
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (completed) {
                return;
            }
            Throwable failure = ProductionAcceptanceVoiceTransportState.failure();
            if (failure != null) {
                completed = true;
                throw new IllegalStateException(
                        "real Simple Voice Chat transport acceptance failed",
                        failure
                );
            }
            ProductionAcceptanceVoiceTransportState.Report report =
                    ProductionAcceptanceVoiceTransportState.report();
            if (report == null) {
                pendingTicks++;
                if (pendingTicks > MAX_PENDING_TICKS) {
                    completed = true;
                    throw new IllegalStateException(
                            "Simple Voice Chat transport acceptance did not complete within "
                                    + MAX_PENDING_TICKS + " server ticks"
                    );
                }
                return;
            }

            Path worldRoot = server.getWorldPath(LevelResource.ROOT);
            Path evidence = worldRoot.resolve(
                    "livingworld/acceptance-voice-transport.json"
            );
            writeAtomically(evidence, GSON.toJson(report.toJson()) + System.lineSeparator());
            LOGGER.info("{}", ProductionAcceptanceVoiceTransportState.PASS_MARKER);
            completed = true;
        });
    }

    private static void writeAtomically(Path path, String value) {
        try {
            Files.createDirectories(path.getParent());
            Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
            Files.writeString(temporary, value);
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
            throw new IllegalStateException("cannot write voice transport evidence", exception);
        }
    }
}
