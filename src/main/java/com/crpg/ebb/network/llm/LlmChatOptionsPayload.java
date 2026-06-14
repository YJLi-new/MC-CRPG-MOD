package com.crpg.ebb.network.llm;

import com.crpg.ebb.EbbMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;
import java.util.UUID;

public record LlmChatOptionsPayload(UUID conversationId, List<String> options) implements CustomPacketPayload {
    public static final Type<LlmChatOptionsPayload> TYPE = new Type<>(EbbMod.id("llm/chat_options"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LlmChatOptionsPayload> CODEC = StreamCodec.ofMember(
            LlmChatOptionsPayload::write,
            LlmChatOptionsPayload::read
    );

    public LlmChatOptionsPayload {
        options = options == null ? List.of() : List.copyOf(options);
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeUUID(conversationId);
        LlmPayloadCodecs.writeStringList(buffer, options, LlmPayloadCodecs.MAX_OPTIONS, LlmPayloadCodecs.MAX_OPTION_LENGTH);
    }

    private static LlmChatOptionsPayload read(RegistryFriendlyByteBuf buffer) {
        return new LlmChatOptionsPayload(buffer.readUUID(), LlmPayloadCodecs.readStringList(buffer, LlmPayloadCodecs.MAX_OPTIONS, LlmPayloadCodecs.MAX_OPTION_LENGTH));
    }

    @Override
    public Type<LlmChatOptionsPayload> type() {
        return TYPE;
    }
}
