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

        boolean humanoid = target instanceof TensuraHumanoidEntity;
        boolean tensuraCreature = target instanceof TensuraTamableEntity;

        // Tensura humanoids keep normal right-click for the upstream EP/equipment inventory. Their
        // shift interaction opens Re:Monster's shared status/skills/command overview instead.
        boolean humanoidShiftOverview = humanoid && player.isShiftKeyDown();

        // Non-humanoid Tensura creatures do not have the rich humanoid inventory, so empty-hand normal
        // right-click opens the overview. Shift remains upstream command cycling for these creatures.
        boolean emptyHandTensuraCreatureOverview = !humanoid
                && !player.isShiftKeyDown()
                && event.getItemStack().isEmpty()
                && tensuraCreature;

        // External companions preserve normal interaction (MCA dialogue, horse inventory, vanilla pet
        // actions) and use shift-right-click for the common overview.
        boolean externalShiftOverview = player.isShiftKeyDown() && !tensuraCreature;

        if (!humanoidShiftOverview && !emptyHandTensuraCreatureOverview && !externalShiftOverview) {
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
