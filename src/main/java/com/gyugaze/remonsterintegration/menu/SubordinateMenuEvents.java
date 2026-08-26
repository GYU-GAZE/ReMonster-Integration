package com.gyugaze.remonsterintegration.menu;

import com.gyugaze.remonsterintegration.network.SubordinateScreenContextPayload;
import com.gyugaze.remonsterintegration.tensura.TensuraSubordinateBridge;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Server-side subordinate interaction helpers.
 *
 * Native-screen entities are never cancelled here. We only send an ownership-authorized context
 * hint to the client before the owning mod opens its screen. Fallback screens are opened later by
 * PlayerInteractOnMixin only when the complete vanilla/mod/item interaction chain returned PASS.
 */
public final class SubordinateMenuEvents {
    private SubordinateMenuEvents() {
    }

    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof LivingEntity target) || !(target instanceof Mob)) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (!TensuraSubordinateBridge.isSubordinateOf(serverPlayer, target)) {
            return;
        }

        if (SubordinateMenuPolicy.hasKnownNativeMenu(target)) {
            PacketDistributor.sendToPlayer(
                    serverPlayer,
                    new SubordinateScreenContextPayload(target.getId())
            );
        }
    }

    /** Opens the small Re:Monster fallback for a subordinate with no native inventory/menu. */
    public static void openFallback(ServerPlayer serverPlayer, LivingEntity target) {
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
