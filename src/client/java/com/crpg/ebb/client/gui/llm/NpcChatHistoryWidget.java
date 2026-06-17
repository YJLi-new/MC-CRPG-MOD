package com.crpg.ebb.client.gui.llm;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

public final class NpcChatHistoryWidget {
    public static final int LINE_HEIGHT = 10;
    private static final int PLAYER_COLOR = 0xFFFFFFFF;
    private static final int NPC_COLOR = 0xFF86EFAC;
    private static final int MUTED_COLOR = 0xFFAAAAAA;

    private NpcChatHistoryWidget() {
    }

    public static void render(GuiGraphicsExtractor graphics, Font font, List<Entry> lines,
                              int x, int y, int width, int height) {
        graphics.enableScissor(x, y, x + width, y + height);
        int lineY = y;
        List<Entry> visible = tail(lines, Math.max(1, height / (LINE_HEIGHT + 2)));
        for (Entry line : visible) {
            int color = switch (line.role()) {
                case "player" -> PLAYER_COLOR;
                case "npc" -> NPC_COLOR;
                default -> MUTED_COLOR;
            };
            Component prefix = Component.literal(roleLabel(line.role()) + (line.streaming() ? " … " : ": "))
                    .withStyle(ChatFormatting.BOLD);
            Component text = Component.empty().append(prefix).append(line.text());
            for (FormattedCharSequence split : font.split(text, width)) {
                if (lineY + LINE_HEIGHT > y + height) {
                    break;
                }
                graphics.text(font, split, x, lineY, color);
                lineY += LINE_HEIGHT + 2;
            }
            lineY += 2;
        }
        graphics.disableScissor();
    }

    private static List<Entry> tail(List<Entry> values, int maxLines) {
        if (values.size() <= maxLines) {
            return values;
        }
        return values.subList(values.size() - maxLines, values.size());
    }

    private static String roleLabel(String role) {
        return switch (role) {
            case "player" -> "you";
            case "npc" -> "npc";
            default -> "system";
        };
    }

    public record Entry(String role, Component text, List<String> citationIds, boolean streaming) {
        public Entry {
            citationIds = citationIds == null ? List.of() : List.copyOf(citationIds);
        }

        public static Entry player(Component text) {
            return new Entry("player", text, List.of(), false);
        }

        public static Entry npc(Component text, List<String> citationIds, boolean streaming) {
            return new Entry("npc", text, citationIds, streaming);
        }

        public static Entry system(Component text) {
            return new Entry("system", text, List.of(), false);
        }
    }
}
