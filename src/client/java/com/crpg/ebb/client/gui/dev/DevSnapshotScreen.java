package com.crpg.ebb.client.gui.dev;

import com.crpg.ebb.client.interaction.ClientInteractionState;
import com.crpg.ebb.interaction.InteractionTarget;
import com.crpg.ebb.network.dev.DevSnapshotPayload;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class DevSnapshotScreen extends Screen {
    private static final int BACKGROUND = 0xE0101018;
    private static final int BORDER = 0xAA64E6FF;
    private static final int TITLE = 0xFF64E6FF;
    private static final int TEXT = 0xFFE8E8E8;
    private static final int MUTED = 0xFFAAAAAA;

    private final List<String> serverLines;
    private int page;

    public DevSnapshotScreen(DevSnapshotPayload payload) {
        super(Component.translatable("screen.ebb.dev_snapshot.title"));
        this.serverLines = List.copyOf(payload.lines());
    }

    @Override
    protected void init() {
        int buttonY = this.height - 28;
        addRenderableWidget(Button.builder(Component.translatable("screen.ebb.dev_snapshot.prev"), button -> {
            if (page > 0) {
                page--;
            }
        }).bounds(this.width / 2 - 154, buttonY, 70, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.ebb.dev_snapshot.next"), button -> {
            if ((page + 1) * linesPerPage() < displayLines().size()) {
                page++;
            }
        }).bounds(this.width / 2 - 76, buttonY, 70, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> this.minecraft.setScreen(null))
                .bounds(this.width / 2 + 8, buttonY, 146, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        extractTransparentBackground(graphics);
        int left = 18;
        int top = 18;
        int right = this.width - 18;
        int bottom = this.height - 36;
        graphics.fill(left, top, right, bottom, BACKGROUND);
        graphics.outline(left, top, right - left, bottom - top, BORDER);
        graphics.centeredText(this.font, this.title, this.width / 2, top + 8, TITLE);

        List<String> lines = displayLines();
        int perPage = linesPerPage();
        int start = Math.min(page * perPage, Math.max(0, lines.size() - 1));
        int end = Math.min(lines.size(), start + perPage);
        int y = top + 26;
        for (int i = start; i < end; i++) {
            String line = lines.get(i);
            int color = line.isBlank() || line.endsWith(":") ? MUTED : TEXT;
            graphics.text(this.font, line, left + 12, y, color);
            y += 10;
        }
        graphics.text(this.font, Component.literal("page " + (page + 1) + " / " + Math.max(1, (int) Math.ceil(lines.size() / (double) perPage))), left + 12, bottom - 14, MUTED);
        super.extractRenderState(graphics, mouseX, mouseY, tickDelta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int linesPerPage() {
        return Math.max(1, (this.height - 92) / 10);
    }

    private List<String> displayLines() {
        List<String> lines = new ArrayList<>(serverLines);
        lines.add("");
        lines.add("Client current target:");
        ClientInteractionState.Snapshot snapshot = ClientInteractionState.snapshot();
        if (snapshot.target().isEmpty()) {
            lines.add("- none (" + snapshot.reason() + ")");
        } else {
            InteractionTarget target = snapshot.target().get();
            lines.add("- id=" + target.id());
            lines.add("- type=" + target.type() + ", dialogue=" + target.dialogueId());
            lines.add("- distance=" + String.format(java.util.Locale.ROOT, "%.2f", snapshot.distance())
                    + ", in_range=" + snapshot.withinInteractionRange()
                    + ", line_of_sight=" + snapshot.lineOfSight()
                    + ", reason=" + snapshot.reason());
        }
        return lines;
    }
}
