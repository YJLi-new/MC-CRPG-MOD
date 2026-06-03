package com.crpg.ebb.routine;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public record NpcRoutineDefinition(
        Identifier id,
        List<Step> steps,
        LookAtPlayer lookAtPlayer
) {
    public static final Set<String> ALLOWED_ACTIONS = Set.of(
            "stand",
            "wait",
            "walk",
            "walk_path",
            "look_at",
            "play_animation",
            "set_pose",
            "teleport_fallback"
    );
    public static final Set<String> ALLOWED_ANIMATIONS = Set.of(
            "idle",
            "walk",
            "fidget",
            "talk",
            "think",
            "dismiss",
            "nervous_idle",
            "scripted"
    );
    public static final Set<String> ALLOWED_POSES = Set.of(
            "standing",
            "blocking",
            "suspicious",
            "guarded",
            "listening",
            "composed",
            "restless",
            "leaning",
            "pacing",
            "conversation",
            "talking",
            "thinking",
            "dismissing",
            "nervous",
            "scripted"
    );

    public NpcRoutineDefinition {
        steps = List.copyOf(steps);
    }

    public Optional<Step> stepForTime(long dayTime) {
        if (steps.isEmpty()) {
            return Optional.empty();
        }
        long t = Math.floorMod(dayTime, 24000L);
        return steps.stream().filter(step -> step.contains(t)).findFirst().or(() -> Optional.of(steps.getFirst()));
    }

    public static Optional<NpcRoutineDefinition> parse(Identifier id, JsonObject json, List<String> messages) {
        try {
            List<Step> steps = new ArrayList<>();
            if (json.has("steps") && json.get("steps").isJsonArray()) {
                JsonArray array = json.getAsJsonArray("steps");
                for (int i = 0; i < array.size(); i++) {
                    JsonElement element = array.get(i);
                    if (!element.isJsonObject()) {
                        messages.add("npc routine " + id + ": steps[" + i + "] must be an object");
                        continue;
                    }
                    parseStep(element.getAsJsonObject(), id, i, messages).ifPresent(steps::add);
                }
            }
            LookAtPlayer lookAtPlayer = parseLookAtPlayer(json.has("look_at_player") && json.get("look_at_player").isJsonObject()
                    ? json.getAsJsonObject("look_at_player")
                    : new JsonObject());
            return Optional.of(new NpcRoutineDefinition(id, steps, lookAtPlayer));
        } catch (RuntimeException ex) {
            messages.add("npc routine " + id + ": " + ex.getMessage());
            return Optional.empty();
        }
    }

    private static Optional<Step> parseStep(JsonObject json, Identifier id, int index, List<String> messages) {
        JsonArray time = json.has("time") && json.get("time").isJsonArray() ? json.getAsJsonArray("time") : null;
        if (time == null || time.size() != 2) {
            messages.add("npc routine " + id + ": steps[" + index + "].time must be [start,end]");
            return Optional.empty();
        }
        int start = time.get(0).getAsInt();
        int end = time.get(1).getAsInt();
        if (start < 0 || start > 24000 || end < 0 || end > 24000 || start == end) {
            messages.add("npc routine " + id + ": steps[" + index + "].time entries must be 0..24000 and not equal");
            return Optional.empty();
        }
        String action = GsonHelper.getAsString(json, "action", "stand").trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_ACTIONS.contains(action)) {
            messages.add("npc routine " + id + ": steps[" + index + "].action \"" + action
                    + "\" is invalid; expected one of " + ALLOWED_ACTIONS);
            return Optional.empty();
        }
        Optional<Vec3> pos = json.has("pos") && json.get("pos").isJsonArray() ? Optional.of(parseVec3(json.getAsJsonArray("pos"))) : Optional.empty();
        List<Vec3> path = new ArrayList<>();
        if (json.has("path") && json.get("path").isJsonArray()) {
            for (JsonElement element : json.getAsJsonArray("path")) {
                if (element.isJsonArray()) {
                    path.add(parseVec3(element.getAsJsonArray()));
                }
            }
        }
        Optional<String> look = json.has("look") && !json.get("look").isJsonNull() ? Optional.of(GsonHelper.getAsString(json, "look")) : Optional.empty();
        Optional<String> animation = optionalString(json, "animation").or(() -> optionalString(json, "play_animation"))
                .map(value -> value.trim().toLowerCase(Locale.ROOT));
        Optional<String> pose = optionalString(json, "pose").or(() -> optionalString(json, "set_pose"))
                .map(value -> value.trim().toLowerCase(Locale.ROOT));
        if (animation.isPresent() && !ALLOWED_ANIMATIONS.contains(animation.get())) {
            messages.add("npc routine " + id + ": steps[" + index + "].animation \"" + animation.get()
                    + "\" is invalid; expected one of " + ALLOWED_ANIMATIONS);
            return Optional.empty();
        }
        if (pose.isPresent() && !ALLOWED_POSES.contains(pose.get())) {
            messages.add("npc routine " + id + ": steps[" + index + "].pose \"" + pose.get()
                    + "\" is invalid; expected one of " + ALLOWED_POSES);
            return Optional.empty();
        }
        if ("walk_path".equals(action) && path.size() < 2) {
            messages.add("npc routine " + id + ": steps[" + index + "].path must contain at least two waypoints for walk_path");
            return Optional.empty();
        }
        if ((Set.of("stand", "walk", "look_at", "teleport_fallback").contains(action)) && pos.isEmpty() && path.isEmpty()) {
            messages.add("npc routine " + id + ": steps[" + index + "] action " + action + " requires pos or path");
            return Optional.empty();
        }
        double teleportDistance = optionalDouble(json, "teleport_distance")
                .or(() -> optionalDouble(json, "teleportFallbackDistance"))
                .orElse(16.0D);
        return Optional.of(new Step(start, end, action, pos, List.copyOf(path), look, animation, pose, teleportDistance));
    }

    private static LookAtPlayer parseLookAtPlayer(JsonObject json) {
        boolean enabled = GsonHelper.getAsBoolean(json, "enabled", false);
        double range = GsonHelper.getAsDouble(json, "range", 4.0D);
        float maxYawSpeed = GsonHelper.getAsFloat(json, "max_yaw_speed", 8.0F);
        boolean requiresLineOfSight = GsonHelper.getAsBoolean(
                json,
                "requires_line_of_sight",
                GsonHelper.getAsBoolean(json, "requiresLineOfSight", true)
        );
        return new LookAtPlayer(enabled, range, maxYawSpeed, requiresLineOfSight);
    }

    private static Vec3 parseVec3(JsonArray array) {
        if (array.size() != 3) {
            throw new IllegalArgumentException("vector must contain 3 numbers");
        }
        return new Vec3(array.get(0).getAsDouble(), array.get(1).getAsDouble(), array.get(2).getAsDouble());
    }

    private static Optional<String> optionalString(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull()
                ? Optional.of(GsonHelper.getAsString(json, key))
                : Optional.empty();
    }

    private static Optional<Double> optionalDouble(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull()
                && json.get(key).isJsonPrimitive()
                && json.get(key).getAsJsonPrimitive().isNumber()
                ? Optional.of(GsonHelper.getAsDouble(json, key))
                : Optional.empty();
    }

    public record Step(
            int startTime,
            int endTime,
            String action,
            Optional<Vec3> pos,
            List<Vec3> path,
            Optional<String> look,
            Optional<String> animation,
            Optional<String> pose,
            double teleportDistance
    ) {
        public boolean contains(long time) {
            if (startTime <= endTime) {
                return time >= startTime && time < endTime;
            }
            return time >= startTime || time < endTime;
        }

        public Vec3 destination() {
            return destinationAt(0);
        }

        public Vec3 destinationAt(int pathIndex) {
            if (!path.isEmpty()) {
                return path.get(Math.floorMod(pathIndex, path.size()));
            }
            return pos.orElse(null);
        }

        public boolean hasWaypointPath() {
            return path.size() > 1;
        }

        public String debugSummary() {
            return "time=[" + startTime + "," + endTime + "] action=" + action
                    + " pos=" + pos.map(Vec3::toString).orElse("-")
                    + " path=" + path.size()
                    + " look=" + look.orElse("-")
                    + " animation=" + animation.orElse("-")
                    + " pose=" + pose.orElse("-")
                    + " teleport_distance=" + teleportDistance;
        }
    }

    public record LookAtPlayer(boolean enabled, double range, float maxYawSpeed, boolean requiresLineOfSight) {
        public String debugSummary() {
            return "look_at_player(enabled=" + enabled
                    + ", range=" + range
                    + ", max_yaw_speed=" + maxYawSpeed
                    + ", requires_line_of_sight=" + requiresLineOfSight + ")";
        }
    }
}
