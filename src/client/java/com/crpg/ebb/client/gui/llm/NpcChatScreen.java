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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
    private static final int CITATION_PANEL_COLOR = 0xC0002030;
    private static final int PANEL_MAX_WIDTH = 680;
    private static final int PANEL_MAX_HEIGHT = 438;
    private static final int PANEL_MARGIN = 16;
    private static final int LINE_HEIGHT = 10;
    private static final long CLIENT_REPLY_TIMEOUT_MS = 35_000L;
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
    private boolean memoryCorrectionMode;
    private boolean showCitations;
    private long waitingSinceMillis;
    private int streamingNpcLineIndex = -1;

    public NpcChatScreen(LlmChatOpenedPayload payload) {
        super(Component.translatable("screen.ebb.llm_chat.title"));
        this.conversationId = payload.conversationId();
        this.npcKey = payload.npcKey();
        this.npcDisplayName = payload.npcDisplayName();
        this.topicHint = payload.topicHint();
        this.status = payload.statusMessage();
        this.maxInputChars = payload.maxInputChars();
        topicHint.ifPresent(topic -> lines.add(ChatLine.system(Component.translatable("screen.ebb.llm_chat.topic", topic))));
    }

    public UUID conversationId() {
        return conversationId;
    }

    @Override
    protected void init() {
        int left = panelLeft();
        int bottom = panelTop() + panelHeight();
        int doneY = bottom - 26;
        int inputY = doneY - 26;
        int actionY = inputY - 24;
        int optionY = actionY - 24;
        int inputWidth = panelWidth() - PANEL_MARGIN * 2 - 82;
        input = new EditBox(this.font, left + PANEL_MARGIN, inputY, inputWidth, 20, Component.translatable("screen.ebb.llm_chat.input"));
        input.setMaxLength(Math.max(1, maxInputChars));
        input.setSuggestion(Component.translatable(memoryCorrectionMode
                ? "screen.ebb.llm_chat.memory_hint"
                : "screen.ebb.llm_chat.input_hint").getString());
        input.setEditable(!waitingForReply);
        addRenderableWidget(input);
        setInitialFocus(input);

        Button send = Button.builder(Component.translatable("screen.ebb.llm_chat.send"), button -> sendCurrentInput())
                .bounds(left + PANEL_MARGIN + inputWidth + 6, inputY, 76, 20).build();
        send.active = !waitingForReply;
        addRenderableWidget(send);

        int optionWidth = Math.max(80, (panelWidth() - PANEL_MARGIN * 2 - 12) / 3);
        for (int i = 0; i < Math.min(3, suggestedOptions.size()); i++) {
            String option = suggestedOptions.get(i);
            int x = left + PANEL_MARGIN + i * (optionWidth + 6);
            Button optionButton = Button.builder(Component.literal(option), button -> sendSuggested(option))
                    .bounds(x, optionY, optionWidth, 20).build();
            optionButton.active = !waitingForReply;
            addRenderableWidget(optionButton);
        }

        int actionWidth = Math.max(80, (panelWidth() - PANEL_MARGIN * 2 - 12) / 3);
        Button returnButton = Button.builder(Component.translatable("screen.ebb.llm_chat.return_to_script"), button -> returnToScript())
                .bounds(left + PANEL_MARGIN, actionY, actionWidth, 20).build();
        returnButton.active = !waitingForReply;
        addRenderableWidget(returnButton);

        Button memoryButton = Button.builder(Component.translatable(memoryCorrectionMode
                        ? "screen.ebb.llm_chat.memory_on"
                        : "screen.ebb.llm_chat.memory_correction"), button -> toggleMemoryCorrection())
                .bounds(left + PANEL_MARGIN + actionWidth + 6, actionY, actionWidth, 20).build();
        memoryButton.active = !waitingForReply;
        addRenderableWidget(memoryButton);

        addRenderableWidget(Button.builder(Component.translatable(showCitations
                        ? "screen.ebb.llm_chat.citations_on"
                        : "screen.ebb.llm_chat.citations"), button -> toggleCitations())
                .bounds(left + PANEL_MARGIN + (actionWidth + 6) * 2, actionY, actionWidth, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> closeByPlayer())
                .bounds(left + PANEL_MARGIN, doneY, panelWidth() - PANEL_MARGIN * 2, 20).build());
    }

    @Override
    public void tick() {
        super.tick();
        if (waitingForReply && waitingSinceMillis > 0L
                && System.currentTimeMillis() - waitingSinceMillis > CLIENT_REPLY_TIMEOUT_MS) {
            waitingForReply = false;
            waitingSinceMillis = 0L;
            streamingNpcLineIndex = -1;
            status = "client_timeout";
            lines.add(ChatLine.system(Component.translatable("screen.ebb.llm_chat.timeout")));
            ClientInteractionNetworking.sendLlmChatCancel(conversationId, "client_timeout");
            rebuildWidgets();
        }
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
        renderHistory(graphics, left + PANEL_MARGIN, top + 54, panelWidth - PANEL_MARGIN * 2, Math.max(40, panelHeight - 172));
        if (showCitations) {
            renderCitationsOverlay(graphics, left + panelWidth - 230, top + 54, 214, Math.max(48, panelHeight - 172));
        }
        if (waitingForReply) {
            graphics.text(this.font, Component.translatable("screen.ebb.llm_chat.waiting"), left + PANEL_MARGIN, bottom - 106, MUTED_COLOR);
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
            lines.add(ChatLine.player(Component.literal(payload.content())));
            streamingNpcLineIndex = -1;
        } else {
            appendNpcChunk(payload);
        }
        status = payload.statusMessage().orElse(status);
        if (payload.done()) {
            waitingForReply = false;
            waitingSinceMillis = 0L;
            streamingNpcLineIndex = -1;
        } else {
            waitingForReply = true;
            waitingSinceMillis = System.currentTimeMillis();
        }
        rebuildWidgets();
    }

    public void setSuggestedOptions(List<String> options) {
        suggestedOptions = options == null ? List.of() : List.copyOf(options);
        rebuildWidgets();
    }

    public void applyError(String reason) {
        waitingForReply = false;
        waitingSinceMillis = 0L;
        streamingNpcLineIndex = -1;
        status = reason;
        lines.add(ChatLine.system(Component.translatable("screen.ebb.llm_chat.error", reason)));
        rebuildWidgets();
    }

    public void closeFromServer(String reason) {
        closingFromServer = true;
        Minecraft.getInstance().setScreen(null);
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.sendOverlayMessage(Component.translatable("message.ebb.llm_chat_closed", reason));
        }
    }

    private void appendNpcChunk(LlmChatChunkPayload payload) {
        int index = streamingNpcLineIndex;
        if (index < 0 || index >= lines.size() || !"npc".equals(lines.get(index).role())) {
            index = lines.size();
            lines.add(ChatLine.npc(Component.empty(), List.of(), true));
        }
        ChatLine previous = lines.get(index);
        Component text = previous.text().copy().append(Component.literal(payload.content()));
        List<String> citations = mergeCitations(previous.citationIds(), payload.citationIds());
        lines.set(index, ChatLine.npc(text, citations, !payload.done()));
        streamingNpcLineIndex = payload.done() ? -1 : index;
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
        String outgoing = memoryCorrectionMode ? "memory_correction: " + value : value;
        beginWaiting(memoryCorrectionMode ? "memory_correction_sending" : "sending");
        if (ClientInteractionNetworking.sendLlmChatMessage(conversationId, NONCE.getAndIncrement(), outgoing)) {
            input.setValue("");
            memoryCorrectionMode = false;
        } else {
            waitingForReply = false;
            waitingSinceMillis = 0L;
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

    private void returnToScript() {
        if (waitingForReply) {
            return;
        }
        beginWaiting("returning_to_script");
        if (!ClientInteractionNetworking.sendLlmChatCancel(conversationId, "return_to_script")) {
            waitingForReply = false;
            waitingSinceMillis = 0L;
            status = "network_unavailable";
        }
        rebuildWidgets();
    }

    private void toggleMemoryCorrection() {
        memoryCorrectionMode = !memoryCorrectionMode;
        status = memoryCorrectionMode ? "memory_correction_ready" : "memory_correction_off";
        rebuildWidgets();
    }

    private void toggleCitations() {
        showCitations = !showCitations;
        status = showCitations ? "citations_visible" : "citations_hidden";
        rebuildWidgets();
    }

    private void closeByPlayer() {
        ClientInteractionNetworking.sendLlmChatCancel(conversationId, "client_closed");
        closingFromServer = true;
        this.minecraft.setScreen(null);
    }

    private void beginWaiting(String newStatus) {
        waitingForReply = true;
        waitingSinceMillis = System.currentTimeMillis();
        status = newStatus;
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
            Component prefix = Component.literal(roleLabel(line.role()) + (line.streaming() ? " … " : ": ")).withStyle(ChatFormatting.BOLD);
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

    private void renderCitationsOverlay(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        List<String> citations = recentCitations();
        graphics.fill(x, y, x + width, y + height, CITATION_PANEL_COLOR);
        graphics.outline(x, y, width, height, PANEL_BORDER);
        graphics.text(this.font, Component.translatable("screen.ebb.llm_chat.citations_heading"), x + 6, y + 6, TITLE_COLOR);
        int lineY = y + 20;
        if (citations.isEmpty()) {
            graphics.text(this.font, Component.translatable("screen.ebb.llm_chat.no_citations"), x + 6, lineY, MUTED_COLOR);
            return;
        }
        for (String citation : citations.subList(0, Math.min(8, citations.size()))) {
            for (net.minecraft.util.FormattedCharSequence split : this.font.split(Component.literal("• " + citation), width - 12)) {
                if (lineY + LINE_HEIGHT > y + height - 6) {
                    return;
                }
                graphics.text(this.font, split, x + 6, lineY, STATUS_COLOR);
                lineY += LINE_HEIGHT + 2;
            }
        }
    }

    private List<String> recentCitations() {
        List<String> citations = new ArrayList<>();
        for (int i = lines.size() - 1; i >= 0 && citations.size() < 12; i--) {
            List<String> ids = lines.get(i).citationIds();
            for (int j = ids.size() - 1; j >= 0 && citations.size() < 12; j--) {
                citations.add(ids.get(j));
            }
        }
        return citations;
    }

    private static List<String> mergeCitations(List<String> previous, List<String> next) {
        Set<String> merged = new LinkedHashSet<>();
        if (previous != null) {
            merged.addAll(previous);
        }
        if (next != null) {
            merged.addAll(next);
        }
        return List.copyOf(merged);
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
            case "streaming" -> "Streaming...";
            case "returning_to_script" -> "Returning to scripted dialogue...";
            case "memory_correction_ready" -> "Memory correction mode: type the correction and send.";
            case "memory_correction_sending" -> "Sending memory correction...";
            case "memory_correction_off" -> "Memory correction mode off.";
            case "citations_visible" -> "Dev citations overlay visible.";
            case "citations_hidden" -> "Dev citations overlay hidden.";
            case "client_timeout" -> "Client timeout; chat controls released.";
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

    private record ChatLine(String role, Component text, List<String> citationIds, boolean streaming) {
        private ChatLine {
            citationIds = citationIds == null ? List.of() : List.copyOf(citationIds);
        }

        static ChatLine player(Component text) {
            return new ChatLine("player", text, List.of(), false);
        }

        static ChatLine npc(Component text, List<String> citationIds, boolean streaming) {
            return new ChatLine("npc", text, citationIds, streaming);
        }

        static ChatLine system(Component text) {
            return new ChatLine("system", text, List.of(), false);
        }
    }
}
