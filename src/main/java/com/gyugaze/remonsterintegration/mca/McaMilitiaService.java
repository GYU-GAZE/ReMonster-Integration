package com.gyugaze.remonsterintegration.mca;

import com.gyugaze.remonsterintegration.tensura.TensuraSubordinateBridge;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;

import java.util.Comparator;
import java.util.Optional;

/** Armed MCA civilian support without replacing profession/schedule/Brain or native self-defence. */
public final class McaMilitiaService {
    private static final String ENGAGED_UNTIL_KEY = "remonster_militia_engaged_until";
    private static final String NEXT_ATTACK_KEY = "remonster_militia_next_attack";
    private static final int TICK_INTERVAL = 5;
    private static final int ATTACK_COOLDOWN = 20;
    private static final int BOW_DRAW_TICKS = 20;
    private static final double MAX_PROTECT_RANGE = 16.0D;
    private static final double MAX_BOW_RANGE = 15.0D;
    private static final double RETREAT_HEALTH_RATIO = 0.25D;

    private McaMilitiaService() {
    }

    public static void tick(Mob villager) {
        if (villager.level().isClientSide()
                || !McaSubordinateAdapter.isMcaVillager(villager)
                || McaSubordinateAdapter.isGuard(villager)
                || villager.tickCount % TICK_INTERVAL != 0) {
            return;
        }

        if (TensuraSubordinateBridge.getResolvedOwner(villager).isPresent()) {
            cleanupExpiredCombat(villager, true);
            return;
        }

        if (healthRatio(villager) < RETREAT_HEALTH_RATIO) {
            disengage(villager);
            return;
        }

        LivingEntity threat = findThreat(villager);
        if (threat == null) {
            stopBow(villager);
            cleanupExpiredCombat(villager, false);
            return;
        }

        if (!ensureArmed(villager)) {
            stopBow(villager);
            cleanupExpiredCombat(villager, true);
            return;
        }

        villager.getPersistentData().putLong(ENGAGED_UNTIL_KEY, villager.level().getGameTime() + 30L);
        villager.setTarget(threat);
        driveCombat(villager, threat);
    }

    /**
     * Presentation-only panic rule. MCA's actual PANIC activity and self-defence tasks remain native.
     * We hide the arms-open panic pose only while a non-guard civilian is armed, healthy and actually
     * fighting. Owned/commanded civilians are included because they use a Mob target too. Below 25%
     * health this immediately becomes false and MCA's normal visible panic returns.
     */
    public static boolean shouldSuppressPanic(LivingEntity villager) {
        if (!McaSubordinateAdapter.isMcaVillager(villager)
                || McaSubordinateAdapter.isGuard(villager)
                || healthRatio(villager) < RETREAT_HEALTH_RATIO
                || !McaCombatWeaponPolicy.hasCombatWeaponInInventory(villager)) {
            return false;
        }

        if (villager instanceof Mob mob && mob.getTarget() != null && mob.getTarget().isAlive()) {
            return true;
        }
        if (villager.level().getGameTime() <= villager.getPersistentData().getLong(ENGAGED_UNTIL_KEY)) {
            return true;
        }
        return hasImmediateThreat(villager);
    }

    private static boolean hasImmediateThreat(LivingEntity villager) {
        LivingEntity attacker = villager.getLastHurtByMob();
        return attacker != null && attacker.isAlive();
    }

    private static LivingEntity findThreat(Mob villager) {
        LivingEntity attacker = villager.getLastHurtByMob();
        if (validThreat(villager, attacker)) {
            return attacker;
        }

        double range = Math.min(MAX_PROTECT_RANGE, Math.max(8.0D, villager.getAttributeValue(Attributes.FOLLOW_RANGE)));
        LivingEntity allyAttacker = villager.level().getEntitiesOfClass(
                        LivingEntity.class,
                        villager.getBoundingBox().inflate(range),
                        candidate -> McaSubordinateAdapter.isMcaVillager(candidate) && candidate != villager
                ).stream()
                .map(LivingEntity::getLastHurtByMob)
                .filter(candidate -> validThreat(villager, candidate))
                .min(Comparator.comparingDouble(villager::distanceToSqr))
                .orElse(null);
        if (allyAttacker != null) {
            return allyAttacker;
        }

        return villager.level().getEntitiesOfClass(
                        Mob.class,
                        villager.getBoundingBox().inflate(range),
                        candidate -> candidate != villager
                                && validThreat(villager, candidate)
                                && candidate.getTarget() != null
                                && McaSubordinateAdapter.isMcaVillager(candidate.getTarget())
                ).stream()
                .min(Comparator.comparingDouble(villager::distanceToSqr))
                .map(LivingEntity.class::cast)
                .orElse(null);
    }

    private static boolean validThreat(Mob villager, LivingEntity candidate) {
        return candidate != null
                && candidate != villager
                && candidate.isAlive()
                && villager.canAttack(candidate)
                && !McaSubordinateAdapter.isMcaVillager(candidate);
    }

    private static boolean ensureArmed(Mob villager) {
        return McaSubordinateAdapter.equipBestBowFromInventory(villager)
                || McaSubordinateAdapter.equipBestMeleeWeaponFromInventory(villager);
    }

    private static void driveCombat(Mob villager, LivingEntity threat) {
        Optional<ItemStack> heldBow = McaSubordinateAdapter.getHeldBowStack(villager);
        if (heldBow.isPresent() && heldBow.get().getItem() instanceof BowItem bow) {
            driveBow(villager, threat, bow);
            return;
        }

        McaBowPositioningService.reset(villager);
        McaSubordinateAdapter.equipBestMeleeWeaponFromInventory(villager);
        villager.getLookControl().setLookAt(threat, 30.0F, 30.0F);
        villager.getNavigation().moveTo(threat, 0.8D);

        double reach = villager.getBbWidth() * 2.0D + threat.getBbWidth();
        if (villager.distanceToSqr(threat) > reach * reach || onCooldown(villager)) {
            return;
        }
        villager.swing(InteractionHand.MAIN_HAND);
        villager.doHurtTarget(threat);
        setCooldown(villager);
    }

    private static void driveBow(Mob villager, LivingEntity threat, BowItem bow) {
        villager.getLookControl().setLookAt(threat, 30.0F, 30.0F);
        double range = Math.min(MAX_BOW_RANGE, bow.getDefaultProjectileRange());
        McaBowPositioningService.Positioning positioning = McaBowPositioningService.update(villager, threat, range);

        if (positioning == McaBowPositioningService.Positioning.APPROACH) {
            stopBow(villager);
            return;
        }
        if (positioning == McaBowPositioningService.Positioning.EMERGENCY_FLEE) {
            stopBow(villager);
            return;
        }

        if (onCooldown(villager)) {
            return;
        }
        if (!villager.isUsingItem()) {
            McaSubordinateAdapter.getHeldBowHand(villager).ifPresent(villager::startUsingItem);
            return;
        }
        if (!(villager.getUseItem().getItem() instanceof BowItem)) {
            stopBow(villager);
            return;
        }

        int pullTicks = villager.getTicksUsingItem();
        if (pullTicks < BOW_DRAW_TICKS) {
            return;
        }
        villager.stopUsingItem();
        if (McaSubordinateAdapter.performHeldBowAttack(villager, threat, BowItem.getPowerForTime(pullTicks))) {
            setCooldown(villager);
        }
    }

    private static boolean onCooldown(Mob villager) {
        return villager.level().getGameTime() < villager.getPersistentData().getLong(NEXT_ATTACK_KEY);
    }

    private static void setCooldown(Mob villager) {
        villager.getPersistentData().putLong(NEXT_ATTACK_KEY, villager.level().getGameTime() + ATTACK_COOLDOWN);
    }

    private static void cleanupExpiredCombat(Mob villager, boolean force) {
        long gameTime = villager.level().getGameTime();
        long engagedUntil = villager.getPersistentData().getLong(ENGAGED_UNTIL_KEY);
        if (!force && gameTime <= engagedUntil) {
            return;
        }
        stopBow(villager);
        McaBowPositioningService.reset(villager);
        villager.getPersistentData().remove(ENGAGED_UNTIL_KEY);
        villager.getPersistentData().remove(NEXT_ATTACK_KEY);
        villager.setTarget(null);
    }

    private static void disengage(Mob villager) {
        stopBow(villager);
        McaBowPositioningService.reset(villager);
        villager.getPersistentData().remove(ENGAGED_UNTIL_KEY);
        villager.getPersistentData().remove(NEXT_ATTACK_KEY);
        villager.setTarget(null);
        villager.getNavigation().stop();
    }

    private static void stopBow(Mob villager) {
        if (villager.isUsingItem() && villager.getUseItem().getItem() instanceof BowItem) {
            villager.stopUsingItem();
        }
    }

    private static double healthRatio(LivingEntity entity) {
        return entity.getMaxHealth() <= 0.0F ? 0.0D : entity.getHealth() / entity.getMaxHealth();
    }
}
