package com.crpg.ebb.client.gui.quest;

import com.crpg.ebb.network.quest.QuestTreePayload;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class QuestTreeScreen extends Screen {
    private static final int BACKGROUND = 0xE0101018;
    private static final int BORDER = 0xAAFFC857;
    private static final int TITLE = 0xFFFFC857;
    private static final int TEXT = 0xFFE8E8E8;
    private static final int MUTED = 0xFFAAAAAA;
    private static final int MAJOR = 0xFFFFC857;
    private static final int MINOR = 0xFF86EFAC;
    private static final int TAKE_ROOT = 0xFFFF7A59;
    private static final int FEAT = 0xFFB197FC;
    private static final int ACTIVE = 0xFF64E6FF;

    private final List<String> lines;
    private int page;
    private Filter filter = Filter.ALL;

    public QuestTreeScreen(QuestTreePayload payload) {
        super(Component.literal("Esoteric Ebb Quest Tree"));
        this.lines = List.copyOf(payload.lines());
    }

    @Override
    protected void init() {
        int buttonY = this.height - 28;
        int tabY = 42;
        int tabX = 30;
        for (Filter value : Filter.values()) {
            addRenderableWidget(Button.builder(Component.literal(value.label), button -> {
                filter = value;
                page = 0;
                rebuildWidgets();
            }).bounds(tabX, tabY, value.width, 18).build());
            tabX += value.width + 4;
        }
        addRenderableWidget(Button.builder(Component.translatable("screen.ebb.dev_snapshot.prev"), button -> {
            if (page > 0) page--;
        }).bounds(this.width / 2 - 154, buttonY, 70, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.ebb.dev_snapshot.next"), button -> {
            if ((page + 1) * linesPerPage() < visibleLines().size()) page++;
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
        graphics.text(this.font, Component.literal("filter=" + filter.label + "  ◆ major  ◇ minor  ★ take-root  ▶ active feat"), left + 12, top + 28, MUTED);
        List<String> visible = visibleLines();
        int perPage = linesPerPage();
        int start = Math.min(page * perPage, Math.max(0, visible.size() - 1));
        int end = Math.min(visible.size(), start + perPage);
        int y = top + 66;
        for (int i = start; i < end; i++) {
            String line = visible.get(i);
            int color = lineColor(line);
            graphics.text(this.font, line, left + 12, y, color);
            y += 10;
        }
        graphics.text(this.font, Component.literal("page " + (page + 1) + " / " + Math.max(1, (int) Math.ceil(visible.size() / (double) perPage))), left + 12, bottom - 14, MUTED);
        super.extractRenderState(graphics, mouseX, mouseY, tickDelta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int linesPerPage() {
        return Math.max(1, (this.height - 132) / 10);
    }

    private List<String> visibleLines() {
        if (filter == Filter.ALL) {
            return lines;
        }
        return lines.stream().filter(filter::matches).toList();
    }

    private static int lineColor(String line) {
        if (line.contains("TAKE ROOT") || line.startsWith("  ★")) return TAKE_ROOT;
        if (line.startsWith("◆ MAJOR")) return MAJOR;
        if (line.startsWith("◇ MINOR")) return MINOR;
        if (line.startsWith("Feat Loadout") || line.contains("feat") || line.contains("modifiers=")) return FEAT;
        if (line.startsWith("▶ ACTIVE")) return ACTIVE;
        if (line.isBlank() || line.endsWith(":")) return MUTED;
        return TEXT;
    }

    private enum Filter {
        ALL("All", 44),
        MAJOR("Major", 58),
        MINOR("Minor", 58),
        FEATS("Feats", 56),
        TAKE_ROOT("Take Root", 84);

        private final String label;
        private final int width;

        Filter(String label, int width) {
            this.label = label;
            this.width = width;
        }

        boolean matches(String line) {
            return switch (this) {
                case ALL -> true;
                case MAJOR -> line.startsWith("◆ MAJOR") || line.contains("major:");
                case MINOR -> line.startsWith("◇ MINOR") || line.contains("minor:");
                case FEATS -> line.startsWith("Feat Loadout") || line.startsWith("active_slots=")
                        || line.startsWith("▶ ") || line.startsWith("- ACTIVE") || line.startsWith("- UNLOCKED")
                        || line.startsWith("- LOCKED") || line.contains("modifiers=");
                case TAKE_ROOT -> line.contains("TAKE ROOT") || line.contains("take_rooted") || line.contains("source=major:");
            };
        }
    }
}
