package com.crpg.ebb.client.interaction;

import com.crpg.ebb.EbbMod;
import com.crpg.ebb.interaction.BlockGroupDefinition;
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

public final class ClientBlockGroupIndex {
    private static volatile Snapshot snapshot = Snapshot.empty();

    private ClientBlockGroupIndex() {
    }

    public static void rebuild(List<BlockGroupDefinition> definitions) {
        Map<Identifier, BlockGroupDefinition> byId = new LinkedHashMap<>();
        Map<DimensionBlockKey, Identifier> byBlock = new LinkedHashMap<>();
        Map<DimensionChunkKey, List<Identifier>> byChunk = new LinkedHashMap<>();
        List<String> messages = new ArrayList<>();
        for (BlockGroupDefinition definition : definitions) {
            byId.put(definition.id(), definition);
            for (BlockPos block : definition.blocks()) {
                DimensionBlockKey blockKey = new DimensionBlockKey(definition.dimension(), block.immutable());
                Identifier previous = byBlock.put(blockKey, definition.id());
                if (previous != null && !previous.equals(definition.id())) {
                    messages.add("client duplicate block group block " + block + " for " + previous + " and " + definition.id());
                }
                DimensionChunkKey chunkKey = new DimensionChunkKey(definition.dimension(), new ChunkPos(block.getX() >> 4, block.getZ() >> 4));
                byChunk.computeIfAbsent(chunkKey, ignored -> new ArrayList<>()).add(definition.id());
            }
        }
        snapshot = new Snapshot(Map.copyOf(byId), Map.copyOf(byBlock), copyChunkMap(byChunk), List.copyOf(messages));
        EbbMod.LOGGER.info("Client block group index synced: {} group(s), {} block(s), {} message(s).",
                snapshot.byId.size(), snapshot.byBlock.size(), snapshot.messages.size());
        messages.forEach(message -> EbbMod.LOGGER.warn("Client block group index: {}", message));
    }

    public static void clear() {
        snapshot = Snapshot.empty();
    }

    public static Optional<BlockGroupDefinition> byBlock(ResourceKey<Level> dimension, BlockPos blockPos) {
        Identifier id = snapshot.byBlock.get(new DimensionBlockKey(dimension, blockPos.immutable()));
        return id == null ? Optional.empty() : Optional.ofNullable(snapshot.byId.get(id));
    }

    public static int groupCount() {
        return snapshot.byId.size();
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
        static Snapshot empty() {
            return new Snapshot(Map.of(), Map.of(), Map.of(), List.of());
        }
    }
}
