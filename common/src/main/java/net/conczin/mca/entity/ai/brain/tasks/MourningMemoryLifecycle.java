package net.conczin.mca.entity.ai.brain.tasks;

import net.conczin.mca.entity.VillagerEntityMCA;
import net.conczin.mca.entity.ai.MemoryModuleTypeMCA;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

/**
 * Centralizes bounded retry timing and stale path-memory cleanup for mourning.
 */
public final class MourningMemoryLifecycle {
    private static final long GRIEVE_COOLDOWN = 24_000L * 7L;
    private static final long GRIEVE_RETRY_DELAY = 1_200L;

    private MourningMemoryLifecycle() {
    }

    public static void retryLater(VillagerEntityMCA villager) {
        long now = villager.level().getGameTime();
        villager.getBrain().setMemory(
                MemoryModuleTypeMCA.LAST_GRIEVE,
                MourningPolicy.retryTimestamp(now, GRIEVE_COOLDOWN, GRIEVE_RETRY_DELAY)
        );
    }

    public static void clearAfterResurrection(VillagerEntityMCA villager) {
        villager.getBrain().eraseMemory(MemoryModuleTypeMCA.LAST_GRIEVE);
        villager.getBrain().eraseMemory(MemoryModuleTypeMCA.MOURNING_SITE);
        villager.getBrain().eraseMemory(MemoryModuleTypeMCA.MOURNING_POSITION);
        villager.getBrain().eraseMemory(MemoryModuleType.PATH);
        villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        villager.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
        villager.getBrain().eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
    }
}
