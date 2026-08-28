package com.gyugaze.remonsterintegration.growth;

import com.gyugaze.remonsterintegration.ReMonsterIntegration;
import com.gyugaze.remonsterintegration.tensura.TensuraExistenceBridge;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.fml.ModList;

/**
 * Fallback EP-to-physical-stat growth for packs that do not provide a dedicated existence scaler.
 *
 * EPGains is the authority in the target modpack. When it is present this service removes any old
 * Re:Monster growth modifiers and stays dormant, preventing two independent EP power curves from
 * stacking on the same entity. The fallback remains available for standalone addon use without
 * EPGains.
 */
public final class UniversalCharacterGrowthService {
    private static final String EPGAINS_MOD_ID = "epgains";
    private static final String BASELINE_EP_KEY = "remonster_growth_baseline_ep";
    private static final String LAST_EP_KEY = "remonster_growth_last_ep";
    private static final int CHECK_INTERVAL_TICKS = 40;
    private static final double LOG_2 = Math.log(2.0D);
    private static final double BONUS_PER_EP_DOUBLING = 0.10D;
    private static final double MAX_CORE_BONUS = 2.0D;

    private static final ResourceLocation HEALTH_ID = id("growth_max_health");
    private static final ResourceLocation ATTACK_ID = id("growth_attack_damage");
    private static final ResourceLocation ARMOR_ID = id("growth_armor");
    private static final ResourceLocation TOUGHNESS_ID = id("growth_armor_toughness");
    private static final ResourceLocation SPEED_ID = id("growth_movement_speed");
    private static final ResourceLocation RANGE_ID = id("growth_follow_range");

    private UniversalCharacterGrowthService() {
    }

    public static void tick(Mob mob) {
        if (mob.level().isClientSide() || !mob.isAlive() || mob.tickCount % CHECK_INTERVAL_TICKS != 0) {
            return;
        }
        reapply(mob);
    }

    public static void reapply(LivingEntity entity) {
        if (hasDedicatedExistenceScaling()) {
            clearFallbackState(entity);
            return;
        }

        double currentEp = TensuraExistenceBridge.getBaseMaxEp(entity);
        if (!Double.isFinite(currentEp) || currentEp <= 0.0D) {
            return;
        }

        CompoundTag data = entity.getPersistentData();
        double baselineEp = data.getDouble(BASELINE_EP_KEY);
        if (!Double.isFinite(baselineEp) || baselineEp <= 0.0D) {
            data.putDouble(BASELINE_EP_KEY, currentEp);
            data.putDouble(LAST_EP_KEY, currentEp);
            removeGrowthModifiers(entity);
            return;
        }

        double previousEp = data.getDouble(LAST_EP_KEY);
        if (Double.isFinite(previousEp) && Math.abs(previousEp - currentEp) < 1.0E-6D) {
            return;
        }
        data.putDouble(LAST_EP_KEY, currentEp);

        double ratio = Math.max(1.0D, currentEp / baselineEp);
        double doublings = Math.log(ratio) / LOG_2;
        double coreBonus = Math.min(MAX_CORE_BONUS, Math.max(0.0D, doublings * BONUS_PER_EP_DOUBLING));

        float oldMaxHealth = entity.getMaxHealth();
        float health = entity.getHealth();
        double healthRatio = oldMaxHealth > 0.0F ? health / oldMaxHealth : 1.0D;

        replace(entity, Attributes.MAX_HEALTH, HEALTH_ID, coreBonus);
        replace(entity, Attributes.ATTACK_DAMAGE, ATTACK_ID, coreBonus);
        replace(entity, Attributes.ARMOR, ARMOR_ID, coreBonus * 0.50D);
        replace(entity, Attributes.ARMOR_TOUGHNESS, TOUGHNESS_ID, coreBonus * 0.50D);
        replace(entity, Attributes.MOVEMENT_SPEED, SPEED_ID, Math.min(0.30D, coreBonus * 0.15D));
        replace(entity, Attributes.FOLLOW_RANGE, RANGE_ID, Math.min(0.50D, coreBonus * 0.25D));

        if (oldMaxHealth > 0.0F && entity.isAlive()) {
            entity.setHealth((float) Math.max(0.01D, Math.min(entity.getMaxHealth(), entity.getMaxHealth() * healthRatio)));
        }
    }

    public static void copyBaselineState(LivingEntity from, LivingEntity to) {
        if (hasDedicatedExistenceScaling()) {
            clearFallbackState(to);
            return;
        }
        CompoundTag source = from.getPersistentData();
        CompoundTag target = to.getPersistentData();
        if (source.contains(BASELINE_EP_KEY)) {
            target.putDouble(BASELINE_EP_KEY, source.getDouble(BASELINE_EP_KEY));
        }
        target.remove(LAST_EP_KEY);
    }

    private static boolean hasDedicatedExistenceScaling() {
        return ModList.get().isLoaded(EPGAINS_MOD_ID);
    }

    private static void clearFallbackState(LivingEntity entity) {
        removeGrowthModifiers(entity);
        entity.getPersistentData().remove(BASELINE_EP_KEY);
        entity.getPersistentData().remove(LAST_EP_KEY);
    }

    private static void removeGrowthModifiers(LivingEntity entity) {
        remove(entity, Attributes.MAX_HEALTH, HEALTH_ID);
        remove(entity, Attributes.ATTACK_DAMAGE, ATTACK_ID);
        remove(entity, Attributes.ARMOR, ARMOR_ID);
        remove(entity, Attributes.ARMOR_TOUGHNESS, TOUGHNESS_ID);
        remove(entity, Attributes.MOVEMENT_SPEED, SPEED_ID);
        remove(entity, Attributes.FOLLOW_RANGE, RANGE_ID);
    }

    private static void replace(
            LivingEntity entity,
            net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
            ResourceLocation id,
            double amount
    ) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null) {
            return;
        }

        instance.removeModifier(id);
        if (amount > 1.0E-9D) {
            instance.addPermanentModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    private static void remove(
            LivingEntity entity,
            net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
            ResourceLocation id
    ) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null) {
            instance.removeModifier(id);
        }
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(ReMonsterIntegration.MOD_ID, path);
    }
}
