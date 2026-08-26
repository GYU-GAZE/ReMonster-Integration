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

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Universal subordinate screen: status, movement commands and exact runtime Tensura skills.
 * Equipment remains native/adapter-specific and is intentionally absent from generic animals.
 */
public final class SubordinateOverviewScreen extends AbstractContainerScreen<SubordinateOverviewMenu> {
    private static final int PANEL_COLOR = 0xEA151515;
    private static final int BORDER_COLOR = 0xFF707070;
    private static final int TEXT_COLOR = 0xFFE6E6E6;
    private static final int MUTED_COLOR = 0xFFA6A6A6;
    private static final int ACCENT_COLOR = 0xFFC7D7FF;
    private static final int VISIBLE_SKILLS = 8;

    private Button followButton;
    private Button wanderButton;
    private Button stayButton;
    private int skillScroll;

    public SubordinateOverviewScreen(
            SubordinateOverviewMenu menu,
            Inventory playerInventory,
            Component title
    ) {
        super(menu, playerInventory, title);
        this.imageWidth = 280;
        this.imageHeight = 265;
    }

    @Override
    protected void init() {
        super.init();

        int y = this.topPos + 232;
        this.followButton = addRenderableWidget(Button.builder(
                Component.literal("Follow"),
                button -> sendMovement(SubordinateOverviewMenu.BUTTON_FOLLOW)
        ).bounds(this.leftPos + 12, y, 80, 20).build());

        this.wanderButton = addRenderableWidget(Button.builder(
                Component.literal("Wander"),
                button -> sendMovement(SubordinateOverviewMenu.BUTTON_WANDER)
        ).bounds(this.leftPos + 100, y, 80, 20).build());

        this.stayButton = addRenderableWidget(Button.builder(
                Component.literal("Stay"),
                button -> sendMovement(SubordinateOverviewMenu.BUTTON_STAY)
        ).bounds(this.leftPos + 188, y, 80, 20).build());

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
        clampSkillScroll();
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

    private void clampSkillScroll() {
        int max = Math.max(0, menu.getSkillIds().size() - VISIBLE_SKILLS);
        skillScroll = Math.max(0, Math.min(skillScroll, max));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int max = Math.max(0, menu.getSkillIds().size() - VISIBLE_SKILLS);
        if (max > 0 && scrollY != 0.0D) {
            skillScroll += scrollY > 0.0D ? -1 : 1;
            skillScroll = Math.max(0, Math.min(skillScroll, max));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
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
        graphics.fill(this.leftPos + 10, this.topPos + 116, this.leftPos + this.imageWidth - 10, this.topPos + 222, 0x66101010);
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
            String hp = String.format(Locale.ROOT, "HP: %.1f / %.1f", target.getHealth(), target.getMaxHealth());
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

        List<ResourceLocation> skills = menu.getSkillIds();
        graphics.drawString(this.font, "Skills (" + skills.size() + ")", 12, 108, ACCENT_COLOR, false);

        if (skills.isEmpty()) {
            graphics.drawString(this.font, "No non-resistance skills", 18, 126, MUTED_COLOR, false);
            return;
        }

        int end = Math.min(skills.size(), skillScroll + VISIBLE_SKILLS);
        int y = 124;
        for (int i = skillScroll; i < end; i++) {
            ResourceLocation id = skills.get(i);
            graphics.drawString(this.font, "• " + humanizeSkill(id), 18, y, TEXT_COLOR, false);
            y += 12;
        }

        if (skills.size() > VISIBLE_SKILLS) {
            String page = (skillScroll + 1) + "-" + end + " / " + skills.size() + "  [mouse wheel]";
            graphics.drawString(this.font, page, 158, 210, MUTED_COLOR, false);
        }
    }

    private static String humanizeSkill(ResourceLocation id) {
        String[] words = id.getPath().split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (!result.isEmpty()) {
                result.append(' ');
            }
            result.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                result.append(word.substring(1));
            }
        }
        if (!"tensura".equals(id.getNamespace())) {
            result.append(" [").append(id.getNamespace()).append(']');
        }
        return result.toString();
    }
}
