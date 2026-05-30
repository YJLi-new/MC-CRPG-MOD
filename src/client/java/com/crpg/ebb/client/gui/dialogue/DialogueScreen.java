package com.crpg.ebb.client.gui.dialogue;

import com.crpg.ebb.client.network.ClientInteractionNetworking;
import com.crpg.ebb.dialogue.ChoiceType;
import com.crpg.ebb.network.OpenDialoguePayload;
import com.crpg.ebb.network.dialogue.DialogueUpdatePayload;
import com.crpg.ebb.network.dialogue.RollResultPayload;
import com.crpg.ebb.network.dialogue.VisibleDialogueChoice;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class DialogueScreen extends Screen {
    private static final int PANEL_COLOR = 0xD0101018;
    private static final int PANEL_BORDER = 0xAA64E6FF;
    private static final int TITLE_COLOR = 0xFF64E6FF;
    private static final int TEXT_COLOR = 0xFFE8E8E8;
    private static final int STATUS_COLOR = 0xFFFFD166;

    private final UUID conversationId;
    private final Identifier dialogueId;
    private String nodeId;
    private String speaker;
    private String text;
    private List<VisibleDialogueChoice> choices;
    private Optional<RollResultPayload> rollResult;
    private Optional<String> statusMessage;
    private boolean closingFromServer;

    public DialogueScreen(OpenDialoguePayload payload) {
        super(Component.translatable("screen.ebb.dialogue.title"));
        this.conversationId = payload.conversationId();
        this.dialogueId = payload.dialogueId();
        this.nodeId = payload.nodeId();
        this.speaker = payload.speaker();
        this.text = payload.text();
        this.choices = List.copyOf(payload.choices());
        this.rollResult = Optional.empty();
        this.statusMessage = Optional.empty();
    }

    public UUID conversationId() {
        return conversationId;
    }

    public void apply(DialogueUpdatePayload payload) {
        this.nodeId = payload.nodeId();
        this.speaker = payload.speaker();
        this.text = payload.text();
        this.choices = List.copyOf(payload.choices());
        this.rollResult = payload.rollResult();
        this.statusMessage = payload.statusMessage();
        if (minecraft != null) {
            rebuildWidgets();
        }
    }

    public void closeFromServer() {
        this.closingFromServer = true;
        Minecraft.getInstance().setScreen(null);
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(420, this.width - 32);
        int left = (this.width - panelWidth) / 2;
        int bottom = this.height - 28;
        int buttonY = Math.max(120, bottom - choices.size() * 24);
        int buttonWidth = panelWidth - 32;

        for (int i = 0; i < choices.size(); i++) {
            VisibleDialogueChoice choice = choices.get(i);
            Button button = Button.builder(choiceLabel(choice), pressed ->
                    ClientInteractionNetworking.sendDialogueChoice(conversationId, choice.id())
            ).bounds(left + 16, buttonY + i * 24, buttonWidth, 20).build();
            addRenderableWidget(button);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        extractTransparentBackground(graphics);

        int panelWidth = Math.min(460, this.width - 24);
        int panelHeight = Math.min(260, this.height - 36);
        int left = (this.width - panelWidth) / 2;
        int top = this.height - panelHeight - 18;
        int right = left + panelWidth;
        int bottom = top + panelHeight;

        graphics.fill(left, top, right, bottom, PANEL_COLOR);
        graphics.outline(left, top, panelWidth, panelHeight, PANEL_BORDER);
        graphics.centeredText(this.font, Component.translatable("screen.ebb.dialogue.heading", dialogueId.toString(), nodeId), this.width / 2, top + 8, TITLE_COLOR);
        graphics.text(this.font, speakerComponent(), left + 16, top + 28, TITLE_COLOR);
        graphics.textWithWordWrap(this.font, Component.literal(text), left + 16, top + 44, panelWidth - 32, TEXT_COLOR);
        int statusY = bottom - 96;
        if (rollResult.isPresent()) {
            RollResultPayload result = rollResult.get();
            graphics.textWithWordWrap(this.font, Component.literal(result.summary()), left + 16, statusY, panelWidth - 32, STATUS_COLOR);
            statusY += 22;
        }
        if (statusMessage.isPresent()) {
            graphics.textWithWordWrap(this.font, Component.literal(statusMessage.get()), left + 16, statusY, panelWidth - 32, STATUS_COLOR);
        }

        super.extractRenderState(graphics, mouseX, mouseY, tickDelta);
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    @Override
    public void onClose() {
        if (!closingFromServer) {
            ClientInteractionNetworking.sendDialogueClose(conversationId, "client_closed");
        }
        super.onClose();
    }

    private Component speakerComponent() {
        return Component.translatable("screen.ebb.dialogue.speaker", speaker).withStyle(ChatFormatting.BOLD);
    }

    private static Component choiceLabel(VisibleDialogueChoice choice) {
        String suffix = choice.checkSummary().map(summary -> "  [" + summary + "]").orElse("");
        return switch (choice.type()) {
            case DIALOGUE -> Component.literal("\u201c" + choice.text() + "\u201d" + suffix).withStyle(ChatFormatting.WHITE);
            case ACTION -> Component.literal("(" + choice.text() + ")" + suffix).withStyle(ChatFormatting.GOLD);
            case THOUGHT -> Component.literal("\u3010" + choice.text() + "\u3011" + suffix).withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC);
        };
    }
}
