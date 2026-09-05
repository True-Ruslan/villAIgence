package net.conczin.mca.livingworld.memory2;

import net.conczin.mca.livingworld.relationship.LivingWorldRelationshipStore;
import net.conczin.mca.livingworld.relationship.NpcSocialGraphStore;
import net.conczin.mca.livingworld.voice.PersistentNpcVoiceStore;

import java.nio.file.Path;
import java.util.UUID;

/**
 * Server-owned cleanup boundary for one NPC's permanent removal from persisted Living World
 * state. Every store below is keyed by NPC UUID and otherwise never drops an entry once written,
 * so a world where NPCs are repeatedly born and die would grow these files without bound.
 */
public final class NpcRemovalLifecycle {
    private NpcRemovalLifecycle() {
    }

    public static void purge(Path worldRoot, UUID npcId) {
        if (worldRoot == null || npcId == null) return;

        NpcSocialGraphStore.forWorld(worldRoot).removeNpc(npcId);
        LivingWorldRelationshipStore.forWorld(worldRoot).removeNpc(npcId);
        MemoryEventStore.forWorld(worldRoot).removeNpc(npcId);
        SemanticMemoryStore.forWorld(worldRoot).removeNpc(npcId);
        PersistentNpcVoiceStore.forWorld(worldRoot).removeNpc(npcId);
    }
}
