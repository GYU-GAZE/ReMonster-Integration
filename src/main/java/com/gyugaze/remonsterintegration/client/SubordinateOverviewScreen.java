package com.gyugaze.remonsterintegration.client;

import com.gyugaze.remonsterintegration.menu.SubordinateOverviewMenu;
import com.gyugaze.remonsterintegration.subordinate.SubordinateMovementMode;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;

import java.util.Locale;
import java.util.Optional;

/**
 * Lightweight Re:Monster screen used only when a subordinate has no native screen of its own.
 * Full skill/status management is delegated to Better Subordinates through the Status button.
 */
public final class SubordinateOverviewScreen extends AbstractContainerScreen<SubordinateOverviewMenu> {
    private static final int PANEL_COLOR = 0xEA151515;
    private static final int BORDER_COLOR = 0xFF707070;
    private static final int TEXT_COLOR = 0xFFE6E6E6;
    private static final int MUTED_COLOR = 0xFFA6A6A6;
    private static final int ACCENT_COLOR = 0xFFC7D7FF;

    private Button followButton;
    private Button wanderButton;
    private Button stayButton;
    private Button statusButton;

    public SubordinateOverviewScreen(
            SubordinateOverviewMenu menu,
            Inventory playerInventory,
            Component title
    ) {
        super(menu, playerInventory, title);
        this.imageWidth = 244;
        this.imageHeight = 142;
    }

    @Override
    protected void init() {
        super.init();

        this.statusButton = addRenderableWidget(Button.builder(
                Component.literal("Status"),
                button -> openStatus()
        ).bounds(this.leftPos + 172, this.topPos + 10, 60, 20).build());

        int y = this.topPos + 108;
        this.followButton = addRenderableWidget(Button.builder(
                Component.literal("Follow"),
                button -> sendMovement(SubordinateOverviewMenu.BUTTON_FOLLOW)
        ).bounds(this.leftPos + 10, y, 70, 20).build());

        this.wanderButton = addRenderableWidget(Button.builder(
                Component.literal("Wander"),
                button -> sendMovement(SubordinateOverviewMenu.BUTTON_WANDER)
        ).bounds(this.leftPos + 87, y, 70, 20).build());

        this.stayButton = addRenderableWidget(Button.builder(
                Component.literal("Stay"),
                button -> sendMovement(SubordinateOverviewMenu.BUTTON_STAY)
        ).bounds(this.leftPos + 164, y, 70, 20).build());

        refreshButtonState();
    }

    private void sendMovement(int buttonId) {
        if (minecraft == null || minecraft.gameMode == null) {
            return;
        }
        minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, buttonId);
    }

    private void openStatus() {
        menu.getTarget().ifPresent(BetterSubordinatesStatusBridge::open);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        refreshButtonState();
    }

    private void refreshButtonState() {
        if (followButton == null) {
            return;
        }

        Optional<SubordinateMovementMode> mode = menu.getMovementMode();
        boolean controllable = mode.isPresent();
        followButton.active = controllable && mode.get() != SubordinateMovementMode.FOLLOW;
        wanderButton.active = controllable && mode.get() != SubordinateMovementMode.WANDER;
        stayButton.active = controllable && mode.get() != SubordinateMovementMode.STAY;

        if (statusButton != null) {
            statusButton.active = menu.getTarget().isPresent() && BetterSubordinatesStatusBridge.isAvailable();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, BORDER_COLOR);
        graphics.fill(this.leftPos + 2, this.topPos + 2, this.leftPos + this.imageWidth - 2, this.topPos + this.imageHeight - 2, PANEL_COLOR);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        Optional<LivingEntity> optionalTarget = menu.getTarget();
        String name = optionalTarget.map(entity -> entity.getDisplayName().getString()).orElse("Unknown subordinate");
        String type = optionalTarget
                .map(entity -> BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString())
                .orElse("unknown");

        graphics.drawString(this.font, this.font.plainSubstrByWidth(name, 150), 10, 12, TEXT_COLOR, false);
        graphics.drawString(this.font, this.font.plainSubstrByWidth(type, 210), 10, 28, MUTED_COLOR, false);

        if (optionalTarget.isPresent()) {
            LivingEntity target = optionalTarget.get();
            String hp = String.format(Locale.ROOT, "HP: %.1f / %.1f", target.getHealth(), target.getMaxHealth());
            graphics.drawString(this.font, hp, 10, 52, TEXT_COLOR, false);
        } else {
            graphics.drawString(this.font, "HP: unavailable", 10, 52, MUTED_COLOR, false);
        }

        graphics.drawString(this.font, "EP: " + menu.getEp(), 10, 66, TEXT_COLOR, false);
        graphics.drawString(this.font, "SHP: " + menu.getSpiritualHealth(), 94, 66, TEXT_COLOR, false);

        String movement = menu.getMovementMode().map(Enum::name).orElse("NATIVE / UNSUPPORTED");
        graphics.drawString(this.font, "Movement: " + movement, 10, 84, ACCENT_COLOR, false);
    }
}
