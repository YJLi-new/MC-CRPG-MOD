package com.crpg.ebb.interaction;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record BlockGroupDefinition(
        Identifier id,
        ResourceKey<Level> dimension,
        List<BlockPos> blocks,
        Map<BlockPos, Identifier> expectedBlocks,
        Vec3 interactionPoint,
        Identifier dialogueId,
        AABB bounds,
        HighlightStyle highlightStyle
) {
    public BlockGroupDefinition {
        blocks = List.copyOf(blocks);
        expectedBlocks = Map.copyOf(expectedBlocks);
        highlightStyle = highlightStyle == null ? HighlightStyle.blockDefault() : highlightStyle;
    }

    public BlockGroupTarget asTarget() {
        return new BlockGroupTarget(id, dimension, blocks, interactionPoint, dialogueId, bounds);
    }

    public boolean expectedBlocksMatch(Level level) {
        if (expectedBlocks.isEmpty()) {
            return true;
        }
        for (Map.Entry<BlockPos, Identifier> entry : expectedBlocks.entrySet()) {
            Block actual = level.getBlockState(entry.getKey()).getBlock();
            Identifier actualId = BuiltInRegistries.BLOCK.getKey(actual);
            if (!entry.getValue().equals(actualId)) {
                return false;
            }
        }
        return true;
    }

    public static Optional<BlockGroupDefinition> parse(Identifier id, JsonObject json, List<String> messages) {
        try {
            ResourceKey<Level> dimension = parseDimension(GsonHelper.getAsString(json, "dimension", "minecraft:overworld"));
            optionalString(json, "id").ifPresent(declaredId -> {
                if (!declaredId.equals(id.toString()) && !declaredId.equals(id.getPath())) {
                    messages.add("Block group " + id + " declares mismatched id=" + declaredId);
                }
            });
            Identifier dialogueId = parseIdentifier(GsonHelper.getAsString(json, "dialogue"));
            ParsedBlocks parsedBlocks = parseBlocksOrBoxes(json, messages, id);
            if (parsedBlocks.blocks().isEmpty()) {
                messages.add("Block group " + id + " has no blocks.");
                return Optional.empty();
            }
            int maxBlocks = InteractionSettings.maxBlocksPerGroup();
            if (parsedBlocks.blocks().size() > maxBlocks) {
                messages.add("Block group " + id + " has " + parsedBlocks.blocks().size()
                        + " blocks, exceeding max_blocks_per_group=" + maxBlocks
                        + "; split it into smaller groups instead of relying on sync truncation.");
                return Optional.empty();
            }

            AABB bounds = computeBounds(parsedBlocks.blocks());
            Vec3 interactionPoint = json.has("interaction_point")
                    ? parseVec3(GsonHelper.getAsJsonArray(json, "interaction_point"), "interaction_point")
                    : json.has("anchor")
                    ? parseVec3(GsonHelper.getAsJsonArray(json, "anchor"), "anchor")
                    : bounds.getCenter();

            HighlightStyle highlightStyle = HighlightStyle.parseOptional(
                    json,
                    HighlightStyle.blockDefault(),
                    messages,
                    "Block group " + id
            ).orElse(HighlightStyle.blockDefault());

            return Optional.of(new BlockGroupDefinition(id, dimension, parsedBlocks.blocks(), parsedBlocks.expectedBlocks(), interactionPoint, dialogueId, bounds, highlightStyle));
        } catch (RuntimeException ex) {
            messages.add("Invalid block group " + id + ": " + ex.getMessage());
            return Optional.empty();
        }
    }

    public static BlockGroupDefinition fromSynced(
            Identifier id,
            ResourceKey<Level> dimension,
            List<BlockPos> blocks,
            Map<BlockPos, Identifier> expectedBlocks,
            Vec3 interactionPoint,
            Identifier dialogueId,
            HighlightStyle highlightStyle
    ) {
        return new BlockGroupDefinition(id, dimension, blocks, expectedBlocks, interactionPoint, dialogueId, computeBounds(blocks), highlightStyle);
    }

    private static ResourceKey<Level> parseDimension(String value) {
        return ResourceKey.create(Registries.DIMENSION, Identifier.parse(value));
    }

    private static ParsedBlocks parseBlocks(JsonArray blocksArray, List<String> messages, Identifier groupId) {
        List<BlockPos> blocks = new ArrayList<>();
        Map<BlockPos, Identifier> expectedBlocks = new LinkedHashMap<>();
        int index = 0;
        for (JsonElement element : blocksArray) {
            if (element.isJsonArray()) {
                JsonArray coords = requireArraySize(element, "blocks[" + index + "]", 3);
                blocks.add(new BlockPos(coords.get(0).getAsInt(), coords.get(1).getAsInt(), coords.get(2).getAsInt()));
            } else if (element.isJsonObject()) {
                JsonObject object = element.getAsJsonObject();
                JsonArray coords = requireArraySize(object.get("pos"), "blocks[" + index + "].pos", 3);
                BlockPos pos = new BlockPos(coords.get(0).getAsInt(), coords.get(1).getAsInt(), coords.get(2).getAsInt());
                blocks.add(pos);
                if (object.has("block") && !object.get("block").isJsonNull()) {
                    Identifier blockId = Identifier.parse(GsonHelper.getAsString(object, "block"));
                    if (!BuiltInRegistries.BLOCK.containsKey(blockId)) {
                        messages.add("Block group " + groupId + " block " + pos + " references unknown block " + blockId);
                    }
                    expectedBlocks.put(pos.immutable(), blockId);
                }
            } else {
                throw new JsonParseException("blocks[" + index + "] must be [x,y,z] or {pos:[x,y,z], block:...}");
            }
            index++;
        }
        return new ParsedBlocks(List.copyOf(blocks), Map.copyOf(expectedBlocks));
    }

    private static ParsedBlocks parseBlocksOrBoxes(JsonObject json, List<String> messages, Identifier groupId) {
        if (json.has("blocks") && json.get("blocks").isJsonArray()) {
            return parseBlocks(GsonHelper.getAsJsonArray(json, "blocks"), messages, groupId);
        }
        if (json.has("boxes") && json.get("boxes").isJsonArray()) {
            return parseBoxes(GsonHelper.getAsJsonArray(json, "boxes"));
        }
        throw new JsonParseException("missing blocks array or boxes array");
    }

    private static ParsedBlocks parseBoxes(JsonArray boxesArray) {
        List<BlockPos> blocks = new ArrayList<>();
        int index = 0;
        for (JsonElement element : boxesArray) {
            if (!element.isJsonObject()) {
                throw new JsonParseException("boxes[" + index + "] must be an object");
            }
            JsonObject object = element.getAsJsonObject();
            JsonArray min = requireArraySize(object.get("min"), "boxes[" + index + "].min", 3);
            JsonArray max = requireArraySize(object.get("max"), "boxes[" + index + "].max", 3);
            int minX = (int) Math.floor(Math.min(min.get(0).getAsDouble(), max.get(0).getAsDouble()));
            int minY = (int) Math.floor(Math.min(min.get(1).getAsDouble(), max.get(1).getAsDouble()));
            int minZ = (int) Math.floor(Math.min(min.get(2).getAsDouble(), max.get(2).getAsDouble()));
            int maxX = (int) Math.floor(Math.max(min.get(0).getAsDouble(), max.get(0).getAsDouble()));
            int maxY = (int) Math.floor(Math.max(min.get(1).getAsDouble(), max.get(1).getAsDouble()));
            int maxZ = (int) Math.floor(Math.max(min.get(2).getAsDouble(), max.get(2).getAsDouble()));
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        blocks.add(new BlockPos(x, y, z));
                    }
                }
            }
            index++;
        }
        return new ParsedBlocks(List.copyOf(blocks), Map.of());
    }

    private static Vec3 parseVec3(JsonArray coords, String name) {
        if (coords.size() != 3) {
            throw new JsonParseException(name + " must contain exactly 3 numbers");
        }
        return new Vec3(coords.get(0).getAsDouble(), coords.get(1).getAsDouble(), coords.get(2).getAsDouble());
    }

    private static JsonArray requireArraySize(JsonElement element, String name, int size) {
        if (element == null || !element.isJsonArray()) {
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

    private static Identifier parseIdentifier(String value) {
        return value.contains(":") ? Identifier.parse(value) : Identifier.fromNamespaceAndPath("ebb", value);
    }

    private static Optional<String> optionalString(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull()
                ? Optional.of(GsonHelper.getAsString(json, key))
                : Optional.empty();
    }

    private record ParsedBlocks(List<BlockPos> blocks, Map<BlockPos, Identifier> expectedBlocks) {
    }
}
