package com.crpg.ebb.network.llm;

import com.crpg.ebb.EbbMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record NpcProfileSyncPayload(
        String npcKey,
        String displayName,
        String tier,
        String profileSummary,
        boolean llmEnabled
) implements CustomPacketPayload {
    public static final Type<NpcProfileSyncPayload> TYPE = new Type<>(EbbMod.id("llm/npc_profile_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, NpcProfileSyncPayload> CODEC = StreamCodec.ofMember(
            NpcProfileSyncPayload::write,
            NpcProfileSyncPayload::read
    );

    public NpcProfileSyncPayload {
        npcKey = npcKey == null ? "" : npcKey;
        displayName = displayName == null ? npcKey : displayName;
        tier = tier == null ? "" : tier;
        profileSummary = profileSummary == null ? "" : profileSummary;
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeUtf(npcKey, LlmPayloadCodecs.MAX_NPC_KEY_LENGTH);
        buffer.writeUtf(displayName, LlmPayloadCodecs.MAX_DISPLAY_NAME_LENGTH);
        buffer.writeUtf(tier, 64);
        buffer.writeUtf(profileSummary, 1024);
        buffer.writeBoolean(llmEnabled);
    }

    private static NpcProfileSyncPayload read(RegistryFriendlyByteBuf buffer) {
        return new NpcProfileSyncPayload(
                buffer.readUtf(LlmPayloadCodecs.MAX_NPC_KEY_LENGTH),
                buffer.readUtf(LlmPayloadCodecs.MAX_DISPLAY_NAME_LENGTH),
                buffer.readUtf(64),
                buffer.readUtf(1024),
                buffer.readBoolean()
        );
    }

    @Override
    public Type<NpcProfileSyncPayload> type() {
        return TYPE;
    }
}
