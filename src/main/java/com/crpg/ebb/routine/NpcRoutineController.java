package com.crpg.ebb.routine;

import com.crpg.ebb.npc.EbbNpcEntity;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.Optional;

public final class NpcRoutineController {
    private NpcRoutineController() {
    }

    public static void tick(EbbNpcEntity npc, ServerLevel level) {
        Optional<Identifier> routineId = npc.routineId();
        if (routineId.isEmpty()) {
            return;
        }
        Optional<NpcRoutineDefinition> routine = NpcRoutineRegistry.byId(routineId.get());
        if (routine.isEmpty()) {
            return;
        }

        applyMovement(npc, level, routine.get());
        applyLookAtPlayer(npc, level, routine.get().lookAtPlayer());
    }

    private static void applyMovement(EbbNpcEntity npc, ServerLevel level, NpcRoutineDefinition routine) {
        routine.stepForTime(level.getOverworldClockTime()).ifPresent(step -> {
            Vec3 destination = step.destination();
            if (destination == null) {
                return;
            }
            if ("stand".equalsIgnoreCase(step.action()) && npc.position().distanceToSqr(destination) < 0.75D) {
                npc.getNavigation().stop();
                return;
            }
            if (npc.position().distanceToSqr(destination) > 0.75D) {
                npc.getNavigation().moveTo(destination.x, destination.y, destination.z, "walk".equalsIgnoreCase(step.action()) ? 0.65D : 0.45D);
            } else {
                npc.getNavigation().stop();
            }
        });
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
                .min(Comparator.comparingDouble(player -> player.distanceToSqr(npc)));
        nearest.ifPresent(player -> npc.getLookControl().setLookAt(player, config.maxYawSpeed(), npc.getMaxHeadXRot()));
    }
}
