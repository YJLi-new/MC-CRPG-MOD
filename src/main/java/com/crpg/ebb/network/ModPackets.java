package com.crpg.ebb.network;

import com.crpg.ebb.EbbMod;
import com.crpg.ebb.dialogue.DialogueService;
import com.crpg.ebb.interaction.InteractionService;
import com.crpg.ebb.interaction.InteractionTarget;
import com.crpg.ebb.interaction.InteractionValidationResult;
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
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
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
}
