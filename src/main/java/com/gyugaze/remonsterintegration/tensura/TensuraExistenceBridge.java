package com.gyugaze.remonsterintegration.tensura;

import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.storage.ep.IExistence;
import net.minecraft.world.entity.LivingEntity;

import java.util.Objects;

/**
 * Boundary for EP and other existence-state access used by cross-mod integrations.
 */
public final class TensuraExistenceBridge {
    private TensuraExistenceBridge() {
    }

    public static IExistence get(LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");
        return TensuraStorages.getExistenceFrom(entity);
    }

    public static double getEp(LivingEntity entity) {
        return get(entity).getEP();
    }

    public static double getGainedEp(LivingEntity entity) {
        return get(entity).getGainedEP();
    }

    public static double getSpiritualHealth(LivingEntity entity) {
        return get(entity).getSpiritualHealth();
    }

    public static void setEp(LivingEntity entity, double ep) {
        if (ep < 0) {
            throw new IllegalArgumentException("EP cannot be negative");
        }

        IExistence existence = get(entity);
        existence.setEP(ep);
        existence.markDirty();
    }
}
