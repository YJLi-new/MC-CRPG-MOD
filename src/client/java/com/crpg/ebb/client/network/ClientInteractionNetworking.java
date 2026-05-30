package com.crpg.ebb.client.network;

import com.crpg.ebb.EbbMod;
import com.crpg.ebb.client.gui.dev.DevSnapshotScreen;
import com.crpg.ebb.client.gui.dialogue.DialogueScreen;
import com.crpg.ebb.client.interaction.ClientInteractionState;
import com.crpg.ebb.interaction.InteractionTarget;
import com.crpg.ebb.network.InteractionDeniedPayload;
import com.crpg.ebb.network.InteractionRequestPayload;
import com.crpg.ebb.network.OpenDialoguePayload;
import com.crpg.ebb.network.dialogue.ChooseDialogueOptionPayload;
import com.crpg.ebb.network.dialogue.CloseDialogueRequestPayload;
import com.crpg.ebb.network.dialogue.DialogueClosePayload;
import com.crpg.ebb.network.dialogue.DialogueUpdatePayload;
import com.crpg.ebb.network.dev.DevSnapshotPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.Optional;
import java.util.UUID;

public final class ClientInteractionNetworking {
    private ClientInteractionNetworking() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(InteractionDeniedPayload.TYPE, (payload, context) ->
                context.client().execute(() -> showDenied(context.client(), payload))
        );
        ClientPlayNetworking.registerGlobalReceiver(OpenDialoguePayload.TYPE, (payload, context) ->
                context.client().execute(() -> openDialogue(context.client(), payload))
        );
        ClientPlayNetworking.registerGlobalReceiver(DialogueUpdatePayload.TYPE, (payload, context) ->
                context.client().execute(() -> updateDialogue(context.client(), payload))
        );
        ClientPlayNetworking.registerGlobalReceiver(DialogueClosePayload.TYPE, (payload, context) ->
                context.client().execute(() -> closeDialogue(context.client(), payload))
        );
        ClientPlayNetworking.registerGlobalReceiver(DevSnapshotPayload.TYPE, (payload, context) ->
                context.client().execute(() -> context.client().setScreen(new DevSnapshotScreen(payload)))
        );
    }

    public static void sendCurrentTargetInteraction(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null || minecraft.screen != null) {
            return;
        }

        ClientInteractionState.Snapshot snapshot = ClientInteractionState.snapshot();
        Optional<InteractionTarget> target = snapshot.target();
        if (target.isEmpty()) {
            minecraft.player.sendOverlayMessage(Component.translatable("message.ebb.no_target"));
            return;
        }
        if (!snapshot.withinInteractionRange() || !snapshot.lineOfSight()) {
            minecraft.player.sendOverlayMessage(Component.translatable("message.ebb.target_too_far"));
            return;
        }
        if (!ClientPlayNetworking.canSend(InteractionRequestPayload.TYPE)) {
            minecraft.player.sendOverlayMessage(Component.translatable("message.ebb.network_unavailable"));
            EbbMod.LOGGER.warn("Cannot send interaction request payload; server does not advertise {}", InteractionRequestPayload.TYPE.id());
            return;
        }

        ClientPlayNetworking.send(InteractionRequestPayload.fromTarget(target.get()));
    }

    public static void sendDialogueChoice(UUID conversationId, String choiceId) {
        if (ClientPlayNetworking.canSend(ChooseDialogueOptionPayload.TYPE)) {
            ClientPlayNetworking.send(new ChooseDialogueOptionPayload(conversationId, choiceId));
        }
    }

    public static void sendDialogueClose(UUID conversationId, String reason) {
        if (ClientPlayNetworking.canSend(CloseDialogueRequestPayload.TYPE)) {
            ClientPlayNetworking.send(new CloseDialogueRequestPayload(conversationId, reason));
        }
    }

    private static void showDenied(Minecraft minecraft, InteractionDeniedPayload payload) {
        if (minecraft.player == null) {
            return;
        }
        minecraft.player.sendOverlayMessage(Component.translatable("message.ebb.interaction_denied", payload.reason()));
    }

    private static void openDialogue(Minecraft minecraft, OpenDialoguePayload payload) {
        minecraft.setScreen(new DialogueScreen(payload));
    }

    private static void updateDialogue(Minecraft minecraft, DialogueUpdatePayload payload) {
        if (minecraft.screen instanceof DialogueScreen dialogueScreen
                && dialogueScreen.conversationId().equals(payload.conversationId())) {
            dialogueScreen.apply(payload);
            return;
        }
        // If a packet races after a local screen close, reopen so the server-authoritative state remains visible.
        minecraft.setScreen(new DialogueScreen(new OpenDialoguePayload(
                payload.conversationId(),
                payload.dialogueId(),
                payload.nodeId(),
                payload.speaker(),
                payload.text(),
                payload.choices()
        )));
    }

    private static void closeDialogue(Minecraft minecraft, DialogueClosePayload payload) {
        if (minecraft.screen instanceof DialogueScreen dialogueScreen
                && dialogueScreen.conversationId().equals(payload.conversationId())) {
            dialogueScreen.closeFromServer();
        }
        if (minecraft.player != null) {
            minecraft.player.sendOverlayMessage(Component.translatable("message.ebb.dialogue_closed", payload.reason()));
        }
    }
}
