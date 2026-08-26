package com.gyugaze.remonsterintegration.evolution;

import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Keeps Re:Monster semantic species state when Minecraft replaces a player entity. */
public final class SpeciesStateEvents {
    private SpeciesStateEvents() {
    }

    public static void onPlayerClone(PlayerEvent.Clone event) {
        SpeciesStateService.copyState(event.getOriginal(), event.getEntity());
        SpeciesScaleService.applyCurrent(event.getEntity());
    }
}
