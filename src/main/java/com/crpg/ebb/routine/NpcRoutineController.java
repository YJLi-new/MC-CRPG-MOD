package com.crpg.ebb.routine;

import com.crpg.ebb.dialogue.DialogueDefinition;
import com.crpg.ebb.dialogue.DialogueNode;
import com.crpg.ebb.dialogue.DialogueNodeType;
import com.crpg.ebb.dialogue.DialogueRegistry;
import com.crpg.ebb.dialogue.DialogueSession;
import com.crpg.ebb.dialogue.DialogueService;
import com.crpg.ebb.npc.EbbNpcEntity;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;

public final class NpcRoutineController {
    private NpcRoutineController() {
    }

    public static void tick(EbbNpcEntity npc, ServerLevel level) {
        if (applyConversationFocus(npc, level)) {
            return;
        }
        npc.endConversationFocus();

        Optional<Identifier> routineId = npc.routineId();
        if (routineId.isEmpty()) {
            npc.setRoutineDebug("no_routine", "-", "-");
            return;
        }
        Optional<NpcRoutineDefinition> routine = NpcRoutineRegistry.byId(routineId.get());
        if (routine.isEmpty()) {
            npc.setRoutineDebug("missing_routine", routineId.get().toString(), "-");
            return;
        }

        applyMovement(npc, level, routine.get());
        applyLookAtPlayer(npc, level, routine.get().lookAtPlayer());
    }

    private static boolean applyConversationFocus(EbbNpcEntity npc, ServerLevel level) {
        Optional<DialogueSession> session = DialogueService.activeConversationSessionForEntity(npc.getUUID());
        Optional<ServerPlayer> player = session.map(DialogueSession::playerUuid)
                .map(uuid -> level.getServer().getPlayerList().getPlayer(uuid))
                .filter(found -> found.level() == level && !found.isSpectator());
        if (player.isEmpty()) {
            return false;
        }
        npc.getNavigation().stop();
        npc.getLookControl().setLookAt(player.get(), 12.0F, npc.getMaxHeadXRot());
        String animation = session.map(NpcRoutineController::conversationAnimationFor).orElse("talk");
        npc.beginConversationFocus(animation);
        npc.setRoutineDebug(
                "conversation_focus",
                player.get().getName().getString() + ":" + animation,
                session.map(value -> value.dialogueId() + "/" + value.nodeId()).orElse("-")
        );
        return true;
    }

    private static void applyMovement(EbbNpcEntity npc, ServerLevel level, NpcRoutineDefinition routine) {
        long dayTime = level.getOverworldClockTime();
        Optional<NpcRoutineDefinition.Step> maybeStep = routine.stepForTime(dayTime);
        if (maybeStep.isEmpty()) {
            npc.setRoutineDebug("no_step", routine.id().toString(), Long.toString(Math.floorMod(dayTime, 24000L)));
            return;
        }
        NpcRoutineDefinition.Step step = maybeStep.get();
        applyStepMetadata(npc, step);
        String action = step.action().toLowerCase(Locale.ROOT);
        String pathKey = routine.id() + ":" + step.startTime() + "-" + step.endTime() + ":" + step.action();
        int pathIndex = npc.routinePathIndex(pathKey, step.path().size());
        Vec3 destination = step.destinationAt(pathIndex);
        npc.setRoutineDebug(action, targetSummary(destination, step), pathKey + "#" + pathIndex);
        if ("wait".equals(action)) {
            npc.getNavigation().stop();
            return;
        }
        if ("play_animation".equals(action) || "set_pose".equals(action)) {
            if (destination == null) {
                npc.getNavigation().stop();
                return;
            }
        }
        if (destination == null) {
            return;
        }
        double distanceSqr = npc.position().distanceToSqr(destination);
        if ("look_at".equals(action)) {
            npc.getNavigation().stop();
            npc.getLookControl().setLookAt(destination.x, destination.y, destination.z, 12.0F, npc.getMaxHeadXRot());
            return;
        }
        if ("teleport_fallback".equals(action) || distanceSqr > step.teleportDistance() * step.teleportDistance()) {
            npc.teleportTo(destination.x, destination.y, destination.z);
            npc.getNavigation().stop();
            return;
        }
        if (step.hasWaypointPath() && distanceSqr < 0.75D) {
            npc.advanceRoutinePath(pathKey, step.path().size());
            destination = step.destinationAt(npc.routinePathIndex(pathKey, step.path().size()));
            distanceSqr = npc.position().distanceToSqr(destination);
        }
        if ("stand".equalsIgnoreCase(step.action()) && !step.hasWaypointPath() && distanceSqr < 0.75D) {
            npc.getNavigation().stop();
            return;
        }
        if (distanceSqr > 0.75D) {
            boolean brisk = "walk".equals(action) || "walk_path".equals(action);
            npc.getNavigation().moveTo(destination.x, destination.y, destination.z, brisk ? 0.65D : 0.45D);
        } else {
            npc.getNavigation().stop();
        }
    }

    private static void applyStepMetadata(EbbNpcEntity npc, NpcRoutineDefinition.Step step) {
        step.pose().ifPresent(npc::setNarrativePose);
        step.animation().ifPresent(npc::setNarrativeAnimation);
        if ("play_animation".equalsIgnoreCase(step.action()) && step.animation().isEmpty()) {
            npc.setNarrativeAnimation("scripted");
        }
        if ("set_pose".equalsIgnoreCase(step.action()) && step.pose().isEmpty()) {
            npc.setNarrativePose("scripted");
        }
    }

    private static String conversationAnimationFor(DialogueSession session) {
        Optional<DialogueDefinition> definition = DialogueRegistry.byId(session.dialogueId());
        Optional<DialogueNode> node = definition.flatMap(value -> value.node(session.nodeId()));
        if (node.isEmpty()) {
            return "nervous_idle";
        }
        DialogueNode current = node.get();
        String speaker = current.speaker().toLowerCase(Locale.ROOT);
        String nodeId = current.id().toLowerCase(Locale.ROOT);
        String text = current.text().toLowerCase(Locale.ROOT);
        if (current.type() == DialogueNodeType.END || nodeId.contains("end") || nodeId.contains("leave")) {
            return "dismiss";
        }
        if (nodeId.contains("failure") || text.contains("失败") || text.contains("谨慎")) {
            return "nervous_idle";
        }
        if (speaker.contains("inner") || speaker.contains("narrator")) {
            return "think";
        }
        return "talk";
    }

    private static String targetSummary(Vec3 destination, NpcRoutineDefinition.Step step) {
        String target = destination == null
                ? "-"
                : String.format(Locale.ROOT, "%.1f,%.1f,%.1f", destination.x, destination.y, destination.z);
        return step.look().map(look -> target + " look=" + look).orElse(target);
    }

    private static void applyLookAtPlayer(EbbNpcEntity npc, ServerLevel level, NpcRoutineDefinition.LookAtPlayer config) {
        if (!config.enabled()) {
            return;
        }
        double range = config.range();
        AABB area = npc.getBoundingBox().inflate(range);
        Optional<ServerPlayer> nearest = level.players().stream()
                .filter(player -> !player.isSpectator())
                .filter(player -> area.contains(player.position()))
                .filter(player -> !config.requiresLineOfSight() || npc.hasLineOfSight(player))
                .min(Comparator.comparingDouble(player -> player.distanceToSqr(npc)));
        nearest.ifPresent(player -> npc.getLookControl().setLookAt(player, config.maxYawSpeed(), npc.getMaxHeadXRot()));
    }
}
