package com.crpg.ebb.network.journal;

import com.crpg.ebb.EbbMod;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;

public record JournalPayload(List<String> lines) implements CustomPacketPayload {
    public static final int MAX_LINES = 1024;
    public static final int MAX_LINE_LENGTH = 1024;
    public static final Type<JournalPayload> TYPE = new Type<>(EbbMod.id("journal/open"));
    public static final StreamCodec<RegistryFriendlyByteBuf, JournalPayload> CODEC = StreamCodec.ofMember(
            JournalPayload::write,
            JournalPayload::read
    );

    public JournalPayload {
        lines = List.copyOf(lines.stream().limit(MAX_LINES).toList());
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        int count = Math.min(lines.size(), MAX_LINES);
        buffer.writeVarInt(count);
        for (int i = 0; i < count; i++) {
            buffer.writeUtf(lines.get(i), MAX_LINE_LENGTH);
        }
    }

    private static JournalPayload read(RegistryFriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        if (count < 0 || count > MAX_LINES) {
            throw new DecoderException("Invalid journal line count: " + count);
        }
        List<String> lines = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            lines.add(buffer.readUtf(MAX_LINE_LENGTH));
        }
        return new JournalPayload(lines);
    }

    @Override
    public Type<JournalPayload> type() {
        return TYPE;
    }
}
