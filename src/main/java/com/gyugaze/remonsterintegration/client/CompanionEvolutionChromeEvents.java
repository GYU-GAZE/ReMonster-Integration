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

/** Adds the gameplay evolution entry point without covering controls owned by the native screen. */
@EventBusSubscriber(modid = ReMonsterIntegration.MOD_ID, value = Dist.CLIENT)
public final class CompanionEvolutionChromeEvents {
    private static final int BUTTON_WIDTH = 58;
    private static final int BUTTON_HEIGHT = 20;
    private static final int GAP = 4;
    private static final int SCREEN_MARGIN = 4;
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
        ).bounds(layout.x(), layout.y(), BUTTON_WIDTH, BUTTON_HEIGHT).build();

        ((ScreenInvoker) screen).remonster$addRenderableWidget(evolve);
        BUTTONS.put(screen, evolve);
    }

    /**
     * Prefer a side gutter next to the native GUI. MCA screens can put their own status widgets in the
     * header area, so sharing the old Status-button row made Evolve visually disappear into MCA UI.
     * Only fall back above/below the native panel when neither side has enough room.
     */
    private static Layout calculateLayout(Screen screen) {
        Minecraft minecraft = Minecraft.getInstance();
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();

        int nativeLeft;
        int nativeTop;
        int nativeWidth;
        int nativeHeight;

        if (screen instanceof AbstractContainerScreen<?> && screen instanceof AbstractContainerScreenAccessor accessor) {
            nativeLeft = accessor.remonster$getLeftPos();
            nativeTop = accessor.remonster$getTopPos();
            nativeWidth = accessor.remonster$getImageWidth();
            nativeHeight = accessor.remonster$getImageHeight();
        } else {
            nativeWidth = Math.min(240, Math.max(120, screenWidth - 16));
            nativeHeight = Math.max(80, screenHeight - 64);
            nativeLeft = (screenWidth - nativeWidth) / 2;
            nativeTop = 32;
        }

        int yBeside = clamp(
                nativeTop + GAP,
                SCREEN_MARGIN,
                Math.max(SCREEN_MARGIN, screenHeight - BUTTON_HEIGHT - SCREEN_MARGIN)
        );

        int rightX = nativeLeft + nativeWidth + GAP;
        if (rightX + BUTTON_WIDTH <= screenWidth - SCREEN_MARGIN) {
            return new Layout(rightX, yBeside);
        }

        int leftX = nativeLeft - BUTTON_WIDTH - GAP;
        if (leftX >= SCREEN_MARGIN) {
            return new Layout(leftX, yBeside);
        }

        int x = clamp(
                nativeLeft + nativeWidth - BUTTON_WIDTH,
                SCREEN_MARGIN,
                Math.max(SCREEN_MARGIN, screenWidth - BUTTON_WIDTH - SCREEN_MARGIN)
        );

        int aboveY = nativeTop - BUTTON_HEIGHT - GAP;
        if (aboveY >= SCREEN_MARGIN) {
            return new Layout(x, aboveY);
        }

        int belowY = clamp(
                nativeTop + nativeHeight + GAP,
                SCREEN_MARGIN,
                Math.max(SCREEN_MARGIN, screenHeight - BUTTON_HEIGHT - SCREEN_MARGIN)
        );
        return new Layout(x, belowY);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    private record Layout(int x, int y) {
    }
}
