package com.crpg.ebb.interaction;

import com.crpg.ebb.EbbMod;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class BlockGroupIndex {
    private static volatile Snapshot snapshot = Snapshot.empty();

    private BlockGroupIndex() {
    }

    public static void rebuild(Map<Identifier, JsonObject> rawDefinitions) {
        Map<Identifier, BlockGroupDefinition> byId = new LinkedHashMap<>();
        Map<DimensionBlockKey, Identifier> byBlock = new LinkedHashMap<>();
        Map<DimensionChunkKey, List<Identifier>> byChunk = new LinkedHashMap<>();
        List<String> messages = new ArrayList<>();

        for (Map.Entry<Identifier, JsonObject> entry : rawDefinitions.entrySet()) {
            Optional<BlockGroupDefinition> parsed = BlockGroupDefinition.parse(entry.getKey(), entry.getValue(), messages);
            if (parsed.isEmpty()) {
                continue;
            }

            BlockGroupDefinition definition = parsed.get();
            byId.put(definition.id(), definition);
            for (BlockPos block : definition.blocks()) {
                DimensionBlockKey blockKey = new DimensionBlockKey(definition.dimension(), block.immutable());
                Identifier previous = byBlock.put(blockKey, definition.id());
                if (previous != null && !previous.equals(definition.id())) {
                    messages.add("Block " + block + " in " + definition.dimension().identifier()
                            + " is assigned to both " + previous + " and " + definition.id());
                }

                DimensionChunkKey chunkKey = new DimensionChunkKey(definition.dimension(), new ChunkPos(block.getX() >> 4, block.getZ() >> 4));
                byChunk.computeIfAbsent(chunkKey, ignored -> new ArrayList<>()).add(definition.id());
            }
        }

        snapshot = new Snapshot(
                Map.copyOf(byId),
                Map.copyOf(byBlock),
                copyChunkMap(byChunk),
                List.copyOf(messages)
        );
        EbbMod.LOGGER.info("Built block group index: {} group(s), {} indexed block(s), {} chunk bucket(s), {} message(s).",
                snapshot.byId.size(), snapshot.byBlock.size(), snapshot.byChunk.size(), snapshot.messages.size());
        for (String message : messages) {
            EbbMod.LOGGER.warn("Block group index: {}", message);
        }
    }

    public static int groupCount() {
        return snapshot.byId.size();
    }

    public static int indexedBlockCount() {
        return snapshot.byBlock.size();
    }

    public static List<String> messages() {
        return snapshot.messages;
    }

    public static Optional<BlockGroupDefinition> byId(Identifier id) {
        return Optional.ofNullable(snapshot.byId.get(id));
    }

    public static Optional<BlockGroupDefinition> byBlock(ResourceKey<Level> dimension, BlockPos blockPos) {
        Identifier id = snapshot.byBlock.get(new DimensionBlockKey(dimension, blockPos.immutable()));
        return id == null ? Optional.empty() : byId(id);
    }

    public static List<BlockGroupDefinition> inChunk(ResourceKey<Level> dimension, ChunkPos chunkPos) {
        List<Identifier> ids = snapshot.byChunk.getOrDefault(new DimensionChunkKey(dimension, chunkPos), List.of());
        return ids.stream().distinct().map(snapshot.byId::get).toList();
    }

    public static String summaryLine() {
        return "block_group_index=" + groupCount()
                + ", indexed_blocks=" + indexedBlockCount()
                + ", index_messages=" + messages().size();
    }

    private static Map<DimensionChunkKey, List<Identifier>> copyChunkMap(Map<DimensionChunkKey, List<Identifier>> source) {
        Map<DimensionChunkKey, List<Identifier>> copy = new LinkedHashMap<>();
        source.forEach((key, value) -> copy.put(key, List.copyOf(value)));
        return Map.copyOf(copy);
    }

    private record DimensionBlockKey(ResourceKey<Level> dimension, BlockPos blockPos) {
    }

    private record DimensionChunkKey(ResourceKey<Level> dimension, ChunkPos chunkPos) {
    }

    private record Snapshot(
            Map<Identifier, BlockGroupDefinition> byId,
            Map<DimensionBlockKey, Identifier> byBlock,
            Map<DimensionChunkKey, List<Identifier>> byChunk,
            List<String> messages
    ) {
        private static Snapshot empty() {
            return new Snapshot(Map.of(), Map.of(), Map.of(), List.of());
        }
    }
}
