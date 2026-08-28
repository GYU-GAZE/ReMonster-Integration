package com.gyugaze.remonsterintegration;

import com.gyugaze.remonsterintegration.command.IntegrationCommands;
import com.gyugaze.remonsterintegration.command.SpawnDiagnosticCommands;
import com.gyugaze.remonsterintegration.command.SubordinateProgressionCommands;
import com.gyugaze.remonsterintegration.compat.CompatibilityAuthorityPolicy;
import com.gyugaze.remonsterintegration.evolution.EvolutionCommands;
import com.gyugaze.remonsterintegration.evolution.EvolutionReloadListener;
import com.gyugaze.remonsterintegration.evolution.SpeciesBodyProfileReloadListener;
import com.gyugaze.remonsterintegration.evolution.SpeciesStateEvents;
import com.gyugaze.remonsterintegration.learning.KillLearningEvents;
import com.gyugaze.remonsterintegration.menu.ReMonsterMenus;
import com.gyugaze.remonsterintegration.menu.SubordinateMenuEvents;
import com.gyugaze.remonsterintegration.network.ReMonsterNetworking;
import com.gyugaze.remonsterintegration.profile.MobProfileEntityEvents;
import com.gyugaze.remonsterintegration.profile.MobProfileReloadListener;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;

@Mod(ReMonsterIntegration.MOD_ID)
public final class ReMonsterIntegration {
    public static final String MOD_ID = "remonster_integration";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ReMonsterIntegration(IEventBus modEventBus, ModContainer modContainer) {
        ReMonsterMenus.MENUS.register(modEventBus);
        modEventBus.addListener(ReMonsterNetworking::registerPayloads);

        NeoForge.EVENT_BUS.addListener(this::onAddReloadListeners);
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, MobProfileEntityEvents::onEntityJoinLevel);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, KillLearningEvents::onLivingDeath);
        NeoForge.EVENT_BUS.addListener(SubordinateMenuEvents::onEntityInteract);
        NeoForge.EVENT_BUS.addListener(SubordinateMenuEvents::onEntityInteractSpecific);
        NeoForge.EVENT_BUS.addListener(SpeciesStateEvents::onPlayerClone);
        CompatibilityAuthorityPolicy.logInstalledAuthorities();
        LOGGER.info("[Re:Monster Integration] loaded");
    }

    private void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new MobProfileReloadListener());
        event.addListener(new SpeciesBodyProfileReloadListener());
        event.addListener(new EvolutionReloadListener());
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        IntegrationCommands.register(event.getDispatcher());
        SpawnDiagnosticCommands.register(event.getDispatcher());
        EvolutionCommands.register(event.getDispatcher());
        SubordinateProgressionCommands.register(event.getDispatcher());
    }
}
