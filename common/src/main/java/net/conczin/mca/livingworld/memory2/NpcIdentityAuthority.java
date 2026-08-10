package net.conczin.mca.livingworld.memory2;

import java.util.UUID;

/** Server-owned boundary that confirms whether a UUID currently represents an eligible MCA NPC. */
@FunctionalInterface
public interface NpcIdentityAuthority {
    boolean isNpc(UUID id);
}
