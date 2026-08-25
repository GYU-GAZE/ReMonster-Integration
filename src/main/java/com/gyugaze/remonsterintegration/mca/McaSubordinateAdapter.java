package com.gyugaze.remonsterintegration.mca;

import com.gyugaze.remonsterintegration.tensura.TensuraSubordinateBridge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.Objects;

/**
 * Minimal in-place bridge that gives an MCA villager Tensura ownership without replacing the entity.
 *
 * Entity class, Brain, inventory, profession, relationships, residency and any Townstead state remain
 * owned by MCA/Townstead. This adapter changes only Tensura's permanent-owner field.
 */
public final class McaSubordinateAdapter {
    private static final String MCA_VILLAGER_CLASS = "net.conczin.mca.entity.VillagerEntityMCA";

    private McaSubordinateAdapter() {
    }

    public static boolean isMcaVillager(LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");

        Class<?> type = entity.getClass();
        while (type != null) {
            if (MCA_VILLAGER_CLASS.equals(type.getName())) {
                return true;
            }
            type = type.getSuperclass();
        }
        return false;
    }

    public static boolean assignPermanentOwner(LivingEntity villager, ServerPlayer owner) {
        Objects.requireNonNull(villager, "villager");
        Objects.requireNonNull(owner, "owner");

        if (!isMcaVillager(villager)) {
            return false;
        }

        TensuraSubordinateBridge.setPermanentOwner(villager, owner);
        return TensuraSubordinateBridge.getResolvedOwnerUuid(villager)
                .filter(owner.getUUID()::equals)
                .isPresent();
    }

    public static boolean clearPermanentOwner(LivingEntity villager) {
        Objects.requireNonNull(villager, "villager");

        if (!isMcaVillager(villager)) {
            return false;
        }

        TensuraSubordinateBridge.clearPermanentOwner(villager);
        return TensuraSubordinateBridge.getResolvedOwnerUuid(villager).isEmpty();
    }
}
