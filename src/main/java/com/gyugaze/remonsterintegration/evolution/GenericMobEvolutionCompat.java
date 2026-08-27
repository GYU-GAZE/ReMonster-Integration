package com.gyugaze.remonsterintegration.evolution;

import com.gyugaze.remonsterintegration.ReMonsterIntegration;
import com.gyugaze.remonsterintegration.compat.MobChampionsBridge;
import com.gyugaze.remonsterintegration.growth.ExternalEnergyFoundationService;
import com.gyugaze.remonsterintegration.growth.UniversalCharacterGrowthService;
import com.gyugaze.remonsterintegration.tensura.TensuraExistenceBridge;
import com.gyugaze.remonsterintegration.tensura.TensuraSkillBridge;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.impl.SkillStorage;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.storage.ep.IExistence;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.EventHooks;

import java.lang.reflect.Method;

/** Controlled non-MCA physical evolution that preserves portable individual progression only. */
public final class GenericMobEvolutionCompat {
    private static final String BETTER_SUBS_CLASS = "com.trbeyond.tensura_better_subs.BetterSubs";

    private GenericMobEvolutionCompat() {
    }

    public static ApplyResult apply(
            LivingEntity source,
            SpeciesBodyProfile.EntityBody body,
            net.minecraft.resources.ResourceLocation targetSpecies
    ) {
        if (!(source.level() instanceof ServerLevel level)) {
            return ApplyResult.failure(source, "generic entity replacement requires a server level");
        }

        EntityType<?> targetType = EntityType.byString(body.entityType().toString()).orElse(null);
        if (targetType == null) {
            return ApplyResult.failure(source, "unknown entity type " + body.entityType());
        }
        if (source.getType() == targetType) {
            return ApplyResult.success(source, "entity body already " + body.entityType());
        }

        Entity created = targetType.create(level);
        if (!(created instanceof LivingEntity target)) {
            if (created != null) {
                created.discard();
            }
            return ApplyResult.failure(source, "target entity type is not a LivingEntity: " + body.entityType());
        }

        boolean added = false;
        try {
            target.moveTo(source.getX(), source.getY(), source.getZ(), source.getYRot(), source.getXRot());
            target.setYHeadRot(source.getYHeadRot());

            // Seed third-party spawn identity before finalizeSpawn. MobChampions otherwise sees a fresh
            // entity and rolls a brand-new rarity during physical evolution.
            MobChampionsBridge.prepareEvolutionSpawn(source, target);
            if (target instanceof Mob targetMob) {
                EventHooks.finalizeMobSpawn(
                        targetMob,
                        level,
                        level.getCurrentDifficultyAt(source.blockPosition()),
                        MobSpawnType.CONVERSION,
                        null
                );
                if (targetMob.isSpawnCancelled()) {
                    target.discard();
                    return ApplyResult.failure(source, "target finalizeSpawn was cancelled");
                }
            }

            if (!level.addFreshEntity(target)) {
                target.discard();
                return ApplyResult.failure(source, "target entity could not be added to the level");
            }
            added = true;

            target.setDeltaMovement(source.getDeltaMovement());
            target.setCustomName(source.getCustomName());
            target.setCustomNameVisible(source.isCustomNameVisible());
            target.setSilent(source.isSilent());
            if (target instanceof Mob targetMob) {
                if (source instanceof Mob sourceMob) {
                    targetMob.setNoAi(sourceMob.isNoAi());
                }
                targetMob.setPersistenceRequired();
            }

            copyReMonsterPersistentData(source, target);
            SpeciesStateService.copyState(source, target);
            UniversalCharacterGrowthService.copyBaselineState(source, target);
            copyExistenceState(source, target);
            ExternalEnergyFoundationService.seedCapacity(target);
            copySkills(source, target);
            copyBetterSubordinatesProgression(source, target);
            copyVanillaTameState(source, target);

            if (body.reinforceBaseStats()) {
                reinforceBaseStats(source, target);
            }

            // Re-apply the SOURCE champion rank to the new species after its native finalizeSpawn.
            // Equipment is copied afterwards so MobChampions cannot invent new gear during evolution.
            MobChampionsBridge.finishEvolutionSpawn(source, target);
            if (body.preserveEquipment()) {
                copyEquipment(source, target);
            }

            double healthRatio = source.getMaxHealth() > 0.0F
                    ? Math.max(0.0D, Math.min(1.0D, source.getHealth() / source.getMaxHealth()))
                    : 1.0D;
            SpeciesStateService.setSpecies(target, targetSpecies);
            UniversalCharacterGrowthService.reapply(target);
            target.setHealth((float) Math.max(0.01D, target.getMaxHealth() * healthRatio));

            source.stopRiding();
            source.discard();
            return ApplyResult.success(target, "entity replacement -> " + body.entityType());
        } catch (RuntimeException | LinkageError exception) {
            if (added || target.isAlive()) {
                target.discard();
            }
            ReMonsterIntegration.LOGGER.error(
                    "Failed controlled evolution replacement {} -> {}",
                    source.getType(),
                    body.entityType(),
                    exception
            );
            return ApplyResult.failure(source, "controlled entity replacement failed: " + exception.getClass().getSimpleName());
        }
    }

    private static void copyExistenceState(LivingEntity source, LivingEntity target) {
        IExistence from = TensuraStorages.getExistenceFrom(source);
        IExistence to = TensuraStorages.getExistenceFrom(target);

        double targetBaseEp = Math.max(
                TensuraExistenceBridge.getBaseMaxEp(target),
                TensuraExistenceBridge.getBaseMaxEp(source)
        );
        TensuraExistenceBridge.setBaseMaxEpSafely(target, targetBaseEp);

        double sourceMaxAura = Math.max(1.0D, TensuraExistenceBridge.getMaxAura(source));
        double sourceMaxMagicule = Math.max(1.0D, TensuraExistenceBridge.getMaxMagicule(source));
        double auraRatio = Math.max(0.0D, Math.min(1.0D, from.getAura() / sourceMaxAura));
        double magiculeRatio = Math.max(0.0D, Math.min(1.0D, from.getMagicule() / sourceMaxMagicule));
        TensuraExistenceBridge.setCurrentEnergy(
                target,
                TensuraExistenceBridge.getMaxAura(target) * auraRatio,
                TensuraExistenceBridge.getMaxMagicule(target) * magiculeRatio
        );

        to.setGainedEP(from.getGainedEP());
        to.setSpiritualHealth(from.getSpiritualHealth());
        to.setAlignment(from.getAlignment());
        to.setOriginalAlignment(from.getOriginalAlignment());
        to.setSoulPoints(from.getSoulPoints());
        to.setHumanKill(from.getHumanKill());
        to.setDemonLordSeed(from.isDemonLordSeed());
        to.setTrueDemonLord(from.isTrueDemonLord());
        to.setBlessed(from.isBlessed());
        to.setHeroEgg(from.isHeroEgg());
        to.setTrueHero(from.isTrueHero());
        to.setNameable(from.isNameable());
        to.setName(from.getName());
        to.setPermanentOwner(from.getPermanentOwner());
        to.setTemporaryOwner(from.getTemporaryOwner());
        to.setSummoner(from.getSummoner());
        to.setSpiritualForm(from.isSpiritualForm());
        to.markDirty();
    }

    private static void copySkills(LivingEntity source, LivingEntity target) {
        SkillStorage targetStorage = TensuraSkillBridge.getStorage(target);
        for (ManasSkillInstance instance : TensuraSkillBridge.getStorage(source).getLearnedSkills()) {
            targetStorage.updateSkill(instance.copy(), true);
        }
    }

    private static void copyBetterSubordinatesProgression(LivingEntity source, LivingEntity target) {
        try {
            Class<?> betterSubs = Class.forName(BETTER_SUBS_CLASS, false, source.getClass().getClassLoader());
            Method getter = betterSubs.getMethod("getSkillExpansionFrom", LivingEntity.class);
            Object from = getter.invoke(null, source);
            Object to = getter.invoke(null, target);
            if (from == null || to == null) {
                return;
            }

            CompoundTag tag = new CompoundTag();
            from.getClass().getMethod("save", CompoundTag.class).invoke(from, tag);
            to.getClass().getMethod("load", CompoundTag.class).invoke(to, tag.copy());
            to.getClass().getMethod("markDirty").invoke(to);
        } catch (ReflectiveOperationException | LinkageError exception) {
            ReMonsterIntegration.LOGGER.debug(
                    "Better Subordinates progression transfer unavailable for {} -> {}",
                    source.getUUID(),
                    target.getUUID(),
                    exception
            );
        }
    }

    private static void copyVanillaTameState(LivingEntity source, LivingEntity target) {
        if (source instanceof TamableAnimal from && target instanceof TamableAnimal to) {
            to.setOwnerUUID(from.getOwnerUUID());
            to.setTame(from.isTame(), false);
            to.setOrderedToSit(from.isOrderedToSit());
        }
    }

    private static void copyEquipment(LivingEntity source, LivingEntity target) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = source.getItemBySlot(slot);
            target.setItemSlot(slot, stack.copy());
        }
    }

    private static void reinforceBaseStats(LivingEntity source, LivingEntity target) {
        reinforce(source, target, Attributes.MAX_HEALTH);
        reinforce(source, target, Attributes.ATTACK_DAMAGE);
        reinforce(source, target, Attributes.ARMOR);
        reinforce(source, target, Attributes.ARMOR_TOUGHNESS);
        reinforce(source, target, Attributes.MOVEMENT_SPEED);
        reinforce(source, target, Attributes.FOLLOW_RANGE);
        reinforce(source, target, Attributes.KNOCKBACK_RESISTANCE);
    }

    private static void reinforce(
            LivingEntity source,
            LivingEntity target,
            net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute
    ) {
        AttributeInstance sourceAttribute = source.getAttribute(attribute);
        AttributeInstance targetAttribute = target.getAttribute(attribute);
        if (sourceAttribute != null && targetAttribute != null) {
            targetAttribute.setBaseValue(Math.max(targetAttribute.getBaseValue(), sourceAttribute.getBaseValue()));
        }
    }

    private static void copyReMonsterPersistentData(LivingEntity source, LivingEntity target) {
        CompoundTag from = source.getPersistentData();
        CompoundTag to = target.getPersistentData();
        for (String key : from.getAllKeys()) {
            if (key.startsWith("remonster_")) {
                net.minecraft.nbt.Tag value = from.get(key);
                if (value != null) {
                    to.put(key, value.copy());
                }
            }
        }
    }

    public record ApplyResult(boolean success, LivingEntity entity, String detail, String failure) {
        static ApplyResult success(LivingEntity entity, String detail) {
            return new ApplyResult(true, entity, detail, "");
        }

        static ApplyResult failure(LivingEntity entity, String reason) {
            return new ApplyResult(false, entity, "unchanged", reason);
        }
    }
}
