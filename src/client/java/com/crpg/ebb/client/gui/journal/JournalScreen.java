package com.crpg.ebb.client.gui.journal;

import com.crpg.ebb.network.journal.JournalPayload;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class JournalScreen extends Screen {
    private static final int BACKGROUND = 0xE0101018;
    private static final int BORDER = 0xAA86EFAC;
    private static final int TITLE = 0xFF86EFAC;
    private static final int TEXT = 0xFFE8E8E8;
    private static final int HEADING = 0xFFFFD166;
    private static final int MUTED = 0xFFAAAAAA;
    private static final int CLUE = 0xFF86EFAC;
    private static final int LEAD = 0xFFFFC857;
    private static final int SCENE = 0xFF64E6FF;
    private static final int QUEST = 0xFFB197FC;

    private final List<String> lines;
    private int page;
    private Filter filter = Filter.ALL;

    public JournalScreen(JournalPayload payload) {
        super(Component.literal("Esoteric Ebb Journal"));
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
        graphics.text(this.font, Component.literal("filter=" + filter.label + "  clue / lead / scene-note / quest-note"), left + 12, top + 28, MUTED);
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
        if (line.equals("clue:") || line.contains("category=clue")) return CLUE;
        if (line.equals("lead:") || line.contains("category=lead")) return LEAD;
        if (line.equals("scene_note:") || line.contains("category=scene_note")) return SCENE;
        if (line.equals("quest_note:") || line.contains("category=quest_note") || line.trim().startsWith("quest=")) return QUEST;
        if (line.endsWith(":")) return HEADING;
        if (line.isBlank()) return MUTED;
        return TEXT;
    }

    private enum Filter {
        ALL("All", 44),
        CLUES("Clues", 60),
        LEADS("Leads", 58),
        QUESTS("Quests", 66),
        SCENES("Scenes", 66);

        private final String label;
        private final int width;

        Filter(String label, int width) {
            this.label = label;
            this.width = width;
        }

        boolean matches(String line) {
            return switch (this) {
                case ALL -> true;
                case CLUES -> line.equals("clue:") || line.contains("category=clue");
                case LEADS -> line.equals("lead:") || line.contains("category=lead");
                case QUESTS -> line.equals("quest_note:") || line.trim().startsWith("quest=") || line.contains("quest=");
                case SCENES -> line.equals("scene_note:") || line.contains("category=scene_note");
            };
        }
    }
}
