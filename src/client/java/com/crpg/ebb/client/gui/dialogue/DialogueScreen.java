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
    private static final int MUTED_COLOR = 0xFFAAAAAA;
    private static final int VISIBLE_CHOICES = 5;

    private final UUID conversationId;
    private final Identifier dialogueId;
    private String nodeId;
    private String speaker;
    private String text;
    private Optional<String> textKey;
    private List<VisibleDialogueChoice> choices;
    private Optional<RollResultPayload> rollResult;
    private Optional<String> statusMessage;
    private boolean closingFromServer;
    private boolean waitingForServer;
    private int choicePage;
    private int textScroll;

    public DialogueScreen(OpenDialoguePayload payload) {
        super(Component.translatable("screen.ebb.dialogue.title"));
        this.conversationId = payload.conversationId();
        this.dialogueId = payload.dialogueId();
        this.nodeId = payload.nodeId();
        this.speaker = payload.speaker();
        this.text = payload.text();
        this.textKey = payload.textKey();
        this.choices = List.copyOf(payload.choices());
        this.rollResult = Optional.empty();
        this.statusMessage = payload.statusMessage();
    }

    public UUID conversationId() {
        return conversationId;
    }

    public void apply(DialogueUpdatePayload payload) {
        this.nodeId = payload.nodeId();
        this.speaker = payload.speaker();
        this.text = payload.text();
        this.textKey = payload.textKey();
        this.choices = List.copyOf(payload.choices());
        this.rollResult = payload.rollResult();
        this.statusMessage = payload.statusMessage();
        this.waitingForServer = false;
        this.choicePage = Math.min(choicePage, maxChoicePage());
        this.textScroll = 0;
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
        int panelWidth = Math.min(520, this.width - 32);
        int left = (this.width - panelWidth) / 2;
        int buttonWidth = panelWidth - 104;
        int buttonY = Math.max(124, this.height - 28 - VISIBLE_CHOICES * 24);
        int start = choicePage * VISIBLE_CHOICES;
        int end = Math.min(choices.size(), start + VISIBLE_CHOICES);

        if (choices.isEmpty()) {
            addRenderableWidget(Button.builder(Component.translatable("screen.ebb.dialogue.end"), button -> closeByPlayer())
                    .bounds(left + 16, this.height - 52, panelWidth - 32, 20).build());
            return;
        }

        for (int i = start; i < end; i++) {
            VisibleDialogueChoice choice = choices.get(i);
            Button button = Button.builder(choiceLabel(choice), pressed -> sendChoice(choice.id()))
                    .bounds(left + 52, buttonY + (i - start) * 24, buttonWidth, 20).build();
            button.active = !waitingForServer;
            addRenderableWidget(button);
        }

        Button prev = Button.builder(Component.literal("▲"), button -> {
            if (choicePage > 0) {
                choicePage--;
                rebuildWidgets();
            }
        }).bounds(left + 16, buttonY, 28, 20).build();
        prev.active = choicePage > 0 && !waitingForServer;
        addRenderableWidget(prev);

        Button next = Button.builder(Component.literal("▼"), button -> {
            if (choicePage < maxChoicePage()) {
                choicePage++;
                rebuildWidgets();
            }
        }).bounds(left + panelWidth - 44, buttonY, 28, 20).build();
        next.active = choicePage < maxChoicePage() && !waitingForServer;
        addRenderableWidget(next);

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> closeByPlayer())
                .bounds(left + 16, this.height - 28, panelWidth - 32, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        extractTransparentBackground(graphics);

        int panelWidth = Math.min(560, this.width - 24);
        int panelHeight = Math.min(320, this.height - 36);
        int left = (this.width - panelWidth) / 2;
        int top = this.height - panelHeight - 18;
        int right = left + panelWidth;
        int bottom = top + panelHeight;

        graphics.fill(left, top, right, bottom, PANEL_COLOR);
        graphics.outline(left, top, panelWidth, panelHeight, PANEL_BORDER);
        graphics.centeredText(this.font, Component.translatable("screen.ebb.dialogue.heading", dialogueId.toString(), nodeId), this.width / 2, top + 8, TITLE_COLOR);
        graphics.text(this.font, speakerComponent(), left + 16, top + 28, TITLE_COLOR);
        int bodyTop = top + 44;
        int bodyBottom = bottom - 130;
        renderScrollableBody(graphics, left + 16, bodyTop, panelWidth - 32, bodyBottom - bodyTop);

        int statusY = bottom - 120;
        if (rollResult.isPresent()) {
            RollResultPayload result = rollResult.get();
            graphics.textWithWordWrap(this.font, Component.literal(result.summary()), left + 16, statusY, panelWidth - 32, STATUS_COLOR);
            statusY += 22;
        }
        if (statusMessage.isPresent()) {
            graphics.textWithWordWrap(this.font, Component.literal(statusMessage.get()), left + 16, statusY, panelWidth - 32, STATUS_COLOR);
            statusY += 22;
        }
        if (waitingForServer) {
            graphics.text(this.font, Component.translatable("screen.ebb.dialogue.waiting"), left + 16, statusY, MUTED_COLOR);
        }
        if (choices.size() > VISIBLE_CHOICES) {
            graphics.text(this.font, Component.literal("choices " + (choicePage + 1) + " / " + (maxChoicePage() + 1)), right - 110, bottom - 42, MUTED_COLOR);
        }

        super.extractRenderState(graphics, mouseX, mouseY, tickDelta);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int max = maxTextScroll();
        if (max <= 0) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        int next = Math.max(0, Math.min(max, textScroll - (int) Math.signum(scrollY)));
        if (next != textScroll) {
            textScroll = next;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (!closingFromServer) {
            ClientInteractionNetworking.sendDialogueClose(conversationId, "client_closed");
        }
        super.onClose();
    }

    private void sendChoice(String choiceId) {
        if (waitingForServer) {
            return;
        }
        waitingForServer = true;
        ClientInteractionNetworking.sendDialogueChoice(conversationId, choiceId);
        rebuildWidgets();
    }

    private void closeByPlayer() {
        ClientInteractionNetworking.sendDialogueClose(conversationId, "client_closed");
        this.closingFromServer = true;
        this.minecraft.setScreen(null);
    }

    private int maxChoicePage() {
        return Math.max(0, (choices.size() - 1) / VISIBLE_CHOICES);
    }

    private int maxTextScroll() {
        int panelWidth = Math.min(560, this.width - 24);
        int panelHeight = Math.min(320, this.height - 36);
        int bodyHeight = panelHeight - 174;
        int visibleLines = Math.max(1, bodyHeight / 10);
        return Math.max(0, this.font.split(bodyComponent(), panelWidth - 32).size() - visibleLines);
    }

    private void renderScrollableBody(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        List<net.minecraft.util.FormattedCharSequence> lines = this.font.split(bodyComponent(), width);
        int visibleLines = Math.max(1, height / 10);
        int maxScroll = Math.max(0, lines.size() - visibleLines);
        textScroll = Math.max(0, Math.min(maxScroll, textScroll));
        graphics.enableScissor(x, y, x + width, y + height);
        int lineY = y;
        for (int i = textScroll; i < Math.min(lines.size(), textScroll + visibleLines); i++) {
            graphics.text(this.font, lines.get(i), x, lineY, TEXT_COLOR);
            lineY += 10;
        }
        graphics.disableScissor();
        if (maxScroll > 0) {
            graphics.text(this.font, Component.literal("text " + (textScroll + 1) + " / " + (maxScroll + 1)), x + width - 90, y + height - 10, MUTED_COLOR);
        }
    }

    private Component speakerComponent() {
        return Component.translatable("screen.ebb.dialogue.speaker", speaker).withStyle(ChatFormatting.BOLD);
    }

    private Component bodyComponent() {
        return textKey.map(Component::translatable).orElseGet(() -> Component.literal(text));
    }

    private static Component choiceText(VisibleDialogueChoice choice) {
        return choice.textKey().map(Component::translatable).orElseGet(() -> Component.literal(choice.text()));
    }

    private static Component choiceLabel(VisibleDialogueChoice choice) {
        String suffix = choice.checkSummary().map(summary -> "  [" + summary + "]").orElse("");
        Component inner = choiceText(choice);
        return switch (choice.type()) {
            case DIALOGUE -> Component.empty().append(Component.literal("\u201c")).append(inner).append(Component.literal("\u201d" + suffix)).withStyle(ChatFormatting.WHITE);
            case ACTION -> Component.empty().append(Component.literal("(")).append(inner).append(Component.literal(")" + suffix)).withStyle(ChatFormatting.GOLD);
            case THOUGHT -> Component.empty().append(Component.literal("\u3010")).append(inner).append(Component.literal("\u3011" + suffix)).withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC);
        };
    }
}
