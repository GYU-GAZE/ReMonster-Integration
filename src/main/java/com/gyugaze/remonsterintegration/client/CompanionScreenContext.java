package com.gyugaze.remonsterintegration.client;

import com.gyugaze.remonsterintegration.mca.McaSubordinateAdapter;
import com.gyugaze.remonsterintegration.menu.SubordinateMenuPolicy;
import com.gyugaze.remonsterintegration.tensura.TensuraSubordinateBridge;
import io.github.manasmods.tensura.entity.template.TensuraHumanoidEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.HorseInventoryScreen;
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
    private static final long SCREEN_TRANSITION_GRACE_MS = 750L;

    private static int pendingTargetId = -1;
    private static long pendingSinceMs = 0L;
    private static int activeTargetId = -1;
    private static Screen activeScreen;
    private static long activeCarryUntilMs = 0L;

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

        long now = System.currentTimeMillis();
        LivingEntity pending = resolve(pendingTargetId);
        boolean pendingFresh = pending != null && now - pendingSinceMs <= PENDING_TIMEOUT_MS;

        if (pendingFresh && matchesNativeScreen(screen, pending)) {
            activate(screen, pending);
            pendingTargetId = -1;
            return Optional.of(pending);
        }

        LivingEntity active = resolve(activeTargetId);
        boolean activeMayTransfer = activeScreen != null || now <= activeCarryUntilMs;
        if (active != null && activeMayTransfer && matchesNativeScreen(screen, active)) {
            activate(screen, active);
            return Optional.of(active);
        }

        if (!pendingFresh) {
            pendingTargetId = -1;
        }
        if (activeScreen == null && now > activeCarryUntilMs) {
            activeTargetId = -1;
            activeCarryUntilMs = 0L;
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
            // Some MCA/Tensura interactions replace one native screen with another. Keep the target
            // only for a very short hand-off window so an unrelated screen opened later cannot inherit it.
            activeCarryUntilMs = System.currentTimeMillis() + SCREEN_TRANSITION_GRACE_MS;
        }
    }

    public static void clearAll() {
        pendingTargetId = -1;
        pendingSinceMs = 0L;
        activeTargetId = -1;
        activeScreen = null;
        activeCarryUntilMs = 0L;
    }

    private static void activate(Screen screen, LivingEntity target) {
        activeTargetId = target.getId();
        activeScreen = screen;
        activeCarryUntilMs = 0L;
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

        if (McaSubordinateAdapter.isMcaVillager(target)) {
            return screen.getClass().getName().startsWith("net.conczin.mca.client.gui.");
        }

        // Data-driven escape hatch for future mods with custom inventories/screens. A tagged entity
        // may attach to any container screen opened immediately from its interaction.
        return SubordinateMenuPolicy.isExplicitNativeTagged(target)
                && screen instanceof AbstractContainerScreen<?>;
    }
}
