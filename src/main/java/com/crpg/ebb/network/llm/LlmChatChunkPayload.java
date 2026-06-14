package com.crpg.ebb.network.llm;

import com.crpg.ebb.EbbMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record LlmChatChunkPayload(
        UUID conversationId,
        String role,
        String content,
        boolean done,
        Optional<String> statusMessage,
        List<String> citationIds
) implements CustomPacketPayload {
    public static final Type<LlmChatChunkPayload> TYPE = new Type<>(EbbMod.id("llm/chat_chunk"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LlmChatChunkPayload> CODEC = StreamCodec.ofMember(
            LlmChatChunkPayload::write,
            LlmChatChunkPayload::read
    );

    public LlmChatChunkPayload {
        role = role == null ? "npc" : role;
        content = content == null ? "" : content;
        statusMessage = statusMessage == null ? Optional.empty() : statusMessage;
        citationIds = citationIds == null ? List.of() : List.copyOf(citationIds);
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeUUID(conversationId);
        buffer.writeUtf(role, 16);
        buffer.writeUtf(content, LlmPayloadCodecs.MAX_REPLY_LENGTH);
        buffer.writeBoolean(done);
        LlmPayloadCodecs.writeOptionalUtf(buffer, statusMessage, LlmPayloadCodecs.MAX_REASON_LENGTH);
        LlmPayloadCodecs.writeStringList(buffer, citationIds, LlmPayloadCodecs.MAX_CITATIONS, LlmPayloadCodecs.MAX_CITATION_LENGTH);
    }

    private static LlmChatChunkPayload read(RegistryFriendlyByteBuf buffer) {
        return new LlmChatChunkPayload(
                buffer.readUUID(),
                buffer.readUtf(16),
                buffer.readUtf(LlmPayloadCodecs.MAX_REPLY_LENGTH),
                buffer.readBoolean(),
                LlmPayloadCodecs.readOptionalUtf(buffer, LlmPayloadCodecs.MAX_REASON_LENGTH),
                LlmPayloadCodecs.readStringList(buffer, LlmPayloadCodecs.MAX_CITATIONS, LlmPayloadCodecs.MAX_CITATION_LENGTH)
        );
    }

    @Override
    public Type<LlmChatChunkPayload> type() {
        return TYPE;
    }
}
