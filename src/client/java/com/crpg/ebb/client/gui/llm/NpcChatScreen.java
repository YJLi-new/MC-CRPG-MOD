package com.crpg.ebb.client.gui.llm;

import com.crpg.ebb.client.network.ClientInteractionNetworking;
import com.crpg.ebb.network.llm.LlmChatChunkPayload;
import com.crpg.ebb.network.llm.LlmChatOpenedPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public final class NpcChatScreen extends Screen {
    private static final int PANEL_COLOR = 0xD0101018;
    private static final int PANEL_BORDER = 0xAA64E6FF;
    private static final int TITLE_COLOR = 0xFF64E6FF;
    private static final int TEXT_COLOR = 0xFFE8E8E8;
    private static final int PLAYER_COLOR = 0xFFFFFFFF;
    private static final int NPC_COLOR = 0xFF86EFAC;
    private static final int STATUS_COLOR = 0xFFFFD166;
    private static final int MUTED_COLOR = 0xFFAAAAAA;
    private static final int PANEL_MAX_WIDTH = 640;
    private static final int PANEL_MAX_HEIGHT = 410;
    private static final int PANEL_MARGIN = 16;
    private static final int LINE_HEIGHT = 10;
    private static final AtomicLong NONCE = new AtomicLong(1L);

    private final UUID conversationId;
    private final String npcKey;
    private final String npcDisplayName;
    private final Optional<String> topicHint;
    private final int maxInputChars;
    private final List<ChatLine> lines = new ArrayList<>();
    private List<String> suggestedOptions = List.of();
    private String status;
    private EditBox input;
    private boolean waitingForReply;
    private boolean closingFromServer;

    public NpcChatScreen(LlmChatOpenedPayload payload) {
        super(Component.translatable("screen.ebb.llm_chat.title"));
        this.conversationId = payload.conversationId();
        this.npcKey = payload.npcKey();
        this.npcDisplayName = payload.npcDisplayName();
        this.topicHint = payload.topicHint();
        this.status = payload.statusMessage();
        this.maxInputChars = payload.maxInputChars();
        topicHint.ifPresent(topic -> lines.add(new ChatLine("system", Component.translatable("screen.ebb.llm_chat.topic", topic))));
    }

    public UUID conversationId() {
        return conversationId;
    }

    @Override
    protected void init() {
        int left = panelLeft();
        int bottom = panelTop() + panelHeight();
        int inputY = bottom - 52;
        int inputWidth = panelWidth() - PANEL_MARGIN * 2 - 82;
        input = new EditBox(this.font, left + PANEL_MARGIN, inputY, inputWidth, 20, Component.translatable("screen.ebb.llm_chat.input"));
        input.setMaxLength(Math.max(1, maxInputChars));
        input.setSuggestion(Component.translatable("screen.ebb.llm_chat.input_hint").getString());
        input.setEditable(!waitingForReply);
        addRenderableWidget(input);
        setInitialFocus(input);

        Button send = Button.builder(Component.translatable("screen.ebb.llm_chat.send"), button -> sendCurrentInput())
                .bounds(left + PANEL_MARGIN + inputWidth + 6, inputY, 76, 20).build();
        send.active = !waitingForReply;
        addRenderableWidget(send);

        int optionY = inputY - 24;
        int optionWidth = Math.max(80, (panelWidth() - PANEL_MARGIN * 2 - 12) / 3);
        for (int i = 0; i < Math.min(3, suggestedOptions.size()); i++) {
            String option = suggestedOptions.get(i);
            int x = left + PANEL_MARGIN + i * (optionWidth + 6);
            Button optionButton = Button.builder(Component.literal(option), button -> sendSuggested(option))
                    .bounds(x, optionY, optionWidth, 20).build();
            optionButton.active = !waitingForReply;
            addRenderableWidget(optionButton);
        }

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> closeByPlayer())
                .bounds(left + PANEL_MARGIN, bottom - 26, panelWidth() - PANEL_MARGIN * 2, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        int left = panelLeft();
        int top = panelTop();
        int panelWidth = panelWidth();
        int panelHeight = panelHeight();
        int right = left + panelWidth;
        int bottom = top + panelHeight;
        graphics.fill(left, top, right, bottom, PANEL_COLOR);
        graphics.outline(left, top, panelWidth, panelHeight, PANEL_BORDER);
        graphics.centeredText(this.font, Component.translatable("screen.ebb.llm_chat.heading", npcDisplayName), this.width / 2, top + 9, TITLE_COLOR);
        graphics.text(this.font, Component.literal(npcKey).withStyle(ChatFormatting.GRAY), left + PANEL_MARGIN, top + 25, MUTED_COLOR);
        if (!status.isBlank()) {
            graphics.text(this.font, Component.literal(statusLabel(status)), left + PANEL_MARGIN, top + 38, STATUS_COLOR);
        }
        renderHistory(graphics, left + PANEL_MARGIN, top + 54, panelWidth - PANEL_MARGIN * 2, Math.max(40, panelHeight - 134));
        if (waitingForReply) {
            graphics.text(this.font, Component.translatable("screen.ebb.llm_chat.waiting"), left + PANEL_MARGIN, bottom - 70, MUTED_COLOR);
        }
        super.extractRenderState(graphics, mouseX, mouseY, tickDelta);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
            sendCurrentInput();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (!closingFromServer) {
            ClientInteractionNetworking.sendLlmChatCancel(conversationId, "client_closed");
        }
        super.onClose();
    }

    public void appendChunk(LlmChatChunkPayload payload) {
        if ("player".equals(payload.role())) {
            lines.add(new ChatLine("player", Component.literal(payload.content())));
        } else {
            Component component = Component.literal(payload.content());
            if (!payload.citationIds().isEmpty()) {
                component = component.copy().append(Component.literal("\n[" + String.join(", ", payload.citationIds()) + "]").withStyle(ChatFormatting.DARK_AQUA));
            }
            lines.add(new ChatLine("npc", component));
        }
        status = payload.statusMessage().orElse(status);
        waitingForReply = false;
        rebuildWidgets();
    }

    public void setSuggestedOptions(List<String> options) {
        suggestedOptions = options == null ? List.of() : List.copyOf(options);
        rebuildWidgets();
    }

    public void applyError(String reason) {
        waitingForReply = false;
        status = reason;
        lines.add(new ChatLine("system", Component.translatable("screen.ebb.llm_chat.error", reason)));
        rebuildWidgets();
    }

    public void closeFromServer(String reason) {
        closingFromServer = true;
        Minecraft.getInstance().setScreen(null);
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.sendOverlayMessage(Component.translatable("message.ebb.llm_chat_closed", reason));
        }
    }

    private void sendCurrentInput() {
        if (waitingForReply || input == null) {
            return;
        }
        String value = input.getValue().strip();
        if (value.isBlank()) {
            status = "empty_message";
            return;
        }
        waitingForReply = true;
        status = "sending";
        if (ClientInteractionNetworking.sendLlmChatMessage(conversationId, NONCE.getAndIncrement(), value)) {
            input.setValue("");
        } else {
            waitingForReply = false;
            status = "network_unavailable";
        }
        rebuildWidgets();
    }

    private void sendSuggested(String option) {
        if (input != null) {
            input.setValue(option);
        }
        sendCurrentInput();
    }

    private void closeByPlayer() {
        ClientInteractionNetworking.sendLlmChatCancel(conversationId, "client_closed");
        closingFromServer = true;
        this.minecraft.setScreen(null);
    }

    private void renderHistory(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        graphics.enableScissor(x, y, x + width, y + height);
        int lineY = y;
        List<ChatLine> visible = tail(lines, Math.max(1, height / (LINE_HEIGHT + 2)));
        for (ChatLine line : visible) {
            int color = switch (line.role()) {
                case "player" -> PLAYER_COLOR;
                case "npc" -> NPC_COLOR;
                default -> MUTED_COLOR;
            };
            Component prefix = Component.literal(roleLabel(line.role()) + ": ").withStyle(ChatFormatting.BOLD);
            Component text = Component.empty().append(prefix).append(line.text());
            for (net.minecraft.util.FormattedCharSequence split : this.font.split(text, width)) {
                if (lineY + LINE_HEIGHT > y + height) {
                    break;
                }
                graphics.text(this.font, split, x, lineY, color);
                lineY += LINE_HEIGHT + 2;
            }
            lineY += 2;
        }
        graphics.disableScissor();
    }

    private static List<ChatLine> tail(List<ChatLine> values, int maxLines) {
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

    private static String statusLabel(String status) {
        return switch (status) {
            case "fake" -> "Fake LLM provider";
            case "fake_reply" -> "Fake reply received";
            case "llm_disabled" -> "LLM disabled";
            case "sending" -> "Sending...";
            default -> status;
        };
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

    private record ChatLine(String role, Component text) {
    }
}
