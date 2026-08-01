package net.conczin.mca.entity.ai.brain.tasks;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import net.conczin.mca.Config;
import net.conczin.mca.entity.VillagerEntityMCA;
import net.conczin.mca.entity.ai.MemoryModuleTypeMCA;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

/**
 * Final server-side grieving activity, isolated from client/editor changes in the source commit.
 */
public final class MourningTaskPackage {
    private static final float GRIEVING_WALK_SPEED = 0.5F;
    private static final int GRIEVING_PATH_TIMEOUT = 1_200;

    private MourningTaskPackage() {
    }

    public static ImmutableList<Pair<Integer, ? extends BehaviorControl<? super VillagerEntityMCA>>> create() {
        MournAtGraveTask mournAtGrave = new MournAtGraveTask();
        return ImmutableList.of(
                Pair.of(2, ExtendedWalkTowardsTask.create(
                        MemoryModuleTypeMCA.MOURNING_POSITION,
                        GRIEVING_WALK_SPEED,
                        0,
                        Config.getInstance().getVillagerPathfindingDistance(),
                        GRIEVING_PATH_TIMEOUT,
                        villager -> true,
                        villager -> { },
                        villager -> !mournAtGrave.hasArrived()
                )),
                Pair.of(0, new SequenceTask<>(
                        ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT),
                        ImmutableList.of(
                                new EnterGraveyardTask(GRIEVING_WALK_SPEED),
                                mournAtGrave,
                                new LambdaTask<>(villager -> finishMourning(villager, mournAtGrave))
                        )
                ))
        );
    }

    private static void finishMourning(
            VillagerEntityMCA villager,
            MournAtGraveTask mournAtGrave
    ) {
        boolean hadAssignedSite = villager.getBrain()
                .getMemoryInternal(MemoryModuleTypeMCA.MOURNING_SITE)
                .isPresent();
        boolean targetStillMournable = EnterGraveyardTask.hasMournableSite(villager);
        boolean periodicCandidateStillExists = EnterGraveyardTask.hasPeriodicMourningCandidate(villager);

        MourningPolicy.Outcome outcome = MourningPolicy.outcome(
                mournAtGrave.hasCompleted(),
                hadAssignedSite,
                targetStillMournable,
                periodicCandidateStillExists
        );

        villager.getBrain().eraseMemory(MemoryModuleTypeMCA.MOURNING_SITE);
        villager.getBrain().eraseMemory(MemoryModuleTypeMCA.MOURNING_POSITION);
        if (outcome == MourningPolicy.Outcome.COMPLETE) {
            villager.getVillagerBrain().justGrieved();
        } else {
            villager.getVillagerBrain().retryGrievingLater();
        }
        villager.getBrain().updateActivityFromSchedule(
                villager.level().getDayTime(),
                villager.level().getGameTime()
        );
    }
}
