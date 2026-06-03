package com.crpg.ebb.client.gui.menu;

import com.crpg.ebb.client.gui.dialogue.ClientDialogueSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class EbbMenuScreen extends Screen {
    private static final int PANEL_COLOR = 0xD0101018;
    private static final int PANEL_BORDER = 0xAA64E6FF;
    private static final int TITLE_COLOR = 0xFF64E6FF;
    private static final int TEXT_COLOR = 0xFFE8E8E8;
    private static final int MUTED_COLOR = 0xFFAAAAAA;
    private static final int PANEL_WIDTH = 300;
    private static final int PANEL_HEIGHT = 214;

    private String statusKey = "screen.ebb.menu.status.ready";

    public EbbMenuScreen() {
        super(Component.translatable("screen.ebb.menu.title"));
    }

    @Override
    protected void init() {
        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;
        int x = left + 20;
        int y = top + 42;
        int buttonWidth = PANEL_WIDTH - 40;

        addRenderableWidget(Button.builder(Component.translatable("screen.ebb.menu.journal"), button -> sendCommandAndClose("ebb journal"))
                .bounds(x, y, buttonWidth, 20).build());
        y += 24;
        addRenderableWidget(Button.builder(Component.translatable("screen.ebb.menu.quest"), button -> sendCommandAndClose("ebb quest"))
                .bounds(x, y, buttonWidth, 20).build());
        y += 24;
        addRenderableWidget(Button.builder(Component.translatable("screen.ebb.menu.attributes"), button -> sendCommandAndClose("ebb attributes"))
                .bounds(x, y, buttonWidth, 20).build());
        y += 24;
        addRenderableWidget(Button.builder(Component.translatable("screen.ebb.menu.vars"), button -> sendCommandAndClose("ebb vars"))
                .bounds(x, y, buttonWidth, 20).build());
        y += 28;

        addRenderableWidget(Button.builder(ClientDialogueSettings.fontScaleLabel(), button -> {
            ClientDialogueSettings.increaseFontScale();
            statusKey = "screen.ebb.menu.status.font_changed";
            rebuildWidgets();
        }).bounds(x, y, (buttonWidth - 6) / 2, 20).build());
        addRenderableWidget(Button.builder(ClientDialogueSettings.textSpeedLabel(), button -> {
            ClientDialogueSettings.cycleTextSpeed();
            statusKey = "screen.ebb.menu.status.speed_changed";
            rebuildWidgets();
        }).bounds(x + (buttonWidth + 6) / 2, y, (buttonWidth - 6) / 2, 20).build());
        y += 28;

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> this.minecraft.setScreen(null))
                .bounds(x, y, buttonWidth, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;
        int right = left + PANEL_WIDTH;
        int bottom = top + PANEL_HEIGHT;
        graphics.fill(left, top, right, bottom, PANEL_COLOR);
        graphics.outline(left, top, PANEL_WIDTH, PANEL_HEIGHT, PANEL_BORDER);
        graphics.centeredText(this.font, this.title, this.width / 2, top + 10, TITLE_COLOR);
        graphics.centeredText(this.font, Component.translatable("screen.ebb.menu.subtitle"), this.width / 2, top + 24, MUTED_COLOR);
        graphics.centeredText(this.font, Component.translatable(statusKey), this.width / 2, bottom - 16, TEXT_COLOR);
        super.extractRenderState(graphics, mouseX, mouseY, tickDelta);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_K) {
            this.minecraft.setScreen(null);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void sendCommandAndClose(String command) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.player.connection == null) {
            statusKey = "screen.ebb.menu.status.no_player";
            rebuildWidgets();
            return;
        }
        client.player.connection.sendCommand(command);
        client.setScreen(null);
    }
}
