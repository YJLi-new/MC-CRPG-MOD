package com.crpg.ebb.network.llm;

import com.crpg.ebb.EbbMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

public record MemoryDebugSnapshotPayload(List<String> lines) implements CustomPacketPayload {
    public static final Type<MemoryDebugSnapshotPayload> TYPE = new Type<>(EbbMod.id("llm/memory_debug_snapshot"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MemoryDebugSnapshotPayload> CODEC = StreamCodec.ofMember(
            MemoryDebugSnapshotPayload::write,
            MemoryDebugSnapshotPayload::read
    );

    public MemoryDebugSnapshotPayload {
        lines = lines == null ? List.of() : List.copyOf(lines);
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        LlmPayloadCodecs.writeStringList(buffer, lines, 96, 512);
    }

    private static MemoryDebugSnapshotPayload read(RegistryFriendlyByteBuf buffer) {
        return new MemoryDebugSnapshotPayload(LlmPayloadCodecs.readStringList(buffer, 96, 512));
    }

    @Override
    public Type<MemoryDebugSnapshotPayload> type() {
        return TYPE;
    }
}
