package com.gyugaze.remonsterintegration.mca;

import com.gyugaze.remonsterintegration.ReMonsterIntegration;
import com.gyugaze.remonsterintegration.subordinate.SubordinateMovementMode;
import com.gyugaze.remonsterintegration.tensura.TensuraSubordinateBridge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;

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
    private static final double MELEE_DAMAGE_EPSILON = 1.0E-6D;

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

    /**
     * Projects the strongest actual main-hand melee item from the MCA inventory into the dominant slot.
     */
    public static boolean equipBestMeleeWeaponFromInventory(LivingEntity villager) {
        Objects.requireNonNull(villager, "villager");
        if (!isMcaVillager(villager) || isGuard(villager)) {
            return false;
        }

        try {
            Object inventoryValue = villager.getClass().getMethod("getInventory").invoke(villager);
            Object slotValue = villager.getClass().getMethod("getDominantSlot").invoke(villager);
            if (!(inventoryValue instanceof Container inventory) || !(slotValue instanceof EquipmentSlot slot)) {
                return false;
            }

            double baseAttackDamage = villager.getAttributeBaseValue(Attributes.ATTACK_DAMAGE);
            ItemStack best = ItemStack.EMPTY;
            double bestAttackDamage = baseAttackDamage;

            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack candidate = inventory.getItem(i);
                double candidateAttackDamage = getMainHandAttackDamage(candidate, baseAttackDamage);
                if (candidateAttackDamage > bestAttackDamage + MELEE_DAMAGE_EPSILON) {
                    best = candidate;
                    bestAttackDamage = candidateAttackDamage;
                }
            }

            if (best.isEmpty()) {
                return false;
            }

            villager.setItemSlot(slot, best);
            return true;
        } catch (ReflectiveOperationException | LinkageError exception) {
            ReMonsterIntegration.LOGGER.debug(
                    "Could not project MCA civilian melee weapon for {}",
                    villager.getUUID(),
                    exception
            );
            return false;
        }
    }

    private static double getMainHandAttackDamage(ItemStack stack, double baseAttackDamage) {
        if (stack.isEmpty()) {
            return Double.NEGATIVE_INFINITY;
        }

        double[] modifiers = new double[] {0.0D, 0.0D, 1.0D, 0.0D};
        stack.forEachModifier(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
            if (!attribute.equals(Attributes.ATTACK_DAMAGE)) {
                return;
            }

            modifiers[3] = 1.0D;
            switch (modifier.operation()) {
                case ADD_VALUE -> modifiers[0] += modifier.amount();
                case ADD_MULTIPLIED_BASE -> modifiers[1] += modifier.amount();
                case ADD_MULTIPLIED_TOTAL -> modifiers[2] *= 1.0D + modifier.amount();
            }
        });

        if (modifiers[3] == 0.0D) {
            return Double.NEGATIVE_INFINITY;
        }

        double withAdditions = baseAttackDamage + modifiers[0];
        double withMultipliedBase = withAdditions + withAdditions * modifiers[1];
        double result = withMultipliedBase * modifiers[2];
        if (!Double.isFinite(result) || result <= baseAttackDamage + MELEE_DAMAGE_EPSILON) {
            return Double.NEGATIVE_INFINITY;
        }
        return result;
    }

    /**
     * Projects the best BowItem already stored in an MCA civilian's inventory into MCA's own dominant
     * equipment slot. Re:Monster deliberately does not force MAIN_HAND: MCA's native ranged attack is
     * hand-agnostic and resolves whichever hand actually holds the bow.
     */
    public static boolean equipBestBowFromInventory(LivingEntity villager) {
        Objects.requireNonNull(villager, "villager");
        if (!isMcaVillager(villager) || isGuard(villager)) {
            return false;
        }

        try {
            Object inventoryValue = villager.getClass().getMethod("getInventory").invoke(villager);
            Object slotValue = villager.getClass().getMethod("getDominantSlot").invoke(villager);
            if (!(inventoryValue instanceof Container inventory) || !(slotValue instanceof EquipmentSlot slot)) {
                return false;
            }

            ItemStack best = ItemStack.EMPTY;
            int bestMaxDamage = -1;
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack candidate = inventory.getItem(i);
                if (candidate.getItem() instanceof BowItem && candidate.getMaxDamage() > bestMaxDamage) {
                    best = candidate;
                    bestMaxDamage = candidate.getMaxDamage();
                }
            }

            if (best.isEmpty()) {
                return false;
            }

            villager.setItemSlot(slot, best);
            return true;
        } catch (ReflectiveOperationException | LinkageError exception) {
            ReMonsterIntegration.LOGGER.debug(
                    "Could not project MCA civilian bow for {}",
                    villager.getUUID(),
                    exception
            );
            return false;
        }
    }

    /** Returns the actual hand currently holding a bow, matching MCA's native hand-agnostic ranged logic. */
    public static Optional<InteractionHand> getHeldBowHand(LivingEntity villager) {
        Objects.requireNonNull(villager, "villager");
        if (villager.getMainHandItem().getItem() instanceof BowItem) {
            return Optional.of(InteractionHand.MAIN_HAND);
        }
        if (villager.getOffhandItem().getItem() instanceof BowItem) {
            return Optional.of(InteractionHand.OFF_HAND);
        }
        return Optional.empty();
    }

    public static Optional<ItemStack> getHeldBowStack(LivingEntity villager) {
        return getHeldBowHand(villager).map(villager::getItemInHand);
    }

    /** Invokes MCA's own VillagerEntityMCA#performRangedAttack implementation. */
    public static boolean performHeldBowAttack(LivingEntity villager, LivingEntity target, float pullProgress) {
        Objects.requireNonNull(villager, "villager");
        Objects.requireNonNull(target, "target");
        if (!isMcaVillager(villager) || isGuard(villager) || getHeldBowHand(villager).isEmpty()) {
            return false;
        }

        try {
            Method performRangedAttack = villager.getClass().getMethod(
                    "performRangedAttack",
                    LivingEntity.class,
                    float.class
            );
            performRangedAttack.invoke(villager, target, pullProgress);
            return true;
        } catch (ReflectiveOperationException | LinkageError exception) {
            ReMonsterIntegration.LOGGER.debug(
                    "Could not invoke MCA civilian bow attack for {}",
                    villager.getUUID(),
                    exception
            );
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
