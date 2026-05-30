package com.crpg.ebb.network.sync;

import com.crpg.ebb.EbbMod;
import com.crpg.ebb.interaction.BlockGroupDefinition;
import io.netty.handler.codec.DecoderException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record BlockGroupSyncPayload(List<BlockGroupDefinition> definitions) implements CustomPacketPayload {
    public static final int MAX_GROUPS = 2048;
    public static final int MAX_BLOCKS_PER_GROUP = 512;
    public static final Type<BlockGroupSyncPayload> TYPE = new Type<>(EbbMod.id("sync/block_groups"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BlockGroupSyncPayload> CODEC = StreamCodec.ofMember(
            BlockGroupSyncPayload::write,
            BlockGroupSyncPayload::read
    );

    public BlockGroupSyncPayload {
        definitions = List.copyOf(definitions.stream().limit(MAX_GROUPS).toList());
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(Math.min(definitions.size(), MAX_GROUPS));
        for (int i = 0; i < Math.min(definitions.size(), MAX_GROUPS); i++) {
            writeDefinition(buffer, definitions.get(i));
        }
    }

    private static void writeDefinition(RegistryFriendlyByteBuf buffer, BlockGroupDefinition definition) {
        buffer.writeIdentifier(definition.id());
        buffer.writeIdentifier(definition.dimension().identifier());
        buffer.writeIdentifier(definition.dialogueId());
        buffer.writeDouble(definition.interactionPoint().x);
        buffer.writeDouble(definition.interactionPoint().y);
        buffer.writeDouble(definition.interactionPoint().z);
        int blockCount = Math.min(definition.blocks().size(), MAX_BLOCKS_PER_GROUP);
        buffer.writeVarInt(blockCount);
        for (int i = 0; i < blockCount; i++) {
            BlockPos blockPos = definition.blocks().get(i);
            buffer.writeBlockPos(blockPos);
            Identifier expected = definition.expectedBlocks().get(blockPos);
            buffer.writeBoolean(expected != null);
            if (expected != null) {
                buffer.writeIdentifier(expected);
            }
        }
    }

    private static BlockGroupSyncPayload read(RegistryFriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        if (count < 0 || count > MAX_GROUPS) {
            throw new DecoderException("Invalid block group sync count: " + count);
        }
        List<BlockGroupDefinition> definitions = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            definitions.add(readDefinition(buffer));
        }
        return new BlockGroupSyncPayload(definitions);
    }

    private static BlockGroupDefinition readDefinition(RegistryFriendlyByteBuf buffer) {
        Identifier id = buffer.readIdentifier();
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, buffer.readIdentifier());
        Identifier dialogueId = buffer.readIdentifier();
        Vec3 interactionPoint = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        int blockCount = buffer.readVarInt();
        if (blockCount <= 0 || blockCount > MAX_BLOCKS_PER_GROUP) {
            throw new DecoderException("Invalid block count for block group " + id + ": " + blockCount);
        }
        List<BlockPos> blocks = new ArrayList<>(blockCount);
        Map<BlockPos, Identifier> expectedBlocks = new LinkedHashMap<>();
        for (int i = 0; i < blockCount; i++) {
            BlockPos blockPos = buffer.readBlockPos().immutable();
            blocks.add(blockPos);
            if (buffer.readBoolean()) {
                expectedBlocks.put(blockPos, buffer.readIdentifier());
            }
        }
        return BlockGroupDefinition.fromSynced(id, dimension, blocks, expectedBlocks, interactionPoint, dialogueId);
    }

    @Override
    public Type<BlockGroupSyncPayload> type() {
        return TYPE;
    }
}
