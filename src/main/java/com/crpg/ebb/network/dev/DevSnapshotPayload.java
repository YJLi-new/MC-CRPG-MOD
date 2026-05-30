package com.crpg.ebb.network.dev;

import com.crpg.ebb.EbbMod;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;

public record DevSnapshotPayload(List<String> lines) implements CustomPacketPayload {
    public static final int MAX_LINES = 2048;
    public static final int MAX_LINE_LENGTH = 1024;
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
        int count = buffer.readVarInt();
        if (count < 0 || count > MAX_LINES) {
            throw new DecoderException("Invalid dev snapshot line count: " + count);
        }
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
