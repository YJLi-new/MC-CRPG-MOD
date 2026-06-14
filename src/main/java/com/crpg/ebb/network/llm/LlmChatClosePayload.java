package com.crpg.ebb.network.llm;

import com.crpg.ebb.EbbMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record LlmChatClosePayload(UUID conversationId, String reason) implements CustomPacketPayload {
    public static final Type<LlmChatClosePayload> TYPE = new Type<>(EbbMod.id("llm/chat_close"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LlmChatClosePayload> CODEC = StreamCodec.ofMember(
            LlmChatClosePayload::write,
            LlmChatClosePayload::read
    );

    public LlmChatClosePayload {
        reason = reason == null ? "closed" : reason;
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeUUID(conversationId);
        buffer.writeUtf(reason, LlmPayloadCodecs.MAX_REASON_LENGTH);
    }

    private static LlmChatClosePayload read(RegistryFriendlyByteBuf buffer) {
        return new LlmChatClosePayload(buffer.readUUID(), buffer.readUtf(LlmPayloadCodecs.MAX_REASON_LENGTH));
    }

    @Override
    public Type<LlmChatClosePayload> type() {
        return TYPE;
    }
}
