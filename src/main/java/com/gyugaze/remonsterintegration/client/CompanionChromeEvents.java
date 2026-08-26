package com.gyugaze.remonsterintegration.client;

import com.gyugaze.remonsterintegration.ReMonsterIntegration;
import com.gyugaze.remonsterintegration.menu.SubordinateMenuPolicy;
import com.gyugaze.remonsterintegration.mixin.client.AbstractContainerScreenAccessor;
import com.gyugaze.remonsterintegration.mixin.client.ScreenInvoker;
import com.gyugaze.remonsterintegration.network.SubordinateCommandPayload;
import com.gyugaze.remonsterintegration.network.SubordinateSnapshotRequestPayload;
import com.gyugaze.remonsterintegration.network.SubordinateTargetingCommandPayload;
import com.gyugaze.remonsterintegration.subordinate.SubordinateMovementMode;
import com.gyugaze.remonsterintegration.subordinate.SubordinateTargetingMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.function.Consumer;

/** Adds Re:Monster quick controls around native subordinate screens without replacing contents. */
@EventBusSubscriber(modid = ReMonsterIntegration.MOD_ID, value = Dist.CLIENT)
public final class CompanionChromeEvents {
    private static final long SNAPSHOT_INTERVAL_MS = 500L;
    private static final int INFO_BG = 0xD9141414;
    private static final int INFO_BORDER = 0xCC707070;
    private static final int TEXT = 0xFFE6E6E6;
    private static final int MUTED = 0xFFA6A6A6;
    private static final int ACCENT = 0xFFC7D7FF;
    private static final int HOTBAR_RESERVE = 26;

    private static final Map<Screen, ChromeState> CHROME = new WeakHashMap<>();

    private CompanionChromeEvents() {
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!event.getEntity().level().isClientSide()) {
            return;
        }
        if (!(event.getTarget() instanceof LivingEntity target)) {
            return;
        }
        if (!SubordinateMenuPolicy.hasKnownNativeMenu(target)) {
            return;
        }
        CompanionScreenContext.capture(target);
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();
        CompanionScreenContext.attachIfMatching(screen)
                .ifPresent(target -> attachChrome(screen, target, button -> event.addListener(button)));
    }

    /**
     * Server-authorized context can arrive after a native client screen has already completed Init.Post
     * (MCA opens its GUI optimistically on the client). Attach to that already-open screen immediately
     * instead of requiring a failed shift-click followed by a second normal click.
     */
    public static void attachCurrentScreenIfMatching() {
        Minecraft minecraft = Minecraft.getInstance();
        Screen screen = minecraft.screen;
        if (screen == null || CHROME.containsKey(screen)) {
            return;
        }

        CompanionScreenContext.attachIfMatching(screen).ifPresent(target -> attachChrome(
                screen,
                target,
                button -> ((ScreenInvoker) screen).remonster$addRenderableWidget(button)
        ));
    }

    private static void attachChrome(Screen screen, LivingEntity target, Consumer<Button> listener) {
        if (CHROME.containsKey(screen)) {
            return;
        }

        Layout layout = calculateLayout(screen);
        int entityId = target.getId();

        Button status = Button.builder(
                Component.literal("Status"),
                button -> resolveTarget(entityId).ifPresent(BetterSubordinatesStatusBridge::open)
        ).bounds(layout.statusX(), layout.headerY() + 5, layout.statusWidth(), 20).build();

        int movementFirst = layout.movementWidth();
        int movementSecond = layout.movementWidth();
        int movementThird = layout.chromeWidth() - movementFirst - movementSecond - 8;

        Button follow = movementButton(
                "Follow", layout.chromeLeft(), layout.movementY(), movementFirst, entityId, SubordinateMovementMode.FOLLOW
        );
        Button wander = movementButton(
                "Wander", layout.chromeLeft() + movementFirst + 4, layout.movementY(), movementSecond,
                entityId, SubordinateMovementMode.WANDER
        );
        Button stay = movementButton(
                "Stay", layout.chromeLeft() + movementFirst + movementSecond + 8, layout.movementY(), movementThird,
                entityId, SubordinateMovementMode.STAY
        );

        int targetingWidth = Math.max(1, (layout.chromeWidth() - 12) / 4);
        int targetingLast = layout.chromeWidth() - targetingWidth * 3 - 12;
        Button neutral = targetingButton(
                "Neutral", layout.chromeLeft(), layout.targetingY(), targetingWidth, entityId, SubordinateTargetingMode.NEUTRAL
        );
        Button passive = targetingButton(
                "Passive", layout.chromeLeft() + targetingWidth + 4, layout.targetingY(), targetingWidth,
                entityId, SubordinateTargetingMode.PASSIVE
        );
        Button aggressive = targetingButton(
                "Aggressive", layout.chromeLeft() + (targetingWidth + 4) * 2, layout.targetingY(), targetingWidth,
                entityId, SubordinateTargetingMode.AGGRESSIVE
        );
        Button protect = targetingButton(
                "Protect", layout.chromeLeft() + (targetingWidth + 4) * 3, layout.targetingY(), targetingLast,
                entityId, SubordinateTargetingMode.PROTECT
        );

        listener.accept(status);
        listener.accept(follow);
        listener.accept(wander);
        listener.accept(stay);
        listener.accept(neutral);
        listener.accept(passive);
        listener.accept(aggressive);
        listener.accept(protect);

        ChromeState state = new ChromeState(
                entityId, layout, follow, wander, stay, neutral, passive, aggressive, protect, status
        );
        CHROME.put(screen, state);
        requestSnapshot(state, true);
    }

    @SubscribeEvent
    public static void onScreenRenderPre(ScreenEvent.Render.Pre event) {
        ChromeState state = CHROME.get(event.getScreen());
        if (state == null) {
            return;
        }

        // Some native screens rebuild themselves after Init.Post. MCA's AbstractDynamicScreen,
        // for example, calls clearWidgets() from setLayout() when interaction data arrives.
        // Reattach only widgets that the native screen discarded instead of replacing its GUI.
        restoreButtonIfMissing(event.getScreen(), state.status());
        restoreButtonIfMissing(event.getScreen(), state.follow());
        restoreButtonIfMissing(event.getScreen(), state.wander());
        restoreButtonIfMissing(event.getScreen(), state.stay());
        restoreButtonIfMissing(event.getScreen(), state.neutral());
        restoreButtonIfMissing(event.getScreen(), state.passive());
        restoreButtonIfMissing(event.getScreen(), state.aggressive());
        restoreButtonIfMissing(event.getScreen(), state.protect());
    }

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        ChromeState state = CHROME.get(event.getScreen());
        if (state == null) {
            return;
        }

        Optional<LivingEntity> optionalTarget = resolveTarget(state.entityId());
        if (optionalTarget.isEmpty()) {
            disable(state);
            return;
        }

        requestSnapshot(state, false);
        refreshButtons(state);
        renderInfo(event.getGuiGraphics(), optionalTarget.get(), state);
    }

    @SubscribeEvent
    public static void onScreenClosing(ScreenEvent.Closing event) {
        CHROME.remove(event.getScreen());
        CompanionScreenContext.onScreenClosing(event.getScreen());
    }

    private static void restoreButtonIfMissing(Screen screen, Button button) {
        if (button == null || screen.children().contains(button)) {
            return;
        }
        ((ScreenInvoker) screen).remonster$addRenderableWidget(button);
    }

    private static Button movementButton(
            String label,
            int x,
            int y,
            int width,
            int entityId,
            SubordinateMovementMode mode
    ) {
        return Button.builder(
                Component.literal(label),
                button -> {
                    PacketDistributor.sendToServer(new SubordinateCommandPayload(entityId, mode.ordinal()));
                    ClientSubordinateSnapshotCache.clear(entityId);
                }
        ).bounds(x, y, Math.max(1, width), 20).build();
    }

    private static Button targetingButton(
            String label,
            int x,
            int y,
            int width,
            int entityId,
            SubordinateTargetingMode mode
    ) {
        return Button.builder(
                Component.literal(label),
                button -> {
                    PacketDistributor.sendToServer(new SubordinateTargetingCommandPayload(entityId, mode.ordinal()));
                    ClientSubordinateSnapshotCache.clear(entityId);
                }
        ).bounds(x, y, Math.max(1, width), 20).build();
    }

    private static void refreshButtons(ChromeState state) {
        Optional<ClientSubordinateSnapshotCache.Snapshot> snapshot = ClientSubordinateSnapshotCache.get(state.entityId());
        SubordinateMovementMode movement = snapshot.map(ClientSubordinateSnapshotCache.Snapshot::movement).orElse(null);
        boolean controllable = snapshot
                .map(value -> value.movementUnlocked() && movement != null)
                .orElse(false);

        state.follow().active = controllable && movement != SubordinateMovementMode.FOLLOW;
        state.wander().active = controllable && movement != SubordinateMovementMode.WANDER;
        state.stay().active = controllable && movement != SubordinateMovementMode.STAY;

        boolean targetingUnlocked = snapshot.map(ClientSubordinateSnapshotCache.Snapshot::targetingUnlocked).orElse(false);
        SubordinateTargetingMode targeting = snapshot.map(ClientSubordinateSnapshotCache.Snapshot::targeting)
                .orElse(SubordinateTargetingMode.NEUTRAL);
        state.neutral().active = targetingUnlocked && targeting != SubordinateTargetingMode.NEUTRAL;
        state.passive().active = targetingUnlocked && targeting != SubordinateTargetingMode.PASSIVE;
        state.aggressive().active = targetingUnlocked && targeting != SubordinateTargetingMode.AGGRESSIVE;
        state.protect().active = targetingUnlocked && targeting != SubordinateTargetingMode.PROTECT;

        if (state.status() != null) {
            state.status().active = BetterSubordinatesStatusBridge.isAvailable();
        }
    }

    private static void disable(ChromeState state) {
        state.follow().active = false;
        state.wander().active = false;
        state.stay().active = false;
        state.neutral().active = false;
        state.passive().active = false;
        state.aggressive().active = false;
        state.protect().active = false;
        if (state.status() != null) {
            state.status().active = false;
        }
    }

    private static void requestSnapshot(ChromeState state, boolean force) {
        long now = System.currentTimeMillis();
        if (!force && now - state.lastSnapshotRequestMs() < SNAPSHOT_INTERVAL_MS) {
            return;
        }
        if (Minecraft.getInstance().player == null) {
            return;
        }
        PacketDistributor.sendToServer(new SubordinateSnapshotRequestPayload(state.entityId()));
        state.lastSnapshotRequestMs(now);
    }

    private static void renderInfo(GuiGraphics graphics, LivingEntity target, ChromeState state) {
        Layout layout = state.layout();
        int x = layout.chromeLeft();
        int y = layout.headerY();
        int right = state.status() == null
                ? layout.chromeLeft() + layout.chromeWidth()
                : layout.statusX() - 4;

        graphics.fill(x, y, right, y + 30, INFO_BORDER);
        graphics.fill(x + 1, y + 1, right - 1, y + 29, INFO_BG);

        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        int availableWidth = Math.max(10, right - x - 8);
        String name = font.plainSubstrByWidth(target.getDisplayName().getString(), availableWidth);
        graphics.drawString(font, name, x + 4, y + 4, TEXT, false);

        Optional<ClientSubordinateSnapshotCache.Snapshot> optionalSnapshot = ClientSubordinateSnapshotCache.get(state.entityId());
        if (optionalSnapshot.isEmpty()) {
            graphics.drawString(font, "syncing...", x + 4, y + 17, MUTED, false);
            return;
        }

        ClientSubordinateSnapshotCache.Snapshot snapshot = optionalSnapshot.get();
        String movement = !snapshot.movementUnlocked()
                ? "LOCKED"
                : snapshot.movement() == null ? "NATIVE" : snapshot.movement().name();
        String targeting = snapshot.targetingUnlocked() ? snapshot.targeting().name() : "LOCKED";
        String details = String.format(
                Locale.ROOT,
                "HP %.0f/%.0f  EP %s  SHP %s  M:%s  T:%s",
                snapshot.health(),
                snapshot.maxHealth(),
                compactNumber(snapshot.ep()),
                compactNumber(snapshot.spiritualHealth()),
                movement,
                targeting
        );
        graphics.drawString(
                font,
                font.plainSubstrByWidth(details, availableWidth),
                x + 4,
                y + 17,
                snapshot.movementUnlocked() && snapshot.movement() != null ? ACCENT : MUTED,
                false
        );
    }

    private static String compactNumber(double value) {
        if (!Double.isFinite(value)) {
            return "0";
        }
        double absolute = Math.abs(value);
        if (absolute >= 1_000_000_000D) {
            return String.format(Locale.ROOT, "%.1fB", value / 1_000_000_000D);
        }
        if (absolute >= 1_000_000D) {
            return String.format(Locale.ROOT, "%.1fM", value / 1_000_000D);
        }
        if (absolute >= 10_000D) {
            return String.format(Locale.ROOT, "%.1fK", value / 1_000D);
        }
        return Long.toString(Math.round(value));
    }

    private static Optional<LivingEntity> resolveTarget(int entityId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return Optional.empty();
        }
        Entity entity = minecraft.level.getEntity(entityId);
        return entity instanceof LivingEntity living && living.isAlive()
                ? Optional.of(living)
                : Optional.empty();
    }

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

        int chromeWidth = Math.min(Math.max(nativeWidth, 240), Math.max(120, screenWidth - 8));
        int center = nativeLeft + nativeWidth / 2;
        int chromeLeft = clamp(center - chromeWidth / 2, 4, Math.max(4, screenWidth - chromeWidth - 4));

        int headerY = nativeTop >= 34 ? nativeTop - 32 : 2;
        int movementY = nativeTop + nativeHeight + 4;
        int targetingY = movementY + 24;
        int controlsBottom = Math.max(22, screenHeight - HOTBAR_RESERVE);
        if (targetingY + 20 > controlsBottom) {
            targetingY = Math.max(2, controlsBottom - 20);
            movementY = Math.max(2, targetingY - 24);
        }

        int statusWidth = 60;
        int statusX = chromeLeft + chromeWidth - statusWidth;
        int movementWidth = Math.max(1, (chromeWidth - 8) / 3);
        return new Layout(
                chromeLeft, chromeWidth, headerY, movementY, targetingY, statusX, statusWidth, movementWidth
        );
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    private record Layout(
            int chromeLeft,
            int chromeWidth,
            int headerY,
            int movementY,
            int targetingY,
            int statusX,
            int statusWidth,
            int movementWidth
    ) {
    }

    private static final class ChromeState {
        private final int entityId;
        private final Layout layout;
        private final Button follow;
        private final Button wander;
        private final Button stay;
        private final Button neutral;
        private final Button passive;
        private final Button aggressive;
        private final Button protect;
        private final Button status;
        private long lastSnapshotRequestMs;

        private ChromeState(
                int entityId,
                Layout layout,
                Button follow,
                Button wander,
                Button stay,
                Button neutral,
                Button passive,
                Button aggressive,
                Button protect,
                Button status
        ) {
            this.entityId = entityId;
            this.layout = layout;
            this.follow = follow;
            this.wander = wander;
            this.stay = stay;
            this.neutral = neutral;
            this.passive = passive;
            this.aggressive = aggressive;
            this.protect = protect;
            this.status = status;
        }

        int entityId() { return entityId; }
        Layout layout() { return layout; }
        Button follow() { return follow; }
        Button wander() { return wander; }
        Button stay() { return stay; }
        Button neutral() { return neutral; }
        Button passive() { return passive; }
        Button aggressive() { return aggressive; }
        Button protect() { return protect; }
        Button status() { return status; }
        long lastSnapshotRequestMs() { return lastSnapshotRequestMs; }
        void lastSnapshotRequestMs(long value) { lastSnapshotRequestMs = value; }
    }
}
