package com.gyugaze.remonsterintegration.client;

import com.gyugaze.remonsterintegration.tensura.TensuraSubordinateBridge;
import io.github.manasmods.tensura.entity.template.TensuraHumanoidEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.HorseInventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;

import java.util.Optional;

/**
 * Associates a native screen with the named subordinate that caused it to open.
 *
 * No native screen is replaced. We only remember the last locally interacted subordinate long
 * enough to decorate the matching screen that Minecraft/the owning mod opens normally.
 */
public final class CompanionScreenContext {
    private static final long PENDING_TIMEOUT_MS = 3_000L;

    private static int pendingTargetId = -1;
    private static long pendingSinceMs = 0L;
    private static int activeTargetId = -1;
    private static Screen activeScreen;

    private CompanionScreenContext() {
    }

    public static void capture(LivingEntity target) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }

        // Client storage is normally synced by Tensura. If it is temporarily missing, do not attach
        // controls to an entity whose ownership cannot yet be proven locally; the server validates
        // every command again regardless.
        if (TensuraSubordinateBridge.getResolvedOwnerUuid(target)
                .filter(minecraft.player.getUUID()::equals)
                .isEmpty()) {
            return;
        }

        pendingTargetId = target.getId();
        pendingSinceMs = System.currentTimeMillis();
    }

    public static Optional<LivingEntity> attachIfMatching(Screen screen) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            clearAll();
            return Optional.empty();
        }

        LivingEntity pending = resolve(pendingTargetId);
        boolean pendingFresh = pending != null
                && System.currentTimeMillis() - pendingSinceMs <= PENDING_TIMEOUT_MS;

        if (pendingFresh && matchesNativeScreen(screen, pending)) {
            activeTargetId = pending.getId();
            activeScreen = screen;
            pendingTargetId = -1;
            return Optional.of(pending);
        }

        LivingEntity active = resolve(activeTargetId);
        if (active != null && matchesNativeScreen(screen, active)) {
            activeScreen = screen;
            return Optional.of(active);
        }

        if (!pendingFresh) {
            pendingTargetId = -1;
        }
        return Optional.empty();
    }

    public static Optional<LivingEntity> getActiveTarget(Screen screen) {
        if (activeScreen != screen) {
            return Optional.empty();
        }
        return Optional.ofNullable(resolve(activeTargetId));
    }

    public static void onScreenClosing(Screen screen) {
        if (activeScreen == screen) {
            activeScreen = null;
            // Keep the entity id briefly so MCA/Tensura screen-to-screen transitions can inherit the
            // same target if their next screen also matches the same ecosystem.
        }
    }

    public static void clearAll() {
        pendingTargetId = -1;
        pendingSinceMs = 0L;
        activeTargetId = -1;
        activeScreen = null;
    }

    private static LivingEntity resolve(int entityId) {
        if (entityId < 0) {
            return null;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return null;
        }
        Entity entity = minecraft.level.getEntity(entityId);
        return entity instanceof LivingEntity living && living.isAlive() ? living : null;
    }

    private static boolean matchesNativeScreen(Screen screen, LivingEntity target) {
        if (target instanceof AbstractHorse) {
            return screen instanceof HorseInventoryScreen;
        }

        if (target instanceof TensuraHumanoidEntity) {
            return screen.getClass().getName().startsWith("io.github.manasmods.tensura.client.screen.Humanoid");
        }

        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
        if ("mca".equals(entityId.getNamespace())) {
            return screen.getClass().getName().startsWith("net.conczin.mca.client.gui.");
        }

        return false;
    }
}
