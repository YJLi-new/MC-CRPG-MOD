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
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

public final class DialogueScreen extends Screen {
    private static final int PANEL_COLOR = 0xD0101018;
    private static final int PANEL_BORDER = 0xAA64E6FF;
    private static final int TITLE_COLOR = 0xFF64E6FF;
    private static final int TEXT_COLOR = 0xFFE8E8E8;
    private static final int STATUS_COLOR = 0xFFFFD166;
    private static final int CHIME_STATUS_COLOR = 0xFF64E6FF;
    private static final int TAKE_ROOT_STATUS_COLOR = 0xFFFF7A59;
    private static final int QUEST_STATUS_COLOR = 0xFFFFC857;
    private static final int FEAT_STATUS_COLOR = 0xFFB197FC;
    private static final int CLUE_STATUS_COLOR = 0xFF86EFAC;
    private static final int RELATION_STATUS_COLOR = 0xFFFF9F9F;
    private static final int MUTED_COLOR = 0xFFAAAAAA;
    private static final int VISIBLE_CHOICES = 5;
    private static final int PANEL_MAX_WIDTH = 560;
    private static final int PANEL_MAX_HEIGHT = 360;
    private static final int PANEL_MARGIN = 16;
    private static final int LINE_HEIGHT = 10;
    private static final long WAITING_TIMEOUT_MS = 10_000L;

    private final UUID conversationId;
    private final Identifier dialogueId;
    private String nodeId;
    private String speaker;
    private String text;
    private Optional<String> textKey;
    private List<VisibleDialogueChoice> choices;
    private Optional<RollResultPayload> rollResult;
    private Optional<String> statusMessage;
    private final List<HistoryEntry> history = new ArrayList<>();
    private String lastHistoryKey = "";
    private boolean closingFromServer;
    private boolean waitingForServer;
    private int choicePage;
    private int textScroll;
    private long nodeOpenedAtMillis;
    private long waitingStartedAtMillis;

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
        this.nodeOpenedAtMillis = System.currentTimeMillis();
        appendHistoryEntry(nodeId, speaker, text, textKey);
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
        this.nodeOpenedAtMillis = System.currentTimeMillis();
        appendHistoryEntry(nodeId, speaker, text, textKey);
        this.waitingForServer = false;
        this.waitingStartedAtMillis = 0L;
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
        int top = panelTop();
        int buttonWidth = panelWidth - 104;
        int buttonY = choicesTop();
        int start = choicePage * VISIBLE_CHOICES;
        int end = Math.min(choices.size(), start + VISIBLE_CHOICES);

        addSettingsWidgets(left, top);

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

    private void addSettingsWidgets(int left, int top) {
        addRenderableWidget(Button.builder(Component.literal("A-"), button -> {
            ClientDialogueSettings.decreaseFontScale();
            textScroll = Math.min(textScroll, maxTextScroll());
            rebuildWidgets();
        }).bounds(left + 16, top + 6, 28, 16).build());
        addRenderableWidget(Button.builder(Component.literal("A+"), button -> {
            ClientDialogueSettings.increaseFontScale();
            textScroll = Math.min(textScroll, maxTextScroll());
            rebuildWidgets();
        }).bounds(left + 48, top + 6, 28, 16).build());
        addRenderableWidget(Button.builder(ClientDialogueSettings.textSpeedLabel(), button -> {
            ClientDialogueSettings.cycleTextSpeed();
            nodeOpenedAtMillis = System.currentTimeMillis();
            rebuildWidgets();
        }).bounds(left + 82, top + 6, 108, 16).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
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
        renderHeaderTitle(graphics, left, top, right);
        drawScaledText(graphics, speakerComponent(), left + PANEL_MARGIN, top + 30, TITLE_COLOR);
        int bodyTop = top + 48;
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
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        if (!waitingForServer && !choices.isEmpty()) {
            int start = choicePage * VISIBLE_CHOICES;
            int visible = visibleChoiceCountOnPage();
            if (keyCode >= GLFW.GLFW_KEY_1 && keyCode <= GLFW.GLFW_KEY_5) {
                int offset = keyCode - GLFW.GLFW_KEY_1;
                if (offset < visible && start + offset < choices.size()) {
                    sendChoice(choices.get(start + offset).id());
                    return true;
                }
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                sendChoice(choices.get(start).id());
                return true;
            }
            if ((keyCode == GLFW.GLFW_KEY_PAGE_UP || keyCode == GLFW.GLFW_KEY_UP) && choicePage > 0) {
                choicePage--;
                rebuildWidgets();
                return true;
            }
            if ((keyCode == GLFW.GLFW_KEY_PAGE_DOWN || keyCode == GLFW.GLFW_KEY_DOWN) && choicePage < maxChoicePage()) {
                choicePage++;
                rebuildWidgets();
                return true;
            }
        }
        if (keyCode == GLFW.GLFW_KEY_HOME && textScroll != 0) {
            textScroll = 0;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_END) {
            int max = maxTextScroll();
            if (textScroll != max) {
                textScroll = max;
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public void tick() {
        super.tick();
        if (waitingForServer && waitingStartedAtMillis > 0L
                && System.currentTimeMillis() - waitingStartedAtMillis > WAITING_TIMEOUT_MS) {
            waitingForServer = false;
            waitingStartedAtMillis = 0L;
            statusMessage = Optional.of(Component.translatable("message.ebb.dialogue_choice_timeout").getString());
            rebuildWidgets();
        }
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
        waitingStartedAtMillis = System.currentTimeMillis();
        rollResult = Optional.empty();
        findChoice(choiceId)
                .flatMap(VisibleDialogueChoice::checkSummary)
                .ifPresent(summary -> statusMessage = Optional.of(Component.translatable("screen.ebb.dialogue.rolling", summary).getString()));
        if (!ClientInteractionNetworking.sendDialogueChoice(conversationId, choiceId)) {
            waitingForServer = false;
            waitingStartedAtMillis = 0L;
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
        int bodyHeight = Math.max(LINE_HEIGHT, statusTop() - 8 - (panelTop() + 48));
        int visibleLines = Math.max(1, bodyHeight / scaledLineHeight());
        return Math.max(0, this.font.split(bodyComponent(), splitWidth(panelWidth() - PANEL_MARGIN * 2)).size() - visibleLines);
    }

    private void renderScrollableBody(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        List<net.minecraft.util.FormattedCharSequence> lines = this.font.split(bodyComponent(), splitWidth(width));
        int lineHeight = scaledLineHeight();
        int visibleLines = Math.max(1, height / lineHeight);
        int maxScroll = Math.max(0, lines.size() - visibleLines);
        textScroll = Math.max(0, Math.min(maxScroll, textScroll));
        graphics.enableScissor(x, y, x + width, y + height);
        int lineY = y;
        for (int i = textScroll; i < Math.min(lines.size(), textScroll + visibleLines); i++) {
            drawScaledText(graphics, lines.get(i), x, lineY, TEXT_COLOR);
            lineY += lineHeight;
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
            lineY = renderStatusEchoes(graphics, statusMessage.get(), x, lineY, width, bottom);
        }
        if (waitingForServer && lineY + LINE_HEIGHT <= bottom) {
            drawScaledText(graphics, Component.translatable("screen.ebb.dialogue.waiting"), x, lineY, MUTED_COLOR);
        }
        graphics.disableScissor();
    }

    private void renderHeaderTitle(GuiGraphicsExtractor graphics, int left, int top, int right) {
        Component title = Component.translatable("screen.ebb.dialogue.heading", dialogueId.toString(), nodeId);
        int titleLeft = left + 202;
        int titleRight = right - PANEL_MARGIN;
        if (titleRight - titleLeft < 80) {
            titleLeft = left + PANEL_MARGIN;
        }
        graphics.enableScissor(titleLeft, top + 4, titleRight, top + 24);
        graphics.text(this.font, title, titleLeft, top + 9, TITLE_COLOR);
        graphics.disableScissor();
    }

    private int renderWrapped(GuiGraphicsExtractor graphics, Component component, int x, int y, int width, int bottom, int color) {
        int lineHeight = scaledLineHeight();
        List<net.minecraft.util.FormattedCharSequence> lines = this.font.split(component, splitWidth(width));
        for (net.minecraft.util.FormattedCharSequence line : lines) {
            if (y + lineHeight > bottom) {
                break;
            }
            drawScaledText(graphics, line, x, y, color);
            y += lineHeight;
        }
        return y + 2;
    }

    private int renderStatusEchoes(GuiGraphicsExtractor graphics, String status, int x, int y, int width, int bottom) {
        for (String rawPart : status.split(";")) {
            String part = rawPart.trim();
            if (part.isEmpty()) {
                continue;
            }
            y = renderWrapped(graphics, Component.literal(statusLabel(part)), x, y, width, bottom, statusColor(part));
            if (y + scaledLineHeight() > bottom) {
                break;
            }
        }
        return y;
    }

    private static String statusLabel(String status) {
        if (status.startsWith("clue_gained:")) return "线索获得： " + status.substring("clue_gained:".length());
        if (status.startsWith("clue_found:")) return "调查线索： " + status.substring("clue_found:".length());
        if (status.startsWith("clue_already_found:")) return "已知线索： " + status.substring("clue_already_found:".length());
        if (status.startsWith("journal_entry_added:")) return "日志更新： " + status.substring("journal_entry_added:".length());
        if (status.startsWith("quest_completed:")) return "任务推进： " + status.substring("quest_completed:".length());
        if (status.startsWith("quest_started:")) return "任务开始： " + status.substring("quest_started:".length());
        if (status.startsWith("take_root:")) return "扎根结算： " + status.substring("take_root:".length()).trim();
        if (status.startsWith("feat_unlocked:")) return "专长解锁： " + status.substring("feat_unlocked:".length());
        if (status.startsWith("feat_activate:")) return "专长槽位： " + status.substring("feat_activate:".length());
        if (status.startsWith("story_var_")) return "状态写回： " + status;
        if (status.startsWith("relation_")) return "关系变化： " + status;
        if (status.startsWith("npc_state_")) return "NPC记忆： " + status;
        if (status.startsWith("npc_routine_")) return "NPC行动： " + status;
        if (status.startsWith("conflict_status:")) return "冲突状态： " + status.substring("conflict_status:".length());
        if (status.startsWith("conflict_started:")) return "冲突开始： " + status.substring("conflict_started:".length());
        if (status.startsWith("conflict_stress:")) return "冲突压力： " + status.substring("conflict_stress:".length());
        if (status.startsWith("conflict_resolve:")) return "冲突进展： " + status.substring("conflict_resolve:".length());
        if (status.startsWith("conflict_outcome#")) return "冲突结果： " + status.substring("conflict_outcome#".length());
        if (status.startsWith("conflict_outcome_missing")) return "冲突结果错误： " + status;
        if (status.startsWith("conflict_")) return "冲突推进： " + status;
        if (status.startsWith("scene_phase:")) return "场景阶段： " + status.substring("scene_phase:".length());
        return status;
    }

    private static int statusColor(String status) {
        if (status.contains("[Chime:")) return CHIME_STATUS_COLOR;
        if (status.startsWith("clue_gained:") || status.startsWith("clue_") || status.startsWith("journal_entry_")) return CLUE_STATUS_COLOR;
        if (status.startsWith("take_root:")) return TAKE_ROOT_STATUS_COLOR;
        if (status.startsWith("quest_")) return QUEST_STATUS_COLOR;
        if (status.startsWith("feat_")) return FEAT_STATUS_COLOR;
        if (status.startsWith("relation_") || status.startsWith("npc_state_") || status.startsWith("npc_routine_")
                || status.startsWith("conflict_") || status.startsWith("scene_phase:")) return RELATION_STATUS_COLOR;
        return STATUS_COLOR;
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
        int preferredHeight = hasStatusLines() ? Math.max(46, scaledLineHeight() * 4) : scaledLineHeight();
        int minTop = panelTop() + 66;
        return Math.max(minTop, choicesTop() - preferredHeight);
    }

    private boolean hasStatusLines() {
        return rollResult.isPresent() || statusMessage.isPresent() || waitingForServer;
    }

    private Component speakerComponent() {
        return Component.translatable("screen.ebb.dialogue.speaker", speaker).withStyle(ChatFormatting.BOLD);
    }

    private Component bodyComponent() {
        if (history.isEmpty()) {
            return textKey.map(key -> Component.translatableWithFallback(key, text)).orElseGet(() -> Component.literal(text));
        }
        Component body = Component.empty();
        for (int i = 0; i < history.size(); i++) {
            HistoryEntry entry = history.get(i);
            if (i > 0) {
                body = body.copy().append(Component.literal("\n\n"));
            }
            boolean current = i == history.size() - 1;
            body = body.copy()
                    .append(Component.literal((current ? "▶ " : "") + entry.speaker() + ": ")
                            .withStyle(current ? ChatFormatting.AQUA : ChatFormatting.GRAY, ChatFormatting.BOLD))
                    .append(entryTextComponent(entry, current));
        }
        return body;
    }

    private Component entryTextComponent(HistoryEntry entry, boolean current) {
        Component component = entry.textKey()
                .map(key -> Component.translatableWithFallback(key, entry.text()))
                .orElseGet(() -> Component.literal(entry.text()));
        if (!current) {
            return component;
        }
        String full = component.getString();
        int visible = visibleCharacters(full);
        if (visible >= full.length()) {
            return component;
        }
        return Component.literal(full.substring(0, Math.max(0, visible)));
    }

    private int visibleCharacters(String text) {
        ClientDialogueSettings.TextSpeed speed = ClientDialogueSettings.textSpeed();
        if (speed == ClientDialogueSettings.TextSpeed.INSTANT || text.isEmpty()) {
            return text.length();
        }
        long elapsed = Math.max(0L, System.currentTimeMillis() - nodeOpenedAtMillis);
        int visible = (int) ((elapsed * speed.charsPerSecond()) / 1000L);
        return Math.min(text.length(), Math.max(1, visible));
    }

    private void appendHistoryEntry(String nodeId, String speaker, String text, Optional<String> textKey) {
        String key = nodeId + "\u0000" + speaker + "\u0000" + text + "\u0000" + textKey.orElse("");
        if (key.equals(lastHistoryKey)) {
            return;
        }
        history.add(new HistoryEntry(nodeId, speaker, text, textKey));
        lastHistoryKey = key;
    }

    private static Component choiceText(VisibleDialogueChoice choice) {
        return choice.textKey()
                .map(key -> Component.translatableWithFallback(key, choice.text()))
                .orElseGet(() -> Component.literal(choice.text()));
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

    private int scaledLineHeight() {
        return Math.max(LINE_HEIGHT, (int) Math.ceil(this.font.lineHeight * ClientDialogueSettings.fontScale()) + 2);
    }

    private int splitWidth(int width) {
        return Math.max(1, (int) Math.floor(width / ClientDialogueSettings.fontScale()));
    }

    private void drawScaledText(GuiGraphicsExtractor graphics, Component text, int x, int y, int color) {
        double scale = ClientDialogueSettings.fontScale();
        if (Math.abs(scale - 1.0D) < 0.001D) {
            graphics.text(this.font, text, x, y, color);
            return;
        }
        graphics.pose().pushMatrix();
        graphics.pose().translate((float) x, (float) y);
        graphics.pose().scale((float) scale);
        graphics.text(this.font, text, 0, 0, color);
        graphics.pose().popMatrix();
    }

    private void drawScaledText(GuiGraphicsExtractor graphics, net.minecraft.util.FormattedCharSequence text, int x, int y, int color) {
        double scale = ClientDialogueSettings.fontScale();
        if (Math.abs(scale - 1.0D) < 0.001D) {
            graphics.text(this.font, text, x, y, color);
            return;
        }
        graphics.pose().pushMatrix();
        graphics.pose().translate((float) x, (float) y);
        graphics.pose().scale((float) scale);
        graphics.text(this.font, text, 0, 0, color);
        graphics.pose().popMatrix();
    }

    private record HistoryEntry(String nodeId, String speaker, String text, Optional<String> textKey) {
        private HistoryEntry {
            textKey = textKey == null ? Optional.empty() : textKey;
        }
    }
}
