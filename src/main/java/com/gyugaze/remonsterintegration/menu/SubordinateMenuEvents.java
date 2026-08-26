package com.gyugaze.remonsterintegration.menu;

import com.gyugaze.remonsterintegration.tensura.TensuraSubordinateBridge;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Opens Re:Monster's compact fallback screen only for owned subordinates that have no native screen.
 *
 * Native-screen entities are never intercepted here; their owning mod keeps the original interaction
 * and Re:Monster decorates the screen client-side instead.
 */
public final class SubordinateMenuEvents {
    private SubordinateMenuEvents() {
    }

    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof LivingEntity target) || !(target instanceof Mob)) {
            return;
        }

        Player player = event.getEntity();
        if (!TensuraSubordinateBridge.isSubordinateOf(player, target)) {
            return;
        }

        if (!SubordinateMenuPolicy.shouldUseFallbackMenu(target)) {
            return;
        }

        // Item interactions keep their original behavior (feeding, dyeing, healing, mod-specific
        // actions, etc.). Sneak interactions are also left alone for Thought Communication/native
        // command flows. Empty-hand normal right-click is the Re:Monster fallback gesture.
        if (player.isShiftKeyDown() || !event.getItemStack().isEmpty()) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        serverPlayer.openMenu(
                new SimpleMenuProvider(
                        (containerId, inventory, menuPlayer) ->
                                new SubordinateOverviewMenu(containerId, inventory, target),
                        Component.literal("Subordinate")
                ),
                buffer -> SubordinateOverviewMenu.writeOpeningData(buffer, target)
        );
    }
}
