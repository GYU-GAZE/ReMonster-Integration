package com.gyugaze.remonsterintegration.subordinate;

import com.gyugaze.remonsterintegration.mca.McaSubordinateAdapter;
import com.gyugaze.remonsterintegration.tensura.TensuraSubordinateBridge;
import io.github.manasmods.tensura.entity.template.subclass.ISubordinate;
import io.github.manasmods.tensura.util.SubordinateHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Universal targeting/posture router.
 *
 * Native ISubordinate entities keep Tensura's behaviour integer and target AI as the source of truth.
 * Other named/owned mobs receive a small compatibility state that mirrors the native semantics as
 * closely as their original AI permits. MCA combat is driven through Brain memories rather than by
 * replacing its Brain or schedule.
 */
public final class SubordinateTargetingService {
    private static final String DATA_KEY = "remonster_targeting_mode";
    private static final int EXTERNAL_TICK_INTERVAL = 5;

    private SubordinateTargetingService() {
    }

    public static boolean setMode(Mob target, SubordinateTargetingMode mode) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(mode, "mode");

        if (target instanceof ISubordinate) {
            applyNative(target, mode);
            return true;
        }

        if (TensuraSubordinateBridge.getResolvedOwner(target).isEmpty()) {
            return false;
        }

        target.getPersistentData().putString(DATA_KEY, mode.name());
        if (mode != SubordinateTargetingMode.AGGRESSIVE) {
            clearTargetState(target);
        }
        return true;
    }

    public static SubordinateTargetingMode getMode(LivingEntity target) {
        Objects.requireNonNull(target, "target");

        if (target instanceof ISubordinate subordinate) {
            return SubordinateTargetingMode.fromNativeBehaviour(subordinate.getBehaviour());
        }

        CompoundTag data = target.getPersistentData();
        if (data.contains(DATA_KEY)) {
            try {
                return SubordinateTargetingMode.valueOf(data.getString(DATA_KEY).toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                // Fall through to the neutral compatibility default.
            }
        }
        return SubordinateTargetingMode.NEUTRAL;
    }

    public static boolean hasExternalMode(Mob target) {
        return !(target instanceof ISubordinate) && target.getPersistentData().contains(DATA_KEY);
    }

    /** Called by a very small Mob tick mixin; exits immediately for native/unconfigured mobs. */
    public static void tickExternal(Mob target) {
        if (target.level().isClientSide()
                || target instanceof ISubordinate
                || !target.getPersistentData().contains(DATA_KEY)
                || target.tickCount % EXTERNAL_TICK_INTERVAL != 0) {
            return;
        }

        Optional<LivingEntity> optionalOwner = TensuraSubordinateBridge.getResolvedOwner(target);
        if (optionalOwner.isEmpty()) {
            return;
        }
        LivingEntity owner = optionalOwner.get();

        // Native Tensura targeting is disabled while ordered to stay/sit. Keep the same invariant for
        // compatibility mobs rather than letting a STAY subordinate drag itself into combat.
        if (SubordinateMovementService.getMode(target).orElse(SubordinateMovementMode.WANDER)
                == SubordinateMovementMode.STAY) {
            clearTargetState(target);
            return;
        }

        SubordinateTargetingMode mode = getMode(target);
        if (mode == SubordinateTargetingMode.PASSIVE) {
            clearTargetState(target);
            return;
        }

        LivingEntity current = target.getTarget();
        if (current != null && !isValidEnemy(target, owner, current)) {
            clearTargetState(target);
            current = null;
        }

        if (current == null) {
            current = switch (mode) {
                case NEUTRAL -> findNeutralTarget(target, owner);
                case PASSIVE -> null;
                case AGGRESSIVE -> findAggressiveTarget(target, owner);
                case PROTECT -> findProtectTarget(target, owner).orElseGet(() -> findNeutralTarget(target, owner));
            };
        }

        if (current != null) {
            applyTarget(target, current);
        }
    }

    private static void applyNative(Mob target, SubordinateTargetingMode mode) {
        switch (mode) {
            case NEUTRAL -> SubordinateHelper.setNeutral(target);
            case PASSIVE -> SubordinateHelper.setPassive(target);
            case AGGRESSIVE -> SubordinateHelper.setAggressive(target);
            case PROTECT -> SubordinateHelper.setProtect(target);
        }
    }

    private static LivingEntity findNeutralTarget(Mob target, LivingEntity owner) {
        LivingEntity candidate = target.getLastHurtByMob();
        if (isValidEnemy(target, owner, candidate)) {
            return candidate;
        }

        candidate = owner.getLastHurtByMob();
        if (isValidEnemy(target, owner, candidate)) {
            return candidate;
        }

        candidate = owner.getLastHurtMob();
        if (isValidEnemy(target, owner, candidate)) {
            return candidate;
        }

        if (owner instanceof Mob ownerMob) {
            candidate = ownerMob.getTarget();
            if (isValidEnemy(target, owner, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static LivingEntity findAggressiveTarget(Mob target, LivingEntity owner) {
        double range = Math.max(8.0D, target.getAttributeValue(Attributes.FOLLOW_RANGE));
        List<LivingEntity> candidates = target.level().getEntitiesOfClass(
                LivingEntity.class,
                target.getBoundingBox().inflate(range),
                candidate -> isValidEnemy(target, owner, candidate)
        );
        return candidates.stream()
                .min(Comparator.comparingDouble(target::distanceToSqr))
                .orElse(null);
    }

    private static Optional<LivingEntity> findProtectTarget(Mob target, LivingEntity owner) {
        double range = Math.max(8.0D, target.getAttributeValue(Attributes.FOLLOW_RANGE));
        return target.level().getEntitiesOfClass(
                        Mob.class,
                        target.getBoundingBox().inflate(range),
                        candidate -> candidate != target && isValidEnemy(target, owner, candidate)
                ).stream()
                .filter(candidate -> {
                    LivingEntity threatened = candidate.getTarget();
                    return threatened != null && isProtectedAlly(target, owner, threatened);
                })
                .min(Comparator.comparingDouble(target::distanceToSqr))
                .map(LivingEntity.class::cast);
    }

    private static boolean isProtectedAlly(Mob target, LivingEntity owner, LivingEntity entity) {
        return entity == target
                || entity == owner
                || TensuraSubordinateBridge.isAlly(owner, entity)
                || TensuraSubordinateBridge.isAlly(target, entity);
    }

    private static boolean isValidEnemy(Mob target, LivingEntity owner, LivingEntity candidate) {
        if (candidate == null || candidate == target || candidate == owner || !candidate.isAlive()) {
            return false;
        }
        if (!target.canAttack(candidate)) {
            return false;
        }
        return !TensuraSubordinateBridge.isAlly(target, candidate)
                && !TensuraSubordinateBridge.isAlly(owner, candidate);
    }

    private static void applyTarget(Mob target, LivingEntity candidate) {
        target.setTarget(candidate);

        if (!McaSubordinateAdapter.isMcaVillager(target)) {
            return;
        }

        // MCA guards already consume ATTACK_TARGET. Ordinary villagers use NEAREST_HOSTILE in their
        // self-defence package. Supplying both memories lets MCA keep ownership of the actual combat
        // tasks while Re:Monster only chooses the target dictated by Thought Communication semantics.
        target.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, candidate);
        target.getBrain().setMemory(MemoryModuleType.NEAREST_HOSTILE, candidate);
        target.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(candidate, true));
        target.getBrain().setMemory(
                MemoryModuleType.WALK_TARGET,
                new WalkTarget(new EntityTracker(candidate, false), 0.6F, 2)
        );
    }

    private static void clearTargetState(Mob target) {
        SubordinateHelper.removeTarget(target);
        if (McaSubordinateAdapter.isMcaVillager(target)) {
            target.getBrain().eraseMemory(MemoryModuleType.NEAREST_HOSTILE);
            target.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
            target.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
            target.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
        }
    }
}
