package com.crpg.ebb.network.llm;

import com.crpg.ebb.EbbMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record LlmChatCancelPayload(UUID conversationId, String reason) implements CustomPacketPayload {
    public static final Type<LlmChatCancelPayload> TYPE = new Type<>(EbbMod.id("llm/chat_cancel"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LlmChatCancelPayload> CODEC = StreamCodec.ofMember(
            LlmChatCancelPayload::write,
            LlmChatCancelPayload::read
    );

    public LlmChatCancelPayload {
        reason = reason == null ? "client_cancel" : reason;
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeUUID(conversationId);
        buffer.writeUtf(reason, LlmPayloadCodecs.MAX_REASON_LENGTH);
    }

    private static LlmChatCancelPayload read(RegistryFriendlyByteBuf buffer) {
        return new LlmChatCancelPayload(buffer.readUUID(), buffer.readUtf(LlmPayloadCodecs.MAX_REASON_LENGTH));
    }

    @Override
    public Type<LlmChatCancelPayload> type() {
        return TYPE;
    }
}
