package net.conczin.mca.entity.ai.navigation;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.phys.Vec3;

public class MCAGroundPathNavigation extends GroundPathNavigation {
    private static final int MAX_WATER_SURFACE_SCAN = 16;

    public MCAGroundPathNavigation(Mob mob, Level level) {
        super(mob, level);
    }

    @Override
    protected PathFinder createPathFinder(int maxVisitedNodes) {
        this.nodeEvaluator = new MCAWalkNodeEvaluator();
        this.nodeEvaluator.setCanPassDoors(true);
        this.nodeEvaluator.setCanOpenDoors(true);
        return new PathFinder(this.nodeEvaluator, maxVisitedNodes);
    }

    @Override
    protected Vec3 getTempMobPos() {
        return new Vec3(this.mob.getX(), getWaterAwareSurfaceY(), this.mob.getZ());
    }

    private int getWaterAwareSurfaceY() {
        int startY = this.mob.getBlockY();
        int fallbackY = Mth.floor(this.mob.getY() + 0.5D);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(
                this.mob.getX(), startY, this.mob.getZ()
        );

        return NavigationWaterSurfacePolicy.findSurfaceY(
                this.mob.isInWater(),
                this.canFloat(),
                startY,
                fallbackY,
                y -> this.level.getFluidState(pos.setY(y)).is(FluidTags.WATER),
                MAX_WATER_SURFACE_SCAN
        );
    }
}
