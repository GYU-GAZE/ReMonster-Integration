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
 * Opens Re:Monster's common subordinate overview without stealing important native interactions.
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

        if (target instanceof TensuraHumanoidEntity) {
            return;
        }

        boolean tensuraCreature = target instanceof TensuraTamableEntity;

        boolean emptyHandTensuraCreatureOverview = !player.isShiftKeyDown()
                && event.getItemStack().isEmpty()
                && tensuraCreature;

        boolean externalShiftOverview = player.isShiftKeyDown() && !tensuraCreature;

        if (!emptyHandTensuraCreatureOverview && !externalShiftOverview) {
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
