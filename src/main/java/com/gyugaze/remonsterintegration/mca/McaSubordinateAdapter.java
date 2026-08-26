package com.gyugaze.remonsterintegration.mca;

import com.gyugaze.remonsterintegration.ReMonsterIntegration;
import com.gyugaze.remonsterintegration.subordinate.SubordinateMovementMode;
import com.gyugaze.remonsterintegration.tensura.TensuraSubordinateBridge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * In-place bridge that gives an MCA villager Tensura ownership and maps Re:Monster movement commands
 * onto MCA's own VillagerBrain without replacing the entity or its Brain.
 */
public final class McaSubordinateAdapter {
    private static final String MCA_VILLAGER_CLASS = "net.conczin.mca.entity.VillagerEntityMCA";
    private static final String MCA_MOVE_STATE_CLASS = "net.conczin.mca.entity.ai.MoveState";

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

    /** MCA exposes this directly on VillagerEntityMCA; reflection keeps the integration optional. */
    public static boolean isGuard(LivingEntity villager) {
        Objects.requireNonNull(villager, "villager");
        if (!isMcaVillager(villager)) {
            return false;
        }

        try {
            Object value = villager.getClass().getMethod("isGuard").invoke(villager);
            return value instanceof Boolean guard && guard;
        } catch (ReflectiveOperationException | LinkageError exception) {
            return false;
        }
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

    /**
     * Maps the common movement vocabulary to MCA's native MoveState enum:
     * WANDER -> MOVE, STAY -> STAY, FOLLOW -> FOLLOW.
     *
     * Reflection keeps MCA an optional runtime integration and avoids a hard classloading dependency.
     */
    public static boolean setMovementMode(LivingEntity villager, SubordinateMovementMode mode, Player owner) {
        Objects.requireNonNull(villager, "villager");
        Objects.requireNonNull(mode, "mode");

        if (!isMcaVillager(villager)) {
            return false;
        }
        if (mode == SubordinateMovementMode.FOLLOW && owner == null) {
            return false;
        }

        try {
            Object brain = villager.getClass().getMethod("getVillagerBrain").invoke(villager);
            Class<?> moveStateClass = Class.forName(MCA_MOVE_STATE_CLASS, false, villager.getClass().getClassLoader());
            Object moveState = enumConstant(moveStateClass, switch (mode) {
                case FOLLOW -> "FOLLOW";
                case WANDER -> "MOVE";
                case STAY -> "STAY";
            });

            Method setMoveState = brain.getClass().getMethod("setMoveState", moveStateClass, Player.class);
            setMoveState.invoke(brain, moveState, mode == SubordinateMovementMode.FOLLOW ? owner : null);
            return true;
        } catch (ReflectiveOperationException | LinkageError exception) {
            ReMonsterIntegration.LOGGER.warn(
                    "Could not map {} movement command onto MCA villager {}",
                    mode,
                    villager.getUUID(),
                    exception
            );
            return false;
        }
    }

    public static Optional<SubordinateMovementMode> getMovementMode(LivingEntity villager) {
        Objects.requireNonNull(villager, "villager");

        if (!isMcaVillager(villager)) {
            return Optional.empty();
        }

        try {
            Object brain = villager.getClass().getMethod("getVillagerBrain").invoke(villager);
            Object moveState = brain.getClass().getMethod("getMoveState").invoke(brain);
            if (!(moveState instanceof Enum<?> enumValue)) {
                return Optional.empty();
            }
            return switch (enumValue.name().toUpperCase(Locale.ROOT)) {
                case "FOLLOW" -> Optional.of(SubordinateMovementMode.FOLLOW);
                case "STAY" -> Optional.of(SubordinateMovementMode.STAY);
                case "MOVE" -> Optional.of(SubordinateMovementMode.WANDER);
                default -> Optional.empty();
            };
        } catch (ReflectiveOperationException | LinkageError exception) {
            return Optional.empty();
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object enumConstant(Class<?> enumClass, String name) {
        if (!enumClass.isEnum()) {
            throw new IllegalArgumentException(enumClass.getName() + " is not an enum");
        }
        return Enum.valueOf((Class<? extends Enum>) enumClass.asSubclass(Enum.class), name);
    }
}
