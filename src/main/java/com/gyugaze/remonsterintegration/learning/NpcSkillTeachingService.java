package com.gyugaze.remonsterintegration.learning;

import com.gyugaze.remonsterintegration.ReMonsterIntegration;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.ability.SkillHelper;
import io.github.manasmods.tensura.ability.TensuraSkill;
import io.github.manasmods.tensura.ability.TensuraSkillInstance;
import io.github.manasmods.tensura.item.misc.BattlewillManualItem;
import io.github.manasmods.tensura.item.misc.MagicTomeItem;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Method;

/**
 * Lets NPCs actually study Tensura manuals/tomes they carry without pretending to be a Player.
 *
 * Tensura's item UI is player-gated, but the underlying learning primitive is not: the native items
 * construct an acquisition-mastery instance and hand it to SkillHelper.learnSkill. We reproduce only
 * that primitive for LivingEntity mobs and consume the real carried item after learning succeeds.
 */
public final class NpcSkillTeachingService {
    private static final String NEXT_CHECK_KEY = "remonster_teaching_next_check";
    private static final int CHECK_INTERVAL_TICKS = 40;

    private NpcSkillTeachingService() {
    }

    public static void tick(Mob mob) {
        if (mob.level().isClientSide() || !mob.isAlive()) {
            return;
        }

        long gameTime = mob.level().getGameTime();
        if (gameTime < mob.getPersistentData().getLong(NEXT_CHECK_KEY)) {
            return;
        }
        mob.getPersistentData().putLong(NEXT_CHECK_KEY, gameTime + CHECK_INTERVAL_TICKS);

        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND}) {
            ItemStack stack = mob.getItemBySlot(slot);
            if (tryStudy(mob, stack)) {
                return;
            }
        }

        Container inventory = resolveInventory(mob);
        if (inventory == null) {
            return;
        }
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (tryStudy(mob, inventory.getItem(i))) {
                inventory.setChanged();
                return;
            }
        }
    }

    private static boolean tryStudy(Mob mob, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        ManasSkill skill = resolveManualSkill(mob, stack);
        if (skill == null || SkillAPI.getSkillsFrom(mob).getSkill(skill).isPresent()) {
            return false;
        }

        TensuraSkillInstance learning = new TensuraSkillInstance(skill);
        int mastery = skill instanceof TensuraSkill tensuraSkill
                ? tensuraSkill.getAcquirementMastery(mob)
                : -TensuraSkill.BASE_CONFIG.Learning.learningPointRequirement;
        learning.setMastery(mastery);

        if (!SkillHelper.learnSkill(mob, learning)) {
            return false;
        }

        stack.shrink(1);
        ReMonsterIntegration.LOGGER.debug(
                "Entity {} studied {} from carried manual/tome",
                mob.getUUID(),
                skill.getRegistryName()
        );
        return true;
    }

    private static ManasSkill resolveManualSkill(Mob mob, ItemStack stack) {
        if (stack.getItem() instanceof BattlewillManualItem manual) {
            ManasSkill bound = manual.getSkill(stack);
            return bound != null ? bound : invokeRandom(manual, "getRandomBattlewill", mob);
        }
        if (stack.getItem() instanceof MagicTomeItem tome) {
            ManasSkill bound = tome.getSkill(stack);
            return bound != null ? bound : invokeRandom(tome, "getRandomAspectual", mob);
        }
        return null;
    }

    private static ManasSkill invokeRandom(Object item, String methodName, Mob mob) {
        try {
            Method method = item.getClass().getDeclaredMethod(methodName, net.minecraft.world.entity.LivingEntity.class);
            method.setAccessible(true);
            Object result = method.invoke(item, mob);
            return result instanceof ManasSkill skill ? skill : null;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            ReMonsterIntegration.LOGGER.debug(
                    "Could not resolve random skill from {} for {}",
                    item.getClass().getName(),
                    mob.getUUID(),
                    exception
            );
            return null;
        }
    }

    private static Container resolveInventory(Mob mob) {
        try {
            Method getInventory = mob.getClass().getMethod("getInventory");
            Object result = getInventory.invoke(mob);
            return result instanceof Container container ? container : null;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }
}
