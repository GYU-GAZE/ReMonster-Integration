package com.gyugaze.remonsterintegration.subordinate;

import com.gyugaze.remonsterintegration.mca.McaSubordinateAdapter;
import com.gyugaze.remonsterintegration.tensura.TensuraSubordinateBridge;
import io.github.manasmods.tensura.entity.template.subclass.ISubordinate;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Cross-mod movement-command router.
 *
 * Native Tensura ISubordinate entities remain authoritative for their own state. Re:Monster only
 * handles integrations whose native AI would otherwise receive no useful state change from Tensura's
 * generic SubordinateHelper calls (currently MCA and vanilla/modded TamableAnimal command state).
 */
public final class SubordinateMovementService {
    private static final String DATA_KEY = "remonster_movement_mode";

    private SubordinateMovementService() {
    }

    /**
     * @return true when Re:Monster fully handled the command and the caller should skip Tensura's
     * original helper implementation.
     */
    public static boolean apply(Mob target, SubordinateMovementMode mode) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(mode, "mode");

        Optional<LivingEntity> resolvedOwner = TensuraSubordinateBridge.getResolvedOwner(target);
        Player playerOwner = resolvedOwner.filter(Player.class::isInstance).map(Player.class::cast).orElse(null);

        if (McaSubordinateAdapter.isMcaVillager(target)) {
            return McaSubordinateAdapter.setMovementMode(target, mode, playerOwner);
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

        // Native Tensura mobs have real ISubordinate state fields/AI and should keep using upstream.
        if (target instanceof ISubordinate) {
            return false;
        }

        return false;
    }

    /**
     * Cycles an external subordinate and returns the state that was actually applied.
     */
    public static Optional<SubordinateMovementMode> cycleExternal(Mob target, Player owner) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(owner, "owner");

        if (McaSubordinateAdapter.isMcaVillager(target) || target instanceof TamableAnimal) {
            SubordinateMovementMode next = getMode(target).orElse(SubordinateMovementMode.FOLLOW).next();
            return applyExternalWithOwner(target, next, owner) ? Optional.of(next) : Optional.empty();
        }

        return Optional.empty();
    }

    private static boolean applyExternalWithOwner(Mob target, SubordinateMovementMode mode, Player owner) {
        if (McaSubordinateAdapter.isMcaVillager(target)) {
            return McaSubordinateAdapter.setMovementMode(target, mode, owner);
        }
        return apply(target, mode);
    }

    public static Optional<SubordinateMovementMode> getMode(LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");

        Optional<SubordinateMovementMode> mca = McaSubordinateAdapter.getMovementMode(entity);
        if (mca.isPresent()) {
            return mca;
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
     * Used by the vanilla FollowOwnerGoal mixin. Unmodified vanilla pets have no stored key and keep
     * their vanilla behavior. A Re:Monster WANDER/STAY command explicitly suppresses follow-owner AI.
     */
    public static boolean allowsVanillaFollow(TamableAnimal animal) {
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
