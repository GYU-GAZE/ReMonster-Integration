package com.gyugaze.remonsterintegration.subordinate;

import com.gyugaze.remonsterintegration.mca.McaSubordinateAdapter;
import com.gyugaze.remonsterintegration.tensura.TensuraSubordinateBridge;
import io.github.manasmods.tensura.entity.template.TensuraTamableEntity;
import io.github.manasmods.tensura.entity.template.subclass.ISubordinate;
import io.github.manasmods.tensura.util.SubordinateHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Cross-mod movement-command router.
 *
 * Real TensuraTamableEntity instances always use Tensura's native ISubordinate state. Re:Monster
 * only stores a parallel movement mode for entities that do not have a real native command state,
 * such as vanilla pets and horses. This prevents split-brain combinations such as a Barghest being
 * visually seated by Thought Communication while Re:Monster separately thinks it is following.
 */
public final class SubordinateMovementService {
    private static final String DATA_KEY = "remonster_movement_mode";
    private static final double HORSE_FOLLOW_SPEED = 1.35D;
    private static final double HORSE_START_FOLLOW_SQR = 5.0D * 5.0D;
    private static final double HORSE_STOP_FOLLOW_SQR = 3.0D * 3.0D;

    private SubordinateMovementService() {
    }

    public static boolean setMode(Mob target, SubordinateMovementMode mode) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(mode, "mode");

        if (McaSubordinateAdapter.isMcaVillager(target)) {
            Optional<LivingEntity> owner = TensuraSubordinateBridge.getResolvedOwner(target);
            Player playerOwner = owner.filter(Player.class::isInstance).map(Player.class::cast).orElse(null);
            return McaSubordinateAdapter.setMovementMode(target, mode, playerOwner);
        }

        // Real Tensura mobs own sitting pose, wandering state, animation and SmartBrain behavior.
        if (target instanceof TensuraTamableEntity) {
            applyNativeTensura(target, mode);
            return true;
        }

        if (applyExternal(target, mode)) {
            return true;
        }

        // Compatibility fallback for another mod that provides a complete ISubordinate implementation.
        if (target instanceof ISubordinate) {
            applyNativeTensura(target, mode);
            return true;
        }
        return false;
    }

    private static void applyNativeTensura(Mob target, SubordinateMovementMode mode) {
        switch (mode) {
            case FOLLOW -> SubordinateHelper.setFollow(target);
            case WANDER -> SubordinateHelper.setWander(target);
            case STAY -> SubordinateHelper.setStay(target);
        }
    }

    /**
     * @return true when Re:Monster fully handled a non-native movement state and the caller should
     * skip Tensura's original helper implementation.
     */
    public static boolean applyExternal(Mob target, SubordinateMovementMode mode) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(mode, "mode");

        if (target instanceof TensuraTamableEntity) {
            return false;
        }

        Optional<LivingEntity> resolvedOwner = TensuraSubordinateBridge.getResolvedOwner(target);
        Player playerOwner = resolvedOwner.filter(Player.class::isInstance).map(Player.class::cast).orElse(null);

        if (McaSubordinateAdapter.isMcaVillager(target)) {
            return McaSubordinateAdapter.setMovementMode(target, mode, playerOwner);
        }

        if (target instanceof AbstractHorse horse) {
            setStoredMode(horse, mode);
            horse.getNavigation().stop();
            return true;
        }

        if (target instanceof TamableAnimal tameable) {
            setStoredMode(tameable, mode);
            switch (mode) {
                case FOLLOW -> tameable.setOrderedToSit(false);
                case WANDER -> {
                    tameable.setOrderedToSit(false);
                    tameable.getNavigation().stop();
                    tameable.setTarget(null);
                }
                case STAY -> {
                    tameable.getNavigation().stop();
                    tameable.setTarget(null);
                    tameable.setOrderedToSit(true);
                }
            }
            return true;
        }

        return false;
    }

    /** Compatibility alias used by the Tensura helper mixin. */
    public static boolean apply(Mob target, SubordinateMovementMode mode) {
        return applyExternal(target, mode);
    }

    public static boolean supportsExternal(Mob target) {
        if (target instanceof TensuraTamableEntity) {
            return false;
        }
        return McaSubordinateAdapter.isMcaVillager(target)
                || target instanceof AbstractHorse
                || target instanceof TamableAnimal;
    }

    public static Optional<SubordinateMovementMode> cycleExternal(Mob target, Player owner) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(owner, "owner");

        if (!supportsExternal(target)) {
            return Optional.empty();
        }

        SubordinateMovementMode next = getMode(target).orElse(SubordinateMovementMode.FOLLOW).next();
        return applyExternalWithOwner(target, next, owner) ? Optional.of(next) : Optional.empty();
    }

    private static boolean applyExternalWithOwner(Mob target, SubordinateMovementMode mode, Player owner) {
        if (McaSubordinateAdapter.isMcaVillager(target)) {
            return McaSubordinateAdapter.setMovementMode(target, mode, owner);
        }
        return applyExternal(target, mode);
    }

    public static Optional<SubordinateMovementMode> getMode(LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");

        Optional<SubordinateMovementMode> mca = McaSubordinateAdapter.getMovementMode(entity);
        if (mca.isPresent()) {
            return mca;
        }

        // Read the same native state that Thought Communication mutates.
        if (entity instanceof TensuraTamableEntity && entity instanceof ISubordinate subordinate) {
            if (subordinate.isOrderedToSit()) {
                return Optional.of(SubordinateMovementMode.STAY);
            }
            if (subordinate.isWandering()) {
                return Optional.of(SubordinateMovementMode.WANDER);
            }
            if (TensuraSubordinateBridge.getResolvedOwnerUuid(entity).isPresent()) {
                return Optional.of(SubordinateMovementMode.FOLLOW);
            }
            return Optional.empty();
        }

        if (entity instanceof AbstractHorse) {
            return getStoredMode(entity).or(() ->
                    TensuraSubordinateBridge.getResolvedOwnerUuid(entity).isPresent()
                            ? Optional.of(SubordinateMovementMode.WANDER)
                            : Optional.empty()
            );
        }

        if (entity instanceof TamableAnimal tameable) {
            if (tameable.isOrderedToSit()) {
                return Optional.of(SubordinateMovementMode.STAY);
            }
            return getStoredMode(entity).or(() ->
                    TensuraSubordinateBridge.getResolvedOwnerUuid(entity).isPresent()
                            ? Optional.of(SubordinateMovementMode.FOLLOW)
                            : Optional.empty()
            );
        }

        if (entity instanceof ISubordinate subordinate) {
            if (subordinate.isOrderedToSit()) {
                return Optional.of(SubordinateMovementMode.STAY);
            }
            if (subordinate.isWandering()) {
                return Optional.of(SubordinateMovementMode.WANDER);
            }
            if (TensuraSubordinateBridge.getResolvedOwnerUuid(entity).isPresent()) {
                return Optional.of(SubordinateMovementMode.FOLLOW);
            }
        }

        return getStoredMode(entity);
    }

    /**
     * Called from the horse tick mixin. Horses have no vanilla FollowOwnerGoal, so Re:Monster adds a
     * small navigation adapter while leaving riding, saddle/chest inventory and all normal horse AI
     * untouched in WANDER mode.
     */
    public static void tickHorse(AbstractHorse horse) {
        if (horse.level().isClientSide() || horse.isVehicle()) {
            return;
        }

        Optional<SubordinateMovementMode> mode = getStoredMode(horse);
        if (mode.isEmpty()) {
            return;
        }

        if (mode.get() == SubordinateMovementMode.STAY) {
            horse.getNavigation().stop();
            return;
        }
        if (mode.get() != SubordinateMovementMode.FOLLOW) {
            return;
        }

        Optional<LivingEntity> optionalOwner = TensuraSubordinateBridge.getResolvedOwner(horse);
        if (optionalOwner.isEmpty()) {
            return;
        }

        LivingEntity owner = optionalOwner.get();
        double distance = horse.distanceToSqr(owner);
        if (distance <= HORSE_STOP_FOLLOW_SQR) {
            horse.getNavigation().stop();
        } else if (distance >= HORSE_START_FOLLOW_SQR || !horse.getNavigation().isInProgress()) {
            horse.getNavigation().moveTo(owner, HORSE_FOLLOW_SPEED);
        }
    }

    /**
     * Unmodified vanilla pets have no stored key and keep vanilla behavior. Explicit Re:Monster
     * WANDER/STAY commands suppress FollowOwnerGoal until FOLLOW is selected again.
     */
    public static boolean allowsVanillaFollow(TamableAnimal animal) {
        if (animal instanceof TensuraTamableEntity) {
            return true;
        }
        return getStoredMode(animal)
                .map(mode -> mode == SubordinateMovementMode.FOLLOW)
                .orElse(true);
    }

    private static void setStoredMode(LivingEntity entity, SubordinateMovementMode mode) {
        entity.getPersistentData().putString(DATA_KEY, mode.name());
    }

    private static Optional<SubordinateMovementMode> getStoredMode(LivingEntity entity) {
        CompoundTag tag = entity.getPersistentData();
        if (!tag.contains(DATA_KEY)) {
            return Optional.empty();
        }
        String value = tag.getString(DATA_KEY).toUpperCase(Locale.ROOT);
        try {
            return Optional.of(SubordinateMovementMode.valueOf(value));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }
}
