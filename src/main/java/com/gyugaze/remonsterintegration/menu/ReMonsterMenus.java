package com.gyugaze.remonsterintegration.menu;

import com.gyugaze.remonsterintegration.ReMonsterIntegration;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Menu registry for Re:Monster's cross-mod subordinate UI.
 */
public final class ReMonsterMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, ReMonsterIntegration.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<SubordinateOverviewMenu>> SUBORDINATE_OVERVIEW =
            MENUS.register("subordinate_overview", () -> IMenuTypeExtension.create(SubordinateOverviewMenu::new));

    private ReMonsterMenus() {
    }
}
