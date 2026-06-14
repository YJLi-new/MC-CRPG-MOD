package com.crpg.ebb.network.llm;

import com.crpg.ebb.EbbMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.Optional;
import java.util.UUID;

public record LlmChatOpenedPayload(
        UUID conversationId,
        String npcKey,
        String npcDisplayName,
        Optional<String> topicHint,
        String statusMessage,
        int maxInputChars
) implements CustomPacketPayload {
    public static final Type<LlmChatOpenedPayload> TYPE = new Type<>(EbbMod.id("llm/chat_opened"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LlmChatOpenedPayload> CODEC = StreamCodec.ofMember(
            LlmChatOpenedPayload::write,
            LlmChatOpenedPayload::read
    );

    public LlmChatOpenedPayload {
        topicHint = topicHint == null ? Optional.empty() : topicHint;
        npcKey = npcKey == null ? "unknown_npc" : npcKey;
        npcDisplayName = npcDisplayName == null ? npcKey : npcDisplayName;
        statusMessage = statusMessage == null ? "" : statusMessage;
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeUUID(conversationId);
        buffer.writeUtf(npcKey, LlmPayloadCodecs.MAX_NPC_KEY_LENGTH);
        buffer.writeUtf(npcDisplayName, LlmPayloadCodecs.MAX_DISPLAY_NAME_LENGTH);
        LlmPayloadCodecs.writeOptionalUtf(buffer, topicHint, LlmPayloadCodecs.MAX_TOPIC_LENGTH);
        buffer.writeUtf(statusMessage, LlmPayloadCodecs.MAX_REASON_LENGTH);
        buffer.writeVarInt(maxInputChars);
    }

    private static LlmChatOpenedPayload read(RegistryFriendlyByteBuf buffer) {
        return new LlmChatOpenedPayload(
                buffer.readUUID(),
                buffer.readUtf(LlmPayloadCodecs.MAX_NPC_KEY_LENGTH),
                buffer.readUtf(LlmPayloadCodecs.MAX_DISPLAY_NAME_LENGTH),
                LlmPayloadCodecs.readOptionalUtf(buffer, LlmPayloadCodecs.MAX_TOPIC_LENGTH),
                buffer.readUtf(LlmPayloadCodecs.MAX_REASON_LENGTH),
                buffer.readVarInt()
        );
    }

    @Override
    public Type<LlmChatOpenedPayload> type() {
        return TYPE;
    }
}
