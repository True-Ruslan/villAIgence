package net.conczin.mca.gametest;

import net.conczin.mca.block.TombstoneBlock;
import net.conczin.mca.entity.VillagerEntityMCA;
import net.conczin.mca.livingworld.memory2.MemoryEvent;
import net.conczin.mca.livingworld.memory2.MemoryEventStore;
import net.conczin.mca.registry.BlocksMCA;
import net.conczin.mca.registry.EntitiesMCA;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.storage.LevelResource;

import java.util.List;
import java.util.UUID;

public final class TombstoneLivingWorldRetentionGameTests implements FabricGameTest {
    private static final UUID FIXTURE_UUID = UUID.fromString(
            "2c4862d1-885d-4f67-a6aa-cbe8197d1134"
    );
    private static final UUID MEMORY_EVENT_UUID = UUID.fromString(
            "22bcf8c4-3cb4-471a-b74e-87fd9e2bd207"
    );

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void capturedTombstoneDeathPreservesLivingWorldMemory(GameTestHelper helper) {
        BlockPos tombstonePos = helper.absolutePos(new BlockPos(2, 1, 2));
        helper.getLevel().setBlockAndUpdate(
                tombstonePos,
                BlocksMCA.UPRIGHT_HEADSTONE.defaultBlockState()
        );

        VillagerEntityMCA villager = helper.spawn(EntitiesMCA.MALE_VILLAGER, 3, 1, 2);
        villager.setUUID(FIXTURE_UUID);

        MemoryEventStore memory = MemoryEventStore.forWorld(
                helper.getLevel().getServer().getWorldPath(LevelResource.ROOT)
        );
        memory.removeNpc(FIXTURE_UUID);
        MemoryEvent retained = new MemoryEvent(
                MEMORY_EVENT_UUID,
                FIXTURE_UUID,
                MemoryEvent.Type.OBSERVATION,
                "tombstone-living-world-retention",
                List.of(FIXTURE_UUID),
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                helper.getLevel().getGameTime(),
                1_700_000_000_000L,
                75,
                20,
                100,
                List.of()
        );
        memory.append(retained, 8);
        helper.assertTrue(
                memory.findById(FIXTURE_UUID, MEMORY_EVENT_UUID).isPresent(),
                "Fixture Living World memory must exist before death"
        );

        boolean damaged = villager.hurt(
                helper.getLevel().damageSources().genericKill(),
                Float.MAX_VALUE
        );
        helper.assertTrue(damaged, "Fixture villager must accept lethal damage");
        helper.assertTrue(!villager.isAlive(), "Fixture villager must die");
        helper.assertTrue(
                helper.getLevel().getBlockEntity(tombstonePos) instanceof TombstoneBlock.Data,
                "Configured grave must retain its tombstone block entity"
        );
        TombstoneBlock.Data tombstone = (TombstoneBlock.Data) helper.getLevel()
                .getBlockEntity(tombstonePos);
        helper.assertTrue(
                tombstone.hasEntity(),
                "Real death path must capture the NPC for later resurrection"
        );

        helper.assertTrue(
                memory.findById(FIXTURE_UUID, MEMORY_EVENT_UUID).isPresent(),
                "A tombstone-captured NPC keeps the same persistent identity, so Living World memory must survive death"
        );

        memory.removeNpc(FIXTURE_UUID);
        helper.succeed();
    }
}
