package com.crpg.ebb.client.render;

import com.crpg.ebb.EbbMod;
import com.crpg.ebb.client.interaction.ClientInteractionState;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

public final class InteractionPromptHud {
    private static final int BACKGROUND = 0x90000000;
    private static final int BORDER = 0x8064E6FF;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int DEBUG_TEXT = 0xFFB8F4FF;

    private InteractionPromptHud() {
    }

    public static void register() {
        HudElementRegistry.attachElementAfter(
                VanillaHudElements.CROSSHAIR,
                EbbMod.id("interaction_prompt"),
                (graphics, tickCounter) -> render(graphics)
        );
    }

    private static void render(net.minecraft.client.gui.GuiGraphicsExtractor graphics) {
        ClientInteractionState.Snapshot snapshot = ClientInteractionState.snapshot();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.screen != null) {
            return;
        }

        Font font = minecraft.font;
        if (minecraft.getDebugOverlay().showDebugScreen()) {
            renderDebugReason(graphics, font, snapshot);
        }

        if (snapshot.target().isEmpty() || !snapshot.withinInteractionRange() || !snapshot.lineOfSight()) {
            return;
        }

        Component prompt = Component.translatable("hud.ebb.interact_prompt", Component.keybind("key.ebb.interact"));
        int x = graphics.guiWidth() / 2;
        int y = graphics.guiHeight() / 2 + 18;
        int width = font.width(prompt);
        int left = x - width / 2 - 6;
        int top = y - 4;
        int right = x + (width + 1) / 2 + 6;
        int bottom = y + 11;

        graphics.fill(left, top, right, bottom, BACKGROUND);
        graphics.outline(left, top, right - left, bottom - top, BORDER);
        graphics.centeredText(font, prompt, x, y, TEXT);
    }

    private static void renderDebugReason(
            net.minecraft.client.gui.GuiGraphicsExtractor graphics,
            Font font,
            ClientInteractionState.Snapshot snapshot
    ) {
        String reason = snapshot.reason();
        int detailSeparator = reason.indexOf(':');
        String reasonKind = detailSeparator >= 0 ? reason.substring(0, detailSeparator) : reason;
        String reasonDetail = detailSeparator >= 0 ? reason.substring(detailSeparator + 1) : "";
        String firstLine = "Ebb target: reason=" + reasonKind
                + " d=" + (Double.isFinite(snapshot.distance())
                ? String.format(java.util.Locale.ROOT, "%.2f", snapshot.distance())
                : "-")
                + (snapshot.withinInteractionRange() ? " in_range" : " too_far")
                + " style=" + snapshot.highlightStyle().renderMode().serializedName();
        java.util.List<String> lines = new java.util.ArrayList<>();
        lines.add(firstLine);
        snapshot.target().ifPresent(target -> {
            String secondLine = "id=" + target.id() + " dialogue=" + target.dialogueId();
            if (!reasonDetail.isBlank()) {
                secondLine += " match=" + reasonDetail;
            }
            lines.add(clip(secondLine, 96));
        });
        if (snapshot.target().isEmpty() && !reasonDetail.isBlank()) {
            lines.add("detail=" + clip(reasonDetail, 96));
        }
        int x = graphics.guiWidth() / 2;
        int maxWidth = Math.max(80, graphics.guiWidth() - 24);
        // Keep the Ebb-only F3 diagnostics out of Minecraft's vanilla debug
        // text blocks.  With auto GUI scaling, guiHeight is much smaller than
        // the raw screenshot height, so this intentionally sits close to the
        // hotbar instead of near the center prompt.
        int y = Math.max(8, graphics.guiHeight() - 46);
        for (int i = 0; i < lines.size(); i++) {
            Component text = Component.literal(clipToWidth(lines.get(i), maxWidth - 8, font));
            int width = font.width(text);
            int lineY = y + i * 10;
            graphics.fill(x - width / 2 - 4, lineY - 2, x + (width + 1) / 2 + 4, lineY + 10, BACKGROUND);
            graphics.centeredText(font, text, x, lineY, DEBUG_TEXT);
        }
    }

    private static String clip(String text, int maxChars) {
        if (text.length() <= maxChars) {
            return text;
        }
        return text.substring(0, Math.max(0, maxChars - 1)) + "…";
    }

    private static String clipToWidth(String text, int maxWidth, Font font) {
        if (font.width(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "…";
        int end = text.length();
        while (end > 0 && font.width(text.substring(0, end) + ellipsis) > maxWidth) {
            end--;
        }
        return text.substring(0, Math.max(0, end)) + ellipsis;
    }
}
