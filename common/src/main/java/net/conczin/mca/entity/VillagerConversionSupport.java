package net.conczin.mca.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.function.ToDoubleFunction;

/**
 * Minecraft adapter for identity-preserving MCA villager conversion.
 */
public final class VillagerConversionSupport {
    private VillagerConversionSupport() {
    }

    public static boolean shouldPreserveUuid(Mob source, EntityType<?> targetType) {
        if (source instanceof VillagerEntityMCA villager) {
            return targetType == villager.getGenetics().getGender().getZombieType();
        }
        if (source instanceof ZombieVillagerEntityMCA zombie) {
            return targetType == zombie.getGenetics().getGender().getVillagerType();
        }
        return false;
    }

    @Nullable
    public static <T extends Mob> T convertPreservingUuid(
            Mob source,
            EntityType<T> targetType,
            boolean keepEquipment,
            ToDoubleFunction<EquipmentSlot> dropChanceGetter
    ) {
        Entity vehicle = source.getVehicle();

        return VillagerConversionIdentityPolicy.convert(
                source.isRemoved(),
                () -> targetType.create(source.level()),
                () -> source instanceof VillagerLike<?> villager
                        ? villager.toNbtForConversion()
                        : new CompoundTag(),
                (converted, conversionData) -> {
                    converted.copyPosition(source);
                    converted.setBaby(source.isBaby());
                    converted.setNoAi(source.isNoAi());
                    if (source.hasCustomName()) {
                        converted.setCustomName(source.getCustomName());
                        converted.setCustomNameVisible(source.isCustomNameVisible());
                    }
                    if (source.isPersistenceRequired()) {
                        converted.setPersistenceRequired();
                    }
                    converted.setInvulnerable(source.isInvulnerable());

                    if (keepEquipment) {
                        converted.setCanPickUpLoot(source.canPickUpLoot());
                        for (EquipmentSlot slot : EquipmentSlot.values()) {
                            ItemStack stack = source.getItemBySlot(slot);
                            if (!stack.isEmpty()) {
                                converted.setItemSlot(slot, stack.copyAndClear());
                                converted.setDropChance(slot, (float) dropChanceGetter.applyAsDouble(slot));
                            }
                        }
                    }

                    converted.setUUID(source.getUUID());
                    if (converted instanceof VillagerLike<?> villager) {
                        villager.readNbtForConversion(conversionData);
                    }
                },
                source::discard,
                converted -> source.level().addFreshEntity(converted),
                converted -> {
                    if (vehicle != null) {
                        converted.startRiding(vehicle, true);
                    }
                }
        ).orElse(null);
    }
}
