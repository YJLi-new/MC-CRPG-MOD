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
import java.util.Optional;

public record NpcRoutineDefinition(
        Identifier id,
        List<Step> steps,
        LookAtPlayer lookAtPlayer
) {
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
        String action = GsonHelper.getAsString(json, "action", "stand");
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
        return Optional.of(new Step(start, end, action, pos, List.copyOf(path), look));
    }

    private static LookAtPlayer parseLookAtPlayer(JsonObject json) {
        boolean enabled = GsonHelper.getAsBoolean(json, "enabled", false);
        double range = GsonHelper.getAsDouble(json, "range", 4.0D);
        float maxYawSpeed = GsonHelper.getAsFloat(json, "max_yaw_speed", 8.0F);
        return new LookAtPlayer(enabled, range, maxYawSpeed);
    }

    private static Vec3 parseVec3(JsonArray array) {
        if (array.size() != 3) {
            throw new IllegalArgumentException("vector must contain 3 numbers");
        }
        return new Vec3(array.get(0).getAsDouble(), array.get(1).getAsDouble(), array.get(2).getAsDouble());
    }

    public record Step(int startTime, int endTime, String action, Optional<Vec3> pos, List<Vec3> path, Optional<String> look) {
        public boolean contains(long time) {
            if (startTime <= endTime) {
                return time >= startTime && time < endTime;
            }
            return time >= startTime || time < endTime;
        }

        public Vec3 destination() {
            if (!path.isEmpty()) {
                return path.getFirst();
            }
            return pos.orElse(null);
        }

        public String debugSummary() {
            return "time=[" + startTime + "," + endTime + "] action=" + action
                    + " pos=" + pos.map(Vec3::toString).orElse("-")
                    + " path=" + path.size()
                    + " look=" + look.orElse("-");
        }
    }

    public record LookAtPlayer(boolean enabled, double range, float maxYawSpeed) {
        public String debugSummary() {
            return "look_at_player(enabled=" + enabled + ", range=" + range + ", max_yaw_speed=" + maxYawSpeed + ")";
        }
    }
}
