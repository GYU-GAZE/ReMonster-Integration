package com.gyugaze.remonsterintegration.tensura;

import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.storage.ep.IExistence;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

import java.util.Objects;

/** Boundary for Tensura existence/energy state used by cross-mod integrations. */
public final class TensuraExistenceBridge {
    private TensuraExistenceBridge() {
    }

    public static IExistence get(LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");
        return TensuraStorages.getExistenceFrom(entity);
    }

    /** Current consumable energy: current aura + current magicule. */
    public static double getEp(LivingEntity entity) {
        return get(entity).getEP();
    }

    public static double getAura(LivingEntity entity) {
        return get(entity).getAura();
    }

    public static double getMagicule(LivingEntity entity) {
        return get(entity).getMagicule();
    }

    public static double getMaxAura(LivingEntity entity) {
        return Math.max(0.0D, EnergyHelper.getMaxAura(entity));
    }

    public static double getMaxMagicule(LivingEntity entity) {
        return Math.max(0.0D, EnergyHelper.getMaxMagicule(entity));
    }

    /** Effective current capacity after Tensura attribute modifiers/limits. */
    public static double getMaxEp(LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");
        return EnergyHelper.getMaxEP(entity);
    }

    /** Base progression EP used by Tensura's native EvolutionRequirement.EPRequirement. */
    public static double getBaseMaxEp(LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");
        return EnergyHelper.getBaseMaxEP(entity);
    }

    public static double getGainedEp(LivingEntity entity) {
        return get(entity).getGainedEP();
    }

    public static double getSpiritualHealth(LivingEntity entity) {
        return get(entity).getSpiritualHealth();
    }

    /** Current-energy setter; this does not change progression/capacity. */
    public static void setEp(LivingEntity entity, double ep) {
        if (ep < 0) {
            throw new IllegalArgumentException("EP cannot be negative");
        }

        IExistence existence = get(entity);
        existence.setEP(ep);
        existence.markDirty();
    }

    public static void setCurrentEnergy(LivingEntity entity, double aura, double magicule) {
        IExistence existence = get(entity);
        existence.setAura(Mth.clamp(aura, 0.0D, getMaxAura(entity)));
        existence.setMagicule(Mth.clamp(magicule, 0.0D, getMaxMagicule(entity)));
        existence.markDirty();
    }

    /**
     * Re-clamps current pools after a skill/race/effect changes effective aura or magicule capacity.
     * Tensura's over-capacity magicule path can apply Magicule Poison to any LivingEntity, so an
     * external body must never be left with stale current magicule above a newly reduced max.
     */
    public static void clampCurrentEnergyToCapacity(LivingEntity entity) {
        setCurrentEnergy(entity, getAura(entity), getMagicule(entity));
    }

    /**
     * Changes base max EP through Tensura's own MAX_AURA/MAX_MAGICULE attributes.
     * Current aura/magicule are only clamped if capacity shrinks.
     */
    public static MaxEpChange setBaseMaxEpSafely(LivingEntity entity, double baseMaxEp) {
        Objects.requireNonNull(entity, "entity");
        if (baseMaxEp < 0.0D) {
            throw new IllegalArgumentException("Base max EP cannot be negative");
        }

        double previousBaseMaxEp = getBaseMaxEp(entity);
        double previousMaxEp = getMaxEp(entity);
        double previousCurrentEp = getEp(entity);

        EnergyHelper.setBaseMaxEP(entity, baseMaxEp);

        double resultingBaseMaxEp = getBaseMaxEp(entity);
        double resultingMaxEp = getMaxEp(entity);
        IExistence existence = get(entity);
        double resultingCurrentEp = existence.getEP();
        boolean currentEpClamped = false;

        double maxAura = getMaxAura(entity);
        double maxMagicule = getMaxMagicule(entity);
        double aura = existence.getAura();
        double magicule = existence.getMagicule();
        double clampedAura = Mth.clamp(aura, 0.0D, maxAura);
        double clampedMagicule = Mth.clamp(magicule, 0.0D, maxMagicule);

        if (Double.compare(aura, clampedAura) != 0 || Double.compare(magicule, clampedMagicule) != 0) {
            existence.setAura(clampedAura);
            existence.setMagicule(clampedMagicule);
            existence.markDirty();
            resultingCurrentEp = existence.getEP();
            currentEpClamped = true;
        }

        return new MaxEpChange(
                previousBaseMaxEp,
                resultingBaseMaxEp,
                previousMaxEp,
                resultingMaxEp,
                previousCurrentEp,
                resultingCurrentEp,
                currentEpClamped
        );
    }

    /**
     * Capacity assignment for external mobs. A mob that was 80% full before a profile expands its
     * aura/magicule pools remains 80% full afterwards instead of being stranded at its tiny constructor
     * defaults. Native Tensura costs/regen still own all subsequent spending and recovery.
     */
    public static MaxEpChange setBaseMaxEpPreservingEnergyRatio(LivingEntity entity, double baseMaxEp) {
        Objects.requireNonNull(entity, "entity");

        double oldMaxAura = getMaxAura(entity);
        double oldMaxMagicule = getMaxMagicule(entity);
        double oldAura = getAura(entity);
        double oldMagicule = getMagicule(entity);
        double auraRatio = fillRatio(oldAura, oldMaxAura);
        double magiculeRatio = fillRatio(oldMagicule, oldMaxMagicule);

        MaxEpChange change = setBaseMaxEpSafely(entity, baseMaxEp);
        if (change.resultingMaxEp() > change.previousMaxEp()) {
            setCurrentEnergy(
                    entity,
                    getMaxAura(entity) * auraRatio,
                    getMaxMagicule(entity) * magiculeRatio
            );
        }

        return new MaxEpChange(
                change.previousBaseMaxEp(),
                change.resultingBaseMaxEp(),
                change.previousMaxEp(),
                change.resultingMaxEp(),
                change.previousCurrentEp(),
                getEp(entity),
                change.currentEpClamped()
        );
    }

    public static MaxEpChange addBaseMaxEpSafely(LivingEntity entity, double amount) {
        Objects.requireNonNull(entity, "entity");
        if (amount < 0.0D) {
            throw new IllegalArgumentException("Base max EP increase cannot be negative");
        }
        return setBaseMaxEpSafely(entity, getBaseMaxEp(entity) + amount);
    }

    private static double fillRatio(double current, double maximum) {
        if (maximum > 1.0E-9D) {
            return Mth.clamp(current / maximum, 0.0D, 1.0D);
        }
        return current > 0.0D ? 1.0D : 0.0D;
    }

    public record MaxEpChange(
            double previousBaseMaxEp,
            double resultingBaseMaxEp,
            double previousMaxEp,
            double resultingMaxEp,
            double previousCurrentEp,
            double resultingCurrentEp,
            boolean currentEpClamped
    ) {
    }
}
