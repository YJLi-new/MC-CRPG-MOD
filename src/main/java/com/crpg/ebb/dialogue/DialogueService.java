package com.crpg.ebb.dialogue;

import com.crpg.ebb.EbbMod;
import com.crpg.ebb.chime.ChimeResolver;
import com.crpg.ebb.feat.FeatRegistry;
import com.crpg.ebb.investigation.InvestigationRegistry;
import com.crpg.ebb.interaction.EntityTarget;
import com.crpg.ebb.interaction.InteractionService;
import com.crpg.ebb.interaction.InteractionTarget;
import com.crpg.ebb.interaction.InteractionValidationResult;
import com.crpg.ebb.network.InteractionDeniedPayload;
import com.crpg.ebb.network.OpenDialoguePayload;
import com.crpg.ebb.network.dialogue.ChooseDialogueOptionPayload;
import com.crpg.ebb.network.dialogue.CloseDialogueRequestPayload;
import com.crpg.ebb.network.dialogue.DialogueClosePayload;
import com.crpg.ebb.network.dialogue.DialogueUpdatePayload;
import com.crpg.ebb.network.dialogue.RollResultPayload;
import com.crpg.ebb.network.dialogue.VisibleDialogueChoice;
import com.crpg.ebb.npc.EbbNpcEntity;
import com.crpg.ebb.state.NarrativeSavedData;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DialogueService {
    private static final String SYNTHETIC_NEXT_CHOICE_ID = "__ebb_continue";
    private static final Map<UUID, DialogueSession> SESSIONS = new ConcurrentHashMap<>();
    private static final Map<UUID, UUID> PLAYER_TO_SESSION = new ConcurrentHashMap<>();
    private static final long SESSION_TIMEOUT_TICKS = 20L * 60L * 5L;
    private static int tickCounter;

    private DialogueService() {
    }

    public static void registerLifecycleEvents() {
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> closeForPlayer(handler.player.getUUID(), "disconnect"));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> closeForPlayer(oldPlayer.getUUID(), "respawn"));
        ServerPlayerEvents.LEAVE.register(player -> closeForPlayer(player.getUUID(), "leave"));
        ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, origin, destination) -> closeForPlayer(player.getUUID(), "changed_level"));
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> clearAll("server_stopping"));
        ServerTickEvents.END_SERVER_TICK.register(DialogueService::onServerTick);
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

        long gameTime = player.level().getGameTime();
        UUID conversationId = UUID.randomUUID();
        Optional<UUID> entityUuid = target instanceof EntityTarget entityTarget ? Optional.of(entityTarget.entityUuid()) : Optional.empty();
        DialogueSession session = new DialogueSession(
                conversationId,
                player.getUUID(),
                target.dialogueId(),
                target.id(),
                target.type(),
                entityUuid,
                definition.get().start(),
                gameTime
        );
        SESSIONS.put(conversationId, session);
        PLAYER_TO_SESSION.put(player.getUUID(), conversationId);
        NarrativeSavedData state = NarrativeSavedData.get((ServerLevel) player.level());
        Optional<String> enterStatus = applyEffects(startNode.get().enterEffects(), state, player, session);
        responseSender.sendPacket(toOpenPayload(session, definition.get(), startNode.get(), state, enterStatus, currentDayTime(player)));
    }

    public static void choose(ServerPlayer player, ChooseDialogueOptionPayload payload, PacketSender responseSender) {
        DialogueSession session = SESSIONS.get(payload.conversationId());
        if (session == null || !session.playerUuid().equals(player.getUUID())) {
            responseSender.sendPacket(new DialogueClosePayload(payload.conversationId(), "missing_or_expired_session"));
            return;
        }
        long gameTime = player.level().getGameTime();
        if (gameTime - session.lastTouchedGameTime() > SESSION_TIMEOUT_TICKS) {
            close(session, responseSender, "session_timeout");
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
        if (choice.isEmpty() && SYNTHETIC_NEXT_CHOICE_ID.equals(payload.choiceId()) && node.get().next().isPresent()) {
            Optional<DialogueNode> next = definition.get().node(node.get().next().get());
            if (next.isEmpty()) {
                close(session, responseSender, "next_node_missing:" + node.get().next().get());
                return;
            }
            DialogueSession updated = session.withNode(node.get().next().get(), gameTime);
            SESSIONS.put(updated.conversationId(), updated);
            Optional<String> enterStatus = applyEffects(next.get().enterEffects(), state, player, updated);
            sendUpdate(responseSender, updated, definition.get(), next.get(), state, Optional.empty(), enterStatus, currentDayTime(player));
            return;
        }
        if (choice.isEmpty()) {
            DialogueSession touched = session.touch(gameTime);
            SESSIONS.put(touched.conversationId(), touched);
            sendUpdate(responseSender, touched, definition.get(), node.get(), state, Optional.empty(), Optional.of("invalid_choice:" + payload.choiceId()), currentDayTime(player));
            return;
        }
        if (choice.get().singleUse() && state.hasPlayerFlag(player.getUUID(), singleUseFlag(session.dialogueId(), choice.get().id()))) {
            DialogueSession touched = session.touch(gameTime);
            SESSIONS.put(touched.conversationId(), touched);
            sendUpdate(responseSender, touched, definition.get(), node.get(), state, Optional.empty(), Optional.of("choice_already_used:" + payload.choiceId()), currentDayTime(player));
            return;
        }
        if (!conditionsMet(choice.get(), state, player.getUUID(), currentDayTime(player))) {
            DialogueSession touched = session.touch(gameTime);
            SESSIONS.put(touched.conversationId(), touched);
            sendUpdate(responseSender, touched, definition.get(), node.get(), state, Optional.empty(), Optional.of("choice_unavailable:" + payload.choiceId()), currentDayTime(player));
            return;
        }
        if (choice.get().type() == ChoiceType.ACTION && choice.get().revalidateTarget()) {
            InteractionValidationResult validation = InteractionService.validateSessionTarget(player, session);
            if (!validation.allowed()) {
                DialogueSession touched = session.touch(gameTime);
                SESSIONS.put(touched.conversationId(), touched);
                sendUpdate(responseSender, touched, definition.get(), node.get(), state, Optional.empty(), Optional.of("action_target_invalid:" + validation.reason()), currentDayTime(player));
                return;
            }
        }

        Optional<String> preEffectStatus = applyEffects(choice.get().effects(), state, player, session);
        ChoiceResolution resolution = resolveChoice(player, state, choice.get());
        Optional<String> outcomeEffectStatus = applyEffects(resolution.outcomeEffects(), state, player, session);
        if (choice.get().singleUse()) {
            state.setPlayerFlag(player.getUUID(), singleUseFlag(session.dialogueId(), choice.get().id()), true);
        }
        Optional<String> status = combineStatus(preEffectStatus, combineStatus(outcomeEffectStatus, resolution.statusMessage()));

        if (resolution.nextNode().isEmpty()) {
            close(session, responseSender, "finished");
            return;
        }
        Optional<DialogueNode> next = definition.get().node(resolution.nextNode().get());
        if (next.isEmpty()) {
            close(session, responseSender, "next_node_missing:" + resolution.nextNode().get());
            return;
        }

        DialogueSession updated = session.withNode(resolution.nextNode().get(), gameTime);
        SESSIONS.put(updated.conversationId(), updated);
        Optional<String> enterStatus = applyEffects(next.get().enterEffects(), state, player, updated);
        sendUpdate(responseSender, updated, definition.get(), next.get(), state, resolution.rollResult(), combineStatus(status, enterStatus), currentDayTime(player));
    }

    public static void closeFromClient(ServerPlayer player, CloseDialogueRequestPayload payload) {
        DialogueSession session = SESSIONS.get(payload.conversationId());
        if (session != null && session.playerUuid().equals(player.getUUID())) {
            remove(session);
            EbbMod.LOGGER.debug("Closed dialogue session {} for {}: {}",
                    payload.conversationId(), player.getName().getString(), payload.reason());
        }
    }

    public static void closeForPlayer(UUID playerUuid, String reason) {
        UUID conversationId = PLAYER_TO_SESSION.remove(playerUuid);
        if (conversationId != null) {
            DialogueSession removed = SESSIONS.remove(conversationId);
            if (removed != null) {
                EbbMod.LOGGER.debug("Closed dialogue session {} for {}: {}", conversationId, playerUuid, reason);
            }
        }
    }

    public static void clearAll(String reason) {
        int count = SESSIONS.size();
        SESSIONS.clear();
        PLAYER_TO_SESSION.clear();
        if (count > 0) {
            EbbMod.LOGGER.info("Cleared {} dialogue session(s): {}", count, reason);
        }
    }

    public static int activeSessionCount() {
        return SESSIONS.size();
    }

    public static Optional<DialogueSession> currentSessionForPlayer(UUID playerUuid) {
        UUID conversationId = PLAYER_TO_SESSION.get(playerUuid);
        return conversationId == null ? Optional.empty() : Optional.ofNullable(SESSIONS.get(conversationId));
    }

    public static Optional<UUID> activeConversationPlayerForEntity(UUID entityUuid) {
        return SESSIONS.values().stream()
                .filter(session -> session.entityUuid().filter(entityUuid::equals).isPresent())
                .map(DialogueSession::playerUuid)
                .findFirst();
    }

    private static void onServerTick(MinecraftServer server) {
        tickCounter++;
        if (tickCounter % 200 != 0) {
            return;
        }
        long gameTime = server.overworld().getGameTime();
        for (DialogueSession session : List.copyOf(SESSIONS.values())) {
            if (gameTime - session.lastTouchedGameTime() > SESSION_TIMEOUT_TICKS) {
                remove(session);
                ServerPlayer player = server.getPlayerList().getPlayer(session.playerUuid());
                if (player != null && ServerPlayNetworking.canSend(player, DialogueClosePayload.TYPE)) {
                    ServerPlayNetworking.send(player, new DialogueClosePayload(session.conversationId(), "session_timeout"));
                }
            }
        }
    }

    private static ChoiceResolution resolveChoice(ServerPlayer player, NarrativeSavedData state, DialogueChoice choice) {
        if (choice.check().isEmpty()) {
            return new ChoiceResolution(choice.next(), Optional.empty(), Optional.empty(), List.of());
        }

        DialogueCheck check = choice.check().get();
        int featModifier = FeatRegistry.totalCheckModifier(state, player.getUUID(), check.attribute());
        int clueModifier = InvestigationRegistry.totalCheckModifier(state, player.getUUID(), check.attribute());
        int attributeScore = state.getAttribute(player.getUUID(), check.attribute()) + check.staticModifier() + featModifier + clueModifier;
        int firstRoll = player.getRandom().nextInt(20) + 1;
        int dieRoll = firstRoll;
        if (check.advantage()) {
            dieRoll = Math.max(firstRoll, player.getRandom().nextInt(20) + 1);
        }
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
        EbbMod.LOGGER.info("Dialogue roll {} choice {} for {}: d20 {} + {} = {} vs DC {} -> {}",
                choice.check().map(DialogueCheck::attribute).orElse(check.attribute()),
                choice.id(),
                player.getName().getString(),
                dieRoll,
                attributeScore,
                total,
                check.dc(),
                outcome);
        return new ChoiceResolution(next, Optional.of(roll), Optional.empty(), check.effectsForOutcome(outcome));
    }

    private static Optional<String> applyEffects(List<DialogueEffect> effects, NarrativeSavedData state, ServerPlayer player, DialogueSession session) {
        List<String> messages = new ArrayList<>();
        for (DialogueEffect effect : effects) {
            effect.apply(state, player.getUUID()).ifPresent(messages::add);
            if (effect.type() == DialogueEffect.EffectType.ROUTINE_PLACEHOLDER
                    || effect.type() == DialogueEffect.EffectType.SET_NPC_ROUTINE) {
                applyRoutineEffect(effect, player, session).ifPresent(messages::add);
            }
        }
        if (messages.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(String.join(", ", messages));
    }

    private static Optional<String> applyRoutineEffect(DialogueEffect effect, ServerPlayer player, DialogueSession session) {
        Optional<Entity> entity = session.entityUuid().map(uuid -> ((ServerLevel) player.level()).getEntityInAnyDimension(uuid));
        if (entity.isPresent() && entity.get() instanceof EbbNpcEntity npc) {
            try {
                Identifier routineId = Identifier.parse(effect.id());
                npc.setRoutineId(routineId);
                return Optional.of("npc_routine_set:" + routineId);
            } catch (RuntimeException ex) {
                return Optional.of("npc_routine_invalid:" + effect.id());
            }
        }
        return Optional.empty();
    }

    private static boolean conditionsMet(DialogueChoice choice, NarrativeSavedData state, UUID playerUuid, long dayTime) {
        return choice.conditions().stream().allMatch(condition -> condition.matches(state, playerUuid, dayTime));
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
            NarrativeSavedData state,
            Optional<String> statusMessage,
            long dayTime
    ) {
        Optional<String> combinedStatus = combineStatus(statusMessage, ChimeResolver.resolve(definition, node, state, session.playerUuid()));
        return new OpenDialoguePayload(
                session.conversationId(),
                definition.id(),
                node.id(),
                node.speaker(),
                node.text(),
                node.textKey(),
                visibleChoices(node, state, session.playerUuid(), session.dialogueId(), dayTime),
                combinedStatus
        );
    }

    private static void sendUpdate(
            PacketSender responseSender,
            DialogueSession session,
            DialogueDefinition definition,
            DialogueNode node,
            NarrativeSavedData state,
            Optional<RollResultPayload> rollResult,
            Optional<String> statusMessage,
            long dayTime
    ) {
        Optional<String> combinedStatus = combineStatus(statusMessage, ChimeResolver.resolve(definition, node, state, session.playerUuid()));
        responseSender.sendPacket(new DialogueUpdatePayload(
                session.conversationId(),
                definition.id(),
                node.id(),
                node.speaker(),
                node.text(),
                node.textKey(),
                visibleChoices(node, state, session.playerUuid(), session.dialogueId(), dayTime),
                rollResult,
                combinedStatus
        ));
    }

    private static List<VisibleDialogueChoice> visibleChoices(DialogueNode node, NarrativeSavedData state, UUID playerUuid, Identifier dialogueId, long dayTime) {
        List<VisibleDialogueChoice> visible = new ArrayList<>(node.choices().stream()
                .filter(choice -> conditionsMet(choice, state, playerUuid, dayTime))
                .filter(choice -> !choice.singleUse() || !state.hasPlayerFlag(playerUuid, singleUseFlag(dialogueId, choice.id())))
                .map(VisibleDialogueChoice::fromChoice)
                .toList());
        if (node.next().isPresent() && node.type() != DialogueNodeType.END) {
            visible.add(new VisibleDialogueChoice(SYNTHETIC_NEXT_CHOICE_ID, ChoiceType.DIALOGUE, "继续。", Optional.empty(), Optional.empty()));
        }
        return List.copyOf(visible);
    }

    private static String singleUseFlag(Identifier dialogueId, String choiceId) {
        return "dialogue_choice_used:" + dialogueId + ":" + choiceId;
    }

    private static long currentDayTime(ServerPlayer player) {
        return ((ServerLevel) player.level()).getOverworldClockTime();
    }

    private record ChoiceResolution(
            Optional<String> nextNode,
            Optional<RollResultPayload> rollResult,
            Optional<String> statusMessage,
            List<DialogueEffect> outcomeEffects
    ) {
    }
}
