package com.gyugaze.remonsterintegration.subordinate;

import com.gyugaze.remonsterintegration.mca.McaSubordinateAdapter;
import com.gyugaze.remonsterintegration.tensura.TensuraSubordinateBridge;
import io.github.manasmods.tensura.entity.template.subclass.ISubordinate;
import io.github.manasmods.tensura.util.SubordinateHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.BowItem;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Universal targeting/posture router.
 *
 * Native ISubordinate entities keep Tensura's behaviour integer and target AI as the source of truth.
 * Other named/owned mobs receive a small compatibility state. The external aggressive adapter is
 * deliberately safer than native Tensura's very broad "nearest non-ally" scan: it proactively hunts
 * hostile mobs, while Neutral/Protect can still retaliate against otherwise-neutral attackers.
 */
public final class SubordinateTargetingService {
    private static final String DATA_KEY = "remonster_targeting_mode";
    private static final String NEXT_MCA_CIVIL_ATTACK_TICK = "remonster_next_mca_civil_attack_tick";
    private static final int EXTERNAL_TICK_INTERVAL = 5;
    private static final int MCA_CIVIL_ATTACK_COOLDOWN = 20;
    private static final int MCA_CIVIL_BOW_DRAW_TICKS = 20;
    private static final double MCA_CIVIL_BOW_MAX_RANGE = 15.0D;

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
        // Never carry a stale target across posture changes. The next compatibility tick resolves a
        // target using the semantics of the newly selected mode.
        clearTargetState(target);
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
        if (current != null && !isValidCurrentTarget(mode, target, owner, current)) {
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
                candidate -> isAggressiveCandidate(target, owner, candidate)
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

    private static boolean isValidCurrentTarget(
            SubordinateTargetingMode mode,
            Mob target,
            LivingEntity owner,
            LivingEntity candidate
    ) {
        return mode == SubordinateTargetingMode.AGGRESSIVE
                ? isAggressiveCandidate(target, owner, candidate)
                : isValidEnemy(target, owner, candidate);
    }

    private static boolean isAggressiveCandidate(Mob target, LivingEntity owner, LivingEntity candidate) {
        if (!isValidEnemy(target, owner, candidate)) {
            return false;
        }

        // Proactive aggression is intentionally restricted to entities Minecraft/mods identify as
        // enemies. A neutral entity can still become a target through retaliation or Protect.
        if (candidate instanceof Enemy) {
            return true;
        }
        if (candidate instanceof Mob mob) {
            LivingEntity threatened = mob.getTarget();
            return threatened != null && isProtectedAlly(target, owner, threatened);
        }
        return false;
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

        if (McaSubordinateAdapter.isGuard(target)) {
            // Guards already own a real MCA combat package. ATTACK_TARGET is the native entry point
            // that also drives their guard equipment/clothing state.
            target.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, candidate);
            return;
        }

        // Ordinary MCA villagers do not have the guard attack package. In particular, feeding them
        // NEAREST_HOSTILE activates MCA's defensive/panic behaviour and makes them act as if THEY are
        // under attack. For commanded civilians we instead drive a narrow combat action, leaving their
        // Brain, schedule, family/work memories and profession untouched.
        driveMcaCivilCombat(target, candidate);
    }

    private static void driveMcaCivilCombat(Mob target, LivingEntity candidate) {
        if (driveMcaCivilBowCombat(target, candidate)) {
            return;
        }

        // MCA civilians intentionally use EquipmentSet.NAKED for self-defence, even though MCA's
        // equipment code already knows how to choose the best sword from their inventory. Project the
        // same best-sword choice into the dominant slot only while Re:Monster is commanding combat.
        McaSubordinateAdapter.equipBestMeleeWeaponFromInventory(target);

        target.getLookControl().setLookAt(candidate, 30.0F, 30.0F);
        target.getNavigation().moveTo(candidate, 0.8D);

        double reach = target.getBbWidth() * 2.0D + candidate.getBbWidth();
        if (target.distanceToSqr(candidate) > reach * reach) {
            return;
        }

        long gameTime = target.level().getGameTime();
        long nextAttack = target.getPersistentData().getLong(NEXT_MCA_CIVIL_ATTACK_TICK);
        if (gameTime < nextAttack) {
            return;
        }

        target.swing(InteractionHand.MAIN_HAND);
        target.doHurtTarget(candidate);
        target.getPersistentData().putLong(NEXT_MCA_CIVIL_ATTACK_TICK, gameTime + MCA_CIVIL_ATTACK_COOLDOWN);
    }

    /**
     * Mirrors the important parts of MCA 7.7.36-beta.3 BowTask without installing the guard/archer
     * Brain package on a civilian. A carried bow takes priority over melee while commanded combat is
     * active. The villager uses MCA's own ranged-attack implementation to create the projectile.
     */
    private static boolean driveMcaCivilBowCombat(Mob target, LivingEntity candidate) {
        if (!McaSubordinateAdapter.equipBestBowFromInventory(target)
                || !(target.getMainHandItem().getItem() instanceof BowItem bow)) {
            return false;
        }

        target.getLookControl().setLookAt(candidate, 30.0F, 30.0F);
        boolean visible = target.getSensing().hasLineOfSight(candidate);
        double distanceSquared = target.distanceToSqr(candidate);
        double bowRange = Math.min(MCA_CIVIL_BOW_MAX_RANGE, bow.getDefaultProjectileRange());

        if (!visible || distanceSquared > bowRange * bowRange) {
            stopCivilBowDraw(target);
            target.getNavigation().moveTo(candidate, 0.8D);
            return true;
        }

        target.getNavigation().stop();
        long gameTime = target.level().getGameTime();
        long nextAttack = target.getPersistentData().getLong(NEXT_MCA_CIVIL_ATTACK_TICK);
        if (gameTime < nextAttack) {
            return true;
        }

        if (!target.isUsingItem()) {
            target.startUsingItem(InteractionHand.MAIN_HAND);
            return true;
        }

        if (!(target.getUseItem().getItem() instanceof BowItem)) {
            return true;
        }

        int pullTicks = target.getTicksUsingItem();
        if (pullTicks < MCA_CIVIL_BOW_DRAW_TICKS) {
            return true;
        }

        target.stopUsingItem();
        if (McaSubordinateAdapter.performHeldBowAttack(target, candidate, BowItem.getPowerForTime(pullTicks))) {
            target.getPersistentData().putLong(
                    NEXT_MCA_CIVIL_ATTACK_TICK,
                    gameTime + MCA_CIVIL_ATTACK_COOLDOWN
            );
        }
        return true;
    }

    private static void stopCivilBowDraw(Mob target) {
        if (target.isUsingItem() && target.getUseItem().getItem() instanceof BowItem) {
            target.stopUsingItem();
        }
    }

    private static void clearTargetState(Mob target) {
        if (!McaSubordinateAdapter.isMcaVillager(target)) {
            SubordinateHelper.removeTarget(target);
            return;
        }

        boolean hadCombatTarget = target.getTarget() != null
                || target.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET);
        stopCivilBowDraw(target);
        target.setTarget(null);
        target.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
        target.getPersistentData().remove(NEXT_MCA_CIVIL_ATTACK_TICK);
        if (hadCombatTarget) {
            target.getNavigation().stop();
        }
    }
}
