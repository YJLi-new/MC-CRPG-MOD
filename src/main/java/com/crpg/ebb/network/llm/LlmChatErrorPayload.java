package com.crpg.ebb.network.llm;

import com.crpg.ebb.EbbMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record LlmChatErrorPayload(UUID conversationId, String reason) implements CustomPacketPayload {
    public static final Type<LlmChatErrorPayload> TYPE = new Type<>(EbbMod.id("llm/chat_error"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LlmChatErrorPayload> CODEC = StreamCodec.ofMember(
            LlmChatErrorPayload::write,
            LlmChatErrorPayload::read
    );

    public LlmChatErrorPayload {
        reason = reason == null ? "llm_error" : reason;
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeUUID(conversationId);
        buffer.writeUtf(reason, LlmPayloadCodecs.MAX_REASON_LENGTH);
    }

    private static LlmChatErrorPayload read(RegistryFriendlyByteBuf buffer) {
        return new LlmChatErrorPayload(buffer.readUUID(), buffer.readUtf(LlmPayloadCodecs.MAX_REASON_LENGTH));
    }

    @Override
    public Type<LlmChatErrorPayload> type() {
        return TYPE;
    }
}
