package com.crpg.ebb.interaction;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record BlockGroupDefinition(
        Identifier id,
        ResourceKey<Level> dimension,
        List<BlockPos> blocks,
        Vec3 interactionPoint,
        Identifier dialogueId,
        AABB bounds
) {
    public BlockGroupDefinition {
        blocks = List.copyOf(blocks);
    }

    public BlockGroupTarget asTarget() {
        return new BlockGroupTarget(id, dimension, blocks, interactionPoint, dialogueId, bounds);
    }

    public static Optional<BlockGroupDefinition> parse(Identifier id, JsonObject json, List<String> messages) {
        try {
            ResourceKey<Level> dimension = parseDimension(GsonHelper.getAsString(json, "dimension", "minecraft:overworld"));
            Identifier dialogueId = Identifier.parse(GsonHelper.getAsString(json, "dialogue"));
            List<BlockPos> blocks = parseBlocks(GsonHelper.getAsJsonArray(json, "blocks"));
            if (blocks.isEmpty()) {
                messages.add("Block group " + id + " has no blocks.");
                return Optional.empty();
            }

            AABB bounds = computeBounds(blocks);
            Vec3 interactionPoint = json.has("interaction_point")
                    ? parseVec3(GsonHelper.getAsJsonArray(json, "interaction_point"), "interaction_point")
                    : bounds.getCenter();

            return Optional.of(new BlockGroupDefinition(id, dimension, blocks, interactionPoint, dialogueId, bounds));
        } catch (RuntimeException ex) {
            messages.add("Invalid block group " + id + ": " + ex.getMessage());
            return Optional.empty();
        }
    }

    private static ResourceKey<Level> parseDimension(String value) {
        return ResourceKey.create(Registries.DIMENSION, Identifier.parse(value));
    }

    private static List<BlockPos> parseBlocks(JsonArray blocksArray) {
        List<BlockPos> blocks = new ArrayList<>();
        int index = 0;
        for (JsonElement element : blocksArray) {
            JsonArray coords = requireArraySize(element, "blocks[" + index + "]", 3);
            blocks.add(new BlockPos(coords.get(0).getAsInt(), coords.get(1).getAsInt(), coords.get(2).getAsInt()));
            index++;
        }
        return blocks;
    }

    private static Vec3 parseVec3(JsonArray coords, String name) {
        if (coords.size() != 3) {
            throw new JsonParseException(name + " must contain exactly 3 numbers");
        }
        return new Vec3(coords.get(0).getAsDouble(), coords.get(1).getAsDouble(), coords.get(2).getAsDouble());
    }

    private static JsonArray requireArraySize(JsonElement element, String name, int size) {
        if (!element.isJsonArray()) {
            throw new JsonParseException(name + " must be an array");
        }
        JsonArray array = element.getAsJsonArray();
        if (array.size() != size) {
            throw new JsonParseException(name + " must contain exactly " + size + " numbers");
        }
        return array;
    }

    private static AABB computeBounds(List<BlockPos> blocks) {
        BlockPos min = blocks.getFirst();
        BlockPos max = blocks.getFirst();
        for (BlockPos block : blocks) {
            min = BlockPos.min(min, block);
            max = BlockPos.max(max, block);
        }
        return AABB.encapsulatingFullBlocks(min, max);
    }
}
