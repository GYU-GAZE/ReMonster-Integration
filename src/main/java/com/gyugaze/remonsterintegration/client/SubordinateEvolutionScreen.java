package com.gyugaze.remonsterintegration.client;

import com.gyugaze.remonsterintegration.network.SubordinateEvolutionCommandPayload;
import com.gyugaze.remonsterintegration.network.SubordinateEvolutionOptionsPayload;
import com.gyugaze.remonsterintegration.network.SubordinateEvolutionOptionsRequestPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Minimal branch chooser for server-authoritative subordinate evolution. */
public final class SubordinateEvolutionScreen extends Screen {
    private static final int PANEL_WIDTH = 300;
    private static final int PANEL_COLOR = 0xEA151515;
    private static final int BORDER_COLOR = 0xFF707070;
    private static final int TEXT_COLOR = 0xFFE6E6E6;
    private static final int MUTED_COLOR = 0xFFA6A6A6;
    private static final int READY_COLOR = 0xFFC7D7FF;
    private static final int MAX_VISIBLE_OPTIONS = 8;

    private final int entityId;
    private final Component subordinateName;
    private long builtSnapshotMs = -1L;
    private int visibleOptionCount;

    public SubordinateEvolutionScreen(int entityId, Component subordinateName) {
        super(Component.literal("Subordinate Evolution"));
        this.entityId = entityId;
        this.subordinateName = subordinateName.copy();
    }

    @Override
    protected void init() {
        ClientSubordinateEvolutionCache.clear(entityId);
        PacketDistributor.sendToServer(new SubordinateEvolutionOptionsRequestPayload(entityId));

        addRenderableWidget(Button.builder(
                Component.literal("Close"),
                button -> onClose()
        ).bounds(this.width / 2 - 40, Math.max(10, this.height - 30), 80, 20).build());
    }

    @Override
    public void tick() {
        super.tick();
        Optional<ClientSubordinateEvolutionCache.Snapshot> optional = ClientSubordinateEvolutionCache.get(entityId);
        if (optional.isEmpty()) {
            return;
        }

        ClientSubordinateEvolutionCache.Snapshot snapshot = optional.get();
        if (builtSnapshotMs >= 0L || snapshot.receivedAtMs() == builtSnapshotMs) {
            return;
        }
        builtSnapshotMs = snapshot.receivedAtMs();
        buildEvolutionButtons(snapshot);
    }

    private void buildEvolutionButtons(ClientSubordinateEvolutionCache.Snapshot snapshot) {
        List<SubordinateEvolutionOptionsPayload.Option> options = snapshot.options();
        visibleOptionCount = Math.min(options.size(), MAX_VISIBLE_OPTIONS);
        int buttonWidth = Math.min(PANEL_WIDTH - 32, Math.max(160, this.width - 48));
        int firstY = 86;

        for (int i = 0; i < visibleOptionCount; i++) {
            SubordinateEvolutionOptionsPayload.Option option = options.get(i);
            String targetName = displaySpecies(option.targetSpecies());
            String label = option.ready()
                    ? targetName + "  [READY]"
                    : targetName + "  [" + compactNumber(snapshot.baseMaxEp())
                    + "/" + compactNumber(option.minimumEp()) + " EP]";

            Button button = addRenderableWidget(Button.builder(
                    Component.literal(label),
                    pressed -> choose(option.targetSpecies())
            ).bounds(this.width / 2 - buttonWidth / 2, firstY + i * 24, buttonWidth, 20).build());
            button.active = option.ready();
        }
    }

    private void choose(ResourceLocation targetSpecies) {
        PacketDistributor.sendToServer(new SubordinateEvolutionCommandPayload(entityId, targetSpecies));
        ClientSubordinateEvolutionCache.clear(entityId);
        onClose();
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(null);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);

        int panelHeight = Math.min(this.height - 20, 116 + Math.max(1, visibleOptionCount) * 24);
        int left = this.width / 2 - PANEL_WIDTH / 2;
        int top = Math.max(8, (this.height - panelHeight) / 2 - 10);
        int right = left + PANEL_WIDTH;
        int bottom = Math.min(this.height - 8, top + panelHeight);
        graphics.fill(left, top, right, bottom, BORDER_COLOR);
        graphics.fill(left + 2, top + 2, right - 2, bottom - 2, PANEL_COLOR);

        graphics.drawCenteredString(font, "Evolution", this.width / 2, top + 10, TEXT_COLOR);
        graphics.drawCenteredString(font, subordinateName, this.width / 2, top + 25, TEXT_COLOR);

        Optional<ClientSubordinateEvolutionCache.Snapshot> optional = ClientSubordinateEvolutionCache.get(entityId);
        if (optional.isEmpty()) {
            graphics.drawCenteredString(font, "Syncing evolution data...", this.width / 2, top + 48, MUTED_COLOR);
        } else {
            ClientSubordinateEvolutionCache.Snapshot snapshot = optional.get();
            String species = snapshot.currentSpecies() == null
                    ? "Species: unset"
                    : "Species: " + displaySpecies(snapshot.currentSpecies());
            graphics.drawCenteredString(font, species, this.width / 2, top + 44, TEXT_COLOR);
            graphics.drawCenteredString(
                    font,
                    "Base MAX EP: " + compactNumber(snapshot.baseMaxEp()),
                    this.width / 2,
                    top + 58,
                    READY_COLOR
            );

            if (snapshot.currentSpecies() == null) {
                graphics.drawCenteredString(
                        font,
                        "No semantic species is assigned to this subordinate yet",
                        this.width / 2,
                        top + 72,
                        MUTED_COLOR
                );
            } else if (snapshot.options().isEmpty()) {
                graphics.drawCenteredString(font, "No loaded evolution branches", this.width / 2, top + 72, MUTED_COLOR);
            } else if (snapshot.options().size() > MAX_VISIBLE_OPTIONS) {
                graphics.drawCenteredString(
                        font,
                        "+" + (snapshot.options().size() - MAX_VISIBLE_OPTIONS) + " more branches not shown",
                        this.width / 2,
                        bottom - 18,
                        MUTED_COLOR
                );
            }
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private static String displaySpecies(ResourceLocation species) {
        String path = species.getPath().replace('_', ' ');
        if (path.isEmpty()) {
            return species.toString();
        }
        return Character.toUpperCase(path.charAt(0)) + path.substring(1);
    }

    private static String compactNumber(double value) {
        if (!Double.isFinite(value)) {
            return "0";
        }
        double absolute = Math.abs(value);
        if (absolute >= 1_000_000D) {
            return String.format(Locale.ROOT, "%.1fM", value / 1_000_000D);
        }
        if (absolute >= 10_000D) {
            return String.format(Locale.ROOT, "%.1fK", value / 1_000D);
        }
        return Long.toString(Math.round(value));
    }
}
