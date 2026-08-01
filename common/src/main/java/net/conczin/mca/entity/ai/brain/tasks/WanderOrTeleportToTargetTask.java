package net.conczin.mca.entity.ai.brain.tasks;

import net.conczin.mca.Config;
import net.conczin.mca.entity.ai.navigation.PathfindingBlacklist;
import net.conczin.mca.entity.ai.navigation.PathfindingProgressTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.MoveToTargetSink;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

import java.util.Map;
import java.util.WeakHashMap;

public class WanderOrTeleportToTargetTask extends MoveToTargetSink {
    // Keep the historical seven-check cadence, but distribute initial checks by entity id.
    private static final int PATHFINDING_INTERVAL = 7;
    // Replan only after sustained lack of meaningful movement. This avoids expensive path churn on short pauses.
    private static final int STUCK_THRESHOLD_TICKS = 20 * 8;
    private static final double MIN_PROGRESS_BLOCKS = 0.5D;

    private final Map<Mob, PathfindingProgressTracker> progressTrackers = new WeakHashMap<>();
    private final Map<Mob, Integer> pathfindingCooldowns = new WeakHashMap<>();

    public WanderOrTeleportToTargetTask() {
        // nop
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel serverWorld, Mob mobEntity) {
        int cooldown = pathfindingCooldowns.computeIfAbsent(
                mobEntity,
                entity -> PathfindingSchedulePolicy.initialCooldown(entity.getId(), PATHFINDING_INTERVAL)
        );
        PathfindingSchedulePolicy.Decision decision = PathfindingSchedulePolicy.tick(
                cooldown,
                PATHFINDING_INTERVAL
        );
        pathfindingCooldowns.put(mobEntity, decision.nextCooldown());
        return decision.shouldRun() && super.checkExtraStartConditions(serverWorld, mobEntity);
    }

    @Override
    protected void tick(ServerLevel world, Mob entity, long l) {
        entity.getBrain().getMemoryInternal(MemoryModuleType.WALK_TARGET).ifPresentOrElse(walkTarget -> {
            BlockPos targetPos = walkTarget.getTarget().currentBlockPosition();
            PathfindingProgressTracker tracker = progressTrackers.computeIfAbsent(
                    entity,
                    ignored -> new PathfindingProgressTracker(STUCK_THRESHOLD_TICKS, MIN_PROGRESS_BLOCKS)
            );

            if (tracker.update(
                    targetPos.getX(), targetPos.getY(), targetPos.getZ(),
                    entity.getX(), entity.getY(), entity.getZ()
            )) {
                // Keep the WALK_TARGET intent, but invalidate/recompute the navigation path. This lets vanilla/MCA
                // choose an alternative route without a tight pathfinding loop or an unconditional teleport.
                entity.getNavigation().recomputePath();
            }

            if (Config.getInstance().allowVillagerTeleporting
                    && !targetPos.closerToCenterThan(entity.position(), Config.getInstance().villagerMinTeleportationDistance)) {
                tryTeleport(world, entity, targetPos);
            }
        }, () -> {
            progressTrackers.remove(entity);
            pathfindingCooldowns.remove(entity);
        });

        super.tick(world, entity, l);
    }

    private void tryTeleport(ServerLevel world, Mob entity, BlockPos targetPos) {
        for (int i = 0; i < 10; ++i) {
            int j = this.getRandomInt(entity, -3, 3);
            int k = this.getRandomInt(entity, -1, 1);
            int l = this.getRandomInt(entity, -3, 3);
            boolean bl = this.tryTeleportTo(world, entity, targetPos, targetPos.getX() + j, targetPos.getY() + k, targetPos.getZ() + l);
            if (bl) {
                return;
            }
        }
    }

    private boolean tryTeleportTo(ServerLevel world, Mob entity, BlockPos targetPos, int x, int y, int z) {
        if (Math.abs((double) x - targetPos.getX()) < 2.0D && Math.abs((double) z - targetPos.getZ()) < 2.0D) {
            return false;
        } else if (!this.canTeleportTo(world, entity, new BlockPos(x, y, z))) {
            return false;
        } else {
            entity.teleportTo((double) x + 0.5D, y, (double) z + 0.5D);
            return true;
        }
    }

    private boolean canTeleportTo(ServerLevel world, Mob entity, BlockPos pos) {
        PathType pathNodeType = WalkNodeEvaluator.getPathTypeStatic(entity, pos.mutable());
        if (pathNodeType != PathType.WALKABLE) {
            return false;
        } else {
            if (!isAreaSafe(world, pos.below())) {
                return false;
            } else {
                BlockPos blockPos = pos.subtract(entity.blockPosition());
                return world.noCollision(entity, entity.getBoundingBox().move(blockPos));
            }
        }
    }

    private int getRandomInt(Mob entity, int min, int max) {
        return entity.getRandom().nextInt(max - min + 1) + min;
    }

    private boolean isAreaSafe(ServerLevel world, BlockPos pos) {
        // The following conditions define whether it is logically
        // safe for the entity to teleport to the specified pos within world
        return !PathfindingBlacklist.isBlocked(world.getBlockState(pos));
    }
}
