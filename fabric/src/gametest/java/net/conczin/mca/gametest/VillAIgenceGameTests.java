package net.conczin.mca.gametest;

import net.conczin.mca.block.TombstoneBlock;
import net.conczin.mca.entity.VillagerEntityMCA;
import net.conczin.mca.entity.ai.navigation.MCAGroundPathNavigation;
import net.conczin.mca.registry.BlocksMCA;
import net.conczin.mca.registry.EntitiesMCA;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class VillAIgenceGameTests implements FabricGameTest {
    private static final UUID TOMBSTONE_FIXTURE_UUID = UUID.fromString(
            "5ac86058-2caf-47a0-8f65-bf2f13e35472"
    );
    private static final UUID SILK_TOUCH_FIXTURE_UUID = UUID.fromString(
            "8cb8b2b2-f56f-4f5e-a80c-146827553c3f"
    );

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void mcaVillagerRegistryAndNavigationBoot(GameTestHelper helper) {
        VillagerEntityMCA villager = helper.spawn(EntitiesMCA.MALE_VILLAGER, 2, 1, 2);

        helper.assertTrue(villager.isAlive(), "Spawned MCA villager must be alive");
        helper.assertTrue(villager.level() == helper.getLevel(),
                "Spawned MCA villager must belong to the GameTest server level");
        helper.assertTrue(villager.getNavigation() instanceof MCAGroundPathNavigation,
                "MCA villager must use MCAGroundPathNavigation");
        helper.succeed();
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void tombstoneItemRoundTripPreservesNpcIdentityAndInventory(GameTestHelper helper) {
        VillagerEntityMCA original = createTombstoneFixture(
                helper,
                TOMBSTONE_FIXTURE_UUID,
                "Acceptance Basiliso"
        );

        BlockState tombstoneState = BlocksMCA.UPRIGHT_HEADSTONE.defaultBlockState();
        TombstoneBlock.Data sourceData = new TombstoneBlock.Data(
                helper.absolutePos(new BlockPos(2, 1, 2)),
                tombstoneState
        );
        sourceData.setEntity(original);

        ItemStack portableTombstone = new ItemStack(BlocksMCA.UPRIGHT_HEADSTONE.asItem());
        sourceData.writeToStack(portableTombstone);
        helper.assertTrue(portableTombstone.has(DataComponents.BLOCK_ENTITY_DATA),
                "Portable tombstone must contain block-entity data");

        VillagerEntityMCA recreated = recreateTombstoneEntity(
                helper,
                portableTombstone,
                helper.absolutePos(new BlockPos(4, 1, 2)),
                tombstoneState
        );
        assertFixtureIdentityAndInventory(
                helper,
                recreated,
                TOMBSTONE_FIXTURE_UUID,
                "Acceptance Basiliso"
        );
        helper.succeed();
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void filledTombstoneSilkTouchDropIsPortableAndUnique(GameTestHelper helper) {
        VillagerEntityMCA original = createTombstoneFixture(
                helper,
                SILK_TOUCH_FIXTURE_UUID,
                "Acceptance Casimiro"
        );
        TombstoneBlock tombstoneBlock = (TombstoneBlock) BlocksMCA.UPRIGHT_HEADSTONE;
        BlockState tombstoneState = tombstoneBlock.defaultBlockState();
        TombstoneBlock.Data sourceData = new TombstoneBlock.Data(
                helper.absolutePos(new BlockPos(2, 1, 4)),
                tombstoneState
        );
        sourceData.setEntity(original);

        ItemStack silkTouchPickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
        silkTouchPickaxe.enchant(
                helper.getLevel()
                        .registryAccess()
                        .registryOrThrow(Registries.ENCHANTMENT)
                        .getHolderOrThrow(Enchantments.SILK_TOUCH),
                1
        );

        LootParams.Builder lootBuilder = new LootParams.Builder(helper.getLevel())
                .withParameter(
                        LootContextParams.ORIGIN,
                        Vec3.atCenterOf(helper.absolutePos(new BlockPos(2, 1, 4)))
                )
                .withParameter(LootContextParams.TOOL, silkTouchPickaxe)
                .withOptionalParameter(LootContextParams.BLOCK_ENTITY, sourceData);

        List<ItemStack> drops = tombstoneBlock.getDrops(tombstoneState, lootBuilder);
        List<ItemStack> tombstoneDrops = drops.stream()
                .filter(stack -> stack.is(tombstoneBlock.asItem()))
                .toList();

        helper.assertTrue(tombstoneDrops.size() == 1,
                "Filled Silk Touch grave must produce exactly one portable tombstone, found "
                        + tombstoneDrops.size());
        ItemStack portableTombstone = tombstoneDrops.getFirst();
        helper.assertTrue(portableTombstone.has(DataComponents.BLOCK_ENTITY_DATA),
                "Silk Touch tombstone drop must contain stored NPC data");

        VillagerEntityMCA recreated = recreateTombstoneEntity(
                helper,
                portableTombstone,
                helper.absolutePos(new BlockPos(5, 1, 4)),
                tombstoneState
        );
        assertFixtureIdentityAndInventory(
                helper,
                recreated,
                SILK_TOUCH_FIXTURE_UUID,
                "Acceptance Casimiro"
        );
        helper.succeed();
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 600)
    public void twoControlledMobsEscapeIndependentWaterLanes(GameTestHelper helper) {
        buildWaterLane(helper, 1);
        buildWaterLane(helper, 5);

        ControlledNavigationMob first = spawnControlledNavigationMob(helper, 1, 1, 2);
        ControlledNavigationMob second = spawnControlledNavigationMob(helper, 5, 1, 2);

        BlockPos firstTarget = helper.absolutePos(new BlockPos(1, 2, 5));
        BlockPos secondTarget = helper.absolutePos(new BlockPos(5, 2, 5));

        helper.startSequence()
                .thenExecuteAfter(2, () -> helper.assertTrue(
                        startNavigation(first, firstTarget),
                        "First controlled mob must build a path to its dry target: "
                                + navigationState(first, firstTarget)
                ))
                .thenExecute(() -> helper.assertTrue(
                        startNavigation(second, secondTarget),
                        "Second controlled mob must build a path to its dry target: "
                                + navigationState(second, secondTarget)
                ))
                .thenWaitUntil(() -> {
                    helper.assertTrue(first.isAlive(),
                            "First controlled water-lane mob must remain alive");
                    helper.assertTrue(second.isAlive(),
                            "Second controlled water-lane mob must remain alive");
                    helper.assertTrue(!first.isInWater(),
                            "First controlled mob must leave its water lane: "
                                    + navigationState(first, firstTarget));
                    helper.assertTrue(!second.isInWater(),
                            "Second controlled mob must leave its water lane: "
                                    + navigationState(second, secondTarget));
                    helper.assertTrue(isNear(first, firstTarget),
                            "First controlled mob must reach its own dry target: "
                                    + navigationState(first, firstTarget));
                    helper.assertTrue(isNear(second, secondTarget),
                            "Second controlled mob must reach its own dry target: "
                                    + navigationState(second, secondTarget));
                })
                .thenSucceed();
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 800)
    public void waterEscapePreservesLandNavigation(GameTestHelper helper) {
        buildWaterLane(helper, 1);
        for (int x = 1; x <= 6; x++) {
            for (int z = 4; z <= 6; z++) {
                setBlock(helper, new BlockPos(x, 1, z), Blocks.STONE);
            }
        }

        ControlledNavigationMob mob = spawnControlledNavigationMob(helper, 1, 1, 2);
        BlockPos shoreTarget = helper.absolutePos(new BlockPos(1, 2, 5));
        BlockPos landTarget = helper.absolutePos(new BlockPos(6, 2, 5));

        helper.startSequence()
                .thenExecuteAfter(2, () -> helper.assertTrue(
                        startNavigation(mob, shoreTarget),
                        "Controlled mob must build a water-exit path: "
                                + navigationState(mob, shoreTarget)
                ))
                .thenWaitUntil(() -> {
                    helper.assertTrue(mob.isAlive(),
                            "Controlled navigation mob must remain alive during water escape");
                    helper.assertTrue(!mob.isInWater(),
                            "Controlled mob must leave water before the land-navigation phase: "
                                    + navigationState(mob, shoreTarget));
                    helper.assertTrue(isNear(mob, shoreTarget),
                            "Controlled mob must reach the dry shore target before the second phase: "
                                    + navigationState(mob, shoreTarget));
                })
                .thenExecuteAfter(2, () -> helper.assertTrue(
                        startNavigation(mob, landTarget),
                        "Controlled mob must build a post-water dry-land path: "
                                + navigationState(mob, landTarget)
                ))
                .thenWaitUntil(() -> {
                    helper.assertTrue(mob.isAlive(),
                            "Controlled navigation mob must remain alive on land");
                    helper.assertTrue(!mob.isInWater(),
                            "Controlled mob must remain dry during the land-navigation phase: "
                                    + navigationState(mob, landTarget));
                    helper.assertTrue(isNear(mob, landTarget),
                            "Controlled mob must reach the second dry-land target after water escape: "
                                    + navigationState(mob, landTarget));
                })
                .thenSucceed();
    }

    private static VillagerEntityMCA createTombstoneFixture(
            GameTestHelper helper,
            UUID uuid,
            String name
    ) {
        VillagerEntityMCA villager = EntitiesMCA.MALE_VILLAGER.create(helper.getLevel());
        helper.assertTrue(villager != null, "MCA villager fixture must be creatable");
        villager.setUUID(uuid);
        villager.setCustomName(Component.literal(name));
        villager.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 3));
        villager.getInventory().setItem(7, new ItemStack(Items.BREAD, 11));
        villager.getInventory().setItem(26, new ItemStack(Items.IRON_SWORD));
        return villager;
    }

    private static VillagerEntityMCA recreateTombstoneEntity(
            GameTestHelper helper,
            ItemStack portableTombstone,
            BlockPos restoredPos,
            BlockState tombstoneState
    ) {
        TombstoneBlock.Data restoredData = new TombstoneBlock.Data(restoredPos, tombstoneState);
        restoredData.readFromStack(portableTombstone);
        helper.assertTrue(restoredData.hasEntity(),
                "Placed tombstone data must recover a stored entity");

        Optional<Entity> recreatedEntity = restoredData.createEntity(helper.getLevel(), false);
        helper.assertTrue(recreatedEntity.isPresent(),
                "Stored tombstone entity must be reconstructable");
        helper.assertTrue(recreatedEntity.get() instanceof VillagerEntityMCA,
                "Reconstructed entity must remain an MCA villager");
        return (VillagerEntityMCA) recreatedEntity.get();
    }

    private static void assertFixtureIdentityAndInventory(
            GameTestHelper helper,
            VillagerEntityMCA recreated,
            UUID expectedUuid,
            String expectedName
    ) {
        helper.assertTrue(recreated.getUUID().equals(expectedUuid),
                "Tombstone round trip must preserve NPC UUID");
        helper.assertTrue(recreated.getName().getString().equals(expectedName),
                "Tombstone round trip must preserve NPC name");
        assertInventoryCount(helper, recreated, Items.DIAMOND, 3);
        assertInventoryCount(helper, recreated, Items.BREAD, 11);
        assertInventoryCount(helper, recreated, Items.IRON_SWORD, 1);
        helper.assertTrue(nonEmptyStackCount(recreated) == 3,
                "Tombstone round trip must neither lose nor duplicate inventory stacks");
    }

    private static void assertInventoryCount(
            GameTestHelper helper,
            VillagerEntityMCA villager,
            Item expectedItem,
            int expectedCount
    ) {
        int actualCount = 0;
        for (int slot = 0; slot < villager.getInventory().getContainerSize(); slot++) {
            ItemStack stack = villager.getInventory().getItem(slot);
            if (stack.is(expectedItem)) {
                actualCount += stack.getCount();
            }
        }
        helper.assertTrue(actualCount == expectedCount,
                "Inventory must preserve " + expectedCount + " of " + expectedItem
                        + ", found " + actualCount);
    }

    private static int nonEmptyStackCount(VillagerEntityMCA villager) {
        int count = 0;
        for (int slot = 0; slot < villager.getInventory().getContainerSize(); slot++) {
            if (!villager.getInventory().getItem(slot).isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private static ControlledNavigationMob spawnControlledNavigationMob(
            GameTestHelper helper,
            int relativeX,
            int relativeY,
            int relativeZ
    ) {
        ControlledNavigationMob mob = new ControlledNavigationMob(helper.getLevel());
        BlockPos spawnPos = helper.absolutePos(new BlockPos(relativeX, relativeY, relativeZ));
        mob.setPos(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D);
        mob.setPersistenceRequired();
        helper.assertTrue(helper.getLevel().addFreshEntity(mob),
                "Controlled navigation mob must be added to the GameTest server level");
        helper.assertTrue(mob.getNavigation() instanceof MCAGroundPathNavigation,
                "Controlled test mob must use the production MCAGroundPathNavigation");
        return mob;
    }

    private static void buildWaterLane(GameTestHelper helper, int laneX) {
        for (int z = 0; z <= 6; z++) {
            setBlock(helper, new BlockPos(laneX, 0, z), Blocks.STONE);
        }
        for (int z = 1; z <= 3; z++) {
            setBlock(helper, new BlockPos(laneX, 1, z), Blocks.WATER);
        }
        for (int z = 0; z <= 3; z++) {
            setBlock(helper, new BlockPos(laneX - 1, 1, z), Blocks.STONE);
            setBlock(helper, new BlockPos(laneX - 1, 2, z), Blocks.STONE);
            setBlock(helper, new BlockPos(laneX + 1, 1, z), Blocks.STONE);
            setBlock(helper, new BlockPos(laneX + 1, 2, z), Blocks.STONE);
        }
        setBlock(helper, new BlockPos(laneX, 1, 0), Blocks.STONE);
        setBlock(helper, new BlockPos(laneX, 2, 0), Blocks.STONE);
        for (int z = 4; z <= 6; z++) {
            setBlock(helper, new BlockPos(laneX, 1, z), Blocks.STONE);
        }
    }

    private static void setBlock(GameTestHelper helper, BlockPos relativePos, Block block) {
        helper.getLevel().setBlockAndUpdate(
                helper.absolutePos(relativePos),
                block.defaultBlockState()
        );
    }

    private static boolean startNavigation(PathfinderMob mob, BlockPos target) {
        return mob.getNavigation().moveTo(
                target.getX() + 0.5D,
                target.getY(),
                target.getZ() + 0.5D,
                1.1D
        );
    }

    private static String navigationState(PathfinderMob mob, BlockPos target) {
        return "pos=" + mob.position()
                + ", target=" + Vec3.atCenterOf(target)
                + ", velocity=" + mob.getDeltaMovement()
                + ", inWater=" + mob.isInWater()
                + ", navigationDone=" + mob.getNavigation().isDone();
    }

    private static boolean isNear(PathfinderMob mob, BlockPos target) {
        double dx = mob.getX() - (target.getX() + 0.5D);
        double dy = mob.getY() - target.getY();
        double dz = mob.getZ() - (target.getZ() + 0.5D);
        return dx * dx + dy * dy + dz * dz <= 2.25D;
    }

    private static final class ControlledNavigationMob extends PathfinderMob {
        private static final double CONTROLLED_BUOYANCY_SPEED = 0.08D;

        private ControlledNavigationMob(Level level) {
            super(EntityType.VILLAGER, level);
        }

        @Override
        protected PathNavigation createNavigation(Level level) {
            MCAGroundPathNavigation navigation = new MCAGroundPathNavigation(this, level);
            navigation.setCanFloat(true);
            return navigation;
        }

        @Override
        protected void registerGoals() {
            // Test-owned navigation only. No autonomous goals may replace the assigned path.
        }

        @Override
        public void aiStep() {
            super.aiStep();
            if (isInWater() && !getNavigation().isDone()) {
                Vec3 velocity = getDeltaMovement();
                setDeltaMovement(
                        velocity.x,
                        Math.max(velocity.y, CONTROLLED_BUOYANCY_SPEED),
                        velocity.z
                );
            }
        }
    }
}
