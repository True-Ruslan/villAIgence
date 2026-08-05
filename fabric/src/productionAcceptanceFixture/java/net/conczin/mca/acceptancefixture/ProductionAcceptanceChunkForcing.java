package net.conczin.mca.acceptancefixture;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

import java.util.LinkedHashSet;
import java.util.Set;

/** Keeps the isolated lifecycle fixture chunks ticking without a connected player. */
public final class ProductionAcceptanceChunkForcing implements ModInitializer {
    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> setForced(server, true));
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> setForced(server, false));
    }

    private static void setForced(MinecraftServer server, boolean forced) {
        ServerLevel level = server.overworld();
        BlockPos capture = level.getSharedSpawnPos().offset(6, 0, 6);
        BlockPos restore = capture.offset(3, 0, 0);
        Set<ChunkPos> chunks = new LinkedHashSet<>();
        chunks.add(new ChunkPos(capture));
        chunks.add(new ChunkPos(restore));
        for (ChunkPos chunk : chunks) {
            level.setChunkForced(chunk.x, chunk.z, forced);
        }
    }
}
