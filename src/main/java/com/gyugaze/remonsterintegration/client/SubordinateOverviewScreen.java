package com.gyugaze.remonsterintegration.client;

import com.gyugaze.remonsterintegration.menu.SubordinateOverviewMenu;
import com.gyugaze.remonsterintegration.subordinate.SubordinateMovementMode;
import com.gyugaze.remonsterintegration.subordinate.SubordinateTargetingMode;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
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
    private Button neutralButton;
    private Button passiveButton;
    private Button aggressiveButton;
    private Button protectButton;
    private Button statusButton;
    private Button evolutionButton;

    public SubordinateOverviewScreen(
            SubordinateOverviewMenu menu,
            Inventory playerInventory,
            Component title
    ) {
        super(menu, playerInventory, title);
        this.imageWidth = 260;
        this.imageHeight = 170;
    }

    @Override
    protected void init() {
        super.init();

        this.evolutionButton = addRenderableWidget(Button.builder(
                Component.literal("Evolution"),
                button -> openEvolution()
        ).bounds(this.leftPos + this.imageWidth - 142, this.topPos + 10, 66, 20).build());

        this.statusButton = addRenderableWidget(Button.builder(
                Component.literal("Status"),
                button -> openStatus()
        ).bounds(this.leftPos + this.imageWidth - 72, this.topPos + 10, 60, 20).build());

        int movementY = this.topPos + 108;
        int movementWidth = (this.imageWidth - 28) / 3;
        this.followButton = addRenderableWidget(Button.builder(
                Component.literal("Follow"),
                button -> sendCommand(SubordinateOverviewMenu.BUTTON_FOLLOW)
        ).bounds(this.leftPos + 10, movementY, movementWidth, 20).build());

        this.wanderButton = addRenderableWidget(Button.builder(
                Component.literal("Wander"),
                button -> sendCommand(SubordinateOverviewMenu.BUTTON_WANDER)
        ).bounds(this.leftPos + 14 + movementWidth, movementY, movementWidth, 20).build());

        this.stayButton = addRenderableWidget(Button.builder(
                Component.literal("Stay"),
                button -> sendCommand(SubordinateOverviewMenu.BUTTON_STAY)
        ).bounds(this.leftPos + 18 + movementWidth * 2, movementY, movementWidth, 20).build());

        int targetingY = this.topPos + 136;
        int targetingWidth = (this.imageWidth - 32) / 4;
        this.neutralButton = targetingButton("Neutral", SubordinateOverviewMenu.BUTTON_NEUTRAL, 10, targetingY, targetingWidth);
        this.passiveButton = targetingButton("Passive", SubordinateOverviewMenu.BUTTON_PASSIVE, 14 + targetingWidth, targetingY, targetingWidth);
        this.aggressiveButton = targetingButton("Aggressive", SubordinateOverviewMenu.BUTTON_AGGRESSIVE, 18 + targetingWidth * 2, targetingY, targetingWidth);
        this.protectButton = targetingButton("Protect", SubordinateOverviewMenu.BUTTON_PROTECT, 22 + targetingWidth * 3, targetingY, targetingWidth);

        refreshButtonState();
    }

    private Button targetingButton(String label, int buttonId, int relativeX, int y, int width) {
        return addRenderableWidget(Button.builder(
                Component.literal(label),
                button -> sendCommand(buttonId)
        ).bounds(this.leftPos + relativeX, y, width, 20).build());
    }

    private void sendCommand(int buttonId) {
        if (minecraft == null || minecraft.gameMode == null) {
            return;
        }
        minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, buttonId);
    }

    private void openStatus() {
        menu.getTarget().ifPresent(BetterSubordinatesStatusBridge::open);
    }

    private void openEvolution() {
        if (minecraft == null) {
            return;
        }
        menu.getTarget().ifPresent(target -> minecraft.setScreen(
                new SubordinateEvolutionScreen(target.getId(), target.getDisplayName())
        ));
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

        Optional<SubordinateMovementMode> movement = menu.getMovementMode();
        boolean controllable = menu.isMovementUnlocked() && movement.isPresent();
        followButton.active = controllable && movement.get() != SubordinateMovementMode.FOLLOW;
        wanderButton.active = controllable && movement.get() != SubordinateMovementMode.WANDER;
        stayButton.active = controllable && movement.get() != SubordinateMovementMode.STAY;

        boolean targetingUnlocked = menu.isTargetingUnlocked();
        SubordinateTargetingMode targeting = menu.getTargetingMode();
        neutralButton.active = targetingUnlocked && targeting != SubordinateTargetingMode.NEUTRAL;
        passiveButton.active = targetingUnlocked && targeting != SubordinateTargetingMode.PASSIVE;
        aggressiveButton.active = targetingUnlocked && targeting != SubordinateTargetingMode.AGGRESSIVE;
        protectButton.active = targetingUnlocked && targeting != SubordinateTargetingMode.PROTECT;

        boolean targetPresent = menu.getTarget().isPresent();
        if (statusButton != null) {
            statusButton.active = targetPresent && BetterSubordinatesStatusBridge.isAvailable();
        }
        if (evolutionButton != null) {
            evolutionButton.active = targetPresent;
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

        graphics.drawString(this.font, this.font.plainSubstrByWidth(name, 106), 10, 12, TEXT_COLOR, false);
        graphics.drawString(this.font, this.font.plainSubstrByWidth(type, 226), 10, 28, MUTED_COLOR, false);

        if (optionalTarget.isPresent()) {
            LivingEntity target = optionalTarget.get();
            String hp = String.format(Locale.ROOT, "HP: %.1f / %.1f", target.getHealth(), target.getMaxHealth());
            graphics.drawString(this.font, hp, 10, 50, TEXT_COLOR, false);
        } else {
            graphics.drawString(this.font, "HP: unavailable", 10, 50, MUTED_COLOR, false);
        }

        graphics.drawString(this.font, "EP: " + menu.getEp(), 10, 64, TEXT_COLOR, false);
        graphics.drawString(this.font, "SHP: " + menu.getSpiritualHealth(), 104, 64, TEXT_COLOR, false);

        String movement = menu.isMovementUnlocked()
                ? menu.getMovementMode().map(Enum::name).orElse("NATIVE / UNSUPPORTED")
                : "LOCKED - learn Telepathy (2,000 EP milestone)";
        graphics.drawString(
                this.font,
                this.font.plainSubstrByWidth("Movement: " + movement, 238),
                10,
                80,
                menu.isMovementUnlocked() ? ACCENT_COLOR : MUTED_COLOR,
                false
        );

        String targeting = menu.isTargetingUnlocked() ? menu.getTargetingMode().name() : "LOCKED - master Thought Communication";
        graphics.drawString(
                this.font,
                this.font.plainSubstrByWidth("Targeting: " + targeting, 238),
                10,
                94,
                menu.isTargetingUnlocked() ? ACCENT_COLOR : MUTED_COLOR,
                false
        );
    }
}
