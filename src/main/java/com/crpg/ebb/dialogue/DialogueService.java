package com.crpg.ebb.dialogue;

import com.crpg.ebb.EbbMod;
import com.crpg.ebb.interaction.InteractionTarget;
import com.crpg.ebb.network.InteractionDeniedPayload;
import com.crpg.ebb.network.OpenDialoguePayload;
import com.crpg.ebb.network.dialogue.ChooseDialogueOptionPayload;
import com.crpg.ebb.network.dialogue.CloseDialogueRequestPayload;
import com.crpg.ebb.network.dialogue.DialogueClosePayload;
import com.crpg.ebb.network.dialogue.DialogueUpdatePayload;
import com.crpg.ebb.network.dialogue.RollResultPayload;
import com.crpg.ebb.network.dialogue.VisibleDialogueChoice;
import com.crpg.ebb.state.NarrativeSavedData;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DialogueService {
    private static final Map<UUID, DialogueSession> SESSIONS = new ConcurrentHashMap<>();
    private static final Map<UUID, UUID> PLAYER_TO_SESSION = new ConcurrentHashMap<>();

    private DialogueService() {
    }

    public static void open(ServerPlayer player, InteractionTarget target, PacketSender responseSender) {
        Optional<DialogueDefinition> definition = DialogueRegistry.byId(target.dialogueId());
        if (definition.isEmpty()) {
            responseSender.sendPacket(new InteractionDeniedPayload(target.id(), "unknown_dialogue:" + target.dialogueId()));
            return;
        }
        Optional<DialogueNode> startNode = definition.get().startNode();
        if (startNode.isEmpty()) {
            responseSender.sendPacket(new InteractionDeniedPayload(target.id(), "dialogue_has_no_start_node"));
            return;
        }

        closeExistingSession(player.getUUID());

        UUID conversationId = UUID.randomUUID();
        DialogueSession session = new DialogueSession(
                conversationId,
                player.getUUID(),
                target.dialogueId(),
                target.id(),
                target.type(),
                definition.get().start()
        );
        SESSIONS.put(conversationId, session);
        PLAYER_TO_SESSION.put(player.getUUID(), conversationId);
        NarrativeSavedData state = NarrativeSavedData.get((ServerLevel) player.level());
        responseSender.sendPacket(toOpenPayload(session, definition.get(), startNode.get(), state));
    }

    public static void choose(ServerPlayer player, ChooseDialogueOptionPayload payload, PacketSender responseSender) {
        DialogueSession session = SESSIONS.get(payload.conversationId());
        if (session == null || !session.playerUuid().equals(player.getUUID())) {
            responseSender.sendPacket(new DialogueClosePayload(payload.conversationId(), "missing_or_expired_session"));
            return;
        }

        Optional<DialogueDefinition> definition = DialogueRegistry.byId(session.dialogueId());
        if (definition.isEmpty()) {
            close(session, responseSender, "dialogue_unloaded");
            return;
        }
        Optional<DialogueNode> node = definition.get().node(session.nodeId());
        if (node.isEmpty()) {
            close(session, responseSender, "current_node_missing");
            return;
        }
        Optional<DialogueChoice> choice = node.get().choice(payload.choiceId());
        NarrativeSavedData state = NarrativeSavedData.get((ServerLevel) player.level());
        if (choice.isEmpty()) {
            sendUpdate(responseSender, session, definition.get(), node.get(), state, Optional.empty(), Optional.of("invalid_choice:" + payload.choiceId()));
            return;
        }
        if (!conditionsMet(choice.get(), state, player.getUUID())) {
            sendUpdate(responseSender, session, definition.get(), node.get(), state, Optional.empty(), Optional.of("choice_unavailable:" + payload.choiceId()));
            return;
        }

        Optional<String> effectStatus = applyEffects(choice.get(), state, player.getUUID());
        ChoiceResolution resolution = resolveChoice(player, state, choice.get());
        Optional<String> status = combineStatus(effectStatus, resolution.statusMessage());

        if (resolution.nextNode().isEmpty()) {
            close(session, responseSender, "finished");
            return;
        }
        Optional<DialogueNode> next = definition.get().node(resolution.nextNode().get());
        if (next.isEmpty()) {
            close(session, responseSender, "next_node_missing:" + resolution.nextNode().get());
            return;
        }

        DialogueSession updated = session.withNode(resolution.nextNode().get());
        SESSIONS.put(updated.conversationId(), updated);
        sendUpdate(responseSender, updated, definition.get(), next.get(), state, resolution.rollResult(), status);
    }

    public static void closeFromClient(ServerPlayer player, CloseDialogueRequestPayload payload) {
        DialogueSession session = SESSIONS.get(payload.conversationId());
        if (session != null && session.playerUuid().equals(player.getUUID())) {
            remove(session);
            EbbMod.LOGGER.debug("Closed dialogue session {} for {}: {}",
                    payload.conversationId(), player.getName().getString(), payload.reason());
        }
    }

    public static int activeSessionCount() {
        return SESSIONS.size();
    }

    private static ChoiceResolution resolveChoice(ServerPlayer player, NarrativeSavedData state, DialogueChoice choice) {
        if (choice.check().isEmpty()) {
            return new ChoiceResolution(choice.next(), Optional.empty(), Optional.empty());
        }

        DialogueCheck check = choice.check().get();
        int attributeScore = state.getAttribute(player.getUUID(), check.attribute());
        int dieRoll = player.getRandom().nextInt(20) + 1;
        int total = dieRoll + attributeScore;
        boolean critical = dieRoll == 1 || dieRoll == 20;
        boolean success;
        String outcome;
        Optional<String> next;

        if (dieRoll == 20) {
            success = true;
            outcome = "critical_success";
            next = check.criticalSuccess().or(check::success).or(choice::next);
        } else if (dieRoll == 1) {
            success = false;
            outcome = "critical_failure";
            next = check.criticalFailure().or(check::failure).or(choice::next);
        } else if (total >= check.dc()) {
            success = true;
            outcome = "success";
            next = check.success().or(choice::next);
        } else {
            success = false;
            outcome = "failure";
            next = check.failure().or(choice::next);
        }

        RollResultPayload roll = new RollResultPayload(
                check.attribute(),
                check.dc(),
                dieRoll,
                attributeScore,
                total,
                success,
                critical,
                outcome
        );
        return new ChoiceResolution(next, Optional.of(roll), Optional.empty());
    }

    private static Optional<String> applyEffects(DialogueChoice choice, NarrativeSavedData state, UUID playerUuid) {
        List<String> messages = new ArrayList<>();
        for (DialogueEffect effect : choice.effects()) {
            effect.apply(state, playerUuid).ifPresent(messages::add);
        }
        if (messages.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(String.join(", ", messages));
    }

    private static boolean conditionsMet(DialogueChoice choice, NarrativeSavedData state, UUID playerUuid) {
        return choice.conditions().stream().allMatch(condition -> condition.matches(state, playerUuid));
    }

    private static Optional<String> combineStatus(Optional<String> first, Optional<String> second) {
        if (first.isPresent() && second.isPresent()) {
            return Optional.of(first.get() + "; " + second.get());
        }
        return first.isPresent() ? first : second;
    }

    private static void closeExistingSession(UUID playerUuid) {
        UUID existing = PLAYER_TO_SESSION.remove(playerUuid);
        if (existing != null) {
            SESSIONS.remove(existing);
        }
    }

    private static void close(DialogueSession session, PacketSender responseSender, String reason) {
        remove(session);
        responseSender.sendPacket(new DialogueClosePayload(session.conversationId(), reason));
    }

    private static void remove(DialogueSession session) {
        SESSIONS.remove(session.conversationId());
        PLAYER_TO_SESSION.remove(session.playerUuid(), session.conversationId());
    }

    private static OpenDialoguePayload toOpenPayload(
            DialogueSession session,
            DialogueDefinition definition,
            DialogueNode node,
            NarrativeSavedData state
    ) {
        return new OpenDialoguePayload(
                session.conversationId(),
                definition.id(),
                node.id(),
                node.speaker(),
                node.text(),
                visibleChoices(node, state, session.playerUuid())
        );
    }

    private static void sendUpdate(
            PacketSender responseSender,
            DialogueSession session,
            DialogueDefinition definition,
            DialogueNode node,
            NarrativeSavedData state,
            Optional<RollResultPayload> rollResult,
            Optional<String> statusMessage
    ) {
        responseSender.sendPacket(new DialogueUpdatePayload(
                session.conversationId(),
                definition.id(),
                node.id(),
                node.speaker(),
                node.text(),
                visibleChoices(node, state, session.playerUuid()),
                rollResult,
                statusMessage
        ));
    }

    private static List<VisibleDialogueChoice> visibleChoices(DialogueNode node, NarrativeSavedData state, UUID playerUuid) {
        return node.choices().stream()
                .filter(choice -> conditionsMet(choice, state, playerUuid))
                .map(VisibleDialogueChoice::fromChoice)
                .toList();
    }

    private record ChoiceResolution(
            Optional<String> nextNode,
            Optional<RollResultPayload> rollResult,
            Optional<String> statusMessage
    ) {
    }
}
