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
        if (snapshot.target().isEmpty() || !snapshot.withinInteractionRange() || !snapshot.lineOfSight()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.screen != null) {
            return;
        }

        Component prompt = Component.translatable("hud.ebb.interact_prompt", Component.keybind("key.ebb.interact"));
        Font font = minecraft.font;
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
}
