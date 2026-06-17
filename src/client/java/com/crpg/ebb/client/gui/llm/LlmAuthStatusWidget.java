package com.crpg.ebb.client.gui.llm;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public final class LlmAuthStatusWidget {
    private static final int STATUS_COLOR = 0xFFFFD166;

    private LlmAuthStatusWidget() {
    }

    public static void renderHint(GuiGraphicsExtractor graphics, Font font, int centerX, int y) {
        graphics.centeredText(font, Component.translatable("screen.ebb.menu.llm_auth_status_hint"), centerX, y, STATUS_COLOR);
    }
}
