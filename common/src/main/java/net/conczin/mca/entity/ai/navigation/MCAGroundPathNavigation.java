package net.conczin.mca.entity.ai.navigation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.phys.Vec3;

public class MCAGroundPathNavigation extends GroundPathNavigation {
    private static final int MAX_WATER_SURFACE_SCAN = 16;
    private static final double CLIMB_VERTICAL_SPEED = 0.16D;
    private static final double CLIMB_HORIZONTAL_SPEED = 0.12D;
    private static final double CLIMB_HORIZONTAL_GAIN = 0.35D;
    private static final double LADDER_ENTRY_OFFSET = 0.1D;
    private static final double ASCENT_NODE_TOLERANCE = 0.20D;
    private static final double DESCENT_NODE_TOLERANCE = 0.08D;
    private static final double EXIT_HEIGHT_TOLERANCE = 0.25D;
    private static final double EXIT_VERTICAL_BIAS = 0.08D;

    public MCAGroundPathNavigation(Mob mob, Level level) {
        super(mob, level);
    }

    public boolean isControllingClimbable() {
        return getClimbContext() != null;
    }

    @Override
    protected PathFinder createPathFinder(int maxVisitedNodes) {
        this.nodeEvaluator = new MCAWalkNodeEvaluator();
        this.nodeEvaluator.setCanPassDoors(true);
        this.nodeEvaluator.setCanOpenDoors(true);
        return new PathFinder(this.nodeEvaluator, maxVisitedNodes);
    }

    public int mca$getWaterAwareSurfaceY() {
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

    @Override
    protected boolean canUpdatePath() {
        return super.canUpdatePath() || this.mob.onClimbable();
    }

    @Override
    public void tick() {
        super.tick();
        ClimbContext context = getClimbContext();
        if (context != null) {
            applyClimbableMotion(context);
        }
    }

    @Override
    protected void followThePath() {
        if (this.path == null || this.path.isDone()) {
            return;
        }

        ClimbContext context = getClimbContext();
        if (context == null) {
            super.followThePath();
            return;
        }

        Vec3 position = this.getTempMobPos();
        if (context.pathTargetsClimbable() && this.mob.onClimbable()) {
            double tolerance = context.verticalDirection() < 0
                    ? DESCENT_NODE_TOLERANCE
                    : ASCENT_NODE_TOLERANCE;
            if (ClimbNavigationPolicy.hasReachedHeight(
                    this.mob.getY(),
                    context.targetNode().y,
                    context.verticalDirection(),
                    tolerance
            )) {
                this.path.advance();
            }
        }

        this.doStuckDetection(position);
    }

    @Override
    protected double getGroundY(Vec3 position) {
        BlockPos targetPos = BlockPos.containing(position);
        if (isClimbable(targetPos)) {
            return this.mob.getY();
        }
        return super.getGroundY(position);
    }

    private ClimbContext getClimbContext() {
        Path path = this.path;
        if (path == null || path.isDone()) {
            return null;
        }

        int nextNodeIndex = path.getNextNodeIndex();
        if (nextNodeIndex < 0 || nextNodeIndex >= path.getNodeCount()) {
            return null;
        }

        Node nextNode = path.getNode(nextNodeIndex);
        if (isClimbable(nextNode.asBlockPos())) {
            Node followingNode = nextNodeIndex + 1 < path.getNodeCount()
                    ? path.getNode(nextNodeIndex + 1)
                    : null;
            boolean exitsClimbable = followingNode != null
                    && !isClimbable(followingNode.asBlockPos());
            return new ClimbContext(
                    nextNode,
                    exitsClimbable ? followingNode : nextNode,
                    true,
                    exitsClimbable,
                    getVerticalDirection(path, nextNodeIndex)
            );
        }

        if (this.mob.onClimbable() && nextNodeIndex > 0) {
            Node previousNode = path.getNode(nextNodeIndex - 1);
            if (isClimbable(previousNode.asBlockPos())) {
                return new ClimbContext(
                        previousNode,
                        nextNode,
                        false,
                        true,
                        getVerticalDirection(path, nextNodeIndex - 1)
                );
            }
        }

        return null;
    }

    private static int getVerticalDirection(Path path, int climbableNodeIndex) {
        Node climbableNode = path.getNode(climbableNodeIndex);
        Integer followingY = climbableNodeIndex + 1 < path.getNodeCount()
                ? path.getNode(climbableNodeIndex + 1).y
                : null;
        Integer previousY = climbableNodeIndex > 0
                ? path.getNode(climbableNodeIndex - 1).y
                : null;
        return ClimbNavigationPolicy.verticalDirection(climbableNode.y, followingY, previousY);
    }

    private void applyClimbableMotion(ClimbContext context) {
        BlockPos climbablePos = findAttachedClimbable(context.climbableNode().asBlockPos());
        Vec3 anchor = getClimbableAnchor(climbablePos);

        if (!this.mob.onClimbable()) {
            this.mob.getMoveControl().setWantedPosition(
                    anchor.x(), this.mob.getY(), anchor.z(), this.speedModifier
            );
            Vec3 movement = this.mob.getDeltaMovement();
            this.mob.setDeltaMovement(
                    ClimbNavigationPolicy.horizontalVelocity(
                            anchor.x() - this.mob.getX(),
                            CLIMB_HORIZONTAL_GAIN,
                            CLIMB_HORIZONTAL_SPEED
                    ),
                    movement.y(),
                    ClimbNavigationPolicy.horizontalVelocity(
                            anchor.z() - this.mob.getZ(),
                            CLIMB_HORIZONTAL_GAIN,
                            CLIMB_HORIZONTAL_SPEED
                    )
            );
            return;
        }

        double targetY = context.targetNode().y;
        boolean atExitHeight = context.exitsClimbable()
                && ClimbNavigationPolicy.hasReachedHeight(
                        this.mob.getY(),
                        targetY,
                        context.verticalDirection(),
                        EXIT_HEIGHT_TOLERANCE
                );
        double targetX = anchor.x();
        double targetZ = anchor.z();
        if (context.exitsClimbable() && (!context.pathTargetsClimbable() || atExitHeight)) {
            targetX = context.targetNode().x + 0.5D;
            targetZ = context.targetNode().z + 0.5D;
        }

        this.mob.getMoveControl().setWantedPosition(targetX, targetY, targetZ, this.speedModifier);

        double controlledY = ClimbNavigationPolicy.verticalVelocity(
                targetY - this.mob.getY(),
                context.verticalDirection(),
                CLIMB_VERTICAL_SPEED
        );
        controlledY = ClimbNavigationPolicy.applyExitBias(
                controlledY,
                context.verticalDirection(),
                atExitHeight,
                EXIT_VERTICAL_BIAS
        );

        this.mob.setXxa(0.0F);
        this.mob.setZza(0.0F);
        this.mob.setDeltaMovement(
                ClimbNavigationPolicy.horizontalVelocity(
                        targetX - this.mob.getX(),
                        CLIMB_HORIZONTAL_GAIN,
                        CLIMB_HORIZONTAL_SPEED
                ),
                controlledY,
                ClimbNavigationPolicy.horizontalVelocity(
                        targetZ - this.mob.getZ(),
                        CLIMB_HORIZONTAL_GAIN,
                        CLIMB_HORIZONTAL_SPEED
                )
        );
    }

    private Vec3 getClimbableAnchor(BlockPos pos) {
        BlockState state = this.level.getBlockState(pos);
        Vec3 center = Vec3.atCenterOf(pos);
        if (state.getBlock() instanceof LadderBlock) {
            Direction facing = state.getValue(LadderBlock.FACING);
            return center.add(
                    facing.getStepX() * LADDER_ENTRY_OFFSET,
                    0.0D,
                    facing.getStepZ() * LADDER_ENTRY_OFFSET
            );
        }
        return center;
    }

    private BlockPos findAttachedClimbable(BlockPos fallback) {
        BlockPos mobPos = this.mob.blockPosition();
        if (isClimbable(mobPos)) {
            return mobPos;
        }

        BlockPos above = mobPos.above();
        if (isClimbable(above)) {
            return above;
        }

        BlockPos below = mobPos.below();
        if (isClimbable(below)) {
            return below;
        }

        return fallback;
    }

    private boolean isClimbable(BlockPos pos) {
        return this.level.getBlockState(pos).is(BlockTags.CLIMBABLE);
    }

    private record ClimbContext(
            Node climbableNode,
            Node targetNode,
            boolean pathTargetsClimbable,
            boolean exitsClimbable,
            int verticalDirection
    ) {
    }
}
