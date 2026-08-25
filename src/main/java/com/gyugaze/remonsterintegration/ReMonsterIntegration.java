package com.gyugaze.remonsterintegration;

import com.gyugaze.remonsterintegration.learning.KillLearningEvents;
import com.gyugaze.remonsterintegration.profile.MobProfileEntityEvents;
import com.gyugaze.remonsterintegration.profile.MobProfileReloadListener;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.slf4j.Logger;

@Mod(ReMonsterIntegration.MOD_ID)
public final class ReMonsterIntegration {
    public static final String MOD_ID = "remonster_integration";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ReMonsterIntegration(IEventBus modEventBus, ModContainer modContainer) {
        NeoForge.EVENT_BUS.addListener(this::onAddReloadListeners);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, MobProfileEntityEvents::onEntityJoinLevel);
        NeoForge.EVENT_BUS.addListener(KillLearningEvents::onLivingDeath);
        LOGGER.info("[Re:Monster Integration] loaded");
    }

    private void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new MobProfileReloadListener());
    }
}
