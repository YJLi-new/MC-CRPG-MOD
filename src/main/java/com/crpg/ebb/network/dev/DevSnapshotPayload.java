package com.crpg.ebb.network.dev;

import com.crpg.ebb.EbbMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;

public record DevSnapshotPayload(List<String> lines) implements CustomPacketPayload {
    public static final int MAX_LINES = 160;
    public static final int MAX_LINE_LENGTH = 512;
    public static final Type<DevSnapshotPayload> TYPE = new Type<>(EbbMod.id("dev/snapshot"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DevSnapshotPayload> CODEC = StreamCodec.ofMember(
            DevSnapshotPayload::write,
            DevSnapshotPayload::read
    );

    public DevSnapshotPayload {
        lines = List.copyOf(lines.stream().limit(MAX_LINES).toList());
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        int count = Math.min(lines.size(), MAX_LINES);
        buffer.writeVarInt(count);
        for (int i = 0; i < count; i++) {
            buffer.writeUtf(lines.get(i), MAX_LINE_LENGTH);
        }
    }

    private static DevSnapshotPayload read(RegistryFriendlyByteBuf buffer) {
        int count = Math.min(buffer.readVarInt(), MAX_LINES);
        List<String> lines = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            lines.add(buffer.readUtf(MAX_LINE_LENGTH));
        }
        return new DevSnapshotPayload(lines);
    }

    @Override
    public Type<DevSnapshotPayload> type() {
        return TYPE;
    }
}
