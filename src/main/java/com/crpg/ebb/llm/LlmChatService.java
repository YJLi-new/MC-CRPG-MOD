package com.crpg.ebb.llm;

import com.crpg.ebb.EbbMod;
import com.crpg.ebb.dialogue.DialogueChoice;
import com.crpg.ebb.dialogue.DialogueDefinition;
import com.crpg.ebb.dialogue.DialogueNode;
import com.crpg.ebb.dialogue.DialogueSession;
import com.crpg.ebb.dialogue.DialogueService;
import com.crpg.ebb.interaction.InteractionService;
import com.crpg.ebb.interaction.InteractionValidationResult;
import com.crpg.ebb.network.llm.LlmChatCancelPayload;
import com.crpg.ebb.network.llm.LlmChatChunkPayload;
import com.crpg.ebb.network.llm.LlmChatClosePayload;
import com.crpg.ebb.network.llm.LlmChatErrorPayload;
import com.crpg.ebb.network.llm.LlmChatMessagePayload;
import com.crpg.ebb.network.llm.LlmChatOpenedPayload;
import com.crpg.ebb.network.llm.LlmChatOptionsPayload;
import com.crpg.ebb.llm.auth.LlmAuthService;
import com.crpg.ebb.npc.profile.NpcPromotionService;
import com.crpg.ebb.npc.knowledge.NpcKnowledgeService;
import com.crpg.ebb.state.NarrativeSavedData;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class LlmChatService {
    private static final Map<UUID, LlmChatSession> SESSIONS = new ConcurrentHashMap<>();
    private static final Map<UUID, UUID> PLAYER_TO_SESSION = new ConcurrentHashMap<>();
    private static final Map<String, AtomicInteger> SECURITY_EVENTS = new ConcurrentHashMap<>();
    private static final Map<String, RateWindow> RATE_WINDOWS = new ConcurrentHashMap<>();
    private static final String PROMOTED_MAJOR_STATUS = "promoted_major";
    private static volatile LlmGatewayClient testingClient;
    private static int tickCounter;

    private LlmChatService() {
    }

    public static void registerLifecycleEvents() {
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> closeForPlayer(handler.player.getUUID(), "disconnect"));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> closeForPlayer(oldPlayer.getUUID(), "respawn"));
        ServerPlayerEvents.LEAVE.register(player -> closeForPlayer(player.getUUID(), "leave"));
        ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, origin, destination) -> closeForPlayer(player.getUUID(), "changed_level"));
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> clearAll("server_stopping"));
        ServerTickEvents.END_SERVER_TICK.register(LlmChatService::onServerTick);
    }

    public static OpenResult openFromDialogue(
            ServerPlayer player,
            DialogueSession dialogueSession,
            DialogueDefinition definition,
            DialogueNode node,
            DialogueChoice choice,
            PacketSender responseSender
    ) {
        LlmConfig config = LlmConfig.current();
        if (!config.active()) {
            recordSecurityEvent("llm_disabled");
            return new OpenResult(false, "llm_disabled");
        }
        if (LlmAuthService.requiresAuth(config) && !LlmAuthService.hasValidToken(player.getUUID())) {
            recordSecurityEvent("llm_auth_required");
            return new OpenResult(false, "auth_required");
        }
        InteractionValidationResult targetValidation = InteractionService.validateSessionTarget(player, dialogueSession);
        if (!targetValidation.allowed()) {
            recordSecurityEvent("llm_target_invalid:" + targetValidation.reason());
            return new OpenResult(false, "llm_target_invalid:" + targetValidation.reason());
        }

        closeExistingSession(player.getUUID());
        long gameTime = player.level().getGameTime();
        UUID conversationId = UUID.randomUUID();
        LlmChoiceSettings settings = choice.llmSettings();
        Optional<NpcPromotionService.PromotionResult> promotion = NpcPromotionService.ensurePromotedIfMinor(player, dialogueSession);
        Optional<NpcPromotionService.PromotionResult> activePromotion = promotion.filter(result -> !"rate_limited".equals(result.status()));
        String npcKey = activePromotion
                .map(result -> result.profileId().toString())
                .orElseGet(() -> settings.npc().orElseGet(() -> inferNpcKey(dialogueSession, node)));
        String npcDisplayName = activePromotion.map(NpcPromotionService.PromotionResult::displayName).orElse(node.speaker());
        String topicHint = settings.topicHint().orElse(choice.text());
        String returnNode = settings.returnNode().orElseGet(node::id);
        String openedStatus = promotion
                .map(result -> result.created() ? PROMOTED_MAJOR_STATUS : result.status())
                .orElse(config.mode().serializedName());
        LlmChatSession session = new LlmChatSession(
                conversationId,
                player.getUUID(),
                dialogueSession.dialogueId(),
                dialogueSession.targetId(),
                dialogueSession.targetType(),
                dialogueSession.entityUuid(),
                node.id(),
                returnNode,
                npcKey,
                npcDisplayName,
                topicHint,
                gameTime,
                gameTime,
                false,
                0L
        );
        SESSIONS.put(conversationId, session);
        PLAYER_TO_SESSION.put(player.getUUID(), conversationId);
        responseSender.sendPacket(new LlmChatOpenedPayload(
                conversationId,
                session.npcKey(),
                session.npcDisplayName(),
                Optional.ofNullable(topicHint).filter(value -> !value.isBlank()),
                promotion.map(result -> openedStatus + ":" + result.profileId()).orElse(openedStatus),
                config.maxInputChars()
        ));
        return new OpenResult(true, "llm_chat_opened:" + openedStatus);
    }

    public static void handleMessage(ServerPlayer player, LlmChatMessagePayload payload, PacketSender responseSender) {
        long gameTime = player.level().getGameTime();
        Optional<LlmChatSession> maybeSession = validateMessagePacket(player.getUUID(), payload.conversationId(), payload.nonce(), gameTime);
        if (maybeSession.isEmpty()) {
            responseSender.sendPacket(new LlmChatErrorPayload(payload.conversationId(), "missing_or_invalid_llm_session"));
            return;
        }
        LlmChatSession session = maybeSession.get();
        LlmConfig config = LlmConfig.current();
        if (!config.active()) {
            recordSecurityEvent("llm_disabled_message");
            responseSender.sendPacket(new LlmChatErrorPayload(payload.conversationId(), "llm_disabled"));
            return;
        }
        if (LlmAuthService.requiresAuth(config) && !LlmAuthService.hasValidToken(player.getUUID())) {
            recordSecurityEvent("llm_auth_required_message");
            responseSender.sendPacket(new LlmChatErrorPayload(payload.conversationId(), "auth_required"));
            return;
        }
        String message = payload.message() == null ? "" : payload.message().strip();
        if (message.isBlank()) {
            responseSender.sendPacket(new LlmChatErrorPayload(payload.conversationId(), "empty_message"));
            return;
        }
        if (message.length() > config.maxInputChars()) {
            recordSecurityEvent("llm_message_too_long");
            responseSender.sendPacket(new LlmChatErrorPayload(payload.conversationId(), "message_too_long"));
            return;
        }
        if (session.awaitingResponse()) {
            responseSender.sendPacket(new LlmChatErrorPayload(payload.conversationId(), "llm_response_pending"));
            return;
        }
        if (!allowRate(config, player.getUUID(), session.npcKey())) {
            recordSecurityEvent("llm_rate_limited");
            responseSender.sendPacket(new LlmChatErrorPayload(payload.conversationId(), "rate_limited"));
            return;
        }
        if (session.entityUuid().isPresent()) {
            InteractionValidationResult validation = InteractionService.validateEntity(player, session.entityUuid().get());
            if (!validation.allowed()) {
                recordSecurityEvent("llm_target_invalid:" + validation.reason());
                responseSender.sendPacket(new LlmChatErrorPayload(payload.conversationId(), "llm_target_invalid:" + validation.reason()));
                return;
            }
        }

        LlmChatSession awaiting = session.awaiting(gameTime, payload.nonce());
        SESSIONS.put(awaiting.conversationId(), awaiting);
        responseSender.sendPacket(new LlmChatChunkPayload(payload.conversationId(), "player", message, true, Optional.of("player_turn"), List.of()));

        NarrativeSavedData narrativeState = NarrativeSavedData.get((ServerLevel) player.level());
        String knowledgeContext = NpcKnowledgeService.promptContext(
                awaiting.npcKey(),
                awaiting.topicHint() + " " + message,
                narrativeState,
                player.getUUID(),
                currentDayTime(player),
                6
        );
        LlmChatRequest request = new LlmChatRequest(
                awaiting.conversationId(),
                awaiting.playerUuid(),
                awaiting.entityUuid(),
                awaiting.dialogueId(),
                awaiting.sourceNodeId(),
                awaiting.npcKey(),
                awaiting.npcDisplayName(),
                awaiting.topicHint(),
                message,
                gameTime,
                knowledgeContext,
                LlmWorldIdentity.serverId(((ServerLevel) player.level()).getServer()),
                LlmWorldIdentity.worldId((ServerLevel) player.level())
        );
        LlmGatewayClient client = clientFor(config);
        CompletableFuture<LlmChatResponse> future = client.sendMessage(request);
        MinecraftServer server = ((ServerLevel) player.level()).getServer();
        future.whenComplete((response, error) -> server.execute(() -> completeResponse(player, awaiting, payload.nonce(), response, error)));
    }

    public static void closeFromClient(ServerPlayer player, LlmChatCancelPayload payload, PacketSender responseSender) {
        LlmChatSession session = SESSIONS.get(payload.conversationId());
        if (session != null && session.playerUuid().equals(player.getUUID())) {
            if ("return_to_script".equals(payload.reason())) {
                boolean reopened = DialogueService.reopenFromLlmChat(player, session, responseSender, "returned_from_llm_chat");
                if (reopened) {
                    remove(session);
                    EbbMod.LOGGER.debug("Returned LLM chat session {} for {} to scripted dialogue node {}",
                            payload.conversationId(), player.getName().getString(), session.returnNodeId());
                } else {
                    responseSender.sendPacket(new LlmChatErrorPayload(payload.conversationId(), "script_return_unavailable"));
                }
                return;
            }
            remove(session);
            EbbMod.LOGGER.debug("Closed LLM chat session {} for {}: {}", payload.conversationId(), player.getName().getString(), payload.reason());
        } else {
            recordSecurityEvent(session == null ? "llm_close_missing_session" : "llm_close_player_mismatch");
        }
    }

    public static void closeForPlayer(UUID playerUuid, String reason) {
        UUID conversationId = PLAYER_TO_SESSION.remove(playerUuid);
        if (conversationId != null) {
            LlmChatSession removed = SESSIONS.remove(conversationId);
            if (removed != null) {
                EbbMod.LOGGER.debug("Closed LLM chat session {} for {}: {}", conversationId, playerUuid, reason);
            }
        }
    }

    public static void clearAll(String reason) {
        int count = SESSIONS.size();
        SESSIONS.clear();
        PLAYER_TO_SESSION.clear();
        if (count > 0) {
            EbbMod.LOGGER.info("Cleared {} LLM chat session(s): {}", count, reason);
        }
    }

    public static int activeSessionCount() {
        return SESSIONS.size();
    }

    public static Optional<LlmChatSession> currentSessionForPlayer(UUID playerUuid) {
        UUID conversationId = PLAYER_TO_SESSION.get(playerUuid);
        return conversationId == null ? Optional.empty() : Optional.ofNullable(SESSIONS.get(conversationId));
    }

    public static String statusLine() {
        LlmConfig config = LlmConfig.current();
        return "Ebb LLM: " + config.summary() + " active_sessions=" + activeSessionCount()
                + " client=" + clientFor(config).providerName()
                + " auth_gate=server_only"
                + " rate_limit_windows=" + RATE_WINDOWS.size()
                + " no_secrets_in_payloads=true";
    }

    public static Map<String, Integer> securityEventSnapshot() {
        Map<String, Integer> snapshot = new java.util.LinkedHashMap<>();
        SECURITY_EVENTS.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> snapshot.put(entry.getKey(), entry.getValue().get()));
        return Map.copyOf(snapshot);
    }

    public static void setClientForTesting(LlmGatewayClient client) {
        testingClient = client;
    }

    public static void clearTestingOverrides() {
        testingClient = null;
        clearAll("testing_reset");
        SECURITY_EVENTS.clear();
        RATE_WINDOWS.clear();
        LlmAuthService.clearTestingOverrides();
        LlmConfig.clearTestingOverride();
    }

    public static LlmChatSession addSessionForTesting(LlmChatSession session) {
        SESSIONS.put(session.conversationId(), session);
        PLAYER_TO_SESSION.put(session.playerUuid(), session.conversationId());
        return session;
    }

    public static int closeExpiredSessionsForTesting(long gameTime) {
        return closeExpiredSessions(gameTime, null);
    }

    private static Optional<LlmChatSession> validateMessagePacket(UUID playerUuid, UUID conversationId, long nonce, long gameTime) {
        LlmChatSession session = SESSIONS.get(conversationId);
        if (session == null) {
            recordSecurityEvent("llm_missing_or_expired_session");
            return Optional.empty();
        }
        if (!session.playerUuid().equals(playerUuid)) {
            recordSecurityEvent("llm_session_player_mismatch");
            return Optional.empty();
        }
        if (nonce <= session.lastNonce()) {
            recordSecurityEvent("llm_replayed_nonce");
            return Optional.empty();
        }
        if (gameTime - session.lastTouchedGameTime() > LlmConfig.current().sessionTimeoutTicks()) {
            recordSecurityEvent("llm_session_timeout");
            remove(session);
            return Optional.empty();
        }
        return Optional.of(session.touch(gameTime));
    }

    private static void completeResponse(ServerPlayer player, LlmChatSession awaiting, long nonce, LlmChatResponse response, Throwable error) {
        LlmChatSession current = SESSIONS.get(awaiting.conversationId());
        if (current == null || !current.playerUuid().equals(player.getUUID()) || current.lastNonce() != nonce) {
            recordSecurityEvent("llm_response_stale_session");
            return;
        }
        if (error != null) {
            EbbMod.LOGGER.warn("LLM fake/gateway provider failed for session {}", awaiting.conversationId(), error);
            ServerPlayNetworking.send(player, new LlmChatErrorPayload(awaiting.conversationId(), "llm_provider_error"));
            SESSIONS.put(current.conversationId(), current.replied(player.level().getGameTime(), nonce));
            return;
        }
        if (response == null) {
            response = LlmChatResponse.error("llm_empty_response");
        }
        if (response.errorReason().isPresent()) {
            ServerPlayNetworking.send(player, new LlmChatErrorPayload(awaiting.conversationId(), response.errorReason().get()));
            SESSIONS.put(current.conversationId(), current.replied(player.level().getGameTime(), nonce));
            return;
        }
        sendStreamingNpcResponse(player, awaiting, response);
        if (!response.suggestedOptions().isEmpty()) {
            ServerPlayNetworking.send(player, new LlmChatOptionsPayload(awaiting.conversationId(), response.suggestedOptions()));
        }
        SESSIONS.put(current.conversationId(), current.replied(player.level().getGameTime(), nonce));
    }

    private static void sendStreamingNpcResponse(ServerPlayer player, LlmChatSession session, LlmChatResponse response) {
        List<String> chunks = LlmConfig.current().llmChatStreaming()
                ? streamingChunks(response.reply(), 96)
                : List.of(response.reply());
        for (int i = 0; i < chunks.size(); i++) {
            boolean done = i == chunks.size() - 1;
            ServerPlayNetworking.send(player, new LlmChatChunkPayload(
                    session.conversationId(),
                    "npc",
                    chunks.get(i),
                    done,
                    Optional.of(done ? response.status() : "streaming"),
                    done ? response.citationIds() : List.of()
            ));
        }
    }

    public static List<String> streamingChunks(String reply, int targetChunkChars) {
        String text = reply == null ? "" : reply;
        if (text.isEmpty()) {
            return List.of("");
        }
        int chunkSize = Math.max(24, targetChunkChars);
        List<String> chunks = new java.util.ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(text.length(), start + chunkSize);
            if (end < text.length()) {
                int split = Math.max(text.lastIndexOf(' ', end), text.lastIndexOf('\n', end));
                if (split > start + 16) {
                    end = split + 1;
                }
            }
            chunks.add(text.substring(start, end));
            start = end;
        }
        return List.copyOf(chunks);
    }

    private static void onServerTick(MinecraftServer server) {
        tickCounter++;
        if (tickCounter % 200 != 0) {
            return;
        }
        closeExpiredSessions(server.overworld().getGameTime(), server);
    }

    private static int closeExpiredSessions(long gameTime, MinecraftServer server) {
        int closed = 0;
        long timeout = LlmConfig.current().sessionTimeoutTicks();
        for (LlmChatSession session : List.copyOf(SESSIONS.values())) {
            if (gameTime - session.lastTouchedGameTime() > timeout) {
                remove(session);
                closed++;
                if (server != null) {
                    ServerPlayer player = server.getPlayerList().getPlayer(session.playerUuid());
                    if (player != null && ServerPlayNetworking.canSend(player, LlmChatClosePayload.TYPE)) {
                        ServerPlayNetworking.send(player, new LlmChatClosePayload(session.conversationId(), "llm_session_timeout"));
                    }
                }
            }
        }
        return closed;
    }

    private static void closeExistingSession(UUID playerUuid) {
        UUID existing = PLAYER_TO_SESSION.remove(playerUuid);
        if (existing != null) {
            SESSIONS.remove(existing);
        }
    }

    private static void remove(LlmChatSession session) {
        SESSIONS.remove(session.conversationId());
        PLAYER_TO_SESSION.remove(session.playerUuid(), session.conversationId());
    }

    private static long currentDayTime(ServerPlayer player) {
        return Math.floorMod(player.level().getOverworldClockTime(), 24_000L);
    }

    private static LlmGatewayClient clientFor(LlmConfig config) {
        LlmGatewayClient test = testingClient;
        if (test != null) {
            return test;
        }
        if (config.fakeMode()) {
            return new FakeLlmGatewayClient(config);
        }
        if (config.networkAccessAllowed()) {
            return new HttpLlmGatewayClient(config);
        }
        return new DisabledLlmGatewayClient();
    }

    private static String inferNpcKey(DialogueSession session, DialogueNode node) {
        if (node.speaker() != null && !node.speaker().isBlank()) {
            return "speaker:" + node.speaker();
        }
        return session.entityUuid().map(uuid -> "entity:" + uuid).orElseGet(() -> session.targetId().toString());
    }

    private static void recordSecurityEvent(String reason) {
        SECURITY_EVENTS.computeIfAbsent(reason, ignored -> new AtomicInteger()).incrementAndGet();
    }

    private static boolean allowRate(LlmConfig config, UUID playerUuid, String npcKey) {
        long now = System.currentTimeMillis();
        long window = now / 60_000L;
        String playerKey = "player:" + playerUuid;
        String npcKeyed = "player_npc:" + playerUuid + ":" + npcKey;
        return incrementWindow(playerKey, window, config.rateLimitPerMinute())
                && incrementWindow(npcKeyed, window, Math.max(1, config.rateLimitPerMinute()));
    }

    private static boolean incrementWindow(String key, long window, int limit) {
        RateWindow state = RATE_WINDOWS.computeIfAbsent(key, ignored -> new RateWindow(window, new AtomicInteger()));
        synchronized (state) {
            if (state.window != window) {
                state.window = window;
                state.count.set(0);
            }
            return state.count.incrementAndGet() <= Math.max(1, limit);
        }
    }

    private static final class RateWindow {
        private long window;
        private final AtomicInteger count;

        private RateWindow(long window, AtomicInteger count) {
            this.window = window;
            this.count = count;
        }
    }

    public record OpenResult(boolean opened, String status) {
    }
}
