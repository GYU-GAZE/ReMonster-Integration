package com.gyugaze.remonsterintegration.menu;

import com.gyugaze.remonsterintegration.tensura.TensuraSubordinateBridge;
import io.github.manasmods.tensura.entity.template.TensuraHumanoidEntity;
import io.github.manasmods.tensura.entity.template.TensuraTamableEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Temporary fallback overview only for native Tensura creatures that have no humanoid inventory.
 *
 * Re:Monster must never steal native MCA, horse, pet or TensuraHumanoid interactions. The long-term
 * UI is a companion panel layered around native screens, not a replacement screen bound to a common
 * click gesture.
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

        // Humanoids retain the complete native Tensura inventory/status flow.
        if (target instanceof TensuraHumanoidEntity) {
            return;
        }

        // External mobs retain every native interaction, including horse shift-right-click inventory.
        if (!(target instanceof TensuraTamableEntity)) {
            return;
        }

        // Non-humanoid Tensura creatures currently lack the humanoid inventory screen. Keep a small
        // fallback overview on empty-hand normal right-click until the native-screen companion panel
        // is complete. Shift-right-click remains Tensura's native command cycle.
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
