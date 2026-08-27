package com.gyugaze.remonsterintegration.client;

import com.gyugaze.remonsterintegration.ReMonsterIntegration;
import com.gyugaze.remonsterintegration.mixin.client.AbstractContainerScreenAccessor;
import com.gyugaze.remonsterintegration.mixin.client.ScreenInvoker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.Map;
import java.util.WeakHashMap;

/** Adds the gameplay evolution entry point alongside the existing native-screen companion chrome. */
@EventBusSubscriber(modid = ReMonsterIntegration.MOD_ID, value = Dist.CLIENT)
public final class CompanionEvolutionChromeEvents {
    private static final int BUTTON_WIDTH = 58;
    private static final Map<Screen, Button> BUTTONS = new WeakHashMap<>();

    private CompanionEvolutionChromeEvents() {
    }

    @SubscribeEvent
    public static void onScreenRenderPre(ScreenEvent.Render.Pre event) {
        Screen screen = event.getScreen();
        Button existing = BUTTONS.get(screen);
        if (existing != null) {
            if (!screen.children().contains(existing)) {
                ((ScreenInvoker) screen).remonster$addRenderableWidget(existing);
            }
            return;
        }

        CompanionScreenContext.getActiveTarget(screen)
                .ifPresent(target -> attach(screen, target));
    }

    @SubscribeEvent
    public static void onScreenClosing(ScreenEvent.Closing event) {
        BUTTONS.remove(event.getScreen());
    }

    private static void attach(Screen screen, LivingEntity target) {
        Layout layout = calculateLayout(screen);
        int entityId = target.getId();
        Component name = target.getDisplayName().copy();

        Button evolve = Button.builder(
                Component.literal("Evolve"),
                button -> Minecraft.getInstance().setScreen(new SubordinateEvolutionScreen(entityId, name))
        ).bounds(layout.evolveX(), layout.y(), BUTTON_WIDTH, 20).build();

        ((ScreenInvoker) screen).remonster$addRenderableWidget(evolve);
        BUTTONS.put(screen, evolve);
    }

    private static Layout calculateLayout(Screen screen) {
        Minecraft minecraft = Minecraft.getInstance();
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();

        int nativeLeft;
        int nativeTop;
        int nativeWidth;

        if (screen instanceof AbstractContainerScreen<?> && screen instanceof AbstractContainerScreenAccessor accessor) {
            nativeLeft = accessor.remonster$getLeftPos();
            nativeTop = accessor.remonster$getTopPos();
            nativeWidth = accessor.remonster$getImageWidth();
        } else {
            nativeWidth = Math.min(240, Math.max(120, screenWidth - 16));
            nativeLeft = (screenWidth - nativeWidth) / 2;
            nativeTop = 32;
        }

        int chromeWidth = Math.min(Math.max(nativeWidth, 240), Math.max(120, screenWidth - 8));
        int center = nativeLeft + nativeWidth / 2;
        int chromeLeft = clamp(center - chromeWidth / 2, 4, Math.max(4, screenWidth - chromeWidth - 4));
        int headerY = nativeTop >= 34 ? nativeTop - 32 : 2;
        int statusX = chromeLeft + chromeWidth - 60;
        int evolveX = Math.max(chromeLeft, statusX - BUTTON_WIDTH - 4);
        return new Layout(evolveX, headerY + 5);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    private record Layout(int evolveX, int y) {
    }
}
