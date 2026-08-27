package com.gyugaze.remonsterintegration.mca;

import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;

/** Shared definition of what counts as an actually armed MCA civilian. */
public final class McaCombatWeaponPolicy {
    private static final double EPSILON = 1.0E-6D;

    private McaCombatWeaponPolicy() {
    }

    public static boolean hasCombatWeaponInInventory(LivingEntity villager) {
        if (!McaSubordinateAdapter.isMcaVillager(villager)) {
            return false;
        }
        Container inventory = getInventory(villager);
        if (inventory == null) {
            return hasDamagingMeleeWeaponInMainHand(villager)
                    || villager.getMainHandItem().getItem() instanceof BowItem;
        }
        double baseDamage = villager.getAttributeBaseValue(Attributes.ATTACK_DAMAGE);
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.getItem() instanceof BowItem || getMainHandAttackDamage(stack, baseDamage) > baseDamage + EPSILON) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasDamagingMeleeWeaponInMainHand(LivingEntity villager) {
        double baseDamage = villager.getAttributeBaseValue(Attributes.ATTACK_DAMAGE);
        return getMainHandAttackDamage(villager.getMainHandItem(), baseDamage) > baseDamage + EPSILON;
    }

    private static Container getInventory(LivingEntity villager) {
        try {
            Object value = villager.getClass().getMethod("getInventory").invoke(villager);
            return value instanceof Container container ? container : null;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
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
        return withMultipliedBase * modifiers[2];
    }
}
