package com.crpg.ebb.network;

import com.crpg.ebb.EbbMod;
import com.crpg.ebb.dialogue.DialogueService;
import com.crpg.ebb.interaction.InteractionService;
import com.crpg.ebb.interaction.InteractionTarget;
import com.crpg.ebb.interaction.InteractionValidationResult;
import com.crpg.ebb.llm.LlmChatService;
import com.crpg.ebb.llm.auth.LlmAuthService;
import com.crpg.ebb.llm.auth.DeviceAuthStartResponse;
import com.crpg.ebb.llm.auth.DeviceAuthStatusResponse;
import com.crpg.ebb.network.dialogue.ChooseDialogueOptionPayload;
import com.crpg.ebb.network.dialogue.CloseDialogueRequestPayload;
import com.crpg.ebb.network.dialogue.DialogueClosePayload;
import com.crpg.ebb.network.dialogue.DialogueUpdatePayload;
import com.crpg.ebb.network.dev.DevSnapshotPayload;
import com.crpg.ebb.network.journal.JournalPayload;
import com.crpg.ebb.network.quest.QuestTreePayload;
import com.crpg.ebb.network.sync.BlockGroupSyncPayload;
import com.crpg.ebb.network.sync.EntityBindingSyncPayload;
import com.crpg.ebb.network.sync.EntityTargetSyncPayload;
import com.crpg.ebb.network.llm.LlmChatCancelPayload;
import com.crpg.ebb.network.llm.LlmChatChunkPayload;
import com.crpg.ebb.network.llm.LlmChatClosePayload;
import com.crpg.ebb.network.llm.LlmChatErrorPayload;
import com.crpg.ebb.network.llm.LlmChatMessagePayload;
import com.crpg.ebb.network.llm.LlmChatOpenedPayload;
import com.crpg.ebb.network.llm.LlmChatOptionsPayload;
import com.crpg.ebb.network.llm.LlmAuthStartPayload;
import com.crpg.ebb.network.llm.LlmAuthStatusPayload;
import com.crpg.ebb.network.llm.LlmAuthStatusRequestPayload;
import com.crpg.ebb.network.llm.LlmAuthUrlPayload;
import com.crpg.ebb.network.llm.MemoryDebugSnapshotPayload;
import com.crpg.ebb.network.llm.NpcProfileSyncPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class ModPackets {
    private ModPackets() {
    }

    public static void register() {
        registerPayloadTypes();
        registerServerReceivers();
    }

    private static void registerPayloadTypes() {
        PayloadTypeRegistry.serverboundPlay().register(InteractionRequestPayload.TYPE, InteractionRequestPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(InteractionDeniedPayload.TYPE, InteractionDeniedPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(OpenDialoguePayload.TYPE, OpenDialoguePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ChooseDialogueOptionPayload.TYPE, ChooseDialogueOptionPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(CloseDialogueRequestPayload.TYPE, CloseDialogueRequestPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(DialogueUpdatePayload.TYPE, DialogueUpdatePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(DialogueClosePayload.TYPE, DialogueClosePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(DevSnapshotPayload.TYPE, DevSnapshotPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(JournalPayload.TYPE, JournalPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(QuestTreePayload.TYPE, QuestTreePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(BlockGroupSyncPayload.TYPE, BlockGroupSyncPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(EntityBindingSyncPayload.TYPE, EntityBindingSyncPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(EntityTargetSyncPayload.TYPE, EntityTargetSyncPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(LlmAuthStartPayload.TYPE, LlmAuthStartPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(LlmAuthStatusRequestPayload.TYPE, LlmAuthStatusRequestPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(LlmAuthUrlPayload.TYPE, LlmAuthUrlPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(LlmAuthStatusPayload.TYPE, LlmAuthStatusPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(LlmChatOpenedPayload.TYPE, LlmChatOpenedPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(LlmChatMessagePayload.TYPE, LlmChatMessagePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(LlmChatChunkPayload.TYPE, LlmChatChunkPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(LlmChatOptionsPayload.TYPE, LlmChatOptionsPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(LlmChatClosePayload.TYPE, LlmChatClosePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(LlmChatCancelPayload.TYPE, LlmChatCancelPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(LlmChatErrorPayload.TYPE, LlmChatErrorPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(NpcProfileSyncPayload.TYPE, NpcProfileSyncPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(MemoryDebugSnapshotPayload.TYPE, MemoryDebugSnapshotPayload.CODEC);
    }

    private static void registerServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(InteractionRequestPayload.TYPE, (payload, context) ->
                context.server().executeIfPossible(() -> handleInteractionRequest(payload, context.player(), context.responseSender()))
        );
        ServerPlayNetworking.registerGlobalReceiver(ChooseDialogueOptionPayload.TYPE, (payload, context) ->
                context.server().executeIfPossible(() -> handleDialogueChoice(payload, context.player(), context.responseSender()))
        );
        ServerPlayNetworking.registerGlobalReceiver(CloseDialogueRequestPayload.TYPE, (payload, context) ->
                context.server().executeIfPossible(() -> DialogueService.closeFromClient(context.player(), payload))
        );
        ServerPlayNetworking.registerGlobalReceiver(LlmAuthStartPayload.TYPE, (payload, context) ->
                context.server().executeIfPossible(() -> handleLlmAuthStart(context.player()))
        );
        ServerPlayNetworking.registerGlobalReceiver(LlmAuthStatusRequestPayload.TYPE, (payload, context) ->
                context.server().executeIfPossible(() -> handleLlmAuthStatus(context.player(), true))
        );
        ServerPlayNetworking.registerGlobalReceiver(LlmChatMessagePayload.TYPE, (payload, context) ->
                context.server().executeIfPossible(() -> LlmChatService.handleMessage(context.player(), payload, context.responseSender()))
        );
        ServerPlayNetworking.registerGlobalReceiver(LlmChatCancelPayload.TYPE, (payload, context) ->
                context.server().executeIfPossible(() -> LlmChatService.closeFromClient(context.player(), payload, context.responseSender()))
        );
    }

    private static void handleDialogueChoice(
            ChooseDialogueOptionPayload payload,
            ServerPlayer player,
            net.fabricmc.fabric.api.networking.v1.PacketSender responseSender
    ) {
        try {
            DialogueService.choose(player, payload, responseSender);
        } catch (Throwable throwable) {
            EbbMod.LOGGER.error("Unhandled Ebb dialogue choice failure for {} conversation={} choice={}",
                    player.getName().getString(), payload.conversationId(), payload.choiceId(), throwable);
            responseSender.sendPacket(new DialogueClosePayload(payload.conversationId(), "server_error"));
        }
    }

    private static void handleInteractionRequest(
            InteractionRequestPayload payload,
            ServerPlayer player,
            net.fabricmc.fabric.api.networking.v1.PacketSender responseSender
    ) {
        InteractionValidationResult result = validate(player, payload);
        if (!result.allowed()) {
            EbbMod.LOGGER.debug("Denied interaction request from {} for {} {}: {}",
                    player.getName().getString(), payload.targetType(), payload.targetId(), result.reason());
            responseSender.sendPacket(new InteractionDeniedPayload(payload.targetId(), result.reason()));
            return;
        }

        InteractionTarget target = result.target().orElseThrow();
        EbbMod.LOGGER.debug("Accepted interaction request from {} for {} -> dialogue {}",
                player.getName().getString(), target.id(), target.dialogueId());
        DialogueService.open(player, target, responseSender);
    }

    private static InteractionValidationResult validate(ServerPlayer player, InteractionRequestPayload payload) {
        return switch (payload.targetType()) {
            case BLOCK_GROUP -> InteractionService.validateBlockGroup(player, payload.targetId());
            case ENTITY -> payload.entityUuid()
                    .map(uuid -> InteractionService.validateEntity(player, uuid))
                    .orElseGet(() -> InteractionValidationResult.deny("missing_entity_uuid"));
        };
    }

    private static void handleLlmAuthStart(ServerPlayer player) {
        LlmAuthService.startDeviceAuth(player).thenAccept(response -> player.level().getServer().execute(() -> {
            if (!ServerPlayNetworking.canSend(player, LlmAuthUrlPayload.TYPE)) {
                player.sendSystemMessage(Component.literal("Ebb LLM auth URL unavailable: client does not advertise payload support."));
                return;
            }
            ServerPlayNetworking.send(player, authUrlPayload(response));
        }));
    }

    private static void handleLlmAuthStatus(ServerPlayer player, boolean verbosePending) {
        LlmAuthService.pollDeviceAuth(player).thenAccept(status -> player.level().getServer().execute(() -> {
            if (ServerPlayNetworking.canSend(player, LlmAuthStatusPayload.TYPE)) {
                ServerPlayNetworking.send(player, authStatusPayload(player, status, verbosePending));
            }
        }));
    }

    private static LlmAuthUrlPayload authUrlPayload(DeviceAuthStartResponse response) {
        if (!response.started()) {
            return new LlmAuthUrlPayload("", "", "", response.provider(), 1L);
        }
        return new LlmAuthUrlPayload(
                response.authSessionId(),
                response.verificationUrl(),
                response.userCode(),
                response.provider(),
                response.intervalSeconds()
        );
    }

    private static LlmAuthStatusPayload authStatusPayload(ServerPlayer player, DeviceAuthStatusResponse status, boolean verbosePending) {
        if (status.authenticated()) {
            return new LlmAuthStatusPayload("authenticated", status.token().orElseThrow().redactedSummary(), 0L, "");
        }
        if ("pending".equals(status.status())) {
            return new LlmAuthStatusPayload("pending", verbosePending ? LlmAuthService.safeStatusLine(player.getUUID()) : "", status.intervalSeconds(), "");
        }
        return new LlmAuthStatusPayload("error", LlmAuthService.safeStatusLine(player.getUUID()), 0L, status.error());
    }
}
