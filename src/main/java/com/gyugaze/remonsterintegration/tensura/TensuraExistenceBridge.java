package com.gyugaze.remonsterintegration.tensura;

import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.storage.ep.IExistence;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.world.entity.LivingEntity;

import java.util.Objects;

/**
 * Boundary for Tensura existence/energy state used by cross-mod integrations.
 *
 * Tensura distinguishes current EP (aura + magicule currently stored) from max/base-max EP
 * (the entity's progression/capacity). Race EP requirements use base max EP and Better Subordinates
 * threshold checks use max EP. Callers that mean progression must therefore never write current EP.
 */
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

    /**
     * Writes current aura + magicule only. This is an energy refill/drain primitive, not a progression
     * setter. Writing above max EP makes Tensura bleed the excess and may trigger Insanity or
     * Magicule Poison.
     */
    public static void setEp(LivingEntity entity, double ep) {
        if (ep < 0) {
            throw new IllegalArgumentException("EP cannot be negative");
        }

        IExistence existence = get(entity);
        existence.setEP(ep);
        existence.markDirty();
    }

    /**
     * Changes base max EP through Tensura's own MAX_AURA/MAX_MAGICULE attributes.
     *
     * Better Subordinates listens to increases of those exact attributes and re-evaluates its native
     * skill thresholds. If capacity is reduced, aura and magicule are clamped independently to their
     * resulting effective maxima so this helper cannot manufacture an artificial poison/insanity state.
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

        double maxAura = Math.max(0.0D, EnergyHelper.getMaxAura(entity));
        double maxMagicule = Math.max(0.0D, EnergyHelper.getMaxMagicule(entity));
        double aura = existence.getAura();
        double magicule = existence.getMagicule();
        double clampedAura = Math.min(Math.max(0.0D, aura), maxAura);
        double clampedMagicule = Math.min(Math.max(0.0D, magicule), maxMagicule);

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

    public static MaxEpChange addBaseMaxEpSafely(LivingEntity entity, double amount) {
        Objects.requireNonNull(entity, "entity");
        if (amount < 0.0D) {
            throw new IllegalArgumentException("Base max EP increase cannot be negative");
        }
        return setBaseMaxEpSafely(entity, getBaseMaxEp(entity) + amount);
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
