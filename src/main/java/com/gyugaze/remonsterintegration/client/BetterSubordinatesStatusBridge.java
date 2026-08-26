package com.gyugaze.remonsterintegration.client;

import com.gyugaze.remonsterintegration.ReMonsterIntegration;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;

import java.lang.reflect.Method;

/** Client-only bridge to Better Subordinates' generic LivingEntity status screen. */
public final class BetterSubordinatesStatusBridge {
    private static final String SCREEN_HELPER = "com.trbeyond.tensura_better_subs.client.screen.ScreenHelperSubordinates";
    private static final int SUBORDINATE_MAIN_SCREEN = 4;

    private static boolean lookupAttempted;
    private static boolean failureLogged;
    private static Method openScreen;

    private BetterSubordinatesStatusBridge() {
    }

    public static boolean isAvailable() {
        return resolveOpenScreen() != null;
    }

    public static boolean open(LivingEntity target) {
        if (target == null || !target.isAlive()) {
            return false;
        }

        Method method = resolveOpenScreen();
        if (method == null) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return false;
        }

        try {
            // This is the exact transition Better Subordinates uses from Tensura's humanoid
            // inventory: close the current server container first, then swap to screen id 4.
            minecraft.player.closeContainer();
            method.invoke(null, SUBORDINATE_MAIN_SCREEN, target);
            return true;
        } catch (ReflectiveOperationException | LinkageError exception) {
            logFailure(exception);
            return false;
        }
    }

    private static synchronized Method resolveOpenScreen() {
        if (lookupAttempted) {
            return openScreen;
        }
        lookupAttempted = true;

        try {
            Class<?> helper = Class.forName(SCREEN_HELPER, false, BetterSubordinatesStatusBridge.class.getClassLoader());
            Method method = helper.getDeclaredMethod("openScreen", int.class, LivingEntity.class);
            method.setAccessible(true);
            openScreen = method;
        } catch (ReflectiveOperationException | LinkageError exception) {
            logFailure(exception);
        }
        return openScreen;
    }

    private static void logFailure(Throwable throwable) {
        if (failureLogged) {
            return;
        }
        failureLogged = true;
        ReMonsterIntegration.LOGGER.warn(
                "Better Subordinates status screen bridge is unavailable; subordinate Status buttons will be disabled",
                throwable
        );
    }
}
