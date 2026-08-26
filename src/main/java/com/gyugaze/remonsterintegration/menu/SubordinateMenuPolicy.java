package com.gyugaze.remonsterintegration.menu;

import com.gyugaze.remonsterintegration.ReMonsterIntegration;
import com.gyugaze.remonsterintegration.mca.McaSubordinateAdapter;
import io.github.manasmods.tensura.entity.template.TensuraHumanoidEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HasCustomInventoryScreen;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.horse.AbstractHorse;

/**
 * Decides whether a subordinate should keep an owning mod's native screen or use Re:Monster's
 * lightweight fallback screen.
 *
 * HasCustomInventoryScreen is the generic vanilla contract used by mounts/creatures that own an
 * inventory UI. This catches TensuraMountEntity (Basilisk, etc.) without enumerating species and is
 * also friendly to future modded mounts that follow the vanilla contract.
 *
 * The entity-type tag remains public/data-driven so integrations with custom screen systems that do
 * not implement HasCustomInventoryScreen can opt into native-screen decoration without hard-coding
 * another class check here.
 */
public final class SubordinateMenuPolicy {
    public static final TagKey<EntityType<?>> NATIVE_SUBORDINATE_MENU = TagKey.create(
            Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(ReMonsterIntegration.MOD_ID, "native_subordinate_menu")
    );

    private SubordinateMenuPolicy() {
    }

    public static boolean hasKnownNativeMenu(LivingEntity target) {
        return target instanceof TensuraHumanoidEntity
                || target instanceof AbstractHorse
                || target instanceof HasCustomInventoryScreen
                || McaSubordinateAdapter.isMcaVillager(target)
                || isExplicitNativeTagged(target);
    }

    public static boolean isExplicitNativeTagged(LivingEntity target) {
        return target.getType().is(NATIVE_SUBORDINATE_MENU);
    }

    public static boolean shouldUseFallbackMenu(LivingEntity target) {
        return target instanceof Mob && !hasKnownNativeMenu(target);
    }
}
