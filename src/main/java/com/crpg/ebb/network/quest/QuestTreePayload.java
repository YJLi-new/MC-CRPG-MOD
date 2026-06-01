package com.crpg.ebb.network.quest;

import com.crpg.ebb.EbbMod;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;

public record QuestTreePayload(List<String> lines) implements CustomPacketPayload {
    public static final int MAX_LINES = 1024;
    public static final int MAX_LINE_LENGTH = 1024;
    public static final Type<QuestTreePayload> TYPE = new Type<>(EbbMod.id("quest/tree"));
    public static final StreamCodec<RegistryFriendlyByteBuf, QuestTreePayload> CODEC = StreamCodec.ofMember(
            QuestTreePayload::write,
            QuestTreePayload::read
    );

    public QuestTreePayload {
        lines = List.copyOf(lines.stream().limit(MAX_LINES).toList());
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        int count = Math.min(lines.size(), MAX_LINES);
        buffer.writeVarInt(count);
        for (int i = 0; i < count; i++) {
            buffer.writeUtf(lines.get(i), MAX_LINE_LENGTH);
        }
    }

    private static QuestTreePayload read(RegistryFriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        if (count < 0 || count > MAX_LINES) {
            throw new DecoderException("Invalid quest tree line count: " + count);
        }
        List<String> lines = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            lines.add(buffer.readUtf(MAX_LINE_LENGTH));
        }
        return new QuestTreePayload(lines);
    }

    @Override
    public Type<QuestTreePayload> type() {
        return TYPE;
    }
}
