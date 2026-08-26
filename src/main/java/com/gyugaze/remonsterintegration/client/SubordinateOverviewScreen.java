package com.gyugaze.remonsterintegration.client;

import com.gyugaze.remonsterintegration.menu.SubordinateOverviewMenu;
import com.gyugaze.remonsterintegration.subordinate.SubordinateMovementMode;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;

import java.util.Optional;

/**
 * First universal subordinate screen: status + movement commands only.
 * Equipment remains native/adapter-specific and is intentionally absent from animals.
 */
public final class SubordinateOverviewScreen extends AbstractContainerScreen<SubordinateOverviewMenu> {
    private static final int PANEL_COLOR = 0xEA151515;
    private static final int BORDER_COLOR = 0xFF707070;
    private static final int TEXT_COLOR = 0xFFE6E6E6;
    private static final int MUTED_COLOR = 0xFFA6A6A6;

    private Button followButton;
    private Button wanderButton;
    private Button stayButton;

    public SubordinateOverviewScreen(
            SubordinateOverviewMenu menu,
            Inventory playerInventory,
            Component title
    ) {
        super(menu, playerInventory, title);
        this.imageWidth = 230;
        this.imageHeight = 150;
    }

    @Override
    protected void init() {
        super.init();

        int y = this.topPos + 112;
        this.followButton = addRenderableWidget(Button.builder(
                Component.literal("Follow"),
                button -> sendMovement(SubordinateOverviewMenu.BUTTON_FOLLOW)
        ).bounds(this.leftPos + 12, y, 64, 20).build());

        this.wanderButton = addRenderableWidget(Button.builder(
                Component.literal("Wander"),
                button -> sendMovement(SubordinateOverviewMenu.BUTTON_WANDER)
        ).bounds(this.leftPos + 83, y, 64, 20).build());

        this.stayButton = addRenderableWidget(Button.builder(
                Component.literal("Stay"),
                button -> sendMovement(SubordinateOverviewMenu.BUTTON_STAY)
        ).bounds(this.leftPos + 154, y, 64, 20).build());

        refreshButtonState();
    }

    private void sendMovement(int buttonId) {
        if (minecraft == null || minecraft.gameMode == null) {
            return;
        }
        minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, buttonId);
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
        followButton.active = mode.orElse(null) != SubordinateMovementMode.FOLLOW;
        wanderButton.active = mode.orElse(null) != SubordinateMovementMode.WANDER;
        stayButton.active = mode.orElse(null) != SubordinateMovementMode.STAY;
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

        graphics.drawString(this.font, name, 12, 10, TEXT_COLOR, false);
        graphics.drawString(this.font, type, 12, 24, MUTED_COLOR, false);

        if (optionalTarget.isPresent()) {
            LivingEntity target = optionalTarget.get();
            String hp = String.format("HP: %.1f / %.1f", target.getHealth(), target.getMaxHealth());
            graphics.drawString(this.font, hp, 12, 46, TEXT_COLOR, false);
        } else {
            graphics.drawString(this.font, "HP: unavailable", 12, 46, MUTED_COLOR, false);
        }

        graphics.drawString(this.font, "EP: " + menu.getEp(), 12, 60, TEXT_COLOR, false);
        graphics.drawString(this.font, "SHP: " + menu.getSpiritualHealth(), 12, 74, TEXT_COLOR, false);
        graphics.drawString(
                this.font,
                "Movement: " + menu.getMovementMode().map(Enum::name).orElse("NATIVE"),
                12,
                92,
                TEXT_COLOR,
                false
        );
    }
}
