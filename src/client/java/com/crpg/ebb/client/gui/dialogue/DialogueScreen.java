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
    private static final int PANEL_MAX_WIDTH = 560;
    private static final int PANEL_MAX_HEIGHT = 360;
    private static final int PANEL_MARGIN = 16;
    private static final int LINE_HEIGHT = 10;

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
        int panelWidth = panelWidth();
        int left = panelLeft();
        int buttonWidth = panelWidth - 104;
        int buttonY = choicesTop();
        int start = choicePage * VISIBLE_CHOICES;
        int end = Math.min(choices.size(), start + VISIBLE_CHOICES);

        if (choices.isEmpty()) {
            addRenderableWidget(Button.builder(Component.translatable("screen.ebb.dialogue.end"), button -> closeByPlayer())
                    .bounds(left + PANEL_MARGIN, doneButtonY(), panelWidth - PANEL_MARGIN * 2, 20).build());
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
                .bounds(left + PANEL_MARGIN, doneButtonY(), panelWidth - PANEL_MARGIN * 2, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        extractTransparentBackground(graphics);

        int panelWidth = panelWidth();
        int panelHeight = panelHeight();
        int left = panelLeft();
        int top = panelTop();
        int right = left + panelWidth;
        int bottom = top + panelHeight;
        int choiceTop = choicesTop();
        int statusTop = statusTop();

        graphics.fill(left, top, right, bottom, PANEL_COLOR);
        graphics.outline(left, top, panelWidth, panelHeight, PANEL_BORDER);
        graphics.centeredText(this.font, Component.translatable("screen.ebb.dialogue.heading", dialogueId.toString(), nodeId), this.width / 2, top + 8, TITLE_COLOR);
        graphics.text(this.font, speakerComponent(), left + PANEL_MARGIN, top + 28, TITLE_COLOR);
        int bodyTop = top + 44;
        int bodyBottom = Math.max(bodyTop + LINE_HEIGHT, statusTop - 8);
        renderScrollableBody(graphics, left + PANEL_MARGIN, bodyTop, panelWidth - PANEL_MARGIN * 2, bodyBottom - bodyTop);
        renderStatusArea(graphics, left + PANEL_MARGIN, statusTop, panelWidth - PANEL_MARGIN * 2, Math.max(LINE_HEIGHT, choiceTop - statusTop - 8));

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
        rollResult = Optional.empty();
        findChoice(choiceId)
                .flatMap(VisibleDialogueChoice::checkSummary)
                .ifPresent(summary -> statusMessage = Optional.of(Component.translatable("screen.ebb.dialogue.rolling", summary).getString()));
        if (!ClientInteractionNetworking.sendDialogueChoice(conversationId, choiceId)) {
            waitingForServer = false;
            statusMessage = Optional.of(Component.translatable("message.ebb.dialogue_choice_network_unavailable").getString());
        }
        rebuildWidgets();
    }

    private Optional<VisibleDialogueChoice> findChoice(String choiceId) {
        return choices.stream().filter(choice -> choice.id().equals(choiceId)).findFirst();
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
        int bodyHeight = Math.max(LINE_HEIGHT, statusTop() - 8 - (panelTop() + 44));
        int visibleLines = Math.max(1, bodyHeight / 10);
        return Math.max(0, this.font.split(bodyComponent(), panelWidth() - PANEL_MARGIN * 2).size() - visibleLines);
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

    private void renderStatusArea(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        int bottom = y + height;
        int lineY = y;
        graphics.enableScissor(x, y, x + width, bottom);
        if (rollResult.isPresent()) {
            lineY = renderWrapped(graphics, Component.literal(rollResult.get().summary()), x, lineY, width, bottom, STATUS_COLOR);
        }
        if (statusMessage.isPresent()) {
            lineY = renderWrapped(graphics, Component.literal(statusMessage.get()), x, lineY, width, bottom, STATUS_COLOR);
        }
        if (waitingForServer && lineY + LINE_HEIGHT <= bottom) {
            graphics.text(this.font, Component.translatable("screen.ebb.dialogue.waiting"), x, lineY, MUTED_COLOR);
        }
        graphics.disableScissor();
    }

    private int renderWrapped(GuiGraphicsExtractor graphics, Component component, int x, int y, int width, int bottom, int color) {
        List<net.minecraft.util.FormattedCharSequence> lines = this.font.split(component, width);
        for (net.minecraft.util.FormattedCharSequence line : lines) {
            if (y + LINE_HEIGHT > bottom) {
                break;
            }
            graphics.text(this.font, line, x, y, color);
            y += LINE_HEIGHT;
        }
        return y + 2;
    }

    private int panelWidth() {
        return Math.min(PANEL_MAX_WIDTH, this.width - 24);
    }

    private int panelHeight() {
        return Math.min(PANEL_MAX_HEIGHT, this.height - 36);
    }

    private int panelLeft() {
        return (this.width - panelWidth()) / 2;
    }

    private int panelTop() {
        return this.height - panelHeight() - 18;
    }

    private int panelBottom() {
        return panelTop() + panelHeight();
    }

    private int doneButtonY() {
        return panelBottom() - 28;
    }

    private int visibleChoiceCountOnPage() {
        int start = choicePage * VISIBLE_CHOICES;
        int end = Math.min(choices.size(), start + VISIBLE_CHOICES);
        return Math.max(1, end - start);
    }

    private int choicesTop() {
        if (choices.isEmpty()) {
            return doneButtonY() - 28;
        }
        return doneButtonY() - 8 - visibleChoiceCountOnPage() * 24;
    }

    private int statusTop() {
        int preferredHeight = hasStatusLines() ? 46 : LINE_HEIGHT;
        int minTop = panelTop() + 62;
        return Math.max(minTop, choicesTop() - preferredHeight);
    }

    private boolean hasStatusLines() {
        return rollResult.isPresent() || statusMessage.isPresent() || waitingForServer;
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
