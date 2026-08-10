package net.conczin.mca.livingworld.memory2;

import net.conczin.mca.entity.VillagerEntityMCA;
import net.minecraft.server.level.ServerLevel;

import java.util.Objects;
import java.util.UUID;

/** Live server-world implementation of the NPC identity authority boundary. */
public final class ServerLevelNpcIdentityAuthority implements NpcIdentityAuthority {
    private final ServerLevel level;

    public ServerLevelNpcIdentityAuthority(ServerLevel level) {
        this.level = Objects.requireNonNull(level, "level");
    }

    @Override
    public boolean isNpc(UUID id) {
        return id != null && level.getEntity(id) instanceof VillagerEntityMCA;
    }
}
