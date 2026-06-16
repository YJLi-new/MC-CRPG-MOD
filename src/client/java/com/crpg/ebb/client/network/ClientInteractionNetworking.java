package com.crpg.ebb.client.network;

import com.crpg.ebb.EbbMod;
import com.crpg.ebb.client.gui.dev.DevSnapshotScreen;
import com.crpg.ebb.client.gui.dialogue.DialogueScreen;
import com.crpg.ebb.client.gui.journal.JournalScreen;
import com.crpg.ebb.client.gui.llm.NpcChatScreen;
import com.crpg.ebb.client.gui.quest.QuestTreeScreen;
import com.crpg.ebb.client.interaction.ClientInteractionState;
import com.crpg.ebb.interaction.InteractionTarget;
import com.crpg.ebb.network.InteractionDeniedPayload;
import com.crpg.ebb.network.InteractionRequestPayload;
import com.crpg.ebb.network.OpenDialoguePayload;
import com.crpg.ebb.network.dialogue.ChooseDialogueOptionPayload;
import com.crpg.ebb.network.dialogue.CloseDialogueRequestPayload;
import com.crpg.ebb.network.dialogue.DialogueClosePayload;
import com.crpg.ebb.network.dialogue.DialogueUpdatePayload;
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
import com.crpg.ebb.network.journal.JournalPayload;
import com.crpg.ebb.network.quest.QuestTreePayload;
import com.crpg.ebb.client.interaction.ClientBlockGroupIndex;
import com.crpg.ebb.client.interaction.ClientEntityTargetIndex;
import com.crpg.ebb.network.dev.DevSnapshotPayload;
import com.crpg.ebb.interaction.entity.EntityBindingRegistry;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
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
        ClientPlayNetworking.registerGlobalReceiver(JournalPayload.TYPE, (payload, context) ->
                context.client().execute(() -> context.client().setScreen(new JournalScreen(payload)))
        );
        ClientPlayNetworking.registerGlobalReceiver(QuestTreePayload.TYPE, (payload, context) ->
                context.client().execute(() -> context.client().setScreen(new QuestTreeScreen(payload)))
        );
        ClientPlayNetworking.registerGlobalReceiver(BlockGroupSyncPayload.TYPE, (payload, context) ->
                context.client().execute(() -> ClientBlockGroupIndex.rebuild(payload.definitions()))
        );
        ClientPlayNetworking.registerGlobalReceiver(EntityBindingSyncPayload.TYPE, (payload, context) ->
                context.client().execute(() -> EntityBindingRegistry.syncFromServer(payload.definitions(), payload.settings()))
        );
        ClientPlayNetworking.registerGlobalReceiver(EntityTargetSyncPayload.TYPE, (payload, context) ->
                context.client().execute(() -> ClientEntityTargetIndex.rebuild(payload.targets()))
        );
        ClientPlayNetworking.registerGlobalReceiver(LlmChatOpenedPayload.TYPE, (payload, context) ->
                context.client().execute(() -> context.client().setScreen(new NpcChatScreen(payload)))
        );
        ClientPlayNetworking.registerGlobalReceiver(LlmChatChunkPayload.TYPE, (payload, context) ->
                context.client().execute(() -> applyLlmChunk(context.client(), payload))
        );
        ClientPlayNetworking.registerGlobalReceiver(LlmChatOptionsPayload.TYPE, (payload, context) ->
                context.client().execute(() -> applyLlmOptions(context.client(), payload))
        );
        ClientPlayNetworking.registerGlobalReceiver(LlmChatErrorPayload.TYPE, (payload, context) ->
                context.client().execute(() -> applyLlmError(context.client(), payload))
        );
        ClientPlayNetworking.registerGlobalReceiver(LlmChatClosePayload.TYPE, (payload, context) ->
                context.client().execute(() -> closeLlmChat(context.client(), payload))
        );
        ClientPlayConnectionEvents.INIT.register((handler, client) -> clearSyncedInteractionData(client, true));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clearSyncedInteractionData(client, false));
    }

    private static void clearSyncedInteractionData(Minecraft client, boolean preserveIntegratedServerRegistries) {
        ClientBlockGroupIndex.clear();
        ClientEntityTargetIndex.clear();
        if (preserveIntegratedServerRegistries && client.getSingleplayerServer() != null) {
            // In an integrated server, common data registries are JVM-global.
            // Clearing EntityBindingRegistry at client INIT wipes the server's
            // freshly reloaded binding registry before InteractionSyncService
            // serializes EntityBindingSyncPayload, producing a 0-binding sync.
            // Dedicated clients still clear stale synced bindings at INIT, and
            // integrated clients receive/keep the authoritative in-process
            // registry until the server sync overwrites it with the same data.
            EbbMod.LOGGER.debug("Preserving shared integrated-server entity binding registry during client INIT clear.");
            return;
        }
        EntityBindingRegistry.clearSynced();
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

    public static boolean sendDialogueChoice(UUID conversationId, String choiceId) {
        if (ClientPlayNetworking.canSend(ChooseDialogueOptionPayload.TYPE)) {
            ClientPlayNetworking.send(new ChooseDialogueOptionPayload(conversationId, choiceId));
            return true;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.sendOverlayMessage(Component.translatable("message.ebb.dialogue_choice_network_unavailable"));
        }
        EbbMod.LOGGER.warn("Cannot send dialogue choice payload; server does not advertise {}", ChooseDialogueOptionPayload.TYPE.id());
        return false;
    }

    public static void sendDialogueClose(UUID conversationId, String reason) {
        if (ClientPlayNetworking.canSend(CloseDialogueRequestPayload.TYPE)) {
            ClientPlayNetworking.send(new CloseDialogueRequestPayload(conversationId, reason));
        }
    }

    public static boolean sendLlmChatMessage(UUID conversationId, long nonce, String message) {
        if (ClientPlayNetworking.canSend(LlmChatMessagePayload.TYPE)) {
            ClientPlayNetworking.send(new LlmChatMessagePayload(conversationId, nonce, message));
            return true;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.sendOverlayMessage(Component.translatable("message.ebb.llm_chat_network_unavailable"));
        }
        EbbMod.LOGGER.warn("Cannot send LLM chat message payload; server does not advertise {}", LlmChatMessagePayload.TYPE.id());
        return false;
    }

    public static boolean sendLlmChatCancel(UUID conversationId, String reason) {
        if (ClientPlayNetworking.canSend(LlmChatCancelPayload.TYPE)) {
            ClientPlayNetworking.send(new LlmChatCancelPayload(conversationId, reason));
            return true;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.sendOverlayMessage(Component.translatable("message.ebb.llm_chat_network_unavailable"));
        }
        EbbMod.LOGGER.warn("Cannot send LLM chat cancel payload; server does not advertise {}", LlmChatCancelPayload.TYPE.id());
        return false;
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
                payload.textKey(),
                payload.choices(),
                payload.statusMessage()
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
    private static void applyLlmChunk(Minecraft minecraft, LlmChatChunkPayload payload) {
        if (minecraft.screen instanceof NpcChatScreen screen
                && screen.conversationId().equals(payload.conversationId())) {
            screen.appendChunk(payload);
            return;
        }
        if (minecraft.player != null) {
            minecraft.player.sendOverlayMessage(Component.literal(payload.content()));
        }
    }

    private static void applyLlmOptions(Minecraft minecraft, LlmChatOptionsPayload payload) {
        if (minecraft.screen instanceof NpcChatScreen screen
                && screen.conversationId().equals(payload.conversationId())) {
            screen.setSuggestedOptions(payload.options());
        }
    }

    private static void applyLlmError(Minecraft minecraft, LlmChatErrorPayload payload) {
        if (minecraft.screen instanceof NpcChatScreen screen
                && screen.conversationId().equals(payload.conversationId())) {
            screen.applyError(payload.reason());
            return;
        }
        if (minecraft.player != null) {
            minecraft.player.sendOverlayMessage(Component.translatable("message.ebb.llm_chat_error", payload.reason()));
        }
    }

    private static void closeLlmChat(Minecraft minecraft, LlmChatClosePayload payload) {
        if (minecraft.screen instanceof NpcChatScreen screen
                && screen.conversationId().equals(payload.conversationId())) {
            screen.closeFromServer(payload.reason());
        }
    }
}
