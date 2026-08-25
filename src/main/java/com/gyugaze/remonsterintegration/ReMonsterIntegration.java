package com.gyugaze.remonsterintegration;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(ReMonsterIntegration.MOD_ID)
public final class ReMonsterIntegration {
    public static final String MOD_ID = "remonster_integration";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ReMonsterIntegration(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("[Re:Monster Integration] loaded");
    }
}
