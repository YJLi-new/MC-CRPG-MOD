package com.crpg.ebb.client.gui.llm;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public final class NpcChatInputWidget {
    private NpcChatInputWidget() {
    }

    public static EditBox create(Font font, int x, int y, int width, int height, int maxChars,
                                 boolean memoryCorrectionMode, boolean waitingForReply) {
        EditBox input = new EditBox(font, x, y, width, height, Component.translatable("screen.ebb.llm_chat.input"));
        input.setMaxLength(Math.max(1, maxChars));
        input.setSuggestion(Component.translatable(memoryCorrectionMode
                ? "screen.ebb.llm_chat.memory_hint"
                : "screen.ebb.llm_chat.input_hint").getString());
        input.setEditable(!waitingForReply);
        return input;
    }
}
