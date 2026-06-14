package com.crpg.ebb.network.llm;

import com.crpg.ebb.EbbMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record LlmChatMessagePayload(UUID conversationId, long nonce, String message) implements CustomPacketPayload {
    public static final Type<LlmChatMessagePayload> TYPE = new Type<>(EbbMod.id("llm/chat_message"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LlmChatMessagePayload> CODEC = StreamCodec.ofMember(
            LlmChatMessagePayload::write,
            LlmChatMessagePayload::read
    );

    public LlmChatMessagePayload {
        message = message == null ? "" : message;
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeUUID(conversationId);
        buffer.writeLong(nonce);
        buffer.writeUtf(message, LlmPayloadCodecs.MAX_MESSAGE_LENGTH);
    }

    private static LlmChatMessagePayload read(RegistryFriendlyByteBuf buffer) {
        return new LlmChatMessagePayload(buffer.readUUID(), buffer.readLong(), buffer.readUtf(LlmPayloadCodecs.MAX_MESSAGE_LENGTH));
    }

    @Override
    public Type<LlmChatMessagePayload> type() {
        return TYPE;
    }
}
