package com.gyugaze.remonsterintegration.growth;

import com.gyugaze.remonsterintegration.tensura.TensuraExistenceBridge;
import io.github.manasmods.tensura.entity.template.subclass.ISubordinate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * Keeps external mobs' current Tensura energy proportional when native EP growth expands capacity.
 * It never regenerates energy on its own and never bypasses a skill/learning cost.
 */
public final class ExternalEnergyFoundationService {
    private static final String SEEDED_KEY = "remonster_energy_capacity_seeded";
    private static final String LAST_MAX_AURA_KEY = "remonster_energy_last_max_aura";
    private static final String LAST_MAX_MAGICULE_KEY = "remonster_energy_last_max_magicule";
    private static final int CHECK_INTERVAL_TICKS = 10;

    private ExternalEnergyFoundationService() {
    }

    public static void tick(Mob mob) {
        if (mob.level().isClientSide() || !mob.isAlive() || !supports(mob)
                || mob.tickCount % CHECK_INTERVAL_TICKS != 0) {
            return;
        }
        syncCapacityGrowth(mob);
    }

    /** Call immediately after a profile intentionally establishes an external mob's starting capacity. */
    public static void seedCapacity(LivingEntity entity) {
        if (!supports(entity)) {
            return;
        }
        CompoundTag data = entity.getPersistentData();
        data.putBoolean(SEEDED_KEY, true);
        data.putDouble(LAST_MAX_AURA_KEY, TensuraExistenceBridge.getMaxAura(entity));
        data.putDouble(LAST_MAX_MAGICULE_KEY, TensuraExistenceBridge.getMaxMagicule(entity));
    }

    /**
     * Detects capacity increases performed by Tensura itself (for example kill EP) and scales current
     * aura/magicule by the old fill fraction. This can be called from the kill event before learning.
     */
    public static void syncCapacityGrowth(LivingEntity entity) {
        if (!supports(entity)) {
            return;
        }

        CompoundTag data = entity.getPersistentData();
        double newMaxAura = TensuraExistenceBridge.getMaxAura(entity);
        double newMaxMagicule = TensuraExistenceBridge.getMaxMagicule(entity);
        if (!data.getBoolean(SEEDED_KEY)) {
            seedCapacity(entity);
            return;
        }

        double oldMaxAura = Math.max(0.0D, data.getDouble(LAST_MAX_AURA_KEY));
        double oldMaxMagicule = Math.max(0.0D, data.getDouble(LAST_MAX_MAGICULE_KEY));
        double aura = TensuraExistenceBridge.getAura(entity);
        double magicule = TensuraExistenceBridge.getMagicule(entity);
        boolean changed = false;

        if (newMaxAura > oldMaxAura + 1.0E-9D) {
            aura = newMaxAura * fillRatio(aura, oldMaxAura);
            changed = true;
        } else if (aura > newMaxAura) {
            aura = newMaxAura;
            changed = true;
        }

        if (newMaxMagicule > oldMaxMagicule + 1.0E-9D) {
            magicule = newMaxMagicule * fillRatio(magicule, oldMaxMagicule);
            changed = true;
        } else if (magicule > newMaxMagicule) {
            magicule = newMaxMagicule;
            changed = true;
        }

        if (changed) {
            TensuraExistenceBridge.setCurrentEnergy(entity, aura, magicule);
        }

        data.putDouble(LAST_MAX_AURA_KEY, newMaxAura);
        data.putDouble(LAST_MAX_MAGICULE_KEY, newMaxMagicule);
    }

    public static void copyTrackingState(LivingEntity from, LivingEntity to) {
        if (!supports(to)) {
            return;
        }
        CompoundTag source = from.getPersistentData();
        CompoundTag target = to.getPersistentData();
        if (source.getBoolean(SEEDED_KEY)) {
            target.putBoolean(SEEDED_KEY, true);
            target.putDouble(LAST_MAX_AURA_KEY, source.getDouble(LAST_MAX_AURA_KEY));
            target.putDouble(LAST_MAX_MAGICULE_KEY, source.getDouble(LAST_MAX_MAGICULE_KEY));
        } else {
            seedCapacity(to);
        }
    }

    private static boolean supports(LivingEntity entity) {
        if (entity instanceof ISubordinate) {
            return false;
        }
        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return !"tensura".equals(entityId.getNamespace());
    }

    private static double fillRatio(double current, double maximum) {
        if (maximum > 1.0E-9D) {
            return Mth.clamp(current / maximum, 0.0D, 1.0D);
        }
        return current > 0.0D ? 1.0D : 0.0D;
    }
}
